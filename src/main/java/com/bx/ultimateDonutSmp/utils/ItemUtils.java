package com.bx.ultimateDonutSmp.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class ItemUtils {

    private static final Material SKULL_ITEM_MATERIAL = resolveMaterial("SKULL_ITEM", Material.SKULL_ITEM);
    private static final Material SKELETON_HEAD_MATERIAL = resolveMaterial("SKULL_ITEM", Material.SKULL_ITEM);
    private static final Material ZOMBIE_HEAD_MATERIAL = resolveMaterial("ZOMBIE_HEAD", Material.SKULL_ITEM);
    private static final Material CREEPER_HEAD_MATERIAL = resolveMaterial("CREEPER_HEAD", Material.SKULL_ITEM);
    private static final Material WITHER_HEAD_MATERIAL = resolveMaterial("WITHER_SKULL_ITEM", Material.SKULL_ITEM);
    private static final Material DRAGON_HEAD_MATERIAL = resolveMaterial("DRAGON_HEAD", Material.SKULL_ITEM);

    public static ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(ColorUtils.colorize(displayName));
        } else {
            meta.setDisplayName("");
        }

        if (lore != null && !lore.isEmpty()) {
            meta.setLore(ColorUtils.colorizeList(lore));
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createItem(Material material, String displayName) {
        return createItem(material, displayName, null);
    }

    /**
     * 1.12.2 colored menu buttons use a shared Material plus a durability/data value
     * (for example {@code STAINED_GLASS_PANE} + 14 for red).
     */
    public static ItemStack createItem(Material material, short data, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(ColorUtils.colorize(displayName));
        } else {
            meta.setDisplayName("");
        }

        if (lore != null && !lore.isEmpty()) {
            meta.setLore(ColorUtils.colorizeList(lore));
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createItem(Material material, short data, String displayName) {
        return createItem(material, data, displayName, null);
    }

    /**
     * Puts a menu name and lore on a copy of an existing item, leaving everything else about it
     * alone. Used where the icon has to stay the real item but still read as a menu entry.
     */
    public static ItemStack withDisplay(ItemStack item, String displayName, List<String> lore) {
        if (item == null) return null;

        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return copy;

        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(ColorUtils.colorize(displayName));
        }
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(ColorUtils.colorizeList(lore));
        }

        copy.setItemMeta(meta);
        return copy;
    }

    public static ItemStack createPlayerHead(OfflinePlayer player, String displayName, List<String> lore) {
        return createPlayerHead(player, null, displayName, lore);
    }

    public static ItemStack createMobHead(String mobTypeKey, String textureValue, String displayName, List<String> lore) {
        if (textureValue != null && !textureValue.trim().isEmpty()) {
            return createPlayerHead(null, textureValue, displayName, lore);
        }

        String key = mobTypeKey == null ? "" : mobTypeKey.trim().toUpperCase(java.util.Locale.US);
        Material vanillaHead;
        switch (key) {
            case "SKELETON":
                vanillaHead = SKELETON_HEAD_MATERIAL;
                break;
            case "ZOMBIE":
                vanillaHead = ZOMBIE_HEAD_MATERIAL;
                break;
            case "CREEPER":
                vanillaHead = CREEPER_HEAD_MATERIAL;
                break;
            case "PIGLIN":
            case "ZOMBIFIED_PIGLIN":
                vanillaHead = SKULL_ITEM_MATERIAL;
                break;
            case "WITHER_SKELETON":
                vanillaHead = WITHER_HEAD_MATERIAL;
                break;
            case "DRAGON":
                vanillaHead = DRAGON_HEAD_MATERIAL;
                break;
            default:
                vanillaHead = null;
                break;
        }

        if (vanillaHead != null) {
            return createItem(vanillaHead, displayName, lore);
        }

        String skinUrl = getMobSkinUrl(key);
        if (skinUrl != null) {
            return createHeadFromSkinUrl(skinUrl, displayName, lore);
        }

        String mhfName;
        switch (key) {
            case "COW":
                mhfName = "MHF_Cow";
                break;
            case "PIG":
                mhfName = "MHF_Pig";
                break;
            case "SPIDER":
                mhfName = "MHF_Spider";
                break;
            case "CAVE_SPIDER":
                mhfName = "MHF_CaveSpider";
                break;
            case "BLAZE":
                mhfName = "MHF_Blaze";
                break;
            case "IRON_GOLEM":
                mhfName = "MHF_Golem";
                break;
            case "ENDERMAN":
                mhfName = "MHF_Enderman";
                break;
            case "SQUID":
                mhfName = "MHF_Squid";
                break;
            case "GHAST":
                mhfName = "MHF_Ghast";
                break;
            case "SHEEP":
                mhfName = "MHF_Sheep";
                break;
            case "CHICKEN":
                mhfName = "MHF_Chicken";
                break;
            default:
                mhfName = "MHF_" + key;
                break;
        }

        OfflinePlayer mhfPlayer = Bukkit.getOfflinePlayer(mhfName);
        return createPlayerHead(mhfPlayer, displayName, lore);
    }

    public static ItemStack createHeadFromSkinUrl(String skinUrlOrBase64, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(SKULL_ITEM_MATERIAL);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta)) {
            return createItem(SKULL_ITEM_MATERIAL, displayName, lore);
        }
        SkullMeta meta = (SkullMeta) rawMeta;

        applyTextureToSkullMeta(meta, skinUrlOrBase64);

        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(ColorUtils.colorize(displayName));
        } else {
            meta.setDisplayName("");
        }

        if (lore != null && !lore.isEmpty()) {
            meta.setLore(ColorUtils.colorizeList(lore));
        }

        item.setItemMeta(meta);
        return item;
    }

    public static boolean applyTextureToSkullMeta(SkullMeta meta, String textureOrUrl) {
        if (meta == null || textureOrUrl == null || textureOrUrl.trim().isEmpty()) {
            return false;
        }

        String base64;
        String rawUrl = null;
        String trimmed = textureOrUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            rawUrl = trimmed.startsWith("http://") ? "https://" + trimmed.substring(7) : trimmed;
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + rawUrl + "\"}}}";
            base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } else {
            base64 = trimmed;
            try {
                byte[] decoded = Base64.getDecoder().decode(base64);
                String jsonStr = new String(decoded, StandardCharsets.UTF_8);
                if (jsonStr.contains("http://textures.minecraft.net/")) {
                    jsonStr = jsonStr.replace("http://textures.minecraft.net/", "https://textures.minecraft.net/");
                    base64 = Base64.getEncoder().encodeToString(jsonStr.getBytes(StandardCharsets.UTF_8));
                }
                JsonObject root = new JsonParser().parse(jsonStr).getAsJsonObject();
                JsonObject textures = root.getAsJsonObject("textures");
                JsonObject skin = textures == null ? null : textures.getAsJsonObject("SKIN");
                if (skin != null && skin.has("url")) {
                    rawUrl = skin.get("url").getAsString();
                    if (rawUrl.startsWith("http://")) {
                        rawUrl = "https://" + rawUrl.substring(7);
                    }
                }
            } catch (Throwable ignored) {}
        }

        UUID profileId = UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8));

        // 1. Try Direct Mojang Authlib GameProfile Reflection (Used by all skull plugins)
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(profileId, "MobHead");
            
            Object property;
            try {
                property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", base64);
            } catch (Throwable t) {
                property = propertyClass.getConstructor(String.class, String.class, String.class).newInstance("textures", base64, null);
            }

            Object propertiesMap = gameProfileClass.getMethod("getProperties").invoke(gameProfile);
            try {
                Class<?> multimapClass = Class.forName("com.google.common.collect.Multimap");
                java.lang.reflect.Method putMethod = multimapClass.getMethod("put", Object.class, Object.class);
                putMethod.invoke(propertiesMap, "textures", property);
            } catch (Throwable t) {
                for (java.lang.reflect.Method m : propertiesMap.getClass().getMethods()) {
                    if (m.getName().equals("put") && m.getParameterCount() == 2) {
                        m.setAccessible(true);
                        m.invoke(propertiesMap, "textures", property);
                        break;
                    }
                }
            }

            java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, gameProfile);
            return true;
        } catch (Throwable ignored1) {}

        return false;
    }

    public static String getMobSkinUrl(String mobTypeKey) {
        if (mobTypeKey == null) return null;
        String key = mobTypeKey.trim().toUpperCase(java.util.Locale.US);
        switch (key) {
            case "IRON_GOLEM":
                return "https://textures.minecraft.net/texture/e13f34227283796bc017244cb46557d64bd562fa9dab0e12af5d23ad699cf697";
            case "PIG":
                return "https://textures.minecraft.net/texture/d875eb45aca34a4d24c3dc1395fc020ccf37f825a17b054a22fd24b189c24c";
            case "SPIDER":
                return "https://textures.minecraft.net/texture/cd541541daaff50896cd258bdbdd4cf80c3ba816735726078bfe393927e57f1";
            case "BLAZE":
                return "https://textures.minecraft.net/texture/b78ef2e4cf2c41a2d14bfde9caff10219f5b1bf5b35a49eb51c6467882cb5f0";
            default:
                return null;
        }
    }

    public static ItemStack createPlayerHead(
            OfflinePlayer player,
            String textureValue,
            String displayName,
            List<String> lore
    ) {
        ItemStack item = new ItemStack(SKULL_ITEM_MATERIAL);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta)) {
            return createItem(SKULL_ITEM_MATERIAL, displayName, lore);
        }
        SkullMeta meta = (SkullMeta) rawMeta;

        if (textureValue != null && !textureValue.trim().isEmpty()) {
            applyTextureToSkullMeta(meta, textureValue);
        } else if (player != null) {
            applyOwnerToSkullMeta(meta, player);
        }
        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(ColorUtils.colorize(displayName));
        } else {
            meta.setDisplayName("");
        }

        if (lore != null && !lore.isEmpty()) {
            meta.setLore(ColorUtils.colorizeList(lore));
        }

        item.setItemMeta(meta);
        return item;
    }

    private static TextureProfileData decodeTextureProfile(String textureValue) {
        if (textureValue == null || textureValue.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(textureValue);
            } catch (IllegalArgumentException ignored) {
                decoded = Base64.getUrlDecoder().decode(textureValue);
            }
            JsonObject root = new JsonParser().parse(new String(decoded, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject textures = root.getAsJsonObject("textures");
            JsonObject skin = textures == null ? null : textures.getAsJsonObject("SKIN");
            if (skin == null || !skin.has("url")) {
                return null;
            }

            URL skinUrl = URI.create(skin.get("url").getAsString()).toURL();
            UUID profileId = parseProfileUuid(root.has("profileId") ? root.get("profileId").getAsString() : null);
            String profileName = root.has("profileName") ? root.get("profileName").getAsString() : null;
            return new TextureProfileData(profileId, profileName, skinUrl);
        } catch (Exception | LinkageError ignored) {
            return null;
        }
    }

    private static UUID parseProfileUuid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String compact = value.replace("-", "");
        if (compact.length() != 32) {
            return null;
        }
        try {
            return UUID.fromString(compact.substring(0, 8)
                    + "-" + compact.substring(8, 12)
                    + "-" + compact.substring(12, 16)
                    + "-" + compact.substring(16, 20)
                    + "-" + compact.substring(20));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

 public static final class TextureProfileData {
    private final UUID profileId;
    private final String profileName;
    private final URL skinUrl;

    public TextureProfileData(UUID profileId, String profileName, URL skinUrl) {
        this.profileId = profileId;
        this.profileName = profileName;
        this.skinUrl = skinUrl;
    }

    public UUID profileId() { return profileId; }
    public String profileName() { return profileName; }
    public URL skinUrl() { return skinUrl; }

    @Override public String toString() {
        return "TextureProfileData[profileId=+profileId, profileName=+profileName, skinUrl=+skinUrl]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextureProfileData that = (TextureProfileData) o;
        return java.util.Objects.equals(profileId, that.profileId) && java.util.Objects.equals(profileName, that.profileName) && java.util.Objects.equals(skinUrl, that.skinUrl);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(profileId, profileName, skinUrl);
    }
}

    public static ItemStack createPlaceholder(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("");
            item.setItemMeta(meta);
        }
        return item;
    }

    /** 1.12.2 colored placeholders use a shared Material plus a durability/data value. */
    public static ItemStack createPlaceholder(Material material, short data) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createGlassPane() {
        return createPlaceholder(Material.STAINED_GLASS_PANE, (short) 7);
    }

    public static ItemStack createGlassPane(Material material) {
        return createPlaceholder(material);
    }

    public static ItemStack fillWith(Material material, int size) {
        return createPlaceholder(material);
    }

    /**
     * Resolves a configured material name to its 1.12.2 material.
     *
     * <p>Returns {@code null} when the name cannot be resolved — a failed lookup stays failed
     * and must never degrade to {@code Material.STONE}: a kit or reward item that silently turns
     * into stone is a gameplay-visible bug, not a fallback. Callers that merely need an icon and
     * must render something should use {@link #parseMaterial(String, Material)} with an explicit
     * fallback instead.</p>
     */
    public static Material parseMaterial(String name) {
        if (name == null || name.isEmpty()) return null;
        LegacyMaterialSupport.Icon resolved = LegacyMaterialSupport.resolve(name);
        return resolved == null ? null : resolved.material();
    }

    /**
     * Same resolution as {@link #parseMaterial(String)}, but with an explicit, caller-chosen
     * fallback so GUI code can always render an item. The fallback is visible at the call site —
     * unlike a hidden STONE substitution it cannot surprise anyone.
     */
    public static Material parseMaterial(String name, Material fallback) {
        Material material = parseMaterial(name);
        return material != null ? material : fallback;
    }

    public static ItemStack addEnchantments(ItemStack item, List<String> enchantmentStrings) {
        if (item == null || enchantmentStrings == null || enchantmentStrings.isEmpty()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
            for (String entry : enchantmentStrings) {
                if (entry == null) continue;
                String[] parts = entry.split(":");
                if (parts.length < 2) continue;
                String name = parts[0].trim().toLowerCase();
                int level;
                try {
                    level = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                Enchantment ench = resolveEnchantment(name);
                if (ench != null) {
                    storageMeta.addStoredEnchant(ench, level, true);
                }
            }
            item.setItemMeta(storageMeta);
            return item;
        }

        for (String entry : enchantmentStrings) {
            if (entry == null) continue;
            String[] parts = entry.split(":");
            if (parts.length < 2) continue;
            String name = parts[0].trim().toLowerCase();
            int level;
            try {
                level = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }
            Enchantment ench = resolveEnchantment(name);
            if (ench != null) {
                item.addUnsafeEnchantment(ench, level);
            }
        }
        return item;
    }

    public static ItemStack setGlint(ItemStack item, Boolean glint) {
        if (item == null || item.getType() == Material.AIR || glint == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        try {
            throw new UnsupportedOperationException();
        } catch (NoSuchMethodError | Exception ignored) {
        }

        if (glint) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void fillInventory(org.bukkit.inventory.Inventory inventory, Material material) {
        ItemStack filler = createPlaceholder(material);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    public static void fillInventory(org.bukkit.inventory.Inventory inventory, Material material, short data) {
        ItemStack filler = createPlaceholder(material, data);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    public static void fillInventory(org.bukkit.inventory.Inventory inventory) {
        fillInventory(inventory, Material.STAINED_GLASS_PANE, (short) 7);
    }

    private static Enchantment resolveEnchantment(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String normalized = name.trim().toUpperCase(java.util.Locale.US).replace(' ', '_').replace('-', '_');
        Enchantment enchantment = Enchantment.getByName(normalized);
        if (enchantment != null) {
            return enchantment;
        }

        if ("SHARPNESS".equals(normalized)) return Enchantment.getByName("DAMAGE_ALL");
        if ("SMITE".equals(normalized)) return Enchantment.getByName("DAMAGE_UNDEAD");
        if ("BANE_OF_ARTHROPODS".equals(normalized)) return Enchantment.getByName("DAMAGE_ARTHROPODS");
        if ("EFFICIENCY".equals(normalized)) return Enchantment.getByName("DIG_SPEED");
        if ("DURABILITY".equals(normalized)) return Enchantment.getByName("DURABILITY");
        if ("FORTUNE".equals(normalized)) return Enchantment.getByName("LOOT_BONUS_BLOCKS");
        if ("LOOTING".equals(normalized)) return Enchantment.getByName("LOOT_BONUS_MOBS");
        if ("PROTECTION".equals(normalized)) return Enchantment.getByName("PROTECTION_ENVIRONMENTAL");
        if ("BLAST_PROTECTION".equals(normalized)) return Enchantment.getByName("PROTECTION_EXPLOSIONS");
        if ("FIRE_PROTECTION".equals(normalized)) return Enchantment.getByName("PROTECTION_FIRE");
        if ("PROJECTILE_PROTECTION".equals(normalized)) return Enchantment.getByName("PROTECTION_PROJECTILE");
        if ("FEATHER_FALLING".equals(normalized)) return Enchantment.getByName("PROTECTION_FALL");
        if ("RESPIRATION".equals(normalized)) return Enchantment.getByName("OXYGEN");
        if ("AQUA_AFFINITY".equals(normalized)) return Enchantment.getByName("WATER_WORKER");
        if ("THORNS".equals(normalized)) return Enchantment.getByName("THORNS");
        if ("DEPTH_STRIDER".equals(normalized)) return Enchantment.getByName("DEPTH_STRIDER");
        if ("FROST_WALKER".equals(normalized)) return Enchantment.getByName("FROST_WALKER");
        if ("BINDING_CURSE".equals(normalized) || "CURSE_OF_BINDING".equals(normalized)) return Enchantment.getByName("BINDING_CURSE");
        if ("VANISHING_CURSE".equals(normalized) || "CURSE_OF_VANISHING".equals(normalized)) return Enchantment.getByName("VANISHING_CURSE");
        if ("POWER".equals(normalized)) return Enchantment.getByName("ARROW_DAMAGE");
        if ("ARROW_KNOCKBACK".equals(normalized)) return Enchantment.getByName("ARROW_KNOCKBACK");
        if ("FLAME".equals(normalized)) return Enchantment.getByName("ARROW_FIRE");
        if ("INFINITY".equals(normalized)) return Enchantment.getByName("ARROW_INFINITE");
        if ("LUCK_OF_THE_SEA".equals(normalized)) return Enchantment.getByName("LUCK");
        if ("LURE".equals(normalized)) return Enchantment.getByName("LURE");
        if ("MENDING".equals(normalized)) return Enchantment.getByName("MENDING");
        if ("SWEEPING".equals(normalized) || "SWEEPING_EDGE".equals(normalized)) return Enchantment.getByName("SWEEPING_EDGE");
        return null;
    }

    /**
     * Sets a skull's owning player across Bukkit generations.
     *
     * <p>{@code SkullMeta#setOwningPlayer(OfflinePlayer)} only exists from Bukkit 1.13 onwards.
     * Spigot 1.12.2 exposes {@code SkullMeta#setOwner(String)} and nothing else, so a direct
     * call to the modern method is a compile error against the 1.12.2 API and a
     * {@code NoSuchMethodError} at runtime when compiled against a newer one. Call sites must go
     * through this helper, which prefers the modern method reflectively and falls back to the
     * 1.12.2 name-based setter.</p>
     *
     * @return {@code true} when an owner was applied
     */
    public static boolean applyOwnerToSkullMeta(SkullMeta meta, OfflinePlayer player) {
        if (meta == null || player == null) {
            return false;
        }
        try {
            java.lang.reflect.Method setOwningPlayer = meta.getClass().getMethod("setOwningPlayer", OfflinePlayer.class);
            setOwningPlayer.invoke(meta, player);
            return true;
        } catch (Throwable ignored) {
            String ownerName = player.getName();
            if (ownerName == null || ownerName.trim().isEmpty()) {
                return false;
            }
            try {
                meta.setOwner(ownerName);
                return true;
            } catch (Throwable ignored2) {
                return false;
            }
        }
    }

    private static Material resolveMaterial(String modernName, Material fallback) {
        Material resolved = Material.matchMaterial(modernName);
        return resolved != null ? resolved : fallback;
    }
}
