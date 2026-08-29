package com.bx.ultimateDonutSmp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 1.12.2 fallback that flattens tab list name components into legacy {@code §} text. The
 * native component routes (PaperAdventure, ChatSerializer JSON, static literal factories) all
 * miss on a stock 1.12.2 Spigot server, so this conversion is what keeps formatted, untruncated
 * names on the tab; a regression here silently falls back to the 16-character plain list name.
 */
class TablistComponentLegacyTextTest {

    @Test
    void coloursAndStylesBecomeLegacyCodes() {
        Component name = Component.text("Owner", NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(" Notch", NamedTextColor.GOLD));

        // NamedTextColor.RED (#FF5555) and GOLD (#FFAA00) are byte-for-byte the legacy §c and
        // §6 palette entries, so the nearest-colour collapse must be an identity mapping here.
        // §e is YELLOW (#FFFF55), not GOLD - the original expectation had that backwards.
        assertEquals("\u00A7c\u00A7lOwner\u00A76 Notch", TablistComponentUpdater.componentToLegacyText(name));

        Component sunny = Component.text("sun", NamedTextColor.YELLOW);
        assertEquals("\u00A7esun", TablistComponentUpdater.componentToLegacyText(sunny));
    }

    @Test
    void hexColoursCollapseToTheNearestLegacyColour() {
        Component name = Component.text("Donut", TextColor.color(0xFF, 0xC0, 0xCB));

        String legacy = TablistComponentUpdater.componentToLegacyText(name);
        // The sixteen-colour palette has no pink: light purple or white are the valid nearest
        // matches; whichever the collapse picks, no markup may survive the conversion.
        assertFalse(legacy.contains("&"), "no unprocessed markup may reach the client");
        assertTrue(legacy.startsWith("\u00A7") && legacy.endsWith("Donut") && legacy.length() == 7,
                "one legacy colour code plus the plain name, got " + legacy);
    }

    @Test
    void longFormattedNamesStayIntactInsteadOfBeingTrimmed() {
        Component name = Component.text("\u00A77[\u00A7cSuperLongRankName\u00A77] \u00A7fPlayername123");

        String legacy = TablistComponentUpdater.componentToLegacyText(name);
        assertTrue(legacy.length() > LegacyScoreboardText.MAX_PLAYER_LIST_NAME_LENGTH,
                "the component route exists to carry names the plain 16-char list name cannot");
    }
}
