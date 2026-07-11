package com.dailyshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ShopGui {

    public static final String ACTION_SELL = "sell";
    public static final String ACTION_COMMAND = "command";
    private static final Set<UUID> OPEN_SHOP_VIEWERS = new HashSet<>();

    public static void open(Player player) {
        boolean isBedrock = false;
        if (Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            try {
                isBedrock = FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
            } catch (Exception ignored) {
            }
        }

        if (isBedrock) {
            openBedrockForm(player);
        } else {
            openJavaInventory(player);
        }
    }

    private static void openJavaInventory(Player player) {
        DailySellShop plugin = DailySellShop.getInstance();
        ShopManager manager = DailySellShop.getInstance().getShopManager();
        List<String> items = manager.getActiveItems();
        List<Integer> sellSlots = manager.getSellSlots();

        Inventory inv = Bukkit.createInventory(null, manager.getMenuSize(), manager.getShopTitle());

        for (MenuButton button : manager.getMenuButtons()) {
            if (button.hasPermission() && !player.hasPermission(button.getPermission())) {
                continue;
            }
            inv.setItem(button.getSlot(), createButtonItem(plugin, player, manager, button));
        }

        int index = 0;
        for (String key : items) {
            if (index >= sellSlots.size()) {
                break;
            }
            ItemStack icon = manager.createItem(key, 1);
            if (icon == null) {
                continue;
            }
            int slot = sellSlots.get(index++);

            int sold = manager.getPlayerSold(player.getUniqueId(), key);
            int limit = manager.getLimit(key);
            double price = manager.getPrice(key);

            ItemMeta meta = icon.getItemMeta();
            if (meta == null) {
                continue;
            }

            meta.setDisplayName(manager.getDisplayName(key));

            List<String> lore = new ArrayList<>();
            List<String> configuredLore = plugin.getSellConfig().getStringList("menu.sell-item.lore");
            if (configuredLore.isEmpty()) {
                configuredLore = List.of(
                        "&7单价: &a${price}",
                        "&7当前进度: &e{sold} / {limit}",
                        "",
                        "&e点击出售背包内该物品");
            }
            int remaining = Math.max(0, limit - sold);
            for (String line : configuredLore) {
                lore.add(DailySellShop.colorize(manager.applyPlaceholders(line, player.getName())
                        .replace("{item}", manager.getDisplayName(key))
                        .replace("{price}", formatMoney(price))
                        .replace("{sold}", String.valueOf(sold))
                        .replace("{limit}", String.valueOf(limit))
                        .replace("{remaining}", String.valueOf(remaining))));
            }
            if (sold >= limit && plugin.getSellConfig().getBoolean("menu.sell-item.append-sold-out-line", true)) {
                lore.add(DailySellShop.colorize(plugin.getSellConfig().getString("menu.sell-item.sold-out-line", "&c本轮额度已满")));
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(key(plugin, "action"), PersistentDataType.STRING, ACTION_SELL);
            meta.getPersistentDataContainer().set(key(plugin, "item"), PersistentDataType.STRING, key);
            icon.setItemMeta(meta);

            inv.setItem(slot, icon);
        }

        player.openInventory(inv);
        registerShop(player);
    }

    private static void openBedrockForm(Player player) {
        DailySellShop plugin = DailySellShop.getInstance();
        ShopManager manager = DailySellShop.getInstance().getShopManager();
        String cleanTitle = manager.getShopTitle().replaceAll("§.", "");

        SimpleForm.Builder builder = SimpleForm.builder().title(cleanTitle);
        List<String> buttonKeyMapping = new ArrayList<>();

        for (String key : manager.getActiveItems()) {
            int sold = manager.getPlayerSold(player.getUniqueId(), key);
            int limit = manager.getLimit(key);
            double price = manager.getPrice(key);
            String name = manager.getDisplayName(key);

            String buttonText = stripColor(name) + "\n$" + formatMoney(price) + " | " + sold + "/" + limit;
            builder.button(buttonText);
            buttonKeyMapping.add(ACTION_SELL + ":" + key);
        }

        for (MenuButton button : manager.getMenuButtons()) {
            if (button.getCommands().isEmpty()) {
                continue;
            }
            if (button.hasPermission() && !player.hasPermission(button.getPermission())) {
                continue;
            }
            builder.button(stripColor(DailySellShop.colorize(manager.applyPlaceholders(button.getName(), player.getName()))));
            buttonKeyMapping.add(ACTION_COMMAND + ":" + button.getId());
        }

        builder.validResultHandler(response -> Bukkit.getScheduler().runTask(plugin, () -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < buttonKeyMapping.size()) {
                String action = buttonKeyMapping.get(index);
                if (action.startsWith(ACTION_SELL + ":")) {
                    String key = action.substring((ACTION_SELL + ":").length());
                    ShopListener.executeSell(player, key);
                    openBedrockForm(player);
                } else if (action.startsWith(ACTION_COMMAND + ":")) {
                    String buttonId = action.substring((ACTION_COMMAND + ":").length());
                    ShopListener.executeButton(player, buttonId);
                }
            }
        }));

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }

    private static ItemStack createButtonItem(DailySellShop plugin, Player player, ShopManager manager, MenuButton button) {
        Material material = Material.getMaterial(button.getMaterial().toUpperCase(Locale.ROOT));
        if (material == null) {
            plugin.getLogger().warning("菜单按钮 " + button.getId() + " 使用了无效材质: " + button.getMaterial());
            material = Material.STONE;
        }

        ItemStack itemStack = new ItemStack(material, button.getAmount());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        meta.setDisplayName(DailySellShop.colorize(manager.applyPlaceholders(button.getName(), player.getName())));
        if (!button.getLore().isEmpty()) {
            List<String> lore = new ArrayList<>();
            for (String line : button.getLore()) {
                lore.add(DailySellShop.colorize(manager.applyPlaceholders(line, player.getName())));
            }
            meta.setLore(lore);
        }

        if (button.getCustomModelData() != null) {
            meta.setCustomModelData(button.getCustomModelData());
        }
        if (button.isGlow()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        if (button.isHideFlags()) {
            meta.addItemFlags(ItemFlag.values());
        }
        meta.getPersistentDataContainer().set(key(plugin, "action"), PersistentDataType.STRING, ACTION_COMMAND);
        meta.getPersistentDataContainer().set(key(plugin, "button"), PersistentDataType.STRING, button.getId());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static boolean isShopOpen(Player player) {
        return OPEN_SHOP_VIEWERS.contains(player.getUniqueId());
    }

    public static void registerShop(Player player) {
        OPEN_SHOP_VIEWERS.add(player.getUniqueId());
    }

    public static void unregisterShop(Player player) {
        OPEN_SHOP_VIEWERS.remove(player.getUniqueId());
    }

    public static NamespacedKey key(DailySellShop plugin, String key) {
        return new NamespacedKey(plugin, key);
    }

    private static String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§.", "").replaceAll("(?i)&[0-9a-fk-orx]", "");
    }
}
