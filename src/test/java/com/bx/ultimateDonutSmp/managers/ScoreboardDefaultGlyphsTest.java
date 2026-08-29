package com.bx.ultimateDonutSmp.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled scoreboard template must not use characters outside the basic multilingual plane.
 * 1.12.2 clients (and EaglerCraft) read glyphs from the server's unicode font pages, which only
 * cover the BMP: an astral character such as U+1F5E1 arrives as a surrogate pair the legacy
 * scoreboard cannot split correctly and shows up as a blank box or garbled text. Everything the
 * default lines use has to sit in a range the legacy font pages actually provide.
 */
class ScoreboardDefaultGlyphsTest {

    @Test
    void bundledTemplateOnlyUsesBasicPlaneGlyphs() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(
                ScoreboardDefaultGlyphsTest.class.getClassLoader()
                        .getResourceAsStream("scoreboard.yml"),
                StandardCharsets.UTF_8)) {
            config.load(reader);
        }

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
}
