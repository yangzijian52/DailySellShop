package com.dailyshop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BuyShopManager {

    private final DailySellShop plugin;
    private final Map<String, BuyProduct> products = new LinkedHashMap<>();
    private final Map<ShopCategory, List<BuyProduct>> productsByCategory = new EnumMap<>(ShopCategory.class);
    private final Map<String, ShopCategory> creativeCategories = new LinkedHashMap<>();
    private final Map<String, Integer> creativeOrder = new LinkedHashMap<>();

    public BuyShopManager(DailySellShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        products.clear();
        productsByCategory.clear();
        loadCreativeOrder();
        for (ShopCategory category : ShopCategory.values()) {
            productsByCategory.put(category, new ArrayList<>());
        }

        loadRegularProducts();
        loadSpawnEggs();

        Comparator<BuyProduct> comparator = Comparator.comparingInt(BuyProduct::weight)
                .thenComparing(BuyProduct::key);
        for (List<BuyProduct> categoryProducts : productsByCategory.values()) {
            categoryProducts.sort(comparator);
        }
        plugin.getLogger().info("购买商店已加载 " + products.size() + " 个商品，其中刷怪蛋 "
                + productsByCategory.get(ShopCategory.SPAWN_EGGS).size() + " 个。");
    }

    private void loadRegularProducts() {
        ConfigurationSection section = plugin.getSellConfig().getConfigurationSection("items");
        if (section == null) {
            plugin.getLogger().warning("SellShopconfig.yml 中未找到 items 节点，普通购买商店为空。");
            return;
        }

        BigDecimal multiplier = decimal(plugin.getShopConfig().get("settings.buy-price-multiplier"), BigDecimal.ONE);
        for (String rawKey : section.getKeys(false)) {
            String key = rawKey.toUpperCase(Locale.ROOT);
            Material material = ShopItemFactory.resolveMaterial(plugin.getSellConfig(), "items", rawKey);
            ItemStack template = ShopItemFactory.create(plugin.getSellConfig(), "items", rawKey, 1);
            if (material == null || template == null || !material.isItem()
                    || material.name().endsWith("_SPAWN_EGG")) {
                continue;
            }

            String overridePath = "product-overrides." + key + ".";
            if (!plugin.getShopConfig().getBoolean(overridePath + "enabled", true)) {
                continue;
            }

            BigDecimal sellPrice = decimal(section.get(rawKey + ".price"), BigDecimal.ZERO);
            BigDecimal price = sellPrice.multiply(multiplier);
            if (plugin.getShopConfig().contains(overridePath + "price")) {
                price = decimal(plugin.getShopConfig().get(overridePath + "price"), price);
            }
            price = money(price);
            if (price.signum() < 0) {
                plugin.getLogger().warning("忽略负数购买价商品: " + key);
                continue;
            }

            String configuredName = section.getString(rawKey + ".name", key);
            String displayName = plugin.getShopConfig().getString(overridePath + "name", configuredName);
            ShopCategory automatic = creativeCategories.get(key);
            if (automatic == null) {
                if (material == Material.POTION) {
                    automatic = ShopCategory.FOOD_AND_DRINKS;
                } else if (material == Material.ENCHANTED_BOOK) {
                    automatic = ShopCategory.RAW_MATERIALS;
                } else {
                    automatic = ShopCategory.classify(material);
                }
            }
            ShopCategory category = ShopCategory.fromConfig(
                    plugin.getShopConfig().getString(overridePath + "category",
                            plugin.getShopConfig().getString("category-overrides." + key)),
                    automatic);
            if (category == ShopCategory.SPAWN_EGGS) {
                category = automatic;
            }

            int weight = plugin.getShopConfig().contains(overridePath + "weight")
                    ? plugin.getShopConfig().getInt(overridePath + "weight")
                    : creativeOrder.getOrDefault(key, 1_000_000);
            String permission = plugin.getShopConfig().getString(overridePath + "permission", "");
            addProduct(new BuyProduct(key, material, DailySellShop.colorize(displayName), price,
                    category, false, weight, 0, permission, template));
        }
    }

    private void loadSpawnEggs() {
        ConfigurationSection section = plugin.getShopConfig().getConfigurationSection("spawn-eggs");
        if (section == null) {
            plugin.getLogger().warning("shopconfig.yml 中未找到 spawn-eggs 节点。");
            return;
        }

        for (String rawKey : section.getKeys(false)) {
            String key = rawKey.toUpperCase(Locale.ROOT);
            String path = "spawn-eggs." + rawKey + ".";
            if (!plugin.getShopConfig().getBoolean(path + "enabled",
                    plugin.getShopConfig().getBoolean("spawn-egg-defaults.enabled", false))) {
                continue;
            }
            Material material = Material.getMaterial(key);
            if (material == null || !material.isItem() || !key.endsWith("_SPAWN_EGG")) {
                plugin.getLogger().warning("忽略无效刷怪蛋配置: " + rawKey);
                continue;
            }

            BigDecimal price = money(decimal(plugin.getShopConfig().get(path + "price"), BigDecimal.ZERO));
            if (price.signum() < 0) {
                plugin.getLogger().warning("忽略负数刷怪蛋价格: " + key);
                continue;
            }
            String displayName = DailySellShop.colorize(plugin.getShopConfig().getString(path + "name", key));
            int weight = plugin.getShopConfig().contains(path + "weight")
                    ? plugin.getShopConfig().getInt(path + "weight")
                    : plugin.getShopConfig().getInt("spawn-egg-defaults.weight", 1000)
                    + creativeOrder.getOrDefault(key, 1_000_000);
            int maxAmount = Math.max(1, plugin.getShopConfig().getInt(path + "max-amount",
                    plugin.getShopConfig().getInt("spawn-egg-defaults.max-amount", getCustomAmountMax())));
            boolean requirePermission = plugin.getShopConfig().getBoolean(path + "require-permission",
                    plugin.getShopConfig().getBoolean("spawn-egg-defaults.require-permission", false));
            String permission = requirePermission
                    ? plugin.getShopConfig().getString(path + "permission",
                    plugin.getShopConfig().getString("spawn-egg-defaults.permission", "dailysellshop.shop.spawnegg"))
                    : "";
            addProduct(new BuyProduct(key, material, displayName, price, ShopCategory.SPAWN_EGGS,
                    true, weight, maxAmount, permission, new ItemStack(material)));
        }
    }

    private void addProduct(BuyProduct product) {
        products.put(product.key(), product);
        productsByCategory.get(product.category()).add(product);
    }

    private void loadCreativeOrder() {
        creativeCategories.clear();
        creativeOrder.clear();
        try (InputStream stream = plugin.getResource("creative-order.yml")) {
            if (stream == null) {
                plugin.getLogger().warning("缺少 creative-order.yml，将使用兼容分类规则。");
                return;
            }
            FileConfiguration catalog = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (ShopCategory category : ShopCategory.values()) {
                List<String> keys = catalog.getStringList(category.id());
                for (int index = 0; index < keys.size(); index++) {
                    String key = keys.get(index).toUpperCase(Locale.ROOT);
                    creativeCategories.putIfAbsent(key, category);
                    creativeOrder.putIfAbsent(key, index);
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("读取 Mojang 创造模式目录失败: " + exception.getMessage());
        }
    }

    public BuyProduct getProduct(String key) {
        return key == null ? null : products.get(key.toUpperCase(Locale.ROOT));
    }

    public List<BuyProduct> getProducts(ShopCategory category) {
        return new ArrayList<>(productsByCategory.getOrDefault(category, List.of()));
    }

    public List<BuyProduct> search(String query, Player player) {
        String normalized = stripColor(query).toLowerCase(Locale.ROOT).trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        return products.values().stream()
                .filter(product -> canView(player, product))
                .filter(product -> product.key().toLowerCase(Locale.ROOT).contains(normalized)
                        || stripColor(product.displayName()).toLowerCase(Locale.ROOT).contains(normalized))
                .limit(500)
                .toList();
    }

    public boolean canView(Player player, BuyProduct product) {
        return product != null
                && isCategoryEnabled(product.category())
                && (!product.hasPermission() || player.hasPermission(product.permission()));
    }

    public boolean isCategoryEnabled(ShopCategory category) {
        return plugin.getShopConfig().getBoolean("categories." + category.id() + ".enabled", true);
    }

    public String getCategoryName(ShopCategory category) {
        return DailySellShop.colorize(plugin.getShopConfig().getString(
                "categories." + category.id() + ".name", "&f" + category.defaultName()));
    }

    public Material getCategoryIcon(ShopCategory category) {
        String configured = plugin.getShopConfig().getString(
                "categories." + category.id() + ".material", category.defaultIcon().name());
        Material material = Material.getMaterial(configured.toUpperCase(Locale.ROOT));
        return material != null && material.isItem() ? material : category.defaultIcon();
    }

    public int getCategorySlot(ShopCategory category) {
        return plugin.getShopConfig().getInt("categories." + category.id() + ".slot", category.ordinal() + 19);
    }

    public int getCustomAmountMax() {
        return Math.max(1, plugin.getShopConfig().getInt("settings.custom-amount-max", 2304));
    }

    public int getMaxAmount(BuyProduct product) {
        if (product != null && product.maxAmount() > 0) {
            return Math.min(getCustomAmountMax(), product.maxAmount());
        }
        return getCustomAmountMax();
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(Object value, BigDecimal fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static String formatMoney(BigDecimal value) {
        return money(value).toPlainString();
    }

    public static String stripColor(String text) {
        String stripped = ChatColor.stripColor(DailySellShop.colorize(text));
        return stripped == null ? "" : stripped;
    }
}
