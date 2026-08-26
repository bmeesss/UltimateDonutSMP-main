package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.models.PlayerData;
import com.bx.ultimateDonutSmp.models.SellCategory;
import com.bx.ultimateDonutSmp.models.WorthResult;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import com.bx.ultimateDonutSmp.utils.NumberUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class WorthManager {

    private static final String NULL_LORE = "__NULL__";
    private static final String LORE_SEPARATOR = ";";
    private static final Set<Material> FISH_CATEGORY_OVERRIDES = new java.util.LinkedHashSet<>(java.util.Arrays.asList(
            Material.COD, 
            Material.COOKED_COD, 
            Material.SALMON, 
            Material.COOKED_SALMON, 
            Material.TROPICAL_FISH, 
            Material.PUFFERFISH, 
            Material.COD_BUCKET, 
            Material.SALMON_BUCKET, 
            Material.TROPICAL_FISH_BUCKET, 
            Material.PUFFERFISH_BUCKET, 
            Material.AXOLOTL_BUCKET, 
            Material.TADPOLE_BUCKET, 
            Material.FISHING_ROD, 
            Material.NAME_TAG, 
            Material.NAUTILUS_SHELL, 
            Material.LILY_PAD, 
            Material.HEART_OF_THE_SEA
    ));
    private static final Set<Material> POTION_CATEGORY_OVERRIDES = new java.util.LinkedHashSet<>(java.util.Arrays.asList(
            Material.POTION, 
            Material.SPLASH_POTION, 
            Material.LINGERING_POTION, 
            Material.TIPPED_ARROW, 
            Material.BREWING_STAND, 
            Material.BLAZE_POWDER, 
            Material.BLAZE_ROD, 
            Material.FERMENTED_SPIDER_EYE, 
            Material.GLASS_BOTTLE, 
            Material.GLISTERING_MELON_SLICE, 
            Material.GHAST_TEAR, 
            Material.MAGMA_CREAM, 
            Material.RABBIT_FOOT, 
            Material.SPIDER_EYE, 
            Material.SUGAR, 
            Material.GOLDEN_CARROT, 
            Material.PHANTOM_MEMBRANE
    ));

public final class WorthBrowserEntry {
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

    @Override public String toString() {
        return "WorthBrowserEntry[material=+material, categoryKey=+categoryKey, unitWorth=+unitWorth, sourceKey=+sourceKey]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorthBrowserEntry that = (WorthBrowserEntry) o;
        return java.util.Objects.equals(material, that.material) && java.util.Objects.equals(categoryKey, that.categoryKey) && java.util.Objects.equals(unitWorth, that.unitWorth) && java.util.Objects.equals(sourceKey, that.sourceKey);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(material, categoryKey, unitWorth, sourceKey);
    }
}

public final class SellWorthEntry {
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

    @Override public String toString() {
        return "SellWorthEntry[material=+material, amount=+amount, totalWorth=+totalWorth, category=+category]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SellWorthEntry that = (SellWorthEntry) o;
        return java.util.Objects.equals(material, that.material) && java.util.Objects.equals(amount, that.amount) && java.util.Objects.equals(totalWorth, that.totalWorth) && java.util.Objects.equals(category, that.category);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(material, amount, totalWorth, category);
    }
}

public final class DirectWorthData {
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

    @Override public String toString() {
        return "DirectWorthData[worth=+worth, sourceKey=+sourceKey, categoryKey=+categoryKey, resolutionType=+resolutionType]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DirectWorthData that = (DirectWorthData) o;
        return java.util.Objects.equals(worth, that.worth) && java.util.Objects.equals(sourceKey, that.sourceKey) && java.util.Objects.equals(categoryKey, that.categoryKey) && java.util.Objects.equals(resolutionType, that.resolutionType);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(worth, sourceKey, categoryKey, resolutionType);
    }
}

    private final UltimateDonutSmp plugin;
    private final NamespacedKey worthDisplayAppliedKey;
    private final NamespacedKey worthDisplayOriginalLoreKey;

    private List<WorthBrowserEntry> browserEntriesCache = Collections.emptyList();
    private boolean browserEntriesLoaded;

    private final java.util.Map<WorthCacheKey, DirectWorthData> directWorthCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Double> enchantmentWorthCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<WorthCacheKey, java.util.Optional<SellCategory>> sellCategoryCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<Material> blockedMaterialsCache = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean blockedMaterialsLoaded = false;
    private volatile boolean packetDisplayActive = false;

    private static final DirectWorthData NULL_DIRECT_WORTH = new DirectWorthData(-1.0, "", "", "");

public final class WorthCacheKey {
    private final Material material;
    private final PotionType potionType;
    private final java.util.Map<Enchantment, java.lang.Integer> enchantments;

    public WorthCacheKey(Material material, PotionType potionType, java.util.Map<Enchantment, java.lang.Integer> enchantments) {
        this.material = material;
        this.potionType = potionType;
        this.enchantments = enchantments;
    }

    public Material material() { return material; }
    public PotionType potionType() { return potionType; }
    public java.util.Map<Enchantment, java.lang.Integer> enchantments() { return enchantments; }


                public static WorthCacheKey of(ItemStack item) {
            if (item == null) {
                return new WorthCacheKey(null, null, java.util.Collections.emptyMap());
            }
            PotionType potionType = null;
            if (item.getItemMeta() instanceof PotionMeta) {
                PotionMeta meta = (PotionMeta) item.getItemMeta();
                potionType = meta.getBasePotionType();
            }
            java.util.Map<Enchantment, java.lang.Integer> enchants = java.util.Collections.emptyMap();
            if (item.getType() == Material.ENCHANTED_BOOK) {
                if (item.getItemMeta() instanceof EnchantmentStorageMeta) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                    enchants = meta.getStoredEnchants();
                }
            } else {
                enchants = item.getEnchantments();
            }
            return new WorthCacheKey(item.getType(), potionType, enchants);
        }
    @Override public int hashCode() {
        return java.util.Objects.hash(material, categoryKey, unitWorth, sourceKey);
    }
}


    public WorthManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
        this.worthDisplayAppliedKey = new NamespacedKey(plugin, "worth_display_applied");
        this.worthDisplayOriginalLoreKey = new NamespacedKey(plugin, "worth_display_original_lore");
    }

    public void reload() {
        browserEntriesCache = Collections.emptyList();
        browserEntriesLoaded = false;
        directWorthCache.clear();
        enchantmentWorthCache.clear();
        sellCategoryCache.clear();
        blockedMaterialsCache.clear();
        blockedMaterialsLoaded = false;
    }

    // toggled on at startup when the packet display is active, so worth is rendered per packet only
    public void setPacketDisplayActive(boolean active) {
        this.packetDisplayActive = active;
    }

    public double getWorth(Material material) {
        if (material == null || material.isAir()) {
            return -1;
        }

        return getWorth(new ItemStack(material));
    }

    public double getWorth(ItemStack item) {
        DirectWorthData data = resolveDirectWorth(item);
        return data == null ? -1 : data.worth();
    }

    public WorthResult resolveWorth(ItemStack item) {
        return resolveWorth(item, 0, true, new HashSet<>());
    }

    public List<SellWorthEntry> resolveSellWorthEntries(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return java.util.Collections.emptyList();
        }

        List<SellWorthEntry> entries = new ArrayList<>();
        collectSellWorthEntries(item, 1, 0, true, new HashSet<>(), entries);
        return new java.util.ArrayList<>(entries);
    }

    public SellCategory getSellCategory(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        if (isBlockedItem(item)) {
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
        if (FISH_CATEGORY_OVERRIDES.contains(material)) {
            return SellCategory.FISH;
        }
        if (POTION_CATEGORY_OVERRIDES.contains(material)) {
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
            browserEntriesCache = java.util.Collections.emptyList();
            browserEntriesLoaded = true;
            return browserEntriesCache;
        }

        List<String> configuredOrder = worthConfig.getStringList("BROWSER.CATEGORY-SORT");
        List<String> categoryOrder = new ArrayList<>();
        if (!configuredOrder.isEmpty()) {
            categoryOrder.addAll(configuredOrder);
        }
        for (String categoryKey : typedValues.getKeys(false)) {
            if (!categoryOrder.contains(categoryKey)) {
                categoryOrder.add(categoryKey);
            }
        }

        List<WorthBrowserEntry> entries = new ArrayList<>();
        Set<Material> seenMaterials = new HashSet<>();
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
                if (material == null || material.isAir() || isBlockedMaterial(material) || !seenMaterials.add(material)) {
                    continue;
                }

                double unitWorth = categorySection.getDouble(key, -1);
                if (unitWorth < 0) {
                    continue;
                }

                entries.add(new WorthBrowserEntry(material, categoryKey, unitWorth, key));
            }
        }

        browserEntriesCache = new java.util.ArrayList<>(entries);
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

    // Resending the whole window is safe on a chest, but these screens carry state the server never
    // sent the client - the three enchantment offers, a half-typed anvil name, a picked recipe. A
    // resync drops it. Java quietly redraws, Geyser does not, so a Bedrock player watches the
    // enchantment offers vanish the moment the item lands in the slot. Kept as a switch rather than
    // a constant set so nothing here touches the InventoryType registry before the server is up.
    static boolean holdsClientState(InventoryType type) {
        if (type == null) {
            return false;
        }

        switch (type) {
            case WORKBENCH:
            case ANVIL:
            case SMITHING:
            case GRINDSTONE:
            case STONECUTTER:
            case CARTOGRAPHY:
            case LOOM:
            case ENCHANTING:
            case MERCHANT:
            case BEACON:
                return true;
            default:
                return false;
        }
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
        if (item == null || item.getType().isAir()) {
            return item;
        }
        if (isWorthDisplayExcluded(item)) {
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
        List<String> desired = base == null ? new ArrayList<>() : new ArrayList<>(base);
        desired.add(ColorUtils.toComponent(loreLine));
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

        Set<Material> targetMaterials = new HashSet<>();
        if (contextItems != null) {
            for (ItemStack item : contextItems) {
                if (item != null && !item.getType().isAir()) {
                    targetMaterials.add(item.getType());
                }
            }
        }

        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack current = storage[slot];
            if (current == null || current.getType().isAir()) {
                continue;
            }

            if (current.getMaxStackSize() <= 1) {
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
        Inventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();

        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack current = storage[slot];
            ItemStack stripped = stripWorthDisplay(current);
            if (stripped != current) {
                storage[slot] = stripped;
                inventory.setItem(slot, stripped);
            }
        }

        mergePlayerStorageStacks(player);
    }

    public void clearWorthDisplay(Player player) {
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
        if (item == null || item.getType().isAir()) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        boolean hasTag = container != null && container.has(worthDisplayAppliedKey, PersistentDataType.BYTE);
        List<String> currentLore = meta.getLore();
        List<String> strippedLore = stripExistingWorthLore(readOriginalLore(meta));

        if (!hasTag && (currentLore == null || Objects.equals(currentLore, strippedLore))) {
            return item;
        }

        ItemStack updated = item.clone();
        ItemMeta updatedMeta = updated.getItemMeta();
        if (updatedMeta == null) {
            return item;
        }

        PersistentDataContainer updatedContainer = updatedMeta.getPersistentDataContainer();
        if (updatedContainer != null) {
            updatedContainer.remove(worthDisplayAppliedKey);
            updatedContainer.remove(worthDisplayOriginalLoreKey);
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

    public String prettifyMaterial(Material material) {
        if (material == null) {
            return "unknown";
        }

        String[] tokens = material.name().toLowerCase(Locale.US).split("_");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
        }
        return builder.toString();
    }

    private WorthResult resolveWorth(ItemStack item, int depth, boolean allowNestedExpansion, Set<Integer> visitedContainers) {
        if (item == null || item.getType().isAir()) {
            return WorthResult.unsellable();
        }
        if (isBlockedItem(item)) {
            return WorthResult.unsellable();
        }

        DirectWorthData directWorth = resolveDirectWorth(item);
        if (isContainerWorthEnabled()
                && allowNestedExpansion
                && isSupportedContainer(item)
                && depth < getMaxContainerDepth()) {
            double contentsWorth = resolveContainerContentsWorth(item, depth + 1, allowNestedExpansion, visitedContainers);
            if (contentsWorth > 0) {
                double baseWorth = directWorth == null ? 0 : directWorth.worth();
                double totalUnitWorth = contentsWorth + (shouldIncludeContainerBasePrice() ? baseWorth : 0);
                if (totalUnitWorth > 0) {
                    return new WorthResult(
                            true,
                            true,
                            totalUnitWorth,
                            totalUnitWorth * item.getAmount(),
                            baseWorth,
                            contentsWorth,
                            "container",
                            item.getType().name(),
                            directWorth == null ? "" : directWorth.categoryKey()
                    );
                }
            }
        }

        if (directWorth == null) {
            return WorthResult.unsellable();
        }

        return new WorthResult(
                true,
                false,
                directWorth.worth(),
                directWorth.worth() * item.getAmount(),
                directWorth.worth(),
                0,
                directWorth.resolutionType(),
                directWorth.sourceKey(),
                directWorth.categoryKey()
        );
    }

    private double resolveContainerContentsWorth(
            ItemStack item,
            int depth,
            boolean allowNestedExpansion,
            Set<Integer> visitedContainers
    ) {
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta)) {
            return 0;
        }
        BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;

        BlockState blockState = blockStateMeta.getBlockState();
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
                if (content == null || content.getType().isAir()) {
                    continue;
                }

                WorthResult contentWorth = resolveWorth(
                        content,
                        depth,
                        allowNestedExpansion && allowNestedContainers(),
                        visitedContainers
                );
                if (contentWorth.sellable()) {
                    total += contentWorth.totalWorth();
                }
            }
            return total;
        } finally {
            visitedContainers.remove(containerIdentity);
        }
    }

    private void collectSellWorthEntries(
            ItemStack item,
            int amountMultiplier,
            int depth,
            boolean allowNestedExpansion,
            Set<Integer> visitedContainers,
            List<SellWorthEntry> entries
    ) {
        if (item == null || item.getType().isAir() || amountMultiplier <= 0) {
            return;
        }
        if (isBlockedItem(item)) {
            return;
        }

        boolean container = isSupportedContainer(item);
        DirectWorthData directWorth = resolveDirectWorth(item);
        if (directWorth != null
                && (!container || !isContainerWorthEnabled() || shouldIncludeContainerBasePrice())) {
            SellCategory category = resolveSellCategory(directWorth, item);
            if (category != null && directWorth.worth() > 0) {
                int amount = multiplyAmount(item.getAmount(), amountMultiplier);
                entries.add(new SellWorthEntry(
                        item.getType(),
                        amount,
                        directWorth.worth() * item.getAmount() * amountMultiplier,
                        category
                ));
            }
        }

        if (!isContainerWorthEnabled()
                || !container
                || !allowNestedExpansion
                || depth >= getMaxContainerDepth()) {
            return;
        }

        collectContainerSellWorthEntries(
                item,
                multiplyAmount(item.getAmount(), amountMultiplier),
                depth + 1,
                allowNestedExpansion && allowNestedContainers(),
                visitedContainers,
                entries
        );
    }

    private void collectContainerSellWorthEntries(
            ItemStack item,
            int amountMultiplier,
            int depth,
            boolean allowNestedExpansion,
            Set<Integer> visitedContainers,
            List<SellWorthEntry> entries
    ) {
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta)) {
            return;
        }
        BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;

        BlockState blockState = blockStateMeta.getBlockState();
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
        return Objects.hash(item.getType(), meta == null ? null : meta.getAsString());
    }

    private boolean isSupportedContainer(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta) {
            return ((BlockStateMeta) meta).getBlockState() instanceof Container;
        }
        return false;
    }

    private boolean isBlockedItem(ItemStack item) {
        return item != null && isBlockedMaterial(item.getType());
    }

    private boolean isBlockedMaterial(Material material) {
        if (material == null || material.isAir()) {
            return false;
        }

        if (!blockedMaterialsLoaded) {
            blockedMaterialsCache.clear();
            List<String> blockItems = plugin.getConfigManager().getWorth().getStringList("BLOCK-ITEMS");
            if (blockItems != null) {
                for (String rawMaterial : blockItems) {
                    if (rawMaterial == null || rawMaterial.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        Material blocked = matchMaterial(rawMaterial.trim());
                        if (blocked != null) {
                            blockedMaterialsCache.add(blocked);
                        }
                    } catch (IllegalArgumentException ignored) {}
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
        if (item == null || item.getType().isAir()) {
            return null;
        }
        if (isBlockedItem(item)) {
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
        if (item == null || item.getType().isAir()) {
            return null;
        }
        if (isBlockedItem(item)) {
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
        if (baseData == null || item == null) {
            return baseData;
        }

        if (item.getType() == Material.ENCHANTED_BOOK) {
            return baseData;
        }

        java.util.Map<Enchantment, Integer> enchantments = item.getEnchantments();
        if (enchantments.isEmpty()) {
            return baseData;
        }

        double extraWorth = 0;
        for (java.util.Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            extraWorth += getEnchantmentWorth(entry.getKey(), entry.getValue());
        }

        if (extraWorth > 0) {
            return new DirectWorthData(
                    baseData.worth() + extraWorth,
                    baseData.sourceKey(),
                    baseData.categoryKey(),
                    baseData.resolutionType() + "_ENCHANTED"
            );
        }

        return baseData;
    }

    private double getEnchantmentWorth(Enchantment enchantment, int level) {
        String enchantmentKey = enchantment.getKey().getKey()
                .toUpperCase(Locale.US)
                .replace('-', '_');
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
            if (section.isConfigurationSection(key)) {
                double worth = findEnchantmentWorthRecursively(section.getConfigurationSection(key), lookupKey);
                if (worth > 0) {
                    return worth;
                }
            }
        }
        return 0;
    }

    private DirectWorthData findWorthRecursively(ConfigurationSection section, ItemStack item, String categoryKey) {
        if (section == null || item == null || item.getType().isAir()) {
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

            DirectWorthData nestedWorth = findWorthRecursively(
                    section.getConfigurationSection(key),
                    item,
                    categoryKey
            );
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
            return new DirectWorthData(
                    section.getDouble(materialKey, -1),
                    materialKey,
                    categoryKey,
                    "DIRECT"
            );
        }
        return null;
    }

        private DirectWorthData resolveSpecificItemWorth(ConfigurationSection section, ItemStack item, String categoryKey) {
        if (item.getType() == Material.ENCHANTED_BOOK) {
            return resolveEnchantedBookWorth(section, item, categoryKey);
        }

        if (item.getItemMeta() instanceof PotionMeta) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            PotionType potionType = meta.getBasePotionType();
            String potionKey = potionType == null ? null : potionType.name();
            if (potionKey != null && section.contains(potionKey) && !section.isConfigurationSection(potionKey)) {
                return new DirectWorthData(
                        section.getDouble(potionKey, -1),
                        potionKey,
                        categoryKey,
                        "DIRECT"
                );
            }
        }
        return null;
    }
}
