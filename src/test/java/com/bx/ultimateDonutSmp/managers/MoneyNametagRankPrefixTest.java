package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.utils.LegacyScoreboardText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Team prefix fitting for the rank shown in front of nametags. The 1.12.2 protocol accepts at
 * most 16 raw characters per team part (colour codes count), and a prefix that stops mid-colour
 * would paint the player name in a stray colour, so the helper must truncate on code boundaries
 * and keep the trailing reset.
 */
class MoneyNametagRankPrefixTest {

    @Test
    void shortPrefixIsReturnedUnchanged() {
        assertEquals("", MoneyNametagManager.fitTeamPrefix(null));
        assertEquals("", MoneyNametagManager.fitTeamPrefix(""));
        assertEquals("\u00A7c[Owner]\u00A7r", MoneyNametagManager.fitTeamPrefix("\u00A7c[Owner]"));
    }

    @Test
    void longPrefixIsCutToTheTeamPartBudget() {
        String fitted = MoneyNametagManager.fitTeamPrefix("\u00A7c[VeryLongRankNameThatNeverFits]");
        assertTrue(fitted.length() <= LegacyScoreboardText.MAX_TEAM_PART_LENGTH,
                "prefix must respect the 16-char team-part limit, got " + fitted.length());
        assertTrue(fitted.endsWith("\u00A7r"), "the name after it must start from a reset colour");
        assertTrue(fitted.startsWith("\u00A7c[VeryLong"), "the prefix keeps its head, got " + fitted);
        assertEquals(-1, indexOfBrokenCode(fitted), "no half-written colour sequence may survive");
    }

    /** A dangling section sign at the very end would be the broken-code case. */
    private static int indexOfBrokenCode(String value) {
        return value.endsWith("\u00A7") ? 0 : -1;
    }
}
