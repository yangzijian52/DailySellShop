package com.dailyshop;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.scheduler.BukkitRunnable;

public class BuyShopListener implements Listener {

    private final DailySellShop plugin;

    public BuyShopListener(DailySellShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !BuyShopGui.isOpen(player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = current.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        String action = BuyShopGui.getString(data, "buy_action");
        if (action == null) {
            return;
        }

        switch (action) {
            case BuyShopGui.ACTION_CATEGORY -> {
                ShopCategory category = parseCategory(BuyShopGui.getString(data, "buy_category"));
                if (category != null) {
                    BuyShopGui.openCategory(player, category, 0);
                }
            }
            case BuyShopGui.ACTION_PRODUCT -> {
                BuyProduct product = plugin.getBuyShopManager().getProduct(
                        BuyShopGui.getString(data, "buy_product"));
                BuyShopGui.openAmount(player, product, 0);
            }
            case BuyShopGui.ACTION_PAGE -> openPage(player, data);
            case BuyShopGui.ACTION_HOME -> BuyShopGui.open(player);
            case BuyShopGui.ACTION_AMOUNT -> {
                BuyProduct product = plugin.getBuyShopManager().getProduct(
                        BuyShopGui.getString(data, "buy_product"));
                Integer amount = BuyShopGui.getInt(data, "buy_amount");
                if (product != null && amount != null) {
                    BuyShopGui.openAmount(player, product, amount);
                }
            }
            case BuyShopGui.ACTION_CUSTOM -> {
                String productKey = BuyShopGui.getString(data, "buy_product");
                plugin.getPurchaseManager().startCustomInput(player, productKey, false);
            }
            case BuyShopGui.ACTION_CONFIRM -> {
                String productKey = BuyShopGui.getString(data, "buy_product");
                Integer amount = BuyShopGui.getInt(data, "buy_amount");
                if (amount != null && plugin.getPurchaseManager().purchase(player, productKey, amount)) {
                    BuyProduct product = plugin.getBuyShopManager().getProduct(productKey);
                    if (product != null) {
                        BuyShopGui.openCategory(player, product.category(), 0);
                    }
                }
            }
            case BuyShopGui.ACTION_BACK_PRODUCTS -> {
                ShopCategory category = parseCategory(BuyShopGui.getString(data, "buy_category"));
                if (category == null) {
                    BuyShopGui.open(player);
                } else {
                    BuyShopGui.openCategory(player, category, 0);
                }
            }
            case BuyShopGui.ACTION_CLOSE -> player.closeInventory();
            case BuyShopGui.ACTION_CONFIGURED -> BuyShopGui.executeConfiguredButton(player,
                    BuyShopGui.getString(data, "buy_button_path"));
            default -> {
            }
        }
    }

    private void openPage(Player player, PersistentDataContainer data) {
        Integer page = BuyShopGui.getInt(data, "buy_page");
        String context = BuyShopGui.getString(data, "buy_context");
        if (page == null || context == null) {
            return;
        }
        if (context.startsWith("category:")) {
            ShopCategory category = parseCategory(context.substring("category:".length()));
            if (category != null) {
                BuyShopGui.openCategory(player, category, page);
            }
        } else if (context.startsWith("search:")) {
            BuyShopGui.openSearch(player, context.substring("search:".length()), page);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && BuyShopGui.isOpen(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            BuyShopGui.unregister(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getPurchaseManager().hasPendingInput(player)) {
            return;
        }
        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message());
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getPurchaseManager().handleCustomInput(player, input);
            }
        }.runTask(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (plugin.getPurchaseManager().hasPendingInput(event.getPlayer())) {
            plugin.getPurchaseManager().checkMovement(event.getPlayer(), event.getTo());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPurchaseManager().cancel(event.getPlayer(), null);
        BuyShopGui.unregister(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.getPurchaseManager().cancel(event.getEntity(), "&c你已死亡，交易已取消。");
    }

    private ShopCategory parseCategory(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ShopCategory.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
