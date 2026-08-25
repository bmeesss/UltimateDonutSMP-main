package com.bx.ultimateDonutSmp.models;

import java.util.UUID;

public final class EconomyTransactionResult {
    private final boolean success;
    private final EconomyFailureReason failureReason;
    private final EconomyReason reason;
    private final UUID targetUuid;
    private final String displayName;
    private final double amount;
    private final double beforeBalance;
    private final double afterBalance;

    public EconomyTransactionResult(boolean success, EconomyFailureReason failureReason, EconomyReason reason, UUID targetUuid, String displayName, double amount, double beforeBalance, double afterBalance) {
        this.success = success;
        this.failureReason = failureReason;
        this.reason = reason;
        this.targetUuid = targetUuid;
        this.displayName = displayName;
        this.amount = amount;
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
    }

    public boolean success() { return success; }
    public EconomyFailureReason failureReason() { return failureReason; }
    public EconomyReason reason() { return reason; }
    public UUID targetUuid() { return targetUuid; }
    public String displayName() { return displayName; }
    public double amount() { return amount; }
    public double beforeBalance() { return beforeBalance; }
    public double afterBalance() { return afterBalance; }



    public boolean invalidAmount() {
        return failureReason == EconomyFailureReason.INVALID_AMOUNT;
    }

    public boolean playerNotFound() {
        return failureReason == EconomyFailureReason.PLAYER_NOT_FOUND;
    }

    public boolean noPlayerData() {
        return failureReason == EconomyFailureReason.NO_PLAYER_DATA;
    }

    public boolean insufficientFunds() {
        return failureReason == EconomyFailureReason.INSUFFICIENT_FUNDS;
    }
    @Override public String toString() {
        return "EconomyTransactionResult[success=+success, failureReason=+failureReason, reason=+reason, targetUuid=+targetUuid, displayName=+displayName, amount=+amount, beforeBalance=+beforeBalance, afterBalance=+afterBalance]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EconomyTransactionResult that = (EconomyTransactionResult) o;
        return java.util.Objects.equals(success, that.success) && java.util.Objects.equals(failureReason, that.failureReason) && java.util.Objects.equals(reason, that.reason) && java.util.Objects.equals(targetUuid, that.targetUuid) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(amount, that.amount) && java.util.Objects.equals(beforeBalance, that.beforeBalance) && java.util.Objects.equals(afterBalance, that.afterBalance);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(success, failureReason, reason, targetUuid, displayName, amount, beforeBalance, afterBalance);
    }
}
