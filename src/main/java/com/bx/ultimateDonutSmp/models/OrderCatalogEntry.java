package com.bx.ultimateDonutSmp.models;

import org.bukkit.inventory.ItemStack;

public final class OrderCatalogEntry {
    private final String categoryKey;
    private final ItemStack previewItem;
    private final String displayName;
    private final String searchText;

    public OrderCatalogEntry(String categoryKey, ItemStack previewItem, String displayName, String searchText) {
        this.categoryKey = categoryKey;
        this.previewItem = previewItem == null ? null : previewItem.clone();
        if (this.previewItem != null) {
            this.previewItem.setAmount(1);
        }
        this.displayName = displayName == null ? "" : displayName;
        this.searchText = searchText == null ? "" : searchText;
    }

    public String categoryKey() { return categoryKey; }
    public ItemStack previewItem() { return previewItem; }
    public String displayName() { return displayName; }
    public String searchText() { return searchText; }

    public org.bukkit.Material material() {
        return previewItem == null ? org.bukkit.Material.AIR : previewItem.getType();
    }

    public ItemStack createPreviewItem() {
        return previewItem == null ? null : previewItem.clone();
    }

    @Override public String toString() {
        return "OrderCatalogEntry[categoryKey=" + categoryKey + ", previewItem=" + previewItem + ", displayName=" + displayName + ", searchText=" + searchText + "]";
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderCatalogEntry that = (OrderCatalogEntry) o;
        return java.util.Objects.equals(categoryKey, that.categoryKey) && java.util.Objects.equals(previewItem, that.previewItem) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(searchText, that.searchText);
    }

    @Override public int hashCode() {
        return java.util.Objects.hash(categoryKey, previewItem, displayName, searchText);
    }
}
