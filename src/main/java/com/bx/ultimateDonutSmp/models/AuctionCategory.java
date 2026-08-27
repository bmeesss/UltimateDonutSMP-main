package com.bx.ultimateDonutSmp.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public enum AuctionCategory {
    ALL("All", Material.COMPASS),
    BLOCKS("Blocks", Material.GRASS),
    TOOLS("Tools", Material.DIAMOND_PICKAXE),
    FOOD("Food", Material.GOLDEN_CARROT),
    COMBAT("Combat", Material.DIAMOND_SWORD),
    POTIONS("Potions", Material.POTION),
    BOOKS("Books", Material.ENCHANTED_BOOK),
    INGREDIENTS("Ingredients", Material.BLAZE_POWDER),
    UTILITIES("Utilities", Material.ENDER_CHEST);

    private final String displayName;
    private final Material icon;

    AuctionCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String defaultDisplayName() {
        return displayName;
    }

    public Material defaultIcon() {
        return icon;
    }

    public boolean matches(ItemStack item) {
        if (this == ALL) {
            return true;
        }
        if (item == null) {
            return false;
        }

        Material type = item.getType();
        if (type == Material.AIR || type.name().endsWith("_AIR")) {
            return false;
        }
        switch (this) {
            case BLOCKS:
                return matches(type, type.isBlock(), false);
            case FOOD:
                return matches(type, false, type.isEdible());
            default:
                return matches(type, false, false);
        }
    }

    public boolean matches(Material type, boolean block, boolean edible) {
        if (this == ALL) {
            return true;
        }
        if (type == null || type == Material.AIR || type.name().endsWith("_AIR")) {
            return false;
        }
        String name = type.name();
        switch (this) {
            case ALL:
                return true;
            case BLOCKS:
                return block;
            case TOOLS:
                return name.endsWith("_AXE")
                    || name.endsWith("_PICKAXE")
                    || name.endsWith("_SHOVEL")
                    || name.endsWith("_HOE")
                    || type == Material.SHEARS
                    || type == Material.FLINT_AND_STEEL
                    || type == Material.FISHING_ROD;
            case FOOD:
                return edible;
            case COMBAT:
                return name.endsWith("_SWORD")
                    || name.endsWith("_AXE")
                    || name.endsWith("_HELMET")
                    || name.endsWith("_CHESTPLATE")
                    || name.endsWith("_LEGGINGS")
                    || name.endsWith("_BOOTS")
                    || name.endsWith("_BOW")
                    || type == Material.BOW
                    || type == Material.FISHING_ROD
                    || type == Material.SHIELD;
            case POTIONS:
                return type == Material.POTION
                    || type == Material.SPLASH_POTION
                    || type == Material.LINGERING_POTION
                    || type == Material.TIPPED_ARROW;
            case BOOKS:
                return type == Material.BOOK
                    || type == Material.BOOK_AND_QUILL
                    || type == Material.WRITTEN_BOOK
                    || type == Material.ENCHANTED_BOOK
                    || type == Material.KNOWLEDGE_BOOK;
            case INGREDIENTS:
                return type == Material.BLAZE_POWDER
                    || type == Material.BLAZE_ROD
                    || type == Material.SULPHUR
                    || type == Material.STRING
                    || type == Material.SPIDER_EYE
                    || type == Material.FERMENTED_SPIDER_EYE
                    || type == Material.SPECKLED_MELON
                    || type == Material.GHAST_TEAR
                    || type == Material.MAGMA_CREAM
                    || type == Material.RABBIT_FOOT
                    || type == Material.LEATHER
                    || type == Material.SUGAR
                    || type == Material.REDSTONE
                    || type == Material.REDSTONE
                    || type == Material.NETHER_STALK;
            case UTILITIES:
                return type == Material.ENDER_CHEST
                    || type == Material.CHEST
                    || type == Material.CHEST
                    || type == Material.ENDER_CHEST
                    || name.endsWith("_ENDER_CHEST")
                    || type == Material.ELYTRA
                    || type == Material.LEASH
                    || type == Material.NAME_TAG
                    || type == Material.COMPASS
                    || type == Material.COMPASS
                    || type == Material.WATCH;
        }
        // AuctionCategory is handled exhaustively above; treat any future category as non-matching.
        return false;
    }

    public static AuctionCategory from(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return ALL;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ALL;
        }
    }

    public static AuctionCategory findCategoryForItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return ALL;
        }
        for (AuctionCategory category : values()) {
            if (category == ALL) {
                continue;
            }
            if (category.matches(item)) {
                return category;
            }
        }
        return ALL;
    }
}
