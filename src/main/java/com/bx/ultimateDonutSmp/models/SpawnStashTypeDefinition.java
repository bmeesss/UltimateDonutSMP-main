package com.bx.ultimateDonutSmp.models;

import java.util.List;

public final class SpawnStashTypeDefinition {
    private final String key;
    private final String displayName;
    private final long ttlSeconds;
    private final double alertRadius;
    private final SpawnStashOffset pasteOffset;
    private final List<SpawnStashBlockDefinition> blocks;

    public SpawnStashTypeDefinition(String key, String displayName, long ttlSeconds, double alertRadius, SpawnStashOffset pasteOffset, List<SpawnStashBlockDefinition> blocks) {
        this.key = key;
        this.displayName = displayName;
        this.ttlSeconds = ttlSeconds;
        this.alertRadius = alertRadius;
        this.pasteOffset = pasteOffset;
        this.blocks = blocks;
    }

    public String key() { return key; }
    public String displayName() { return displayName; }
    public long ttlSeconds() { return ttlSeconds; }
    public double alertRadius() { return alertRadius; }
    public SpawnStashOffset pasteOffset() { return pasteOffset; }
    public List<SpawnStashBlockDefinition> blocks() { return blocks; }

    @Override public String toString() {
        return "SpawnStashTypeDefinition[key=+key, displayName=+displayName, ttlSeconds=+ttlSeconds, alertRadius=+alertRadius, pasteOffset=+pasteOffset, blocks=+blocks]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpawnStashTypeDefinition that = (SpawnStashTypeDefinition) o;
        return java.util.Objects.equals(key, that.key) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(ttlSeconds, that.ttlSeconds) && java.util.Objects.equals(alertRadius, that.alertRadius) && java.util.Objects.equals(pasteOffset, that.pasteOffset) && java.util.Objects.equals(blocks, that.blocks);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(key, displayName, ttlSeconds, alertRadius, pasteOffset, blocks);
    }
}
