package com.bx.ultimateDonutSmp.utils;

/**
 * Sanitises scoreboard and tab-list text for the Bukkit 1.12.2 legacy chat protocol.
 *
 * <p>1.12.2 rejects long raw strings at the API level: an objective display name may be at most
 * {@value #MAX_OBJECTIVE_NAME_LENGTH} characters, a team prefix or suffix at most
 * {@value #MAX_TEAM_PART_LENGTH} characters, and a player list name at most
 * {@value #MAX_PLAYER_LIST_NAME_LENGTH} characters. Every one of those counts the raw string,
 * colour codes included, which is why the modern per-character hex gradients used elsewhere in
 * this plugin ({@code §x§0§0§6§9§d§6} alone is 14 characters) blow through every limit and throw
 * {@code IllegalArgumentException} thousands of times per minute from the render loop.</p>
 *
 * <p>This helper converts the modern RGB/hex colour markup to the nearest legacy
 * {@code ChatColor} so the colour survives (a 1.12.2 client cannot render {@code §x} anyway), and
 * then truncates the raw string on token boundaries so a colour sequence or a surrogate pair can
 * never be cut in half. Redundant repeated colour codes are collapsed first, which usually makes
 * a gradient title such as {@code &#0069d6&lE&#0374da&lc…} fit the limit as {@code §9§lEconomySMP}
 * without losing a single visible character.</p>
 *
 * <p>The class is deliberately free of Bukkit imports so the sanitisation can be unit tested
 * without a server.</p>
 */
public final class LegacyScoreboardText {

    /** Bukkit 1.12.2 limit for {@code Objective#setDisplayName(String)}. */
    public static final int MAX_OBJECTIVE_NAME_LENGTH = 32;

    /** Bukkit 1.12.2 limit for {@code Team#setPrefix(String)} / {@code Team#setSuffix(String)}. */
    public static final int MAX_TEAM_PART_LENGTH = 16;

    /** Bukkit 1.12.2 limit for {@code Player#setPlayerListName(String)}. */
    public static final int MAX_PLAYER_LIST_NAME_LENGTH = 16;

    private static final char SECTION = (char) 0xA7;

    /** Legacy chat colours in ChatColor order; index matches the hex digit of the colour code. */
    private static final int[][] LEGACY_COLORS = {
            {0x00, 0x00, 0x00}, // 0 black
            {0x00, 0x00, 0xAA}, // 1 dark_blue
            {0x00, 0xAA, 0x00}, // 2 dark_green
            {0x00, 0xAA, 0xAA}, // 3 dark_aqua
            {0xAA, 0x00, 0x00}, // 4 dark_red
            {0xAA, 0x00, 0xAA}, // 5 dark_purple
            {0xFF, 0xAA, 0x00}, // 6 gold
            {0xAA, 0xAA, 0xAA}, // 7 gray
            {0x55, 0x55, 0x55}, // 8 dark_gray
            {0x55, 0x55, 0xFF}, // 9 blue
            {0x55, 0xFF, 0x55}, // a green
            {0x55, 0xFF, 0xFF}, // b aqua
            {0xFF, 0x55, 0x55}, // c red
            {0xFF, 0x55, 0xFF}, // d light_purple
            {0xFF, 0xFF, 0x55}, // e yellow
            {0xFF, 0xFF, 0xFF}  // f white
    };

    private static final String COLOR_CODES = "0123456789abcdefABCDEF";
    private static final String FORMAT_CODES = "klmnorKLMNOR";

    private LegacyScoreboardText() {
    }

    /**
     * Prepares a sidebar objective display name for
     * {@code Objective#setDisplayName(String)} on 1.12.2: hex colours are converted to their
     * nearest legacy colour, redundant codes are collapsed and the raw string is safely truncated
     * to at most {@value #MAX_OBJECTIVE_NAME_LENGTH} characters.
     */
    public static String sanitizeObjectiveName(String raw) {
        return sanitize(raw, MAX_OBJECTIVE_NAME_LENGTH);
    }

    /**
     * Prepares a tab-list name for {@code Player#setPlayerListName(String)} on 1.12.2, which
     * accepts at most {@value #MAX_PLAYER_LIST_NAME_LENGTH} raw characters.
     */
    public static String sanitizePlayerListName(String raw) {
        return sanitize(raw, MAX_PLAYER_LIST_NAME_LENGTH);
    }

    /** Converts, collapses and safely truncates to {@code limit} raw characters. */
    public static String sanitize(String raw, int limit) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String legacy = truncate(collapse(toLegacyColors(raw)), limit);
        return legacy;
    }

    /**
     * Converts every modern RGB/hex colour markup to the nearest legacy colour code.
     *
     * <ul>
     *   <li>{@code §x§0§0§6§9§d§6} (the form {@code ColorUtils} produces) &rarr; {@code §9};</li>
     *   <li>{@code &#RRGGBB}, {@code {#RRGGBB}}, {@code <#RRGGBB>}, {@code &x#RRGGBB} and bare
     *       {@code #RRGGBB} &rarr; {@code §c}, mirroring the hex patterns ColorUtils accepts, in
     *       case a value reaches the scoreboard without having been colourised first;</li>
     *   <li>{@code </#RRGGBB>} &rarr; {@code §r}.</li>
     * </ul>
     *
     * <p>A malformed {@code §x} sequence (not followed by six {@code §h} pairs) is dropped rather
     * than half-preserved, so the output never contains a broken colour sequence.</p>
     */
    public static String toLegacyColors(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        int length = text.length();
        while (i < length) {
            char c = text.charAt(i);

            if (c == SECTION && i + 1 < length) {
                char code = text.charAt(i + 1);
                if ((code == 'x' || code == 'X')) {
                    int[] rgb = parseSectionHex(text, i);
                    if (rgb != null) {
                        out.append(SECTION).append(nearestLegacyCode(rgb[0], rgb[1], rgb[2]));
                        i += 14;
                        continue;
                    }
                    // Malformed §x sequence: drop the marker itself so no broken pair survives.
                    i += 2;
                    continue;
                }
                out.append(c).append(code);
                i += 2;
                continue;
            }

            if (c == '&') {
                // &x#RRGGBB before &#RRGGBB because the latter is a prefix of the former.
                if (startsWith(text, i, "&x#") && isHexAt(text, i + 3, 6)) {
                    out.append(SECTION).append(nearestLegacyCode(text, i + 3));
                    i += 10;
                    continue;
                }
                if (startsWith(text, i, "&#") && isHexAt(text, i + 2, 6)) {
                    out.append(SECTION).append(nearestLegacyCode(text, i + 2));
                    i += 8;
                    continue;
                }
                if (i + 1 < length) {
                    char ampersandCode = Character.toLowerCase(text.charAt(i + 1));
                    if (COLOR_CODES.indexOf(ampersandCode) >= 0 || FORMAT_CODES.indexOf(ampersandCode) >= 0) {
                        out.append(SECTION).append(text.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                }
            }

            if (c == '{' && i + 8 < length && text.charAt(i + 1) == '#'
                    && text.charAt(i + 8) == '}' && isHexAt(text, i + 2, 6)) {
                out.append(SECTION).append(nearestLegacyCode(text, i + 2));
                i += 9;
                continue;
            }

            if (c == '<') {
                boolean closing = i + 1 < length && text.charAt(i + 1) == '/';
                int hashIndex = closing ? i + 2 : i + 1;
                int hexStart = hashIndex + 1;
                int tokenLength = closing ? 10 : 9;
                if (hashIndex < length && text.charAt(hashIndex) == '#'
                        && i + tokenLength <= length
                        && text.charAt(i + tokenLength - 1) == '>'
                        && isHexAt(text, hexStart, 6)) {
                    if (closing) {
                        out.append(SECTION).append('r');
                    } else {
                        out.append(SECTION).append(nearestLegacyCode(text, hexStart));
                    }
                    i += tokenLength;
                    continue;
                }
            }

            if (c == '#' && isHexAt(text, i + 1, 6)) {
                out.append(SECTION).append(nearestLegacyCode(text, i + 1));
                i += 7;
                continue;
            }

            out.append(c);
            i++;
        }
        return out.toString();
    }

    /**
     * Rebuilds a legacy string with the minimum colour codes that render identically. A colour
     * code resets formatting in the legacy chat protocol, so codes are always emitted colour
     * first, formats after; a state that has not changed emits nothing. A gradient title whose
     * hex colours all map to the same legacy colour therefore collapses to a single code pair,
     * while an explicit re-statement of the colour (which clears formatting) is preserved.
     */
    public static String collapse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        Character desiredColor = null;
        StringBuilder desiredFormats = new StringBuilder(5);
        boolean sawColorSinceFlush = false;
        Character emittedColor = null;
        StringBuilder emittedFormats = new StringBuilder(5);

        int i = 0;
        int length = text.length();
        while (i < length) {
            char c = text.charAt(i);
            if (c == SECTION && i + 1 < length) {
                char rawCode = text.charAt(i + 1);
                char code = Character.toLowerCase(rawCode);
                if (COLOR_CODES.indexOf(code) >= 0) {
                    desiredColor = Character.valueOf(code);
                    desiredFormats.setLength(0);
                    sawColorSinceFlush = true;
                    i += 2;
                    continue;
                }
                if (FORMAT_CODES.indexOf(code) >= 0) {
                    if (code == 'r') {
                        desiredColor = null;
                        desiredFormats.setLength(0);
                        sawColorSinceFlush = true;
                    } else if (desiredFormats.indexOf(String.valueOf(code)) < 0) {
                        desiredFormats.append(code);
                    }
                    i += 2;
                    continue;
                }
                // Unknown code: keep it verbatim (rare, e.g. §z from odd configs).
                out.append(c).append(rawCode);
                i += 2;
                continue;
            }
            // Visible character: flush the minimal codes that reach the desired state.
            boolean needsReemit = sawColorSinceFlush
                    && (desiredColor == null ? emittedColor != null : !desiredColor.equals(emittedColor)
                    || hasFormatOutside(emittedFormats, desiredFormats));
            if (needsReemit) {
                out.append(SECTION).append(desiredColor != null ? desiredColor.charValue() : 'r');
                emittedColor = desiredColor;
                emittedFormats.setLength(0);
                for (int f = 0; f < desiredFormats.length(); f++) {
                    out.append(SECTION).append(desiredFormats.charAt(f));
                    emittedFormats.append(desiredFormats.charAt(f));
                }
            } else {
                for (int f = 0; f < desiredFormats.length(); f++) {
                    char format = desiredFormats.charAt(f);
                    if (emittedFormats.indexOf(String.valueOf(format)) < 0) {
                        out.append(SECTION).append(format);
                        emittedFormats.append(format);
                    }
                }
            }
            sawColorSinceFlush = false;
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** {@code true} when {@code formats} contains a code that is not in {@code subset}. */
    private static boolean hasFormatOutside(StringBuilder formats, CharSequence subset) {
        for (int i = 0; i < formats.length(); i++) {
            if (subset.length() == 0 || subset.toString().indexOf(formats.charAt(i)) < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Truncates the raw string to at most {@code limit} characters without ever cutting through
     * a {@code §}-code or a surrogate pair. Trailing codes that colour no visible text are
     * dropped so the budget goes to visible characters.
     */
    public static String truncate(String text, int limit) {
        if (text == null || text.isEmpty() || limit <= 0) {
            return "";
        }
        if (text.length() <= limit) {
            return stripTrailingCodes(text);
        }
        StringBuilder out = new StringBuilder(limit + 2);
        int i = 0;
        int length = text.length();
        boolean lastWasCode = false;
        while (i < length) {
            char c = text.charAt(i);
            if (c == SECTION) {
                if (i + 1 >= length) {
                    break; // dangling section sign: never copy it
                }
                if (out.length() + 2 > limit) {
                    break;
                }
                out.append(c).append(text.charAt(i + 1));
                lastWasCode = true;
                i += 2;
                continue;
            }
            if (Character.isHighSurrogate(c) && i + 1 < length && Character.isLowSurrogate(text.charAt(i + 1))) {
                if (out.length() + 2 > limit) {
                    break;
                }
                out.append(c).append(text.charAt(i + 1));
                lastWasCode = false;
                i += 2;
                continue;
            }
            if (out.length() + 1 > limit) {
                break;
            }
            out.append(c);
            lastWasCode = false;
            i++;
        }
        String result = out.toString();
        if (lastWasCode) {
            result = stripTrailingCodes(result);
        }
        return result;
    }

    /**
     * The minimal codes that re-open the colour/format state active at {@code splitIndex} — used
     * to colour the second half of a team line split across prefix and suffix, because the
     * invisible entry between them resets formatting.
     */
    public static String codesActiveAt(String text, int splitIndex) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int limit = Math.min(splitIndex, text.length());
        Character color = null;
        StringBuilder formats = new StringBuilder(5);
        int i = 0;
        while (i < limit) {
            char c = text.charAt(i);
            if (c == SECTION && i + 1 < limit) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (COLOR_CODES.indexOf(code) >= 0) {
                    color = Character.valueOf(code);
                    formats.setLength(0);
                } else if (code == 'r') {
                    color = null;
                    formats.setLength(0);
                } else if (FORMAT_CODES.indexOf(code) >= 0 && formats.indexOf(String.valueOf(code)) < 0) {
                    formats.append(code);
                }
                i += 2;
                continue;
            }
            i++;
        }
        StringBuilder reopen = new StringBuilder(6);
        if (color != null) {
            reopen.append(SECTION).append(color.charValue());
        }
        for (int f = 0; f < formats.length(); f++) {
            reopen.append(SECTION).append(formats.charAt(f));
        }
        return reopen.toString();
    }

    // ── internals ──────────────────────────────────────────────────────────────

    /** Parses {@code §x§R§R§G§G§B§B} starting at {@code start}; {@code null} when malformed. */
    private static int[] parseSectionHex(String text, int start) {
        if (start + 14 > text.length()) {
            return null;
        }
        int[] rgb = new int[3];
        int value = 0;
        for (int pair = 0; pair < 6; pair++) {
            int offset = start + 2 + pair * 2;
            if (text.charAt(offset) != SECTION) {
                return null;
            }
            char digit = text.charAt(offset + 1);
            int nibble = Character.digit(digit, 16);
            if (nibble < 0) {
                return null;
            }
            value = (value << 4) | nibble;
        }
        rgb[0] = (value >> 16) & 0xFF;
        rgb[1] = (value >> 8) & 0xFF;
        rgb[2] = value & 0xFF;
        return rgb;
    }

    private static boolean isHexAt(String text, int start, int count) {
        if (start < 0 || start + count > text.length()) {
            return false;
        }
        for (int i = start; i < start + count; i++) {
            if (Character.digit(text.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWith(String text, int offset, String prefix) {
        return text.regionMatches(offset, prefix, 0, prefix.length());
    }

    private static char nearestLegacyCode(String hex, int start) {
        int r = Character.digit(hex.charAt(start), 16) * 16 + Character.digit(hex.charAt(start + 1), 16);
        int g = Character.digit(hex.charAt(start + 2), 16) * 16 + Character.digit(hex.charAt(start + 3), 16);
        int b = Character.digit(hex.charAt(start + 4), 16) * 16 + Character.digit(hex.charAt(start + 5), 16);
        return nearestLegacyCode(r, g, b);
    }

    /**
     * Nearest legacy colour by the perceptual "redmean" weighted RGB distance, which keeps blue
     * gradients blue instead of snapping them to teal the way plain Euclidean distance does.
     */
    static char nearestLegacyCode(int red, int green, int blue) {
        int bestIndex = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int index = 0; index < LEGACY_COLORS.length; index++) {
            int[] color = LEGACY_COLORS[index];
            int meanRed = (red + color[0]) / 2;
            double weightRed = 2.0 + meanRed / 256.0;
            double weightBlue = 2.0 + (255 - meanRed) / 256.0;
            long dr = red - color[0];
            long dg = green - color[1];
            long db = blue - color[2];
            double distance = weightRed * dr * dr + 4.0 * dg * dg + weightBlue * db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return COLOR_CODES.charAt(bestIndex);
    }

    private static String stripTrailingCodes(String text) {
        int end = text.length();
        while (end >= 2 && text.charAt(end - 2) == SECTION
                && (COLOR_CODES.indexOf(text.charAt(end - 1)) >= 0
                || FORMAT_CODES.indexOf(text.charAt(end - 1)) >= 0)) {
            end -= 2;
        }
        return end == text.length() ? text : text.substring(0, end);
    }
}
