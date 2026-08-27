package com.bx.ultimateDonutSmp.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class ItemUtils {

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
                vanillaHead = Material.SKELETON_SKULL;
                break;
            case "ZOMBIE":
                vanillaHead = Material.ZOMBIE_HEAD;
                break;
            case "CREEPER":
                vanillaHead = Material.CREEPER_HEAD;
                break;
            case "PIGLIN":
            case "ZOMBIFIED_PIGLIN":
                vanillaHead = Material.PIGLIN_HEAD;
                break;
            case "WITHER_SKELETON":
                vanillaHead = Material.WITHER_SKELETON_SKULL;
                break;
            case "DRAGON":
                vanillaHead = Material.DRAGON_HEAD;
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
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta)) {
            return createItem(Material.PLAYER_HEAD, displayName, lore);
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
                JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();
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

        // 2. Try Modern Paper PlayerProfile API
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(profileId, "MobHead");
            if (rawUrl != null) {
                try {
                    PlayerTextures textures = profile.getTextures();
                    textures.setSkin(java.net.URI.create(rawUrl).toURL());
                    profile.setTextures(textures);
                } catch (Throwable ignored) {}
            }

            try {
                Class<?> profilePropClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
                Object prop = profilePropClass.getConstructor(String.class, String.class).newInstance("textures", base64);
                profile.getClass().getMethod("setProperty", profilePropClass).invoke(profile, prop);
            } catch (Throwable ignored) {}

            try {
                java.lang.reflect.Method setPlayerProfileMethod = meta.getClass().getMethod("setPlayerProfile", PlayerProfile.class);
                setPlayerProfileMethod.invoke(meta, profile);
                return true;
            } catch (Throwable ignored) {
                meta.setOwnerProfile(profile);
                return true;
            }
        } catch (Throwable ignored2) {}

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
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta)) {
            return createItem(Material.PLAYER_HEAD, displayName, lore);
        }
        SkullMeta meta = (SkullMeta) rawMeta;

        if (textureValue != null && !textureValue.trim().isEmpty()) {
            applyTextureToSkullMeta(meta, textureValue);
        } else if (player != null) {
            meta.setOwningPlayer(player);
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

    private static boolean applyTextureProfile(SkullMeta meta, OfflinePlayer fallback, String textureValue) {
        TextureProfileData data = decodeTextureProfile(textureValue);
        if (data == null) {
            return false;
        }

        try {
            UUID profileId = data.profileId() != null
                    ? data.profileId()
                    : UUID.nameUUIDFromBytes(("uds-head:" + textureValue).getBytes(StandardCharsets.UTF_8));
            String profileName = data.profileName();
            if (profileName == null || profileName.trim().isEmpty() || profileName.length() > 16) {
                profileName = fallback == null ? null : fallback.getName();
            }

            PlayerProfile profile = Bukkit.createPlayerProfile(profileId, profileName);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(data.skinUrl());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
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
            JsonObject root = JsonParser.parseString(new String(decoded, StandardCharsets.UTF_8))
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

public final class TextureProfileData {
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

    public static ItemStack createGlassPane() {
        return createPlaceholder(Material.GRAY_STAINED_GLASS_PANE);
    }

    public static ItemStack createGlassPane(Material material) {
        return createPlaceholder(material);
    }

    public static ItemStack fillWith(Material material, int size) {
        return createPlaceholder(material);
    }

    public static Material parseMaterial(String name) {
        if (name == null || name.isEmpty()) return Material.STONE;
        try {
            return Material.valueOf(name.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
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
                Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name));
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
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name));
            if (ench != null) {
                item.addUnsafeEnchantment(ench, level);
            }
        }
        return item;
    }

    public static ItemStack setGlint(ItemStack item, Boolean glint) {
        if (item == null || item.getType().isAir() || glint == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        try {
            meta.setEnchantmentGlintOverride(glint);
            item.setItemMeta(meta);
            return item;
        } catch (NoSuchMethodError | Exception ignored) {
        }

        if (glint) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
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

    public static void fillInventory(org.bukkit.inventory.Inventory inventory) {
        fillInventory(inventory, Material.GRAY_STAINED_GLASS_PANE);
    }
}
