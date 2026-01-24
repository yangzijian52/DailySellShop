package com.dailyshop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用此命令！");
            return true;
        }

        Player player = (Player) sender;
        DailySellShop plugin = DailySellShop.getInstance();

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("dailysell.admin")) {
                plugin.reloadConfig();

                // 核心修改：调用 forceUpdate 重新读取模式和倍率
                plugin.getShopManager().forceUpdate();

                String mode = plugin.getConfig().getString("refresh-mode");
                String msg = plugin.getConfig().getString("messages.config-reloaded")
                        .replace("&", "§")
                        .replace("%mode%", mode);
                player.sendMessage(msg);
                return true;
            }
        }

        ShopGui.open(player);
        return true;
    }
}
