package com.dailyshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {

    private final DailySellShop plugin;
    private final List<String> activeItems = new ArrayList<>();
    private final Map<UUID, Map<String, Integer>> playerSales = new ConcurrentHashMap<>();
    private final List<MenuButton> menuButtons = new ArrayList<>();
    private String currentRootSection = "items";
    private double limitMultiplier = 1.0;
    private int lastDayOfYear = -1;
    private int lastHourOfDay = -1;
    private ZoneId timezone = ZoneId.of("Asia/Shanghai");
    private String configuredRefreshMode = "DAILY";

    public ShopManager(DailySellShop plugin) {
        this.plugin = plugin;

        forceUpdate();
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkTimeReset, 20L, 400L);
    }

    private ZonedDateTime getNow() {
        return ZonedDateTime.now(timezone);
    }

    public void forceUpdate() {
        reloadTimezone();
        reloadRefreshMode();
        ZonedDateTime now = getNow();
        lastDayOfYear = now.getDayOfYear();
        lastHourOfDay = now.getHour();

        String mode = refreshMode();
        if (mode.equals("HOURLY")) {
            limitMultiplier = hourlyMultiplier();
        } else {
            limitMultiplier = 1.0;
        }
        rotateItems();
        loadMenuButtons();
    }

    private void checkTimeReset() {
        ZonedDateTime now = getNow();
        int currentDay = now.getDayOfYear();
        int currentHour = now.getHour();

        String mode = refreshMode();
        boolean shouldReset = false;

        if (mode.equals("DAILY")) {
            if (currentDay != lastDayOfYear) {
                plugin.getLogger().info("[每日模式] 已到北京时间 00:00，开始刷新商店。");
                limitMultiplier = 1.0;
                shouldReset = true;
            }
        } else if (mode.equals("HOURLY")) {
            if (currentHour != lastHourOfDay) {
                plugin.getLogger().info("[小时模式] 已到整点，开始刷新商店。");
                limitMultiplier = hourlyMultiplier();
                shouldReset = true;
            }
        }

        if (shouldReset) {
            lastDayOfYear = currentDay;
            lastHourOfDay = currentHour;

            playerSales.clear();
            rotateItems();
        }
    }

    public void rotateItems() {
        activeItems.clear();
        boolean isRandom = plugin.getSellConfig().getBoolean("random-rotation.enabled", true);

        if (isRandom) {
            currentRootSection = "items";
            ConfigurationSection section = plugin.getSellConfig().getConfigurationSection("items");
            if (section != null) {
                List<String> allKeys = new ArrayList<>(section.getKeys(false));
                int amount = Math.max(0, plugin.getSellConfig().getInt("random-rotation.amount", 10));
                Collections.shuffle(allKeys);
                activeItems.addAll(allKeys.subList(0, Math.min(amount, allKeys.size())));
            }
        } else {
            currentRootSection = "static-items";
            ConfigurationSection section = plugin.getSellConfig().getConfigurationSection("static-items");
            if (section != null) {
                activeItems.addAll(section.getKeys(false));
            }
        }
    }

    private void loadMenuButtons() {
        menuButtons.clear();
        ConfigurationSection section = plugin.getSellConfig().getConfigurationSection("menu.buttons");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            String path = "menu.buttons." + id + ".";
            int slot = plugin.getSellConfig().getInt(path + "slot", plugin.getSellConfig().getInt(path + "index", -1));
            if (slot < 0 || slot >= getMenuSize()) {
                plugin.getLogger().warning("忽略无效菜单按钮槽位: " + id + " -> " + slot);
                continue;
            }

            List<String> lore = plugin.getSellConfig().getStringList(path + "lore");
            List<String> commands = plugin.getSellConfig().getStringList(path + "commands");
            String permission = plugin.getSellConfig().getString(path + "permission", "");
            int amount = Math.max(1, Math.min(64, plugin.getSellConfig().getInt(path + "amount", 1)));
            boolean glow = plugin.getSellConfig().getBoolean(path + "glow",
                    plugin.getSellConfig().getBoolean(path + "isEnchant", false));
            boolean hideFlags = plugin.getSellConfig().getBoolean(path + "hide-flags",
                    plugin.getSellConfig().getBoolean(path + "hideFlag", false));
            Integer customModelData = plugin.getSellConfig().contains(path + "custom-model-data")
                    ? plugin.getSellConfig().getInt(path + "custom-model-data")
                    : null;

            menuButtons.add(new MenuButton(
                    id,
                    slot,
                    plugin.getSellConfig().getString(path + "material", "STONE"),
                    amount,
                    plugin.getSellConfig().getString(path + "name", id),
                    lore,
                    commands,
                    permission == null ? "" : permission,
                    glow,
                    hideFlags,
                    customModelData));
        }
    }

    public List<String> getActiveItems() {
        return new ArrayList<>(activeItems);
    }

    public int getPlayerSold(UUID uuid, String itemKey) {
        return playerSales.computeIfAbsent(uuid, k -> new HashMap<>()).getOrDefault(itemKey, 0);
    }

    public void addPlayerSold(UUID uuid, String itemKey, int amount) {
        Map<String, Integer> data = playerSales.computeIfAbsent(uuid, k -> new HashMap<>());
        data.put(itemKey, data.getOrDefault(itemKey, 0) + amount);
    }

    public double getPrice(String key) {
        return plugin.getSellConfig().getDouble(currentRootSection + "." + key + ".price");
    }

    public int getLimit(String key) {
        int base = plugin.getSellConfig().getInt(currentRootSection + "." + key + ".daily-limit");
        int limit = (int) Math.floor(base * limitMultiplier);
        return base > 0 ? Math.max(1, limit) : 0;
    }

    public String getDisplayName(String key) {
        return DailySellShop.colorize(plugin.getSellConfig().getString(currentRootSection + "." + key + ".name", key));
    }

    public Material getMaterial(String key) {
        return ShopItemFactory.resolveMaterial(plugin.getSellConfig(), currentRootSection, key);
    }

    public ItemStack createItem(String key, int amount) {
        return ShopItemFactory.create(plugin.getSellConfig(), currentRootSection, key, amount);
    }

    public String getShopTitle() {
        String mode = refreshMode();
        if (mode.equals("HOURLY")) {
            return DailySellShop.colorize(plugin.getSellConfig().getString("menu.title-hourly",
                    plugin.getSellConfig().getString("messages.title-hourly", "限时收购")));
        }
        return DailySellShop.colorize(plugin.getSellConfig().getString("menu.title-daily",
                plugin.getSellConfig().getString("messages.title-daily", "每日收购")));
    }

    public int getMenuSize() {
        int size = plugin.getSellConfig().getInt("menu.size", 54);
        int rows = Math.max(1, Math.min(6, (int) Math.ceil(size / 9.0)));
        return rows * 9;
    }

    public List<Integer> getSellSlots() {
        List<Integer> slots = plugin.getSellConfig().getIntegerList("menu.sell-slots");
        if (!slots.isEmpty()) {
            return slots.stream()
                    .filter(slot -> slot >= 0 && slot < getMenuSize())
                    .toList();
        }

        List<Integer> defaults = new ArrayList<>();
        int size = getMenuSize();
        for (int slot = 0; slot < size; slot++) {
            defaults.add(slot);
        }
        return defaults;
    }

    public List<MenuButton> getMenuButtons() {
        return new ArrayList<>(menuButtons);
    }

    public MenuButton getMenuButton(String id) {
        for (MenuButton button : menuButtons) {
            if (button.getId().equals(id)) {
                return button;
            }
        }
        return null;
    }

    public String applyPlaceholders(String text, String playerName) {
        if (text == null) {
            return "";
        }
        return text
                .replace("{player}", playerName)
                .replace("%player%", playerName)
                .replace("%player_name%", playerName)
                .replace("{mode}", refreshMode());
    }

    private void reloadTimezone() {
        String configured = plugin.getSellConfig().getString("timezone", "Asia/Shanghai");
        try {
            timezone = ZoneId.of(configured == null ? "Asia/Shanghai" : configured);
        } catch (DateTimeException exception) {
            timezone = ZoneId.of("Asia/Shanghai");
            plugin.getLogger().warning("无效时区 '" + configured + "'，已回退为 Asia/Shanghai。");
        }
    }

    private String refreshMode() {
        return configuredRefreshMode;
    }

    private void reloadRefreshMode() {
        String configured = plugin.getSellConfig().getString("refresh-mode", "DAILY");
        String normalized = configured == null ? "DAILY" : configured.toUpperCase(Locale.ROOT);
        if (!normalized.equals("DAILY") && !normalized.equals("HOURLY")) {
            plugin.getLogger().warning("无效刷新模式 '" + configured + "'，已回退为 DAILY。");
            normalized = "DAILY";
        }
        configuredRefreshMode = normalized;
    }

    private double hourlyMultiplier() {
        double value = plugin.getSellConfig().getDouble("hourly-settings.multiplier", 0.5);
        if (!Double.isFinite(value) || value <= 0.0) {
            plugin.getLogger().warning("hourly-settings.multiplier 必须是正数，已回退为 0.5。");
            return 0.5;
        }
        return value;
    }
}
