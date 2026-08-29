package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport;
import com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport.Icon;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards two worth regressions seen on the 1.12.2 release: visual approximation icons leaking a
 * modern material's price onto a completely different legacy item, and the worth lore marker
 * printing its detection text into the tooltip.
 */
class WorthLegacyAliasRegressionTest {

    /**
     * The alias table maps "legacy item an inventory can actually hold" -> configured worth keys.
     * Only EXACT renames may enter it; an approximation (netherite block shown as a glowstone)
     * must not hand the netherite price to every glowstone a player picked up.
     */
    @Test
    @SuppressWarnings("unchecked")
    void approximatedIconsNeverAliasWorthKeys() throws Exception {
        YamlConfiguration worth = new YamlConfiguration();
        worth.set("ORES.GLOWSTONE", 3.0);
        worth.set("ORES.IRON_ORE", 10.0);
        // Introduced after 1.12.2; the compatibility layer only has an approximate stand-in.
        worth.set("ORES.NETHERITE_BLOCK", 225000.0);
        worth.set("ORES.DEEPSLATE_IRON_ORE", 42.0);
        // A true 1.13 rename stays eligible: 1.12.2 calls oak planks just WOOD.
        worth.set("OTHER.OAK_PLANKS", 0.1);

        WorthManager manager = allocateWorthManager();
        Field aliasesField = WorthManager.class.getDeclaredField("worthKeyAliases");
        aliasesField.setAccessible(true);
        aliasesField.set(manager, new HashMap<String, List<String>>());

        Method collect = WorthManager.class.getDeclaredMethod(
                "collectWorthKeyAliases",
                org.bukkit.configuration.ConfigurationSection.class,
                java.util.Set.class);
        collect.setAccessible(true);
        collect.invoke(manager, worth, new HashSet<String>());

        Map<String, List<String>> aliases = (Map<String, List<String>>) aliasesField.get(manager);

        List<String> glowstone = aliases.get(Material.GLOWSTONE.name() + ":0");
        assertTrue(glowstone == null || !glowstone.contains("NETHERITE_BLOCK"),
                "glowstone must not inherit the netherite block worth, got " + glowstone);
        assertTrue(glowstone != null && glowstone.contains("GLOWSTONE"),
                "the exact legacy key must still be present for the glowstone item");

        List<String> ironOre = aliases.get(Material.IRON_ORE.name() + ":0");
        assertTrue(ironOre == null || !ironOre.contains("DEEPSLATE_IRON_ORE"),
                "surface iron ore must not inherit the deepslate variant price, got " + ironOre);

        List<String> planks = aliases.get(Material.WOOD.name() + ":0");
        assertTrue(planks != null && planks.contains("OAK_PLANKS"),
                "exact renames keep aliasing, otherwise 1.12.2 items lose their configured worth");
    }

    /** The approximation layer itself must carry the exactness flag on every borrowed stand-in. */
    @Test
    void approximationFlagsStayOnBorrowedIcons() {
        assertFalse(resolvesExactly("NETHERITE_BLOCK"));
        assertFalse(resolvesExactly("DEEPSLATE_GOLD_ORE"));
        assertFalse(resolvesExactly("CROSSBOW"));
        assertFalse(resolvesExactly("SKELETON_SPAWN_EGG"));
        assertTrue(resolvesExactly("GLOWSTONE"));
        assertTrue(resolvesExactly("OAK_PLANKS"));
        assertTrue(resolvesExactly("ENCHANTED_GOLDEN_APPLE"));
        assertTrue(resolvesExactly("MAGMA"));
    }

    private static boolean resolvesExactly(String key) {
        Icon icon = LegacyMaterialSupport.resolve(key);
        return icon != null && icon.isExact();
    }

    /**
     * The marker may only consist of {@code §} sequences: anything else is visible text in the
     * tooltip, which is exactly how the "WORTH:" regression appeared in-game.
     */
    @Test
    void worthLoreMarkerCarriesNoVisibleText() throws Exception {
        Field markerField = WorthManager.class.getDeclaredField("WORTH_LORE_MARKER");
        markerField.setAccessible(true);
        String marker = (String) markerField.get(null);
        assertTrue(Pattern.compile("^(\\u00A7[0-9A-Fa-fK-Ok-oRrXx])+$").matcher(marker).matches(),
                "marker must be pure formatting, got " + marker.replace('\u00A7', '&'));
    }

    /** Old saved items still carry the visible marker; the stripper must keep finding them. */
    @Test
    void legacyVisibleMarkersAreStillRecognised() throws Exception {
        Method detect = WorthManager.class.getDeclaredMethod("isWorthLoreLine", String.class);
        detect.setAccessible(true);
        assertTrue((Boolean) detect.invoke(null, "\u00A70\u00A7rWORTH: \u00A77Worth \u00A7a$5"));
        assertTrue((Boolean) detect.invoke(null, "\u00A70\u00A7r\u00A7r\u00A7r\u00A77Worth \u00A7a$5"));
        assertFalse((Boolean) detect.invoke(null, "\u00A77Worth: \u00A7a$5"));
        assertFalse((Boolean) detect.invoke(null, "WORTH: $5"));
        assertFalse((Boolean) detect.invoke(null, (Object) null));
    }

    private static WorthManager allocateWorthManager() throws Exception {
        Field theUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafeField.get(null);
        return (WorthManager) unsafe.allocateInstance(WorthManager.class);
    }

}
