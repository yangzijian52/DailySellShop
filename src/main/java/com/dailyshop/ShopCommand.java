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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以使用此命令。");
            return true;
        }

        DailySellShop plugin = DailySellShop.getInstance();

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("dailysell.admin")) {
                plugin.reloadConfig();
                plugin.getShopManager().forceUpdate();

                String mode = plugin.getConfig().getString("refresh-mode", "DAILY");
                String msg = DailySellShop.colorize(plugin.getConfig().getString("messages.config-reloaded"))
                        .replace("%mode%", mode);
                player.sendMessage(msg);
                return true;
            }
            return true;
        }

        Bukkit.getScheduler().runTask(plugin, () -> ShopGui.open(player));
        return true;
    }
}
