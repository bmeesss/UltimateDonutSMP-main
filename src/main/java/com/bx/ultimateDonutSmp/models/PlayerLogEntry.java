package com.bx.ultimateDonutSmp.models;

import java.util.UUID;

public final class PlayerLogEntry {
    private final long id;
    private final UUID playerUuid;
    private final String playerName;
    private final String category;
    private final String logType;
    private final String details;
    private final long timestamp;

    public PlayerLogEntry(long id, UUID playerUuid, String playerName, String category, String logType, String details, long timestamp) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.category = category;
        this.logType = logType;
        this.details = details;
        this.timestamp = timestamp;
    }

    public long id() { return id; }
    public UUID playerUuid() { return playerUuid; }
    public String playerName() { return playerName; }
    public String category() { return category; }
    public String logType() { return logType; }
    public String details() { return details; }
    public long timestamp() { return timestamp; }

    @Override public String toString() {
        return "PlayerLogEntry[id=+id, playerUuid=+playerUuid, playerName=+playerName, category=+category, logType=+logType, details=+details, timestamp=+timestamp]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerLogEntry that = (PlayerLogEntry) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(playerUuid, that.playerUuid) && java.util.Objects.equals(playerName, that.playerName) && java.util.Objects.equals(category, that.category) && java.util.Objects.equals(logType, that.logType) && java.util.Objects.equals(details, that.details) && java.util.Objects.equals(timestamp, that.timestamp);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(id, playerUuid, playerName, category, logType, details, timestamp);
    }
}
