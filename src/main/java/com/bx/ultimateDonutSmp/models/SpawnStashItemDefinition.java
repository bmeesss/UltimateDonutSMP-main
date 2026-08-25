package com.bx.ultimateDonutSmp.models;

import org.bukkit.Material;

import java.util.List;

public final class SpawnStashItemDefinition {
    private final int slot;
    private final Material material;
    private final int amount;
    private final String displayName;
    private final List<String> lore;

    public SpawnStashItemDefinition(int slot, Material material, int amount, String displayName, List<String> lore) {
        this.slot = slot;
        this.material = material;
        this.amount = amount;
        this.displayName = displayName;
        this.lore = lore;
    }

    public int slot() { return slot; }
    public Material material() { return material; }
    public int amount() { return amount; }
    public String displayName() { return displayName; }
    public List<String> lore() { return lore; }

    @Override public String toString() {
        return "SpawnStashItemDefinition[slot=+slot, material=+material, amount=+amount, displayName=+displayName, lore=+lore]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpawnStashItemDefinition that = (SpawnStashItemDefinition) o;
        return java.util.Objects.equals(slot, that.slot) && java.util.Objects.equals(material, that.material) && java.util.Objects.equals(amount, that.amount) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(lore, that.lore);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(slot, material, amount, displayName, lore);
    }
}
