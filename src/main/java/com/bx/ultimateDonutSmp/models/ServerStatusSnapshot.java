package com.bx.ultimateDonutSmp.models;

public final class ServerStatusSnapshot {
    private final String serverId;
    private final String displayName;
    private final boolean online;
    private final int playerCount;
    private final String softwareLabel;
    private final String performanceLabel;
    private final long lastUpdatedAt;
    private final long latencyMs;

    public ServerStatusSnapshot(String serverId, String displayName, boolean online, int playerCount, String softwareLabel, String performanceLabel, long lastUpdatedAt, long latencyMs) {
        this.serverId = serverId;
        this.displayName = displayName;
        this.online = online;
        this.playerCount = playerCount;
        this.softwareLabel = softwareLabel;
        this.performanceLabel = performanceLabel;
        this.lastUpdatedAt = lastUpdatedAt;
        this.latencyMs = latencyMs;
    }

    public String serverId() { return serverId; }
    public String displayName() { return displayName; }
    public boolean online() { return online; }
    public int playerCount() { return playerCount; }
    public String softwareLabel() { return softwareLabel; }
    public String performanceLabel() { return performanceLabel; }
    public long lastUpdatedAt() { return lastUpdatedAt; }
    public long latencyMs() { return latencyMs; }



    public ServerStatusSnapshot {
        serverId = serverId == null ? "" : serverId;
        displayName = displayName == null || displayName.isBlank() ? serverId : displayName;
        playerCount = Math.max(0, playerCount);
        softwareLabel = normalizeLabel(softwareLabel);
        performanceLabel = normalizeLabel(performanceLabel);
        lastUpdatedAt = Math.max(0L, lastUpdatedAt);
        latencyMs = Math.max(0L, latencyMs);
    }

    public static ServerStatusSnapshot offline(String serverId, String displayName) {
        return new ServerStatusSnapshot(
                serverId,
                displayName,
                false,
                0,
                "N/A",
                "N/A",
                0L,
                0L
        );
    }

    public ServerStatusSnapshot withIdentity(String newServerId, String newDisplayName) {
        return new ServerStatusSnapshot(
                newServerId,
                newDisplayName,
                online,
                playerCount,
                softwareLabel,
                performanceLabel,
                lastUpdatedAt,
                latencyMs
        );
    }

    private static String normalizeLabel(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
    @Override public String toString() {
        return "ServerStatusSnapshot[serverId=+serverId, displayName=+displayName, online=+online, playerCount=+playerCount, softwareLabel=+softwareLabel, performanceLabel=+performanceLabel, lastUpdatedAt=+lastUpdatedAt, latencyMs=+latencyMs]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerStatusSnapshot that = (ServerStatusSnapshot) o;
        return java.util.Objects.equals(serverId, that.serverId) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(online, that.online) && java.util.Objects.equals(playerCount, that.playerCount) && java.util.Objects.equals(softwareLabel, that.softwareLabel) && java.util.Objects.equals(performanceLabel, that.performanceLabel) && java.util.Objects.equals(lastUpdatedAt, that.lastUpdatedAt) && java.util.Objects.equals(latencyMs, that.latencyMs);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(serverId, displayName, online, playerCount, softwareLabel, performanceLabel, lastUpdatedAt, latencyMs);
    }
}
