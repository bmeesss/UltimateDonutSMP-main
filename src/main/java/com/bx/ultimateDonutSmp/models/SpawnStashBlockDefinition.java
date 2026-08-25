package com.bx.ultimateDonutSmp.models;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;

public final class SpawnStashBlockDefinition {
    private final SpawnStashOffset offset;
    private final Material material;
    private final String blockData;
    private final String spawnerTypeKey;
    private final long spawnerStackAmount;
    private final SpawnerInstance.AccessMode spawnerAccessMode;
    private final EntityType spawnerEntity;
    private final List<String> signLines;
    private final List<SpawnStashItemDefinition> containerItems;

    public SpawnStashBlockDefinition(SpawnStashOffset offset, Material material, String blockData, String spawnerTypeKey, long spawnerStackAmount, SpawnerInstance.AccessMode spawnerAccessMode, EntityType spawnerEntity, List<String> signLines, List<SpawnStashItemDefinition> containerItems) {
        this.offset = offset;
        this.material = material;
        this.blockData = blockData;
        this.spawnerTypeKey = spawnerTypeKey;
        this.spawnerStackAmount = spawnerStackAmount;
        this.spawnerAccessMode = spawnerAccessMode;
        this.spawnerEntity = spawnerEntity;
        this.signLines = signLines;
        this.containerItems = containerItems;
    }

    public SpawnStashOffset offset() { return offset; }
    public Material material() { return material; }
    public String blockData() { return blockData; }
    public String spawnerTypeKey() { return spawnerTypeKey; }
    public long spawnerStackAmount() { return spawnerStackAmount; }
    public SpawnerInstance.AccessMode spawnerAccessMode() { return spawnerAccessMode; }
    public EntityType spawnerEntity() { return spawnerEntity; }
    public List<String> signLines() { return signLines; }
    public List<SpawnStashItemDefinition> containerItems() { return containerItems; }

    @Override public String toString() {
        return "SpawnStashBlockDefinition[offset=+offset, material=+material, blockData=+blockData, spawnerTypeKey=+spawnerTypeKey, spawnerStackAmount=+spawnerStackAmount, spawnerAccessMode=+spawnerAccessMode, spawnerEntity=+spawnerEntity, signLines=+signLines, containerItems=+containerItems]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpawnStashBlockDefinition that = (SpawnStashBlockDefinition) o;
        return java.util.Objects.equals(offset, that.offset) && java.util.Objects.equals(material, that.material) && java.util.Objects.equals(blockData, that.blockData) && java.util.Objects.equals(spawnerTypeKey, that.spawnerTypeKey) && java.util.Objects.equals(spawnerStackAmount, that.spawnerStackAmount) && java.util.Objects.equals(spawnerAccessMode, that.spawnerAccessMode) && java.util.Objects.equals(spawnerEntity, that.spawnerEntity) && java.util.Objects.equals(signLines, that.signLines) && java.util.Objects.equals(containerItems, that.containerItems);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(offset, material, blockData, spawnerTypeKey, spawnerStackAmount, spawnerAccessMode, spawnerEntity, signLines, containerItems);
    }
}
