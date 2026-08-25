package com.bx.ultimateDonutSmp.models;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class DeliveryQuote {
    private final boolean success;
    private final String failureCode;
    private final Order order;
    private final List<ItemStack> acceptedItems;
    private final List<ItemStack> returnedItems;
    private final int quantity;
    private final double payout;

    public DeliveryQuote(boolean success, String failureCode, Order order, List<ItemStack> acceptedItems, List<ItemStack> returnedItems, int quantity, double payout) {
        this.success = success;
        this.failureCode = failureCode;
        this.order = order;
        this.acceptedItems = acceptedItems;
        this.returnedItems = returnedItems;
        this.quantity = quantity;
        this.payout = payout;
    }

    public boolean success() { return success; }
    public String failureCode() { return failureCode; }
    public Order order() { return order; }
    public List<ItemStack> acceptedItems() { return acceptedItems; }
    public List<ItemStack> returnedItems() { return returnedItems; }
    public int quantity() { return quantity; }
    public double payout() { return payout; }


    public DeliveryQuote {
        acceptedItems = copy(acceptedItems);
        returnedItems = copy(returnedItems);
    }

    private static List<ItemStack> copy(List<ItemStack> items) {
        return items == null ? java.util.Collections.emptyList() : items.stream()
                .filter(java.util.Objects::nonNull)
                .map(ItemStack::clone)
                .collect(java.util.stream.Collectors.toList());
    }
    @Override public String toString() {
        return "DeliveryQuote[success=+success, failureCode=+failureCode, order=+order, acceptedItems=+acceptedItems, returnedItems=+returnedItems, quantity=+quantity, payout=+payout]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryQuote that = (DeliveryQuote) o;
        return java.util.Objects.equals(success, that.success) && java.util.Objects.equals(failureCode, that.failureCode) && java.util.Objects.equals(order, that.order) && java.util.Objects.equals(acceptedItems, that.acceptedItems) && java.util.Objects.equals(returnedItems, that.returnedItems) && java.util.Objects.equals(quantity, that.quantity) && java.util.Objects.equals(payout, that.payout);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(success, failureCode, order, acceptedItems, returnedItems, quantity, payout);
    }
}
