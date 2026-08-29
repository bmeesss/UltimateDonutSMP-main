package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.utils.LegacyScoreboardText;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PingManagerTest {

    @Test
    void nullPlayerReturnsZeroPing() throws Exception {
        // PingManager's constructor registers listeners and starts a task on the live server,
        // which a unit test cannot provide. The null-player contract of getPing does not touch
        // any of that state, so an instance allocated without running the constructor is enough.
        PingManager pingManager = allocateWithoutConstructor();
        assertEquals(0, pingManager.getPing(null));
    }

    private static PingManager allocateWithoutConstructor() throws Exception {
        Field theUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafeField.get(null);
        return (PingManager) unsafe.allocateInstance(PingManager.class);
    }

    // ---------------------------------------------------------------- formatting

    @Test
    void formatPingRendersMeasuredNumbersVerbatim() {
        // The number shown is exactly the number measured - no rounding, no re-scaling,
        // no substitution of "better looking" values.
        assertEquals("42", PingManager.formatPing(42));
        assertEquals("0", PingManager.formatPing(0));   // real 0 ms hop behind a local proxy
        assertEquals("1", PingManager.formatPing(1));   // real 1 ms must still be possible
        assertEquals("314", PingManager.formatPing(314));
    }

    @Test
    void formatPingRendersUnknownAsQuestionMark() {
        assertEquals("?", PingManager.formatPing(PingManager.UNKNOWN));
        assertEquals("?", PingManager.formatPing(-1));
        assertEquals("?", PingManager.formatPing(-999));
        assertEquals(-1, PingManager.UNKNOWN);
        assertEquals("?", PingManager.UNKNOWN_DISPLAY);
    }

    @Test
    void formattingIsDeterministicUnderRepeatedRefreshes() {
        // The scoreboard re-renders every few seconds; the same measurement must always
        // produce the same string, or the display flickers between refreshes.
        for (int i = 0; i < 1000; i++) {
            assertEquals("42", PingManager.formatPing(42));
            assertEquals("?", PingManager.formatPing(PingManager.UNKNOWN));
        }
    }

    // ---------------------------------------------------------------- no invented latencies

    private static String pingManagerSource() throws Exception {
        Path file = Paths.get("src/main/java/com/bx/ultimateDonutSmp/managers/PingManager.java");
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test
    void managerNeverInventsAFallbackLatency() throws Exception {
        String source = pingManagerSource();
        // The old code answered "unmeasured" with a fabricated 1 ms and also pushed that
        // number into the NMS latency field, overwriting the server's real measurement and
        // the tab signal bars. Both behaviours must stay gone.
        assertFalse(source.contains("int fallback"), "no invented fallback constant allowed");
        assertFalse(source.contains("return 1;"), "getPing must never return a hardcoded 1");
        assertFalse(source.contains("setNmsLatency"), "no writes into the NMS latency field");
        assertFalse(source.contains("Random"), "ping values must never be randomized");
        assertFalse(source.contains("updateInventory"), "ping refresh must not touch inventories");
    }

    @Test
    void getPingReadsTheServersOwnLatencyFirst() throws Exception {
        String source = pingManagerSource();
        Matcher body = Pattern.compile("public int getPing\\(Player player\\) \\{([\\s\\S]*?)\\n    }")
                .matcher(source);
        assertTrue(body.find(), "getPing(Player) method expected in PingManager");
        String getPing = body.group(1);
        int serverRead = getPing.indexOf("readServerLatency(player)");
        int cacheRead = getPing.indexOf("pingCache.get(");
        assertTrue(serverRead >= 0, "must read EntityPlayer#ping / Player#getPing() reflectively");
        assertTrue(cacheRead >= 0, "ProtocolLib keep-alive cache remains the secondary source");
        assertTrue(serverRead < cacheRead, "the server's own measurement takes precedence");
        assertTrue(getPing.contains("return UNKNOWN;"),
                "an unmeasured player must yield UNKNOWN, never a stand-in number");
    }

    @Test
    void managerAvoidsModernApiAndTickingTasks() throws Exception {
        String source = pingManagerSource();
        // 1.16+ would allow player.getPing() directly; 1.12.2 does not, so the method may
        // only ever be looked up by name via reflection.
        assertFalse(source.contains("player.getPing()"), "no compile-time dependency on modern Player#getPing");
        assertFalse(source.contains("var "), "Java 8 source level");
        // Sampling stays on the existing 5 s global refresh (plus the vanilla ~20 s keep-alive
        // cadence); no new per-tick ping task may appear.
        assertEquals(1, source.split("runGlobalTimer", -1).length - 1,
                "exactly one scheduled refresh loop expected");
        assertTrue(source.contains("100L, 100L"), "refresh cadence must stay at 5 s (100 ticks)");
    }

    // ---------------------------------------------------------------- scoreboard UI

    private static YamlConfiguration loadTemplate() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(
                PingManagerTest.class.getClassLoader().getResourceAsStream("scoreboard.yml"),
                StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        return config;
    }

    private static String pingLine(YamlConfiguration config) {
        for (String line : config.getStringList("SCOREBOARD.LINES")) {
            if (line != null && line.contains("%economy_ping%")) {
                return line;
            }
        }
        throw new AssertionError("scoreboard.yml must contain a line with %economy_ping%");
    }

    @Test
    void scoreboardPingLineSaysPingInsteadOfARegion() throws Exception {
        String line = pingLine(loadTemplate());
        // The parenthesized value was the ping all along, hidden behind a "NA East" region
        // label; the line must now announce itself as the ping it renders.
        assertEquals("&7Ping: &#0069D6%economy_ping%ms", line);
        List<String> lines = loadTemplate().getStringList("SCOREBOARD.LINES");
        for (String candidate : lines) {
            assertFalse(candidate.contains("East") || candidate.contains("West"),
                    "the scoreboard must not label the latency line as a region: " + candidate);
        }
    }

    @Test
    void renderedPingLineFitsLegacyTeamLimitsAndKeepsText() throws Exception {
        String converted = LegacyScoreboardText.toLegacyColors(
                pingLine(loadTemplate()).replace("%economy_ping%", "42"));
        assertEquals("Ping: 42ms", converted.replaceAll("§.", ""));
        assertTrue(converted.startsWith("§7Ping: "), "viewer design keeps the dim prefix");
        // The old line carried the value inside "&7(...)" around a second color code; the
        // new one must stay inside the 16-char prefix budget on its own.
        assertTrue(converted.length() <= LegacyScoreboardText.MAX_TEAM_PART_LENGTH,
                "prefix-only line budget exceeded: " + converted.length());
        assertFalse(converted.contains("§x"), "hex accent must collapse to a legacy color code");
        // Scoreboard diffing compares rendered strings: re-converting an already-legacy
        // string must be a no-op so a stable ping never flickers across refreshes.
        assertEquals(converted, LegacyScoreboardText.toLegacyColors(converted));
    }

    // ---------------------------------------------------------------- consumers

    private static String sourceOf(String relative) throws Exception {
        Path file = Paths.get("src/main/java/com/bx/ultimateDonutSmp", relative);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test
    void everyPingDisplayRoutesThroughFormatPing() throws Exception {
        String expansion = sourceOf("api/EconomyExpansion.java");
        Matcher block = Pattern.compile("equalsIgnoreCase\\(\"ping\"\\)\\) \\{([^}]*)}")
                .matcher(expansion);
        assertTrue(block.find(), "%economy_ping% parameter block expected");
        assertTrue(block.group(1).contains("formatPing"), "placeholder must format, not stringify raw");
        assertFalse(block.group(1).contains("return \"0\""),
                "offline players must not be reported as a measured 0 ms");

        assertTrue(sourceOf("commands/PingCommand.java").contains("formatPing"));
        assertTrue(sourceOf("menus/RTPMenu.java").contains("formatPing"));
        // The tablist PlayerInfoData packet still needs a raw non-negative int.
        assertTrue(sourceOf("managers/HideProtocolLibBridge.java")
                .contains("Math.max(0, plugin.getPingManager().getPing(target))"));
        // No display may re-introduce a raw String.valueOf of the sentinel.
        for (String file : new String[]{
                "commands/PingCommand.java", "menus/RTPMenu.java", "api/EconomyExpansion.java"}) {
            assertFalse(sourceOf(file).contains("String.valueOf(plugin.getPingManager().getPing"),
                    file + " must not print the raw ping sentinel");
        }
    }

    @Test
    void pingSystemDoesNotTouchTheNametagUi() throws Exception {
        // The scoreboard is the sanctioned ping display (do not move it); Money Nametag
        // must stay independent of the ping pipeline in both directions.
        String nametag = sourceOf("managers/MoneyNametagManager.java");
        assertFalse(nametag.contains("PingManager") || nametag.contains("ping"),
                "Money Nametag must not gain ping couplings");
    }
}
