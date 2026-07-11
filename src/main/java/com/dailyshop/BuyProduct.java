package com.dailyshop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public record BuyProduct(
        String key,
        Material material,
        String displayName,
        BigDecimal price,
        ShopCategory category,
        boolean spawnEgg,
        int weight,
        int maxAmount,
        String permission,
        ItemStack template) {

    public boolean hasPermission() {
        return permission != null && !permission.isBlank();
    }

    public ItemStack createItem(int amount) {
        ItemStack item = template.clone();
        item.setAmount(amount);
        return item;
    }
}
