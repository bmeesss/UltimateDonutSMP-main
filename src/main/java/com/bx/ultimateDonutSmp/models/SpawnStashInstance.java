package com.bx.ultimateDonutSmp.models;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpawnStashInstance {
    private final long id;
    private final String typeKey;
    private final String displayName;
    private final UUID creatorUuid;
    private final String creatorName;
    private final String worldName;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final BlockFace facing;
    private final long createdAtMillis;
    private final long expiresAtMillis;
    private final double alertRadius;
    private final List<Location> blockLocations;
    private final Set<String> blockKeys;
    private final List<BlockState> snapshots;
    private final Map<String, Long> alertCooldowns;

    public SpawnStashInstance(long id, String typeKey, String displayName, UUID creatorUuid, String creatorName, String worldName, int originX, int originY, int originZ, BlockFace facing, long createdAtMillis, long expiresAtMillis, double alertRadius, List<Location> blockLocations, Set<String> blockKeys, List<BlockState> snapshots, Map<String, Long> alertCooldowns) {
        this.id = id;
        this.typeKey = typeKey;
        this.displayName = displayName;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.worldName = worldName;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.facing = facing;
        this.createdAtMillis = createdAtMillis;
        this.expiresAtMillis = expiresAtMillis;
        this.alertRadius = alertRadius;
        this.blockLocations = blockLocations;
        this.blockKeys = blockKeys;
        this.snapshots = snapshots;
        this.alertCooldowns = alertCooldowns == null ? new ConcurrentHashMap<String, Long>() : alertCooldowns;
    }

    public long id() { return id; }
    public String typeKey() { return typeKey; }
    public String displayName() { return displayName; }
    public UUID creatorUuid() { return creatorUuid; }
    public String creatorName() { return creatorName; }
    public String worldName() { return worldName; }
    public int originX() { return originX; }
    public int originY() { return originY; }
    public int originZ() { return originZ; }
    public BlockFace facing() { return facing; }
    public long createdAtMillis() { return createdAtMillis; }
    public long expiresAtMillis() { return expiresAtMillis; }
    public double alertRadius() { return alertRadius; }
    public List<Location> blockLocations() { return blockLocations; }
    public Set<String> blockKeys() { return blockKeys; }
    public List<BlockState> snapshots() { return snapshots; }
    public Map<String, Long> alertCooldowns() { return alertCooldowns; }
    public Location originLocation() {
        return new Location(blockLocations.isEmpty() ? null : blockLocations.get(0).getWorld(),
                originX + 0.5D, originY + 0.5D, originZ + 0.5D);
    }
    @Override public String toString() {
        return "SpawnStashInstance[id=+id, typeKey=+typeKey, displayName=+displayName, creatorUuid=+creatorUuid, creatorName=+creatorName, worldName=+worldName, originX=+originX, originY=+originY, originZ=+originZ, facing=+facing, createdAtMillis=+createdAtMillis, expiresAtMillis=+expiresAtMillis, alertRadius=+alertRadius, blockLocations=+blockLocations, blockKeys=+blockKeys, snapshots=+snapshots, alertCooldowns=+alertCooldowns]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpawnStashInstance that = (SpawnStashInstance) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(typeKey, that.typeKey) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(creatorUuid, that.creatorUuid) && java.util.Objects.equals(creatorName, that.creatorName) && java.util.Objects.equals(worldName, that.worldName) && java.util.Objects.equals(originX, that.originX) && java.util.Objects.equals(originY, that.originY) && java.util.Objects.equals(originZ, that.originZ) && java.util.Objects.equals(facing, that.facing) && java.util.Objects.equals(createdAtMillis, that.createdAtMillis) && java.util.Objects.equals(expiresAtMillis, that.expiresAtMillis) && java.util.Objects.equals(alertRadius, that.alertRadius) && java.util.Objects.equals(blockLocations, that.blockLocations) && java.util.Objects.equals(blockKeys, that.blockKeys) && java.util.Objects.equals(snapshots, that.snapshots) && java.util.Objects.equals(alertCooldowns, that.alertCooldowns);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(id, typeKey, displayName, creatorUuid, creatorName, worldName, originX, originY, originZ, facing, createdAtMillis, expiresAtMillis, alertRadius, blockLocations, blockKeys, snapshots, alertCooldowns);
    }
}
