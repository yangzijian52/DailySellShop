package com.dailyshop;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ShopListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        DailySellShop plugin = DailySellShop.getInstance();

        String dailyTitle = DailySellShop.colorize(plugin.getConfig().getString("messages.title-daily", ""));
        String hourlyTitle = DailySellShop.colorize(plugin.getConfig().getString("messages.title-hourly", ""));

        if (!title.equals(dailyTitle) && !title.equals(hourlyTitle)) {
            return;
        }

        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String key = event.getCurrentItem().getType().name();

        if (!plugin.getShopManager().getActiveItems().contains(key)) {
            return;
        }

        executeSell(player, key);
        ShopGui.open(player);
    }

    public static void executeSell(Player player, String itemKey) {
        DailySellShop plugin = DailySellShop.getInstance();
        ShopManager manager = plugin.getShopManager();
        Material mat = Material.getMaterial(itemKey);

        if (mat == null) {
            return;
        }

        int limit = manager.getLimit(itemKey);
        int sold = manager.getPlayerSold(player.getUniqueId(), itemKey);

        if (sold >= limit) {
            String msg = DailySellShop.colorize(plugin.getConfig().getString("messages.limit-reached"));
            player.sendMessage(DailySellShop.colorize(plugin.getConfig().getString("messages.prefix")) + msg);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
            return;
        }

        int playerHas = 0;
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == mat) {
                playerHas += itemStack.getAmount();
            }
        }

        if (playerHas <= 0) {
            String msg = DailySellShop.colorize(plugin.getConfig().getString("messages.no-item"));
            player.sendMessage(DailySellShop.colorize(plugin.getConfig().getString("messages.prefix")) + msg);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
            return;
        }

        int canSell = limit - sold;
        int finalAmount = Math.min(playerHas, canSell);

        ItemStack toRemove = new ItemStack(mat, finalAmount);
        if (!player.getInventory().removeItem(toRemove).isEmpty()) {
            player.sendMessage("§c交易失败。");
            return;
        }

        double price = manager.getPrice(itemKey);
        double totalMoney = price * finalAmount;
        DailySellShop.getEconomy().depositPlayer(player, totalMoney);

        manager.addPlayerSold(player.getUniqueId(), itemKey, finalAmount);

        String msg = DailySellShop.colorize(plugin.getConfig().getString("messages.sold"))
                .replace("%amount%", String.valueOf(finalAmount))
                .replace("%item%", manager.getDisplayName(itemKey))
                .replace("%money%", String.format("%.2f", totalMoney))
                .replace("%limit%", String.valueOf(limit - (sold + finalAmount)));

        player.sendMessage(DailySellShop.colorize(plugin.getConfig().getString("messages.prefix")) + msg);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }
}
