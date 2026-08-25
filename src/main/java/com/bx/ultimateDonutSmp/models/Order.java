package com.bx.ultimateDonutSmp.models;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class Order {
    private final long id;
    private final UUID ownerUuid;
    private final String ownerName;
    private final ItemStack requestedItem;
    private final String requestedMaterialKey;
    private final String categoryKey;
    private final OrderStatus status;
    private final int requestedQuantity;
    private final int deliveredQuantity;
    private final int collectedQuantity;
    private final double priceEach;
    private final double totalBudget;
    private final double paidAmount;
    private final double escrowRemaining;
    private final long createdAt;
    private final long expiresAt;
    private final long closedAt;

    public Order(long id, UUID ownerUuid, String ownerName, ItemStack requestedItem, String requestedMaterialKey, String categoryKey, OrderStatus status, int requestedQuantity, int deliveredQuantity, int collectedQuantity, double priceEach, double totalBudget, double paidAmount, double escrowRemaining, long createdAt, long expiresAt, long closedAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.requestedItem = requestedItem;
        this.requestedMaterialKey = requestedMaterialKey;
        this.categoryKey = categoryKey;
        this.status = status;
        this.requestedQuantity = requestedQuantity;
        this.deliveredQuantity = deliveredQuantity;
        this.collectedQuantity = collectedQuantity;
        this.priceEach = priceEach;
        this.totalBudget = totalBudget;
        this.paidAmount = paidAmount;
        this.escrowRemaining = escrowRemaining;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.closedAt = closedAt;
    }

    public long id() { return id; }
    public UUID ownerUuid() { return ownerUuid; }
    public String ownerName() { return ownerName; }
    public ItemStack requestedItem() { return requestedItem; }
    public String requestedMaterialKey() { return requestedMaterialKey; }
    public String categoryKey() { return categoryKey; }
    public OrderStatus status() { return status; }
    public int requestedQuantity() { return requestedQuantity; }
    public int deliveredQuantity() { return deliveredQuantity; }
    public int collectedQuantity() { return collectedQuantity; }
    public double priceEach() { return priceEach; }
    public double totalBudget() { return totalBudget; }
    public double paidAmount() { return paidAmount; }
    public double escrowRemaining() { return escrowRemaining; }
    public long createdAt() { return createdAt; }
    public long expiresAt() { return expiresAt; }
    public long closedAt() { return closedAt; }



    public boolean active() {
        return status == OrderStatus.ACTIVE;
    }

    public boolean filled() {
        return status == OrderStatus.FILLED;
    }

    public boolean expired() {
        return status == OrderStatus.EXPIRED;
    }

    public boolean cancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public boolean closed() {
        return status != OrderStatus.ACTIVE;
    }

    public int remainingQuantity() {
        return Math.max(0, requestedQuantity - deliveredQuantity);
    }

    public long secondsRemaining(long nowMillis) {
        return Math.max(0L, (expiresAt - nowMillis) / 1000L);
    }

    public double progressPercent() {
        if (requestedQuantity <= 0) {
            return 0D;
        }
        return Math.min(100D, (deliveredQuantity * 100D) / requestedQuantity);
    }
    @Override public String toString() {
        return "Order[id=+id, ownerUuid=+ownerUuid, ownerName=+ownerName, requestedItem=+requestedItem, requestedMaterialKey=+requestedMaterialKey, categoryKey=+categoryKey, status=+status, requestedQuantity=+requestedQuantity, deliveredQuantity=+deliveredQuantity, collectedQuantity=+collectedQuantity, priceEach=+priceEach, totalBudget=+totalBudget, paidAmount=+paidAmount, escrowRemaining=+escrowRemaining, createdAt=+createdAt, expiresAt=+expiresAt, closedAt=+closedAt]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order that = (Order) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(ownerUuid, that.ownerUuid) && java.util.Objects.equals(ownerName, that.ownerName) && java.util.Objects.equals(requestedItem, that.requestedItem) && java.util.Objects.equals(requestedMaterialKey, that.requestedMaterialKey) && java.util.Objects.equals(categoryKey, that.categoryKey) && java.util.Objects.equals(status, that.status) && java.util.Objects.equals(requestedQuantity, that.requestedQuantity) && java.util.Objects.equals(deliveredQuantity, that.deliveredQuantity) && java.util.Objects.equals(collectedQuantity, that.collectedQuantity) && java.util.Objects.equals(priceEach, that.priceEach) && java.util.Objects.equals(totalBudget, that.totalBudget) && java.util.Objects.equals(paidAmount, that.paidAmount) && java.util.Objects.equals(escrowRemaining, that.escrowRemaining) && java.util.Objects.equals(createdAt, that.createdAt) && java.util.Objects.equals(expiresAt, that.expiresAt) && java.util.Objects.equals(closedAt, that.closedAt);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(id, ownerUuid, ownerName, requestedItem, requestedMaterialKey, categoryKey, status, requestedQuantity, deliveredQuantity, collectedQuantity, priceEach, totalBudget, paidAmount, escrowRemaining, createdAt, expiresAt, closedAt);
    }
}
