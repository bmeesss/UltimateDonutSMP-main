package com.bx.ultimateDonutSmp.models;

public final class OrderBatchClaimResult {
    private final int itemClaims;
    private final int refundClaims;
    private final int failedClaims;
    private final int itemAmount;
    private final double refundAmount;

    public OrderBatchClaimResult(int itemClaims, int refundClaims, int failedClaims, int itemAmount, double refundAmount) {
        this.itemClaims = itemClaims;
        this.refundClaims = refundClaims;
        this.failedClaims = failedClaims;
        this.itemAmount = itemAmount;
        this.refundAmount = refundAmount;
    }

    public int itemClaims() { return itemClaims; }
    public int refundClaims() { return refundClaims; }
    public int failedClaims() { return failedClaims; }
    public int itemAmount() { return itemAmount; }
    public double refundAmount() { return refundAmount; }

    @Override public String toString() {
        return "OrderBatchClaimResult[itemClaims=+itemClaims, refundClaims=+refundClaims, failedClaims=+failedClaims, itemAmount=+itemAmount, refundAmount=+refundAmount]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderBatchClaimResult that = (OrderBatchClaimResult) o;
        return java.util.Objects.equals(itemClaims, that.itemClaims) && java.util.Objects.equals(refundClaims, that.refundClaims) && java.util.Objects.equals(failedClaims, that.failedClaims) && java.util.Objects.equals(itemAmount, that.itemAmount) && java.util.Objects.equals(refundAmount, that.refundAmount);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(itemClaims, refundClaims, failedClaims, itemAmount, refundAmount);
    }
}
