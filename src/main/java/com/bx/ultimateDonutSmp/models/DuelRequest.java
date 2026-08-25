package com.bx.ultimateDonutSmp.models;

import java.util.UUID;

public final class DuelRequest {
    private final UUID challengerUuid;
    private final String challengerName;
    private final UUID targetUuid;
    private final String targetName;
    private final DuelMapSelection mapSelection;
    private final DuelPrivacyMode privacyMode;
    private final long expiresAt;

    public DuelRequest(UUID challengerUuid, String challengerName, UUID targetUuid, String targetName, DuelMapSelection mapSelection, DuelPrivacyMode privacyMode, long expiresAt) {
        this.challengerUuid = challengerUuid;
        this.challengerName = challengerName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.mapSelection = mapSelection;
        this.privacyMode = privacyMode;
        this.expiresAt = expiresAt;
    }

    public UUID challengerUuid() { return challengerUuid; }
    public String challengerName() { return challengerName; }
    public UUID targetUuid() { return targetUuid; }
    public String targetName() { return targetName; }
    public DuelMapSelection mapSelection() { return mapSelection; }
    public DuelPrivacyMode privacyMode() { return privacyMode; }
    public long expiresAt() { return expiresAt; }


    public boolean isExpired(long now) {
        return now >= expiresAt;
    }
    @Override public String toString() {
        return "DuelRequest[challengerUuid=+challengerUuid, challengerName=+challengerName, targetUuid=+targetUuid, targetName=+targetName, mapSelection=+mapSelection, privacyMode=+privacyMode, expiresAt=+expiresAt]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuelRequest that = (DuelRequest) o;
        return java.util.Objects.equals(challengerUuid, that.challengerUuid) && java.util.Objects.equals(challengerName, that.challengerName) && java.util.Objects.equals(targetUuid, that.targetUuid) && java.util.Objects.equals(targetName, that.targetName) && java.util.Objects.equals(mapSelection, that.mapSelection) && java.util.Objects.equals(privacyMode, that.privacyMode) && java.util.Objects.equals(expiresAt, that.expiresAt);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(challengerUuid, challengerName, targetUuid, targetName, mapSelection, privacyMode, expiresAt);
    }
}
