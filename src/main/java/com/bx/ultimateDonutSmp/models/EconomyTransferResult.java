package com.bx.ultimateDonutSmp.models;

import java.util.UUID;

public final class EconomyTransferResult {
    private final boolean success;
    private final EconomyFailureReason failureReason;
    private final EconomyReason reason;
    private final UUID senderUuid;
    private final String senderName;
    private final UUID recipientUuid;
    private final String recipientName;
    private final double amount;
    private final double senderBeforeBalance;
    private final double senderAfterBalance;
    private final double recipientBeforeBalance;
    private final double recipientAfterBalance;

    public EconomyTransferResult(boolean success, EconomyFailureReason failureReason, EconomyReason reason, UUID senderUuid, String senderName, UUID recipientUuid, String recipientName, double amount, double senderBeforeBalance, double senderAfterBalance, double recipientBeforeBalance, double recipientAfterBalance) {
        this.success = success;
        this.failureReason = failureReason;
        this.reason = reason;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.recipientUuid = recipientUuid;
        this.recipientName = recipientName;
        this.amount = amount;
        this.senderBeforeBalance = senderBeforeBalance;
        this.senderAfterBalance = senderAfterBalance;
        this.recipientBeforeBalance = recipientBeforeBalance;
        this.recipientAfterBalance = recipientAfterBalance;
    }

    public boolean success() { return success; }
    public EconomyFailureReason failureReason() { return failureReason; }
    public EconomyReason reason() { return reason; }
    public UUID senderUuid() { return senderUuid; }
    public String senderName() { return senderName; }
    public UUID recipientUuid() { return recipientUuid; }
    public String recipientName() { return recipientName; }
    public double amount() { return amount; }
    public double senderBeforeBalance() { return senderBeforeBalance; }
    public double senderAfterBalance() { return senderAfterBalance; }
    public double recipientBeforeBalance() { return recipientBeforeBalance; }
    public double recipientAfterBalance() { return recipientAfterBalance; }



    public boolean invalidAmount() {
        return failureReason == EconomyFailureReason.INVALID_AMOUNT;
    }

    public boolean insufficientFunds() {
        return failureReason == EconomyFailureReason.INSUFFICIENT_FUNDS;
    }

    public boolean sameAccount() {
        return failureReason == EconomyFailureReason.SAME_ACCOUNT;
    }

    public boolean playerNotFound() {
        return failureReason == EconomyFailureReason.PLAYER_NOT_FOUND;
    }

    public boolean noPlayerData() {
        return failureReason == EconomyFailureReason.NO_PLAYER_DATA;
    }
    @Override public String toString() {
        return "EconomyTransferResult[success=+success, failureReason=+failureReason, reason=+reason, senderUuid=+senderUuid, senderName=+senderName, recipientUuid=+recipientUuid, recipientName=+recipientName, amount=+amount, senderBeforeBalance=+senderBeforeBalance, senderAfterBalance=+senderAfterBalance, recipientBeforeBalance=+recipientBeforeBalance, recipientAfterBalance=+recipientAfterBalance]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EconomyTransferResult that = (EconomyTransferResult) o;
        return java.util.Objects.equals(success, that.success) && java.util.Objects.equals(failureReason, that.failureReason) && java.util.Objects.equals(reason, that.reason) && java.util.Objects.equals(senderUuid, that.senderUuid) && java.util.Objects.equals(senderName, that.senderName) && java.util.Objects.equals(recipientUuid, that.recipientUuid) && java.util.Objects.equals(recipientName, that.recipientName) && java.util.Objects.equals(amount, that.amount) && java.util.Objects.equals(senderBeforeBalance, that.senderBeforeBalance) && java.util.Objects.equals(senderAfterBalance, that.senderAfterBalance) && java.util.Objects.equals(recipientBeforeBalance, that.recipientBeforeBalance) && java.util.Objects.equals(recipientAfterBalance, that.recipientAfterBalance);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(success, failureReason, reason, senderUuid, senderName, recipientUuid, recipientName, amount, senderBeforeBalance, senderAfterBalance, recipientBeforeBalance, recipientAfterBalance);
    }
}
