package com.bx.ultimateDonutSmp.models;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class DeliveryRequest {
    private final long orderId;
    private final List<ItemStack> items;
    private final int expectedQuantity;
    private final double expectedPriceEach;

    public DeliveryRequest(long orderId, List<ItemStack> items, int expectedQuantity, double expectedPriceEach) {
        this.orderId = orderId;
        this.items = items;
        this.expectedQuantity = expectedQuantity;
        this.expectedPriceEach = expectedPriceEach;
    }

    public long orderId() { return orderId; }
    public List<ItemStack> items() { return items; }
    public int expectedQuantity() { return expectedQuantity; }
    public double expectedPriceEach() { return expectedPriceEach; }


    public DeliveryRequest {
        items = items == null ? java.util.Collections.emptyList() : items.stream()
                .filter(java.util.Objects::nonNull)
                .map(ItemStack::clone)
                .collect(java.util.stream.Collectors.toList());
    }
    @Override public String toString() {
        return "DeliveryRequest[orderId=+orderId, items=+items, expectedQuantity=+expectedQuantity, expectedPriceEach=+expectedPriceEach]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryRequest that = (DeliveryRequest) o;
        return java.util.Objects.equals(orderId, that.orderId) && java.util.Objects.equals(items, that.items) && java.util.Objects.equals(expectedQuantity, that.expectedQuantity) && java.util.Objects.equals(expectedPriceEach, that.expectedPriceEach);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(orderId, items, expectedQuantity, expectedPriceEach);
    }
}
