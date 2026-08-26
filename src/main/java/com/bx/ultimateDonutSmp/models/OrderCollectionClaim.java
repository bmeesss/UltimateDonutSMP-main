package com.bx.ultimateDonutSmp.models;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class OrderCollectionClaim {
    private final long id;
    private final UUID ownerUuid;
    private final long orderId;
    private final ClaimType claimType;
    private final ItemStack item;
    private final double moneyAmount;
    private final long createdAt;
    private final long claimedAt;

    public OrderCollectionClaim(long id, UUID ownerUuid, long orderId, ClaimType claimType, ItemStack item, double moneyAmount, long createdAt, long claimedAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.orderId = orderId;
        this.claimType = claimType;
        this.item = item;
        this.moneyAmount = moneyAmount;
        this.createdAt = createdAt;
        this.claimedAt = claimedAt;
    }

    public long id() { return id; }
    public UUID ownerUuid() { return ownerUuid; }
    public long orderId() { return orderId; }
    public ClaimType claimType() { return claimType; }
    public ItemStack item() { return item; }
    public double moneyAmount() { return moneyAmount; }
    public long createdAt() { return createdAt; }
    public long claimedAt() { return claimedAt; }



    public enum ClaimType {
        ITEM,
        REFUND;

        public static ClaimType fromDatabase(String rawValue) {
            if (rawValue == null || rawValue.trim().isEmpty()) {
                return ITEM;
            }

            try {
                return ClaimType.valueOf(rawValue.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return ITEM;
            }
        }
    }

    public boolean itemClaim() {
        return claimType == ClaimType.ITEM;
    }

    public boolean refundClaim() {
        return claimType == ClaimType.REFUND;
    }

    public boolean claimed() {
        return claimedAt > 0L;
    }
    @Override public String toString() {
        return "OrderCollectionClaim[id=+id, ownerUuid=+ownerUuid, orderId=+orderId, claimType=+claimType, item=+item, moneyAmount=+moneyAmount, createdAt=+createdAt, claimedAt=+claimedAt]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderCollectionClaim that = (OrderCollectionClaim) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(ownerUuid, that.ownerUuid) && java.util.Objects.equals(orderId, that.orderId) && java.util.Objects.equals(claimType, that.claimType) && java.util.Objects.equals(item, that.item) && java.util.Objects.equals(moneyAmount, that.moneyAmount) && java.util.Objects.equals(createdAt, that.createdAt) && java.util.Objects.equals(claimedAt, that.claimedAt);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(id, ownerUuid, orderId, claimType, item, moneyAmount, createdAt, claimedAt);
    }
}
