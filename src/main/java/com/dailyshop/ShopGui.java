package com.dailyshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

public class ShopGui {

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
        ShopManager manager = DailySellShop.getInstance().getShopManager();
        List<String> items = manager.getActiveItems();

        int rows = (int) Math.ceil(items.size() / 9.0);
        rows = Math.max(1, Math.min(6, rows));

        Inventory inv = Bukkit.createInventory(null, rows * 9, manager.getShopTitle());

        for (String key : items) {
            Material mat = Material.getMaterial(key);
            if (mat == null) {
                continue;
            }

            int sold = manager.getPlayerSold(player.getUniqueId(), key);
            int limit = manager.getLimit(key);
            double price = manager.getPrice(key);

            ItemStack icon = new ItemStack(mat);
            ItemMeta meta = icon.getItemMeta();
            if (meta == null) {
                continue;
            }

            meta.setDisplayName(manager.getDisplayName(key));

            List<String> lore = new ArrayList<>();
            lore.add("§7单价: §a$" + price);
            lore.add("§7当前进度: §e" + sold + " / " + limit);
            lore.add("");
            if (sold >= limit) {
                lore.add("§c本轮额度已满");
            } else {
                lore.add("§e点击出售背包内该物品");
            }
            meta.setLore(lore);
            icon.setItemMeta(meta);

            inv.addItem(icon);
        }

        player.openInventory(inv);
    }

    private static void openBedrockForm(Player player) {
        ShopManager manager = DailySellShop.getInstance().getShopManager();
        String cleanTitle = manager.getShopTitle().replaceAll("§.", "");

        SimpleForm.Builder builder = SimpleForm.builder().title(cleanTitle);
        List<String> buttonKeyMapping = new ArrayList<>();

        for (String key : manager.getActiveItems()) {
            int sold = manager.getPlayerSold(player.getUniqueId(), key);
            int limit = manager.getLimit(key);
            double price = manager.getPrice(key);
            String name = manager.getDisplayName(key);

            String buttonText = name + "\n§r§8[$" + price + "] " + sold + "/" + limit;
            builder.button(buttonText);
            buttonKeyMapping.add(key);
        }

        builder.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < buttonKeyMapping.size()) {
                String key = buttonKeyMapping.get(index);
                ShopListener.executeSell(player, key);
                openBedrockForm(player);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }
}
