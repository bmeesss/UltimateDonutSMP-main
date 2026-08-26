package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class FeatureManager {

    private static final String ROOT = "FEATURES";

    public enum DisabledCommandAction {
        MESSAGE,
        UNKNOWN,
        UNREGISTER;

        public static DisabledCommandAction fromString(String raw) {
            if (raw == null) {
                return MESSAGE;
            }
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {        case "UNREGISTER": case "DISABLE": case "OFF": case "REMOVE": UNREGISTER            break;        case "UNKNOWN": case "HIDE": UNKNOWN            break;        default: MESSAGE            break;
            };
        }
    }

    public enum Feature {
        CHAT("CHAT", "chat", "global chat commands and moderation controls.", "WRITABLE_BOOK", "CHAT"),
        IGNORE("IGNORE", "ignore", "player ignore and unignore commands.", "BARRIER", "IGNORE"),
        MESSAGING("MESSAGING", "messaging", "private messages, replies, and pm toggles.", "PAPER", "MESSAGE"),
        BOUNTY("BOUNTY", "bounty", "bounty command and menus.", "TARGET", "BOUNTY"),
        CUBOIDS("CUBOIDS", "cuboids", "cuboid region management and bound region helpers.", "WOODEN_AXE", "CUBOID"),
        AFK("AFK", "afk", "afk command, menus, and afk movement task.", "CLOCK", "AFK"),
        SHARDS("SHARDS", "shards", "shard balances, shard pay, passive rewards, and shard cuboids.", "AMETHYST_SHARD", "SHARDS"),
        WARPS("WARPS", "warps", "warp commands and warp manager commands.", "ENDER_PEARL", "WARP"),
        TEAMS("TEAMS", "teams", "team command, team homes, and team menus.", "IRON_HELMET", "TEAM"),
        BILLFORD("BILLFORD", "billford", "billford trade menu and rotation task.", "EMERALD", "BILLFORD"),
        HOMES("HOMES", "homes", "home commands and home menu.", "LIGHT_BLUE_BED", "HOME"),
        LEADERBOARDS("LEADERBOARDS", "leaderboards", "leaderboard commands and leaderboard menus.", "GOLD_INGOT", "LEADERBOARDS"),
        NIGHT_VISION("NIGHT_VISION", "night vision", "night vision player toggle command.", "GOLDEN_CARROT", "NIGHT-VISION"),
        PHANTOM("PHANTOM", "phantom toggle", "phantom spawning toggle command.", "PHANTOM_MEMBRANE", "PHANTOM"),
        RTP("RTP", "rtp", "random teleport command and rtp menu.", "COMPASS", "RTP"),
        RTP_ZONE("RTP_ZONE", "rtp zone", "cuboid-triggered rtp countdown zone.", "ENDER_EYE", null),
        SELL("SELL", "sell", "sell commands and sell menus.", "HOPPER", "SELL"),
        WORTH("WORTH", "worth", "worth browser and worth display helpers.", "EMERALD", "SELL"),
        SETTINGS("SETTINGS", "settings", "player settings menu.", "COMPARATOR", "SETTINGS"),
        SHOP("SHOP", "shop", "shop command and purchase menus.", "CHEST", "SHOP"),
        ENDER_CHEST("ENDER_CHEST", "ender chest", "custom ender chest command and listener.", "ENDER_CHEST", "ENDERCHEST"),
        GAMEMODE("GAMEMODE", "gamemode", "staff gamemode commands.", "GRASS_BLOCK", "GAMEMODE"),
        SOCIAL("SOCIAL", "social", "social media, store, and media commands.", "BOOK", "SOCIAL"),
        SPAWN("SPAWN", "spawn", "spawn command and spawn menu.", "BEACON", "SPAWN"),
        STATS("STATS", "stats", "stats, ping, and playtime commands.", "PLAYER_HEAD", "STATS"),
        TPA("TPA", "tpa", "teleport request commands and confirm menu.", "ENDER_PEARL", "TPA"),
        TPA_AUTO("TPA_AUTO", "tpa auto", "tpa auto-accept commands.", "REDSTONE_TORCH", "TPAUTO"),
        FIND_PLAYER("FIND_PLAYER", "find player", "staff find player command.", "SPYGLASS", "FINDPLAYER"),
        CRATES("CRATES", "crates", "crate commands, menus, key-all, and visual effects.", "TRIPWIRE_HOOK", "CRATE"),
        RULES("RULES", "rules", "rules command and rules menu.", "BOOKSHELF", "RULES"),
        HELP("HELP", "help", "help command and server info menu.", "KNOWLEDGE_BOOK", "HELP"),
        SCOREBOARD("SCOREBOARD", "scoreboard", "sidebar scoreboard task and display.", "MAP", null),
        TABLIST("TABLIST", "tablist", "tablist header, footer, and player list names.", "NAME_TAG", null),
        AUCTION_HOUSE("AUCTION_HOUSE", "auction house", "auction house commands, listings, claims, and expiry task.", "GOLD_INGOT", null),
        ORDERS("ORDERS", "orders", "orders board commands, menus, and expiry task.", "WRITABLE_BOOK", null),
        STAFF_MODE("STAFF_MODE", "staff mode", "staff mode command, hotbar, vanish, and staff tools.", "NETHERITE_CHESTPLATE", null),
        SPAWN_STASH("SPAWN_STASH", "SpawnStash", "Staff bait stash spawning, alerts, and rollback cleanup.", "CHEST", "SPAWN-STASH"),
        FREEZE("FREEZE", "freeze", "freeze command, listeners, and freeze state enforcement.", "PACKED_ICE", null),
        INVSEE("INVSEE", "invsee", "inventory inspection command and sessions.", "CHEST_MINECART", null),
        PROFILE_VIEWER("PROFILE_VIEWER", "profile viewer", "profile viewer command and homes browser.", "PLAYER_HEAD", null),
        PUNISHMENTS("PUNISHMENTS", "punishments", "punishment commands, aliases, and history menus.", "IRON_AXE", null),
        SPAWNERS("SPAWNERS", "spawners", "managed spawner commands, listeners, visibility, and generation.", "SPAWNER", null),
        CLEAR_LAG("CLEAR_LAG", "clearlag", "clearlag command and cleanup task.", "LAVA_BUCKET", null),
        PORTALS("PORTALS", "portals", "portal triggers, manager command, and portal holograms.", "END_PORTAL_FRAME", null),
        AMETHYST_TOOLS("AMETHYST_TOOLS", "amethyst tools", "amethyst tool command, listener, and expiry task.", "AMETHYST_SHARD", null),
        COMBAT("COMBAT", "combat", "combat tagging listener and command blocking.", "SHIELD", null),
        FAST_CRYSTALS("FAST_CRYSTALS", "fast crystals", "fast crystal placement/breaking behavior.", "END_CRYSTAL", null),
        KEY_ALL("KEY_ALL", "key-all", "automatic crate key-all rewards.", "TRIPWIRE_HOOK", null),
        OPTIMIZATION("OPTIMIZATION", "optimization", "runtime optimization monitor and adaptive task skipping.", "REDSTONE", null),
        MAINTENANCE("MAINTENANCE", "maintenance", "seamless maintenance system with lobby redirection.", "REDSTONE_LAMP", "MAINTENANCE"),
        HIDE("HIDE", "Hide", "Persistent player identity scrambling and configured disguises.", "NAME_TAG", "HIDE"),
        FRIENDS("FRIENDS", "friends", "player friends/follows system.", "PLAYER_HEAD", "FRIEND"),
        SAFETY("SAFETY", "safety", "safety command and info.", "BOOK", "SAFETY");

        private final String configKey;
        private final String displayName;
        private final String description;
        private final String iconMaterial;
        private final String legacyCommandKey;

        Feature(String configKey, String displayName, String description, String iconMaterial, String legacyCommandKey) {
            this.configKey = configKey;
            this.displayName = displayName;
            this.description = description;
            this.iconMaterial = iconMaterial;
            this.legacyCommandKey = legacyCommandKey;
        }

        public String configKey() {
            return configKey;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }

        public String iconMaterial() {
            return iconMaterial;
        }

        public String legacyCommandKey() {
            return legacyCommandKey;
        }

        public static Optional<Feature> fromInput(String input) {
            String normalized = normalize(input);
            return Arrays.stream(values())
                    .filter(feature -> normalize(feature.configKey).equals(normalized)
                            || normalize(feature.name()).equals(normalized)
                            || normalize(feature.displayName).equals(normalized)
                            || (feature.legacyCommandKey != null
                            && normalize(feature.legacyCommandKey).equals(normalized)))
                    .findFirst();
        }

        public static Feature fromLegacyCommandKey(String key) {
            String normalized = normalize(key);
            return Arrays.stream(values())
                    .filter(feature -> normalize(feature.configKey).equals(normalized)
                            || (feature.legacyCommandKey != null
                            && normalize(feature.legacyCommandKey).equals(normalized)))
                    .findFirst()
                    .orElse(null);
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .toUpperCase(Locale.ROOT);
        }
    }

    private final UltimateDonutSmp plugin;
    private final java.util.Map<Feature, Boolean> featureCache = new java.util.concurrent.ConcurrentHashMap<>();

    public FeatureManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    public void clearCache() {
        featureCache.clear();
    }

    public DisabledCommandAction getDisabledCommandAction() {
        String value = plugin.getConfigManager().getConfig().getString("FEATURES_SETTINGS.DISABLED_COMMAND_ACTION", "MESSAGE");
        return DisabledCommandAction.fromString(value);
    }

    public List<Feature> getFeatures() {
        return java.util.Collections.singletonList(Feature.values());
    }

    public boolean isEnabled(Feature feature) {
        if (feature == null) {
            return true;
        }
        // A capturing lambda would be allocated on every call, cache hit included, and this runs on
        // per-tick paths. Read first, compute only on a miss.
        Boolean cached = featureCache.get(feature);
        if (cached != null) {
            return cached;
        }
        boolean resolved = isEnabled(plugin.getConfigManager().getConfig(), feature);
        featureCache.put(feature, resolved);
        return resolved;
    }

    public boolean areEnabled(Feature... features) {
        for (Feature feature : features) {
            if (feature != null && !isEnabled(feature)) {
                return false;
            }
        }
        return true;
    }

    public boolean isCommandFeatureEnabled(String commandName) {
        return areEnabled(featuresForCommand(commandName));
    }

    public static Feature[] featuresForCommand(String commandName) {
        String key = commandName == null ? "" : commandName.trim().toLowerCase(Locale.ROOT);
        return switch (key) {        case "team": new Feature[]{Feature.TEAMS}            break;        case "chat": new Feature[]{Feature.CHAT}            break;        case "ignore": case "unignore": new Feature[]{Feature.IGNORE}            break;        case "msg": case "reply": case "pm": new Feature[]{Feature.MESSAGING}            break;        case "home": case "homes": case "sethome": case "delhome": case "renamehome": new Feature[]{Feature.HOMES}            break;        case "spawn": new Feature[]{Feature.SPAWN}            break;        case "afk": new Feature[]{Feature.AFK}            break;        case "tpa": case "tpahere": case "tpaccept": case "tpadeny": case "tpacancel": new Feature[]{Feature.TPA}            break;        case "tpauto": case "tpahereauto": new Feature[]{Feature.TPA, Feature.TPA_AUTO}            break;        case "shards": case "shardpay": case "addshards": case "removeshards": case "setshards": new Feature[]{Feature.SHARDS}            break;        case "crate": case "crates": case "keys": new Feature[]{Feature.CRATES}            break;        case "shop": new Feature[]{Feature.SHOP}            break;        case "shardshop": new Feature[]{Feature.SHOP, Feature.SHARDS}            break;        case "orders": new Feature[]{Feature.ORDERS}            break;
        case "auctionhouse": new Feature[]{Feature.AUCTION_HOUSE}            break;        case "enderchest": case "ecsee": new Feature[]{Feature.ENDER_CHEST}            break;        case "sell": case "sellhand": case "sellall": case "sellhistory": new Feature[]{Feature.SELL}            break;        case "worth": new Feature[]{Feature.SELL, Feature.WORTH}            break;        case "rtp": new Feature[]{Feature.RTP}            break;        case "stats": case "ping": case "playtime": new Feature[]{Feature.STATS}            break;        case "leaderboard": new Feature[]{Feature.LEADERBOARDS}            break;        case "freeze": new Feature[]{Feature.FREEZE}            break;        case "gamemode": new Feature[]{Feature.GAMEMODE}            break;        case "staffmode": case "stafflist": case "vanish": case "fakeplayer": new Feature[]{Feature.STAFF_MODE}            break;        case "staffchat": new Feature[0]            break;        case "helpop": case "report": new Feature[0]            break;        case "spawnstash": new Feature[]{Feature.SPAWN_STASH}            break;        case "invsee": new Feature[]{Feature.INVSEE}            break;        case "profileviewer": case "seehomes": new Feature[]{Feature.PROFILE_VIEWER}            break;
            case "punishments", "ban", "tempban", "mute", "tempmute", "warn", "kick", "blacklist",
                    "unban", "unmute", "unblacklist" -> new Feature[]{Feature.PUNISHMENTS};        case "bounty": new Feature[]{Feature.BOUNTY}            break;        case "warp": case "warpmanager": case "setwarp": case "delwarp": new Feature[]{Feature.WARPS}            break;        case "portalmanager": new Feature[]{Feature.PORTALS}            break;        case "nightvision": new Feature[]{Feature.NIGHT_VISION}            break;        case "phantom": new Feature[]{Feature.PHANTOM}            break;        case "findplayer": new Feature[]{Feature.FIND_PLAYER}            break;        case "settings": new Feature[]{Feature.SETTINGS}            break;        case "twitter": case "store": case "social": new Feature[]{Feature.SOCIAL}            break;        case "rules": new Feature[]{Feature.RULES}            break;        case "help": new Feature[]{Feature.HELP}            break;        case "servers": new Feature[0]            break;        case "billford": new Feature[]{Feature.BILLFORD}            break;        case "spawner": new Feature[]{Feature.SPAWNERS}            break;        case "clearlag": new Feature[]{Feature.CLEAR_LAG}            break;        case "hide": case "disguise": new Feature[]{Feature.HIDE}            break;        case "cuboid": new Feature[]{Feature.CUBOIDS}            break;        case "amethysttool": new Feature[]{Feature.AMETHYST_TOOLS}            break;        case "friends": case "friend": new Feature[]{Feature.FRIENDS}            break;        case "safety": new Feature[]{Feature.SAFETY}            break;        default: new Feature[0]            break;
        };
    }

    public static boolean isEnabled(FileConfiguration config, Feature feature) {
        if (config == null || feature == null) {
            return true;
        }

        String featurePath = path(feature);
        if (config.contains(featurePath)) {
            return config.getBoolean(featurePath, true);
        }

        String legacyKey = feature.legacyCommandKey();
        if (legacyKey != null && config.contains("COMMANDS." + legacyKey)) {
            return config.getBoolean("COMMANDS." + legacyKey, true);
        }

        return true;
    }

    public static boolean isCommandEnabled(FileConfiguration config, String commandKey) {
        Feature feature = Feature.fromLegacyCommandKey(commandKey);
        if (feature != null) {
            return isEnabled(config, feature);
        }
        return config == null || config.getBoolean("COMMANDS." + commandKey, true);
    }

    public boolean setEnabled(Feature feature, boolean enabled) {
        if (feature == null) {
            return false;
        }

        plugin.getConfigManager().getConfig().set(path(feature), enabled);
        clearCache();
        if (!plugin.getConfigManager().saveConfig()) {
            return false;
        }
        applyRuntimeState(feature);
        return true;
    }

    public boolean toggle(Feature feature) {
        return setEnabled(feature, !isEnabled(feature));
    }

    public String statusText(Feature feature) {
        return isEnabled(feature)
                ? plugin.getConfigManager().getMessageOrDefault("FEATURES.STATUS-ENABLED", "&aEnabled")
                : plugin.getConfigManager().getMessageOrDefault("FEATURES.STATUS-DISABLED", "&cDisabled");
    }

    public void sendDisabledMessage(CommandSender sender, Feature feature, String commandLabel) {
        String message = plugin.getConfigManager().getMessageOrDefault(
                "FEATURES.DISABLED",
                "&cThe {feature} feature is currently disabled.",
                "{feature}", feature.displayName(),
                "{feature_key}", feature.configKey(),
                "{command}", commandLabel == null ? "" : commandLabel
        );
        sender.sendMessage(ColorUtils.toComponent(message));
    }

    public void applyRuntimeState(Feature feature) {
        if (feature == null) {
            return;
        }

        plugin.syncCommands();

        switch (feature) {        case SCOREBOARD: {

                            if (plugin.getScoreboardManager() != null) {
                                plugin.getScoreboardManager().updateAll();
                            }
                        break;        }        case TABLIST: {

                            if (plugin.getTablistManager() != null) {
                                plugin.getTablistManager().updateAll();
                                plugin.getTablistManager().updateNamesAll();
                            }
                        break;        }        case SHARDS: {

                            if (plugin.getShardManager() != null) {
                                plugin.getShardManager().reloadSettings();
                            }
                        break;        }        case RTP_ZONE: {

                            if (plugin.getRtpZoneManager() != null) {
                                plugin.getRtpZoneManager().reloadSettings();
                                for (Player player : plugin.getServer().getOnlinePlayers()) {
                                    plugin.getRtpZoneManager().clearState(player);
                                }
                            }
                        break;        }        case RTP: {

                            if (plugin.getRtpManager() != null) {
                                plugin.getRtpManager().reload();
                            }
                            if (plugin.getRtpZoneManager() != null) {
                                plugin.getRtpZoneManager().reloadSettings();
                                for (Player player : plugin.getServer().getOnlinePlayers()) {
                                    plugin.getRtpZoneManager().clearState(player);
                                }
                            }
                        break;        }        case CRATES: {

                            if (plugin.getCrateManager() != null) {
                                plugin.getCrateManager().reload();
                                plugin.getCrateManager().clearAllSessions();
                            }
                            if (plugin.getCrateVisualManager() != null) {
                                plugin.getCrateVisualManager().reload();
                            }
                        break;        }        case ENDER_CHEST: {

                            if (plugin.getEnderChestManager() != null) {
                                plugin.getEnderChestManager().reload();
                            }
                        break;        }        case STAFF_MODE: {

                            if (!isEnabled(feature) && plugin.getStaffModeManager() != null) {
                                plugin.getStaffModeManager().shutdown();
                            }
                        break;        }        case FREEZE: {

                            if (!isEnabled(feature) && plugin.getFreezeManager() != null) {
                                plugin.getFreezeManager().shutdown();
                            }
                        break;        }        case INVSEE: {

                            if (!isEnabled(feature) && plugin.getInvseeManager() != null) {
                                plugin.getInvseeManager().shutdown();
                            }
                        break;        }        case SPAWNERS: {

                            if (plugin.getSpawnerManager() != null) {
                                plugin.getSpawnerManager().reload();
                            }
                            if (plugin.getAntiEspManager() != null) {
                                plugin.getAntiEspManager().refreshAllPlayers();
                            }
                        break;        }        case AUCTION_HOUSE: {

                            if (plugin.getAuctionHouseManager() != null) {
                                plugin.getAuctionHouseManager().reload();
                            }
                        break;        }        case ORDERS: {

                            if (plugin.getOrdersManager() != null) {
                                plugin.getOrdersManager().reload();
                            }
                        break;        }        case AUCTION_HOUSE: {

                            if (plugin.getSpawnStashManager() != null) {
                                if (isEnabled(feature)) {
                                    plugin.getSpawnStashManager().reload();
                                } else {
                                    plugin.getSpawnStashManager().shutdown();
                                }
                            }
                        break;        }        case OPTIMIZATION: {

                            if (plugin.getOptimizationManager() != null) {
                                plugin.getOptimizationManager().reload();
                            }
                        break;        }        default: {

                        break;        }
        }
    }

    private static String path(Feature feature) {
        return ROOT + "." + feature.configKey() + ".ENABLED";
    }
}
