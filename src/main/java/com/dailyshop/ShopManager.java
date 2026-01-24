package com.dailyshop;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {
    private final DailySellShop plugin;

    // 存储当前生效的物品Key
    private final List<String> activeItems = new ArrayList<>();

    // 记录当前读取的是哪个配置节点 ("items" 还是 "static-items")
    private String currentRootSection = "items";

    // 记录玩家出售数据: UUID -> (ItemKey -> 已售数量)
    private final Map<UUID, Map<String, Integer>> playerDailySales = new ConcurrentHashMap<>();

    private int lastDayOfYear = -1;

    public ShopManager(DailySellShop plugin) {
        this.plugin = plugin;
        rotateDailyItems();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkTimeReset, 20L, 1200L);
    }

    private void checkTimeReset() {
        ZoneId zone = ZoneId.of(plugin.getConfig().getString("timezone", "Asia/Shanghai"));
        ZonedDateTime now = ZonedDateTime.now(zone);
        int currentDay = now.getDayOfYear();

        if (lastDayOfYear != -1 && currentDay != lastDayOfYear) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("北京时间 00:00 已到，刷新商店！");
                rotateDailyItems();
                playerDailySales.clear();
            });
        }
        lastDayOfYear = currentDay;
    }

    // --- 核心修改：区分随机模式和固定模式 ---
    public void rotateDailyItems() {
        activeItems.clear();

        boolean isRandomEnabled = plugin.getConfig().getBoolean("daily-rotation.enabled", true);

        if (isRandomEnabled) {
            // === 模式 A: 每日随机 ===
            currentRootSection = "items"; // 标记读取源为 items
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("items");

            if (section != null) {
                List<String> allKeys = new ArrayList<>(section.getKeys(false));
                int amount = plugin.getConfig().getInt("daily-rotation.amount", 10);

                Collections.shuffle(allKeys); // 洗牌
                // 取前N个
                activeItems.addAll(allKeys.subList(0, Math.min(amount, allKeys.size())));
            }
        } else {
            // === 模式 B: 固定列表 ===
            currentRootSection = "static-items"; // 标记读取源为 static-items
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("static-items");

            if (section != null) {
                // 直接添加所有 key，不随机，不打乱
                activeItems.addAll(section.getKeys(false));
            }
        }

        plugin.getLogger().info("商店已刷新，当前模式: " + (isRandomEnabled ? "随机池" : "固定列表") +
                ", 物品数量: " + activeItems.size());
    }

    public List<String> getActiveItems() {
        return activeItems;
    }

    public int getPlayerSold(UUID playerUUID, String itemKey) {
        return playerDailySales.computeIfAbsent(playerUUID, k -> new HashMap<>())
                .getOrDefault(itemKey, 0);
    }

    public void addPlayerSold(UUID playerUUID, String itemKey, int amount) {
        Map<String, Integer> sales = playerDailySales.computeIfAbsent(playerUUID, k -> new HashMap<>());
        sales.put(itemKey, sales.getOrDefault(itemKey, 0) + amount);
    }

    // --- 读取数据方法的修改 ---
    // 现在的路径是动态的：currentRootSection + "." + key + ".price"

    public double getPrice(String key) {
        return plugin.getConfig().getDouble(currentRootSection + "." + key + ".price");
    }

    public int getLimit(String key) {
        return plugin.getConfig().getInt(currentRootSection + "." + key + ".daily-limit");
    }

    public String getDisplayName(String key) {
        return plugin.getConfig().getString(currentRootSection + "." + key + ".name", key).replace("&", "§");
    }
}
