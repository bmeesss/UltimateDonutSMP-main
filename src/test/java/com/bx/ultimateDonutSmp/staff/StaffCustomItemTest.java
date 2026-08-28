package com.bx.ultimateDonutSmp.staff;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffCustomItemTest {

    private static final Set<Integer> BUILT_IN_SLOTS = new java.util.LinkedHashSet<>(java.util.Arrays.asList(0,  1,  4,  7,  8));

    @Test
    void parsesAFullDefinition() throws Exception {
        List<String> warnings = new ArrayList<>();
        List<StaffCustomItem> items = parse(String.join("\n", "CUSTOM-ITEMS:\n", "  invsee:\n", "    SLOT: 2\n", "    MATERIAL: CHEST\n", "    NAME: '&eInventory'\n", "    LORE:\n", "    - '&7Right-click a player'\n", "    EXECUTE-AS: CONSOLE\n", "    PERMISSION: ultimatedonutsmp.staff.mode.custom.invsee\n", "    REQUIRE-TARGET: true\n", "    COMMANDS:\n", "    - '/invsee {target}'\n"), warnings);

        assertEquals(1, items.size());
        StaffCustomItem item = items.get(0);
        assertEquals("INVSEE", item.id());
        assertEquals(2, item.slot());
        assertEquals(Material.CHEST, item.material());
        assertEquals(java.util.Collections.singletonList("&7Right-click a player"), item.lore());
        assertEquals(StaffCustomItem.ExecuteAs.CONSOLE, item.executeAs());
        assertTrue(item.hasPermission());
        assertTrue(item.requireTarget());
        assertEquals(java.util.Collections.singletonList("invsee {target}"), item.commands(), "leading slashes must be stripped");
        assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);
    }

    @Test
    void appliesDefaultsForOptionalKeys() throws Exception {
        List<StaffCustomItem> items = parse(String.join("\n", "CUSTOM-ITEMS:\n", "  minimal:\n", "    SLOT: 3\n", "    COMMANDS:\n", "    - 'spawn'\n"), new ArrayList<>());

        assertEquals(1, items.size());
        StaffCustomItem item = items.get(0);
        assertEquals(Material.STONE, item.material());
        assertEquals(StaffCustomItem.ExecuteAs.PLAYER, item.executeAs(), "commands must default to the staff member");
        assertFalse(item.hasPermission());
        assertFalse(item.requireTarget());
    }

    @Test
    void acceptsASingleStringCommand() throws Exception {
        List<StaffCustomItem> items = parse(String.join("\n", "CUSTOM-ITEMS:\n", "  single:\n", "    SLOT: 3\n", "    COMMANDS: 'heal {player}'\n"), new ArrayList<>());

        assertEquals(java.util.Collections.singletonList("heal {player}"), items.get(0).commands());
    }

    @Test
    void skipsDisabledEntriesWithoutWarning() throws Exception {
        List<String> warnings = new ArrayList<>();
        List<StaffCustomItem> items = parse(String.join("\n", "CUSTOM-ITEMS:\n", "  off:\n", "    ENABLED: false\n", "    SLOT: 3\n", "    COMMANDS:\n", "    - 'spawn'\n"), warnings);

        assertTrue(items.isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void rejectsSlotsOutsideTheHotbar() throws Exception {
        List<String> warnings = new ArrayList<>();
        List<StaffCustomItem> items = parse(String.join("\n", "CUSTOM-ITEMS:\n", "  missing-slot:\n", "    COMMANDS:\n", "    - 'spawn'\n", "  too-high:\n", "    SLOT: 9\n", "    COMMANDS:\n", "    - 'spawn'\n"), warnings);

        assertTrue(items.isEmpty());
        assertEquals(2, warnings.size());
    }

    @Test
    void rejectsSlotsHeldByBuiltInToolsAndOtherCustomItems() throws Exception {
        List<String> warnings = new ArrayList<>();
        List<StaffCustomItem> items = parse(String.join("\n", "CUSTOM-ITEMS:\n", "  clashes-with-vanish:\n", "    SLOT: 0\n", "    COMMANDS:\n", "    - 'spawn'\n", "  first:\n", "    SLOT: 2\n", "    COMMANDS:\n", "    - 'spawn'\n", "  second:\n", "    SLOT: 2\n", "    COMMANDS:\n", "    - 'spawn'\n"), warnings);

        assertEquals(1, items.size());
        assertEquals("FIRST", items.get(0).id());
        assertEquals(2, warnings.size());
    }

    @Test
    void rejectsEntriesWithoutRunnableCommands() throws Exception {
        List<String> warnings = new ArrayList<>();
        List<StaffCustomItem> items = parse(String.join("\n", "CUSTOM-ITEMS:\n", "  no-commands:\n", "    SLOT: 2\n", "  blank-commands:\n", "    SLOT: 3\n", "    COMMANDS:\n", "    - '   '\n", "    - '/'\n"), warnings);

        assertTrue(items.isEmpty());
        assertEquals(2, warnings.size());
    }

    @Test
    void rejectsAnUnknownExecuteAsInsteadOfGuessing() throws Exception {
        // Falling back to PLAYER would be surprising, and falling back to CONSOLE would be unsafe,
        // so a typo must drop the item rather than run it with the wrong rights.
        List<String> warnings = new ArrayList<>();
        List<StaffCustomItem> items = parse(String.join("\n", "CUSTOM-ITEMS:\n", "  typo:\n", "    SLOT: 2\n", "    EXECUTE-AS: SERVER\n", "    COMMANDS:\n", "    - 'spawn'\n"), warnings);

        assertTrue(items.isEmpty());
        assertEquals(1, warnings.size());
    }

    @Test
    void keepsAnUnknownMaterialUsableWithAWarning() throws Exception {
        List<String> warnings = new ArrayList<>();
        List<StaffCustomItem> items = parse(String.join("\n", "CUSTOM-ITEMS:\n", "  bad-material:\n", "    SLOT: 2\n", "    MATERIAL: NOT_A_REAL_BLOCK\n", "    COMMANDS:\n", "    - 'spawn'\n"), warnings);

        assertEquals(1, items.size());
        assertEquals(Material.STONE, items.get(0).material());
        assertEquals(1, warnings.size());
    }

    @Test
    void bundledExampleIsValidAndShipsDisabled() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(java.nio.file.Paths.get("src/main/resources", "staff-mode.yml").toFile());

        assertNotNull(config.getConfigurationSection("CUSTOM-ITEMS"),
                "staff-mode.yml must document the CUSTOM-ITEMS section");
        assertFalse(config.getBoolean("CUSTOM-ITEMS.EXAMPLE.ENABLED", true),
                "the bundled example must ship disabled so a config restore never arms a command item");
        assertNotNull(config.getString("MESSAGES.CUSTOM-ITEM-NO-TARGET"),
                "the require-target rejection needs a configurable message");

        List<String> warnings = new ArrayList<>();
        StaffCustomItem.parseAll(config.getConfigurationSection("CUSTOM-ITEMS"), BUILT_IN_SLOTS, warnings::add);
        assertTrue(warnings.isEmpty(), () -> "bundled example is misconfigured: " + warnings);

        // The example is only useful if it loads once an admin flips ENABLED.
        config.set("CUSTOM-ITEMS.EXAMPLE.ENABLED", true);
        List<StaffCustomItem> enabled = StaffCustomItem.parseAll(
                config.getConfigurationSection("CUSTOM-ITEMS"), BUILT_IN_SLOTS, warnings::add);
        assertEquals(1, enabled.size(), () -> "bundled example does not load when enabled: " + warnings);
        assertEquals(StaffCustomItem.ExecuteAs.PLAYER, enabled.get(0).executeAs());
    }

    @Test
    void aRenamedEnabledEntryOnAFreeSlotLoads() throws Exception {
        // The shape reported in #145: the bundled example renamed, armed, and moved to a free slot.
        List<String> warnings = new ArrayList<>();
        List<StaffCustomItem> items = parse(String.join("\n", "CUSTOM-ITEMS:\n", "  SUS:\n", "    ENABLED: true\n", "    SLOT: 6\n", "    MATERIAL: TNT\n", "    NAME: '&cS&cU&cS'\n", "    LORE:\n", "    - '&7Click to open /sus'\n", "    EXECUTE-AS: PLAYER\n", "    PERMISSION: ''\n", "    REQUIRE-TARGET: false\n", "    COMMANDS:\n", "    - 'sus'\n"), warnings);

        assertEquals(1, items.size(), () -> "the entry was rejected: " + warnings);
        StaffCustomItem item = items.get(0);
        assertEquals("SUS", item.id());
        assertEquals(6, item.slot());
        assertEquals(Material.TNT, item.material());
        assertEquals(StaffCustomItem.ExecuteAs.PLAYER, item.executeAs());
        assertFalse(item.hasPermission(), "an empty PERMISSION must not gate the item");
        assertEquals(java.util.Collections.singletonList("sus"), item.commands());
        assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);
    }

    private static List<StaffCustomItem> parse(String yaml, List<String> warnings) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return StaffCustomItem.parseAll(config.getConfigurationSection("CUSTOM-ITEMS"), BUILT_IN_SLOTS, warnings::add);
    }
}
