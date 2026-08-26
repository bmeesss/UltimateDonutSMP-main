package com.bx.ultimateDonutSmp.models;

public enum OrderStatus {
    ACTIVE,
    FILLED,
    EXPIRED,
    CANCELLED;

    public static OrderStatus fromDatabase(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return ACTIVE;
        }

        try {
            return OrderStatus.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ACTIVE;
        }
    }
}
