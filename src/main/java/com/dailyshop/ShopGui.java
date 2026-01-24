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
        // 检查是否安装了 Floodgate 并且玩家是基岩版玩家
        boolean isBedrock = false;
        if (Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            try {
                isBedrock = FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
            } catch (Exception ignored) {}
        }

        if (isBedrock) {
            openBedrockForm(player);
        } else {
            openJavaInventory(player);
        }
    }

    // --- Java版：箱子界面 ---
    private static void openJavaInventory(Player player) {
        ShopManager manager = DailySellShop.getInstance().getShopManager();
        List<String> items = manager.getActiveItems();

        // 自动计算箱子行数
        int rows = (int) Math.ceil(items.size() / 9.0);
        rows = Math.max(1, Math.min(6, rows)); // 1到6行

        Inventory inv = Bukkit.createInventory(null, rows * 9, "§0今日收购 (北京时间刷新)");

        for (String key : items) {
            Material mat = Material.getMaterial(key);
            if (mat == null) continue;

            int sold = manager.getPlayerSold(player.getUniqueId(), key);
            int limit = manager.getLimit(key);
            double price = manager.getPrice(key);

            ItemStack icon = new ItemStack(mat);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(manager.getDisplayName(key));

            List<String> lore = new ArrayList<>();
            lore.add("§7单价: §a$" + price);
            lore.add("§7今日进度: §e" + sold + " / " + limit);
            lore.add("");

            if (sold >= limit) {
                lore.add("§c今日额度已满");
            } else {
                lore.add("§e点击出售背包内该物品");
            }
            meta.setLore(lore);
            icon.setItemMeta(meta);

            inv.addItem(icon);
        }
        player.openInventory(inv);
    }

    // --- 基岩版：Form表单 ---
    private static void openBedrockForm(Player player) {
        ShopManager manager = DailySellShop.getInstance().getShopManager();

        SimpleForm.Builder builder = SimpleForm.builder()
                .title("收购商店")
                .content("北京时间每日0点刷新随机收购列表");

        // 这里的列表用于记录按钮ID对应的物品Key
        List<String> buttonKeyMapping = new ArrayList<>();

        for (String key : manager.getActiveItems()) {
            int sold = manager.getPlayerSold(player.getUniqueId(), key);
            int limit = manager.getLimit(key);
            double price = manager.getPrice(key);
            String name = manager.getDisplayName(key);

            // 按钮文字：显示名称、价格和进度
            // 例如: 钻石 [$100] (0/64)
            String buttonText = name + "\n§r§8[$" + price + "] " + sold + "/" + limit;

            builder.button(buttonText);
            buttonKeyMapping.add(key);
        }

        builder.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < buttonKeyMapping.size()) {
                String key = buttonKeyMapping.get(index);

                // 执行出售逻辑
                ShopListener.executeSell(player, key);

                // 交易完成后重新打开界面，刷新数据
                openBedrockForm(player);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }
}
