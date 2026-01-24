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
        // 检查标题是否匹配
        if (!event.getView().getTitle().equals("§0今日收购 (北京时间刷新)")) return;

        event.setCancelled(true); // 禁止拿取物品

        if (event.getCurrentItem() == null) return;

        // 只有点击的是有效物品才处理 (防止点击空白处报错)
        Material mat = event.getCurrentItem().getType();
        if (mat == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();

        // 假设配置文件里的 key 就是材质名 (如 DIAMOND)
        String key = mat.name();

        // 检查这个物品是否在今日列表里
        if (!DailySellShop.getInstance().getShopManager().getActiveItems().contains(key)) return;

        executeSell(player, key);

        // 刷新界面显示最新的进度
        ShopGui.open(player);
    }

    /**
     * 核心出售逻辑 (静态方法，供Java版事件和基岩版表单共同调用)
     */
    public static void executeSell(Player player, String itemKey) {
        DailySellShop plugin = DailySellShop.getInstance();
        ShopManager manager = plugin.getShopManager();
        Material mat = Material.getMaterial(itemKey);

        if (mat == null) return;

        // 1. 获取限制数据
        int limit = manager.getLimit(itemKey);
        int sold = manager.getPlayerSold(player.getUniqueId(), itemKey);

        if (sold >= limit) {
            player.sendMessage("§c今日该物品出售额度已用完！");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        // 2. 统计玩家背包里有多少个该物品
        int playerHas = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat) {
                playerHas += is.getAmount();
            }
        }

        if (playerHas <= 0) {
            player.sendMessage("§c你背包里没有 " + manager.getDisplayName(itemKey) + "！");
            return;
        }

        // 3. 计算实际能卖多少个
        int canSell = limit - sold;
        int finalAmount = Math.min(playerHas, canSell); // 取 背包数量 和 剩余额度 的较小值

        // 4. 执行扣除 (原子操作模拟)
        ItemStack toRemove = new ItemStack(mat, finalAmount);

        // removeItem 返回未能移除的物品Map。如果不为空，说明移除失败
        if (!player.getInventory().removeItem(toRemove).isEmpty()) {
            player.sendMessage("§c系统错误：扣除物品失败，交易取消。");
            return;
        }

        // 5. 给钱
        double price = manager.getPrice(itemKey);
        double totalMoney = price * finalAmount;
        DailySellShop.getEconomy().depositPlayer(player, totalMoney);

        // 6. 更新记录
        manager.addPlayerSold(player.getUniqueId(), itemKey, finalAmount);

        // 7. 提示信息
        player.sendMessage(String.format("§a成功出售 %d 个 %s，获得 $%s (今日剩余: %d)",
                finalAmount,
                manager.getDisplayName(itemKey),
                String.format("%.2f", totalMoney),
                (limit - (sold + finalAmount))
        ));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }
}
