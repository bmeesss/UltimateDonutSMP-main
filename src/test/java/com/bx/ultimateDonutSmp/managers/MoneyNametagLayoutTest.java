package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.utils.LegacyScoreboardText;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the 1.12.2 money nametag layout: the team suffix the balance is rendered into
 * (sixteen raw characters, legacy colours, safe truncation) and the per-viewer bookkeeping that
 * decides which teams exist and when a line needs rewriting. No server is needed for any of it.
 */
class MoneyNametagLayoutTest {

    private static final int TEAM_PART_LIMIT = LegacyScoreboardText.MAX_TEAM_PART_LENGTH;
    private static final String SECTION = "\u00A7";

    private static UUID uuid(long seed) {
        return new UUID(seed, seed);
    }

    private static void assertWellFormedSuffix(String suffix) {
        assertTrue(suffix.length() <= TEAM_PART_LIMIT,
                "suffix must respect the 1.12.2 team limit, got: " + suffix.length());
        assertFalse(suffix.isEmpty(), "an empty text must render no suffix");
        assertFalse(suffix.endsWith(SECTION), "suffix must not end in a dangling colour code: " + suffix);
        for (int i = 0; i < suffix.length(); i++) {
            char c = suffix.charAt(i);
            if (Character.isHighSurrogate(c)) {
                assertTrue(i + 1 < suffix.length() && Character.isLowSurrogate(suffix.charAt(i + 1)),
                        "suffix must not split a surrogate pair: " + suffix);
            }
        }
    }

    @Test
    void normalWorthRendersIntoSixteenCharacterSuffix() {
        String rendered = MoneyNametagManager.render("&a${balance}", 1_250_000D, false);
        assertEquals("&a$1,250,000", rendered);
        String suffix = MoneyNametagManager.fitTeamSuffix(rendered);
        assertEquals(" " + SECTION + "a$1,250,000", suffix);
        assertWellFormedSuffix(suffix);
    }

    @Test
    void zeroWorthStillShowsALine() {
        String suffix = MoneyNametagManager.fitTeamSuffix(
                MoneyNametagManager.render("&a${balance}", 0D, true));
        assertEquals(" " + SECTION + "a$0", suffix);
        assertWellFormedSuffix(suffix);
    }

    @Test
    void largeWorthUsesTheShortFormatAndStillFits() {
        String suffix = MoneyNametagManager.fitTeamSuffix(
                MoneyNametagManager.render("&a${balance}", 2_500_000_000D, true));
        assertEquals(" " + SECTION + "a$2.5B", suffix);
        assertWellFormedSuffix(suffix);
    }

    @Test
    void negativeValuesAreShownAsTheyAre() {
        String suffix = MoneyNametagManager.fitTeamSuffix(
                MoneyNametagManager.render("&a${balance}", -500D, false));
        assertEquals(" " + SECTION + "a$-500", suffix);
        assertWellFormedSuffix(suffix);
    }

    @Test
    void hexColoursAreReducedToLegacyCodesBeforeTheTeamLimit() {
        String suffix = MoneyNametagManager.fitTeamSuffix("&#0069d6&l$1.25M");
        assertEquals(" " + SECTION + "9" + SECTION + "l$1.25M", suffix);
        assertFalse(suffix.contains("x"), "no §x hex sequence may reach the client: " + suffix);
        assertWellFormedSuffix(suffix);
    }

    @Test
    void aLongBalanceLineIsTruncatedAtAWholeColourCode() {
        String suffix = MoneyNametagManager.fitTeamSuffix("&7Worth: &a$1,250,000");
        assertTrue(suffix.startsWith(" " + SECTION + "7Worth:"));
        assertWellFormedSuffix(suffix);
    }

    @Test
    void fullCommaNumbersStayVisibleWhileTheyFit() {
        // A balance of a trillion with the full format needs 17 raw characters plus the space, so
        // the renderer must shorten it safely instead of dropping colours or cutting a code.
        String suffix = MoneyNametagManager.fitTeamSuffix(
                MoneyNametagManager.render("&a${balance}", 1_000_000_000_000D, false));
        assertTrue(suffix.length() <= TEAM_PART_LIMIT);
        assertTrue(suffix.contains("$1,000,000"), "balance digits should survive until the limit");
        assertWellFormedSuffix(suffix);
    }

    @Test
    void teamNamesStayWithinTheLimitAndAreUniquePerTarget() {
        MoneyNametagState state = new MoneyNametagState();
        UUID first = uuid(1L);
        UUID second = uuid(2L);
        String firstTeam = state.teamNameFor(first);
        String secondTeam = state.teamNameFor(second);
        assertTrue(firstTeam.startsWith("udsm"));
        assertTrue(firstTeam.length() <= MoneyNametagState.MAX_TEAM_NAME_LENGTH);
        assertTrue(secondTeam.length() <= MoneyNametagState.MAX_TEAM_NAME_LENGTH);
        assertFalse(firstTeam.equals(secondTeam));
    }

    @Test
    void aPlayerNameAtThe112LimitIsKeptIntact() {
        String name = "PlayerNameMax16!"; // 16 raw characters, the 1.12.2 maximum
        assertEquals(16, name.length());

        MoneyNametagState state = new MoneyNametagState();
        MoneyNametagState.Entry entry = state.entryFor(uuid(1L), uuid(2L));
        assertFalse(entry.currentlyShows("balance", name));
        entry.text = "balance";
        entry.memberName = name;
        assertTrue(entry.currentlyShows("balance", name));
    }

    @Test
    void multiplePlayersGetTheirOwnEntryPerViewer() {
        MoneyNametagState state = new MoneyNametagState();
        UUID viewerOne = uuid(1L);
        UUID viewerTwo = uuid(2L);
        UUID targetOne = uuid(10L);
        UUID targetTwo = uuid(11L);

        state.entryFor(viewerOne, targetOne);
        state.entryFor(viewerOne, targetTwo);
        state.entryFor(viewerTwo, targetOne);

        List<UUID> viewersOfOne = state.viewersFor(targetOne);
        assertEquals(2, viewersOfOne.size());
        assertTrue(viewersOfOne.contains(viewerOne));
        assertTrue(viewersOfOne.contains(viewerTwo));

        List<UUID> viewersOfTwo = state.viewersFor(targetTwo);
        assertEquals(1, viewersOfTwo.size());
        assertEquals(viewerOne, viewersOfTwo.get(0));

        // The same target keeps the same team name on every viewer's board.
        assertEquals(
                state.entryFor(viewerOne, targetOne).teamName,
                state.entryFor(viewerTwo, targetOne).teamName);
    }

    @Test
    void aBalanceChangeMarksTheLineForARewrite() {
        MoneyNametagState state = new MoneyNametagState();
        MoneyNametagState.Entry entry = state.entryFor(uuid(1L), uuid(2L));
        entry.text = " " + SECTION + "a$1,250,000";
        entry.memberName = "Steve";
        String sentText = entry.text;
        assertTrue(entry.currentlyShows(sentText, "Steve"));

        state.markChanged(uuid(2L));
        assertFalse(entry.currentlyShows(sentText, "Steve"),
                "a changed balance must force the next pass to rewrite the team");
    }

    @Test
    void quittingRemovesTheTargetFromEveryViewer() {
        MoneyNametagState state = new MoneyNametagState();
        UUID viewerOne = uuid(1L);
        UUID viewerTwo = uuid(2L);
        UUID target = uuid(3L);
        MoneyNametagState.Entry beforeOne = state.entryFor(viewerOne, target);
        state.entryFor(viewerTwo, target);

        state.forgetTarget(target);

        assertNull(state.entryOrNull(viewerOne, target));
        assertNull(state.entryOrNull(viewerTwo, target));
        assertTrue(state.viewersFor(target).isEmpty());
        assertFalse(beforeOne.teamName.equals(state.teamNameFor(target)),
                "a returning player gets a fresh team name");
    }

    @Test
    void turningTheFeatureOffOnlyClearsThatViewer() {
        MoneyNametagState state = new MoneyNametagState();
        UUID viewerOne = uuid(1L);
        UUID viewerTwo = uuid(2L);
        UUID target = uuid(3L);
        state.entryFor(viewerOne, target);
        state.entryFor(viewerTwo, target);

        state.forgetViewer(viewerOne);

        assertNull(state.entryOrNull(viewerOne, target));
        assertEquals(1, state.viewersFor(target).size());
        assertTrue(state.entryOrNull(viewerTwo, target) != null);
        assertTrue(state.hasViewer(viewerTwo));
        assertFalse(state.hasViewer(viewerOne));
    }

    @Test
    void noModernNumberFormatOrProtocolLibClassesAreReferenced() throws Exception {
        try (InputStream in = MoneyNametagManager.class.getResourceAsStream(
                "/com/bx/ultimateDonutSmp/managers/MoneyNametagManager.class")) {
            assertTrue(in != null, "class resource must be on the test classpath");
            byte[] bytes = new byte[in.available()];
            int read = 0;
            while (read < bytes.length) {
                int chunk = in.read(bytes, read, bytes.length - read);
                if (chunk < 0) break;
                read += chunk;
            }
            String pool = new String(bytes, 0, read, StandardCharsets.ISO_8859_1);
            assertFalse(pool.contains("WrappedNumberFormat"),
                    "the manager must not reference scoreboard number formats");
            assertFalse(pool.contains("NumberFormat"),
                    "the manager must not reference scoreboard number formats");
            assertFalse(pool.contains("com/comphenix/protocol"),
                    "the manager must not depend on ProtocolLib");
        }
    }
}
