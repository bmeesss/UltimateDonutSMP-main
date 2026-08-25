package com.bx.ultimateDonutSmp.models;

import java.util.UUID;

public final class FollowEntry {
    private final UUID followerUuid;
    private final UUID followedUuid;
    private final String followedNameSnapshot;
    private final boolean transactionsEnabled;
    private final boolean messagesEnabled;
    private final boolean paymentsEnabled;
    private final boolean activityEnabled;
    private final boolean tpaAutoAcceptEnabled;
    private final boolean teleportRequestsEnabled;
    private final long createdAt;

    public FollowEntry(UUID followerUuid, UUID followedUuid, String followedNameSnapshot, boolean transactionsEnabled, boolean messagesEnabled, boolean paymentsEnabled, boolean activityEnabled, boolean tpaAutoAcceptEnabled, boolean teleportRequestsEnabled, long createdAt) {
        this.followerUuid = followerUuid;
        this.followedUuid = followedUuid;
        this.followedNameSnapshot = followedNameSnapshot;
        this.transactionsEnabled = transactionsEnabled;
        this.messagesEnabled = messagesEnabled;
        this.paymentsEnabled = paymentsEnabled;
        this.activityEnabled = activityEnabled;
        this.tpaAutoAcceptEnabled = tpaAutoAcceptEnabled;
        this.teleportRequestsEnabled = teleportRequestsEnabled;
        this.createdAt = createdAt;
    }

    public UUID followerUuid() { return followerUuid; }
    public UUID followedUuid() { return followedUuid; }
    public String followedNameSnapshot() { return followedNameSnapshot; }
    public boolean transactionsEnabled() { return transactionsEnabled; }
    public boolean messagesEnabled() { return messagesEnabled; }
    public boolean paymentsEnabled() { return paymentsEnabled; }
    public boolean activityEnabled() { return activityEnabled; }
    public boolean tpaAutoAcceptEnabled() { return tpaAutoAcceptEnabled; }
    public boolean teleportRequestsEnabled() { return teleportRequestsEnabled; }
    public long createdAt() { return createdAt; }

    @Override public String toString() {
        return "FollowEntry[followerUuid=+followerUuid, followedUuid=+followedUuid, followedNameSnapshot=+followedNameSnapshot, transactionsEnabled=+transactionsEnabled, messagesEnabled=+messagesEnabled, paymentsEnabled=+paymentsEnabled, activityEnabled=+activityEnabled, tpaAutoAcceptEnabled=+tpaAutoAcceptEnabled, teleportRequestsEnabled=+teleportRequestsEnabled, createdAt=+createdAt]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FollowEntry that = (FollowEntry) o;
        return java.util.Objects.equals(followerUuid, that.followerUuid) && java.util.Objects.equals(followedUuid, that.followedUuid) && java.util.Objects.equals(followedNameSnapshot, that.followedNameSnapshot) && java.util.Objects.equals(transactionsEnabled, that.transactionsEnabled) && java.util.Objects.equals(messagesEnabled, that.messagesEnabled) && java.util.Objects.equals(paymentsEnabled, that.paymentsEnabled) && java.util.Objects.equals(activityEnabled, that.activityEnabled) && java.util.Objects.equals(tpaAutoAcceptEnabled, that.tpaAutoAcceptEnabled) && java.util.Objects.equals(teleportRequestsEnabled, that.teleportRequestsEnabled) && java.util.Objects.equals(createdAt, that.createdAt);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(followerUuid, followedUuid, followedNameSnapshot, transactionsEnabled, messagesEnabled, paymentsEnabled, activityEnabled, tpaAutoAcceptEnabled, teleportRequestsEnabled, createdAt);
    }
}
