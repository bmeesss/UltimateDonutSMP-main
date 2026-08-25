package com.bx.ultimateDonutSmp.models;

import java.util.UUID;

public final class OrderDelivery {
    private final long id;
    private final long orderId;
    private final UUID delivererUuid;
    private final String delivererName;
    private final int quantity;
    private final double payout;
    private final long createdAt;

    public OrderDelivery(long id, long orderId, UUID delivererUuid, String delivererName, int quantity, double payout, long createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.delivererUuid = delivererUuid;
        this.delivererName = delivererName;
        this.quantity = quantity;
        this.payout = payout;
        this.createdAt = createdAt;
    }

    public long id() { return id; }
    public long orderId() { return orderId; }
    public UUID delivererUuid() { return delivererUuid; }
    public String delivererName() { return delivererName; }
    public int quantity() { return quantity; }
    public double payout() { return payout; }
    public long createdAt() { return createdAt; }

    @Override public String toString() {
        return "OrderDelivery[id=+id, orderId=+orderId, delivererUuid=+delivererUuid, delivererName=+delivererName, quantity=+quantity, payout=+payout, createdAt=+createdAt]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderDelivery that = (OrderDelivery) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(orderId, that.orderId) && java.util.Objects.equals(delivererUuid, that.delivererUuid) && java.util.Objects.equals(delivererName, that.delivererName) && java.util.Objects.equals(quantity, that.quantity) && java.util.Objects.equals(payout, that.payout) && java.util.Objects.equals(createdAt, that.createdAt);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(id, orderId, delivererUuid, delivererName, quantity, payout, createdAt);
    }
}
