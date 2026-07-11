package com.dailyshop;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        DailySellShop plugin = DailySellShop.getInstance();

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("dailysell.admin")) {
                plugin.reloadAllConfigurations();
                plugin.getShopManager().forceUpdate();
                plugin.getBuyShopManager().reload();
                plugin.getPurchaseManager().cancelAll("&c配置已重载，当前自定义交易已取消。");

                String mode = plugin.getSellConfig().getString("refresh-mode", "DAILY");
                String msg = DailySellShop.colorize(plugin.getSellConfig().getString("messages.config-reloaded"))
                        .replace("%mode%", mode);
                sender.sendMessage(msg);
                return true;
            }
            sender.sendMessage(DailySellShop.colorize(plugin.getSellConfig().getString(
                    "messages.no-permission", "&c你没有权限使用此命令。")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以使用此命令。");
            return true;
        }

        Bukkit.getScheduler().runTask(plugin, () -> ShopGui.open(player));
        return true;
    }
}
