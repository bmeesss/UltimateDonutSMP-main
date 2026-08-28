package com.bx.ultimateDonutSmp.utils;

import com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport.Icon;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Orders material lookup on Bukkit 1.12.2: legacy names resolve natively, the flattened modern
 * names the shipped configurations use map to their 1.12.2 equivalent, and unsupported materials
 * resolve to null — never to a STONE fallback that would display the wrong item.
 */
class LegacyMaterialSupportTest {

    private static Material material(String raw) {
        Icon icon = LegacyMaterialSupport.resolve(raw);
        return icon == null ? null : icon.material();
    }

    @Test
    void legacyNamesResolveNatively() {
        assertEquals(Material.STONE, material("STONE"));
        assertEquals(Material.STONE, material("stone"));
        assertEquals(Material.WATCH, material("WATCH"));
        assertEquals(Material.HOPPER, material("HOPPER"));
        assertEquals(Material.INK_SACK, material("INK_SACK"));
        assertEquals(Material.STRUCTURE_BLOCK, material("STRUCTURE_BLOCK"));
    }

    @Test
    void flattenedNamesUsedByTheShippedConfigsResolve() {
        assertEquals(Material.GRASS, material("GRASS_BLOCK"));
        assertEquals(Material.PISTON_BASE, material("PISTON"));
        assertEquals(Material.LEASH, material("LEAD"));
        assertEquals(Material.SULPHUR, material("GUNPOWDER"));
        assertEquals(Material.WOOD_PICKAXE, material("WOODEN_PICKAXE"));
        assertEquals(Material.GOLD_AXE, material("GOLDEN_AXE"));
        assertEquals(Material.STONE_SPADE, material("STONE_SHOVEL"));
        assertEquals(Material.DIAMOND_SPADE, material("DIAMOND_SHOVEL"));
        assertEquals(Material.WORKBENCH, material("CRAFTING_TABLE"));
        assertEquals(Material.ENDER_STONE, material("END_STONE"));
        assertEquals(Material.QUARTZ_ORE, material("NETHER_QUARTZ_ORE"));
        assertEquals(Material.ENDER_STONE, material("ENDER_STONE"));
        assertEquals(Material.FIREWORK, material("FIREWORK_ROCKET"));
        assertEquals(Material.SPRUCE_DOOR_ITEM, material("SPRUCE_DOOR"));
        assertEquals(Material.BREWING_STAND_ITEM, material("BREWING_STAND"));
    }

    @Test
    void colourAndWoodFamiliesCarryTheirDataValue() {
        Icon whiteTerracotta = LegacyMaterialSupport.resolve("WHITE_TERRACOTTA");
        assertEquals(Material.STAINED_CLAY, whiteTerracotta.material());
        assertEquals(0, whiteTerracotta.data());

        Icon blackTerracotta = LegacyMaterialSupport.resolve("BLACK_TERRACOTTA");
        assertEquals(Material.STAINED_CLAY, blackTerracotta.material());
        assertEquals(15, blackTerracotta.data());

        assertEquals(Material.CONCRETE, material("WHITE_CONCRETE"));
        assertEquals(Material.WOOL, material("WHITE_WOOL"));
        assertEquals(Material.THIN_GLASS, material("WHITE_GLASS"));
        assertEquals(Material.SILVER_SHULKER_BOX, material("LIGHT_GRAY_SHULKER_BOX"));

        Icon oakLog = LegacyMaterialSupport.resolve("OAK_LOG");
        assertEquals(Material.LOG, oakLog.material());
        assertEquals(0, oakLog.data());

        Icon darkOakLog = LegacyMaterialSupport.resolve("DARK_OAK_LOG");
        assertEquals(Material.LOG_2, darkOakLog.material());
        assertEquals(1, darkOakLog.data());

        assertEquals(Material.WOOD, material("OAK_PLANKS"));
        assertEquals(Material.WOOD_STEP, material("OAK_SLAB"));
        assertEquals(Material.STEP, material("COBBLESTONE_SLAB"));
        assertEquals(Material.STONE, material("ANDESITE"));
        assertEquals(Material.SMOOTH_BRICK, material("STONE_BRICKS"));
        assertEquals(Material.COBBLE_WALL, material("COBBLESTONE_WALL"));
        assertEquals(Material.HARD_CLAY, material("TERRACOTTA"));
    }

    @Test
    void dyeDataRunsOppositeToTheWoolOrder() {
        assertEquals(15, LegacyMaterialSupport.resolve("WHITE_DYE").data());
        assertEquals(0, LegacyMaterialSupport.resolve("BLACK_DYE").data());
        assertEquals(Material.INK_SACK, material("WHITE_DYE"));
    }

    @Test
    void orderSearchCornerCasesResolveTheRequestedMaterial() {
        // The exact materials the order-search acceptance names.
        assertEquals(Material.STONE, material("STONE"));
        assertEquals(Material.DIAMOND, material("DIAMOND"));
        assertEquals(Material.DIAMOND_SWORD, material("DIAMOND_SWORD"));

        // A modern-name query and its legacy spelling reach the SAME material, so both
        // /order oak_door and /order wood_door find the same catalog entry.
        assertEquals(material("OAK_DOOR"), material("WOOD_DOOR"));
        assertEquals(material("GRASS_BLOCK"), material("GRASS"));
        assertEquals(material("NETHERITE_SWORD"), material("DIAMOND_SWORD"));
        assertEquals(material("GOLDEN_AXE"), material("GOLD_AXE"));

        // Random invalid input must not resolve at all - and definitely not to STONE.
        String[] invalid = {"gibberish_xyz", "NOT_A_MATERIAL", "stone2", "129zvc", "", " ", "stone oak"};
        for (String name : invalid) {
            assertNull(material(name), name + " must not resolve");
        }
        assertNotSame(Material.STONE, material("gibberish_xyz"));
    }

    @Test
    void unsupportedMaterialsStayUnsupportedInsteadOfFallingBackToStone() {
        String[] unsupported = {
                "LIGHT", "JIGSAW", "GIBBERISH_XYZ", "NOT_A_MATERIAL", "MANGROVE_LOG",
                "CHERRY_PLANKS", "SWEET_BERRIES", "TRIDENT", "SPYGLASS", "COPPER_INGOT",
                "TINTED_GLASS", "BLACKSTONE", "BASALT", "CAMPFIRE", "BARREL", "LECTERN",
                "PHANTOM_MEMBRANE", "NAUTILUS_SHELL",
        };
        for (String name : unsupported) {
            assertNull(material(name), name + " must not resolve");
            assertTrue(material(name) != Material.STONE || name.equals("STONE"), name);
        }
        assertTrue(LegacyMaterialSupport.isUnsupportedOnLegacy("LIGHT"));
        assertTrue(LegacyMaterialSupport.isUnsupportedOnLegacy("JIGSAW"));
    }

    @Test
    void paneNamesKeepWorkingThroughTheCentralLayer() {
        Icon blackPane = LegacyMaterialSupport.resolve("BLACK_STAINED_GLASS_PANE");
        assertEquals(Material.STAINED_GLASS_PANE, blackPane.material());
        assertEquals(15, blackPane.data());
        assertNotNull(LegacyMaterialSupport.resolvePane("RED"));
    }

    @Test
    void ordersCatalogDoesNotSubstituteStoneForUnrelatedMaterials() {
        // The Orders category classifier used to compare against a STONE fallback, silently
        // counting STONE itself as a combat material.
        Material firework = material("FIREWORK_ROCKET");
        assertNotNull(firework);
        assertTrue(firework != Material.STONE);
        Material grass = material("GRASS_BLOCK");
        assertTrue(grass != Material.STONE);
    }
}
