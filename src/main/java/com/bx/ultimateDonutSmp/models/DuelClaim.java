package com.bx.ultimateDonutSmp.models;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class DuelClaim {
    private final long matchId;
    private final UUID playerUuid;
    private final String defeatedName;
    private final List<ItemStack> items;
    private final long createdAt;

    public DuelClaim(long matchId, UUID playerUuid, String defeatedName, List<ItemStack> items, long createdAt) {
        this.matchId = matchId;
        this.playerUuid = playerUuid;
        this.defeatedName = defeatedName;
        this.items = items;
        this.createdAt = createdAt;
    }

    public long matchId() { return matchId; }
    public UUID playerUuid() { return playerUuid; }
    public String defeatedName() { return defeatedName; }
    public List<ItemStack> items() { return items; }
    public long createdAt() { return createdAt; }


    public int itemCount() {
        return items == null ? 0 : items.size();
    }
    @Override public String toString() {
        return "DuelClaim[matchId=+matchId, playerUuid=+playerUuid, defeatedName=+defeatedName, items=+items, createdAt=+createdAt]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuelClaim that = (DuelClaim) o;
        return java.util.Objects.equals(matchId, that.matchId) && java.util.Objects.equals(playerUuid, that.playerUuid) && java.util.Objects.equals(defeatedName, that.defeatedName) && java.util.Objects.equals(items, that.items) && java.util.Objects.equals(createdAt, that.createdAt);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(matchId, playerUuid, defeatedName, items, createdAt);
    }
}
