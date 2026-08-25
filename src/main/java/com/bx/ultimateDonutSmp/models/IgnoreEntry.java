package com.bx.ultimateDonutSmp.models;

import java.util.UUID;

public final class IgnoreEntry {
    private final UUID ownerUuid;
    private final UUID ignoredUuid;
    private final String ignoredNameSnapshot;
    private final long createdAt;

    public IgnoreEntry(UUID ownerUuid, UUID ignoredUuid, String ignoredNameSnapshot, long createdAt) {
        this.ownerUuid = ownerUuid;
        this.ignoredUuid = ignoredUuid;
        this.ignoredNameSnapshot = ignoredNameSnapshot;
        this.createdAt = createdAt;
    }

    public UUID ownerUuid() { return ownerUuid; }
    public UUID ignoredUuid() { return ignoredUuid; }
    public String ignoredNameSnapshot() { return ignoredNameSnapshot; }
    public long createdAt() { return createdAt; }

    @Override public String toString() {
        return "IgnoreEntry[ownerUuid=+ownerUuid, ignoredUuid=+ignoredUuid, ignoredNameSnapshot=+ignoredNameSnapshot, createdAt=+createdAt]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IgnoreEntry that = (IgnoreEntry) o;
        return java.util.Objects.equals(ownerUuid, that.ownerUuid) && java.util.Objects.equals(ignoredUuid, that.ignoredUuid) && java.util.Objects.equals(ignoredNameSnapshot, that.ignoredNameSnapshot) && java.util.Objects.equals(createdAt, that.createdAt);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(ownerUuid, ignoredUuid, ignoredNameSnapshot, createdAt);
    }
}
