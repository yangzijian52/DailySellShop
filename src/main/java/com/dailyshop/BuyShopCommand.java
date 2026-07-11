package com.dailyshop;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuyShopCommand implements CommandExecutor, TabCompleter {

    private final DailySellShop plugin;

    public BuyShopCommand(DailySellShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("只有玩家可以直接打开商店；控制台可使用 /dss open <玩家>。");
                return true;
            }
            if (!player.hasPermission("dailysellshop.shop.use")) {
                noPermission(player);
                return true;
            }
            BuyShopGui.open(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "open" -> openFor(sender, args);
            case "search" -> search(sender, args);
            case "help" -> help(sender);
            default -> help(sender);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("dailysellshop.shop.admin")) {
            noPermission(sender);
            return;
        }
        plugin.reloadAllConfigurations();
        plugin.getShopManager().forceUpdate();
        plugin.getBuyShopManager().reload();
        plugin.getPurchaseManager().cancelAll("&c配置已重载，当前自定义交易已取消。");
        sender.sendMessage(DailySellShop.colorize(plugin.getShopConfig().getString(
                "messages.reload-success", "&a购买商店与收购商店配置已重载。")));
    }

    private void openFor(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dailysellshop.shop.admin")) {
            noPermission(sender);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("用法: /dss open <玩家>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("找不到在线玩家: " + args[1]);
            return;
        }
        BuyShopGui.open(target);
        sender.sendMessage("已为 " + target.getName() + " 打开玩家商店。");
    }

    private void search(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以搜索商品。");
            return;
        }
        if (!player.hasPermission("dailysellshop.shop.use")) {
            noPermission(player);
            return;
        }
        if (args.length < 2) {
            player.sendMessage(DailySellShop.colorize("&e用法: /dss search <物品名称或 Material>"));
            return;
        }
        String query = String.join(" ", List.of(args).subList(1, args.length));
        BuyShopGui.openSearch(player, query, 0);
    }

    private void help(CommandSender sender) {
        sender.sendMessage(DailySellShop.colorize("&6DailySellShop 玩家商店命令"));
        sender.sendMessage(DailySellShop.colorize("&e/dss &7- 打开商店"));
        sender.sendMessage(DailySellShop.colorize("&e/dss search <物品> &7- 搜索商品"));
        if (sender.hasPermission("dailysellshop.shop.admin")) {
            sender.sendMessage(DailySellShop.colorize("&e/dss open <玩家> &7- 为玩家打开商店"));
            sender.sendMessage(DailySellShop.colorize("&e/dss reload &7- 重载全部配置"));
        }
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(DailySellShop.colorize(plugin.getShopConfig().getString(
                "messages.no-permission", "&c你没有权限执行此操作。")));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("search", "help"));
            if (sender.hasPermission("dailysellshop.shop.admin")) {
                options.add("open");
                options.add("reload");
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")
                && sender.hasPermission("dailysellshop.shop.admin")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).toList();
    }
}
