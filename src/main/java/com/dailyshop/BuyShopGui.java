package com.dailyshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BuyShopGui {

    public static final String ACTION_CATEGORY = "category";
    public static final String ACTION_PRODUCT = "product";
    public static final String ACTION_PAGE = "page";
    public static final String ACTION_HOME = "home";
    public static final String ACTION_AMOUNT = "amount";
    public static final String ACTION_CUSTOM = "custom";
    public static final String ACTION_CONFIRM = "confirm";
    public static final String ACTION_BACK_PRODUCTS = "back-products";
    public static final String ACTION_CLOSE = "close";
    public static final String ACTION_NONE = "none";
    public static final String ACTION_CONFIGURED = "configured";
    private static final Set<UUID> OPEN_VIEWERS = new HashSet<>();

    private BuyShopGui() {
    }

    public static void open(Player player) {
        if (isBedrock(player)) {
            openBedrockHome(player);
        } else {
            openJavaHome(player);
        }
    }

    public static void openCategory(Player player, ShopCategory category, int page) {
        DailySellShop plugin = DailySellShop.getInstance();
        if (!plugin.getBuyShopManager().isCategoryEnabled(category)) {
            plugin.getPurchaseManager().send(player, "messages.category-disabled", "&c该分类当前未开放。");
            return;
        }
        List<BuyProduct> products = plugin.getBuyShopManager().getProducts(category).stream()
                .filter(product -> plugin.getBuyShopManager().canView(player, product))
                .toList();
        if (isBedrock(player)) {
            openBedrockProducts(player, category, products, null, page);
        } else {
            openJavaProducts(player, category, products, null, page);
        }
    }

    public static void openSearch(Player player, String query, int page) {
        DailySellShop plugin = DailySellShop.getInstance();
        List<BuyProduct> products = plugin.getBuyShopManager().search(query, player);
        if (products.isEmpty()) {
            plugin.getPurchaseManager().send(player, "messages.search-empty",
                    "&c没有找到包含“{query}”的商品。", "{query}", query);
            return;
        }
        if (isBedrock(player)) {
            openBedrockProducts(player, null, products, query, page);
        } else {
            openJavaProducts(player, null, products, query, page);
        }
    }

    public static void openAmount(Player player, BuyProduct product, int selectedAmount) {
        if (product == null) {
            return;
        }
        if (isBedrock(player)) {
            openBedrockAmounts(player, product);
        } else {
            openJavaAmount(player, product, selectedAmount);
        }
    }

    public static void openPurchaseConfirmation(Player player, BuyProduct product, int amount) {
        if (product == null) {
            return;
        }
        if (isBedrock(player)) {
            openBedrockConfirmation(player, product, amount);
        } else {
            openJavaAmount(player, product, amount);
        }
    }

    private static void openJavaHome(Player player) {
        DailySellShop plugin = DailySellShop.getInstance();
        BuyShopManager manager = plugin.getBuyShopManager();
        int size = menuSize(plugin.getShopConfig().getInt("menus.home.size", 54));
        String title = color(plugin.getShopConfig().getString("menus.home.title", "&0玩家商店 &8| &7商品分类"));
        Inventory inventory = Bukkit.createInventory(null, size, title);

        for (ShopCategory category : ShopCategory.values()) {
            if (!manager.isCategoryEnabled(category)) {
                continue;
            }
            int slot = manager.getCategorySlot(category);
            if (slot < 0 || slot >= size) {
                continue;
            }
            List<BuyProduct> visible = manager.getProducts(category).stream()
                    .filter(product -> manager.canView(player, product)).toList();
            Material icon = manager.getCategoryIcon(category);
            String name = manager.getCategoryName(category);
            List<String> lore = configuredLore("menus.home.category-lore", List.of(
                    "&7共有 &f{count} &7件商品",
                    "", "&e点击查看该分类"));
            Map<String, String> placeholders = placeholders(player, null, 0);
            placeholders.put("{count}", String.valueOf(visible.size()));
            ItemStack item = createItem(icon, name, lore, placeholders, ACTION_CATEGORY);
            setString(item, "buy_category", category.name());
            inventory.setItem(slot, item);
        }

        setIfValid(inventory, configuredButton("menus.home.buttons.balance", Material.EMERALD,
                "&a余额: &f${balance}", List.of("&7购买价默认等于收购单价"), player, ACTION_NONE),
                plugin.getShopConfig().getInt("menus.home.buttons.balance.slot", 4));
        setIfValid(inventory, configuredButton("menus.home.buttons.close", Material.BARRIER,
                "&c关闭商店", List.of(), player, ACTION_CLOSE),
                plugin.getShopConfig().getInt("menus.home.buttons.close.slot", 49));
        renderExtraButtons(inventory, "menus.home.buttons", Set.of("balance", "close"), player);
        fillEmpty(inventory, "menus.home.fill");
        openInventory(player, inventory);
    }

    private static void openJavaProducts(Player player, ShopCategory category, List<BuyProduct> products,
                                         String searchQuery, int requestedPage) {
        DailySellShop plugin = DailySellShop.getInstance();
        List<Integer> slots = itemSlots("menus.products.item-slots", defaultProductSlots());
        int pageCount = Math.max(1, (int) Math.ceil(products.size() / (double) slots.size()));
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        String categoryName = category == null ? "搜索结果" : strip(plugin.getBuyShopManager().getCategoryName(category));
        String titleTemplate = plugin.getShopConfig().getString("menus.products.title", "&0{category} &8| &7第 {page}/{pages} 页");
        String title = color(titleTemplate.replace("{category}", categoryName)
                .replace("{page}", String.valueOf(page + 1)).replace("{pages}", String.valueOf(pageCount)));
        Inventory inventory = Bukkit.createInventory(null,
                menuSize(plugin.getShopConfig().getInt("menus.products.size", 54)), title);

        int start = page * slots.size();
        int end = Math.min(start + slots.size(), products.size());
        for (int index = start; index < end; index++) {
            BuyProduct product = products.get(index);
            int slot = slots.get(index - start);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            List<String> lore = configuredLore("menus.products.product-lore", List.of(
                    "&7物品 ID: &f{material}",
                    "&7单价: &a${price}",
                    "&7你的余额: &f${balance}",
                    "", "&e点击选择购买数量"));
            ItemStack item = createItem(product.createItem(1), product.displayName(), lore,
                    placeholders(player, product, 0), ACTION_PRODUCT);
            setString(item, "buy_product", product.key());
            inventory.setItem(slot, item);
        }

        String context = category != null ? "category:" + category.name() : "search:" + searchQuery;
        if (page > 0) {
            ItemStack previous = configuredButton("menus.products.buttons.previous", Material.ARROW,
                    "&a上一页", List.of("&7返回第 {target-page} 页"), player, ACTION_PAGE);
            setString(previous, "buy_context", context);
            setInt(previous, "buy_page", page - 1);
            setIfValid(inventory, previous,
                    plugin.getShopConfig().getInt("menus.products.buttons.previous.slot", 45));
        }
        if (page + 1 < pageCount) {
            ItemStack next = configuredButton("menus.products.buttons.next", Material.ARROW,
                    "&a下一页", List.of("&7前往第 {target-page} 页"), player, ACTION_PAGE);
            setString(next, "buy_context", context);
            setInt(next, "buy_page", page + 1);
            setIfValid(inventory, next,
                    plugin.getShopConfig().getInt("menus.products.buttons.next.slot", 53));
        }
        ItemStack info = configuredButton("menus.products.buttons.page", Material.PAPER,
                "&f第 {page}/{pages} 页", List.of("&7共 {count} 件商品", "&7余额: &f${balance}"),
                player, ACTION_NONE, Map.of("{page}", String.valueOf(page + 1),
                        "{pages}", String.valueOf(pageCount), "{count}", String.valueOf(products.size())));
        setIfValid(inventory, info, plugin.getShopConfig().getInt("menus.products.buttons.page.slot", 49));
        setIfValid(inventory, configuredButton("menus.products.buttons.home", Material.NETHER_STAR,
                "&e返回分类首页", List.of(), player, ACTION_HOME),
                plugin.getShopConfig().getInt("menus.products.buttons.home.slot", 48));
        setIfValid(inventory, configuredButton("menus.products.buttons.close", Material.BARRIER,
                "&c关闭商店", List.of(), player, ACTION_CLOSE),
                plugin.getShopConfig().getInt("menus.products.buttons.close.slot", 50));
        renderExtraButtons(inventory, "menus.products.buttons",
                Set.of("previous", "home", "page", "close", "next"), player);
        fillEmpty(inventory, "menus.products.fill");
        openInventory(player, inventory);
    }

    private static void openJavaAmount(Player player, BuyProduct product, int selectedAmount) {
        DailySellShop plugin = DailySellShop.getInstance();
        PurchaseManager purchases = plugin.getPurchaseManager();
        int size = menuSize(plugin.getShopConfig().getInt("menus.amount.size", 54));
        String title = color(plugin.getShopConfig().getString("menus.amount.title", "&0选择购买数量"));
        Inventory inventory = Bukkit.createInventory(null, size, title);
        int max = plugin.getBuyShopManager().getMaxAmount(product);

        List<String> summaryLore = configuredLore("menus.amount.summary-lore", List.of(
                "&7单价: &a${price}",
                "&7已选数量: &f{amount}",
                "&7总价: &6${total}",
                "&7预计占用: &f{slots} 个背包格",
                "&7当前余额: &f${balance}"));
        ItemStack summary = createItem(product.createItem(1), product.displayName(), summaryLore,
                placeholders(player, product, selectedAmount), ACTION_NONE);
        setIfValid(inventory, summary, plugin.getShopConfig().getInt("menus.amount.summary-slot", 13));

        List<Integer> presets = plugin.getShopConfig().getIntegerList("menus.amount.presets");
        if (presets.isEmpty()) {
            presets = List.of(1, 5, 10, 20, 30, 40, 50, 64);
        }
        List<Integer> presetSlots = itemSlots("menus.amount.preset-slots",
                List.of(19, 20, 21, 22, 23, 24, 25, 26));
        for (int index = 0; index < Math.min(presets.size(), presetSlots.size()); index++) {
            int amount = presets.get(index);
            if (amount <= 0 || amount > max) {
                continue;
            }
            BigDecimal total = BuyShopManager.money(product.price().multiply(BigDecimal.valueOf(amount)));
            ItemStack preset = configuredButton("menus.amount.preset-button", Material.LIME_STAINED_GLASS_PANE,
                    "&a购买 {amount} 个", List.of("&7总价: &f${total}", "", "&e点击选择"),
                    player, ACTION_AMOUNT, Map.of("{amount}", String.valueOf(amount),
                            "{total}", BuyShopManager.formatMoney(total)));
            setString(preset, "buy_product", product.key());
            setInt(preset, "buy_amount", amount);
            setIfValid(inventory, preset, presetSlots.get(index));
        }

        ItemStack custom = configuredButton("menus.amount.buttons.custom", Material.WRITABLE_BOOK,
                "&b自定义数量", List.of("&7范围: 1 - {max}", "&7输入期间移动超过 {distance} 格将取消"),
                player, ACTION_CUSTOM, Map.of("{max}", String.valueOf(max),
                        "{distance}", formatNumber(plugin.getShopConfig().getDouble(
                                "settings.custom-input-cancel-distance", 2.0))));
        setString(custom, "buy_product", product.key());
        setIfValid(inventory, custom, plugin.getShopConfig().getInt("menus.amount.buttons.custom.slot", 31));

        if (selectedAmount > 0 && selectedAmount <= max) {
            BigDecimal total = BuyShopManager.money(product.price().multiply(BigDecimal.valueOf(selectedAmount)));
            ItemStack confirm = configuredButton("menus.amount.buttons.confirm", Material.EMERALD_BLOCK,
                    "&a确认购买 {amount} 个", List.of("&7总价: &f${total}", "", "&a点击确认扣款"),
                    player, ACTION_CONFIRM, Map.of("{amount}", String.valueOf(selectedAmount),
                            "{total}", BuyShopManager.formatMoney(total)));
            setString(confirm, "buy_product", product.key());
            setInt(confirm, "buy_amount", selectedAmount);
            setIfValid(inventory, confirm,
                    plugin.getShopConfig().getInt("menus.amount.buttons.confirm.slot", 40));
        }

        ItemStack back = configuredButton("menus.amount.buttons.back", Material.ARROW,
                "&e返回商品列表", List.of(), player, ACTION_BACK_PRODUCTS);
        setString(back, "buy_category", product.category().name());
        setIfValid(inventory, back, plugin.getShopConfig().getInt("menus.amount.buttons.back.slot", 45));
        setIfValid(inventory, configuredButton("menus.amount.buttons.close", Material.BARRIER,
                "&c取消交易", List.of(), player, ACTION_CLOSE),
                plugin.getShopConfig().getInt("menus.amount.buttons.close.slot", 53));
        renderExtraButtons(inventory, "menus.amount.buttons",
                Set.of("custom", "confirm", "back", "close"), player);
        fillEmpty(inventory, "menus.amount.fill");
        openInventory(player, inventory);
    }

    private static void openBedrockHome(Player player) {
        DailySellShop plugin = DailySellShop.getInstance();
        BuyShopManager manager = plugin.getBuyShopManager();
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(strip(plugin.getShopConfig().getString("menus.home.title", "玩家商店")))
                .content("余额: $" + plugin.getPurchaseManager().formatBalance(player));
        List<String> mapping = new ArrayList<>();
        for (ShopCategory category : ShopCategory.values()) {
            if (!manager.isCategoryEnabled(category)) {
                continue;
            }
            long count = manager.getProducts(category).stream().filter(product -> manager.canView(player, product)).count();
            builder.button(strip(manager.getCategoryName(category)) + "\n" + count + " 件商品");
            mapping.add("category:" + category.name());
        }
        ConfigurationSection buttonSection = plugin.getShopConfig().getConfigurationSection("menus.home.buttons");
        if (buttonSection != null) {
            for (String id : buttonSection.getKeys(false)) {
                String path = "menus.home.buttons." + id;
                if (plugin.getShopConfig().getStringList(path + ".commands").isEmpty()) {
                    continue;
                }
                String permission = plugin.getShopConfig().getString(path + ".permission", "");
                if (!permission.isBlank() && !player.hasPermission(permission)) {
                    continue;
                }
                builder.button(strip(plugin.getShopConfig().getString(path + ".name", id)));
                mapping.add("button:" + path);
            }
        }
        builder.validResultHandler(response -> runSync(() -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < mapping.size()) {
                String action = mapping.get(index);
                if (action.startsWith("category:")) {
                    openCategory(player, ShopCategory.valueOf(action.substring(9)), 0);
                } else if (action.startsWith("button:")) {
                    executeConfiguredButton(player, action.substring(7));
                }
            }
        }));
        sendForm(player, builder.build());
    }

    private static void openBedrockProducts(Player player, ShopCategory category, List<BuyProduct> products,
                                             String searchQuery, int requestedPage) {
        DailySellShop plugin = DailySellShop.getInstance();
        int pageSize = Math.max(1, Math.min(50,
                plugin.getShopConfig().getInt("settings.bedrock-page-size", 20)));
        int pageCount = Math.max(1, (int) Math.ceil(products.size() / (double) pageSize));
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        String name = category == null ? "搜索: " + searchQuery : strip(plugin.getBuyShopManager().getCategoryName(category));
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(name)
                .content("第 " + (page + 1) + "/" + pageCount + " 页 | 余额: $"
                        + plugin.getPurchaseManager().formatBalance(player));
        List<String> actions = new ArrayList<>();
        int start = page * pageSize;
        int end = Math.min(start + pageSize, products.size());
        for (int index = start; index < end; index++) {
            BuyProduct product = products.get(index);
            builder.button(strip(product.displayName()) + "\n$" + BuyShopManager.formatMoney(product.price()));
            actions.add("product:" + product.key());
        }
        if (page > 0) {
            builder.button("上一页");
            actions.add("page:" + (page - 1));
        }
        if (page + 1 < pageCount) {
            builder.button("下一页");
            actions.add("page:" + (page + 1));
        }
        builder.button("返回分类首页");
        actions.add("home");
        builder.validResultHandler(response -> runSync(() -> {
            int index = response.clickedButtonId();
            if (index < 0 || index >= actions.size()) {
                return;
            }
            String action = actions.get(index);
            if (action.startsWith("product:")) {
                BuyProduct product = plugin.getBuyShopManager().getProduct(action.substring(8));
                openAmount(player, product, 0);
            } else if (action.startsWith("page:")) {
                int target = Integer.parseInt(action.substring(5));
                if (category != null) {
                    openCategory(player, category, target);
                } else {
                    openSearch(player, searchQuery, target);
                }
            } else {
                openBedrockHome(player);
            }
        }));
        sendForm(player, builder.build());
    }

    private static void openBedrockAmounts(Player player, BuyProduct product) {
        DailySellShop plugin = DailySellShop.getInstance();
        int max = plugin.getBuyShopManager().getMaxAmount(product);
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(strip(product.displayName()))
                .content("单价: $" + BuyShopManager.formatMoney(product.price()) + "\n余额: $"
                        + plugin.getPurchaseManager().formatBalance(player));
        List<Integer> amounts = plugin.getShopConfig().getIntegerList("menus.amount.presets");
        if (amounts.isEmpty()) {
            amounts = List.of(1, 5, 10, 20, 30, 40, 50, 64);
        }
        List<Integer> mapping = new ArrayList<>();
        for (int amount : amounts) {
            if (amount > 0 && amount <= max) {
                BigDecimal total = BuyShopManager.money(product.price().multiply(BigDecimal.valueOf(amount)));
                builder.button("购买 " + amount + " 个\n$" + BuyShopManager.formatMoney(total));
                mapping.add(amount);
            }
        }
        builder.button("自定义数量");
        mapping.add(-1);
        builder.button("返回商品列表");
        mapping.add(-2);
        builder.validResultHandler(response -> runSync(() -> {
            int index = response.clickedButtonId();
            if (index < 0 || index >= mapping.size()) {
                return;
            }
            int amount = mapping.get(index);
            if (amount > 0) {
                openBedrockConfirmation(player, product, amount);
            } else if (amount == -1) {
                plugin.getPurchaseManager().startCustomInput(player, product.key(), true);
            } else {
                openCategory(player, product.category(), 0);
            }
        }));
        sendForm(player, builder.build());
    }

    public static void openBedrockCustomInput(Player player, BuyProduct product, boolean retry) {
        DailySellShop plugin = DailySellShop.getInstance();
        int max = plugin.getBuyShopManager().getMaxAmount(product);
        CustomForm.Builder builder = CustomForm.builder()
                .title("自定义购买数量")
                .label((retry ? "上次输入无效，请重试一次。\n" : "")
                        + strip(product.displayName()) + " | 单价 $" + BuyShopManager.formatMoney(product.price()))
                .input("数量（1 - " + max + "）", "请输入正整数", "1");
        builder.validResultHandler(response -> runSync(() ->
                plugin.getPurchaseManager().handleCustomInput(player, response.asInput(1))));
        builder.closedOrInvalidResultHandler(() -> runSync(() ->
                plugin.getPurchaseManager().cancel(player, "&c交易已取消。")));
        sendForm(player, builder.build());
    }

    private static void openBedrockConfirmation(Player player, BuyProduct product, int amount) {
        DailySellShop plugin = DailySellShop.getInstance();
        BigDecimal total = BuyShopManager.money(product.price().multiply(BigDecimal.valueOf(amount)));
        int slots = plugin.getPurchaseManager().requiredSlots(product, amount);
        ModalForm.Builder builder = ModalForm.builder()
                .title("确认购买")
                .content(strip(product.displayName()) + "\n数量: " + amount + "\n总价: $"
                        + BuyShopManager.formatMoney(total) + "\n预计占用: " + slots + " 个背包格\n余额: $"
                        + plugin.getPurchaseManager().formatBalance(player))
                .button1("确认购买")
                .button2("返回");
        builder.validResultHandler(response -> runSync(() -> {
            if (response.clickedFirst()) {
                plugin.getPurchaseManager().purchase(player, product.key(), amount);
            } else {
                openBedrockAmounts(player, product);
            }
        }));
        sendForm(player, builder.build());
    }

    public static boolean isOpen(Player player) {
        return OPEN_VIEWERS.contains(player.getUniqueId());
    }

    public static void unregister(Player player) {
        OPEN_VIEWERS.remove(player.getUniqueId());
    }

    public static boolean isBedrock(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static NamespacedKey key(String name) {
        return new NamespacedKey(DailySellShop.getInstance(), name);
    }

    private static void openInventory(Player player, Inventory inventory) {
        player.openInventory(inventory);
        OPEN_VIEWERS.add(player.getUniqueId());
    }

    private static ItemStack configuredButton(String path, Material fallbackMaterial, String fallbackName,
                                              List<String> fallbackLore, Player player, String action) {
        return configuredButton(path, fallbackMaterial, fallbackName, fallbackLore, player, action, Map.of());
    }

    private static ItemStack configuredButton(String path, Material fallbackMaterial, String fallbackName,
                                              List<String> fallbackLore, Player player, String action,
                                              Map<String, String> extraPlaceholders) {
        DailySellShop plugin = DailySellShop.getInstance();
        Material material = Material.getMaterial(plugin.getShopConfig().getString(path + ".material",
                fallbackMaterial.name()).toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) {
            material = fallbackMaterial;
        }
        String name = plugin.getShopConfig().getString(path + ".name", fallbackName);
        List<String> lore = configuredLore(path + ".lore", fallbackLore);
        Map<String, String> values = placeholders(player, null, 0);
        values.putAll(extraPlaceholders);
        List<String> commands = plugin.getShopConfig().getStringList(path + ".commands");
        String effectiveAction = commands.isEmpty() ? action : ACTION_CONFIGURED;
        ItemStack item = createItem(material, name, lore, values, effectiveAction);
        if (!commands.isEmpty()) {
            setString(item, "buy_button_path", path);
        }
        return item;
    }

    private static void renderExtraButtons(Inventory inventory, String root, Set<String> reserved, Player player) {
        DailySellShop plugin = DailySellShop.getInstance();
        ConfigurationSection section = plugin.getShopConfig().getConfigurationSection(root);
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            if (reserved.contains(id)) {
                continue;
            }
            String path = root + "." + id;
            int slot = plugin.getShopConfig().getInt(path + ".slot", -1);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            String permission = plugin.getShopConfig().getString(path + ".permission", "");
            if (!permission.isBlank() && !player.hasPermission(permission)) {
                continue;
            }
            inventory.setItem(slot, configuredButton(path, Material.STONE, id, List.of(),
                    player, ACTION_NONE));
        }
    }

    public static void executeConfiguredButton(Player player, String path) {
        DailySellShop plugin = DailySellShop.getInstance();
        if (path == null || path.isBlank() || !path.startsWith("menus.")) {
            return;
        }
        String permission = plugin.getShopConfig().getString(path + ".permission", "");
        if (!permission.isBlank() && !player.hasPermission(permission)) {
            plugin.getPurchaseManager().send(player, "messages.no-permission", "&c你没有权限执行此操作。");
            return;
        }
        for (String command : plugin.getShopConfig().getStringList(path + ".commands")) {
            ShopListener.executeAction(player, command);
        }
    }

    private static ItemStack createItem(Material material, String name, List<String> lore,
                                        Map<String, String> placeholders, String action) {
        return createItem(new ItemStack(material), name, lore, placeholders, action);
    }

    private static ItemStack createItem(ItemStack item, String name, List<String> lore,
                                        Map<String, String> placeholders, String action) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(color(apply(name, placeholders)));
        List<String> renderedLore = new ArrayList<>();
        for (String line : lore) {
            renderedLore.add(color(apply(line, placeholders)));
        }
        meta.setLore(renderedLore);
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(key("buy_action"), PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private static Map<String, String> placeholders(Player player, BuyProduct product, int amount) {
        DailySellShop plugin = DailySellShop.getInstance();
        Map<String, String> values = new HashMap<>();
        values.put("{player}", player.getName());
        values.put("{balance}", plugin.getPurchaseManager().formatBalance(player));
        values.put("{amount}", String.valueOf(amount));
        if (product != null) {
            BigDecimal total = BuyShopManager.money(product.price().multiply(BigDecimal.valueOf(amount)));
            values.put("{item}", product.displayName());
            values.put("{material}", product.key());
            values.put("{price}", BuyShopManager.formatMoney(product.price()));
            values.put("{total}", BuyShopManager.formatMoney(total));
            values.put("{slots}", String.valueOf(plugin.getPurchaseManager().requiredSlots(product, amount)));
            values.put("{capacity}", String.valueOf(plugin.getPurchaseManager().calculateCapacity(player, product)));
        }
        return values;
    }

    private static String apply(String text, Map<String, String> placeholders) {
        String result = text == null ? "" : text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static List<String> configuredLore(String path, List<String> fallback) {
        List<String> configured = DailySellShop.getInstance().getShopConfig().getStringList(path);
        return configured.isEmpty() ? fallback : configured;
    }

    private static void fillEmpty(Inventory inventory, String path) {
        DailySellShop plugin = DailySellShop.getInstance();
        if (!plugin.getShopConfig().getBoolean(path + ".enabled", true)) {
            return;
        }
        Material material = Material.getMaterial(plugin.getShopConfig().getString(
                path + ".material", "BLACK_STAINED_GLASS_PANE").toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) {
            material = Material.BLACK_STAINED_GLASS_PANE;
        }
        ItemStack fill = createItem(material, plugin.getShopConfig().getString(path + ".name", "&8"),
                List.of(), Map.of(), ACTION_NONE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, fill);
            }
        }
    }

    private static List<Integer> itemSlots(String path, List<Integer> fallback) {
        List<Integer> configured = DailySellShop.getInstance().getShopConfig().getIntegerList(path);
        return configured.isEmpty() ? fallback : configured;
    }

    private static List<Integer> defaultProductSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < 45; slot++) {
            slots.add(slot);
        }
        return slots;
    }

    private static void setIfValid(Inventory inventory, ItemStack item, int slot) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    private static void setString(ItemStack item, String key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key(key), PersistentDataType.STRING, value == null ? "" : value);
            item.setItemMeta(meta);
        }
    }

    private static void setInt(ItemStack item, String key, int value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key(key), PersistentDataType.INTEGER, value);
            item.setItemMeta(meta);
        }
    }

    public static String getString(PersistentDataContainer data, String key) {
        return data.get(key(key), PersistentDataType.STRING);
    }

    public static Integer getInt(PersistentDataContainer data, String key) {
        return data.get(key(key), PersistentDataType.INTEGER);
    }

    private static int menuSize(int configured) {
        int rows = Math.max(1, Math.min(6, (int) Math.ceil(configured / 9.0)));
        return rows * 9;
    }

    private static void sendForm(Player player, org.geysermc.cumulus.form.Form form) {
        try {
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
        } catch (RuntimeException exception) {
            DailySellShop.getInstance().getLogger().warning("无法向基岩版玩家发送商店表单: " + exception.getMessage());
        }
    }

    private static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(DailySellShop.getInstance(), runnable);
    }

    private static String color(String text) {
        return DailySellShop.colorize(text);
    }

    private static String strip(String text) {
        return BuyShopManager.stripColor(text);
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
