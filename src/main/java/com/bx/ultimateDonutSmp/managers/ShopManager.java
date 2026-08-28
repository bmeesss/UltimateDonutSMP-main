package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.utils.PermissionUtils;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.amethyst.AmethystToolType;
import com.bx.ultimateDonutSmp.models.AuctionListing;
import com.bx.ultimateDonutSmp.models.EconomyReason;
import com.bx.ultimateDonutSmp.models.EconomyTransactionResult;
import com.bx.ultimateDonutSmp.models.PlayerData;
import com.bx.ultimateDonutSmp.models.SellCategory;
import com.bx.ultimateDonutSmp.models.ShopPreference;
import com.bx.ultimateDonutSmp.storage.ShopPreferenceRepository;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import com.bx.ultimateDonutSmp.utils.ItemSerializationUtils;
import com.bx.ultimateDonutSmp.utils.ItemUtils;
import com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport;
import com.bx.ultimateDonutSmp.utils.NumberUtils;
import com.bx.ultimateDonutSmp.utils.PlayerSettingUtils;
import com.bx.ultimateDonutSmp.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.logging.Level;

public class ShopManager {

    private static final String PRICE_TAG_PREFIX = "[PRICE] ";
    private static final Set<String> CATEGORY_META_KEYS = new java.util.LinkedHashSet<>(java.util.Arrays.asList("MENU-TITLE",  "MENU-SIZE",  "ORDER"));
    private static final Set<String> MENU_META_KEYS = new java.util.LinkedHashSet<>(java.util.Arrays.asList(
            "TITLE", 
            "SIZE", 
            "CURRENCY", 
            "BACK-BUTTON-SLOT", 
            "FIRST-PAGE-SLOT", 
            "PREVIOUS-PAGE-SLOT", 
            "PAGE-INFO-SLOT", 
            "NEXT-PAGE-SLOT", 
            "LAST-PAGE-SLOT", 
            "ITEMS-PER-PAGE", 
            "ITEMS"
    ));
    private static final Set<Material> FISH_CATEGORY_OVERRIDES = resolveMaterials(new String[][]{
            {"COD", "RAW_FISH"},
            {"COOKED_COD", "COOKED_FISH"},
            {"SALMON", "RAW_FISH"},
            {"COOKED_SALMON", "COOKED_FISH"},
            {"TROPICAL_FISH", "RAW_FISH"},
            {"PUFFERFISH", "RAW_FISH"},
            {"COD_BUCKET", null},
            {"SALMON_BUCKET", null},
            {"TROPICAL_FISH_BUCKET", null},
            {"PUFFERFISH_BUCKET", null},
            {"AXOLOTL_BUCKET", null},
            {"TADPOLE_BUCKET", null},
            {"FISHING_ROD", "FISHING_ROD"},
            {"NAME_TAG", "NAME_TAG"},
            {"NAUTILUS_SHELL", null},
            {"LILY_PAD", "WATER_LILY"},
            {"HEART_OF_THE_SEA", null}
    });
    private static final Set<Material> POTION_CATEGORY_OVERRIDES = resolveMaterials(new String[][]{
            {"POTION", "POTION"},
            {"SPLASH_POTION", "SPLASH_POTION"},
            {"LINGERING_POTION", "LINGERING_POTION"},
            {"TIPPED_ARROW", "TIPPED_ARROW"},
            {"BREWING_STAND", "BREWING_STAND_ITEM"},
            {"BLAZE_POWDER", "BLAZE_POWDER"},
            {"BLAZE_ROD", "BLAZE_ROD"},
            {"FERMENTED_SPIDER_EYE", "FERMENTED_SPIDER_EYE"},
            {"GLASS_BOTTLE", "GLASS_BOTTLE"},
            {"GLISTERING_MELON_SLICE", "SPECKLED_MELON"},
            {"GHAST_TEAR", "GHAST_TEAR"},
            {"MAGMA_CREAM", "MAGMA_CREAM"},
            {"RABBIT_FOOT", "RABBIT_FOOT"},
            {"SPIDER_EYE", "SPIDER_EYE"},
            {"SUGAR", "SUGAR"},
            {"GOLDEN_CARROT", "GOLDEN_CARROT"},
            {"PHANTOM_MEMBRANE", null}
    });
    private static final int MAX_MULTIPLIER_BAR_SEGMENTS = 10;

    public enum Currency { MONEY, SHARD }

    public enum PurchaseFailureReason {
        INVALID_ITEM,
        INVALID_QUANTITY,
        NO_PLAYER_DATA,
        NO_PERMISSION,
        NO_MONEY,
        NO_SHARDS,
        INVENTORY_FULL,
        REWARD_FAILED
    }

public final class ShopCategory {
    private final String key;
    private final String menuSection;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final int slot;

    public ShopCategory(String key, String menuSection, Material material, String displayName, List<String> lore, int slot) {
        this.key = key;
        this.menuSection = menuSection;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.slot = slot;
    }

    public String key() { return key; }
    public String menuSection() { return menuSection; }
    public Material material() { return material; }
    public String displayName() { return displayName; }
    public List<String> lore() { return lore; }
    public int slot() { return slot; }

    @Override public String toString() {
        return "ShopCategory[key=+key, menuSection=+menuSection, material=+material, displayName=+displayName, lore=+lore, slot=+slot]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShopCategory that = (ShopCategory) o;
        return java.util.Objects.equals(key, that.key) && java.util.Objects.equals(menuSection, that.menuSection) && java.util.Objects.equals(material, that.material) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(lore, that.lore) && java.util.Objects.equals(slot, that.slot);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(key, menuSection, material, displayName, lore, slot);
    }
}

public final class ShopRestriction {
    private final int minQuantity;
    private final int maxQuantity;
    private final int defaultQuantity;
    private final boolean hideQuantityButtons;

    public ShopRestriction(int minQuantity, int maxQuantity, int defaultQuantity, boolean hideQuantityButtons) {
            minQuantity = Math.max(1, minQuantity);
            maxQuantity = Math.max(minQuantity, maxQuantity);
            defaultQuantity = Math.max(minQuantity, Math.min(maxQuantity, defaultQuantity));
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.defaultQuantity = defaultQuantity;
        this.hideQuantityButtons = hideQuantityButtons;
    }

    public int minQuantity() { return minQuantity; }
    public int maxQuantity() { return maxQuantity; }
    public int defaultQuantity() { return defaultQuantity; }
    public boolean hideQuantityButtons() { return hideQuantityButtons; }




        public int clamp(int value) {
            return Math.max(minQuantity, Math.min(maxQuantity, value));
        }

        public boolean adjustable() {
            return !hideQuantityButtons && maxQuantity > minQuantity;
        }

    @Override public String toString() {
        return "ShopRestriction[minQuantity=+minQuantity, maxQuantity=+maxQuantity, defaultQuantity=+defaultQuantity, hideQuantityButtons=+hideQuantityButtons]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShopRestriction that = (ShopRestriction) o;
        return java.util.Objects.equals(minQuantity, that.minQuantity) && java.util.Objects.equals(maxQuantity, that.maxQuantity) && java.util.Objects.equals(defaultQuantity, that.defaultQuantity) && java.util.Objects.equals(hideQuantityButtons, that.hideQuantityButtons);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(minQuantity, maxQuantity, defaultQuantity, hideQuantityButtons);
    }
}

public final class SellProgressInfo {
    private final SellCategory category;
    private final double earned;
    private final int completedLevels;
    private final double currentMultiplier;
    private final String nextMultiplierDisplay;
    private final double previousGoal;
    private final double nextGoal;
    private final int percentage;
    private final String progressBar;
    private final boolean maxed;

    public SellProgressInfo(SellCategory category, double earned, int completedLevels, double currentMultiplier, String nextMultiplierDisplay, double previousGoal, double nextGoal, int percentage, String progressBar, boolean maxed) {
        this.category = category;
        this.earned = earned;
        this.completedLevels = completedLevels;
        this.currentMultiplier = currentMultiplier;
        this.nextMultiplierDisplay = nextMultiplierDisplay;
        this.previousGoal = previousGoal;
        this.nextGoal = nextGoal;
        this.percentage = percentage;
        this.progressBar = progressBar;
        this.maxed = maxed;
    }

    public SellCategory category() { return category; }
    public double earned() { return earned; }
    public int completedLevels() { return completedLevels; }
    public double currentMultiplier() { return currentMultiplier; }
    public String nextMultiplierDisplay() { return nextMultiplierDisplay; }
    public double previousGoal() { return previousGoal; }
    public double nextGoal() { return nextGoal; }
    public int percentage() { return percentage; }
    public String progressBar() { return progressBar; }
    public boolean maxed() { return maxed; }

    @Override public String toString() {
        return "SellProgressInfo[category=+category, earned=+earned, completedLevels=+completedLevels, currentMultiplier=+currentMultiplier, nextMultiplierDisplay=+nextMultiplierDisplay, previousGoal=+previousGoal, nextGoal=+nextGoal, percentage=+percentage, progressBar=+progressBar, maxed=+maxed]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SellProgressInfo that = (SellProgressInfo) o;
        return java.util.Objects.equals(category, that.category) && java.util.Objects.equals(earned, that.earned) && java.util.Objects.equals(completedLevels, that.completedLevels) && java.util.Objects.equals(currentMultiplier, that.currentMultiplier) && java.util.Objects.equals(nextMultiplierDisplay, that.nextMultiplierDisplay) && java.util.Objects.equals(previousGoal, that.previousGoal) && java.util.Objects.equals(nextGoal, that.nextGoal) && java.util.Objects.equals(percentage, that.percentage) && java.util.Objects.equals(progressBar, that.progressBar) && java.util.Objects.equals(maxed, that.maxed);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(category, earned, completedLevels, currentMultiplier, nextMultiplierDisplay, previousGoal, nextGoal, percentage, progressBar, maxed);
    }
}

    public enum SellStatus {
        SUCCESS,
        NO_SELLABLE_ITEMS,
        TRANSACTION_FAILED
    }

public final class SellResult {
    private final SellStatus status;
    private final double totalPayout;
    private final Set<SellCategory> leveledUpCategories;

    public SellResult(SellStatus status, double totalPayout, Set<SellCategory> leveledUpCategories) {
        this.status = status;
        this.totalPayout = totalPayout;
        this.leveledUpCategories = leveledUpCategories;
    }

    public SellStatus status() { return status; }
    public double totalPayout() { return totalPayout; }
    public Set<SellCategory> leveledUpCategories() { return leveledUpCategories; }


        public boolean hasSales() {
            return status == SellStatus.SUCCESS;
        }

        public boolean transactionFailed() {
            return status == SellStatus.TRANSACTION_FAILED;
        }

    @Override public String toString() {
        return "SellResult[status=+status, totalPayout=+totalPayout, leveledUpCategories=+leveledUpCategories]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SellResult that = (SellResult) o;
        return java.util.Objects.equals(status, that.status) && java.util.Objects.equals(totalPayout, that.totalPayout) && java.util.Objects.equals(leveledUpCategories, that.leveledUpCategories);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(status, totalPayout, leveledUpCategories);
    }
}

public final class PendingSellHistory {
    private final Material material;
    private final int amount;
    private final double payout;

    public PendingSellHistory(Material material, int amount, double payout) {
        this.material = material;
        this.amount = amount;
        this.payout = payout;
    }

    public Material material() { return material; }
    public int amount() { return amount; }
    public double payout() { return payout; }

    @Override public String toString() {
        return "PendingSellHistory[material=+material, amount=+amount, payout=+payout]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PendingSellHistory that = (PendingSellHistory) o;
        return java.util.Objects.equals(material, that.material) && java.util.Objects.equals(amount, that.amount) && java.util.Objects.equals(payout, that.payout);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(material, amount, payout);
    }
}

    private static final class PendingSale {
        private final Map<SellCategory, Double> currentProgress = new EnumMap<>(SellCategory.class);
        private final EnumMap<SellCategory, Double> earnedByCategory = new EnumMap<>(SellCategory.class);
        private final List<PendingSellHistory> history = new ArrayList<>();
        private final List<Integer> soldSlots = new ArrayList<>();
        private double totalPayout;
    }

public final class PurchaseResult {
    private final boolean success;
    private final PurchaseFailureReason reason;
    private final ShopItem item;
    private final int quantity;
    private final double totalPrice;
    private final Currency currency;

    public PurchaseResult(boolean success, PurchaseFailureReason reason, ShopItem item, int quantity, double totalPrice, Currency currency) {
        this.success = success;
        this.reason = reason;
        this.item = item;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.currency = currency;
    }

    public boolean success() { return success; }
    public PurchaseFailureReason reason() { return reason; }
    public ShopItem item() { return item; }
    public int quantity() { return quantity; }
    public double totalPrice() { return totalPrice; }
    public Currency currency() { return currency; }


        public boolean insufficientMoney() {
            return reason == PurchaseFailureReason.NO_MONEY;
        }

        public boolean insufficientShards() {
            return reason == PurchaseFailureReason.NO_SHARDS;
        }

        public boolean inventoryFull() {
            return reason == PurchaseFailureReason.INVENTORY_FULL;
        }

    @Override public String toString() {
        return "PurchaseResult[success=+success, reason=+reason, item=+item, quantity=+quantity, totalPrice=+totalPrice, currency=+currency]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurchaseResult that = (PurchaseResult) o;
        return java.util.Objects.equals(success, that.success) && java.util.Objects.equals(reason, that.reason) && java.util.Objects.equals(item, that.item) && java.util.Objects.equals(quantity, that.quantity) && java.util.Objects.equals(totalPrice, that.totalPrice) && java.util.Objects.equals(currency, that.currency);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(success, reason, item, quantity, totalPrice, currency);
    }
}

public static final class ManagedSpawnerReward {
    private final String typeKey;

    public ManagedSpawnerReward(String typeKey) {
        this.typeKey = typeKey;
    }

    public String typeKey() { return typeKey; }

    @Override public String toString() {
        return "ManagedSpawnerReward[typeKey=+typeKey]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManagedSpawnerReward that = (ManagedSpawnerReward) o;
        return java.util.Objects.equals(typeKey, that.typeKey);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(typeKey);
    }
}

public static final class RewardDeliveryResult {
    private final boolean success;
    private final String message;
    private final Throwable throwable;

    public RewardDeliveryResult(boolean success, String message, Throwable throwable) {
        this.success = success;
        this.message = message;
        this.throwable = throwable;
    }

    public boolean success() { return success; }
    public String message() { return message; }
    public Throwable throwable() { return throwable; }


        static RewardDeliveryResult ok() {
            return new RewardDeliveryResult(true, "", null);
        }

        static RewardDeliveryResult failure(String message) {
            return new RewardDeliveryResult(false, message == null ? "" : message, null);
        }

        static RewardDeliveryResult failure(String message, Throwable throwable) {
            return new RewardDeliveryResult(false, message == null ? "" : message, throwable);
        }

    @Override public String toString() {
        return "RewardDeliveryResult[success=+success, message=+message, throwable=+throwable]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RewardDeliveryResult that = (RewardDeliveryResult) o;
        return java.util.Objects.equals(success, that.success) && java.util.Objects.equals(message, that.message) && java.util.Objects.equals(throwable, that.throwable);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(success, message, throwable);
    }
}

public static final class ShopItem {
    private final String key;
    private final String menuSection;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final int slot;
    private final double pricePerUnit;
    private final Currency currency;
    private final String command;
    private final boolean giveItem;
    private final String permission;
    private final int minQuantity;
    private final int maxQuantity;
    private final int defaultQuantity;
    private final Boolean hideQuantityButtons;
    private final AmethystToolType amethystToolType;
    private final long amethystDurationSeconds;
    private final List<String> enchantments;
    private final Boolean glint;
    private final String serializedItemData;

    public ShopItem(String key, String menuSection, Material material, String displayName, List<String> lore, int slot, double pricePerUnit, Currency currency, String command, boolean giveItem, String permission, int minQuantity, int maxQuantity, int defaultQuantity, Boolean hideQuantityButtons, AmethystToolType amethystToolType, long amethystDurationSeconds, List<String> enchantments, Boolean glint, String serializedItemData) {
        this.key = key;
        this.menuSection = menuSection;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.slot = slot;
        this.pricePerUnit = pricePerUnit;
        this.currency = currency;
        this.command = command;
        this.giveItem = giveItem;
        this.permission = permission;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.defaultQuantity = defaultQuantity;
        this.hideQuantityButtons = hideQuantityButtons;
        this.amethystToolType = amethystToolType;
        this.amethystDurationSeconds = amethystDurationSeconds;
        this.enchantments = enchantments;
        this.glint = glint;
        this.serializedItemData = serializedItemData;
    }

    public String key() { return key; }
    public String menuSection() { return menuSection; }
    public Material material() { return material; }
    public String displayName() { return displayName; }
    public List<String> lore() { return lore; }
    public int slot() { return slot; }
    public double pricePerUnit() { return pricePerUnit; }
    public Currency currency() { return currency; }
    public String command() { return command; }
    public boolean giveItem() { return giveItem; }
    public String permission() { return permission; }
    public int minQuantity() { return minQuantity; }
    public int maxQuantity() { return maxQuantity; }
    public int defaultQuantity() { return defaultQuantity; }
    public Boolean hideQuantityButtons() { return hideQuantityButtons; }
    public AmethystToolType amethystToolType() { return amethystToolType; }
    public long amethystDurationSeconds() { return amethystDurationSeconds; }
    public List<String> enchantments() { return enchantments; }
    public Boolean glint() { return glint; }
    public String serializedItemData() { return serializedItemData; }
        /** Kept so callers that predate custom item data keep compiling. */
        public ShopItem(
                String key,
                String menuSection,
                Material material,
                String displayName,
                List<String> lore,
                int slot,
                double pricePerUnit,
                Currency currency,
                String command,
                boolean giveItem,
                String permission,
                int minQuantity,
                int maxQuantity,
                int defaultQuantity,
                Boolean hideQuantityButtons,
                AmethystToolType amethystToolType,
                long amethystDurationSeconds,
                List<String> enchantments,
                Boolean glint
        ) {
            this(
                    key, menuSection, material, displayName, lore, slot, pricePerUnit,
                    currency, command, giveItem, permission, minQuantity, maxQuantity,
                    defaultQuantity, hideQuantityButtons, amethystToolType, amethystDurationSeconds,
                    enchantments, glint, null
            );
        }

        public boolean hasCustomItemData() {
            return serializedItemData != null && !serializedItemData.trim().isEmpty();
        }

        public boolean isAmethystToolReward() {
            return amethystToolType != null;
        }

    @Override public String toString() {
        return "ShopItem[key=+key, menuSection=+menuSection, material=+material, displayName=+displayName, lore=+lore, slot=+slot, pricePerUnit=+pricePerUnit, currency=+currency, command=+command, giveItem=+giveItem, permission=+permission, minQuantity=+minQuantity, maxQuantity=+maxQuantity, defaultQuantity=+defaultQuantity, hideQuantityButtons=+hideQuantityButtons, amethystToolType=+amethystToolType, amethystDurationSeconds=+amethystDurationSeconds, enchantments=+enchantments, glint=+glint, serializedItemData=+serializedItemData]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShopItem that = (ShopItem) o;
        return java.util.Objects.equals(key, that.key) && java.util.Objects.equals(menuSection, that.menuSection) && java.util.Objects.equals(material, that.material) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(lore, that.lore) && java.util.Objects.equals(slot, that.slot) && java.util.Objects.equals(pricePerUnit, that.pricePerUnit) && java.util.Objects.equals(currency, that.currency) && java.util.Objects.equals(command, that.command) && java.util.Objects.equals(giveItem, that.giveItem) && java.util.Objects.equals(permission, that.permission) && java.util.Objects.equals(minQuantity, that.minQuantity) && java.util.Objects.equals(maxQuantity, that.maxQuantity) && java.util.Objects.equals(defaultQuantity, that.defaultQuantity) && java.util.Objects.equals(hideQuantityButtons, that.hideQuantityButtons) && java.util.Objects.equals(amethystToolType, that.amethystToolType) && java.util.Objects.equals(amethystDurationSeconds, that.amethystDurationSeconds) && java.util.Objects.equals(enchantments, that.enchantments) && java.util.Objects.equals(glint, that.glint) && java.util.Objects.equals(serializedItemData, that.serializedItemData);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(key, menuSection, material, displayName, lore, slot, pricePerUnit, currency, command, giveItem, permission, minQuantity, maxQuantity, defaultQuantity, hideQuantityButtons, amethystToolType, amethystDurationSeconds, enchantments, glint, serializedItemData);
    }
}

public static final class AuctionQuote {
    private final AuctionListing listing;
    private final double unitPrice;

    public AuctionQuote(AuctionListing listing, double unitPrice) {
        this.listing = listing;
        this.unitPrice = unitPrice;
    }

    public AuctionListing listing() { return listing; }
    public double unitPrice() { return unitPrice; }

    @Override public String toString() {
        return "AuctionQuote[listing=+listing, unitPrice=+unitPrice]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionQuote that = (AuctionQuote) o;
        return java.util.Objects.equals(listing, that.listing) && java.util.Objects.equals(unitPrice, that.unitPrice);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(listing, unitPrice);
    }
}

    private final UltimateDonutSmp plugin;
    private final ShopPreferenceRepository preferenceRepository;
    private final Map<UUID, ShopPreference> preferenceCache = new ConcurrentHashMap<>();
    private final Map<String, ItemStack> customItemCache = new ConcurrentHashMap<>();

    public ShopManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
        this.preferenceRepository = new ShopPreferenceRepository(plugin);
        this.preferenceRepository.initialize().join();
        reload();
    }

    public void reload() {
        customItemCache.clear();
        validateShopConfiguration();
    }

    public void shutdown() {
        preferenceRepository.shutdown();
        preferenceCache.clear();
    }

    public CompletableFuture<ShopPreference> loadPreference(UUID playerId) {
        return preferenceRepository.load(playerId).thenApply(preference -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online != null && online.isOnline()) {
                preferenceCache.put(playerId, preference);
            }
            return preference;
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.WARNING, "Failed to load shop preference for " + playerId, throwable);
            return getPreference(playerId);
        });
    }

    public ShopPreference getPreference(UUID playerId) {
        return preferenceCache.computeIfAbsent(
                playerId,
                ignored -> new ShopPreference(playerId, java.util.Collections.emptySet())
        );
    }

    public boolean areFavoritesEnabled() {
        return plugin.getConfigManager().getShop()
                .getBoolean("SHOP-GUI.FAVORITES.ENABLED", true);
    }

    public boolean toggleFavorite(UUID playerId, ShopItem item) {
        if (!areFavoritesEnabled()) {
            return false;
        }
        String favoriteId = favoriteId(item);
        ShopPreference updated = preferenceCache.compute(playerId, (ignored, current) -> {
            ShopPreference base = current == null
                    ? new ShopPreference(playerId, java.util.Collections.emptySet())
                    : current;
            return base.withFavorite(favoriteId, !base.favorites().contains(favoriteId));
        });
        boolean favorite = updated.favorites().contains(favoriteId);
        preferenceRepository.setFavorite(playerId, favoriteId, favorite).exceptionally(throwable -> {
            plugin.getLogger().log(Level.WARNING, "Failed to save shop favorite for " + playerId, throwable);
            return null;
        });
        return favorite;
    }

    public boolean isFavorite(UUID playerId, ShopItem item) {
        return getPreference(playerId).favorites().contains(favoriteId(item));
    }

    public List<ShopItem> loadFavoriteItems(UUID playerId) {
        Set<String> favorites = getPreference(playerId).favorites();
        if (favorites.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return loadAllItems().stream()
                .filter(item -> favorites.contains(favoriteId(item)))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<ShopItem> loadAllItems() {
        Map<String, ShopItem> items = new java.util.LinkedHashMap<>();
        for (ShopCategory category : loadCategories()) {
            for (ShopItem item : loadMenuItems(category.menuSection())) {
                items.putIfAbsent(favoriteId(item), item);
            }
        }
        return new java.util.ArrayList<>(items.values());
    }

    public String favoriteId(ShopItem item) {
        if (item == null) {
            return "";
        }
        return item.menuSection().trim().toUpperCase(Locale.ROOT)
                + ":"
                + item.key().trim().toUpperCase(Locale.ROOT);
    }

    public AuctionQuote findBestAuctionQuote(Player buyer, ShopItem item) {
        if (buyer == null
                || item == null
                || !shouldDeliverConfiguredItem(item)
                || item.isAmethystToolReward()
                || !isAuctionPriceEnabled()
                || plugin.getAuctionHouseManager() == null
                || !plugin.getAuctionHouseManager().isEnabled()) {
            return null;
        }
        ItemStack desiredItem = createPurchasedItem(item, 1);
        return findBestAuctionQuote(
                plugin.getAuctionHouseManager().getActiveListings(AuctionHouseManager.AuctionSort.NEWEST),
                buyer.getUniqueId(),
                desiredItem,
                System.currentTimeMillis()
        );
    }

    public static AuctionQuote findBestAuctionQuote(
            List<AuctionListing> listings,
            UUID buyerId,
            ItemStack desiredItem,
            long now
    ) {
        return findBestAuctionQuote(listings, buyerId, desiredItem, now, ItemStack::isSimilar);
    }

    public static AuctionQuote findBestAuctionQuote(
            List<AuctionListing> listings,
            UUID buyerId,
            ItemStack desiredItem,
            long now,
            BiPredicate<ItemStack, ItemStack> similarity
    ) {
        if (listings == null || desiredItem == null || isAir(desiredItem.getType())) {
            return null;
        }
        BiPredicate<ItemStack, ItemStack> matcher = similarity == null ? ItemStack::isSimilar : similarity;
        return listings.stream()
                .filter(Objects::nonNull)
                .filter(AuctionListing::active)
                .filter(listing -> listing.expiresAt() > now)
                .filter(listing -> buyerId == null || !buyerId.equals(listing.sellerUuid()))
                .filter(listing -> listing.item() != null
                        && !isAir(listing.item().getType())
                        && listing.item().getAmount() > 0
                        && matcher.test(listing.item(), desiredItem))
                .map(listing -> new AuctionQuote(
                        listing,
                        listing.price() / Math.max(1, listing.item().getAmount())
                ))
                .filter(quote -> Double.isFinite(quote.unitPrice()) && quote.unitPrice() >= 0D)
                .min(Comparator.comparingDouble(AuctionQuote::unitPrice)
                        .thenComparingDouble(quote -> quote.listing().price())
                        .thenComparingLong(quote -> quote.listing().createdAt()))
                .orElse(null);
    }

    private static boolean isAir(Material material) {
        return material == null || material == Material.AIR;
    }

    private static Set<Material> resolveMaterials(String[][] mappings) {
        Set<Material> resolved = new java.util.LinkedHashSet<>();
        for (String[] mapping : mappings) {
            String modern = mapping[0];
            String legacy = mapping[1];
            Material material = resolveMaterial(modern, legacy);
            if (material != null) {
                resolved.add(material);
            }
        }
        return resolved;
    }

    private static Material resolveMaterial(String modernName, String legacyName) {
        Material modern = modernName == null ? null : Material.matchMaterial(modernName);
        if (modern != null) {
            return modern;
        }
        return legacyName == null ? null : Material.matchMaterial(legacyName);
    }

    public boolean isAuctionPriceEnabled() {
        return plugin.getConfigManager().getShop()
                .getBoolean("SHOP-GUI.SHOW-AUCTION-PRICE", true);
    }

    public void cleanupPlayer(UUID playerId) {
        preferenceCache.remove(playerId);
    }

    public List<ShopCategory> loadCategories() {
        ConfigurationSection categoriesSection = plugin.getConfigManager().getShop()
                .getConfigurationSection("CATEGORIES");
        if (categoriesSection == null) {
            return java.util.Collections.emptyList();
        }

        List<String> orderedKeys = new ArrayList<>();
        for (String configuredKey : categoriesSection.getStringList("ORDER")) {
            if (configuredKey == null || configuredKey.trim().isEmpty()) {
                continue;
            }

            String normalized = configuredKey.trim().toUpperCase(Locale.US);
            if (!orderedKeys.contains(normalized)) {
                orderedKeys.add(normalized);
            }
        }

        for (String key : categoriesSection.getKeys(false)) {
            String normalized = key.toUpperCase(Locale.US);
            if (CATEGORY_META_KEYS.contains(normalized) || orderedKeys.contains(normalized)) {
                continue;
            }
            orderedKeys.add(normalized);
        }

        List<ShopCategory> categories = new ArrayList<>();
        for (String key : orderedKeys) {
            ConfigurationSection section = categoriesSection.getConfigurationSection(key);
            if (section == null || !section.getBoolean("ENABLED", true)) {
                continue;
            }

            categories.add(new ShopCategory(
                    key,
                    normalizeMenuSection(section.getString("OPEN-MENU"), key),
                    ItemUtils.parseMaterial(section.getString("MATERIAL", "STONE"), Material.STONE),
                    section.getString("DISPLAY-NAME", key),
                    section.getStringList("LORE"),
                    section.getInt("SLOT", 0)
            ));
        }

        return new java.util.ArrayList<>(categories);
    }

    public List<ShopItem> loadMenuItems(String menuSection) {
        List<ShopItem> items = new ArrayList<>();
        ConfigurationSection menuConfig = plugin.getConfigManager().getShop()
                .getConfigurationSection(menuSection);
        if (menuConfig == null) {
            return items;
        }

        ConfigurationSection itemsSection = menuConfig.getConfigurationSection("ITEMS");
        ConfigurationSection sourceSection = itemsSection != null ? itemsSection : menuConfig;
        Currency defaultCurrency = parseCurrency(menuConfig.getString("CURRENCY", "MONEY"));

        for (String key : sourceSection.getKeys(false)) {
            if (sourceSection == menuConfig && MENU_META_KEYS.contains(key.toUpperCase(Locale.US))) {
                continue;
            }

            ConfigurationSection itemSec = sourceSection.getConfigurationSection(key);
            if (itemSec == null || !itemSec.getBoolean("ENABLED", true) || !itemSec.contains("MATERIAL")) {
                continue;
            }

            Material material = ItemUtils.parseMaterial(itemSec.getString("MATERIAL", "STONE"));
            if (material == null) {
                // A shop item whose material cannot be resolved must not be sold as stone.
                plugin.getLogger().warning("[Shop] Shop item '" + key + "' in " + menuSection
                        + " has unresolvable MATERIAL '" + itemSec.getString("MATERIAL")
                        + "'; the item was skipped.");
                continue;
            }

            List<String> enchantments = new ArrayList<>();
            if (itemSec.isConfigurationSection("ENCHANTMENTS")) {
                ConfigurationSection enchSec = itemSec.getConfigurationSection("ENCHANTMENTS");
                if (enchSec != null) {
                    for (String enchKey : enchSec.getKeys(false)) {
                        int level = enchSec.getInt(enchKey, 1);
                        enchantments.add(enchKey + ":" + level);
                    }
                }
            } else if (itemSec.isList("ENCHANTMENTS")) {
                enchantments.addAll(itemSec.getStringList("ENCHANTMENTS"));
            }

            Boolean glint = itemSec.contains("GLINT") ? itemSec.getBoolean("GLINT") : null;

            items.add(new ShopItem(
                    key,
                    menuSection,
                    material,
                    itemSec.getString("DISPLAY-NAME", key),
                    itemSec.getStringList("LORE"),
                    itemSec.getInt("SLOT", 0),
                    itemSec.getDouble("PRICE-PER-UNIT", 0),
                    parseCurrency(itemSec.getString("CURRENCY", defaultCurrency.name())),
                    itemSec.getString("COMMAND", ""),
                    itemSec.getBoolean("GIVE-ITEM", true),
                    itemSec.getString("PERMISSION", ""),
                    itemSec.contains("MIN-QUANTITY") ? itemSec.getInt("MIN-QUANTITY") : -1,
                    itemSec.contains("MAX-QUANTITY") ? itemSec.getInt("MAX-QUANTITY") : -1,
                    itemSec.contains("DEFAULT-QUANTITY") ? itemSec.getInt("DEFAULT-QUANTITY") : -1,
                    itemSec.contains("HIDE-QUANTITY-BUTTONS") ? itemSec.getBoolean("HIDE-QUANTITY-BUTTONS") : null,
                    null,
                    -1L,
                    enchantments,
                    glint,
                    itemSec.getString("ITEM-DATA")
            ));
        }

        if ("SHARD-MENU".equalsIgnoreCase(menuSection)) {
            items.addAll(loadAmethystShardShopItems(menuSection));
        }

        items.sort(Comparator.comparingInt(ShopItem::slot).thenComparing(ShopItem::key, String.CASE_INSENSITIVE_ORDER));
        return new java.util.ArrayList<>(items);
    }

    private List<ShopItem> loadAmethystShardShopItems(String menuSection) {
        if (plugin.getAmethystToolsManager() == null || !plugin.getAmethystToolsManager().isEnabled()) {
            return java.util.Collections.emptyList();
        }

        List<ShopItem> items = new ArrayList<>();
        for (AmethystToolType type : AmethystToolType.values()) {
            ConfigurationSection toolSection = plugin.getAmethystToolsManager().getToolSection(type);
            ConfigurationSection shopSection = toolSection == null
                    ? null
                    : toolSection.getConfigurationSection("SHARD-SHOP");
            if (toolSection == null || shopSection == null || !shopSection.getBoolean("ENABLED", false)) {
                continue;
            }

            long configuredDuration = shopSection.getLong(
                    "DURATION",
                    toolSection.getLong("DURATION", 86400L)
            );
            long duration = Math.max(1L, configuredDuration);
            double price = shopSection.getDouble("PRICE-PER-UNIT", 0D);
            if (!Double.isFinite(price) || price <= 0D) {
                continue;
            }
            List<String> lore = toolSection.getStringList("LORE").stream()
                    .map(line -> line.replace("{time}", NumberUtils.formatTimeLong(duration)))
                    .collect(java.util.stream.Collectors.toList());

            Material toolMaterial = ItemUtils.parseMaterial(toolSection.getString("MATERIAL", "STONE"));
            if (toolMaterial == null) {
                // A broken tool MATERIAL must not put a stone block on sale as an amethyst tool.
                plugin.getLogger().warning("[Shop] Amethyst tool " + type.getConfigKey()
                        + " has unresolvable MATERIAL '" + toolSection.getString("MATERIAL")
                        + "'; the tool was not listed.");
                continue;
            }

            items.add(new ShopItem(
                    "AMETHYST-" + type.getConfigKey(),
                    menuSection,
                    toolMaterial,
                    toolSection.getString("NAME", type.getDisplayName()),
                    lore,
                    shopSection.getInt("SLOT", 0),
                    price,
                    Currency.SHARD,
                    "",
                    true,
                    shopSection.getString("PERMISSION", ""),
                    shopSection.getInt("MIN-QUANTITY", 1),
                    shopSection.getInt("MAX-QUANTITY", 1),
                    shopSection.getInt("DEFAULT-QUANTITY", 1),
                    shopSection.getBoolean("HIDE-QUANTITY-BUTTONS", true),
                    type,
                    duration,
                    java.util.Collections.<String>emptyList(),
                    null,
                    null
            ));
        }
        return items;
    }

    public ShopRestriction getPurchaseRestriction(ShopItem item) {
        ConfigurationSection restrictions = plugin.getConfigManager().getMenus()
                .getConfigurationSection("PURCHASE-SHOP-MENU.RESTRICTIONS");

        int minQuantity = 1;
        int maxQuantity = 64;
        int defaultQuantity = 1;
        boolean hideQuantityButtons = false;

        if (restrictions != null) {
            ConfigurationSection defaultSection = restrictions.getConfigurationSection("DEFAULT");
            if (defaultSection != null) {
                minQuantity = defaultSection.getInt("MIN_QUANTITY", minQuantity);
                maxQuantity = defaultSection.getInt("MAX_QUANTITY", maxQuantity);
                defaultQuantity = defaultSection.getInt("DEFAULT_QUANTITY", defaultQuantity);
                hideQuantityButtons = defaultSection.getBoolean("HIDE_QUANTITY_BUTTONS", hideQuantityButtons);
            }

            if (item != null) {
                ConfigurationSection materialSection = restrictions.getConfigurationSection(item.material().name());
                if (materialSection != null) {
                    minQuantity = materialSection.getInt("MIN_QUANTITY", minQuantity);
                    maxQuantity = materialSection.getInt("MAX_QUANTITY", maxQuantity);
                    defaultQuantity = materialSection.getInt("DEFAULT_QUANTITY", defaultQuantity);
                    hideQuantityButtons = materialSection.getBoolean("HIDE_QUANTITY_BUTTONS", hideQuantityButtons);
                }
            }
        }

        if (item != null) {
            if (item.minQuantity() > 0) {
                minQuantity = item.minQuantity();
            }
            if (item.maxQuantity() > 0) {
                maxQuantity = item.maxQuantity();
            }
            if (item.defaultQuantity() > 0) {
                defaultQuantity = item.defaultQuantity();
            }
            if (item.hideQuantityButtons() != null) {
                hideQuantityButtons = item.hideQuantityButtons();
            }
        }

        return new ShopRestriction(minQuantity, maxQuantity, defaultQuantity, hideQuantityButtons);
    }

    public PurchaseResult previewPurchase(Player player, ShopItem item, int amount) {
        return validatePurchase(player, item, amount);
    }

    /**
     * Process a shop purchase and return a rich result for menu feedback.
     */
    public PurchaseResult purchase(Player player, ShopItem item, int amount) {
        PurchaseResult preview = validatePurchase(player, item, amount);
        if (!preview.success()) {
            return preview;
        }

        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) {
            return failPurchase(item, amount, preview.totalPrice(), PurchaseFailureReason.NO_PLAYER_DATA);
        }

        long shardCost = preview.currency() == Currency.SHARD ? Math.round(preview.totalPrice()) : 0L;
        if (preview.currency() == Currency.SHARD) {
            if (!data.removeShards(shardCost)) {
                return failPurchase(item, amount, preview.totalPrice(), PurchaseFailureReason.NO_SHARDS);
            }
        } else {
            EconomyTransactionResult withdrawResult = plugin.getEconomyManager().withdraw(player, preview.totalPrice(), EconomyReason.SHOP_PURCHASE);
            if (!withdrawResult.success()) {
                return failPurchase(item, amount, preview.totalPrice(), PurchaseFailureReason.NO_MONEY);
            }
        }

        RewardDeliveryResult commandReward = deliverCommandReward(player, item, preview.quantity());
        if (!commandReward.success()) {
            refundPurchase(player, data, preview, shardCost);
            logRewardFailure(player, item, preview, commandReward);
            return failPurchase(item, preview.quantity(), preview.totalPrice(), PurchaseFailureReason.REWARD_FAILED);
        }

        RewardDeliveryResult itemReward = deliverItemReward(player, item, preview.quantity());
        if (!itemReward.success()) {
            refundPurchase(player, data, preview, shardCost);
            logRewardFailure(player, item, preview, itemReward);
            return failPurchase(item, preview.quantity(), preview.totalPrice(), PurchaseFailureReason.REWARD_FAILED);
        }

        if (preview.currency() != Currency.SHARD) {
            data.addMoneySpent(preview.totalPrice());
        }

        String itemName = item.displayName() != null && !item.displayName().trim().isEmpty()
                ? ColorUtils.strip(item.displayName())
                : plugin.getWorthManager().prettifyMaterial(item.material());
        String formattedPrice = preview.currency() == Currency.SHARD
                ? preview.totalPrice() + " Shards"
                : plugin.getCurrencyManager().formatMoney(preview.totalPrice());
        plugin.getPlayerLogsManager().log(
                player.getUniqueId(),
                player.getName(),
                "Shop",
                "SHOP_BUY",
                "Bought " + itemName + " x" + preview.quantity() + " for " + formattedPrice
        );

        return preview;
    }

    private RewardDeliveryResult deliverItemReward(Player player, ShopItem item, int quantity) {
        if (!shouldDeliverConfiguredItem(item)) {
            return RewardDeliveryResult.ok();
        }

        if (item.isAmethystToolReward()) {
            if (!isAmethystRewardAvailable(item)) {
                return RewardDeliveryResult.failure("amethyst tools is disabled or the configured tool is unavailable.");
            }

            List<ItemStack> rewards = new ArrayList<>();
            for (int index = 0; index < quantity; index++) {
                ItemStack reward = plugin.getAmethystToolsManager().createTool(
                        item.amethystToolType(),
                        player.getUniqueId(),
                        item.amethystDurationSeconds()
                );
                if (reward == null) {
                    return RewardDeliveryResult.failure(
                            "failed to create amethyst tool reward: " + item.amethystToolType().name()
                    );
                }
                rewards.add(reward);
            }
            plugin.getWorthManager().stripStorageWorthDisplayForNativePickup(player);
            for (ItemStack reward : rewards) {
                player.getInventory().addItem(reward).values().forEach(left ->
                        player.getWorld().dropItemNaturally(player.getLocation(), left));
            }
        } else {
            plugin.getWorthManager().stripStorageWorthDisplayForNativePickup(player);
            ItemStack stack = createPurchasedItem(item, quantity);
            player.getInventory().addItem(stack).values().forEach(left ->
                    player.getWorld().dropItemNaturally(player.getLocation(), left));
        }

        plugin.getWorthManager().syncWorthDisplay(player);
        player.updateInventory();
        return RewardDeliveryResult.ok();
    }

    private RewardDeliveryResult deliverCommandReward(Player player, ShopItem item, int quantity) {
        if (item == null || item.command() == null || item.command().trim().isEmpty()) {
            return RewardDeliveryResult.ok();
        }

        ManagedSpawnerReward spawnerReward = parseManagedSpawnerReward(item.command());
        if (spawnerReward != null) {
            try {
                SpawnerManager.ActionResult result = plugin.getSpawnerManager().giveSpawner(player, spawnerReward.typeKey(), quantity);
                return result.success()
                        ? RewardDeliveryResult.ok()
                        : RewardDeliveryResult.failure(result.message());
            } catch (RuntimeException exception) {
                return RewardDeliveryResult.failure(
                        "managed spawner reward threw an exception: " + item.command(),
                        exception
                );
            }
        }

        String command = resolveShopCommand(player, item.command(), quantity);
        try {
            boolean dispatched = plugin.getSpigotScheduler().dispatchConsoleCommand(command);
            return dispatched
                    ? RewardDeliveryResult.ok()
                    : RewardDeliveryResult.failure("command returned false: " + command);
        } catch (RuntimeException exception) {
            return RewardDeliveryResult.failure("command threw an exception: " + command, exception);
        }
    }

    private static ManagedSpawnerReward parseManagedSpawnerReward(String command) {
        if (command == null || command.trim().isEmpty()) {
            return null;
        }

        String normalized = command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }

        String[] parts = normalized.split("\\s+");
        if (parts.length != 5
                || !parts[0].equalsIgnoreCase("spawner")
                || !parts[1].equalsIgnoreCase("give")
                || !isPlayerPlaceholder(parts[2])
                || !isAmountPlaceholder(parts[4])) {
            return null;
        }

        return new ManagedSpawnerReward(parts[3]);
    }

    static boolean isManagedSpawnerRewardCommand(String command) {
        return parseManagedSpawnerReward(command) != null;
    }

    static boolean shouldDeliverConfiguredItem(ShopItem item) {
        return item != null
                && item.giveItem()
                && !isManagedSpawnerRewardCommand(item.command());
    }

    private static boolean isPlayerPlaceholder(String token) {
        return token != null
                && (token.equalsIgnoreCase("{username}") || token.equalsIgnoreCase("{player}"));
    }

    private static boolean isAmountPlaceholder(String token) {
        return token != null && token.equalsIgnoreCase("{amount}");
    }

    private String resolveShopCommand(Player player, String command, int quantity) {
        return command
                .replace("{username}", player.getName())
                .replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(quantity));
    }

    private void refundPurchase(Player player, PlayerData data, PurchaseResult purchase, long shardCost) {
        if (purchase.currency() == Currency.SHARD) {
            data.addShards(shardCost);
            return;
        }

        EconomyTransactionResult refundResult = plugin.getEconomyManager().deposit(player, purchase.totalPrice(), EconomyReason.SHOP_REFUND);
        if (!refundResult.success()) {
            plugin.getLogger().warning("[ShopManager] Failed to refund shop purchase for "
                    + player.getName() + " after reward delivery failed.");
        }
    }

    private void logRewardFailure(
            Player player,
            ShopItem item,
            PurchaseResult purchase,
            RewardDeliveryResult result
    ) {
        String message = "[shopmanager] failed to deliver shop reward for "
                + player.getName()
                + " item=" + (item == null ? "unknown" : item.key())
                + " quantity=" + purchase.quantity()
                + " price=" + purchase.totalPrice()
                + " currency=" + purchase.currency()
                + " reason=" + ColorUtils.strip(result.message());
        if (result.throwable() != null) {
            plugin.getLogger().log(Level.WARNING, message, result.throwable());
        } else {
            plugin.getLogger().warning(message);
        }
    }

    private PurchaseResult validatePurchase(Player player, ShopItem item, int amount) {
        if (player == null || item == null) {
            return failPurchase(item, amount, 0, PurchaseFailureReason.INVALID_ITEM);
        }

        ShopRestriction restriction = getPurchaseRestriction(item);
        if (amount < restriction.minQuantity() || amount > restriction.maxQuantity()) {
            return failPurchase(item, amount, item.pricePerUnit() * Math.max(1, amount), PurchaseFailureReason.INVALID_QUANTITY);
        }

        if (item.permission() != null && !item.permission().trim().isEmpty() && !PermissionUtils.has(player, item.permission())) {
            return failPurchase(item, amount, item.pricePerUnit() * amount, PurchaseFailureReason.NO_PERMISSION);
        }

        if (item.pricePerUnit() < 0) {
            return failPurchase(item, amount, 0, PurchaseFailureReason.INVALID_ITEM);
        }

        if (item.isAmethystToolReward() && !isAmethystRewardAvailable(item)) {
            return failPurchase(item, amount, item.pricePerUnit() * amount, PurchaseFailureReason.INVALID_ITEM);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) {
            return failPurchase(item, amount, item.pricePerUnit() * amount, PurchaseFailureReason.NO_PLAYER_DATA);
        }

        double totalPrice = item.pricePerUnit() * amount;
        if (item.currency() == Currency.SHARD) {
            long shardCost = Math.round(totalPrice);
            if (!data.hasShards(shardCost)) {
                return failPurchase(item, amount, totalPrice, PurchaseFailureReason.NO_SHARDS);
            }
        } else if (!plugin.getEconomyManager().has(player, totalPrice)) {
            return failPurchase(item, amount, totalPrice, PurchaseFailureReason.NO_MONEY);
        }

        if (shouldDeliverConfiguredItem(item) && !canFitPurchasedItem(player, item, amount)) {
            return failPurchase(item, amount, totalPrice, PurchaseFailureReason.INVENTORY_FULL);
        }

        return new PurchaseResult(true, null, item, amount, totalPrice, item.currency());
    }

    private PurchaseResult failPurchase(ShopItem item, int quantity, double totalPrice, PurchaseFailureReason reason) {
        return new PurchaseResult(
                false,
                reason,
                item,
                Math.max(0, quantity),
                Math.max(0, totalPrice),
                item == null ? Currency.MONEY : item.currency()
        );
    }

    private Currency parseCurrency(String currencyName) {
        if (currencyName == null || currencyName.trim().isEmpty()) {
            return Currency.MONEY;
        }

        return "SHARD".equalsIgnoreCase(currencyName) || "SHARDS".equalsIgnoreCase(currencyName)
                ? Currency.SHARD
                : Currency.MONEY;
    }

    private String normalizeMenuSection(String rawMenuSection, String fallbackKey) {
        String raw = rawMenuSection == null || rawMenuSection.trim().isEmpty()
                ? fallbackKey + "-MENU"
                : rawMenuSection;
        return raw.replace("{", "").replace("}", "").trim().toUpperCase(Locale.US);
    }

    private ItemStack createPurchasedItem(ShopItem item, int amount) {
        ItemStack custom = createCustomItem(item);
        if (custom != null) {
            custom.setAmount(Math.max(1, amount));
            return custom;
        }

        ItemStack stack = new ItemStack(item.material(), Math.max(1, amount));
        if (item.enchantments() != null && !item.enchantments().isEmpty()) {
            ItemUtils.addEnchantments(stack, item.enchantments());
        }
        if (item.glint() != null) {
            ItemUtils.setGlint(stack, item.glint());
        }
        return stack;
    }

    /**
     * The exact item an admin stored through the shop editor, or null when the entry is a plain
     * MATERIAL one. Menus use this so the icon matches what the buyer actually receives.
     */
    public ItemStack createCustomItem(ShopItem item) {
        if (item == null || !item.hasCustomItemData()) {
            return null;
        }

        ItemStack cached = customItemCache.get(item.serializedItemData());
        if (cached != null) {
            return cached.clone();
        }

        try {
            ItemStack stored = ItemSerializationUtils.deserialize(item.serializedItemData());
            if (stored == null || isAir(stored.getType())) {
                return null;
            }
            customItemCache.put(item.serializedItemData(), stored.clone());
            return stored;
        } catch (IOException | ClassNotFoundException | IllegalArgumentException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to read ITEM-DATA for shop item " + item.key() + " in " + item.menuSection(), exception);
            return null;
        }
    }

    // ── Shop editor ─────────────────────────────────────────────────────────────

    /** Outcome of an edit made through the shop editor menu. */
public final class EditResult {
    private final boolean success;
    private final String message;

    public EditResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean success() { return success; }
    public String message() { return message; }

    @Override public String toString() {
        return "EditResult[success=+success, message=+message]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EditResult that = (EditResult) o;
        return java.util.Objects.equals(success, that.success) && java.util.Objects.equals(message, that.message);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(success, message);
    }
}

    /** An item with its shop price, once a {@code [PRICE] <amount>} rename has been read off it. */
public final class PricedItem {
    private final ItemStack item;
    private final Double price;

    public PricedItem(ItemStack item, Double price) {
        this.item = item;
        this.price = price;
    }

    public ItemStack item() { return item; }
    public Double price() { return price; }

    @Override public String toString() {
        return "PricedItem[item=+item, price=+price]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PricedItem that = (PricedItem) o;
        return java.util.Objects.equals(item, that.item) && java.util.Objects.equals(price, that.price);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(item, price);
    }
}

    /**
     * Reads a {@code [PRICE] <amount>} rename off an item and hands back the item with that rename
     * removed, so the price tag never ends up baked into what the buyer receives. Items without the
     * tag come back untouched and with a null price.
     */
    public PricedItem readPriceTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return new PricedItem(item, null);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return new PricedItem(item, null);
        }

        Double parsed = parsePriceTag(ColorUtils.strip(meta.getDisplayName()));
        if (parsed == null) {
            return new PricedItem(item, null);
        }

        ItemStack cleaned = item.clone();
        ItemMeta cleanedMeta = cleaned.getItemMeta();
        if (cleanedMeta != null) {
            cleanedMeta.setDisplayName(null);
            cleaned.setItemMeta(cleanedMeta);
        }
        return new PricedItem(cleaned, parsed);
    }

    /**
     * Pulls the amount out of a {@code [PRICE] 250} rename, or null when the name is not a price tag
     * or the amount will not do as a price.
     */
    public static Double parsePriceTag(String displayName) {
        if (displayName == null) {
            return null;
        }

        String trimmed = displayName.trim();
        if (!trimmed.toUpperCase(Locale.US).startsWith(PRICE_TAG_PREFIX)) {
            return null;
        }

        double parsed;
        try {
            parsed = Double.parseDouble(trimmed.substring(PRICE_TAG_PREFIX.length()).trim().replace(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }

        return parsed > 0 && Double.isFinite(parsed) ? parsed : null;
    }

    /** Menu sections an admin can open in the editor, in the order the categories menu shows them. */
    public List<String> getEditableMenuSections() {
        List<String> sections = new ArrayList<>();
        for (ShopCategory category : loadCategories()) {
            if (!sections.contains(category.menuSection())) {
                sections.add(category.menuSection());
            }
        }
        return new java.util.ArrayList<>(sections);
    }

    /** Turns what an admin typed ("end", "end-menu", "{end-menu}") into a real shop.yml section. */
    public String resolveMenuSection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String normalized = input.replace("{", "").replace("}", "").trim().toUpperCase(Locale.US);
        List<String> sections = getEditableMenuSections();
        if (sections.contains(normalized)) {
            return normalized;
        }

        String suffixed = normalized.endsWith("-MENU") ? normalized : normalized + "-MENU";
        return sections.contains(suffixed) ? suffixed : null;
    }

    public int getMenuSize(String menuSection) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getShop()
                .getConfigurationSection(menuSection);
        int configured = menuConfig == null ? 27 : menuConfig.getInt("SIZE", 27);
        int bounded = Math.max(9, Math.min(54, configured));
        return bounded % 9 == 0 ? bounded : ((bounded / 9) + 1) * 9;
    }

    /** Where a shop menu puts its back button, which the editor reuses for its close button. */
    public int getBackButtonSlot(String menuSection) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getShop()
                .getConfigurationSection(menuSection);
        int size = getMenuSize(menuSection);
        return menuConfig == null ? size - 9 : menuConfig.getInt("BACK-BUTTON-SLOT", size - 9);
    }

    /**
     * Slots a shop menu keeps for its own back and paging buttons. An item parked on one of these is
     * what makes ShopMenu give up on configured slots and fall back to paging, so the editor refuses
     * to place there.
     */
    public Set<Integer> getReservedSlots(String menuSection) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getShop()
                .getConfigurationSection(menuSection);
        int size = getMenuSize(menuSection);
        if (menuConfig == null) {
            return java.util.Collections.emptySet();
        }

        Set<Integer> reserved = new HashSet<>();
        reserved.add(getBackButtonSlot(menuSection));
        reserved.add(menuConfig.getInt("FIRST-PAGE-SLOT", size - 8));
        reserved.add(menuConfig.getInt("PREVIOUS-PAGE-SLOT", size - 7));
        reserved.add(menuConfig.getInt("PAGE-INFO-SLOT", size - 5));
        reserved.add(menuConfig.getInt("NEXT-PAGE-SLOT", size - 3));
        reserved.add(menuConfig.getInt("LAST-PAGE-SLOT", size - 2));
        return new java.util.HashSet<>(reserved);
    }

    /**
     * Stores {@code item} in {@code slot} of {@code menuSection}, replacing whatever was there. The
     * whole item is written as ITEM-DATA so enchantments and every other bit of item data survive the
     * round trip, with MATERIAL and friends written alongside so the entry stays readable in shop.yml.
     */
    public EditResult upsertMenuItem(String menuSection, int slot, ItemStack item, Double price) {
        if (item == null || isAir(item.getType())) {
            return new EditResult(false, "&cSelect an item from your inventory first.");
        }

        ConfigurationSection menuConfig = plugin.getConfigManager().getOriginalShop()
                .getConfigurationSection(menuSection);
        if (menuConfig == null) {
            return new EditResult(false, "&cThat shop menu no longer exists.");
        }

        if (slot < 0 || slot >= getMenuSize(menuSection)) {
            return new EditResult(false, "&cThat slot is outside this shop menu.");
        }

        if (getReservedSlots(menuSection).contains(slot)) {
            return new EditResult(false, "&cThat slot belongs to the menu buttons. Pick another one.");
        }

        ItemStack storedItem = item.clone();
        storedItem.setAmount(1);

        CrashProtectionManager.ValidationResult safetyResult = plugin.getCrashProtectionManager()
                .validateForStorage(storedItem, CrashProtectionManager.Context.SHOP);
        if (!safetyResult.allowed()) {
            plugin.getCrashProtectionManager().logBlockedItem(
                    "shop " + menuSection + " slot " + slot,
                    storedItem,
                    CrashProtectionManager.Context.SHOP,
                    safetyResult
            );
            return new EditResult(false, plugin.getConfigManager().getMessageOrDefault(
                    "CRASH_PROTECTION.ITEM_BLOCKED",
                    "&cThat item cannot be used here because its data looks unsafe. &7context: &f{context}&7. reason: &f{reason}",
                    "{context}", CrashProtectionManager.Context.SHOP.displayName(),
                    "{reason}", safetyResult.reason()
            ));
        }

        double resolvedPrice = price != null ? price : getWorth(storedItem);
        if (resolvedPrice <= 0) {
            return new EditResult(false, "&cThat item has no worth entry, so it needs a price. "
                    + "Rename it to &f[PRICE] 250&c and place it again.");
        }

        String serializedItemData;
        try {
            serializedItemData = ItemSerializationUtils.serialize(storedItem);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to serialize shop item for " + menuSection + " slot " + slot, exception);
            return new EditResult(false, "&cFailed to store that item in the shop.");
        }

        ConfigurationSection itemsSection = menuConfig.getConfigurationSection("ITEMS");
        String basePath = menuSection + (itemsSection != null ? ".ITEMS." : ".");
        String existingKey = findItemKeyBySlot(menuSection, slot);
        boolean editing = existingKey != null;
        String itemKey = editing ? existingKey : generateItemKey(menuSection, storedItem.getType(), slot);
        String path = basePath + itemKey;

        FileConfiguration shopConfig = plugin.getConfigManager().getOriginalShop();
        ItemMeta meta = storedItem.getItemMeta();
        String displayName = editorDisplayName(meta, storedItem.getType());

        // configName preserves a coloured pane's legacy data value in the written Material name
        // (the flattened 1.13+ spelling a modern build wrote); non-pane items still store
        // getType().name(), and the authoritative round trip stays ITEM-DATA.
        shopConfig.set(path + ".MATERIAL", LegacyMaterialSupport.configName(storedItem));
        shopConfig.set(path + ".DISPLAY-NAME", displayName);
        shopConfig.set(path + ".SLOT", slot);
        shopConfig.set(path + ".PRICE-PER-UNIT", resolvedPrice);
        shopConfig.set(path + ".ENCHANTMENTS", editorEnchantments(storedItem));
        shopConfig.set(path + ".ITEM-DATA", serializedItemData);

        if (!plugin.getConfigManager().saveShop()) {
            return new EditResult(false, "&cFailed to save shop.yml while updating that slot.");
        }

        reload();
        return new EditResult(true, "&aStored &f" + ColorUtils.strip(displayName)
                + "&a in slot &f" + slot + "&a at &f" + NumberUtils.format(resolvedPrice) + "&a.");
    }

    /** Drops whatever item sits in {@code slot} of {@code menuSection} out of shop.yml. */
    public EditResult removeMenuItem(String menuSection, int slot) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getOriginalShop()
                .getConfigurationSection(menuSection);
        if (menuConfig == null) {
            return new EditResult(false, "&cThat shop menu no longer exists.");
        }

        String itemKey = findItemKeyBySlot(menuSection, slot);
        if (itemKey == null) {
            return new EditResult(false, "&cThere is nothing in slot &f" + slot + "&c.");
        }

        ConfigurationSection itemsSection = menuConfig.getConfigurationSection("ITEMS");
        String path = menuSection + (itemsSection != null ? ".ITEMS." : ".") + itemKey;
        plugin.getConfigManager().getOriginalShop().set(path, null);

        if (!plugin.getConfigManager().saveShop()) {
            return new EditResult(false, "&cFailed to save shop.yml while clearing that slot.");
        }

        reload();
        return new EditResult(true, "&aCleared slot &f" + slot + "&a.");
    }

    private String findItemKeyBySlot(String menuSection, int slot) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getOriginalShop()
                .getConfigurationSection(menuSection);
        if (menuConfig == null) {
            return null;
        }

        ConfigurationSection itemsSection = menuConfig.getConfigurationSection("ITEMS");
        ConfigurationSection sourceSection = itemsSection != null ? itemsSection : menuConfig;
        for (String key : sourceSection.getKeys(false)) {
            if (sourceSection == menuConfig && MENU_META_KEYS.contains(key.toUpperCase(Locale.US))) {
                continue;
            }

            ConfigurationSection itemSec = sourceSection.getConfigurationSection(key);
            if (itemSec != null && itemSec.contains("MATERIAL") && itemSec.getInt("SLOT", -1) == slot) {
                return key;
            }
        }
        return null;
    }

    private String generateItemKey(String menuSection, Material material, int slot) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getOriginalShop()
                .getConfigurationSection(menuSection);
        ConfigurationSection itemsSection = menuConfig == null ? null : menuConfig.getConfigurationSection("ITEMS");
        ConfigurationSection sourceSection = itemsSection != null ? itemsSection : menuConfig;

        String base = material.name().replace('_', '-') + "-ITEM";
        if (sourceSection == null || !sourceSection.contains(base)) {
            return base;
        }
        return base + "-" + slot;
    }

    private String editorDisplayName(ItemMeta meta, Material material) {
        if (meta == null || !meta.hasDisplayName()) {
            return "&f" + plugin.getWorthManager().prettifyMaterial(material);
        }
        return meta.getDisplayName().replace('\u00A7', '&');
    }

    private List<String> editorEnchantments(ItemStack item) {
        if (item == null || item.getEnchantments().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<String> enchantments = new ArrayList<>();
        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            enchantments.add(entry.getKey().getName().toLowerCase(Locale.US) + ":" + entry.getValue());
        }
        return enchantments;
    }

    private boolean canFitPurchasedItem(Player player, ShopItem item, int amount) {
        if (item.isAmethystToolReward()) {
            int emptySlots = 0;
            for (ItemStack current : player.getInventory().getStorageContents()) {
                if (current == null || isAir(current.getType())) {
                    emptySlots++;
                }
            }
            return emptySlots >= amount;
        }

        ItemStack simulated = createPurchasedItem(item, amount);
        ItemStack[] storage = player.getInventory().getStorageContents();
        int remaining = simulated.getAmount();
        ItemStack singleItem = simulated.clone();
        singleItem.setAmount(1);

        for (ItemStack current : storage) {
            if (current == null || isAir(current.getType())) {
                remaining -= simulated.getMaxStackSize();
            } else if (canStack(plugin.getWorthManager().stripWorthDisplay(current), singleItem)) {
                remaining -= Math.max(0, current.getMaxStackSize() - current.getAmount());
            }

            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    private boolean isAmethystRewardAvailable(ShopItem item) {
        return item != null
                && item.amethystToolType() != null
                && plugin.getAmethystToolsManager() != null
                && plugin.getAmethystToolsManager().isEnabled()
                && plugin.getAmethystToolsManager().getToolSection(item.amethystToolType()) != null;
    }

    private boolean canStack(ItemStack first, ItemStack second) {
        return first.isSimilar(second) && first.getAmount() < first.getMaxStackSize();
    }

    private void validateShopConfiguration() {
        validateAmethystShardShopConfiguration();
        ConfigurationSection shopConfig = plugin.getConfigManager().getShop();
        ConfigurationSection categoriesSection = shopConfig.getConfigurationSection("CATEGORIES");
        if (categoriesSection == null) {
            plugin.getLogger().warning("shop.yml is missing the CATEGORIES section.");
            return;
        }

        Set<Integer> usedCategorySlots = new HashSet<>();
        for (ShopCategory category : loadCategories()) {
            if (!usedCategorySlots.add(category.slot())) {
                plugin.getLogger().warning("Duplicate shop category slot detected at " + category.slot()
                        + " for category " + category.key() + ".");
            }

            if (shopConfig.getConfigurationSection(category.menuSection()) == null) {
                plugin.getLogger().warning("Shop category " + category.key()
                        + " points to missing menu section " + category.menuSection() + ".");
            }
        }

        for (ShopCategory category : loadCategories()) {
            Set<Integer> usedItemSlots = new HashSet<>();
            for (ShopItem item : loadMenuItems(category.menuSection())) {
                if (item.pricePerUnit() < 0) {
                    plugin.getLogger().warning("Shop item " + item.key() + " in " + category.menuSection()
                            + " has a negative price-per-unit.");
                }

                if (item.slot() >= 0 && !usedItemSlots.add(item.slot())) {
                    plugin.getLogger().warning("Duplicate shop item slot detected at " + item.slot()
                            + " in " + category.menuSection() + ".");
                }

                if (item.currency() == Currency.MONEY) {
                    double worth = getWorth(item.material());
                    if (worth > item.pricePerUnit()) {
                        plugin.getLogger().warning("Potential shop arbitrage detected for " + item.material().name()
                                + " in " + category.menuSection() + ": buy " + plugin.getCurrencyManager().formatMoney(item.pricePerUnit())
                                + " but worth " + plugin.getCurrencyManager().formatMoney(worth) + ".");
                    }
                }
            }
        }
    }

    private void validateAmethystShardShopConfiguration() {
        if (plugin.getAmethystToolsManager() == null) {
            return;
        }
        for (AmethystToolType type : AmethystToolType.values()) {
            ConfigurationSection toolSection = plugin.getAmethystToolsManager().getToolSection(type);
            ConfigurationSection shopSection = toolSection == null
                    ? null
                    : toolSection.getConfigurationSection("SHARD-SHOP");
            if (shopSection == null || !shopSection.getBoolean("ENABLED", false)) {
                continue;
            }
            double price = shopSection.getDouble("PRICE-PER-UNIT", 0D);
            if (!Double.isFinite(price) || price <= 0D) {
                plugin.getLogger().warning("Amethyst tool " + type.name()
                        + " is enabled for /shardshop but price-per-unit is not greater than zero.");
            }
        }
    }

    public double getWorth(Material material) {
        return plugin.getWorthManager().getWorth(material);
    }

    public double getWorth(ItemStack item) {
        return plugin.getWorthManager().getWorth(item);
    }

    public void syncWorthDisplay(Player player) {
        plugin.getWorthManager().syncWorthDisplay(player);
    }

    public void clearWorthDisplay(Player player) {
        plugin.getWorthManager().clearWorthDisplay(player);
    }

    public void sanitizeInventory(Inventory inventory) {
        plugin.getWorthManager().sanitizeInventory(inventory);
    }

    public ItemStack stripWorthDisplay(ItemStack item) {
        return plugin.getWorthManager().stripWorthDisplay(item);
    }

    public String getWorthLoreLine(ItemStack item) {
        return plugin.getWorthManager().getWorthLoreLine(item);
    }

    private double findWorthRecursively(ConfigurationSection section, ItemStack item) {
        if (section == null || item == null || isAir(item.getType())) {
            return -1;
        }

        double directWorth = getDirectWorth(section, item);
        if (directWorth >= 0) {
            return directWorth;
        }

        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                continue;
            }

            double nestedWorth = findWorthRecursively(section.getConfigurationSection(key), item);
            if (nestedWorth >= 0) {
                return nestedWorth;
            }
        }
        return -1;
    }

    private double getDirectWorth(ConfigurationSection section, ItemStack item) {
        double specificWorth = resolveSpecificItemWorth(section, item);
        if (specificWorth >= 0) {
            return specificWorth;
        }

        String materialKey = item.getType().name();
        if (section.contains(materialKey) && !section.isConfigurationSection(materialKey)) {
            return section.getDouble(materialKey, -1);
        }
        return -1;
    }

    private double resolveSpecificItemWorth(ConfigurationSection section, ItemStack item) {
        if (item.getType() == Material.ENCHANTED_BOOK) {
            return resolveEnchantedBookWorth(section, item);
        }

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof PotionMeta) {
            PotionMeta meta = (PotionMeta) itemMeta;
            return resolvePotionWorth(section, item.getType(), meta);
        }

        return -1;
    }

    private double resolveEnchantedBookWorth(ConfigurationSection section, ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof EnchantmentStorageMeta)) {
            return -1;
        }

        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
        if (meta.getStoredEnchants().isEmpty()) {
            return -1;
        }

        double total = 0;
        boolean matched = false;
        for (Map.Entry<Enchantment, Integer> enchantmentEntry : meta.getStoredEnchants().entrySet()) {
            String enchantmentKey = enchantmentEntry.getKey().getName()
                    .toUpperCase(Locale.US)
                    .replace('-', '_');
            String worthKey = item.getType().name() + ":" + enchantmentKey + ":" + enchantmentEntry.getValue();
            if (section.contains(worthKey) && !section.isConfigurationSection(worthKey)) {
                total += section.getDouble(worthKey, 0);
                matched = true;
            }
        }

        return matched ? total : -1;
    }

    private double resolvePotionWorth(ConfigurationSection section, Material material, PotionMeta meta) {
        PotionData potionData = meta.getBasePotionData();
        PotionType potionType = potionData == null ? null : potionData.getType();
        if (potionType == null) {
            return -1;
        }

        String potionKey = buildPotionWorthKey(material, potionType);
        if (potionKey == null || !section.contains(potionKey) || section.isConfigurationSection(potionKey)) {
            return -1;
        }

        return section.getDouble(potionKey, -1);
    }

    private String buildPotionWorthKey(Material material, PotionType potionType) {
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

    public SellCategory getSellCategory(ItemStack item) {
        return plugin.getWorthManager().getSellCategory(item);
    }

    public Map<SellCategory, Double> getSellProgress(java.util.UUID uuid) {
        return plugin.getDatabaseManager().getSellProgress(uuid);
    }

    public SellProgressInfo getSellProgressInfo(java.util.UUID uuid, SellCategory category) {
        return getSellProgressInfo(getSellProgress(uuid), category);
    }

    public SellProgressInfo getSellProgressInfo(Map<SellCategory, Double> progress, SellCategory category) {
        List<Long> levels = getSellProgressLevels();
        double earned = progress.getOrDefault(category, 0D);
        int completedLevels = getCompletedLevels(earned, levels);
        boolean maxed = completedLevels >= levels.size();
        double currentMultiplier = 1.0 + (completedLevels * 0.1);
        double previousGoal = completedLevels <= 0 ? 0 : levels.get(completedLevels - 1);
        double nextGoal = maxed ? levels.get(levels.size() - 1) : levels.get(completedLevels);
        int percentage = calculateProgressPercentage(earned, previousGoal, nextGoal, maxed);

        return new SellProgressInfo(
                category,
                earned,
                completedLevels,
                currentMultiplier,
                maxed ? "MAX" : formatMultiplier(1.0 + ((completedLevels + 1) * 0.1)),
                previousGoal,
                nextGoal,
                percentage,
                buildProgressBar(percentage),
                maxed
        );
    }

    public double getCurrentSellMultiplier(Map<SellCategory, Double> progress, SellCategory category) {
        return 1.0 + (getCompletedLevels(progress.getOrDefault(category, 0D), getSellProgressLevels()) * 0.1);
    }

    public SellResult sellInventoryContents(Player player, Inventory inventory, int startInclusive, int endExclusive) {
        return sellInventoryContents(
                player,
                inventory,
                startInclusive,
                endExclusive,
                EconomyReason.SELL_PAYOUT,
                true
        );
    }

    public SellResult sellInventoryContents(
            Player player,
            Inventory inventory,
            int startInclusive,
            int endExclusive,
            EconomyReason reason,
            boolean sendFeedback
    ) {
        if (player == null || inventory == null) {
            return emptySale();
        }

        PendingSale sale = createPendingSale(player);
        int firstSlot = Math.max(0, startInclusive);
        int lastSlot = Math.min(inventory.getSize(), Math.max(firstSlot, endExclusive));

        for (int slot = firstSlot; slot < lastSlot; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || isAir(item.getType())) {
                continue;
            }

            double payout = collectSellWorthEntries(item, sale);
            if (payout <= 0) {
                continue;
            }

            sale.totalPayout += payout;
            sale.soldSlots.add(slot);
        }

        return executeSale(player, sale, reason, sendFeedback, () -> {
            for (int slot : sale.soldSlots) {
                inventory.setItem(slot, null);
            }
        });
    }

    private PendingSale createPendingSale(Player player) {
        PendingSale sale = new PendingSale();
        sale.currentProgress.putAll(getSellProgress(player.getUniqueId()));
        return sale;
    }

    private SellResult emptySale() {
        return new SellResult(SellStatus.NO_SELLABLE_ITEMS, 0, java.util.Collections.emptySet());
    }

    private SellResult failedSale() {
        return new SellResult(SellStatus.TRANSACTION_FAILED, 0, java.util.Collections.emptySet());
    }

    private SellResult executeSale(
            Player player,
            PendingSale sale,
            EconomyReason reason,
            boolean sendFeedback,
            Runnable removeSoldItems
    ) {
        if (sale.totalPayout <= 0) {
            return emptySale();
        }

        EconomyReason resolvedReason = reason == null ? EconomyReason.SELL_PAYOUT : reason;
        EconomyTransactionResult depositResult = plugin.getEconomyManager().deposit(player, sale.totalPayout, resolvedReason);
        if (!depositResult.success()) {
            return failedSale();
        }

        removeSoldItems.run();
        return commitSale(player, sale, sendFeedback);
    }

    public double sellInventory(Player player, boolean handOnly) {
        PendingSale sale = createPendingSale(player);
        ItemStack[] contents = handOnly
                ? new ItemStack[]{player.getInventory().getItemInMainHand()}
                : player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || isAir(item.getType())) {
                continue;
            }

            double payout = collectSellWorthEntries(item, sale);
            if (payout <= 0) {
                continue;
            }

            sale.totalPayout += payout;
            sale.soldSlots.add(i);
        }

        SellResult result = executeSale(player, sale, EconomyReason.SELL_PAYOUT, true, () -> {
            if (handOnly) {
                player.getInventory().setItemInMainHand(null);
            } else {
                for (int slot : sale.soldSlots) {
                    player.getInventory().setItem(slot, null);
                }
            }
        });

        return result.hasSales() ? result.totalPayout() : 0;
    }

    private double collectSellWorthEntries(ItemStack item, PendingSale sale) {
        List<WorthManager.SellWorthEntry> entries = plugin.getWorthManager().resolveSellWorthEntries(item);
        if (entries.isEmpty()) {
            return 0;
        }

        double totalPayout = 0;
        for (WorthManager.SellWorthEntry entry : entries) {
            double payout = entry.totalWorth() * getCurrentSellMultiplier(sale.currentProgress, entry.category());
            if (payout <= 0) {
                continue;
            }

            totalPayout += payout;
            sale.earnedByCategory.merge(entry.category(), entry.totalWorth(), Double::sum);
            sale.history.add(new PendingSellHistory(entry.material(), entry.amount(), payout));
        }
        return totalPayout;
    }

    private SellResult commitSale(Player player, PendingSale sale, boolean sendFeedback) {
        EnumSet<SellCategory> leveledUpCategories = EnumSet.noneOf(SellCategory.class);

        List<DatabaseManager.SellHistoryRecord> historyBatch = new ArrayList<>();
        List<DatabaseManager.PlayerLogRecord> logBatch = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (PendingSellHistory historyEntry : sale.history) {
            historyBatch.add(plugin.getDatabaseManager().new SellHistoryRecord(
                    player.getUniqueId(),
                    historyEntry.material().name(),
                    historyEntry.amount(),
                    historyEntry.payout(),
                    now
            ));
            String prettyName = plugin.getWorthManager().prettifyMaterial(historyEntry.material());
            logBatch.add(plugin.getDatabaseManager().new PlayerLogRecord(
                    player.getUniqueId(),
                    player.getName(),
                    "Shop",
                    "SHOP_SELL",
                    "Sold " + prettyName + " x" + historyEntry.amount() + " for " + plugin.getCurrencyManager().formatMoney(historyEntry.payout()),
                    now
            ));
        }

        Map<SellCategory, Double> earnedCopy = new EnumMap<>(sale.earnedByCategory);
        for (Map.Entry<SellCategory, Double> entry : sale.earnedByCategory.entrySet()) {
            SellCategory category = entry.getKey();
            double before = sale.currentProgress.getOrDefault(category, 0D);
            double after = before + entry.getValue();
            if (getCompletedLevels(after, getSellProgressLevels())
                    > getCompletedLevels(before, getSellProgressLevels())) {
                leveledUpCategories.add(category);
            }

            sale.currentProgress.put(category, after);
        }

        plugin.getDatabaseManager().executeAsync(() -> {
            plugin.getDatabaseManager().addSellHistoryBatch(historyBatch);
            plugin.getDatabaseManager().addPlayerLogBatch(logBatch);
            for (Map.Entry<SellCategory, Double> entry : earnedCopy.entrySet()) {
                plugin.getDatabaseManager().addSellProgress(player.getUniqueId(), entry.getKey(), entry.getValue());
            }
        });

        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            data.addMoneyMade(sale.totalPayout);
        }

        if (sendFeedback) {
            sendSellFeedback(player, sale.totalPayout);
        }
        if (sale.totalPayout > 0) {
            SoundUtils.play(player, plugin.getConfigManager().getSound("SELL.SUCCESS"));
        }
        if (!leveledUpCategories.isEmpty()) {
            SoundUtils.play(player, plugin.getConfigManager().getSound("SELL.LEVEL-UP"));
        }

        return new SellResult(
                SellStatus.SUCCESS,
                sale.totalPayout,
                leveledUpCategories.isEmpty() ? java.util.Collections.emptySet() : EnumSet.copyOf(leveledUpCategories)
        );
    }

    private void sendSellFeedback(Player player, double totalPayout) {
        String sellMsg = plugin.getConfigManager().getConfig()
                .getString("SETTINGS.SELL-MESSAGE", "&a+{price_formatted}")
                .replace("%price%", plugin.getCurrencyManager().formatCompactAmount(CurrencyManager.CurrencyType.MONEY, totalPayout))
                .replace("%price_formatted%", plugin.getCurrencyManager().formatMoney(totalPayout))
                .replace("{price}", plugin.getCurrencyManager().formatCompactAmount(CurrencyManager.CurrencyType.MONEY, totalPayout))
                .replace("{price_formatted}", plugin.getCurrencyManager().formatMoney(totalPayout));
        PlayerSettingUtils.sendActionBar(plugin, player, sellMsg);
    }

    private List<Long> getSellProgressLevels() {
        List<Long> levels = plugin.getConfigManager().getMenus().getLongList("PROGRESS-MENU.LEVEL");
        if (!levels.isEmpty()) {
            return levels;
        }

        return new java.util.ArrayList<>(java.util.Arrays.asList(25_000L,  150_000L,  500_000L,  1_000_000L));
    }

    private int getCompletedLevels(double earned, List<Long> levels) {
        int completed = 0;
        for (Long level : levels) {
            if (earned >= level) {
                completed++;
            } else {
                break;
            }
        }
        return completed;
    }

    private int calculateProgressPercentage(double earned, double previousGoal, double nextGoal, boolean maxed) {
        if (maxed || nextGoal <= previousGoal) {
            return 100;
        }

        double progress = (earned - previousGoal) / (nextGoal - previousGoal);
        return (int) Math.max(0, Math.min(100, Math.round(progress * 100)));
    }

    private String formatMultiplier(double multiplier) {
        return String.format(Locale.US, "%.1fx", multiplier);
    }

    private String buildProgressBar(int percentage) {
        String symbol = plugin.getConfigManager().getMenus()
                .getString("PROGRESS-MENU.PROGRESS-BAR", "\u25A0");
        int filledSegments = (int) Math.round((percentage / 100.0) * MAX_MULTIPLIER_BAR_SEGMENTS);
        filledSegments = Math.max(0, Math.min(MAX_MULTIPLIER_BAR_SEGMENTS, filledSegments));

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < MAX_MULTIPLIER_BAR_SEGMENTS; i++) {
            builder.append(i < filledSegments ? "&#6BF18D" : "&7").append(symbol);
        }
        return builder.toString();
    }
}
