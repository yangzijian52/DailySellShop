package com.dailyshop;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {

    private final DailySellShop plugin;
    private final List<String> activeItems = new ArrayList<>();
    private final Map<UUID, Map<String, Integer>> playerSales = new ConcurrentHashMap<>();
    private String currentRootSection = "items";
    private double limitMultiplier = 1.0;
    private int lastDayOfYear = -1;
    private int lastHourOfDay = -1;

    public ShopManager(DailySellShop plugin) {
        this.plugin = plugin;

        ZonedDateTime now = getNow();
        lastDayOfYear = now.getDayOfYear();
        lastHourOfDay = now.getHour();

        forceUpdate();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkTimeReset, 20L, 400L);
    }

    private ZonedDateTime getNow() {
        return ZonedDateTime.now(ZoneId.of(plugin.getConfig().getString("timezone", "Asia/Shanghai")));
    }

    public void forceUpdate() {
        String mode = plugin.getConfig().getString("refresh-mode", "DAILY").toUpperCase();
        if (mode.equals("HOURLY")) {
            limitMultiplier = plugin.getConfig().getDouble("hourly-settings.multiplier", 0.5);
        } else {
            limitMultiplier = 1.0;
        }
        rotateItems();
    }

    private void checkTimeReset() {
        ZonedDateTime now = getNow();
        int currentDay = now.getDayOfYear();
        int currentHour = now.getHour();

        String mode = plugin.getConfig().getString("refresh-mode", "DAILY").toUpperCase();
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
                limitMultiplier = plugin.getConfig().getDouble("hourly-settings.multiplier", 0.5);
                shouldReset = true;
            }
        }

        if (shouldReset) {
            lastDayOfYear = currentDay;
            lastHourOfDay = currentHour;

            Bukkit.getScheduler().runTask(plugin, () -> {
                playerSales.clear();
                rotateItems();
            });
        }
    }

    public void rotateItems() {
        activeItems.clear();
        boolean isRandom = plugin.getConfig().getBoolean("random-rotation.enabled", true);

        if (isRandom) {
            currentRootSection = "items";
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("items");
            if (section != null) {
                List<String> allKeys = new ArrayList<>(section.getKeys(false));
                int amount = plugin.getConfig().getInt("random-rotation.amount", 10);
                Collections.shuffle(allKeys);
                activeItems.addAll(allKeys.subList(0, Math.min(amount, allKeys.size())));
            }
        } else {
            currentRootSection = "static-items";
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("static-items");
            if (section != null) {
                activeItems.addAll(section.getKeys(false));
            }
        }
    }

    public List<String> getActiveItems() {
        return activeItems;
    }

    public int getPlayerSold(UUID uuid, String itemKey) {
        return playerSales.computeIfAbsent(uuid, k -> new HashMap<>()).getOrDefault(itemKey, 0);
    }

    public void addPlayerSold(UUID uuid, String itemKey, int amount) {
        Map<String, Integer> data = playerSales.computeIfAbsent(uuid, k -> new HashMap<>());
        data.put(itemKey, data.getOrDefault(itemKey, 0) + amount);
    }

    public double getPrice(String key) {
        return plugin.getConfig().getDouble(currentRootSection + "." + key + ".price");
    }

    public int getLimit(String key) {
        int base = plugin.getConfig().getInt(currentRootSection + "." + key + ".daily-limit");
        return (int) (base * limitMultiplier);
    }

    public String getDisplayName(String key) {
        return DailySellShop.colorize(plugin.getConfig().getString(currentRootSection + "." + key + ".name", key));
    }

    public String getShopTitle() {
        String mode = plugin.getConfig().getString("refresh-mode", "DAILY").toUpperCase();
        if (mode.equals("HOURLY")) {
            return DailySellShop.colorize(plugin.getConfig().getString("messages.title-hourly", "限时收购"));
        }
        return DailySellShop.colorize(plugin.getConfig().getString("messages.title-daily", "每日收购"));
    }
}
