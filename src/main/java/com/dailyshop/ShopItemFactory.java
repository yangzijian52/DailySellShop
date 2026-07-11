package com.dailyshop;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Locale;

public final class ShopItemFactory {

    private ShopItemFactory() {
    }

    public static Material resolveMaterial(FileConfiguration config, String root, String key) {
        String configured = config.getString(root + "." + key + ".material", key);
        return configured == null ? null : Material.getMaterial(configured.toUpperCase(Locale.ROOT));
    }

    public static ItemStack create(FileConfiguration config, String root, String key, int amount) {
        Material material = resolveMaterial(config, root, key);
        if (material == null || !material.isItem()) {
            return null;
        }
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        String path = root + "." + key + ".";
        ItemMeta meta = item.getItemMeta();

        String potionKey = config.getString(path + "potion-type", "");
        if (!potionKey.isBlank() && meta instanceof PotionMeta potionMeta) {
            PotionType potionType = Registry.POTION.get(NamespacedKey.minecraft(potionKey.toLowerCase(Locale.ROOT)));
            if (potionType != null) {
                potionMeta.setBasePotionType(potionType);
                item.setItemMeta(potionMeta);
            }
        }

        String enchantmentKey = config.getString(path + "enchantment", "");
        if (!enchantmentKey.isBlank() && item.getItemMeta() instanceof EnchantmentStorageMeta enchantmentMeta) {
            Enchantment enchantment = Registry.ENCHANTMENT.get(
                    NamespacedKey.minecraft(enchantmentKey.toLowerCase(Locale.ROOT)));
            if (enchantment != null) {
                enchantmentMeta.addStoredEnchant(enchantment, enchantment.getMaxLevel(), true);
                item.setItemMeta(enchantmentMeta);
            }
        }
        return item;
    }
}
