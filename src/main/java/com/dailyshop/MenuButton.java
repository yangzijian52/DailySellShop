package com.dailyshop;

import java.util.List;

public class MenuButton {

    private final String id;
    private final int slot;
    private final String material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final List<String> commands;
    private final String permission;
    private final boolean glow;
    private final boolean hideFlags;
    private final Integer customModelData;

    public MenuButton(
            String id,
            int slot,
            String material,
            int amount,
            String name,
            List<String> lore,
            List<String> commands,
            String permission,
            boolean glow,
            boolean hideFlags,
            Integer customModelData) {
        this.id = id;
        this.slot = slot;
        this.material = material;
        this.amount = amount;
        this.name = name;
        this.lore = lore;
        this.commands = commands;
        this.permission = permission;
        this.glow = glow;
        this.hideFlags = hideFlags;
        this.customModelData = customModelData;
    }

    public String getId() {
        return id;
    }

    public int getSlot() {
        return slot;
    }

    public String getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<String> getCommands() {
        return commands;
    }

    public String getPermission() {
        return permission;
    }

    public boolean hasPermission() {
        return permission != null && !permission.isBlank();
    }

    public boolean isGlow() {
        return glow;
    }

    public boolean isHideFlags() {
        return hideFlags;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }
}
