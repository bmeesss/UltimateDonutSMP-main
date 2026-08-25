package com.bx.ultimateDonutSmp.models;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class AuctionListing {
    private final long id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final UUID buyerUuid;
    private final Status status;
    private final double price;
    private final double tax;
    private final ItemStack item;
    private final long createdAt;
    private final long expiresAt;
    private final long soldAt;
    private final long cancelledAt;
    private final long expiredAt;
    private final String category;

    public AuctionListing(long id, UUID sellerUuid, String sellerName, UUID buyerUuid, Status status, double price, double tax, ItemStack item, long createdAt, long expiresAt, long soldAt, long cancelledAt, long expiredAt, String category) {
    }

    public long id() { return id; }
    public UUID sellerUuid() { return sellerUuid; }
    public String sellerName() { return sellerName; }
    public UUID buyerUuid() { return buyerUuid; }
    public Status status() { return status; }
    public double price() { return price; }
    public double tax() { return tax; }
    public ItemStack item() { return item; }
    public long createdAt() { return createdAt; }
    public long expiresAt() { return expiresAt; }
    public long soldAt() { return soldAt; }
    public long cancelledAt() { return cancelledAt; }
    public long expiredAt() { return expiredAt; }
    public String category() { return category; }



    public


    public enum Status {
        ACTIVE,
        SOLD,
        EXPIRED,
        CANCELLED;

        public static Status fromDatabase(String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return ACTIVE;
            }

            try {
                return Status.valueOf(rawValue.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return ACTIVE;
            }
        }
    }

    public boolean active() {
        return status == Status.ACTIVE;
    }

    public boolean sold() {
        return status == Status.SOLD;
    }

    public boolean expired() {
        return status == Status.EXPIRED;
    }

    public boolean cancelled() {
        return status == Status.CANCELLED;
    }

    public double sellerPayout() {
        return Math.max(0D, price - tax);
    }

    public long secondsRemaining(long nowMillis) {
        return Math.max(0L, (expiresAt - nowMillis) / 1000L);
    }
    @Override public String toString() {
        return "AuctionListing[id=+id, sellerUuid=+sellerUuid, sellerName=+sellerName, buyerUuid=+buyerUuid, status=+status, price=+price, tax=+tax, item=+item, createdAt=+createdAt, expiresAt=+expiresAt, soldAt=+soldAt, cancelledAt=+cancelledAt, expiredAt=+expiredAt, category=+category]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionListing that = (AuctionListing) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(sellerUuid, that.sellerUuid) && java.util.Objects.equals(sellerName, that.sellerName) && java.util.Objects.equals(buyerUuid, that.buyerUuid) && java.util.Objects.equals(status, that.status) && java.util.Objects.equals(price, that.price) && java.util.Objects.equals(tax, that.tax) && java.util.Objects.equals(item, that.item) && java.util.Objects.equals(createdAt, that.createdAt) && java.util.Objects.equals(expiresAt, that.expiresAt) && java.util.Objects.equals(soldAt, that.soldAt) && java.util.Objects.equals(cancelledAt, that.cancelledAt) && java.util.Objects.equals(expiredAt, that.expiredAt) && java.util.Objects.equals(category, that.category);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(id, sellerUuid, sellerName, buyerUuid, status, price, tax, item, createdAt, expiresAt, soldAt, cancelledAt, expiredAt, category);
    }
}
