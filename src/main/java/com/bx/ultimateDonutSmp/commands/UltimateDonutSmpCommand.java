package com.bx.ultimateDonutSmp.commands;

import com.bx.ultimateDonutSmp.utils.PermissionUtils;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.managers.FeatureManager;
import com.bx.ultimateDonutSmp.managers.MaintenanceManager;
import com.bx.ultimateDonutSmp.managers.OptimizationManager;
import com.bx.ultimateDonutSmp.managers.SpawnManager;
import com.bx.ultimateDonutSmp.managers.StatsWipeManager;
import com.bx.ultimateDonutSmp.menus.FeatureToggleMenu;
import com.bx.ultimateDonutSmp.menus.StatsWipeMenu;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public class UltimateDonutSmpCommand implements CommandExecutor, TabCompleter {

    private static final String RELOAD_PERMISSION = "ultimatedonutsmp.admin.reload";
    private static final String STATS_WIPE_PERMISSION = "ultimatedonutsmp.admin.statswipe";
    private static final String OPTIMIZE_PERMISSION = "ultimatedonutsmp.admin.optimize";
    private static final String SETUP_PERMISSION = "ultimatedonutsmp.admin.setup";
    private static final String FEATURES_PERMISSION = "ultimatedonutsmp.admin.features";
    private static final int COMMANDS_PER_PAGE = 8;

    private static final List<String> ROOT_COMPLETIONS = new java.util.ArrayList<>(java.util.Arrays.asList("reload",  "statswipe",  "optimize",  "setup",  "features",  "maintenance"));
    private static final List<String> SETUP_COMPLETIONS = new java.util.ArrayList<>(java.util.Arrays.asList("status",  "apply",  "setspawn",  "setafk",  "commands"));
    private static final List<String> COMMAND_CATEGORIES = new java.util.ArrayList<>(java.util.Arrays.asList("all",  "starter",  "economy",  "market",  "pvp",  "staff",  "admin",  "setup"));

    private final UltimateDonutSmp plugin;

    public UltimateDonutSmpCommand(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                handleReload(sender);
                break;
            case "statswipe":
                handleStatsWipe(sender, label, args);
                break;
            case "optimize":
            case "optimization":
                handleOptimize(sender, label, args);
                break;
            case "setup":
                handleSetup(sender, label, args);
                break;
            case "features":
                handleFeatures(sender, label, args);
                break;
            case "maintenance":
                handleMaintenance(sender, label, args);
                break;
            default:
                sendUsage(sender, label);
                break;
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!PermissionUtils.has(sender, RELOAD_PERMISSION)) {
            sender.sendMessage(ColorUtils.toComponent("&cYou do not have permission to reload plugin settings."));
            return;
        }

        try {
            plugin.reloadAllPluginConfigurations();
            sender.sendMessage(ColorUtils.toComponent("&aUltimateDonutSMP configuration reloaded."));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "failed to reload ultimatedonutsmp configuration.", exception);
            sender.sendMessage(ColorUtils.toComponent("&cFailed to reload configuration. Check console for details."));
        }
    }

    private void handleStatsWipe(CommandSender sender, String label, String[] args) {
        if (!PermissionUtils.has(sender, STATS_WIPE_PERMISSION)) {
            sender.sendMessage(ColorUtils.toComponent(message("NO-PERMISSION",
                    "&cYou do not have permission to use stats wipe.")));
            return;
        }

        if (args.length == 1 || isGuiAlias(args[1])) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ColorUtils.toComponent(message("PLAYER-ONLY-GUI",
                        "&cOpen the stats wipe GUI in-game, or use /" + label + " statswipe <target> confirm.")));
                return;
            }

            new StatsWipeMenu(plugin).open(player);
            return;
        }

        StatsWipeManager.WipeTarget target = StatsWipeManager.WipeTarget.fromInput(args[1]).orElse(null);
        if (target == null) {
            sender.sendMessage(ColorUtils.toComponent(message("INVALID-TARGET",
                    "&cInvalid stats wipe target. Available: {targets}")
                    .replace("{targets}", availableTargets())));
            return;
        }

        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage(ColorUtils.toComponent(message("DIRECT-USAGE",
                    "&cUse /" + label + " statswipe <target> confirm to run directly, or /" + label + " statswipe to open the GUI.")));
            return;
        }

        StatsWipeManager.WipeResult result = plugin.getStatsWipeManager().wipeTarget(target, sender.getName());
        if (result.busy()) {
            sender.sendMessage(ColorUtils.toComponent(message("BUSY", "&cA wipe is already in progress.")));
            return;
        }
        if (!result.success()) {
            String error = result.errorMessage() == null || result.errorMessage().trim().isEmpty()
                    ? "unknown error"
                    : result.errorMessage();
            sender.sendMessage(ColorUtils.toComponent(message("FAILED",
                    "&cStats wipe failed: {error}")
                    .replace("{error}", error)));
            return;
        }

        sender.sendMessage(ColorUtils.toComponent(message("SUCCESS",
                "&aWipe complete: &f{target}&a. Affected records: &f{count}&a.")
                .replace("{target}", target.getDisplayName())
                .replace("{count}", String.valueOf(result.affectedCount(target)))));
    }

    private void handleOptimize(CommandSender sender, String label, String[] args) {
        if (!PermissionUtils.has(sender, OPTIMIZE_PERMISSION)) {
            sender.sendMessage(ColorUtils.toComponent("&cYou do not have permission to use optimization tools."));
            return;
        }

        OptimizationManager optimizationManager = plugin.getOptimizationManager();
        if (optimizationManager == null) {
            sender.sendMessage(ColorUtils.toComponent("&cOptimization manager is not available."));
            return;
        }

        if (args.length == 1 || args[1].equalsIgnoreCase("status")) {
            sendOptimizationStatus(sender, label, optimizationManager);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "reload": {
                optimizationManager.reload();
                sender.sendMessage(ColorUtils.toComponent("&aOptimization settings reloaded."));
                break;
                break;
            }
            case "reset": {
                optimizationManager.resetStats();
                sender.sendMessage(ColorUtils.toComponent("&aOptimization runtime counters reset."));
                break;
                break;
            }
            default:
                sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " optimize [status|reload|reset]"));
                break;
        }
    }

    private void handleSetup(CommandSender sender, String label, String[] args) {
        if (!PermissionUtils.has(sender, SETUP_PERMISSION)) {
            sender.sendMessage(ColorUtils.toComponent("&cYou do not have permission to use setup tools."));
            return;
        }

        if (args.length == 1 || args[1].equalsIgnoreCase("status")) {
            sendSetupStatus(sender, label);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "apply":
                handleSetupApply(sender, label, args);
                break;
            case "setspawn":
                handleSetupLocation(sender, true);
                break;
            case "setafk":
                handleSetupLocation(sender, false);
                break;
            case "commands":
                handleSetupCommands(sender, label, args);
                break;
            default:
                sendSetupUsage(sender, label);
                break;
        }
    }

    private void handleFeatures(CommandSender sender, String label, String[] args) {
        if (!PermissionUtils.has(sender, FEATURES_PERMISSION)) {
            sender.sendMessage(ColorUtils.toComponent(
                    plugin.getConfigManager().getMessageOrDefault(
                            "FEATURES.NO-PERMISSION",
                            "&cYou do not have permission to manage feature toggles."
                    )
            ));
            return;
        }

        if (args.length == 1 || (args.length >= 2 && isGuiAlias(args[1]))) {
            if (sender instanceof Player) {
            Player player = (Player) args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                handleReload(sender);
                break;
            case "statswipe":
                handleStatsWipe(sender, label, args);
                break;
            case "optimize":
            case "optimization":
                handleOptimize(sender, label, args);
                break;
            case "setup":
                handleSetup(sender, label, args);
                break;
            case "features":
                handleFeatures(sender, label, args);
                break;
            case "maintenance":
                handleMaintenance(sender, label, args);
                break;
            default:
                sendUsage(sender, label);
                break;
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!PermissionUtils.has(sender, RELOAD_PERMISSION)) {
            sender.sendMessage(ColorUtils.toComponent("&cYou do not have permission to reload plugin settings."));
            return;
        }

        try {
            plugin.reloadAllPluginConfigurations();
            sender.sendMessage(ColorUtils.toComponent("&aUltimateDonutSMP configuration reloaded."));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "failed to reload ultimatedonutsmp configuration.", exception);
            sender.sendMessage(ColorUtils.toComponent("&cFailed to reload configuration. Check console for details."));
        }
    }

    private void handleStatsWipe(CommandSender sender, String label, String[] args) {
        if (!PermissionUtils.has(sender, STATS_WIPE_PERMISSION)) {
            sender.sendMessage(ColorUtils.toComponent(message("NO-PERMISSION",
                    "&cYou do not have permission to use stats wipe.")));
            return;
        }

        if (args.length == 1 || isGuiAlias(args[1])) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ColorUtils.toComponent(message("PLAYER-ONLY-GUI",
                        "&cOpen the stats wipe GUI in-game, or use /" + label + " statswipe <target> confirm.")));
                return;
            }

            new StatsWipeMenu(plugin).open(player);
            return;
        }

        StatsWipeManager.WipeTarget target = StatsWipeManager.WipeTarget.fromInput(args[1]).orElse(null);
        if (target == null) {
            sender.sendMessage(ColorUtils.toComponent(message("INVALID-TARGET",
                    "&cInvalid stats wipe target. Available: {targets}")
                    .replace("{targets}", availableTargets())));
            return;
        }

        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage(ColorUtils.toComponent(message("DIRECT-USAGE",
                    "&cUse /" + label + " statswipe <target> confirm to run directly, or /" + label + " statswipe to open the GUI.")));
            return;
        }

        StatsWipeManager.WipeResult result = plugin.getStatsWipeManager().wipeTarget(target, sender.getName());
        if (result.busy()) {
            sender.sendMessage(ColorUtils.toComponent(message("BUSY", "&cA wipe is already in progress.")));
            return;
        }
        if (!result.success()) {
            String error = result.errorMessage() == null || result.errorMessage().trim().isEmpty()
                    ? "unknown error"
                    : result.errorMessage();
            sender.sendMessage(ColorUtils.toComponent(message("FAILED",
                    "&cStats wipe failed: {error}")
                    .replace("{error}", error)));
            return;
        }

        sender.sendMessage(ColorUtils.toComponent(message("SUCCESS",
                "&aWipe complete: &f{target}&a. Affected records: &f{count}&a.")
                .replace("{target}", target.getDisplayName())
                .replace("{count}", String.valueOf(result.affectedCount(target)))));
    }

    private void handleOptimize(CommandSender sender, String label, String[] args) {
        if (!PermissionUtils.has(sender, OPTIMIZE_PERMISSION)) {
            sender.sendMessage(ColorUtils.toComponent("&cYou do not have permission to use optimization tools."));
            return;
        }

        OptimizationManager optimizationManager = plugin.getOptimizationManager();
        if (optimizationManager == null) {
            sender.sendMessage(ColorUtils.toComponent("&cOptimization manager is not available."));
            return;
        }

        if (args.length == 1 || args[1].equalsIgnoreCase("status")) {
            sendOptimizationStatus(sender, label, optimizationManager);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "reload": {
                optimizationManager.reload();
                sender.sendMessage(ColorUtils.toComponent("&aOptimization settings reloaded."));
                break;
                break;
            }
            case "reset": {
                optimizationManager.resetStats();
                sender.sendMessage(ColorUtils.toComponent("&aOptimization runtime counters reset."));
                break;
                break;
            }
            default:
                sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " optimize [status|reload|reset]"));
                break;
        }
    }

    private void handleSetup(CommandSender sender, String label, String[] args) {
        if (!PermissionUtils.has(sender, SETUP_PERMISSION)) {
            sender.sendMessage(ColorUtils.toComponent("&cYou do not have permission to use setup tools."));
            return;
        }

        if (args.length == 1 || args[1].equalsIgnoreCase("status")) {
            sendSetupStatus(sender, label);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "apply":
                handleSetupApply(sender, label, args);
                break;
            case "setspawn":
                handleSetupLocation(sender, true);
                break;
            case "setafk":
                handleSetupLocation(sender, false);
                break;
            case "commands":
                handleSetupCommands(sender, label, args);
                break;
            default:
                sendSetupUsage(sender, label);
                break;
        }
    }

    private void handleFeatures(CommandSender sender, String label, String[] args) {
        if (!PermissionUtils.has(sender, FEATURES_PERMISSION)) {
            sender.sendMessage(ColorUtils.toComponent(
                    plugin.getConfigManager().getMessageOrDefault(
                            "FEATURES.NO-PERMISSION",
                            "&cYou do not have permission to manage feature toggles."
                    )
            ));
            return;
        }

        if (args.length == 1 || (args.length >= 2 && isGuiAlias(args[1]))) {
            if (sender;
                new FeatureToggleMenu(plugin).open(player);
                return;
            }
            sendFeatureList(sender);
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            sendFeatureList(sender);
            return;
        }

        if (!action.equals("toggle") && !action.equals("enable") && !action.equals("disable")) {
            sendFeatureUsage(sender, label);
            return;
        }

        if (args.length < 3) {
            sendFeatureUsage(sender, label);
            return;
        }

        FeatureManager.Feature feature = FeatureManager.Feature.fromInput(args[2]).orElse(null);
        if (feature == null) {
            sender.sendMessage(ColorUtils.toComponent(
                    plugin.getConfigManager().getMessageOrDefault(
                            "FEATURES.UNKNOWN",
                            "&cUnknown feature: &f{feature}",
                            "{feature}", args[2]
                    )
            ));
            return;
        }

        boolean success;
        if (action.equals("toggle")) {
            success = plugin.getFeatureManager().toggle(feature);
        } else {
            success = plugin.getFeatureManager().setEnabled(feature, action.equals("enable"));
        }

        if (!success) {
            sender.sendMessage(ColorUtils.toComponent(
                    plugin.getConfigManager().getMessageOrDefault(
                            "FEATURES.TOGGLE-FAILED",
                            "&cFailed to update {feature}.",
                            "{feature}", feature.displayName(),
                            "{feature_key}", feature.configKey()
                    )
            ));
            return;
        }

        sender.sendMessage(ColorUtils.toComponent(
                plugin.getConfigManager().getMessageOrDefault(
                        "FEATURES.TOGGLED",
                        "&a{feature} is now {state}.",
                        "{feature}", feature.displayName(),
                        "{feature_key}", feature.configKey(),
                        "{state}", plugin.getFeatureManager().statusText(feature)
                )
        ));
    }

    private void handleSetupApply(CommandSender sender, String label, String[] args) {
        if (args.length < 4 || !args[2].equalsIgnoreCase("single-paper") || !args[3].equalsIgnoreCase("confirm")) {
            sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " setup apply single-paper confirm"));
            return;
        }

        try {
            FileConfiguration config = plugin.getConfigManager().getConfig();
            FileConfiguration database = plugin.getConfigManager().getDatabase();
            FileConfiguration network = plugin.getConfigManager().getNetwork();

            config.set("SETTINGS.SPAWN-MENU", true);
            config.set("SETTINGS.AFK-MENU", true);

            database.set("DATABASE.TYPE", "SQLITE");
            database.set("DATABASE.SQLITE.FILE", "data/data.db");
            database.set("REDIS.ENABLED", false);

            network.set("NETWORK.LOCAL_SERVER_ID", "local");
            network.set("NETWORK.LOCAL_DISPLAY_NAME", "local");
            network.set("NETWORK-STATUS.LOCAL-SERVER-ID", "local");
            network.set("NETWORK-STATUS.LOCAL-DISPLAY-NAME", "local");
            network.set("NETWORK-STATUS.SERVERS", null);
            network.set("NETWORK-STATUS.SERVERS.LOCAL.DISPLAY", "local");
            network.set("NETWORK-STATUS.SERVERS.LOCAL.SOURCE.TYPE", "LOCAL");

            boolean saved = plugin.getConfigManager().saveConfig()
                    & plugin.getConfigManager().saveDatabase()
                    & plugin.getConfigManager().saveNetwork();
            if (!saved) {
                sender.sendMessage(ColorUtils.toComponent("&cSingle paper preset was applied in memory, but one or more files failed to save. Check console."));
                return;
            }

            plugin.reloadAllPluginConfigurations();
            sender.sendMessage(ColorUtils.toComponent("&aSingle paper setup preset applied."));
            sender.sendMessage(ColorUtils.toComponent("&7Next: use &f/" + label + " setup setspawn &7and &f/" + label + " setup setafk&7."));
            sender.sendMessage(ColorUtils.toComponent("&eRestart the server before going live so storage changes are fully applied."));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "failed to apply single paper setup preset.", exception);
            sender.sendMessage(ColorUtils.toComponent("&cFailed to apply setup preset. Check console for details."));
        }
    }

    private void handleSetupLocation(CommandSender sender, boolean spawn) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.toComponent("&cOnly players can save setup locations."));
            return;
        }

        Location location = player.getLocation();
        if (spawn) {
            SpawnManager.SetupLocationResult result = plugin.getSpawnManager().setSpawnLocation(location);
            if (!result.success()) {
                plugin.getLogger().warning("failed to save setup spawn location at "
                        + describeLocation(location) + ": " + result.message());
                sender.sendMessage(ColorUtils.toComponent("&cSpawn location could not be saved: &f" + result.message()));
                return;
            }
            sender.sendMessage(ColorUtils.toComponent("&aSpawn location saved at &f" + describeLocation(location)
                    + "&a. &7Area &f" + result.areaId() + " &7slot &f" + result.slot() + "&7."));
            return;
        }

        SpawnManager.SetupLocationResult result = plugin.getSpawnManager().setAfkLocation(location);
        if (!result.success()) {
            plugin.getLogger().warning("failed to save setup AFK location at "
                    + describeLocation(location) + ": " + result.message());
            sender.sendMessage(ColorUtils.toComponent("&cAFK location could not be saved: &f" + result.message()));
            return;
        }
        sender.sendMessage(ColorUtils.toComponent("&aAFK location saved at &f" + describeLocation(location)
                + "&a. &7Area &f" + result.areaId() + " &7slot &f" + result.slot() + "&7."));
    }

    private void sendSetupStatus(CommandSender sender, String label) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        FileConfiguration database = plugin.getConfigManager().getDatabase();

        sender.sendMessage(ColorUtils.toComponent("&8&m---------- &bUltimateDonutSMP setup &8&m----------"));
        sendCheck(sender, isJava21OrNewer(), "java",
                System.getProperty("java.version", "unknown") + " (java 21+ required)");
        sendCheck(sender, isPaperLikeServer(), "server",
                plugin.getServer().getName() + " " + plugin.getServer().getBukkitVersion());
        sendCheck(sender, isDatabaseConnected(), "storage",
                database.getString("DATABASE.TYPE", "SQLITE").toUpperCase(Locale.ROOT) + " configured");
        sendCheck(sender, isRedisReadyForSingleServer(database), "redis",
                redisDetail(database));
        sendCheck(sender, plugin.getSpawnManager().hasSpawn(), "spawn",
                plugin.getSpawnManager().hasSpawn() ? "ready" : "use /" + label + " setup setspawn");
        sendCheck(sender, plugin.getSpawnManager().hasAfk(), "afk",
                plugin.getSpawnManager().hasAfk() ? "ready" : "use /" + label + " setup setafk");
        List<String> rtpWorlds = availableRtpWorlds();
        sendCheck(sender, plugin.getRtpManager().isEnabled()
                        && !rtpWorlds.isEmpty(),
                "rtp worlds",
                rtpWorlds.isEmpty() ? "no configured rtp worlds are loaded" : String.join(", ", rtpWorlds));
        sendCheck(sender, true, "optional integrations", optionalIntegrationDetail());
        sender.sendMessage(ColorUtils.toComponent("&7Preset: &f/" + label + " setup apply single-paper confirm"));
        sender.sendMessage(ColorUtils.toComponent("&7Commands: &f/" + label + " setup commands [category] [page]"));
    }

    private void handleSetupCommands(CommandSender sender, String label, String[] args) {
        String category = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "all";
        if (!COMMAND_CATEGORIES.contains(category)) {
            sender.sendMessage(ColorUtils.toComponent("&cUnknown command category. Available: &f" + String.join(", ", COMMAND_CATEGORIES)));
            return;
        }

        int page = args.length >= 4 ? parsePage(args[3]) : 1;
        List<CommandEntry> entries = commandEntries(category, label);
        if (entries.isEmpty()) {
            sender.sendMessage(ColorUtils.toComponent("&cNo commands found for category: &f" + category));
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) COMMANDS_PER_PAGE));
        page = Math.max(1, Math.min(page, totalPages));
        int start = (page - 1) * COMMANDS_PER_PAGE;
        int end = Math.min(entries.size(), start + COMMANDS_PER_PAGE);

        sender.sendMessage(ColorUtils.toComponent("&8&m---------- &bUDS commands &8(&f" + category + " " + page + "/" + totalPages + "&8) &8&m----------"));
        for (int index = start; index < end; index++) {
            CommandEntry entry = entries.get(index);
            sender.sendMessage(ColorUtils.toComponent("&f" + entry.usage() + " &8- &7" + entry.description()));
        }
        sender.sendMessage(ColorUtils.toComponent("&7Categories: &f" + String.join("&7, &f", COMMAND_CATEGORIES)));
    }

    private List<CommandEntry> commandEntries(String category, String label) {
        if (category.equals("setup")) {
            return new java.util.ArrayList<>(java.util.Arrays.asList(
                    new CommandEntry("/" + label + " setup [status]", "show setup checklist"), 
                    new CommandEntry("/" + label + " setup apply single-paper confirm", "apply the single paper preset"), 
                    new CommandEntry("/" + label + " setup setspawn", "save your current location as spawn"), 
                    new CommandEntry("/" + label + " setup setafk", "save your current location as afk"), 
                    new CommandEntry("/" + label + " setup commands [category] [page]", "browse command usage")
            ));
        }

        Map<String, Map<String, Object>> commandMap = plugin.getDescription().getCommands();
        List<String> commandNames = commandNamesForCategory(category, commandMap.keySet());
        List<CommandEntry> entries = new ArrayList<>();
        for (String commandName : commandNames) {
            Map<String, Object> details = commandMap.get(commandName);
            if (details == null) {
                continue;
            }

            String usage = String.valueOf(details.getOrDefault("usage", "/" + commandName));
            String description = String.valueOf(details.getOrDefault("description", "no description"));
            String aliases = formatAliases(details.get("aliases"));
            if (!aliases.trim().isEmpty()) {
                description += " (aliases: " + aliases + ")";
            }
            if (!plugin.getFeatureManager().isCommandFeatureEnabled(commandName)) {
                continue;
            }
            entries.add(new CommandEntry(usage, description));
        }
        return entries;
    }

    private void sendFeatureList(CommandSender sender) {
        sender.sendMessage(ColorUtils.toComponent("&8&m---------- &bFeature toggles &8&m----------"));
        for (FeatureManager.Feature feature : plugin.getFeatureManager().getFeatures()) {
            sender.sendMessage(ColorUtils.toComponent("&f" + feature.configKey()
                    + " &8- " + plugin.getFeatureManager().statusText(feature)
                    + " &8- &7" + feature.displayName()));
        }
    }

    private List<String> commandNamesForCategory(String category, Collection<String> allCommandNames) {
        if (category.equals("all")) {
            return new ArrayList<>(allCommandNames);
        }

        switch (category) {
            case "starter":
                return new java.util.ArrayList<>(java.util.Arrays.asList(
                    "spawn",  "afk",  "home",  "homes",  "sethome",  "delhome",  "renamehome", 
                    "rtp",  "warp",  "tpa",  "tpahere",  "tpaccept",  "tpadeny",  "tpacancel", 
                     "settings",  "stats",  "ping",  "playtime",  "social",  "twitter",
                    "store",  "rules",  "help",  "servers"
            ));
            case "economy":
                return new java.util.ArrayList<>(java.util.Arrays.asList(
                    "balance",  "pay",  "addmoney",  "removemoney",  "setmoney",  "shards",  "shardpay", 
                    "addshards",  "removeshards",  "setshards", 
                    "shop",  "sell",  "sellhand",  "sellall",  "sellhistory",  "worth"
            ));
            case "market":
                return new java.util.ArrayList<>(java.util.Arrays.asList(
                    "auctionhouse",  "orders",  "billford",  "bounty",  "crate",  "crates",  "keys",  "spawner"
            ));
            case "staff":
                return new java.util.ArrayList<>(java.util.Arrays.asList(
                    "staffmode",  "stafflist",  "staffchat",  "helpop",  "report",  "freeze",  "fly",  "flyspeed", 
                    "heal",  "feed",  "gamemode",  "randomteleport",  "teleport",  "alts",  "vanish", 
                    "invsee",  "profileviewer",  "punishments",  "ban",  "tempban",  "mute",  "tempmute", 
                    "warn",  "kick",  "blacklist",  "unban",  "unmute",  "unblacklist",  "findplayer",  "rename"
            ));
            case "admin":
                return new java.util.ArrayList<>(java.util.Arrays.asList(
                    "ultimatedonutsmp",  "clearlag",  "cuboid",  "setwarp",  "delwarp",  "warpmanager", 
                     "portalmanager",  "amethysttool",  "arena",  "crate",  "spawner",
                    "shop",  "orders",  "auctionhouse",  "enderchest",  "freeze",  "staffmode",  "invsee",  "worth"
            ));
            default:
                return java.util.Collections.emptyList();
        }
    }

    private String formatAliases(Object rawAliases) {
        if (rawAliases instanceof Collection<?> aliases) {
            return aliases.stream()
                    .map(String::valueOf)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
        }
        if (rawAliases instanceof String alias && !alias.trim().isEmpty()) {
            return alias;
        }
        return "";
    }

    private void sendCheck(CommandSender sender, boolean ok, String label, String detail) {
        String state = ok ? "&aok" : "&echeck";
        sender.sendMessage(ColorUtils.toComponent("&8- " + state + " &f" + label + " &8- &7" + detail));
    }

    private boolean isJava21OrNewer() {
        String version = System.getProperty("java.version", "");
        String normalized = version.startsWith("1.") ? version.substring(2) : version;
        int dotIndex = normalized.indexOf('.');
        String major = dotIndex >= 0 ? normalized.substring(0, dotIndex) : normalized;
        try {
            return Integer.parseInt(major) >= 21;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean isPaperLikeServer() {
        String serverName = plugin.getServer().getName().toLowerCase(Locale.ROOT);
        return serverName.contains("paper") || serverName.contains("purpur") || serverName.contains("pufferfish");
    }

    private boolean isDatabaseConnected() {
        Connection connection = plugin.getDatabaseManager() == null ? null : plugin.getDatabaseManager().getConnection();
        if (connection == null) {
            return false;
        }

        try {
            return !connection.isClosed();
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean isRedisReadyForSingleServer(FileConfiguration database) {
        return !database.getBoolean("REDIS.ENABLED", false);
    }

    private String redisDetail(FileConfiguration database) {
        if (!database.getBoolean("REDIS.ENABLED", false)) {
            return "disabled for single-server setup";
        }
        return "disabled (Redis removed)";
    }

    private List<String> availableRtpWorlds() {
        ConfigurationSection worlds = plugin.getConfigManager().getRtp().getConfigurationSection("WORLD-SETTINGS");
        if (worlds == null) {
            return java.util.Collections.emptyList();
        }

        List<String> loaded = new ArrayList<>();
        for (String worldName : worlds.getKeys(false)) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                loaded.add(world.getName());
            }
        }
        loaded.sort(String.CASE_INSENSITIVE_ORDER);
        return loaded;
    }

    private String optionalIntegrationDetail() {
        List<String> installed = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String pluginName : new java.util.ArrayList<>(java.util.Arrays.asList("PlaceholderAPI",  "LuckPerms",  "Vault",  "ProtocolLib"))) {
            if (plugin.getServer().getPluginManager().getPlugin(pluginName) == null) {
                missing.add(pluginName);
            } else {
                installed.add(pluginName);
            }
        }

        String installedText = installed.isEmpty() ? "none installed" : "installed: " + String.join(", ", installed);
        if (missing.isEmpty()) {
            return installedText;
        }
        return installedText + "; optional missing: " + String.join(", ", missing);
    }

    private String describeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }
        return location.getWorld().getName()
                + " " + location.getBlockX()
                + ", " + location.getBlockY()
                + ", " + location.getBlockZ();
    }

    private int parsePage(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void sendOptimizationStatus(
            CommandSender sender,
            String label,
            OptimizationManager optimizationManager
    ) {
        sender.sendMessage(ColorUtils.toComponent("&8&m---------- &bOptimization &8&m----------"));
        sender.sendMessage(ColorUtils.toComponent("&7Enabled: &f" + optimizationManager.isEnabled()
                + " &8| &7State: " + optimizationManager.getLoadState().display()));
        sender.sendMessage(ColorUtils.toComponent("&7TPS: &f" + optimizationManager.formatMetric(optimizationManager.getLastTps())
                + " &8| &7MSPT: &f" + optimizationManager.formatMetric(optimizationManager.getLastMspt())));
        sender.sendMessage(ColorUtils.toComponent("&7Memory: &f" + optimizationManager.getUsedMemoryMb()
                + "mb&8/&f" + optimizationManager.getMaxMemoryMb() + "mb"));
        sender.sendMessage(ColorUtils.toComponent("&7Skipped task runs: &f"
                + optimizationManager.getTotalSkippedRuns()
                + " &8(&7Scoreboard=&f" + optimizationManager.getSkippedRuns(OptimizationManager.OptimizedTask.SCOREBOARD)
                + "&8, &7Tablist=&f" + optimizationManager.getSkippedRuns(OptimizationManager.OptimizedTask.TABLIST)
                + "&8)"));
        sender.sendMessage(ColorUtils.toComponent("&7Usage: &f/" + label + " optimize [status|reload|reset]"));
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " <reload|statswipe|optimize|setup|features>"));
    }

    private void sendSetupUsage(CommandSender sender, String label) {
        sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " setup <status|apply|setspawn|setafk|commands>"));
    }

    private void sendFeatureUsage(CommandSender sender, String label) {
        sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " features [list|toggle|enable|disable] [feature]"));
    }

    private String message(String key, String fallback) {
        return plugin.getConfigManager().getMessages().getString("STATS-WIPE." + key, fallback);
    }

    private boolean isGuiAlias(String input) {
        return input.equalsIgnoreCase("menu") || input.equalsIgnoreCase("gui");
    }

    private String availableTargets() {
        return Arrays.stream(StatsWipeManager.WipeTarget.values())
                .map(StatsWipeManager.WipeTarget::getDisplayName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("player stats");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partialMatches(args[0], ROOT_COMPLETIONS);
        }

        String root = args[0].toLowerCase(Locale.ROOT);
        if (root.equals("optimize") || root.equals("optimization")) {
            return args.length == 2 ? partialMatches(args[1], new java.util.ArrayList<>(java.util.Arrays.asList("status",  "reload",  "reset"))) : java.util.Collections.emptyList();
        }

        if (root.equals("features")) {
            return completeFeatures(args);
        }

        if (root.equals("maintenance")) {
            if (args.length == 2) {
                return partialMatches(args[1], new java.util.ArrayList<>(java.util.Arrays.asList("on",  "off",  "status",  "setlobby")));
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("setlobby")) {
                List<String> servers = new ArrayList<>();
                ConfigurationSection sec = plugin.getConfigManager().getNetwork().getConfigurationSection("NETWORK-STATUS.SERVERS");
                if (sec != null) {
                    servers.addAll(sec.getKeys(false));
                }
                return partialMatches(args[2], servers);
            }
            return java.util.Collections.emptyList();
        }

        if (root.equals("statswipe")) {
            if (args.length == 2) {
                List<String> targets = Arrays.stream(StatsWipeManager.WipeTarget.values())
                        .map(target -> target.getDisplayName().toLowerCase(Locale.ROOT).replace(" ", "-"))
                        .collect(java.util.stream.Collectors.toList());
                return partialMatches(args[1], targets);
            }
            return args.length == 3 ? partialMatches(args[2], java.util.Collections.singletonList("confirm")) : java.util.Collections.emptyList();
        }

        if (!root.equals("setup")) {
            return java.util.Collections.emptyList();
        }

        if (args.length == 2) {
            return partialMatches(args[1], SETUP_COMPLETIONS);
        }

        String setupSub = args[1].toLowerCase(Locale.ROOT);
        if (setupSub.equals("apply")) {
            if (args.length == 3) {
                return partialMatches(args[2], java.util.Collections.singletonList("single-paper"));
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("single-paper")) {
                return partialMatches(args[3], java.util.Collections.singletonList("confirm"));
            }
        }

        if (setupSub.equals("commands")) {
            if (args.length == 3) {
                return partialMatches(args[2], COMMAND_CATEGORIES);
            }
            if (args.length == 4) {
                return partialMatches(args[3], new java.util.ArrayList<>(java.util.Arrays.asList("1",  "2",  "3",  "4",  "5")));
            }
        }

        return java.util.Collections.emptyList();
    }

    private List<String> completeFeatures(String[] args) {
        if (args.length == 2) {
            return partialMatches(args[1], new java.util.ArrayList<>(java.util.Arrays.asList("list",  "toggle",  "enable",  "disable")));
        }

        if (args.length == 3 && new java.util.ArrayList<>(java.util.Arrays.asList("toggle",  "enable",  "disable")).contains(args[1].toLowerCase(Locale.ROOT))) {
            List<String> featureKeys = plugin.getFeatureManager().getFeatures().stream()
                    .map(feature -> feature.configKey().toLowerCase(Locale.ROOT).replace('_', '-'))
                    .collect(java.util.stream.Collectors.toList());
            return partialMatches(args[2], featureKeys);
        }

        return java.util.Collections.emptyList();
    }

    private List<String> partialMatches(String input, Collection<String> options) {
        String normalized = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(normalized))
                .collect(java.util.stream.Collectors.toList());
    }

    private void handleMaintenance(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("ultimatedonutsmp.admin.maintenance")) {
            sender.sendMessage(ColorUtils.toComponent("&cYou do not have permission to manage maintenance mode."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " maintenance <on|off|status|setlobby [server]>"));
            return;
        }

        MaintenanceManager mm = plugin.getMaintenanceManager();
        if (mm == null) {
            sender.sendMessage(ColorUtils.toComponent("&cMaintenance manager is not available."));
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "on":
            case "start":
            case "enable": {
                if (mm.isMaintenanceActive()) {
                                sender.sendMessage(ColorUtils.toComponent("&eMaintenance mode is already active."));
                                return;
                            }
                            mm.startMaintenance();
                sender.sendMessage(ColorUtils.toComponent("&aMaintenance mode has been enabled. Players are being redirected."));
                break;
                break;
            }
            case "off":
            case "stop":
            case "disable": {
                if (!mm.isMaintenanceActive()) {
                                sender.sendMessage(ColorUtils.toComponent("&eMaintenance mode is not active."));
                                return;
                            }
                            mm.stopMaintenance();
                sender.sendMessage(ColorUtils.toComponent("&aMaintenance mode has been disabled. Reconnect signal sent."));
                break;
                break;
            }
            case "status": {
                boolean active = mm.isMaintenanceActive();
                String lobby = mm.getLobbyServer();
                sender.sendMessage(ColorUtils.toComponent("&d&lMaintenance status:"));
                sender.sendMessage(ColorUtils.toComponent("  &fActive: " + (active ? "&aYes" : "&cNo")));
                sender.sendMessage(ColorUtils.toComponent("  &fLobby server: &b" + lobby));
                break;
                break;
            }
            case "setlobby": {
                if (args.length < 3) {
                                sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " maintenance setlobby <server>"));
                                return;
                            }
                            String lobby = args[2];
                mm.setLobbyServer(lobby);
                sender.sendMessage(ColorUtils.toComponent("&aLobby server set to &b" + lobby + "&a."));
                break;
                break;
            }
            default:
                sender.sendMessage(ColorUtils.toComponent("&cUsage: /" + label + " maintenance <on|off|status|setlobby [server]>"));
                break;
        }
    }

public final class CommandEntry {
    private final String usage;
    private final String description;

    public CommandEntry(String usage, String description) {
        this.usage = usage;
        this.description = description;
    }

    public String usage() { return usage; }
    public String description() { return description; }

    @Override public String toString() {
        return "CommandEntry[usage=+usage, description=+description]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandEntry that = (CommandEntry) o;
        return java.util.Objects.equals(usage, that.usage) && java.util.Objects.equals(description, that.description);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(usage, description);
    }
}
}
