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

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("dailysell.admin")) {
                DailySellShop.getInstance().reloadConfig();
                DailySellShop.getInstance().getShopManager().rotateDailyItems(); // 重新加载随机池
                player.sendMessage("§a配置已重载，物品池已刷新。");
                return true;
            }
        }

        // 打开菜单
        ShopGui.open(player);
        return true;
    }
}
