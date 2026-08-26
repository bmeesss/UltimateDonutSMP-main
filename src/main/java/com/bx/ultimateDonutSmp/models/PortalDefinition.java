package com.bx.ultimateDonutSmp.models;

import java.util.Locale;

public final class PortalDefinition {
    private final String id;
    private final String displayName;
    private final String cuboidName;
    private final String destinationType;
    private final String destinationValue;
    private final boolean enabled;
    private final String permission;
    private final int priority;
    private final long triggerCooldownMillis;
    private final String enterMessage;
    private final String hologramWorld;
    private final double hologramX;
    private final double hologramY;
    private final double hologramZ;

    public PortalDefinition(String id, String displayName, String cuboidName, String destinationType, String destinationValue, boolean enabled, String permission, int priority, long triggerCooldownMillis, String enterMessage, String hologramWorld, double hologramX, double hologramY, double hologramZ) {
        id = normalizeId(id);
        displayName = normalizeText(displayName);
        cuboidName = normalizeToken(cuboidName);
        destinationType = normalizeToken(destinationType).toUpperCase(Locale.ROOT);
        destinationValue = normalizeToken(destinationValue);
        permission = normalizeText(permission);
        triggerCooldownMillis = Math.max(0L, triggerCooldownMillis);
        enterMessage = normalizeText(enterMessage);
        hologramWorld = normalizeToken(hologramWorld);
        this.id = id;
        this.displayName = displayName;
        this.cuboidName = cuboidName;
        this.destinationType = destinationType;
        this.destinationValue = destinationValue;
        this.enabled = enabled;
        this.permission = permission;
        this.priority = priority;
        this.triggerCooldownMillis = triggerCooldownMillis;
        this.enterMessage = enterMessage;
        this.hologramWorld = hologramWorld;
        this.hologramX = hologramX;
        this.hologramY = hologramY;
        this.hologramZ = hologramZ;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String cuboidName() { return cuboidName; }
    public String destinationType() { return destinationType; }
    public String destinationValue() { return destinationValue; }
    public boolean enabled() { return enabled; }
    public String permission() { return permission; }
    public int priority() { return priority; }
    public long triggerCooldownMillis() { return triggerCooldownMillis; }
    public String enterMessage() { return enterMessage; }
    public String hologramWorld() { return hologramWorld; }
    public double hologramX() { return hologramX; }
    public double hologramY() { return hologramY; }
    public double hologramZ() { return hologramZ; }





    public String effectiveDisplayName() {
        return displayName.trim().isEmpty() ? id : displayName;
    }

    public PortalDefinition withDisplayName(String value) {
        return new PortalDefinition(id, value, cuboidName, destinationType, destinationValue, enabled,
                permission, priority, triggerCooldownMillis, enterMessage, hologramWorld, hologramX, hologramY, hologramZ);
    }

    public PortalDefinition withCuboidName(String value) {
        return new PortalDefinition(id, displayName, value, destinationType, destinationValue, enabled,
                permission, priority, triggerCooldownMillis, enterMessage, hologramWorld, hologramX, hologramY, hologramZ);
    }

    public PortalDefinition withDestination(String type, String value) {
        return new PortalDefinition(id, displayName, cuboidName, type, value, enabled,
                permission, priority, triggerCooldownMillis, enterMessage, hologramWorld, hologramX, hologramY, hologramZ);
    }

    public PortalDefinition withEnabled(boolean value) {
        return new PortalDefinition(id, displayName, cuboidName, destinationType, destinationValue, value,
                permission, priority, triggerCooldownMillis, enterMessage, hologramWorld, hologramX, hologramY, hologramZ);
    }

    public PortalDefinition withPriority(int value) {
        return new PortalDefinition(id, displayName, cuboidName, destinationType, destinationValue, enabled,
                permission, value, triggerCooldownMillis, enterMessage, hologramWorld, hologramX, hologramY, hologramZ);
    }

    public PortalDefinition withPermission(String value) {
        return new PortalDefinition(id, displayName, cuboidName, destinationType, destinationValue, enabled,
                value, priority, triggerCooldownMillis, enterMessage, hologramWorld, hologramX, hologramY, hologramZ);
    }

    public PortalDefinition withTriggerCooldownMillis(long value) {
        return new PortalDefinition(id, displayName, cuboidName, destinationType, destinationValue, enabled,
                permission, priority, value, enterMessage, hologramWorld, hologramX, hologramY, hologramZ);
    }

    public PortalDefinition withEnterMessage(String value) {
        return new PortalDefinition(id, displayName, cuboidName, destinationType, destinationValue, enabled,
                permission, priority, triggerCooldownMillis, value, hologramWorld, hologramX, hologramY, hologramZ);
    }

    public PortalDefinition withHologramLocation(String world, double x, double y, double z) {
        return new PortalDefinition(id, displayName, cuboidName, destinationType, destinationValue, enabled,
                permission, priority, triggerCooldownMillis, enterMessage, world, x, y, z);
    }

    public boolean hasCustomHologramLocation() {
        return !hologramWorld.trim().isEmpty();
    }

    private static String normalizeId(String value) {
        String normalized = normalizeToken(value);
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
    @Override public String toString() {
        return "PortalDefinition[id=+id, displayName=+displayName, cuboidName=+cuboidName, destinationType=+destinationType, destinationValue=+destinationValue, enabled=+enabled, permission=+permission, priority=+priority, triggerCooldownMillis=+triggerCooldownMillis, enterMessage=+enterMessage, hologramWorld=+hologramWorld, hologramX=+hologramX, hologramY=+hologramY, hologramZ=+hologramZ]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalDefinition that = (PortalDefinition) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(cuboidName, that.cuboidName) && java.util.Objects.equals(destinationType, that.destinationType) && java.util.Objects.equals(destinationValue, that.destinationValue) && java.util.Objects.equals(enabled, that.enabled) && java.util.Objects.equals(permission, that.permission) && java.util.Objects.equals(priority, that.priority) && java.util.Objects.equals(triggerCooldownMillis, that.triggerCooldownMillis) && java.util.Objects.equals(enterMessage, that.enterMessage) && java.util.Objects.equals(hologramWorld, that.hologramWorld) && java.util.Objects.equals(hologramX, that.hologramX) && java.util.Objects.equals(hologramY, that.hologramY) && java.util.Objects.equals(hologramZ, that.hologramZ);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(id, displayName, cuboidName, destinationType, destinationValue, enabled, permission, priority, triggerCooldownMillis, enterMessage, hologramWorld, hologramX, hologramY, hologramZ);
    }
}
