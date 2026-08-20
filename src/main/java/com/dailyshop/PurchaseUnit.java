package com.dailyshop;

public enum PurchaseUnit {
    ITEM("item", "个"),
    STACK("stack", "组"),
    BOX("box", "盒");

    private final String id;
    private final String displayName;

    PurchaseUnit(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int itemAmount(BuyProduct product, int count) {
        int stackSize = product.createItem(1).getMaxStackSize();
        return switch (this) {
            case ITEM -> count;
            case STACK -> Math.multiplyExact(count, stackSize);
            case BOX -> Math.multiplyExact(count, Math.multiplyExact(stackSize, 27));
        };
    }

    public static PurchaseUnit fromId(String value) {
        if (value != null) {
            for (PurchaseUnit unit : values()) {
                if (unit.id.equalsIgnoreCase(value)) {
                    return unit;
                }
            }
        }
        return ITEM;
    }
}
