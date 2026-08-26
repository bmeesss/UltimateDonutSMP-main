package com.bx.ultimateDonutSmp.models;

public enum PunishmentFilterState {
    ALL("All"),
    ACTIVE("Active"),
    INACTIVE("inactive");

    private final String displayName;

    PunishmentFilterState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PunishmentFilterState next() {
        switch (this) {
            case ALL:
                return ACTIVE;
            case ACTIVE:
                return INACTIVE;
            case INACTIVE:
                return ALL;
            default:
                return null;
        }
    }
}
