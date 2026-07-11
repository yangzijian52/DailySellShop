package com.dailyshop;

import org.bukkit.Material;
import org.bukkit.inventory.CreativeCategory;

import java.util.Locale;
import java.util.Set;

public enum ShopCategory {
    BUILDING_BLOCKS("building-blocks", "建筑方块", Material.BRICKS),
    COLORED_BLOCKS("colored-blocks", "染色方块", Material.CYAN_WOOL),
    NATURAL_BLOCKS("natural-blocks", "自然方块", Material.GRASS_BLOCK),
    FUNCTIONAL_BLOCKS("functional-blocks", "功能方块", Material.CRAFTING_TABLE),
    REDSTONE_BLOCKS("redstone-blocks", "红石方块", Material.REDSTONE),
    TOOLS_AND_UTILITIES("tools-and-utilities", "工具与实用物品", Material.DIAMOND_PICKAXE),
    COMBAT("combat", "战斗物品", Material.DIAMOND_SWORD),
    FOOD_AND_DRINKS("food-and-drinks", "食物与饮品", Material.GOLDEN_APPLE),
    RAW_MATERIALS("raw-materials", "原材料", Material.IRON_INGOT),
    SPAWN_EGGS("spawn-eggs", "刷怪蛋", Material.CREEPER_SPAWN_EGG);

    private static final Set<String> COLORS = Set.of(
            "WHITE", "LIGHT_GRAY", "GRAY", "BLACK", "BROWN", "RED", "ORANGE", "YELLOW",
            "LIME", "GREEN", "CYAN", "LIGHT_BLUE", "BLUE", "PURPLE", "MAGENTA", "PINK");
    private static final String[] COLORED_TYPES = {
            "WOOL", "CARPET", "CONCRETE", "CONCRETE_POWDER", "TERRACOTTA", "GLAZED_TERRACOTTA",
            "STAINED_GLASS", "STAINED_GLASS_PANE", "SHULKER_BOX", "BED", "CANDLE", "BANNER"
    };
    private static final String[] NATURAL_MARKERS = {
            "_LOG", "_WOOD", "_STEM", "_HYPHAE", "_LEAVES", "_SAPLING", "_PROPAGULE",
            "_FLOWER", "_TULIP", "_ORCHID", "_CORAL", "_CORAL_BLOCK", "_CORAL_FAN", "_ROOTS",
            "_FUNGUS", "_MUSHROOM", "_MUSHROOM_BLOCK", "_ORE", "_DIRT", "_SAND", "_GRAVEL",
            "STONE", "DEEPSLATE", "TUFF", "CALCITE", "DRIPSTONE", "NETHERRACK", "NYLIUM",
            "BASALT", "BLACKSTONE", "SOUL_SOIL", "SOUL_SAND", "END_STONE", "OBSIDIAN", "ICE",
            "SNOW", "MOSS", "SCULK", "VINE", "GRASS", "FERN", "BUSH", "CACTUS", "BAMBOO",
            "KELP", "SEAGRASS", "LILY_PAD", "CLAY", "MUD", "AMETHYST", "MAGMA_BLOCK"
    };

    private final String id;
    private final String defaultName;
    private final Material defaultIcon;

    ShopCategory(String id, String defaultName, Material defaultIcon) {
        this.id = id;
        this.defaultName = defaultName;
        this.defaultIcon = defaultIcon;
    }

    public String id() {
        return id;
    }

    public String defaultName() {
        return defaultName;
    }

    public Material defaultIcon() {
        return defaultIcon;
    }

    public static ShopCategory fromConfig(String value, ShopCategory fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (ShopCategory category : values()) {
            if (category.id.equals(normalized)
                    || category.name().equalsIgnoreCase(value.trim())) {
                return category;
            }
        }
        return fallback;
    }

    public static ShopCategory classify(Material material) {
        if (material.name().endsWith("_SPAWN_EGG")) {
            return SPAWN_EGGS;
        }
        if (isColoredBlock(material)) {
            return COLORED_BLOCKS;
        }

        CreativeCategory creative = material.getCreativeCategory();
        if (creative == null) {
            return material.isBlock() ? FUNCTIONAL_BLOCKS : RAW_MATERIALS;
        }
        return switch (creative) {
            case BUILDING_BLOCKS -> isNaturalBlock(material) ? NATURAL_BLOCKS : BUILDING_BLOCKS;
            case DECORATIONS -> isNaturalBlock(material) ? NATURAL_BLOCKS : FUNCTIONAL_BLOCKS;
            case REDSTONE -> REDSTONE_BLOCKS;
            case TRANSPORTATION, TOOLS -> TOOLS_AND_UTILITIES;
            case COMBAT -> COMBAT;
            case FOOD, BREWING -> FOOD_AND_DRINKS;
            case MISC -> material.isBlock()
                    ? (isNaturalBlock(material) ? NATURAL_BLOCKS : FUNCTIONAL_BLOCKS)
                    : RAW_MATERIALS;
        };
    }

    private static boolean isColoredBlock(Material material) {
        if (!material.isBlock()) {
            return false;
        }
        String name = material.name();
        for (String color : COLORS) {
            if (name.startsWith(color + "_")) {
                for (String type : COLORED_TYPES) {
                    if (name.endsWith("_" + type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isNaturalBlock(Material material) {
        if (!material.isBlock()) {
            return false;
        }
        String name = material.name();
        if (name.startsWith("POLISHED_") || name.contains("_BRICKS") || name.contains("_SLAB")
                || name.contains("_STAIRS") || name.contains("_WALL") || name.contains("CHISELED_")
                || name.contains("CUT_")) {
            return false;
        }
        for (String marker : NATURAL_MARKERS) {
            if (name.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
