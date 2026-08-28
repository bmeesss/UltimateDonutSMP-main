package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.utils.LegacyScoreboardText;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;

import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardLineSplitTest {

    // The dagger the default scoreboard ships with. Outside the BMP, so Java holds it as a surrogate pair.
    private static final String DAGGER = Character.toString(0x1F5E1);

    // Bukkit 1.12.2 allows at most 16 raw characters per team prefix and per team suffix.
    private static final int TEAM_PART_LIMIT = LegacyScoreboardText.MAX_TEAM_PART_LENGTH;

    private ScoreboardManager manager() throws Exception {
        // The real constructor needs a live server; the line splitting only reads its arguments.
        Constructor<Object> objectConstructor = Object.class.getConstructor();
        Constructor<?> managerConstructor = ReflectionFactory.getReflectionFactory()
                .newConstructorForSerialization(ScoreboardManager.class, objectConstructor);
        return (ScoreboardManager) managerConstructor.newInstance();
    }

    private int findSafeSplit(String text, int max) throws Exception {
        Method findSafeSplit = ScoreboardManager.class.getDeclaredMethod("findSafeSplit", String.class, int.class);
        findSafeSplit.setAccessible(true);
        return (int) findSafeSplit.invoke(manager(), text, max);
    }

    private String[] halvesOf(String text) throws Exception {
        String[] halves = new String[2];
        Team team = (Team) Proxy.newProxyInstance(
                Team.class.getClassLoader(),
                new Class<?>[]{Team.class},
                (proxy, method, args) -> {
                    if ("setPrefix".equals(method.getName())) halves[0] = (String) args[0];
                    if ("setSuffix".equals(method.getName())) halves[1] = (String) args[0];
                    return null;
                });

        Method applyLineSpigot = ScoreboardManager.class.getDeclaredMethod("applyLineSpigot", Team.class, String.class);
        applyLineSpigot.setAccessible(true);
        applyLineSpigot.invoke(manager(), team, text);
        return halves;
    }

    private boolean hasUnpairedSurrogate(String text) {
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isHighSurrogate(current)) {
                if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1))) return true;
            } else if (Character.isLowSurrogate(current)) {
                if (i == 0 || !Character.isHighSurrogate(text.charAt(i - 1))) return true;
            }
        }
        return false;
    }

    private boolean hasDanglingSectionSign(String text) {
        return !text.isEmpty() && text.charAt(text.length() - 1) == '\u00A7';
    }

    @Test
    void shortLinesStayInThePrefixAlone() throws Exception {
        String[] halves = halvesOf("\u00A7aMoney: 1k");
        assertEquals("\u00A7aMoney: 1k", halves[0]);
        assertEquals("", halves[1]);
    }

    @Test
    void longLinesSplitIntoBoundedPrefixAndSuffix() throws Exception {
        String[] halves = halvesOf("a".repeat(64));
        assertTrue(halves[0].length() <= TEAM_PART_LIMIT, "prefix length " + halves[0].length());
        assertTrue(halves[1].length() <= TEAM_PART_LIMIT, "suffix length " + halves[1].length());
        assertEquals("a".repeat(TEAM_PART_LIMIT), halves[0]);
        assertEquals("a".repeat(TEAM_PART_LIMIT), halves[1]);
    }

    @Test
    void theSuffixReopensTheColourThatWasActiveAtTheSplit() throws Exception {
        String line = "\u00A7c" + "a".repeat(40);
        String[] halves = halvesOf(line);
        assertTrue(halves[1].startsWith("\u00A7c"), halves[1]);
        assertTrue(halves[1].length() <= TEAM_PART_LIMIT, "suffix length " + halves[1].length());
        assertEquals("\u00A7c" + "a".repeat(TEAM_PART_LIMIT - 2), halves[0]);
        assertEquals("\u00A7c" + "a".repeat(TEAM_PART_LIMIT - 2), halves[1]);
    }

    @Test
    void theSplitNeverLandsInsideAnEmoji() throws Exception {
        // The dagger starts at index 15, so the 16 character cut would land between its halves.
        String line = "a".repeat(15) + DAGGER + DAGGER + "b".repeat(20);
        String[] halves = halvesOf(line);
        assertFalse(hasUnpairedSurrogate(halves[0]), halves[0]);
        assertFalse(hasUnpairedSurrogate(halves[1]), halves[1]);
        assertFalse(hasDanglingSectionSign(halves[0]));
        assertTrue(halves[0].length() <= TEAM_PART_LIMIT);
        assertTrue(halves[1].length() <= TEAM_PART_LIMIT);
        // The first half backed off so the dagger could stay whole in the suffix.
        assertEquals("a".repeat(15), halves[0]);
        assertTrue(halves[1].startsWith(DAGGER));
    }

    @Test
    void theSplitNeverLandsInsideAColourCode() throws Exception {
        String line = "a".repeat(15) + "\u00A7c" + "b".repeat(20);
        String[] halves = halvesOf(line);
        assertFalse(hasDanglingSectionSign(halves[0]));
        assertTrue(halves[0].length() <= TEAM_PART_LIMIT);
        assertTrue(halves[1].length() <= TEAM_PART_LIMIT);
        // The suffix re-opens the colour that the prefix was about to introduce.
        assertTrue(halves[1].startsWith("\u00A7c"), halves[1]);
    }

    @Test
    void findSafeSplitStillBacksOffPastASurrogateAndASectionSign() throws Exception {
        String emoji = "a".repeat(62) + "\u00A7" + DAGGER + "b".repeat(20);
        assertEquals(62, findSafeSplit(emoji, 64));

        String section = "a".repeat(63) + "\u00A7c" + "b".repeat(20);
        assertEquals(63, findSafeSplit(section, 64));

        assertEquals(64, findSafeSplit("a".repeat(100), 64));
        assertEquals("a".repeat(20), "a".repeat(64).substring(0, findSafeSplit("a".repeat(64), 64)));
    }
}
