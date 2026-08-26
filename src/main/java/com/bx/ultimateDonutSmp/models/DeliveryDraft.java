package com.bx.ultimateDonutSmp.models;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class DeliveryDraft {
    private final UUID playerUuid;
    private final long orderId;
    private final List<ItemStack> acceptedItems;
    private final int quantity;
    private final double payout;
    private final long createdAt;

    public DeliveryDraft(UUID playerUuid, long orderId, List<ItemStack> acceptedItems, int quantity, double payout, long createdAt) {
        acceptedItems = copy(acceptedItems);
        this.playerUuid = playerUuid;
        this.orderId = orderId;
        this.acceptedItems = acceptedItems;
        this.quantity = quantity;
        this.payout = payout;
        this.createdAt = createdAt;
    }

    public UUID playerUuid() { return playerUuid; }
    public long orderId() { return orderId; }
    public List<ItemStack> acceptedItems() { return acceptedItems; }
    public int quantity() { return quantity; }
    public double payout() { return payout; }
    public long createdAt() { return createdAt; }




    private static List<ItemStack> copy(List<ItemStack> items) {
        return items == null ? java.util.Collections.emptyList() : items.stream()
                .filter(java.util.Objects::nonNull)
                .map(ItemStack::clone)
                .collect(java.util.stream.Collectors.toList());
    }
    @Override public String toString() {
        return "DeliveryDraft[playerUuid=+playerUuid, orderId=+orderId, acceptedItems=+acceptedItems, quantity=+quantity, payout=+payout, createdAt=+createdAt]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryDraft that = (DeliveryDraft) o;
        return java.util.Objects.equals(playerUuid, that.playerUuid) && java.util.Objects.equals(orderId, that.orderId) && java.util.Objects.equals(acceptedItems, that.acceptedItems) && java.util.Objects.equals(quantity, that.quantity) && java.util.Objects.equals(payout, that.payout) && java.util.Objects.equals(createdAt, that.createdAt);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(playerUuid, orderId, acceptedItems, quantity, payout, createdAt);
    }
}
