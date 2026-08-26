package com.bx.ultimateDonutSmp.models;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class AuctionClaim {
    private final long id;
    private final UUID ownerUuid;
    private final ClaimType claimType;
    private final long sourceListingId;
    private final double moneyAmount;
    private final ItemStack item;
    private final long createdAt;
    private final long claimedAt;

    public AuctionClaim(long id, UUID ownerUuid, ClaimType claimType, long sourceListingId, double moneyAmount, ItemStack item, long createdAt, long claimedAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.claimType = claimType;
        this.sourceListingId = sourceListingId;
        this.moneyAmount = moneyAmount;
        this.item = item;
        this.createdAt = createdAt;
        this.claimedAt = claimedAt;
    }

    public long id() { return id; }
    public UUID ownerUuid() { return ownerUuid; }
    public ClaimType claimType() { return claimType; }
    public long sourceListingId() { return sourceListingId; }
    public double moneyAmount() { return moneyAmount; }
    public ItemStack item() { return item; }
    public long createdAt() { return createdAt; }
    public long claimedAt() { return claimedAt; }



    public enum ClaimType {
        MONEY,
        ITEM;

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

    public boolean claimed() {
        return claimedAt > 0L;
    }

    public boolean moneyClaim() {
        return claimType == ClaimType.MONEY;
    }

    public boolean itemClaim() {
        return claimType == ClaimType.ITEM;
    }
    @Override public String toString() {
        return "AuctionClaim[id=+id, ownerUuid=+ownerUuid, claimType=+claimType, sourceListingId=+sourceListingId, moneyAmount=+moneyAmount, item=+item, createdAt=+createdAt, claimedAt=+claimedAt]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionClaim that = (AuctionClaim) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(ownerUuid, that.ownerUuid) && java.util.Objects.equals(claimType, that.claimType) && java.util.Objects.equals(sourceListingId, that.sourceListingId) && java.util.Objects.equals(moneyAmount, that.moneyAmount) && java.util.Objects.equals(item, that.item) && java.util.Objects.equals(createdAt, that.createdAt) && java.util.Objects.equals(claimedAt, that.claimedAt);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(id, ownerUuid, claimType, sourceListingId, moneyAmount, item, createdAt, claimedAt);
    }
}
