package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.models.PlayerData;
import com.bx.ultimateDonutSmp.models.SellCategory;
import com.bx.ultimateDonutSmp.models.WorthResult;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class WorthManager {

    /**
     * Detection marker prepended to the worth lore line. It must carry no visible text: the
     * line's own formatting codes are what the player reads, so the marker is reduced to pure
     * formatting. The three resets keep the run invisible even in resource packs that paint
     * unstyled text, while staying a distinctive byte sequence no ordinary config line holds.
     */
    private static final String WORTH_LORE_MARKER = "\u00A70\u00A7r\u00A7r\u00A7r";

    /** Markers older builds wrote into saved items; still stripped, never re-added. */
    private static final String[] LEGACY_WORTH_LORE_MARKERS = { "\u00A70\u00A7rWORTH:" };
    private static final Set<String> FISH_CATEGORY_OVERRIDE_NAMES = new HashSet<>(Arrays.asList(
            "COD", "COOKED_COD", "SALMON", "COOKED_SALMON", "TROPICAL_FISH", "PUFFERFISH",
            "COD_BUCKET", "SALMON_BUCKET", "TROPICAL_FISH_BUCKET", "PUFFERFISH_BUCKET", "AXOLOTL_BUCKET",
            "TADPOLE_BUCKET", "FISHING_ROD", "NAME_TAG", "NAUTILUS_SHELL", "LILY_PAD", "HEART_OF_THE_SEA",
            "RAW_FISH", "COOKED_FISH"
    ));
    private static final Set<String> POTION_CATEGORY_OVERRIDE_NAMES = new HashSet<>(Arrays.asList(
            "POTION", "SPLASH_POTION", "LINGERING_POTION", "TIPPED_ARROW", "BREWING_STAND", "BLAZE_POWDER",
            "BLAZE_ROD", "FERMENTED_SPIDER_EYE", "GLASS_BOTTLE", "GLISTERING_MELON_SLICE", "GHAST_TEAR",
            "MAGMA_CREAM", "RABBIT_FOOT", "SPIDER_EYE", "SUGAR", "GOLDEN_CARROT", "PHANTOM_MEMBRANE"
    ));

    public static final class WorthBrowserEntry {
        private final Material material;
        private final String categoryKey;
        private final double unitWorth;
        private final String sourceKey;

        public WorthBrowserEntry(Material material, String categoryKey, double unitWorth, String sourceKey) {
            this.material = material;
            this.categoryKey = categoryKey;
            this.unitWorth = unitWorth;
            this.sourceKey = sourceKey;
        }

        public Material material() { return material; }
        public String categoryKey() { return categoryKey; }
        public double unitWorth() { return unitWorth; }
        public String sourceKey() { return sourceKey; }
    }

    public static final class SellWorthEntry {
        private final Material material;
        private final int amount;
        private final double totalWorth;
        private final SellCategory category;

        public SellWorthEntry(Material material, int amount, double totalWorth, SellCategory category) {
            this.material = material;
            this.amount = amount;
            this.totalWorth = totalWorth;
            this.category = category;
        }

        public Material material() { return material; }
        public int amount() { return amount; }
        public double totalWorth() { return totalWorth; }
        public SellCategory category() { return category; }
    }

    public static final class DirectWorthData {
        private final double worth;
        private final String sourceKey;
        private final String categoryKey;
        private final String resolutionType;

        public DirectWorthData(double worth, String sourceKey, String categoryKey, String resolutionType) {
            this.worth = worth;
            this.sourceKey = sourceKey;
            this.categoryKey = categoryKey;
            this.resolutionType = resolutionType;
        }

        public double worth() { return worth; }
        public String sourceKey() { return sourceKey; }
        public String categoryKey() { return categoryKey; }
        public String resolutionType() { return resolutionType; }
    }

    public static final class WorthCacheKey {
        private final Material material;
        private final PotionType potionType;
        private final Map<String, Integer> enchantments;

        public WorthCacheKey(Material material, PotionType potionType, Map<String, Integer> enchantments) {
            this.material = material;
            this.potionType = potionType;
            this.enchantments = enchantments == null ? Collections.<String, Integer>emptyMap() : enchantments;
        }

        public static WorthCacheKey of(ItemStack item) {
            if (item == null || isAir(item.getType())) {
                return new WorthCacheKey(null, null, Collections.<String, Integer>emptyMap());
            }

            PotionType potionType = null;
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof PotionMeta) {
                potionType = resolveBasePotionType((PotionMeta) meta);
            }

            Map<String, Integer> enchants = new HashMap<>();
            if (item.getType() == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
                for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                    enchants.put(getEnchantmentIdentifier(entry.getKey()), entry.getValue());
                }
            } else {
                for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                    enchants.put(getEnchantmentIdentifier(entry.getKey()), entry.getValue());
                }
            }

            return new WorthCacheKey(item.getType(), potionType, enchants);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WorthCacheKey)) return false;
            WorthCacheKey that = (WorthCacheKey) o;
            return material == that.material
                    && potionType == that.potionType
                    && Objects.equals(enchantments, that.enchantments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(material, potionType, enchantments);
        }
    }

    private final UltimateDonutSmp plugin;

    private List<WorthBrowserEntry> browserEntriesCache = Collections.emptyList();
    private boolean browserEntriesLoaded;

    private final Map<WorthCacheKey, DirectWorthData> directWorthCache = new java.util.concurrent.ConcurrentHashMap<WorthCacheKey, DirectWorthData>();
    private final Map<String, Double> enchantmentWorthCache = new java.util.concurrent.ConcurrentHashMap<String, Double>();
    private final Map<WorthCacheKey, java.util.Optional<SellCategory>> sellCategoryCache = new java.util.concurrent.ConcurrentHashMap<WorthCacheKey, java.util.Optional<SellCategory>>();
    private final Set<Material> blockedMaterialsCache = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean blockedMaterialsLoaded = false;
    /** Reverse index: "MATERIAL:durability" -&gt; worth.yml keys that name the same item in 1.13+ spelling. */
    private final Map<String, List<String>> worthKeyAliases = new HashMap<String, List<String>>();
    private boolean worthKeyAliasesLoaded = false;
    private volatile boolean packetDisplayActive = false;

    private static final DirectWorthData NULL_DIRECT_WORTH = new DirectWorthData(-1.0, "", "", "");

    public WorthManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        browserEntriesCache = Collections.emptyList();
        browserEntriesLoaded = false;
        directWorthCache.clear();
        enchantmentWorthCache.clear();
        sellCategoryCache.clear();
        blockedMaterialsCache.clear();
        blockedMaterialsLoaded = false;
        worthKeyAliases.clear();
        worthKeyAliasesLoaded = false;
    }

    public void setPacketDisplayActive(boolean active) {
        this.packetDisplayActive = active;
    }

    public double getWorth(Material material) {
        if (isAir(material)) {
            return -1;
        }
        return getWorth(new ItemStack(material));
    }

    public double getWorth(ItemStack item) {
        DirectWorthData data = resolveDirectWorth(item);
        return data == null ? -1 : data.worth();
    }

    public WorthResult resolveWorth(ItemStack item) {
        return resolveWorth(item, 0, true, new HashSet<Integer>());
    }

    public List<SellWorthEntry> resolveSellWorthEntries(ItemStack item) {
        if (item == null || isAir(item.getType())) {
            return Collections.emptyList();
        }
        List<SellWorthEntry> entries = new ArrayList<SellWorthEntry>();
        collectSellWorthEntries(item, 1, 0, true, new HashSet<Integer>(), entries);
        return new ArrayList<SellWorthEntry>(entries);
    }

    public SellCategory getSellCategory(ItemStack item) {
        if (item == null || isAir(item.getType()) || isBlockedItem(item)) {
            return null;
        }

        WorthCacheKey cacheKey = WorthCacheKey.of(item);
        java.util.Optional<SellCategory> cached = sellCategoryCache.get(cacheKey);
        if (cached != null) {
            return cached.orElse(null);
        }

        SellCategory resolved = getSellCategoryNoCache(item);
        sellCategoryCache.put(cacheKey, java.util.Optional.ofNullable(resolved));
        return resolved;
    }

    private SellCategory getSellCategoryNoCache(ItemStack item) {
        Material material = item.getType();
        if (FISH_CATEGORY_OVERRIDE_NAMES.contains(material.name())) {
            return SellCategory.FISH;
        }
        if (POTION_CATEGORY_OVERRIDE_NAMES.contains(material.name())) {
            return SellCategory.POTIONS;
        }

        ConfigurationSection typedValues = plugin.getConfigManager().getWorth().getConfigurationSection("TYPE");
        if (typedValues == null) {
            return null;
        }

        for (SellCategory category : SellCategory.values()) {
            ConfigurationSection categorySection = typedValues.getConfigurationSection(category.getWorthSectionKey());
            if (categorySection == null) {
                continue;
            }
            DirectWorthData directWorth = findWorthRecursively(categorySection, item, category.getWorthSectionKey());
            if (directWorth != null) {
                return category;
            }
        }
        return null;
    }

    public List<WorthBrowserEntry> getBrowserEntries() {
        if (browserEntriesLoaded) {
            return browserEntriesCache;
        }

        FileConfiguration worthConfig = plugin.getConfigManager().getWorth();
        ConfigurationSection typedValues = worthConfig.getConfigurationSection("TYPE");
        if (typedValues == null) {
            browserEntriesCache = Collections.emptyList();
            browserEntriesLoaded = true;
            return browserEntriesCache;
        }

        List<String> configuredOrder = worthConfig.getStringList("BROWSER.CATEGORY-SORT");
        List<String> categoryOrder = new ArrayList<String>();
        if (configuredOrder != null && !configuredOrder.isEmpty()) {
            categoryOrder.addAll(configuredOrder);
        }
        for (String categoryKey : typedValues.getKeys(false)) {
            if (!categoryOrder.contains(categoryKey)) {
                categoryOrder.add(categoryKey);
            }
        }

        List<WorthBrowserEntry> entries = new ArrayList<WorthBrowserEntry>();
        Set<Material> seenMaterials = new HashSet<Material>();

        for (String categoryKey : categoryOrder) {
            ConfigurationSection categorySection = typedValues.getConfigurationSection(categoryKey);
            if (categorySection == null) {
                continue;
            }

            for (String key : categorySection.getKeys(false)) {
                if (categorySection.isConfigurationSection(key) || key.contains(":")) {
                    continue;
                }

                Material material = matchMaterial(key);
                if (material == null || isAir(material) || isBlockedMaterial(material) || !seenMaterials.add(material)) {
                    continue;
                }

                double unitWorth = categorySection.getDouble(key, -1);
                if (unitWorth < 0) {
                    continue;
                }

                entries.add(new WorthBrowserEntry(material, categoryKey, unitWorth, key));
            }
        }

        browserEntriesCache = new ArrayList<WorthBrowserEntry>(entries);
        browserEntriesLoaded = true;
        return browserEntriesCache;
    }

    public String getBrowserTitle() {
        return plugin.getConfigManager().getWorth().getString("BROWSER.TITLE", "&8item prices");
    }

    public int getBrowserSize() {
        int configured = plugin.getConfigManager().getWorth().getInt("BROWSER.SIZE", 54);
        if (configured < 18 || configured > 54 || configured % 9 != 0) {
            return 54;
        }
        return configured;
    }

    public int getBrowserItemsPerPage() {
        int inventorySize = getBrowserSize();
        int configured = plugin.getConfigManager().getWorth().getInt("BROWSER.ITEMS-PER-PAGE", inventorySize - 9);
        int maxAllowed = Math.max(9, inventorySize - 9);
        return Math.max(9, Math.min(maxAllowed, configured));
    }

    public void syncWorthDisplay(Player player) {
        syncWorthDisplay(player, false);
    }

    public void syncWorthDisplay(Player player, boolean forceUpdate) {
        if (player == null) {
            return;
        }

        // Packet mode renders worth into copies of the outgoing items and never stores lore in
        // the real inventory; syncing real items alongside it would rewrite held stacks and
        // force a full window resync (the visible flicker) for work that is never needed.
        if (packetDisplayActive) {
            return;
        }

        boolean enabled = isWorthDisplayEnabled(player);
        syncInventoryWorthDisplay(player.getInventory(), enabled);

        ItemStack cursor = player.getItemOnCursor();
        ItemStack updatedCursor = updateWorthDisplay(cursor, enabled);
        if (updatedCursor != cursor) {
            player.setItemOnCursor(updatedCursor);
        }

        if (player.isOnline() && shouldUpdateInventory(player, forceUpdate)) {
            player.updateInventory();
        }
    }

    public void syncWorthDisplay(Player player, Inventory inventory) {
        syncWorthDisplay(player, inventory, false);
    }

    public void syncWorthDisplay(Player player, Inventory inventory, boolean forceUpdate) {
        if (player == null) {
            return;
        }
        if (packetDisplayActive) {
            return;
        }
        syncInventoryWorthDisplay(inventory, isWorthDisplayEnabled(player));
        if (player.isOnline() && shouldUpdateInventory(player, forceUpdate)) {
            player.updateInventory();
        }
    }

    private boolean shouldUpdateInventory(Player player) {
        return shouldUpdateInventory(player, false);
    }

    private boolean shouldUpdateInventory(Player player, boolean forceUpdate) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory == null) {
            return true;
        }

        InventoryType type = topInventory.getType();
        if (holdsClientState(type)) {
            return false;
        }

        if (forceUpdate) {
            return true;
        }

        return type != InventoryType.CRAFTING && type != InventoryType.CREATIVE;
    }

    static boolean holdsClientState(InventoryType type) {
        if (type == null) {
            return false;
        }

        String name = type.name();
        return "WORKBENCH".equals(name)
                || "ANVIL".equals(name)
                || "SMITHING".equals(name)
                || "GRINDSTONE".equals(name)
                || "STONECUTTER".equals(name)
                || "CARTOGRAPHY".equals(name)
                || "LOOM".equals(name)
                || "ENCHANTING".equals(name)
                || "MERCHANT".equals(name)
                || "BEACON".equals(name);
    }

    public boolean canResendOpenInventory(Player player) {
        return shouldUpdateInventory(player, true);
    }

    public ItemStack applyWorthDisplayForPlayer(Player player, ItemStack item) {
        if (player == null) {
            return item;
        }
        return updateWorthDisplay(item, isWorthDisplayEnabled(player));
    }

    public boolean isWorthDisplayEnabledFor(Player player) {
        return isWorthDisplayEnabled(player);
    }

    public ItemStack renderClientWorthDisplay(ItemStack item) {
        if (item == null || isAir(item.getType()) || isWorthDisplayExcluded(item)) {
            return item;
        }

        String loreLine = getWorthLoreLine(item);
        if (loreLine == null || loreLine.trim().isEmpty()) {
            return item;
        }

        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return item;
        }

        List<String> base = stripExistingWorthLore(meta.getLore());
        List<String> desired = base == null ? new ArrayList<String>() : new ArrayList<String>(base);
        desired.add(ColorUtils.toComponent(markWorthLore(loreLine)));
        meta.setLore(desired);
        clone.setItemMeta(meta);
        return clone;
    }

    public boolean stripStorageWorthDisplayForNativePickup(Player player, ItemStack... contextItems) {
        if (player == null) {
            return false;
        }

        boolean modified = false;
        Inventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();

        Set<Material> targetMaterials = new HashSet<Material>();
        if (contextItems != null) {
            for (ItemStack item : contextItems) {
                if (item != null && !isAir(item.getType())) {
                    targetMaterials.add(item.getType());
                }
            }
        }

        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack current = storage[slot];
            if (current == null || isAir(current.getType()) || current.getMaxStackSize() <= 1) {
                continue;
            }
            if (!targetMaterials.isEmpty() && !targetMaterials.contains(current.getType())) {
                continue;
            }

            ItemStack stripped = stripWorthDisplay(current);
            if (stripped != current) {
                storage[slot] = stripped;
                inventory.setItem(slot, stripped);
                modified = true;
            }
        }
        return modified;
    }

    public void mergeStorageStacksForNativeBehavior(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack stripped = stripWorthDisplay(storage[slot]);
            if (stripped != storage[slot]) {
                storage[slot] = stripped;
                inventory.setItem(slot, stripped);
            }
        }
        mergePlayerStorageStacks(player);
    }

    public void mergePlayerStorageStacks(Player player) {
        if (player == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();

        for (int i = 0; i < storage.length; i++) {
            ItemStack current = storage[i];
            if (current == null || isAir(current.getType()) || current.getMaxStackSize() <= 1) {
                continue;
            }

            current = stripWorthDisplay(current);
            if (current != storage[i]) {
                storage[i] = current;
                inventory.setItem(i, current);
            }

            int max = current.getMaxStackSize();
            for (int j = i + 1; j < storage.length && current.getAmount() < max; j++) {
                ItemStack next = storage[j];
                if (next == null || isAir(next.getType())) {
                    continue;
                }
                next = stripWorthDisplay(next);
                if (!current.isSimilar(next)) {
                    continue;
                }

                int transfer = Math.min(max - current.getAmount(), next.getAmount());
                if (transfer <= 0) {
                    continue;
                }

                current.setAmount(current.getAmount() + transfer);
                next.setAmount(next.getAmount() - transfer);

                inventory.setItem(i, current);
                if (next.getAmount() <= 0) {
                    storage[j] = null;
                    inventory.setItem(j, null);
                } else {
                    storage[j] = next;
                    inventory.setItem(j, next);
                }
            }
        }
    }

    public void clearWorthDisplay(Player player) {
        if (player == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            ItemStack stripped = stripWorthDisplay(current);
            if (stripped != current) {
                inventory.setItem(slot, stripped);
            }
        }

        ItemStack cursor = player.getItemOnCursor();
        ItemStack strippedCursor = stripWorthDisplay(cursor);
        if (strippedCursor != cursor) {
            player.setItemOnCursor(strippedCursor);
        }

        if (shouldUpdateInventory(player)) {
            player.updateInventory();
        }
    }

    public void sanitizeInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            ItemStack stripped = stripWorthDisplay(current);
            if (stripped != current) {
                inventory.setItem(slot, stripped);
            }
        }
    }

    private void syncInventoryWorthDisplay(Inventory inventory, boolean enabled) {
        if (inventory == null) {
            return;
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            ItemStack updated = updateWorthDisplay(current, enabled);
            if (updated != current) {
                inventory.setItem(slot, updated);
            }
        }
    }

    public boolean isSimilarIgnoringWorth(ItemStack a, ItemStack b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.getType() != b.getType()) {
            return false;
        }
        ItemStack strippedA = stripWorthDisplay(a);
        ItemStack strippedB = stripWorthDisplay(b);
        return strippedA.isSimilar(strippedB);
    }

    public ItemStack stripWorthDisplay(ItemStack item) {
        if (item == null || isAir(item.getType())) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        List<String> currentLore = meta.getLore();
        List<String> strippedLore = stripExistingWorthLore(readOriginalLore(meta));
        if (Objects.equals(currentLore, strippedLore)) {
            return item;
        }

        ItemStack updated = item.clone();
        ItemMeta updatedMeta = updated.getItemMeta();
        if (updatedMeta == null) {
            return item;
        }

        updatedMeta.setLore(strippedLore == null || strippedLore.isEmpty() ? null : strippedLore);
        updated.setItemMeta(updatedMeta);
        return updated;
    }

    public String getWorthLoreLine(ItemStack item) {
        WorthResult worthResult = resolveWorth(item);
        if (!worthResult.sellable()) {
            return null;
        }

        String itemName = prettifyMaterial(item == null ? null : item.getType());
        double displayWorth = getDisplayWorth(worthResult);
        return replaceWorthPlaceholders(
                getWorthLoreFormat(),
                plugin.getCurrencyManager().formatCompactAmount(CurrencyManager.CurrencyType.MONEY, displayWorth),
                plugin.getCurrencyManager().formatCompactAmount(CurrencyManager.CurrencyType.MONEY, worthResult.unitWorth()),
                String.valueOf(item == null ? 0 : item.getAmount()),
                itemName,
                plugin.getCurrencyManager().formatMoney(displayWorth),
                plugin.getCurrencyManager().formatMoney(worthResult.unitWorth())
        );
    }

    public static double getDisplayWorth(WorthResult worthResult) {
        if (worthResult == null || !worthResult.sellable()) {
            return 0;
        }
        return worthResult.totalWorth();
    }

    public String prettifyMaterial(Material material) {
        if (material == null) {
            return "unknown";
        }

        String[] tokens = material.name().toLowerCase(Locale.US).split("_");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
        }
        return builder.toString();
    }

    public Material findMaterial(String input) {
        return matchMaterial(input);
    }

    private WorthResult resolveWorth(ItemStack item, int depth, boolean allowNestedExpansion, Set<Integer> visitedContainers) {
        if (item == null || isAir(item.getType()) || isBlockedItem(item)) {
            return WorthResult.unsellable();
        }

        DirectWorthData directWorth = resolveDirectWorth(item);
        if (isContainerWorthEnabled() && allowNestedExpansion && isSupportedContainer(item) && depth < getMaxContainerDepth()) {
            double contentsWorth = resolveContainerContentsWorth(item, depth + 1, allowNestedExpansion, visitedContainers);
            if (contentsWorth > 0) {
                double baseWorth = directWorth == null ? 0 : directWorth.worth();
                double totalUnitWorth = contentsWorth + (shouldIncludeContainerBasePrice() ? baseWorth : 0);
                if (totalUnitWorth > 0) {
                    return new WorthResult(true, true, totalUnitWorth, totalUnitWorth * item.getAmount(), baseWorth,
                            contentsWorth, "container", item.getType().name(), directWorth == null ? "" : directWorth.categoryKey());
                }
            }
        }

        if (directWorth == null) {
            return WorthResult.unsellable();
        }

        return new WorthResult(true, false, directWorth.worth(), directWorth.worth() * item.getAmount(),
                directWorth.worth(), 0, directWorth.resolutionType(), directWorth.sourceKey(), directWorth.categoryKey());
    }

    private double resolveContainerContentsWorth(ItemStack item, int depth, boolean allowNestedExpansion, Set<Integer> visitedContainers) {
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta)) {
            return 0;
        }

        BlockState blockState = ((BlockStateMeta) itemMeta).getBlockState();
        if (!(blockState instanceof Container)) {
            return 0;
        }
        Container container = (Container) blockState;

        int containerIdentity = buildContainerIdentity(item);
        if (!visitedContainers.add(containerIdentity)) {
            return 0;
        }

        try {
            double total = 0;
            for (ItemStack content : container.getInventory().getContents()) {
                if (content == null || isAir(content.getType())) {
                    continue;
                }
                WorthResult contentWorth = resolveWorth(content, depth, allowNestedExpansion && allowNestedContainers(), visitedContainers);
                if (contentWorth.sellable()) {
                    total += contentWorth.totalWorth();
                }
            }
            return total;
        } finally {
            visitedContainers.remove(containerIdentity);
        }
    }

    private void collectSellWorthEntries(ItemStack item, int amountMultiplier, int depth, boolean allowNestedExpansion,
                                         Set<Integer> visitedContainers, List<SellWorthEntry> entries) {
        if (item == null || isAir(item.getType()) || amountMultiplier <= 0 || isBlockedItem(item)) {
            return;
        }

        boolean container = isSupportedContainer(item);
        DirectWorthData directWorth = resolveDirectWorth(item);
        if (directWorth != null && (!container || !isContainerWorthEnabled() || shouldIncludeContainerBasePrice())) {
            SellCategory category = resolveSellCategory(directWorth, item);
            if (category != null && directWorth.worth() > 0) {
                int amount = multiplyAmount(item.getAmount(), amountMultiplier);
                entries.add(new SellWorthEntry(item.getType(), amount,
                        directWorth.worth() * item.getAmount() * amountMultiplier, category));
            }
        }

        if (!isContainerWorthEnabled() || !container || !allowNestedExpansion || depth >= getMaxContainerDepth()) {
            return;
        }

        collectContainerSellWorthEntries(item, multiplyAmount(item.getAmount(), amountMultiplier), depth + 1,
                allowNestedExpansion && allowNestedContainers(), visitedContainers, entries);
    }

    private void collectContainerSellWorthEntries(ItemStack item, int amountMultiplier, int depth,
                                                  boolean allowNestedExpansion, Set<Integer> visitedContainers,
                                                  List<SellWorthEntry> entries) {
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta)) {
            return;
        }
        BlockState blockState = ((BlockStateMeta) itemMeta).getBlockState();
        if (!(blockState instanceof Container)) {
            return;
        }

        Container container = (Container) blockState;
        int containerIdentity = buildContainerIdentity(item);
        if (!visitedContainers.add(containerIdentity)) {
            return;
        }

        try {
            for (ItemStack content : container.getInventory().getContents()) {
                collectSellWorthEntries(content, amountMultiplier, depth, allowNestedExpansion, visitedContainers, entries);
            }
        } finally {
            visitedContainers.remove(containerIdentity);
        }
    }

    private SellCategory resolveSellCategory(DirectWorthData directWorth, ItemStack item) {
        if (directWorth != null && directWorth.categoryKey() != null && !directWorth.categoryKey().trim().isEmpty()) {
            return SellCategory.fromConfigKey(directWorth.categoryKey()).orElse(null);
        }
        return getSellCategory(item);
    }

    private int multiplyAmount(int amount, int multiplier) {
        long result = (long) amount * multiplier;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private int buildContainerIdentity(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return Objects.hash(item.getType(), meta == null ? null : meta.getDisplayName(), meta == null ? null : meta.getLore(),
                meta == null ? null : meta.getEnchants());
    }

    private boolean isSupportedContainer(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        return meta instanceof BlockStateMeta && ((BlockStateMeta) meta).getBlockState() instanceof Container;
    }

    private boolean isBlockedItem(ItemStack item) {
        return item != null && isBlockedMaterial(item.getType());
    }

    private boolean isBlockedMaterial(Material material) {
        if (material == null || isAir(material)) {
            return false;
        }

        if (!blockedMaterialsLoaded) {
            blockedMaterialsCache.clear();
            List<String> blockItems = plugin.getConfigManager().getWorth().getStringList("BLOCK-ITEMS");
            if (blockItems != null) {
                for (String rawMaterial : blockItems) {
                    Material blocked = matchMaterial(rawMaterial);
                    if (blocked != null) {
                        blockedMaterialsCache.add(blocked);
                    }
                }
            }
            blockedMaterialsLoaded = true;
        }

        return blockedMaterialsCache.contains(material);
    }

    private boolean isContainerWorthEnabled() {
        return plugin.getConfigManager().getWorth().getBoolean("CONTAINER.ENABLED", true);
    }

    private boolean shouldIncludeContainerBasePrice() {
        return plugin.getConfigManager().getWorth().getBoolean("CONTAINER.INCLUDE-CONTAINER-BASE-PRICE", true);
    }

    private boolean allowNestedContainers() {
        return plugin.getConfigManager().getWorth().getBoolean("CONTAINER.ALLOW-NESTED-CONTAINERS", false);
    }

    private int getMaxContainerDepth() {
        return Math.max(1, plugin.getConfigManager().getWorth().getInt("CONTAINER.MAX-CONTAINER-DEPTH", 1));
    }

    private DirectWorthData resolveDirectWorth(ItemStack item) {
        if (item == null || isAir(item.getType()) || isBlockedItem(item)) {
            return null;
        }

        WorthCacheKey cacheKey = WorthCacheKey.of(item);
        DirectWorthData cached = directWorthCache.get(cacheKey);
        if (cached != null) {
            return cached == NULL_DIRECT_WORTH ? null : cached;
        }

        DirectWorthData resolved = resolveDirectWorthNoCache(item);
        directWorthCache.put(cacheKey, resolved == null ? NULL_DIRECT_WORTH : resolved);
        return resolved;
    }

    private DirectWorthData resolveDirectWorthNoCache(ItemStack item) {
        if (item == null || isAir(item.getType()) || isBlockedItem(item)) {
            return null;
        }

        FileConfiguration worthConfig = plugin.getConfigManager().getWorth();
        ConfigurationSection typedValues = worthConfig.getConfigurationSection("TYPE");
        DirectWorthData resolved = null;

        if (typedValues != null) {
            for (String categoryKey : typedValues.getKeys(false)) {
                ConfigurationSection categorySection = typedValues.getConfigurationSection(categoryKey);
                DirectWorthData typedWorth = findWorthRecursively(categorySection, item, categoryKey);
                if (typedWorth != null) {
                    resolved = typedWorth;
                    break;
                }
            }
        }

        if (resolved == null) {
            resolved = findWorthRecursively(worthConfig, item, "");
        }

        return addEnchantmentWorthIfApplicable(item, resolved);
    }

    private DirectWorthData addEnchantmentWorthIfApplicable(ItemStack item, DirectWorthData baseData) {
        if (baseData == null || item == null || item.getType() == Material.ENCHANTED_BOOK) {
            return baseData;
        }

        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        if (enchantments.isEmpty()) {
            return baseData;
        }

        double extraWorth = 0;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            extraWorth += getEnchantmentWorth(entry.getKey(), entry.getValue());
        }

        if (extraWorth <= 0) {
            return baseData;
        }

        return new DirectWorthData(baseData.worth() + extraWorth, baseData.sourceKey(), baseData.categoryKey(),
                baseData.resolutionType() + "_ENCHANTED");
    }

    private double getEnchantmentWorth(Enchantment enchantment, int level) {
        String enchantmentKey = getEnchantmentIdentifier(enchantment).toUpperCase(Locale.US).replace('-', '_');
        String lookupKey = "ENCHANTED_BOOK:" + enchantmentKey + ":" + level;

        Double cached = enchantmentWorthCache.get(lookupKey);
        if (cached != null) {
            return cached;
        }

        double resolved = getEnchantmentWorthNoCache(lookupKey);
        enchantmentWorthCache.put(lookupKey, resolved);
        return resolved;
    }

    private double getEnchantmentWorthNoCache(String lookupKey) {
        FileConfiguration worthConfig = plugin.getConfigManager().getWorth();
        if (worthConfig == null) {
            return 0;
        }

        double direct = worthConfig.getDouble("TYPE.BOOK." + lookupKey, -1);
        if (direct >= 0) {
            return direct;
        }

        return findEnchantmentWorthRecursively(worthConfig, lookupKey);
    }

    private double findEnchantmentWorthRecursively(ConfigurationSection section, String lookupKey) {
        if (section == null) {
            return 0;
        }

        if (section.contains(lookupKey) && !section.isConfigurationSection(lookupKey)) {
            return section.getDouble(lookupKey, 0);
        }

        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                continue;
            }
            double worth = findEnchantmentWorthRecursively(section.getConfigurationSection(key), lookupKey);
            if (worth > 0) {
                return worth;
            }
        }
        return 0;
    }

    private DirectWorthData findWorthRecursively(ConfigurationSection section, ItemStack item, String categoryKey) {
        if (section == null || item == null || isAir(item.getType())) {
            return null;
        }

        DirectWorthData directWorth = getDirectWorth(section, item, categoryKey);
        if (directWorth != null) {
            return directWorth;
        }

        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                continue;
            }

            DirectWorthData nestedWorth = findWorthRecursively(section.getConfigurationSection(key), item, categoryKey);
            if (nestedWorth != null) {
                return nestedWorth;
            }
        }
        return null;
    }

    private DirectWorthData getDirectWorth(ConfigurationSection section, ItemStack item, String categoryKey) {
        DirectWorthData specificWorth = resolveSpecificItemWorth(section, item, categoryKey);
        if (specificWorth != null) {
            return specificWorth;
        }

        String materialKey = item.getType().name();
        if (section.contains(materialKey) && !section.isConfigurationSection(materialKey)) {
            return new DirectWorthData(section.getDouble(materialKey, -1), materialKey, categoryKey, "DIRECT");
        }

        // worth.yml is written with 1.13+ flattened names (OAK_PLANKS, COBBLESTONE_SLAB, ...).
        // On 1.12.2 item.getType().name() is the shared legacy name (WOOD, STEP, ...), so a direct
        // key lookup silently misses and the item ends up with no worth at all. Resolve the modern
        // spellings back onto the legacy material + durability before giving up.
        for (String alias : aliasKeysFor(item.getType(), item.getDurability())) {
            if (section.contains(alias) && !section.isConfigurationSection(alias)) {
                return new DirectWorthData(section.getDouble(alias, -1), alias, categoryKey, "DIRECT");
            }
        }
        return null;
    }

    private List<String> aliasKeysFor(Material material, short durability) {
        if (material == null) {
            return Collections.emptyList();
        }
        loadWorthKeyAliases();
        List<String> keys = worthKeyAliases.get(material.name() + ":" + durability);
        return keys == null ? Collections.<String>emptyList() : keys;
    }

    private void loadWorthKeyAliases() {
        if (worthKeyAliasesLoaded) {
            return;
        }
        worthKeyAliasesLoaded = true;

        FileConfiguration worthConfig = plugin.getConfigManager().getWorth();
        if (worthConfig == null) {
            return;
        }
        collectWorthKeyAliases(worthConfig, new HashSet<String>());
    }

    private void collectWorthKeyAliases(ConfigurationSection section, Set<String> visitedKeys) {
        for (String key : section.getKeys(false)) {
            if (key.contains(":") || key.indexOf('.') >= 0) {
                continue;
            }
            if (section.isConfigurationSection(key)) {
                collectWorthKeyAliases(section.getConfigurationSection(key), visitedKeys);
                continue;
            }
            if (!visitedKeys.add(key)) {
                continue;
            }
            com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport.Icon icon =
                    com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport.resolve(key);
            if (icon == null || icon.material() == null || !icon.isExact()) {
                // A visual approximation (netherite block rendered as glowstone, deepslate ore
                // as the surface ore, ...) may decorate a menu, but the 1.12.2 item it borrows
                // is a different item in an inventory. Aliasing worth through it would price a
                // $3 glowstone stack at the netherite-block worth.
                continue;
            }
            String aliasTarget = icon.material().name() + ":" + icon.data();
            List<String> keys = worthKeyAliases.get(aliasTarget);
            if (keys == null) {
                keys = new ArrayList<String>();
                worthKeyAliases.put(aliasTarget, keys);
            }
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }
    }

    private DirectWorthData resolveSpecificItemWorth(ConfigurationSection section, ItemStack item, String categoryKey) {
        if (item.getType() == Material.ENCHANTED_BOOK) {
            return resolveEnchantedBookWorth(section, item, categoryKey);
        }

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof PotionMeta) {
            PotionType potionType = resolveBasePotionType((PotionMeta) itemMeta);
            String potionKey = buildPotionWorthKey(item.getType(), potionType);
            if (potionKey != null && section.contains(potionKey) && !section.isConfigurationSection(potionKey)) {
                return new DirectWorthData(section.getDouble(potionKey, -1), potionKey, categoryKey, "DIRECT");
            }

            if (potionType != null) {
                String keyOnlyType = potionType.name();
                if (section.contains(keyOnlyType) && !section.isConfigurationSection(keyOnlyType)) {
                    return new DirectWorthData(section.getDouble(keyOnlyType, -1), keyOnlyType, categoryKey, "DIRECT");
                }
            }
        }
        return null;
    }

    private DirectWorthData resolveEnchantedBookWorth(ConfigurationSection section, ItemStack item, String categoryKey) {
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof EnchantmentStorageMeta)) {
            return null;
        }

        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
        if (meta.getStoredEnchants().isEmpty()) {
            return null;
        }

        double total = 0;
        boolean matched = false;
        for (Map.Entry<Enchantment, Integer> enchantmentEntry : meta.getStoredEnchants().entrySet()) {
            String enchantmentKey = getEnchantmentIdentifier(enchantmentEntry.getKey()).toUpperCase(Locale.US).replace('-', '_');
            String worthKey = item.getType().name() + ":" + enchantmentKey + ":" + enchantmentEntry.getValue();
            if (section.contains(worthKey) && !section.isConfigurationSection(worthKey)) {
                total += section.getDouble(worthKey, 0);
                matched = true;
            }
        }

        if (matched) {
            return new DirectWorthData(total, "ENCHANTED_BOOK", categoryKey, "BOOK_VARIANT");
        }

        String baseKey = item.getType().name();
        if (section.contains(baseKey) && !section.isConfigurationSection(baseKey)) {
            return new DirectWorthData(section.getDouble(baseKey, -1), baseKey, categoryKey, "DIRECT");
        }

        return null;
    }

    private boolean isWorthDisplayEnabled(Player player) {
        if (player == null) {
            return false;
        }
        PlayerData data = plugin.getPlayerDataManager().get(player);
        return data == null || data.isWorthDisplayEnabled();
    }

    private ItemStack updateWorthDisplay(ItemStack item, boolean enabled) {
        if (item == null || isAir(item.getType())) {
            return item;
        }

        if (packetDisplayActive) {
            return item;
        }

        if (!enabled || isWorthDisplayExcluded(item)) {
            return stripWorthDisplay(item);
        }

        String loreLine = getWorthLoreLine(item);
        if (loreLine == null || loreLine.trim().isEmpty()) {
            return stripWorthDisplay(item);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        List<String> baseLore = stripExistingWorthLore(meta.getLore());
        List<String> desiredLore = baseLore == null ? new ArrayList<String>() : new ArrayList<String>(baseLore);
        desiredLore.add(ColorUtils.toComponent(markWorthLore(loreLine)));

        if (Objects.equals(meta.getLore(), desiredLore)) {
            return item;
        }

        ItemStack updated = item.clone();
        ItemMeta updatedMeta = updated.getItemMeta();
        if (updatedMeta == null) {
            return item;
        }

        updatedMeta.setLore(desiredLore);
        updated.setItemMeta(updatedMeta);
        return updated;
    }

    private boolean isWorthDisplayExcluded(ItemStack item) {
        return item == null || isAir(item.getType()) || isBlockedItem(item);
    }

    private List<String> stripExistingWorthLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return lore;
        }

        List<String> out = new ArrayList<String>();
        for (String line : lore) {
            if (line == null) {
                out.add(null);
                continue;
            }
            if (!isWorthLoreLine(line)) {
                out.add(line);
            }
        }
        return out;
    }

    private List<String> readOriginalLore(ItemMeta meta) {
        if (meta == null) {
            return null;
        }
        return meta.getLore();
    }

    private String getWorthLoreFormat() {
        return plugin.getConfigManager().getWorth().getString("DISPLAY.FORMAT", "&7Worth: &a${price}");
    }

    private String replaceWorthPlaceholders(String format,
                                            String priceCompact,
                                            String unitCompact,
                                            String amount,
                                            String item,
                                            String price,
                                            String unitPrice) {
        String value = format == null ? "" : format;
        return value
                .replace("{price}", priceCompact)
                .replace("{unit_price}", unitCompact)
                .replace("{stack_size}", amount)
                .replace("{amount}", amount)
                .replace("{item}", item)
                .replace("{price_raw}", price)
                .replace("{unit_price_raw}", unitPrice)
                .replace("{total_price}", priceCompact)
                .replace("{unit_price_compact}", unitCompact)
                .replace("{stack_price_compact}", priceCompact);
    }

    private static Material matchMaterial(String input) {
        if (input == null) {
            return null;
        }

        String normalized = input.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        normalized = normalized.replace("minecraft:", "");
        normalized = normalized.toUpperCase(Locale.US).replace(' ', '_').replace('-', '_');

        Material material = Material.matchMaterial(normalized);
        if (material != null) {
            return material;
        }

        if ("MELON".equals(normalized)) return Material.matchMaterial("MELON_BLOCK");
        if ("INK_SACK".equals(normalized)) return Material.matchMaterial("INK_SACK");
        if ("GREEN_DYE".equals(normalized)) return Material.matchMaterial("CACTUS_GREEN");
        if ("LAPIS_LAZULI".equals(normalized)) return Material.matchMaterial("INK_SACK");
        if ("COCOA_BEANS".equals(normalized)) return Material.matchMaterial("INK_SACK");

        // 1.13+ flattened names never match the 1.12.2 enum directly, so the worth browser used to
        // drop those catalog entries in silence. Resolve them through the compatibility layer. Data
        // carrying spellings (SPRUCE_PLANKS, ...) are skipped: the browser keys on a Material only
        // and would render the wrong variant.
        com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport.Icon resolved =
                com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport.resolve(normalized);
        if (resolved != null && resolved.material() != null && resolved.data() == 0) {
            return resolved.material();
        }

        return null;
    }

    private static boolean isAir(Material material) {
        return material == null || material == Material.AIR;
    }

    private static PotionType resolveBasePotionType(PotionMeta meta) {
        if (meta == null) {
            return null;
        }
        try {
            PotionData data = meta.getBasePotionData();
            return data == null ? null : data.getType();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String getEnchantmentIdentifier(Enchantment enchantment) {
        if (enchantment == null) {
            return "";
        }

        String name = enchantment.getName();
        if (name == null) {
            return "";
        }
        return name.toUpperCase(Locale.US).replace(' ', '_');
    }

    private static String buildPotionWorthKey(Material material, PotionType potionType) {
        if (material == null || potionType == null) {
            return null;
        }

        String potionName = potionType.name().toUpperCase(Locale.US);
        boolean upgraded = false;

        if (potionName.startsWith("STRONG_")) {
            potionName = potionName.substring("STRONG_".length());
            upgraded = true;
        } else if (potionName.startsWith("LONG_")) {
            potionName = potionName.substring("LONG_".length());
        }

        StringBuilder builder = new StringBuilder(material.name()).append(':').append(potionName);
        if (upgraded) {
            builder.append(":2");
        }
        return builder.toString();
    }

    private static boolean isWorthLoreLine(String line) {
        if (line == null) {
            return false;
        }
        if (line.contains(WORTH_LORE_MARKER)) {
            return true;
        }
        for (String legacy : LEGACY_WORTH_LORE_MARKERS) {
            if (line.contains(legacy)) {
                return true;
            }
        }
        return false;
    }

    private static String markWorthLore(String loreLine) {
        return WORTH_LORE_MARKER + loreLine;
    }
}
