package com.dailyshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

public class ShopListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        DailySellShop plugin = DailySellShop.getInstance();

        String dailyTitle = DailySellShop.colorize(plugin.getSellConfig().getString("messages.title-daily", ""));
        String hourlyTitle = DailySellShop.colorize(plugin.getSellConfig().getString("messages.title-hourly", ""));

        if (!ShopGui.isShopOpen(player) && !title.equals(dailyTitle) && !title.equals(hourlyTitle)) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = event.getCurrentItem().getItemMeta();
        if (meta == null) {
            return;
        }

        String action = meta.getPersistentDataContainer().get(
                ShopGui.key(plugin, "action"), PersistentDataType.STRING);
        if (action == null) {
            return;
        }

        if (action.equals(ShopGui.ACTION_SELL)) {
            String key = meta.getPersistentDataContainer().get(
                    ShopGui.key(plugin, "item"), PersistentDataType.STRING);
            if (key == null || !plugin.getShopManager().getActiveItems().contains(key)) {
                return;
            }
            executeSell(player, key);
            ShopGui.open(player);
        } else if (action.equals(ShopGui.ACTION_COMMAND)) {
            String buttonId = meta.getPersistentDataContainer().get(
                    ShopGui.key(plugin, "button"), PersistentDataType.STRING);
            executeButton(player, buttonId);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            ShopGui.unregisterShop(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && ShopGui.isShopOpen(player)) {
            event.setCancelled(true);
        }
    }

    public static void executeButton(Player player, String buttonId) {
        if (buttonId == null || buttonId.isBlank()) {
            return;
        }

        DailySellShop plugin = DailySellShop.getInstance();
        MenuButton button = plugin.getShopManager().getMenuButton(buttonId);
        if (button == null) {
            return;
        }

        if (button.hasPermission() && !player.hasPermission(button.getPermission())) {
            player.sendMessage(DailySellShop.colorize(plugin.getSellConfig().getString(
                    "messages.no-permission", "&c你没有权限使用这个按钮。")));
            return;
        }

        for (String rawCommand : button.getCommands()) {
            executeAction(player, rawCommand);
        }
    }

    public static void executeSell(Player player, String itemKey) {
        DailySellShop plugin = DailySellShop.getInstance();
        ShopManager manager = plugin.getShopManager();
        ItemStack template = manager.createItem(itemKey, 1);

        if (!plugin.hasEconomy()) {
            player.sendMessage(DailySellShop.colorize(plugin.getSellConfig().getString("messages.prefix", ""))
                    + "§c经济系统当前不可用，暂时无法出售物品。");
            return;
        }

        if (template == null) {
            return;
        }

        int limit = manager.getLimit(itemKey);
        int sold = manager.getPlayerSold(player.getUniqueId(), itemKey);

        if (sold >= limit) {
            String msg = DailySellShop.colorize(plugin.getSellConfig().getString("messages.limit-reached"));
            player.sendMessage(DailySellShop.colorize(plugin.getSellConfig().getString("messages.prefix")) + msg);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
            return;
        }

        int playerHas = 0;
        for (ItemStack itemStack : player.getInventory().getStorageContents()) {
            if (itemStack != null && itemStack.isSimilar(template)) {
                playerHas += itemStack.getAmount();
            }
        }

        if (playerHas <= 0) {
            String msg = DailySellShop.colorize(plugin.getSellConfig().getString("messages.no-item"));
            player.sendMessage(DailySellShop.colorize(plugin.getSellConfig().getString("messages.prefix")) + msg);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
            return;
        }

        int canSell = limit - sold;
        int finalAmount = Math.min(playerHas, canSell);
        double configuredPrice = manager.getPrice(itemKey);
        if (!Double.isFinite(configuredPrice) || configuredPrice < 0) {
            plugin.getLogger().warning("拒绝使用无效收购价交易: " + itemKey + " -> " + configuredPrice);
            player.sendMessage(DailySellShop.colorize(plugin.getSellConfig().getString("messages.prefix", ""))
                    + "§c该商品价格配置无效，请联系管理员。");
            return;
        }
        BigDecimal totalMoney = BigDecimal.valueOf(configuredPrice)
                .multiply(BigDecimal.valueOf(finalAmount))
                .setScale(2, RoundingMode.HALF_UP);
        ItemStack[] snapshot = cloneStorage(player.getInventory().getStorageContents());

        ItemStack toRemove = template.clone();
        toRemove.setAmount(finalAmount);
        Map<Integer, ItemStack> leftovers = player.getInventory().removeItem(toRemove);
        if (!leftovers.isEmpty()) {
            player.getInventory().setStorageContents(snapshot);
            player.sendMessage("§c交易失败。");
            return;
        }

        EconomyService.TransactionResult deposit = DailySellShop.getEconomy().deposit(player, totalMoney.doubleValue());
        if (!deposit.success()) {
            player.getInventory().setStorageContents(snapshot);
            String error = deposit.errorMessage() == null || deposit.errorMessage().isBlank()
                    ? "未知原因"
                    : deposit.errorMessage();
            String message = DailySellShop.colorize(plugin.getSellConfig().getString(
                    "messages.deposit-failed", "&c经济入账失败，物品已退回：%error%"))
                    .replace("%error%", error);
            player.sendMessage(DailySellShop.colorize(plugin.getSellConfig().getString("messages.prefix", ""))
                    + message);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            return;
        }

        manager.addPlayerSold(player.getUniqueId(), itemKey, finalAmount);

        String msg = DailySellShop.colorize(plugin.getSellConfig().getString("messages.sold"))
                .replace("%amount%", String.valueOf(finalAmount))
                .replace("%item%", manager.getDisplayName(itemKey))
                .replace("%money%", totalMoney.toPlainString())
                .replace("%limit%", String.valueOf(limit - (sold + finalAmount)));

        player.sendMessage(DailySellShop.colorize(plugin.getSellConfig().getString("messages.prefix")) + msg);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    private static ItemStack[] cloneStorage(ItemStack[] contents) {
        ItemStack[] snapshot = new ItemStack[contents.length];
        for (int index = 0; index < contents.length; index++) {
            snapshot[index] = contents[index] == null ? null : contents[index].clone();
        }
        return snapshot;
    }

    public static void executeAction(Player player, String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return;
        }

        DailySellShop plugin = DailySellShop.getInstance();
        String command = plugin.getShopManager().applyPlaceholders(rawCommand.trim(), player.getName());
        String lower = command.toLowerCase(Locale.ROOT);

        if (lower.startsWith("[command]")) {
            player.performCommand(stripAction(command));
        } else if (lower.startsWith("[console]")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripAction(command));
        } else if (lower.startsWith("[op]")) {
            runAsOp(player, stripAction(command));
        } else if (lower.startsWith("[message]")) {
            player.sendMessage(DailySellShop.colorize(stripAction(command)));
        } else if (lower.startsWith("[close]")) {
            player.closeInventory();
        } else if (lower.startsWith("[sound]")) {
            playSound(player, stripAction(command));
        } else {
            player.performCommand(command.startsWith("/") ? command.substring(1) : command);
        }
    }

    private static String stripAction(String command) {
        int end = command.indexOf(']');
        if (end < 0) {
            return command.startsWith("/") ? command.substring(1) : command;
        }
        String value = command.substring(end + 1).trim();
        return value.startsWith("/") ? value.substring(1) : value;
    }

    private static void runAsOp(Player player, String command) {
        boolean wasOp = player.isOp();
        try {
            if (!wasOp) {
                player.setOp(true);
            }
            player.performCommand(command.startsWith("/") ? command.substring(1) : command);
        } finally {
            if (!wasOp) {
                player.setOp(false);
            }
        }
    }

    private static void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isBlank()) {
            return;
        }
        Sound sound = SoundResolver.resolve(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } else {
            DailySellShop.getInstance().getLogger().warning("无效菜单音效: " + soundName);
        }
    }
}
