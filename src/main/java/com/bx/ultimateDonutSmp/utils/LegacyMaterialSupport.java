package com.bx.ultimateDonutSmp.utils;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
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
 *   <li>besides the stained glass pane family it now also resolves the flattened colour
 *       families ({@code *_WOOL}, {@code *_TERRACOTTA}, {@code *_CONCRETE}, {@code *_CARPET},
 *       {@code *_GLASS}, {@code *_BED}, {@code *_BANNER}, {@code *_DYE}, {@code *_SHULKER_BOX}),
 *       the wood families ({@code *_LOG}, {@code *_PLANKS}, {@code *_SAPLING}, {@code *_BOAT},
 *       {@code *_SLAB}, {@code *_DOOR}), the stone variants ({@code ANDESITE},
 *       {@code POLISHED_GRANITE}, {@code CHISELED_SANDSTONE}, &hellip;), the wood tool renames
 *       ({@code WOODEN_}/{@code GOLDEN_}/{@code *_SHOVEL}) and the straight 1.13 renames
 *       ({@code GRASS_BLOCK}, {@code PISTON}, {@code LEAD}, {@code GUNPOWDER}, &hellip;) that
 *       the shipped filter/orders configurations use;</li>
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

    /** 1.13+ flat name &rarr; the 1.12.2 enum name of the same item. */
    private static final Map<String, String> RENAMES;

    /** 1.13+ wood type &rarr; 1.12.2 data value for the LOG/LOG_2, WOOD, SAPLING and BOAT families. */
    private static final Map<String, Integer> WOOD_DATA;

    /** Colour names ordered longest-first so {@code LIGHT_GRAY} is never read as {@code GRAY}. */
    private static final String[] COLOR_ORDER = {
            "LIGHT_BLUE", "LIGHT_GRAY", "MAGENTA", "ORANGE", "PURPLE", "YELLOW", "BROWN",
            "GREEN", "WHITE", "BLACK", "BLUE", "GRAY", "LIME", "PINK", "CYAN", "RED"
    };

    /** Wood prefixes ordered so {@code DARK_OAK} is never read as {@code OAK}. */
    private static final String[] WOOD_ORDER = {"DARK_OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "OAK"};

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

        Map<String, String> renames = new HashMap<String, String>();
        // 1.13+ renames of items/blocks that exist unchanged (bar the name) on 1.12.2.
        renames.put("GRASS_BLOCK", "GRASS");
        renames.put("DIRT_PATH", "GRASS_PATH");
        renames.put("IRON_BARS", "IRON_FENCE");
        renames.put("PISTON", "PISTON_BASE");
        renames.put("STICKY_PISTON", "PISTON_STICKY_BASE");
        renames.put("COBBLESTONE_WALL", "COBBLE_WALL");
        renames.put("STONE_BRICKS", "SMOOTH_BRICK");
        renames.put("STONE_BRICK_STAIRS", "SMOOTH_STAIRS");
        renames.put("NETHER_BRICKS", "NETHER_BRICK");
        renames.put("RED_NETHER_BRICKS", "RED_NETHER_BRICK");
        renames.put("END_STONE_BRICKS", "END_BRICKS");
        renames.put("BRICKS", "BRICK");
        renames.put("CHISELED_STONE_BRICKS", "SMOOTH_BRICK");
        renames.put("LEAD", "LEASH");
        renames.put("CLOCK", "WATCH");
        renames.put("PORKCHOP", "PORK");
        renames.put("COOKED_PORKCHOP", "GRILLED_PORK");
        renames.put("BEEF", "RAW_BEEF");
        renames.put("CHICKEN", "RAW_CHICKEN");
        renames.put("MUSHROOM_STEW", "MUSHROOM_SOUP");
        renames.put("MELON_SLICE", "MELON");
        renames.put("WRITABLE_BOOK", "BOOK_AND_QUILL");
        renames.put("FILLED_MAP", "MAP");
        renames.put("ENDER_EYE", "EYE_OF_ENDER");
        renames.put("EXPERIENCE_BOTTLE", "EXP_BOTTLE");
        renames.put("GUNPOWDER", "SULPHUR");
        renames.put("ENCHANTING_TABLE", "ENCHANTMENT_TABLE");
        renames.put("TOTEM_OF_UNDYING", "TOTEM");
        renames.put("DRAGON_BREATH", "DRAGONS_BREATH");
        renames.put("COBWEB", "WEB");
        renames.put("LILY_PAD", "WATER_LILY");
        renames.put("WHEAT_SEEDS", "SEEDS");
        renames.put("NETHER_WART", "NETHER_WARTS");
        renames.put("CARROTS", "CARROT_ITEM");
        renames.put("POTATOES", "POTATO_ITEM");
        renames.put("REPEATER", "DIODE");
        renames.put("COMPARATOR", "REDSTONE_COMPARATOR");
        renames.put("STONE_PRESSURE_PLATE", "STONE_PLATE");
        renames.put("LIGHT_WEIGHTED_PRESSURE_PLATE", "GOLD_PLATE");
        renames.put("HEAVY_WEIGHTED_PRESSURE_PLATE", "IRON_PLATE");
        renames.put("OAK_FENCE", "FENCE");
        renames.put("OAK_FENCE_GATE", "FENCE_GATE");
        renames.put("OAK_TRAPDOOR", "TRAP_DOOR");
        renames.put("OAK_BUTTON", "WOOD_BUTTON");
        renames.put("OAK_PRESSURE_PLATE", "WOOD_PLATE");
        renames.put("OAK_DOOR", "WOOD_DOOR");
        renames.put("OAK_STAIRS", "WOOD_STAIRS");
        renames.put("SPRUCE_STAIRS", "SPRUCE_WOOD_STAIRS");
        renames.put("BIRCH_STAIRS", "BIRCH_WOOD_STAIRS");
        renames.put("JUNGLE_STAIRS", "JUNGLE_WOOD_STAIRS");
        renames.put("CHEST_MINECART", "STORAGE_MINECART");
        renames.put("FURNACE_MINECART", "POWERED_MINECART");
        renames.put("TNT_MINECART", "EXPLOSIVE_MINECART");
        renames.put("COMMAND_BLOCK_MINECART", "COMMAND_MINECART");
        renames.put("CARROT_ON_A_STICK", "CARROT_STICK");
        renames.put("OAK_PLANKS", "WOOD");
        renames.put("WOODEN_PICKAXE", "WOOD_PICKAXE");
        renames.put("WOODEN_AXE", "WOOD_AXE");
        renames.put("WOODEN_SHOVEL", "WOOD_SPADE");
        renames.put("WOODEN_HOE", "WOOD_HOE");
        renames.put("WOODEN_SWORD", "WOOD_SWORD");
        renames.put("STONE_SHOVEL", "STONE_SPADE");
        renames.put("IRON_SHOVEL", "IRON_SPADE");
        renames.put("GOLDEN_SHOVEL", "GOLD_SPADE");
        renames.put("GOLDEN_PICKAXE", "GOLD_PICKAXE");
        renames.put("GOLDEN_AXE", "GOLD_AXE");
        renames.put("GOLDEN_HOE", "GOLD_HOE");
        renames.put("GOLDEN_SWORD", "GOLD_SWORD");
        renames.put("GOLDEN_HELMET", "GOLD_HELMET");
        renames.put("GOLDEN_CHESTPLATE", "GOLD_CHESTPLATE");
        renames.put("GOLDEN_LEGGINGS", "GOLD_LEGGINGS");
        renames.put("GOLDEN_BOOTS", "GOLD_BOOTS");
        renames.put("END_STONE", "ENDER_STONE");
        renames.put("CRAFTING_TABLE", "WORKBENCH");
        renames.put("NETHER_QUARTZ_ORE", "QUARTZ_ORE");
        renames.put("BREWING_STAND", "BREWING_STAND_ITEM");
        renames.put("CAULDRON", "CAULDRON_ITEM");
        renames.put("REDSTONE_TORCH", "REDSTONE_TORCH_ON");
        renames.put("FIRE_CHARGE", "FIREBALL");
        renames.put("STONE_STAIRS", "SMOOTH_STAIRS");
        renames.put("MUSIC_DISC_13", "GOLD_RECORD");
        renames.put("MUSIC_DISC_CAT", "GREEN_RECORD");
        renames.put("MUSIC_DISC_BLOCKS", "RECORD_3");
        renames.put("MUSIC_DISC_CHIRP", "RECORD_4");
        renames.put("MUSIC_DISC_FAR", "RECORD_5");
        renames.put("MUSIC_DISC_MALL", "RECORD_6");
        renames.put("MUSIC_DISC_MELLOHI", "RECORD_7");
        renames.put("MUSIC_DISC_STAL", "RECORD_8");
        renames.put("MUSIC_DISC_STRAD", "RECORD_9");
        renames.put("MUSIC_DISC_WARD", "RECORD_10");
        renames.put("MUSIC_DISC_11", "RECORD_11");
        renames.put("MUSIC_DISC_WAIT", "RECORD_12");
        // Closest-role stand-ins for post-1.12.2 items the configs still name.
        renames.put("AMETHYST_SHARD", "PRISMARINE_CRYSTALS");
        renames.put("BLUE_ICE", "PACKED_ICE");
        renames.put("CHIPPED_ANVIL", "ANVIL");
        renames.put("DAMAGED_ANVIL", "ANVIL");
        RENAMES = Collections.unmodifiableMap(renames);

        Map<String, Integer> woods = new HashMap<String, Integer>();
        woods.put("OAK", Integer.valueOf(0));
        woods.put("SPRUCE", Integer.valueOf(1));
        woods.put("BIRCH", Integer.valueOf(2));
        woods.put("JUNGLE", Integer.valueOf(3));
        woods.put("ACACIA", Integer.valueOf(4));
        woods.put("DARK_OAK", Integer.valueOf(5));
        WOOD_DATA = Collections.unmodifiableMap(woods);
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
        Short data = (Short) PANE_DATA.get(color);
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
        // Modern configs mean the door item; on 1.12.2 the wood door block names exist as
        // block-only materials, so they must map to the *_DOOR_ITEM form before the enum lookup.
        Icon doorItem = resolveWoodDoorItem(name);
        if (doorItem != null) {
            return doorItem;
        }
        // Explicit aliases must win over the raw enum lookup: BREWING_STAND and CAULDRON exist
        // as block-only 1.12.2 materials, but configs referencing them always mean the usable
        // *_ITEM form the renames already declare.
        String renamed = (String) RENAMES.get(name);
        if (renamed != null) {
            Material renamedMaterial = Material.matchMaterial(renamed);
            if (renamedMaterial != null) {
                return new Icon(renamedMaterial, (short) 0, name);
            }
        }
        Material material;
        try {
            material = Material.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            material = Material.matchMaterial(name);
        }
        if (material != null) {
            return of(material);
        }
        // Bukkit 1.12.2 uses COMMAND, COMMAND_CHAIN and COMMAND_REPEATING.
        if ("COMMAND_BLOCK".equals(name)) return of(Material.COMMAND);
        if ("CHAIN_COMMAND_BLOCK".equals(name)) return of(Material.COMMAND_CHAIN);
        if ("REPEATING_COMMAND_BLOCK".equals(name)) return of(Material.COMMAND_REPEATING);
        if ("SPAWNER".equals(name)) {
            return of(Material.MOB_SPAWNER);
        }
        if ("RAIL".equals(name)) {
            return of(Material.RAILS);
        }
        if ("OAK_SIGN".equals(name)) {
            return of(Material.SIGN_POST);
        }
        if ("DRAGON_HEAD".equals(name)) {
            return new Icon(Material.SKULL_ITEM, (short) 5, name);
        }
        if ("ENCHANTED_GOLDEN_APPLE".equals(name)) {
            return new Icon(Material.GOLDEN_APPLE, (short) 1, name);
        }
        if ("FIREWORK_ROCKET".equals(name)) {
            return of(Material.FIREWORK);
        }
        // Names introduced after 1.12.2 retain their reward/tool role using the
        // closest item that actually exists in the 1.12.2 Bukkit enum.
        if ("DIAMOND_SHOVEL".equals(name)) return of(Material.DIAMOND_SPADE);
        if (name.startsWith("NETHERITE_")) {
            if (name.endsWith("HELMET")) return of(Material.DIAMOND_HELMET);
            if (name.endsWith("CHESTPLATE")) return of(Material.DIAMOND_CHESTPLATE);
            if (name.endsWith("LEGGINGS")) return of(Material.DIAMOND_LEGGINGS);
            if (name.endsWith("BOOTS")) return of(Material.DIAMOND_BOOTS);
            if (name.endsWith("SWORD")) return of(Material.DIAMOND_SWORD);
            if (name.endsWith("AXE")) return of(Material.DIAMOND_AXE);
            if (name.endsWith("PICKAXE")) return of(Material.DIAMOND_PICKAXE);
            if (name.endsWith("SHOVEL")) return of(Material.DIAMOND_SPADE);
            if ("NETHERITE_INGOT".equals(name) || "NETHERITE_SCRAP".equals(name)) return of(Material.DIAMOND);
            if ("NETHERITE_UPGRADE_SMITHING_TEMPLATE".equals(name)) return of(Material.DIAMOND);
        }
        if ("CROSSBOW".equals(name)) return of(Material.BOW);
        if ("MACE".equals(name)) return of(Material.DIAMOND_AXE);
        // SpawnStash structures use these flattened block names; retain their closest 1.12.2 role.
        if ("DEEPSLATE".equals(name) || "BUDDING_AMETHYST".equals(name)) {
            return of(Material.STONE);
        }
        if ("DEEPSLATE_TILES".equals(name) || "DEEPSLATE_BRICKS".equals(name)) {
            return of(Material.SMOOTH_BRICK);
        }
        if ("COBBLED_DEEPSLATE".equals(name) || "POLISHED_DEEPSLATE".equals(name)
                || "CHISELED_DEEPSLATE".equals(name)) return of(Material.STONE);
        if ("POLISHED_BLACKSTONE_BRICKS".equals(name) || "REINFORCED_DEEPSLATE".equals(name)) {
            return of(Material.SMOOTH_BRICK);
        }
        if ("AMETHYST_CLUSTER".equals(name)) {
            return of(Material.QUARTZ_BLOCK);
        }
        if ("ECHO_SHARD".equals(name)) {
            return of(Material.PRISMARINE_SHARD);
        }
        if ("DEEPSLATE_REDSTONE_ORE".equals(name)) {
            return of(Material.REDSTONE_ORE);
        }
        if ("DEEPSLATE_GOLD_ORE".equals(name)) {
            return of(Material.GOLD_ORE);
        }
        if ("DEEPSLATE_IRON_ORE".equals(name)) {
            return of(Material.IRON_ORE);
        }
        if ("PURPLE_SHULKER_BOX".equals(name)) {
            return of(Material.CHEST);
        }
        if ("LANTERN".equals(name)) return of(Material.TORCH);
        if ("GOAT_HORN".equals(name) || "BREEZE_ROD".equals(name)) return of(Material.BLAZE_ROD);
        if ("AXOLOTL_BUCKET".equals(name) || "FROGSPAWN".equals(name)) return of(Material.WATER_BUCKET);
        if ("SNIFFER_EGG".equals(name) || "SNIFFER_SPAWN_EGG".equals(name)
                || "CAMEL_SPAWN_EGG".equals(name)) return of(Material.MONSTER_EGG);
        if (name.endsWith("_SPAWN_EGG")) return of(Material.MONSTER_EGG);
        if ("NETHERITE_BLOCK".equals(name) || "SHROOMLIGHT".equals(name)
                || "OCHRE_FROGLIGHT".equals(name) || "PEARLESCENT_FROGLIGHT".equals(name)
                || "VERDANT_FROGLIGHT".equals(name)) return of(Material.GLOWSTONE);
        // Families the 1.13 flattening turned into one material plus a colour/wood prefix.
        Icon family = resolveColorFamily(name);
        if (family != null) {
            return family;
        }
        family = resolveWoodFamily(name);
        if (family != null) {
            return family;
        }
        family = resolveStoneFamily(name);
        if (family != null) {
            return family;
        }
        // These blocks were introduced after 1.12.2 and have no safe equivalent.
        if ("LIGHT".equals(name) || "JIGSAW".equals(name)) return null;
        return null;
    }

    /** Maps the flattened {@code *_DOOR} item names to the 1.12.2 {@code *_DOOR_ITEM} materials. */
    private static Icon resolveWoodDoorItem(String name) {
        for (String wood : WOOD_ORDER) {
            if ((wood + "_DOOR").equals(name)) {
                String target = "OAK".equals(wood) ? "WOOD_DOOR" : wood + "_DOOR_ITEM";
                Material door = Material.matchMaterial(target);
                if (door != null) {
                    return new Icon(door, (short) 0, name);
                }
            }
        }
        return null;
    }

    /**
     * Resolves the flattened colour families ({@code WHITE_WOOL}, {@code RED_TERRACOTTA},
     * {@code LIGHT_BLUE_DYE}, {@code BLACK_BED}, …) to the shared 1.12.2 material plus the
     * dye-order data value. Dyes are the one inverted family: dye data runs opposite to the
     * other colour orders.
     *
     * @return the icon, or {@code null} when the value is not a flattened colour family member
     */
    private static Icon resolveColorFamily(String name) {
        String color = null;
        String suffix = null;
        // COLOR_ORDER is longest-first so LIGHT_BLUE is not read as BLUE.
        for (String candidate : COLOR_ORDER) {
            if (name.startsWith(candidate + "_")) {
                color = candidate;
                suffix = name.substring(candidate.length() + 1);
                break;
            }
        }
        if (color == null) {
            return null;
        }
        short data = ((Short) PANE_DATA.get(color)).shortValue();
        if ("WOOL".equals(suffix)) {
            return new Icon(Material.WOOL, data, name);
        }
        if ("CARPET".equals(suffix)) {
            return new Icon(Material.CARPET, data, name);
        }
        if ("TERRACOTTA".equals(suffix)) {
            return new Icon(Material.STAINED_CLAY, data, name);
        }
        if ("GLASS".equals(suffix)) {
            return new Icon(Material.THIN_GLASS, data, name);
        }
        if ("CONCRETE".equals(suffix)) {
            return new Icon(Material.CONCRETE, data, name);
        }
        if ("CONCRETE_POWDER".equals(suffix)) {
            return new Icon(Material.CONCRETE_POWDER, data, name);
        }
        if ("BED".equals(suffix)) {
            return new Icon(Material.BED, data, name);
        }
        if ("BANNER".equals(suffix)) {
            return new Icon(Material.BANNER, data, name);
        }
        if ("DYE".equals(suffix)) {
            // Dye data runs opposite to the wool order (ink sac 0, bone meal 15).
            return new Icon(Material.INK_SACK, (short) (15 - data), name);
        }
        if ("SHULKER_BOX".equals(suffix)) {
            // 1.12.2 spells light gray "SILVER" in the per-colour shulker box enum names.
            String enumColor = "LIGHT_GRAY".equals(color) ? "SILVER" : color;
            Material box = Material.matchMaterial(enumColor + "_SHULKER_BOX");
            return box != null ? new Icon(box, (short) 0, name) : null;
        }
        if ("GLASS_PANE".equals(suffix)) {
            return new Icon(Material.STAINED_GLASS_PANE, data, name);
        }
        return null;
    }

    /**
     * Resolves the wood families ({@code OAK_LOG}, {@code STRIPPED_BIRCH_LOG}, {@code SPRUCE_PLANKS},
     * {@code ACACIA_SAPLING}, {@code DARK_OAK_BOAT}, {@code OAK_SLAB}, …) to their 1.12.2
     * material and data value.
     */
    private static Icon resolveWoodFamily(String name) {
        String wood = null;
        String suffix = null;
        if (name.startsWith("STRIPPED_")) {
            name = name.substring("STRIPPED_".length());
        }
        // WOOD_ORDER lists DARK_OAK before OAK so the longer prefix wins.
        for (String candidate : WOOD_ORDER) {
            if (name.startsWith(candidate + "_")) {
                wood = candidate;
                suffix = name.substring(candidate.length() + 1);
                break;
            }
        }
        if (wood == null) {
            return null;
        }
        int woodData = ((Integer) WOOD_DATA.get(wood)).intValue();
        if ("LOG".equals(suffix) || "WOOD".equals(suffix)) {
            // LOG: oak/spruce/birch/jungle in LOG, acacia/dark_oak in LOG_2.
            if (woodData <= 3) {
                return new Icon(Material.LOG, (short) woodData, name);
            }
            return new Icon(Material.LOG_2, (short) (woodData - 4), name);
        }
        if ("PLANKS".equals(suffix)) {
            return new Icon(Material.WOOD, (short) woodData, name);
        }
        if ("SAPLING".equals(suffix)) {
            return new Icon(Material.SAPLING, (short) woodData, name);
        }
        if ("BOAT".equals(suffix)) {
            return new Icon(Material.BOAT, (short) woodData, name);
        }
        if ("SLAB".equals(suffix)) {
            return new Icon(Material.WOOD_STEP, (short) woodData, name);
        }
        if ("DOOR".equals(suffix)) {
            Material door = Material.matchMaterial(wood + "_DOOR_ITEM");
            return door != null ? new Icon(door, (short) 0, name) : null;
        }
        if ("FENCE".equals(suffix)) {
            // Only oak is the plain FENCE; the other woods carry their own enum names already.
            return "OAK".equals(wood) ? of(Material.FENCE) : null;
        }
        if ("FENCE_GATE".equals(suffix)) {
            return "OAK".equals(wood) ? of(Material.FENCE_GATE) : null;
        }
        if ("TRAPDOOR".equals(suffix) || "BUTTON".equals(suffix) || "PRESSURE_PLATE".equals(suffix)) {
            // Every wood species shares one wooden trapdoor/button/plate on 1.12.2 (no data
            // variants), so any species spelling maps to the shared material unambiguously.
            if ("TRAPDOOR".equals(suffix)) return of(Material.TRAP_DOOR);
            if ("BUTTON".equals(suffix)) return of(Material.WOOD_BUTTON);
            return of(Material.WOOD_PLATE);
        }
        return null;
    }

    /**
     * Resolves the stone variants that 1.13 split into their own names ({@code ANDESITE},
     * {@code POLISHED_GRANITE}, {@code CHISELED_SANDSTONE}, {@code PRISMARINE_BRICKS}, …) to
     * the shared 1.12.2 material with the matching data value.
     */
    private static Icon resolveStoneFamily(String name) {
        if ("GRANITE".equals(name)) return new Icon(Material.STONE, (short) 1, name);
        if ("POLISHED_GRANITE".equals(name)) return new Icon(Material.STONE, (short) 2, name);
        if ("DIORITE".equals(name)) return new Icon(Material.STONE, (short) 3, name);
        if ("POLISHED_DIORITE".equals(name)) return new Icon(Material.STONE, (short) 4, name);
        if ("ANDESITE".equals(name)) return new Icon(Material.STONE, (short) 5, name);
        if ("POLISHED_ANDESITE".equals(name)) return new Icon(Material.STONE, (short) 6, name);
        if ("COARSE_DIRT".equals(name)) return new Icon(Material.DIRT, (short) 1, name);
        if ("PODZOL".equals(name)) return new Icon(Material.DIRT, (short) 2, name);
        if ("RED_SAND".equals(name)) return new Icon(Material.SAND, (short) 1, name);
        if ("CHISELED_SANDSTONE".equals(name)) return new Icon(Material.SANDSTONE, (short) 1, name);
        if ("CUT_SANDSTONE".equals(name) || "SMOOTH_SANDSTONE".equals(name)) {
            return new Icon(Material.SANDSTONE, (short) 2, name);
        }
        if ("CHISELED_RED_SANDSTONE".equals(name)) return new Icon(Material.RED_SANDSTONE, (short) 1, name);
        if ("CUT_RED_SANDSTONE".equals(name) || "SMOOTH_RED_SANDSTONE".equals(name)) {
            return new Icon(Material.RED_SANDSTONE, (short) 2, name);
        }
        if ("CHISELED_QUARTZ_BLOCK".equals(name)) return new Icon(Material.QUARTZ_BLOCK, (short) 1, name);
        if ("QUARTZ_PILLAR".equals(name) || "SMOOTH_QUARTZ".equals(name)) {
            return new Icon(Material.QUARTZ_BLOCK, (short) 2, name);
        }
        if ("PRISMARINE_BRICKS".equals(name)) return new Icon(Material.PRISMARINE, (short) 1, name);
        if ("DARK_PRISMARINE".equals(name)) return new Icon(Material.PRISMARINE, (short) 2, name);
        if ("MOSSY_STONE_BRICKS".equals(name)) return new Icon(Material.SMOOTH_BRICK, (short) 1, name);
        if ("CRACKED_STONE_BRICKS".equals(name)) return new Icon(Material.SMOOTH_BRICK, (short) 2, name);
        if ("CHISELED_STONE_BRICKS".equals(name)) return new Icon(Material.SMOOTH_BRICK, (short) 3, name);
        if ("MOSSY_COBBLESTONE_WALL".equals(name)) return new Icon(Material.COBBLE_WALL, (short) 1, name);
        if ("TERRACOTTA".equals(name)) return of(Material.HARD_CLAY);
        if ("GLASS_PANE".equals(name)) return of(Material.THIN_GLASS);
        if ("SHULKER_BOX".equals(name)) return of(Material.PURPLE_SHULKER_BOX);
        if ("STONE_SLAB".equals(name) || "SMOOTH_STONE_SLAB".equals(name)) {
            return new Icon(Material.STEP, (short) 0, name);
        }
        if ("SANDSTONE_SLAB".equals(name)) return new Icon(Material.STEP, (short) 1, name);
        if ("PETRIFIED_OAK_SLAB".equals(name)) return new Icon(Material.STEP, (short) 2, name);
        if ("COBBLESTONE_SLAB".equals(name)) return new Icon(Material.STEP, (short) 3, name);
        if ("BRICK_SLAB".equals(name)) return new Icon(Material.STEP, (short) 4, name);
        if ("STONE_BRICK_SLAB".equals(name)) return new Icon(Material.STEP, (short) 5, name);
        if ("NETHER_BRICK_SLAB".equals(name)) return new Icon(Material.STEP, (short) 6, name);
        if ("QUARTZ_SLAB".equals(name)) return new Icon(Material.STEP, (short) 7, name);
        if ("RED_SANDSTONE_SLAB".equals(name)) return new Icon(Material.STONE_SLAB2, (short) 0, name);
        if ("COD".equals(name)) return new Icon(Material.RAW_FISH, (short) 0, name);
        if ("SALMON".equals(name)) return new Icon(Material.RAW_FISH, (short) 1, name);
        if ("COOKED_COD".equals(name)) return new Icon(Material.COOKED_FISH, (short) 0, name);
        if ("COOKED_SALMON".equals(name)) return new Icon(Material.COOKED_FISH, (short) 1, name);
        if ("CHARCOAL".equals(name)) return new Icon(Material.COAL, (short) 1, name);
        if ("INK_SAC".equals(name)) return new Icon(Material.INK_SACK, (short) 0, name);
        if ("BEETROOTS".equals(name)) return of(Material.BEETROOT);
        return null;
    }

    /** Returns whether a name is known to be intentionally unavailable on Bukkit 1.12.2. */
    public static boolean isUnsupportedOnLegacy(String raw) {
        String name = normalize(raw);
        return "LIGHT".equals(name) || "JIGSAW".equals(name) || "AMETHYST_BLOCK".equals(name);
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
            String color = (String) PANE_COLOR_BY_DATA.get(Short.valueOf(item.getDurability()));
            if (color != null) {
                return color + PANE_SUFFIX;
            }
        }
        return type.name();
    }

    /** Resolves renamed entity types without pretending newer mobs exist on 1.12.2. */
    public static EntityType resolveEntityType(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String name = normalize(raw).replace('-', '_');
        if ("ZOMBIFIED_PIGLIN".equals(name)) {
            return EntityType.PIG_ZOMBIE;
        }
        if ("PIGLIN".equals(name)) {
            return null;
        }
        try {
            return EntityType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        // Material names use underscores; human input ("oak door") and prose config values
        // ("Oak Door") use spaces. Folding whitespace runs into underscores — the same thing
        // modern Material.matchMaterial does — lets both spellings resolve, and 1.12.2
        // matchMaterial itself does not do this.
        StringBuilder normalized = new StringBuilder(trimmed.length());
        boolean pendingSeparator = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char character = trimmed.charAt(i);
            if (Character.isWhitespace(character)) {
                pendingSeparator = normalized.length() > 0;
                continue;
            }
            if (pendingSeparator) {
                normalized.append('_');
                pendingSeparator = false;
            }
            normalized.append(Character.toUpperCase(character));
        }
        return normalized.toString();
    }
}
