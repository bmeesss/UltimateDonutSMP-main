package com.bx.ultimateDonutSmp.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.12.2 scoreboard text sanitisation: hex colours convert to the nearest legacy colour, the
 * raw string never exceeds the API limits, and no malformed section-sign sequence can survive.
 */
class LegacyScoreboardTextTest {

    private static final char S = '\u00A7';

    /** Raw length, every § followed by a known code, no dangling §, no §x remains. */
    private static boolean wellFormedLegacy(String s, int limit) {
        if (s == null || s.length() > limit) {
            return false;
        }
        String codes = "0123456789abcdefklmnor";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == S) {
                if (i + 1 >= s.length()) {
                    return false; // dangling section sign
                }
                char code = Character.toLowerCase(s.charAt(i + 1));
                if (codes.indexOf(code) < 0) {
                    return false; // unknown or broken code (§x included)
                }
                i++;
            }
        }
        return true;
    }

    private static String sectionHex(String hex) {
        StringBuilder sb = new StringBuilder();
        sb.append(S).append('x');
        for (int i = 0; i < 6; i++) {
            sb.append(S).append(hex.charAt(i));
        }
        return sb.toString();
    }

    private static String stripCodes(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == S) {
                i++;
                continue;
            }
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    @Test
    void shippedGradientTitleFitsThe32CharacterObjectiveLimit() {
        // The exact title from scoreboard.yml: per-letter hex gradient plus bold codes.
        String raw = "&#0069d6&lE&#0374da&lc&#067fdf&lo&#0a8be3&ln&#0d96e7&lo&#10a1ec&lm"
                + "&#13acf0&ly&#17b8f4&lS&#1ac3f9&lM&#1dcefd&lP";
        String sanitized = LegacyScoreboardText.sanitizeObjectiveName(raw);
        assertTrue(sanitized.length() <= 32, "raw length " + sanitized.length());
        assertTrue(wellFormedLegacy(sanitized, 32), sanitized);
        assertEquals("EconomySMP", stripCodes(sanitized), sanitized);
        assertTrue(sanitized.indexOf(S) >= 0, "colour should survive");
    }

    @Test
    void sectionHexFormConvertsToNearestLegacyColour() {
        String converted = LegacyScoreboardText.toLegacyColors(sectionHex("FF5555") + "red");
        assertEquals(String.valueOf(S) + "c" + "red", converted);
    }

    @Test
    void malformedSectionHexIsDroppedNotHalfPreserved() {
        String malformed = "abc" + S + "x" + S + "f";
        String converted = LegacyScoreboardText.toLegacyColors(malformed);
        assertTrue(wellFormedLegacy(converted, Integer.MAX_VALUE), converted);
    }

    @Test
    void plainLegacyCodesPassThroughUntouched() {
        String plain = S + "a" + S + "l" + "Money";
        assertEquals(plain, LegacyScoreboardText.toLegacyColors(plain));
    }

    @Test
    void rawHexFormsConvert() {
        String tagged = LegacyScoreboardText.toLegacyColors(
                "<#FF5555>red</#FF5555> {#55FF55}green &#5555FFblue #FFFF55y");
        assertEquals(String.valueOf(S) + "c" + "red" + S + "r" + " " + S + "a" + "green "
                + S + "9" + "blue " + S + "e" + "y", tagged);
    }

    @Test
    void nearestLegacyColourUsesPerceptualDistance() {
        assertEquals('c', LegacyScoreboardText.nearestLegacyCode(0xFF, 0x55, 0x55));
        assertEquals('f', LegacyScoreboardText.nearestLegacyCode(255, 255, 255));
        assertEquals('a', LegacyScoreboardText.nearestLegacyCode(85, 255, 85));
        assertEquals('9', LegacyScoreboardText.nearestLegacyCode(0x00, 0x69, 0xD6));
    }

    @Test
    void truncationNeverSplitsAColourCodeOrSurrogatePair() {
        String dagger = new String(Character.toChars(0x1F5E1));
        String cut = LegacyScoreboardText.truncate("abcdefghijklmno" + dagger + dagger, 16);
        assertTrue(cut.length() <= 16, "length " + cut.length());
        assertTrue(wellFormedLegacy(cut, 16), cut);
        boolean paired = true;
        for (int i = 0; i < cut.length(); i++) {
            char c = cut.charAt(i);
            if (Character.isHighSurrogate(c)) {
                paired = i + 1 < cut.length() && Character.isLowSurrogate(cut.charAt(i + 1));
                break;
            }
        }
        assertTrue(paired, cut);
    }

    @Test
    void truncationDropsTrailingCodesThatColourNothing() {
        String truncated = LegacyScoreboardText.truncate("abc" + S + "c", 4);
        assertEquals("abc", truncated);
    }

    @Test
    void teamPartAndPlayerListNameStayWithinTheirLimits() {
        String longLine = sectionHex("A303F9") + "Shards 123,456 and more text";
        String teamPart = LegacyScoreboardText.sanitize(longLine, LegacyScoreboardText.MAX_TEAM_PART_LENGTH);
        String listName = LegacyScoreboardText.sanitizePlayerListName(sectionHex("00A4FC") + "VeryLongPlayerName");
        assertTrue(teamPart.length() <= 16, "length " + teamPart.length());
        assertTrue(wellFormedLegacy(teamPart, 16), teamPart);
        assertTrue(listName.length() <= 16, "length " + listName.length());
        assertTrue(wellFormedLegacy(listName, 16), listName);
    }

    @Test
    void collapseRemovesRedundantRepeatsButKeepsColourRestatements() {
        String repeated = S + "9" + S + "l" + "E" + S + "9" + S + "l" + "c" + S + "9" + S + "l" + "o";
        assertEquals(String.valueOf(S) + "9" + S + "l" + "Eco", LegacyScoreboardText.collapse(repeated));
        // A restated colour resets formatting in the legacy protocol, so it must be kept.
        String restated = S + "9" + S + "l" + "E" + S + "9" + "c";
        assertEquals(restated, LegacyScoreboardText.collapse(restated));
    }

    @Test
    void codesActiveAtReopensColourStateAfterASplit() {
        String source = S + "a" + S + "l" + "abcdefghijklmnop";
        assertEquals(String.valueOf(S) + "a" + S + "l", LegacyScoreboardText.codesActiveAt(source, 8));
        assertEquals("", LegacyScoreboardText.codesActiveAt("abcdefghijklmnop", 8));
    }

    @Test
    void nullAndEmptyInputSanitiseToEmpty() {
        assertEquals("", LegacyScoreboardText.sanitizeObjectiveName(null));
        assertEquals("", LegacyScoreboardText.sanitizeObjectiveName(""));
        assertEquals("", LegacyScoreboardText.sanitizePlayerListName(null));
    }

    @Test
    void exactBoundaryCasesAroundTheLimits() {
        // Exactly 32 raw characters pass through unchanged.
        StringBuilder exact = new StringBuilder("\u00A79\u00A7lEconomySMP title!!");
        while (exact.length() < 32) {
            exact.append('!');
        }
        assertEquals(32, exact.length());
        assertEquals(exact.toString(), LegacyScoreboardText.sanitizeObjectiveName(exact.toString()));

        // 33 visible characters become exactly 32.
        assertEquals(32, LegacyScoreboardText.sanitizeObjectiveName(repeat("a", 33)).length());

        // 31 visible characters + a colour code (33 raw): the code colours nothing and is dropped.
        String withCode = repeat("b", 31) + S + "c";
        assertEquals(33, withCode.length());
        String dropped = LegacyScoreboardText.sanitizeObjectiveName(withCode);
        assertEquals(31, dropped.length());
        assertTrue(dropped.endsWith("b"), dropped);

        // Exactly 16 raw team-part characters stay whole; 17 cut safely.
        String team16 = S + "a" + repeat("x", 14);
        assertEquals(16, team16.length());
        assertEquals(team16, LegacyScoreboardText.sanitize(team16, LegacyScoreboardText.MAX_TEAM_PART_LENGTH));
        String team17 = S + "a" + repeat("x", 15);
        String cut17 = LegacyScoreboardText.truncate(team17, LegacyScoreboardText.MAX_TEAM_PART_LENGTH);
        assertEquals(16, cut17.length());
        assertTrue(wellFormedLegacy(cut17, 16), cut17);

        // 14 visible characters + one emoji (2 code units) fit exactly 16 and stay whole.
        String dagger = new String(Character.toChars(0x1F5E1));
        String emoji16 = repeat("y", 14) + dagger;
        String emojiKept = LegacyScoreboardText.truncate(emoji16, LegacyScoreboardText.MAX_TEAM_PART_LENGTH);
        assertEquals(16, emojiKept.length());
        assertTrue(emojiKept.endsWith(dagger));
    }

    @Test
    void everySanitisedStringIsBoundedAndWellFormed() {
        String[] battery = {
                "", "plain", S + "r", S + "0" + S + "1" + S + "2",
                sectionHex("ABCDEF") + sectionHex("012345") + sectionHex("FEDCBA"),
                "&#ffffff&#000000&#ffffff", "<#abcdef>",
                "team &#FC0000⚔ &fKills &#FC000099",
        };
        for (String input : battery) {
            String objective = LegacyScoreboardText.sanitizeObjectiveName(input);
            String team = LegacyScoreboardText.sanitize(input, LegacyScoreboardText.MAX_TEAM_PART_LENGTH);
            assertTrue(objective.length() <= 32, input);
            assertTrue(wellFormedLegacy(objective, 32), objective);
            assertTrue(team.length() <= 16, input);
            assertTrue(wellFormedLegacy(team, 16), team);
        }
    }

    @Test
    void hexOnlyTitleKeepsASensibleLegacyColour() {
        String sanitized = LegacyScoreboardText.sanitizeObjectiveName(sectionHex("0069D6") + "EconomySMP");
        assertTrue(sanitized.startsWith(String.valueOf(S)), sanitized);
        assertEquals("EconomySMP", stripCodes(sanitized));
    }
    private static String repeat(String unit, int count) {
        StringBuilder builder = new StringBuilder(unit.length() * Math.max(0, count));
        for (int i = 0; i < count; i++) {
            builder.append(unit);
        }
        return builder.toString();
    }
}
