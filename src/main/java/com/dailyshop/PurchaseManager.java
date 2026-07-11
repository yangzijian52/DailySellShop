package com.dailyshop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class PurchaseManager {

    private final DailySellShop plugin;
    private final Map<UUID, PendingInput> pendingInputs = new HashMap<>();
    private final Set<UUID> processing = new HashSet<>();
    private final Object logLock = new Object();
    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "DailySellShop-PurchaseLog");
        thread.setDaemon(true);
        return thread;
    });

    public PurchaseManager(DailySellShop plugin) {
        this.plugin = plugin;
    }

    public void startCustomInput(Player player, String productKey, boolean bedrock) {
        BuyProduct product = plugin.getBuyShopManager().getProduct(productKey);
        if (product == null || !plugin.getBuyShopManager().canView(player, product)) {
            send(player, "messages.product-unavailable", "&c该商品已下架或你没有购买权限。");
            return;
        }

        cancel(player, null);
        int timeoutSeconds = Math.max(1, plugin.getShopConfig().getInt("settings.custom-input-timeout-seconds", 60));
        PendingInput pending = new PendingInput(product.key(), player.getLocation().clone(),
                System.currentTimeMillis() + timeoutSeconds * 1000L, bedrock);
        pending.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingInput current = pendingInputs.get(player.getUniqueId());
            if (current == pending) {
                pendingInputs.remove(player.getUniqueId());
                send(player, "messages.custom-timeout", "&c输入超时，交易已取消。");
            }
        }, timeoutSeconds * 20L);
        pendingInputs.put(player.getUniqueId(), pending);

        if (bedrock) {
            BuyShopGui.openBedrockCustomInput(player, product, false);
        } else {
            player.closeInventory();
            send(player, "messages.custom-prompt",
                    "&e请在 {seconds} 秒内输入购买数量，输入 cancel 或 取消可取消交易。",
                    "{seconds}", String.valueOf(timeoutSeconds));
        }
    }

    public boolean hasPendingInput(Player player) {
        return pendingInputs.containsKey(player.getUniqueId());
    }

    public void handleCustomInput(Player player, String rawInput) {
        PendingInput pending = pendingInputs.get(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (System.currentTimeMillis() > pending.expiresAt) {
            cancel(player, "&c输入超时，交易已取消。");
            return;
        }

        String input = rawInput == null ? "" : rawInput.trim();
        if (input.equalsIgnoreCase("cancel") || input.equals("取消")) {
            cancel(player, "&c交易已取消。");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            invalidInput(player, pending);
            return;
        }

        BuyProduct product = plugin.getBuyShopManager().getProduct(pending.productKey);
        int max = plugin.getBuyShopManager().getMaxAmount(product);
        if (product == null || amount <= 0 || amount > max) {
            invalidInput(player, pending);
            return;
        }

        completePending(player, pending);
        BuyShopGui.openPurchaseConfirmation(player, product, amount);
    }

    private void invalidInput(Player player, PendingInput pending) {
        pending.invalidAttempts++;
        if (pending.invalidAttempts >= 2) {
            cancel(player, "&c再次输入无效，交易已取消。");
            return;
        }

        int max = plugin.getBuyShopManager().getMaxAmount(
                plugin.getBuyShopManager().getProduct(pending.productKey));
        send(player, "messages.custom-invalid",
                "&c输入无效，请输入 1 至 {max} 的整数。你还可以重试一次。",
                "{max}", String.valueOf(max));
        if (pending.bedrock) {
            BuyProduct product = plugin.getBuyShopManager().getProduct(pending.productKey);
            if (product != null) {
                BuyShopGui.openBedrockCustomInput(player, product, true);
            }
        }
    }

    public void checkMovement(Player player, Location destination) {
        PendingInput pending = pendingInputs.get(player.getUniqueId());
        if (pending == null || destination == null) {
            return;
        }
        if (pending.origin.getWorld() == null || destination.getWorld() == null
                || !pending.origin.getWorld().equals(destination.getWorld())) {
            cancel(player, "&c你切换了世界，交易已取消。");
            return;
        }
        double distance = Math.max(0.0, plugin.getShopConfig().getDouble(
                "settings.custom-input-cancel-distance", 2.0));
        if (pending.origin.distanceSquared(destination) > distance * distance) {
            cancel(player, "&c你移动超过 " + formatDistance(distance) + " 格，交易已取消。");
        }
    }

    public void cancel(Player player, String message) {
        PendingInput pending = pendingInputs.remove(player.getUniqueId());
        if (pending != null && pending.timeoutTask != null) {
            pending.timeoutTask.cancel();
        }
        if (pending != null && message != null && !message.isBlank() && player.isOnline()) {
            sendRaw(player, message);
        }
    }

    public void cancelAll(String message) {
        for (UUID uuid : Set.copyOf(pendingInputs.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                cancel(player, message);
            } else {
                PendingInput pending = pendingInputs.remove(uuid);
                if (pending != null && pending.timeoutTask != null) {
                    pending.timeoutTask.cancel();
                }
            }
        }
    }

    private void completePending(Player player, PendingInput pending) {
        if (pendingInputs.remove(player.getUniqueId(), pending) && pending.timeoutTask != null) {
            pending.timeoutTask.cancel();
        }
    }

    public boolean purchase(Player player, String productKey, int requestedAmount) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> purchase(player, productKey, requestedAmount));
            return false;
        }
        if (!processing.add(player.getUniqueId())) {
            return fail(player, "messages.transaction-processing", "&c上一笔交易仍在处理中，请稍后再试。");
        }

        try {
            BuyProduct product = plugin.getBuyShopManager().getProduct(productKey);
            if (product == null || !plugin.getBuyShopManager().canView(player, product)) {
                return fail(player, "messages.product-unavailable", "&c该商品已下架或你没有购买权限。");
            }
            if (!player.hasPermission("dailysellshop.shop.buy")) {
                return fail(player, "messages.no-permission", "&c你没有购买商品的权限。");
            }
            if (product.spawnEgg() && !player.hasPermission("dailysellshop.shop.spawnegg")) {
                return fail(player, "messages.no-permission", "&c你没有购买刷怪蛋的权限。");
            }
            int max = plugin.getBuyShopManager().getMaxAmount(product);
            if (requestedAmount <= 0 || requestedAmount > max) {
                return fail(player, "messages.invalid-amount", "&c购买数量必须在 1 至 {max} 之间。",
                        "{max}", String.valueOf(max));
            }
            if (!plugin.hasEconomy()) {
                return fail(player, "messages.economy-unavailable", "&c经济系统当前不可用，暂时无法购买。");
            }

            int capacity = calculateCapacity(player, product);
            if (capacity <= 0) {
                return fail(player, "messages.inventory-full", "&c背包没有空间，无法购买该商品。");
            }
            int finalAmount = Math.min(requestedAmount, capacity);
            BigDecimal total = BuyShopManager.money(product.price().multiply(BigDecimal.valueOf(finalAmount)));
            if (!DailySellShop.getEconomy().has(player, total.doubleValue())) {
                return fail(player, "messages.insufficient-funds",
                        "&c余额不足，需要 ${total}，当前余额 ${balance}。",
                        "{total}", BuyShopManager.formatMoney(total),
                        "{balance}", formatBalance(player));
            }

            ItemStack[] snapshot = cloneStorage(player.getInventory().getStorageContents());
            EconomyService.TransactionResult withdrawal = DailySellShop.getEconomy().withdraw(player, total.doubleValue());
            if (!withdrawal.success()) {
                return fail(player, "messages.withdraw-failed", "&c扣款失败：{error}",
                        "{error}", withdrawal.errorMessage() == null ? "未知错误" : withdrawal.errorMessage());
            }

            if (!giveItems(player, product, finalAmount)) {
                player.getInventory().setStorageContents(snapshot);
                EconomyService.TransactionResult refund = DailySellShop.getEconomy().deposit(player, total.doubleValue());
                if (!refund.success()) {
                    plugin.getLogger().severe("购买发放失败且自动退款失败: " + player.getUniqueId()
                            + " " + product.key() + " $" + total + " 原因=" + refund.errorMessage());
                }
                return fail(player, "messages.delivery-failed", "&c物品发放失败，款项已自动退回。");
            }

            writeTransactionLog(player, product, finalAmount, total);
            send(player, "messages.purchase-success",
                    "&a成功购买 {amount} 个 {item}，花费 ${total}，剩余 ${balance}。",
                    "{amount}", String.valueOf(finalAmount),
                    "{item}", product.displayName(),
                    "{total}", BuyShopManager.formatMoney(total),
                    "{balance}", formatBalance(player));
            if (finalAmount < requestedAmount) {
                send(player, "messages.partial-purchase",
                        "&e背包空间不足，原计划购买 {requested} 个，实际购买 {amount} 个。",
                        "{requested}", String.valueOf(requestedAmount),
                        "{amount}", String.valueOf(finalAmount));
            }
            playConfiguredSound(player, "sounds.purchase-success", "ENTITY_PLAYER_LEVELUP");
            return true;
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    public int calculateCapacity(Player player, BuyProduct product) {
        ItemStack prototype = product.createItem(1);
        int maxStack = prototype.getMaxStackSize();
        long capacity = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                capacity += maxStack;
            } else if (item.isSimilar(prototype)) {
                capacity += Math.max(0, maxStack - item.getAmount());
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    public int requiredSlots(BuyProduct product, int amount) {
        int stackSize = product.createItem(1).getMaxStackSize();
        return (int) Math.ceil(amount / (double) stackSize);
    }

    private boolean giveItems(Player player, BuyProduct product, int amount) {
        int maxStack = product.createItem(1).getMaxStackSize();
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(maxStack, remaining);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(
                    product.createItem(stackAmount));
            if (!leftovers.isEmpty()) {
                return false;
            }
            remaining -= stackAmount;
        }
        return true;
    }

    private ItemStack[] cloneStorage(ItemStack[] contents) {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            clone[i] = contents[i] == null ? null : contents[i].clone();
        }
        return clone;
    }

    private void writeTransactionLog(Player player, BuyProduct product, int amount, BigDecimal total) {
        if (!plugin.getShopConfig().getBoolean("logging.enabled", true)) {
            return;
        }
        String line = Instant.now() + "\t" + player.getUniqueId() + "\t" + safe(player.getName())
                + "\t" + product.key() + "\t" + amount + "\t" + product.price()
                + "\t" + total + System.lineSeparator();
        String configuredPath = plugin.getShopConfig().getString("logging.file", "logs/purchases.log");
        File logFile = new File(plugin.getDataFolder(), configuredPath);
        try {
            logExecutor.execute(() -> {
                synchronized (logLock) {
                    try {
                        File parent = logFile.getParentFile();
                        if (parent != null) {
                            Files.createDirectories(parent.toPath());
                        }
                        Files.writeString(logFile.toPath(), line, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException exception) {
                        plugin.getLogger().warning("无法写入购买日志: " + exception.getMessage());
                    }
                }
            });
        } catch (RejectedExecutionException exception) {
            plugin.getLogger().warning("插件正在关闭，购买日志未能写入: " + product.key());
        }
        if (plugin.getShopConfig().getBoolean("logging.console", false)) {
            plugin.getLogger().info("购买记录: " + player.getName() + " " + product.key()
                    + " x" + amount + " $" + total);
        }
    }

    public String formatBalance(Player player) {
        return plugin.hasEconomy()
                ? String.format(Locale.ROOT, "%.2f", DailySellShop.getEconomy().getBalance(player))
                : "不可用";
    }

    public void send(Player player, String path, String fallback, String... replacements) {
        String message = plugin.getShopConfig().getString(path, fallback);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        sendRaw(player, message);
    }

    private boolean fail(Player player, String path, String fallback, String... replacements) {
        send(player, path, fallback, replacements);
        playConfiguredSound(player, "sounds.purchase-failure", "BLOCK_NOTE_BLOCK_BASS");
        return false;
    }

    private void playConfiguredSound(Player player, String path, String fallback) {
        if (!plugin.getShopConfig().getBoolean(path + ".enabled", true)) {
            return;
        }
        String configured = plugin.getShopConfig().getString(path + ".name", fallback);
        Sound sound = SoundResolver.resolve(configured);
        if (sound == null) {
            plugin.getLogger().warning("shopconfig.yml 中存在无效音效: " + configured);
            return;
        }
        float volume = (float) Math.max(0.0, plugin.getShopConfig().getDouble(path + ".volume", 1.0));
        float pitch = (float) Math.max(0.0, plugin.getShopConfig().getDouble(path + ".pitch", 1.0));
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void sendRaw(Player player, String message) {
        String prefix = plugin.getShopConfig().getString("messages.prefix", "&8[&6玩家商店&8] ");
        player.sendMessage(DailySellShop.colorize(prefix + message));
    }

    public void shutdown() {
        cancelAll("&c插件正在关闭，当前交易已取消。");
        processing.clear();
        logExecutor.shutdown();
        try {
            if (!logExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("购买日志队列未能在 2 秒内完全写入。");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static String safe(String value) {
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static String formatDistance(double distance) {
        return distance == Math.rint(distance)
                ? String.valueOf((int) distance)
                : String.format(Locale.ROOT, "%.1f", distance);
    }

    private static class PendingInput {
        private final String productKey;
        private final Location origin;
        private final long expiresAt;
        private final boolean bedrock;
        private int invalidAttempts;
        private BukkitTask timeoutTask;

        private PendingInput(String productKey, Location origin, long expiresAt, boolean bedrock) {
            this.productKey = productKey;
            this.origin = origin;
            this.expiresAt = expiresAt;
            this.bedrock = bedrock;
        }
    }
}
