package com.bx.ultimateDonutSmp.models;

import java.util.UUID;

public final class HideState {
    private final UUID playerUuid;
    private final String realNameSnapshot;
    private final HideMode mode;
    private final String alias;
    private final String aliasNormalized;
    private final String skinKey;
    private final String skinUsername;
    private final String textureValue;
    private final String textureSignature;
    private final long createdAt;
    private final long updatedAt;

    public HideState(UUID playerUuid, String realNameSnapshot, HideMode mode, String alias, String aliasNormalized, String skinKey, String skinUsername, String textureValue, String textureSignature, long createdAt, long updatedAt) {
        realNameSnapshot = safe(realNameSnapshot);
        alias = safe(alias);
        aliasNormalized = safe(aliasNormalized);
        skinKey = safe(skinKey);
        skinUsername = safe(skinUsername);
        textureValue = safe(textureValue);
        textureSignature = safe(textureSignature);
        this.playerUuid = playerUuid;
        this.realNameSnapshot = realNameSnapshot;
        this.mode = mode;
        this.alias = alias;
        this.aliasNormalized = aliasNormalized;
        this.skinKey = skinKey;
        this.skinUsername = skinUsername;
        this.textureValue = textureValue;
        this.textureSignature = textureSignature;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID playerUuid() { return playerUuid; }
    public String realNameSnapshot() { return realNameSnapshot; }
    public HideMode mode() { return mode; }
    public String alias() { return alias; }
    public String aliasNormalized() { return aliasNormalized; }
    public String skinKey() { return skinKey; }
    public String skinUsername() { return skinUsername; }
    public String textureValue() { return textureValue; }
    public String textureSignature() { return textureSignature; }
    public long createdAt() { return createdAt; }
    public long updatedAt() { return updatedAt; }




    public boolean hasTexture() {
        return !textureValue.trim().isEmpty();
    }

    public HideState withTexture(String value, String signature, long timestamp) {
        return new HideState(
                playerUuid,
                realNameSnapshot,
                mode,
                alias,
                aliasNormalized,
                skinKey,
                skinUsername,
                value,
                signature,
                createdAt,
                timestamp
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
    @Override public String toString() {
        return "HideState[playerUuid=+playerUuid, realNameSnapshot=+realNameSnapshot, mode=+mode, alias=+alias, aliasNormalized=+aliasNormalized, skinKey=+skinKey, skinUsername=+skinUsername, textureValue=+textureValue, textureSignature=+textureSignature, createdAt=+createdAt, updatedAt=+updatedAt]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HideState that = (HideState) o;
        return java.util.Objects.equals(playerUuid, that.playerUuid) && java.util.Objects.equals(realNameSnapshot, that.realNameSnapshot) && java.util.Objects.equals(mode, that.mode) && java.util.Objects.equals(alias, that.alias) && java.util.Objects.equals(aliasNormalized, that.aliasNormalized) && java.util.Objects.equals(skinKey, that.skinKey) && java.util.Objects.equals(skinUsername, that.skinUsername) && java.util.Objects.equals(textureValue, that.textureValue) && java.util.Objects.equals(textureSignature, that.textureSignature) && java.util.Objects.equals(createdAt, that.createdAt) && java.util.Objects.equals(updatedAt, that.updatedAt);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(playerUuid, realNameSnapshot, mode, alias, aliasNormalized, skinKey, skinUsername, textureValue, textureSignature, createdAt, updatedAt);
    }
}
