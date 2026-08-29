package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.utils.LegacyScoreboardText;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled scoreboard template must not use characters outside the basic multilingual plane.
 * 1.12.2 clients (and EaglerCraft) read glyphs from the server's unicode font pages, which only
 * cover the BMP: an astral character such as U+1F5E1 arrives as a surrogate pair the legacy
 * scoreboard cannot split correctly and shows up as a blank box or garbled text. Everything the
 * default lines use has to sit in a range the legacy font pages actually provide.
 *
 * <p>Beyond the BMP the two 1.12.2 client font paths still disagree on coverage: a vanilla
 * client has full unicode pages plus unifont fallback, while an EaglerCraft client only ships
 * the glyph set its font atlas was compiled with. A glyph missing from the atlas is not drawn
 * as a box - FontRenderer gives it zero width, so the character silently vanishes. That is
 * exactly what removed U+2694 CROSSED SWORDS from the kills line on EaglerCraft while Java
 * rendered it. Icons that must show on both platforms therefore live in the guaranteed set:
 * printable ASCII plus Latin-1, which every MC 1.12.2 and Eagler font texture ships (the
 * multiplication sign U+00D7 sits there and is the closest universal stand-in for crossed
 * swords). The tests below pin that contract for the kills line and keep every other line's
 * glyph untouched.
 */
class ScoreboardDefaultGlyphsTest {

    /** Loads the bundled template from the test classpath (target/classes copy of the seed). */
    private static YamlConfiguration loadTemplate() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(
                ScoreboardDefaultGlyphsTest.class.getClassLoader()
                        .getResourceAsStream("scoreboard.yml"),
                StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        return config;
    }

    private static String killLine(YamlConfiguration config) {
        for (String line : config.getStringList("SCOREBOARD.LINES")) {
            if (line != null && line.contains("%economy_kills%")) {
                return line;
            }
        }
        throw new AssertionError("scoreboard.yml must contain a line with %economy_kills%");
    }

    @Test
    void bundledTemplateOnlyUsesBasicPlaneGlyphs() throws Exception {
        YamlConfiguration config = loadTemplate();

        List<String> offenders = new ArrayList<>();
        collectStrings(config, offenders);
        assertTrue(offenders.isEmpty(),
                "scoreboard.yml keeps non-BMP characters in: " + offenders);
    }

    private void collectStrings(org.bukkit.configuration.ConfigurationSection section, List<String> out) {
        for (String key : section.getKeys(true)) {
            if (section.isConfigurationSection(key)) {
                continue;
            }
            for (String line : section.getStringList(key)) {
                check(line, key, out);
            }
            check(section.getString(key), key, out);
        }
    }

    private void check(String line, String key, List<String> out) {
        if (line == null) {
            return;
        }
        line.codePoints().filter(cp -> cp > 0xFFFF)
                .forEach(cp -> out.add(key + " -> U+" + Integer.toHexString(cp).toUpperCase()));
    }

    /**
     * The kills icon must be a glyph both 1.12.2 font systems ship unconditionally: ASCII or
     * Latin-1. U+2694 (the modern emoji-style swords) is the Eagler-invisible character this
     * test guards against, and U+00D7 (multiplication sign) is the pinned replacement.
     */
    @Test
    void killIconIsRenderableOnJava1122AndEaglercraft() throws Exception {
        String kills = killLine(loadTemplate());

        assertTrue(kills.indexOf('\u00D7') >= 0,
                "kills line must use U+00D7 (guaranteed in every MC 1.12.2 / Eagler font) as "
                        + "the kill icon");
        assertFalse(kills.indexOf('\u2694') >= 0,
                "U+2694 CROSSED SWORDS is missing from EaglerCraft font atlases and renders "
                        + "as zero width (invisible); it must not return to the kills line");

        for (int i = 0; i < kills.length(); i++) {
            char c = kills.charAt(i);
            boolean guaranteed = (c >= 0x20 && c <= 0x7E) || (c >= 0xA0 && c <= 0xFF);
            assertTrue(guaranteed,
                    "kills line character U+" + Integer.toHexString(c)
                            + " is outside the ASCII+Latin-1 set both clients guarantee");
        }
    }

    /**
     * The configured kills line must survive the exact runtime pipeline (toLegacyColors ->
     * team prefix/suffix split) with the icon inside the prefix half: colour codes resolved to
     * legacy § without reintroducing §x/hex sequences, the icon present at its original raw
     * position, and both halves within the hard 1.12.2 limits.
     */
    @Test
    void configuredKillLineSurvivesColourProcessingAndSplitLimits() throws Exception {
        String converted = LegacyScoreboardText.toLegacyColors(killLine(loadTemplate()));

        assertFalse(converted.contains("\u00A7x"),
                "legacy scoreboard output must not carry §x hex sequences: " + converted);
        assertFalse(converted.contains("&#"),
                "unprocessed hex remained in: " + converted);
        // FC0000 is red; toLegacyColors must map it onto the nearest legacy colour code.
        assertTrue(converted.indexOf('\u00A7') == 0 && converted.charAt(1) == 'c',
                "kills line must open with the legacy red (§c) resolved from &#FC0000: " + converted);
        int iconAt = converted.indexOf('\u00D7');
        assertEquals(2, iconAt, "the icon must sit right after the opening colour code, "
                + "exactly where the removed sword glyph sat");

        // Reproduce ScoreboardManager.applyLineSpigot's split contract: a prefix of at most
        // 16 raw characters (the icon half) and a suffix of at most 16 with re-opened codes.
        assertTrue(converted.length() > LegacyScoreboardText.MAX_TEAM_PART_LENGTH);
        String prefix = converted.substring(0, LegacyScoreboardText.MAX_TEAM_PART_LENGTH);
        String remainder = LegacyScoreboardText.codesActiveAt(converted, prefix.length())
                + converted.substring(prefix.length());
        String suffix = remainder.substring(0, Math.min(remainder.length(),
                LegacyScoreboardText.MAX_TEAM_PART_LENGTH));
        assertTrue(prefix.indexOf('\u00D7') >= 0,
                "the kill icon must live inside the prefix (it is the first visible character)");
        assertTrue(suffix.length() <= LegacyScoreboardText.MAX_TEAM_PART_LENGTH,
                "1.12.2 team suffix limit");
        assertTrue(LegacyScoreboardText.sanitize(converted,
                LegacyScoreboardText.MAX_OBJECTIVE_NAME_LENGTH).length()
                <= LegacyScoreboardText.MAX_OBJECTIVE_NAME_LENGTH,
                "1.12.2 32-character line budget must hold for the kills template");
    }

    /** The other scoreboard glyphs must stay exactly as they are (only the kills icon moves). */
    @Test
    void otherLineIconsAreUnchanged() throws Exception {
        YamlConfiguration config = loadTemplate();
        String joined = String.join("\n", config.getStringList("SCOREBOARD.LINES"));
        assertTrue(joined.indexOf('\u2605') >= 0, "shards icon U+2605 must remain");
        assertTrue(joined.indexOf('\u2620') >= 0, "deaths icon U+2620 must remain");
        assertTrue(joined.indexOf('\u231A') >= 0, "playtime icon U+231A must remain");
        assertTrue(joined.indexOf('\u231B') >= 0, "hourglass icon U+231B must remain");
        assertTrue(config.getString("SCOREBOARD.TEAM").indexOf('\u26CF') >= 0,
                "team icon U+26CF must remain");
        assertTrue(config.getString("SCOREBOARD.SHARD-BOOSTER").indexOf('\u26A1') >= 0,
                "booster icon U+26A1 must remain");
    }

    /**
     * The scoreboard pipeline stays on the legacy 1.12.2 mechanisms and Java-8 language level:
     * no NMS/Paper/Adventure imports, no records/var/modern APIs in the files that build the
     * lines, and the platform limits keep their original hard values.
     */
    @Test
    void scoreboardPipelineStaysLegacyAndJavaEight() throws Exception {
        assertEquals(32, LegacyScoreboardText.MAX_OBJECTIVE_NAME_LENGTH);
        assertEquals(16, LegacyScoreboardText.MAX_TEAM_PART_LENGTH);

        String[] sources = {
                "src/main/java/com/bx/ultimateDonutSmp/managers/ScoreboardManager.java",
                "src/main/java/com/bx/ultimateDonutSmp/utils/LegacyScoreboardText.java"
        };
        String root = System.getProperty("basedir", ".") + "/";
        for (String path : sources) {
            String source = new String(java.nio.file.Files.readAllBytes(
                    new java.io.File(root + path).toPath()), StandardCharsets.UTF_8)
                    .replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
            String name = path.substring(path.lastIndexOf('/') + 1);
            assertFalse(source.contains("import net.minecraft."), name + ": NMS must stay reflective");
            assertFalse(source.contains("import io.papermc."), name + ": no Paper-only APIs");
            assertFalse(source.contains("import net.kyori."), name + ": no Adventure-only APIs");
            assertFalse(source.contains("Clientbound"), name + ": no modern packet class names");
            assertFalse(source.contains("RenderState"), name + ": modern scoreboard/display APIs banned");
            assertFalse(source.matches("(?s).*record\\s+[A-Z].*"), name + ": no records");
            assertFalse(source.matches("(?s).*\\bvar\\s+[a-zA-Z].*"), name + ": no var");
            assertFalse(source.contains(".isBlank("), name + ": isBlank() is Java 11");
        }
    }
}
