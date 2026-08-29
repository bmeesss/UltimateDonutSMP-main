package com.bx.ultimateDonutSmp.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Worth lore line formatting. The {price} placeholder carries the bare compact number from
 * CurrencyManager#formatCompactAmount (no currency symbol of its own); the $ must therefore be
 * written exactly once in the configured format. The shipped defaults used "$${price}", and
 * because the placeholder pipeline is a plain string replace, every tooltip rendered a doubled
 * dollar sign ("Worth: $$1.2K"). WorthManager#getWorthLoreFormat is the single source of truth,
 * so the bundled worth.yml seed and the Java fallback default must stay byte-identical, and no
 * second format section may reappear.
 */
class WorthLoreFormatTest {

    private static final Pattern JAVA_DEFAULT =
            Pattern.compile("getString\\(\"DISPLAY\\.FORMAT\", \"([^\"]*)\"\\)");

    private static final String EXPECTED_FORMAT = "&7Worth: &a${price}";

    @Test
    void bundledFormatHasASingleCurrencySignBeforeThePlaceholder() throws Exception {
        String format = bundledFormat();
        assertEquals(EXPECTED_FORMAT, format);
        assertFalse(format.contains("$$"), "the pipeline never unescapes $$, so it prints twice");
        assertTrue(format.contains("{price}"), "the bare number placeholder must be substituted");
    }

    @Test
    void javaFallbackDefaultMatchesTheBundledSeed() throws Exception {
        String source = new String(Files.readAllBytes(
                new File("src/main/java/com/bx/ultimateDonutSmp/managers/WorthManager.java").toPath()),
                StandardCharsets.UTF_8);
        Matcher matcher = JAVA_DEFAULT.matcher(source);
        assertTrue(matcher.find(), "getWorthLoreFormat must read DISPLAY.FORMAT with a literal default");
        assertEquals(matcher.group(1), bundledFormat(),
                "the Java default and the shipped worth.yml seed must not drift apart");
    }

    @Test
    void placeholderReplacementRendersExactlyOneDollarSign() throws Exception {
        WorthManager manager = allocateWithoutConstructor();
        Method replace = WorthManager.class.getDeclaredMethod(
                "replaceWorthPlaceholders",
                String.class, String.class, String.class, String.class,
                String.class, String.class, String.class);
        replace.setAccessible(true);

        String line = (String) replace.invoke(
                manager, EXPECTED_FORMAT, "1.2K", "20", "64", "Stone", "$1,250.00", "20.00");
        assertEquals("&7Worth: &a$1.2K", line);

        String raw = (String) replace.invoke(
                manager, "&7Worth: &a{price_raw}", "1.2K", "20", "64", "Stone", "$1,250.00", "20.00");
        assertEquals("&7Worth: &$1,250.00", raw,
                "{price_raw} already embeds the symbol via formatMoney; a lone $ in the format "
                        + "would double it for admins copying that style, so the placeholder "
                        + "alone must carry the currency");
    }

    @Test
    void noDeadSecondFormatSectionSurvivesInConfig() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new File("src/main/resources/config.yml"));
        assertFalse(config.contains("WORTH-LORE"),
                "WORTH-LORE in config.yml had zero consumers; the format lives in worth.yml only");
    }

    @Test
    void everyLanguageMirrorStaysInSync() throws Exception {
        File[] locales = new File("src/main/resources/languages").listFiles();
        assertTrue(locales != null && locales.length > 0, "bundled language files exist");
        java.util.Arrays.sort(locales);
        for (File locale : locales) {
            if (!locale.getName().endsWith(".yml")) {
                continue;
            }
            YamlConfiguration language = new YamlConfiguration();
            language.load(locale);
            String mirror = language.getString("CONFIG.WORTH.DISPLAY.FORMAT");
            if (mirror != null) {
                assertEquals(bundledFormat(), mirror, locale.getName()
                        + " mirrors the live DISPLAY.FORMAT; a drifted copy would resurrect the"
                        + " doubled sign the moment localization starts consuming the mirror");
            }
        }
    }

    @Test
    void noBundledResourceKeepsTheDoubledSign() throws Exception {
        try (java.util.stream.Stream<java.nio.file.Path> paths =
                     Files.walk(java.nio.file.Paths.get("src/main/resources"))) {
            for (java.nio.file.Path path : (Iterable<java.nio.file.Path>) paths
                    .filter(p -> p.toString().endsWith(".yml"))::iterator) {
                String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                assertFalse(content.contains("$${price}"),
                        path + " still ships the doubled currency sign");
            }
        }
    }

    private static String bundledFormat() throws Exception {
        YamlConfiguration worth = new YamlConfiguration();
        worth.load(new File("src/main/resources/worth.yml"));
        String format = worth.getString("DISPLAY.FORMAT");
        assertTrue(format != null && !format.isEmpty(), "worth.yml must ship DISPLAY.FORMAT");
        return format;
    }

    private static WorthManager allocateWithoutConstructor() throws Exception {
        Field theUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafeField.get(null);
        return (WorthManager) unsafe.allocateInstance(WorthManager.class);
    }
}
