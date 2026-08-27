package com.bx.ultimateDonutSmp.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Central Spigot 1.12.2 material-name compatibility support.
 *
 * <p>Snapshot 1.13 flattened the "one Material plus a durability value" families into one enum
 * constant per colour, so {@code Material.STAINED_GLASS_PANE} + data became
 * {@code STAINED_GLASS_PANE} and friends. 1.12.2 only knows the shared Material plus the
 * legacy data value, which means the modern names cannot be resolved by {@code Material.valueOf}
 * or {@code Material.matchMaterial} and silently degrade: {@link ItemUtils#parseMaterial(String)}
 * answers {@code Material.STONE}, while the config-driven menu helpers answer their own fallback.
 * Both outcomes are wrong for a configuration file written against the modern build.</p>
 *
 * <p>This class is deliberately additive and deliberately narrow:</p>
 * <ul>
 *   <li>it never decides what an unresolvable value means. Anything it does not recognise is
 *       reported as "not handled here" ({@code null}) so every call site keeps its existing
 *       fallback semantics;</li>
 *   <li>it does not create items. {@link ItemUtils#createItem(Material, short, String,
 *       java.util.List)} and {@link ItemUtils#createPlaceholder(Material, short)} stay the only
 *       ItemStack factories;</li>
 *   <li>only the stained glass pane family is implemented today. Dyes ({@code INK_SACK}),
 *       terracotta ({@code STAINED_CLAY}), heads and every other 1.13+ item stay in their own
 *       batches until their data mapping is validated the same way;</li>
 *   <li>legacy 1.12.2 names ({@code WATCH}, {@code MOB_SPAWNER}, {@code BOOK_AND_QUILL},
 *       {@code EYE_OF_ENDER}, {@code EXP_BOTTLE}, {@code GRASS}, {@code HOPPER}, {@code INK_SACK},
 *       &hellip;) are passed through untouched with data {@code 0};</li>
 *   <li>no new configuration syntax is introduced. Values stay single Material names, exactly as
 *       the shipped {@code *.yml} files write them today.</li>
 * </ul>
 *
 * <p>Data values are the 1.12.2 durability values of the flattened family (wool order): white 0,
 * orange 1, magenta 2, light blue 3, yellow 4, lime 5, pink 6, gray 7, light gray 8 (1.12.2 calls
 * that colour {@code SILVER}), cyan 9, purple 10, blue 11, brown 12, green 13, red 14, black 15.
 * These are the values the already migrated call sites use: gray 7, black 15, red 14, lime 5,
 * light gray 8, plus {@code STAINED_CLAY} 5 and 14.</p>
 */
public final class LegacyMaterialSupport {

    /** The Material every stained glass pane colour shares on 1.12.2. */
    private static final Material PANE_MATERIAL = Material.STAINED_GLASS_PANE;

    /** Suffix that 1.13+ prepends a colour name to, e.g. {@code STAINED_GLASS_PANE}. */
    private static final String PANE_SUFFIX = "_STAINED_GLASS_PANE";

    /** 1.13+ colour name -&gt; 1.12.2 stained glass pane data value. */
    private static final Map<String, Short> PANE_DATA;

    /** Inverse of {@link #PANE_DATA}, used to write a pane back into a config file. */
    private static final Map<Short, String> PANE_COLOR_BY_DATA;

    static {
        Map<String, Short> data = new HashMap<String, Short>();
        data.put("WHITE", Short.valueOf((short) 0));
        data.put("ORANGE", Short.valueOf((short) 1));
        data.put("MAGENTA", Short.valueOf((short) 2));
        data.put("LIGHT_BLUE", Short.valueOf((short) 3));
        data.put("YELLOW", Short.valueOf((short) 4));
        data.put("LIME", Short.valueOf((short) 5));
        data.put("PINK", Short.valueOf((short) 6));
        data.put("GRAY", Short.valueOf((short) 7));
        data.put("LIGHT_GRAY", Short.valueOf((short) 8));
        data.put("CYAN", Short.valueOf((short) 9));
        data.put("PURPLE", Short.valueOf((short) 10));
        data.put("BLUE", Short.valueOf((short) 11));
        data.put("BROWN", Short.valueOf((short) 12));
        data.put("GREEN", Short.valueOf((short) 13));
        data.put("RED", Short.valueOf((short) 14));
        data.put("BLACK", Short.valueOf((short) 15));
        PANE_DATA = Collections.unmodifiableMap(data);

        Map<Short, String> colors = new HashMap<Short, String>();
        for (Map.Entry<String, Short> entry : PANE_DATA.entrySet()) {
            colors.put(entry.getValue(), entry.getKey());
        }
        PANE_COLOR_BY_DATA = Collections.unmodifiableMap(colors);
    }

    private LegacyMaterialSupport() {}

    /**
     * A resolved 1.12.2 item identity: the Material to build with, the legacy data value to give
     * it, and the configuration name that represents it.
     *
     * <p>Equality and hashing use the Material and the data value only. The configured name is the
     * serialization label used when a value has to be written back to a config file, so two icons
     * that render identically stay equal even when they were spelled differently.</p>
     */
    public static final class Icon {

        private final Material material;
        private final short data;
        private final String configuredName;

        private Icon(Material material, short data, String configuredName) {
            this.material = material;
            this.data = data;
            this.configuredName = configuredName;
        }

        /** The Material to build the item with. Never {@code null}. */
        public Material material() {
            return material;
        }

        /** The legacy durability value to build the item with. {@code 0} for every non-pane. */
        public short data() {
            return data;
        }

        /**
         * The name this icon is represented by in a configuration file. For a pane that is the
         * flattened 1.13+ colour name (for example {@code STAINED_GLASS_PANE}), which keeps
         * generated defaults and stored override strings byte-identical to the modern build.
         */
        public String configuredName() {
            return configuredName;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            Icon that = (Icon) other;
            return data == that.data && material == that.material;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(material, Short.valueOf(data));
        }

        @Override
        public String toString() {
            return data == 0 ? material.name() : material.name() + " with data " + data;
        }
    }

    /**
     * Builds the icon for a pane colour, accepting either the bare 1.13+ colour name
     * ({@code BLACK}) or the full material name ({@code STAINED_GLASS_PANE}).
     *
     * @return the icon, or {@code null} when the name is not a known pane colour
     */
    public static Icon pane(String colorOrPaneName) {
        return resolvePane(colorOrPaneName);
    }

    /**
     * Icon for a plain 1.12.2 material with no legacy data value. Coloured panes normally use
     * {@link #pane(String)} instead, which also carries the flattened configuration name.
     */
    public static Icon of(Material material) {
        if (material == null) {
            return null;
        }
        return new Icon(material, (short) 0, material.name());
    }

    /** {@code true} when the value names a 1.13+ stained glass pane colour. */
    public static boolean isPaneName(String raw) {
        return resolvePane(raw) != null;
    }

    /**
     * Resolves a 1.13+ flattened pane name to the 1.12.2 Material plus its legacy data value.
     * Anything that is not a pane colour returns {@code null} so the caller can continue with its
     * own parsing and fallback rules.
     *
     * <p>Surrounding whitespace and letter case are ignored, matching the normalisation every
     * existing config boundary in this project already performs.</p>
     */
    public static Icon resolvePane(String raw) {
        String name = normalize(raw);
        if (name.isEmpty()) {
            return null;
        }
        String color = name.endsWith(PANE_SUFFIX)
                ? name.substring(0, name.length() - PANE_SUFFIX.length())
                : name;
        Short data = PANE_DATA.get(color);
        if (data == null) {
            return null;
        }
        return new Icon(PANE_MATERIAL, data.shortValue(), color + PANE_SUFFIX);
    }

    /**
     * Pane-aware resolution for configuration and database values.
     *
     * <ul>
     *   <li>a 1.13+ pane name -&gt; {@code STAINED_GLASS_PANE} plus the matching data value;</li>
     *   <li>{@code STAINED_GLASS_PANE} -&gt; {@code STAINED_GLASS_PANE} with data {@code 0};</li>
     *   <li>any other valid 1.12.2 Material name ({@code HOPPER}, {@code WATCH},
     *       {@code INK_SACK}, &hellip;) -&gt; that Material with data {@code 0};</li>
     *   <li>{@code null}, blank or unknown names -&gt; {@code null}, meaning "keep the behaviour
     *       you already have": the caller's fallback Material, or
     *       {@link ItemUtils#parseMaterial(String)} returning {@code Material.STONE}.</li>
     * </ul>
     */
    public static Icon resolve(String raw) {
        Icon pane = resolvePane(raw);
        if (pane != null) {
            return pane;
        }
        String name = normalize(raw);
        if (name.isEmpty()) {
            return null;
        }
        Material material = Material.matchMaterial(name);
        return material == null ? null : of(material);
    }

    /**
     * Resolves a configured value and keeps {@code fallback} whenever the value is missing or
     * unresolvable. This is the seam the config-driven menu helpers need: it centralises the
     * "otherwise use the fallback" step without changing what an invalid value means.
     */
    public static Icon resolve(String raw, Icon fallback) {
        Icon resolved = resolve(raw);
        return resolved == null ? fallback : resolved;
    }

    /**
     * The configuration name for an item. A pane whose colour lives in the legacy data value is
     * written back as its flattened 1.13+ colour name so {@link #resolve(String)} restores the same
     * Material and data later; every other item keeps using the Material name exactly as
     * {@code getType().name()} does today.
     *
     * <p>A pane with data {@code 0} deliberately keeps the bare {@code STAINED_GLASS_PANE} name:
     * {@code 0} is the shared default of the whole family, both spellings resolve to the identical
     * {@code STAINED_GLASS_PANE} + 0 icon (see {@link Icon#equals(Object)}), and values written by
     * {@code getType().name()} stay byte-identical. Only a colour that would actually be lost is
     * upgraded to the flattened alias, so non-pane items are never rewritten.</p>
     */
    public static String configName(ItemStack item) {
        if (item == null || item.getType() == null) {
            return null;
        }
        Material type = item.getType();
        if (type == PANE_MATERIAL && item.getDurability() != 0) {
            String color = PANE_COLOR_BY_DATA.get(Short.valueOf(item.getDurability()));
            if (color != null) {
                return color + PANE_SUFFIX;
            }
        }
        return type.name();
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
