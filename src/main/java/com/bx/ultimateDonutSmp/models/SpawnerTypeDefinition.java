package com.bx.ultimateDonutSmp.models;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;

public final class SpawnerTypeDefinition {
    private final String key;
    private final String displayName;
    private final EntityType entityType;
    private final Material iconMaterial;
    private final long baseItemsPerCycle;
    private final double xpPerCycle;
    private final String headTexture;
    private final List<DropDefinition> drops;

    public SpawnerTypeDefinition(String key, String displayName, EntityType entityType, Material iconMaterial, long baseItemsPerCycle, double xpPerCycle, String headTexture, List<DropDefinition> drops) {
    }

    public String key() { return key; }
    public String displayName() { return displayName; }
    public EntityType entityType() { return entityType; }
    public Material iconMaterial() { return iconMaterial; }
    public long baseItemsPerCycle() { return baseItemsPerCycle; }
    public double xpPerCycle() { return xpPerCycle; }
    public String headTexture() { return headTexture; }
    public List<DropDefinition> drops() { return drops; }



    public


    public SpawnerTypeDefinition(
            String key,
            String displayName,
            EntityType entityType,
            Material iconMaterial,
            long baseItemsPerCycle,
            double xpPerCycle,
            List<DropDefinition> drops
    ) {
        this(key, displayName, entityType, iconMaterial, baseItemsPerCycle, xpPerCycle, null, drops);
    }

    public SpawnerTypeDefinition {
        displayName = displayName == null || displayName.isBlank() ? key : displayName;
        iconMaterial = iconMaterial == null ? Material.SPAWNER : iconMaterial;
        baseItemsPerCycle = Math.max(1L, baseItemsPerCycle);
        xpPerCycle = Math.max(0.0, xpPerCycle);
        headTexture = headTexture == null || headTexture.isBlank() ? null : headTexture.trim();
        drops = new java.util.ArrayList<>(drops == null ? java.util.Collections.emptyList() : drops);
    }

public final class DropDefinition {
    private final String key;
    private final Material material;
    private final long min;
    private final long max;
    private final double chance;

    public DropDefinition(String key, Material material, long min, long max, double chance) {
        this.key = key;
        this.material = material;
        this.min = min;
        this.max = max;
        this.chance = chance;
    }

    public String key() { return key; }
    public Material material() { return material; }
    public long min() { return min; }
    public long max() { return max; }
    public double chance() { return chance; }


        public DropDefinition {
            key = key == null ? "" : key.trim();
            material = material == null ? Material.STONE : material;
            min = Math.max(0L, min);
            max = Math.max(min, max);
            chance = Math.max(0D, Math.min(1D, chance));
        }

        public double averageDropAmount() {
            return (min + max) / 2.0D;
        }

    @Override public String toString() {
        return "DropDefinition[key=+key, material=+material, min=+min, max=+max, chance=+chance]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DropDefinition that = (DropDefinition) o;
        return java.util.Objects.equals(key, that.key) && java.util.Objects.equals(material, that.material) && java.util.Objects.equals(min, that.min) && java.util.Objects.equals(max, that.max) && java.util.Objects.equals(chance, that.chance);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(key, material, min, max, chance);
    }
}
    @Override public String toString() {
        return "SpawnerTypeDefinition[key=+key, displayName=+displayName, entityType=+entityType, iconMaterial=+iconMaterial, baseItemsPerCycle=+baseItemsPerCycle, xpPerCycle=+xpPerCycle, headTexture=+headTexture, drops=+drops]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpawnerTypeDefinition that = (SpawnerTypeDefinition) o;
        return java.util.Objects.equals(key, that.key) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(entityType, that.entityType) && java.util.Objects.equals(iconMaterial, that.iconMaterial) && java.util.Objects.equals(baseItemsPerCycle, that.baseItemsPerCycle) && java.util.Objects.equals(xpPerCycle, that.xpPerCycle) && java.util.Objects.equals(headTexture, that.headTexture) && java.util.Objects.equals(drops, that.drops);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(key, displayName, entityType, iconMaterial, baseItemsPerCycle, xpPerCycle, headTexture, drops);
    }
}
