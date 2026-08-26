package com.bx.ultimateDonutSmp.commands;

import com.bx.ultimateDonutSmp.utils.PermissionUtils;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.managers.FeatureManager;
import com.bx.ultimateDonutSmp.managers.WorthManager;
import com.bx.ultimateDonutSmp.models.AuctionListing;
import com.bx.ultimateDonutSmp.models.Home;
import com.bx.ultimateDonutSmp.models.PlayerData;
import com.bx.ultimateDonutSmp.models.SpawnerTypeDefinition;
import com.bx.ultimateDonutSmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class UniversalCommandTabCompleter implements TabCompleter {

    private static final List<String> AMOUNTS = new java.util.ArrayList<>(java.util.Arrays.asList(
            "1",  "10",  "100",  "1K",  "10K",  "100K",  "1M",  "10M",  "100M",  "1B"
    ));
    private static final List<String> DURATIONS = new java.util.ArrayList<>(java.util.Arrays.asList("30s",  "15m",  "1h",  "2h",  "1d",  "7d"));
    private static final List<String> TOGGLES = new java.util.ArrayList<>(java.util.Arrays.asList("true",  "false",  "on",  "off"));
    static final List<String> TEAM_SUBCOMMANDS = new java.util.ArrayList<>(java.util.Arrays.asList(
            "create",  "disband",  "invite",  "join",  "leave",  "kick",  "home",  "sethome",  "delhome",  "chat",  "info",  "pvp"
    ));
    private static final List<String> ARENA_SUBCOMMANDS = new java.util.ArrayList<>(java.util.Arrays.asList(
            "create",  "delete",  "setpos1",  "setpos2",  "setreturn",  "setdisplay",  "enable",  "disable",  "queue",  "list",  "reload"
    ));
    private static final List<String> CUBOID_SUBCOMMANDS = new java.util.ArrayList<>(java.util.Arrays.asList(
            "wand",  "create",  "save",  "delete",  "list",  "bind",  "system",  "reload"
    ));
    private static final List<String> CUBOID_ROLES = new java.util.ArrayList<>(java.util.Arrays.asList("spawn",  "shard",  "rtp-zone"));
    private static final List<String> TELEPORT_ROOTS = new java.util.ArrayList<>(java.util.Arrays.asList("here",  "all",  "top"));

    private final UltimateDonutSmp plugin;

    public UniversalCommandTabCompleter(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.testPermissionSilent(sender) || !isFeatureEnabled(command.getName())) {
            return java.util.Collections.emptyList();
        }

        String commandName = normalize(command.getName());
        String label = normalize(alias);
        return switch (commandName) {        case "team": completeTeam(sender, args)            break;        case "home": case "homes": case "sethome": case "delhome": case "renamehome": completeHome(sender, commandName, args)            break;        case "rtp": singleArg(args, plugin.getRtpManager().getPortalSelectorSuggestions())            break;        case "shop": completeShop(sender, args)            break;        case "shardshop": java.util.Collections.emptyList()            break;        case "safety": completeSafety(sender, args)            break;                case "orders": completeOrders(sender, args)            break;
        case "auctionhouse": completeAuctionHouse(sender, args)            break;        case "enderchest": completeReloadOnly(sender, args, "ultimatedonutsmp.admin.enderchest")            break;        case "ecsee": completeEcsee(sender, args)            break;        case "sellhand": singleArg(args, AMOUNTS)            break;        case "worth": completeWorth(sender, args)            break;        case "balance": case "stats": case "playtime": case "alts": case "profileviewer": case "seehomes": case "punishments": case "logs": completeKnownPlayer(args, sender, true)            break;        case "ping": case "findplayer": completeOnlinePlayer(args, sender, true)            break;        case "pay": case "shardpay": completePayment(sender, args)            break;        case "addmoney": case "removemoney": case "setmoney": case "addshards": case "removeshards": case "setshards": completeMoneyAdmin(sender, args)            break;        case "shards": completeShards(sender, args)            break;        case "freeze": completePlayerOrReload(sender, args, "ultimatedonutsmp.admin.freeze", false)            break;        case "fly": case "heal": case "feed": completeOnlinePlayer(args, sender, true)            break;        case "flyspeed": completeFlySpeed(args, sender)            break;        case "staffmode": completePlayerOrReload(sender, args, "ultimatedonutsmp.admin.staffmode", true)            break;        case "helpop": case "staffchat": java.util.Collections.emptyList()            break;        case "rename": singleArg(args, new java.util.ArrayList<>(java.util.Arrays.asList("reset",  "clear",  "remove")))            break;
            case "randomteleport", "leave", "draw", "pm", "spawn", "afk", "sell", "sellall", "sellhistory",
                    "stafflist", "vanish", "tpauto", "tpahereauto", "nightvision", "phantom", "settings",
                     "twitter", "store", "social", "rules", "help", "servers", "billford",
                    "clearlag", "crates", "keys" -> java.util.Collections.emptyList();        case "teleport": completeTeleport(sender, label, args)            break;        case "invsee": completePlayerOrReload(sender, args, "ultimatedonutsmp.admin.invsee", false)            break;
            case "ban", "tempban", "mute", "tempmute", "warn", "kick", "blacklist", "unban", "unmute",
                    "unblacklist" -> completePunishment(sender, commandName, args);        case "bounty": completeBounty(sender, args)            break;        case "tpa": case "tpahere": completeOnlinePlayer(args, sender, false)            break;        case "tpaccept": case "tpadeny": completeOnlinePlayer(args, sender, true)            break;        case "leaderboard": singleArg(args, leaderboardTypes())            break;        case "spawner": completeSpawner(sender, args)            break;        case "cuboid": completeCuboid(sender, args)            break;        default: java.util.Collections.emptyList()            break;
        };
    }

    private List<String> completeTeam(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return java.util.Collections.emptyList();
        }
        if (args.length == 1) {
            return partial(args[0], TEAM_SUBCOMMANDS);
        }
        if (args.length != 2) {
            return java.util.Collections.emptyList();
        }

        return switch (normalize(args[0])) {        case "invite": partial(args[1], onlinePlayerNames(sender, false))            break;        case "join": partial(args[1], plugin.getTeamManager().getPendingInvites(player.getUniqueId()))            break;        case "kick": partial(args[1], teamMemberNames(player, false))            break;        case "info": partial(args[1], teamNames())            break;        default: java.util.Collections.emptyList()            break;
        };
    }

    private List<String> teamNames() {
        return plugin.getTeamManager().getAllTeams().stream()
                .map(Team::getName)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<String> completeHome(CommandSender sender, String commandName, String[] args) {
        if (!(sender instanceof Player player)) {
            return java.util.Collections.emptyList();
        }
        if (args.length != 1) {
            return java.util.Collections.emptyList();
        }
        return switch (commandName) {        case "home": case "delhome": case "renamehome": partial(args[0], homeNames(player))            break;        default: java.util.Collections.emptyList()            break;
        };
    }

    private List<String> completeOrders(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return java.util.Collections.emptyList();
        }
        List<String> options = new ArrayList<>(new java.util.ArrayList<>(java.util.Arrays.asList("browse",  "my",  "collect")));
        if (has(sender, "ultimatedonutsmp.admin.orders")) {
            options.add("reload");
        }
        return partial(args[0], options);
    }

    private List<String> completeOrders(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(new java.util.ArrayList<>(java.util.Arrays.asList("sell",  "my",  "claims",  "cancel")));
            if (has(sender, "ultimatedonutsmp.admin.auctionhouse")) {
                options.add("reload");
            }
            return partial(args[0], options);
        }
        if (args.length == 2 && normalize(args[0]).equals("sell")) {
            return partial(args[1], AMOUNTS);
        }
        if (args.length == 2 && normalize(args[0]).equals("cancel") && sender instanceof Player) {
            Player player = (Player) !command.testPermissionSilent(sender) || !isFeatureEnabled(command.getName())) {
            return java.util.Collections.emptyList();
        }

        String commandName = normalize(command.getName());
        String label = normalize(alias);
        return switch (commandName) {        case "team": completeTeam(sender, args)            break;        case "home": case "homes": case "sethome": case "delhome": case "renamehome": completeHome(sender, commandName, args)            break;        case "rtp": singleArg(args, plugin.getRtpManager().getPortalSelectorSuggestions())            break;        case "shop": completeShop(sender, args)            break;        case "shardshop": java.util.Collections.emptyList()            break;        case "safety": completeSafety(sender, args)            break;                case "orders": completeOrders(sender, args)            break;
        case "auctionhouse": completeAuctionHouse(sender, args)            break;        case "enderchest": completeReloadOnly(sender, args, "ultimatedonutsmp.admin.enderchest")            break;        case "ecsee": completeEcsee(sender, args)            break;        case "sellhand": singleArg(args, AMOUNTS)            break;        case "worth": completeWorth(sender, args)            break;        case "balance": case "stats": case "playtime": case "alts": case "profileviewer": case "seehomes": case "punishments": case "logs": completeKnownPlayer(args, sender, true)            break;        case "ping": case "findplayer": completeOnlinePlayer(args, sender, true)            break;        case "pay": case "shardpay": completePayment(sender, args)            break;        case "addmoney": case "removemoney": case "setmoney": case "addshards": case "removeshards": case "setshards": completeMoneyAdmin(sender, args)            break;        case "shards": completeShards(sender, args)            break;        case "freeze": completePlayerOrReload(sender, args, "ultimatedonutsmp.admin.freeze", false)            break;        case "fly": case "heal": case "feed": completeOnlinePlayer(args, sender, true)            break;        case "flyspeed": completeFlySpeed(args, sender)            break;        case "staffmode": completePlayerOrReload(sender, args, "ultimatedonutsmp.admin.staffmode", true)            break;        case "helpop": case "staffchat": java.util.Collections.emptyList()            break;        case "rename": singleArg(args, new java.util.ArrayList<>(java.util.Arrays.asList("reset",  "clear",  "remove")))            break;
            case "randomteleport", "leave", "draw", "pm", "spawn", "afk", "sell", "sellall", "sellhistory",
                    "stafflist", "vanish", "tpauto", "tpahereauto", "nightvision", "phantom", "settings",
                     "twitter", "store", "social", "rules", "help", "servers", "billford",
                    "clearlag", "crates", "keys" -> java.util.Collections.emptyList();        case "teleport": completeTeleport(sender, label, args)            break;        case "invsee": completePlayerOrReload(sender, args, "ultimatedonutsmp.admin.invsee", false)            break;
            case "ban", "tempban", "mute", "tempmute", "warn", "kick", "blacklist", "unban", "unmute",
                    "unblacklist" -> completePunishment(sender, commandName, args);        case "bounty": completeBounty(sender, args)            break;        case "tpa": case "tpahere": completeOnlinePlayer(args, sender, false)            break;        case "tpaccept": case "tpadeny": completeOnlinePlayer(args, sender, true)            break;        case "leaderboard": singleArg(args, leaderboardTypes())            break;        case "spawner": completeSpawner(sender, args)            break;        case "cuboid": completeCuboid(sender, args)            break;        default: java.util.Collections.emptyList()            break;
        };
    }

    private List<String> completeTeam(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return java.util.Collections.emptyList();
        }
        if (args.length == 1) {
            return partial(args[0], TEAM_SUBCOMMANDS);
        }
        if (args.length != 2) {
            return java.util.Collections.emptyList();
        }

        return switch (normalize(args[0])) {        case "invite": partial(args[1], onlinePlayerNames(sender, false))            break;        case "join": partial(args[1], plugin.getTeamManager().getPendingInvites(player.getUniqueId()))            break;        case "kick": partial(args[1], teamMemberNames(player, false))            break;        case "info": partial(args[1], teamNames())            break;        default: java.util.Collections.emptyList()            break;
        };
    }

    private List<String> teamNames() {
        return plugin.getTeamManager().getAllTeams().stream()
                .map(Team::getName)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<String> completeHome(CommandSender sender, String commandName, String[] args) {
        if (!(sender instanceof Player player)) {
            return java.util.Collections.emptyList();
        }
        if (args.length != 1) {
            return java.util.Collections.emptyList();
        }
        return switch (commandName) {        case "home": case "delhome": case "renamehome": partial(args[0], homeNames(player))            break;        default: java.util.Collections.emptyList()            break;
        };
    }

    private List<String> completeOrders(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return java.util.Collections.emptyList();
        }
        List<String> options = new ArrayList<>(new java.util.ArrayList<>(java.util.Arrays.asList("browse",  "my",  "collect")));
        if (has(sender, "ultimatedonutsmp.admin.orders")) {
            options.add("reload");
        }
        return partial(args[0], options);
    }

    private List<String> completeOrders(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(new java.util.ArrayList<>(java.util.Arrays.asList("sell",  "my",  "claims",  "cancel")));
            if (has(sender, "ultimatedonutsmp.admin.auctionhouse")) {
                options.add("reload");
            }
            return partial(args[0], options);
        }
        if (args.length == 2 && normalize(args[0]).equals("sell")) {
            return partial(args[1], AMOUNTS);
        }
        if (args.length == 2 && normalize(args[0]).equals("cancel") && sender;
            return partial(args[1], ownAuctionListingIds(player));
        }
        return java.util.Collections.emptyList();
    }

    private List<String> completeWorth(CommandSender sender, String[] args) {
        if (args == null || args.length == 0) {
            return java.util.Collections.emptyList();
        }

        List<WorthManager.WorthBrowserEntry> entries = plugin.getWorthManager().getBrowserEntries();
        List<String> prettifiedNames = entries.stream()
                .map(WorthManager.WorthBrowserEntry::material)
                .filter(mat -> mat != null && !mat.isAir())
                .map(mat -> plugin.getWorthManager().prettifyMaterial(mat))
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.addAll(new java.util.ArrayList<>(java.util.Arrays.asList("hand",  "held",  "item",  "check",  "browse",  "prices")));
            if (has(sender, "ultimatedonutsmp.admin.worth")) {
                options.add("reload");
            }
            options.addAll(prettifiedNames);
            return partial(args[0], options);
        }

        boolean isFirstArgAmount = false;
        try {
            int amount = Integer.parseInt(args[0]);
            if (amount > 0) {
                isFirstArgAmount = true;
            }
        } catch (NumberFormatException ignored) {
        }

        int itemStartIndex = isFirstArgAmount ? 1 : 0;
        if (args.length <= itemStartIndex) {
            return java.util.Collections.emptyList();
        }

        if (isFirstArgAmount && args.length == 2) {
            return partial(args[1], prettifiedNames);
        }

        String itemTypedSoFar = String.join(" ", java.util.Arrays.copyOfRange(args, itemStartIndex, args.length)).toLowerCase(Locale.ROOT);
        String itemTypedPrefixBeforeLast = String.join(" ", java.util.Arrays.copyOfRange(args, itemStartIndex, args.length - 1)).toLowerCase(Locale.ROOT);
        String lastArg = args[args.length - 1];

        List<String> suggestions = new ArrayList<>();
        for (String fullName : prettifiedNames) {
            String lowerFull = fullName.toLowerCase(Locale.ROOT);
            if (lowerFull.startsWith(itemTypedSoFar)) {
                suggestions.add(fullName);
            }
            if (!itemTypedPrefixBeforeLast.isEmpty() && lowerFull.startsWith(itemTypedPrefixBeforeLast + " ")) {
                String remainder = fullName.substring(itemTypedPrefixBeforeLast.length() + 1);
                suggestions.add(remainder);
            }
        }

        return partial(lastArg, suggestions);
    }

    private List<String> completePayment(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return partial(args[0], knownPlayerNames(sender, false));
        }
        if (args.length == 2) {
            return partial(args[1], AMOUNTS);
        }
        return java.util.Collections.emptyList();
    }

    private List<String> completeMoneyAdmin(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return partial(args[0], knownPlayerNames(sender, true));
        }
        if (args.length == 2) {
            return partial(args[1], AMOUNTS);
        }
        return java.util.Collections.emptyList();
    }

    private List<String> completeShards(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(knownPlayerNames(sender, true));
            if (has(sender, "ultimatedonutsmp.admin.shards")) {
                options.add("everywhere");
            }
            return partial(args[0], options);
        }
        if (!normalize(args[0]).equals("everywhere") || !has(sender, "ultimatedonutsmp.admin.shards")) {
            return java.util.Collections.emptyList();
        }
        if (args.length == 2) {
            return partial(args[1], new java.util.ArrayList<>(java.util.Arrays.asList("status",  "debug")));
        }
        if (args.length == 3) {
            return partial(args[2], onlinePlayerNames(sender, true));
        }
        return java.util.Collections.emptyList();
    }

    private List<String> completePlayerOrReload(CommandSender sender, String[] args, String adminPermission, boolean includeSelf) {
        if (args.length != 1) {
            return java.util.Collections.emptyList();
        }
        List<String> options = new ArrayList<>(onlinePlayerNames(sender, includeSelf));
        if (has(sender, adminPermission)) {
            options.add("reload");
        }
        return partial(args[0], options);
    }

    private List<String> completeEcsee(CommandSender sender, String[] args) {
        if (args.length != 1
                || !plugin.getEnderChestManager().isInspectionEnabled()
                || !plugin.getEnderChestManager().canInspect(sender)) {
            return java.util.Collections.emptyList();
        }
        return partial(args[0], plugin.getEnderChestManager().getInspectionTargetSuggestions());
    }

    private List<String> completeTeleport(CommandSender sender, String label, String[] args) {
        if (label.equals("tphere")) {
            return completeOnlinePlayer(args, sender, false);
        }
        if (label.equals("tpall")) {
            return java.util.Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>(onlinePlayerNames(sender, true));
            options.addAll(TELEPORT_ROOTS);
            return partial(args[0], options);
        }
        if (args.length == 2 && normalize(args[0]).equals("here")) {
            return partial(args[1], onlinePlayerNames(sender, false));
        }
        if (args.length == 4 && !normalize(args[0]).equals("here")) {
            return partial(args[3], worldNames());
        }
        return java.util.Collections.emptyList();
    }

    private List<String> completePunishment(CommandSender sender, String commandName, String[] args) {
        if (args.length == 1) {
            boolean onlineOnly = commandName.equals("kick");
            return onlineOnly
                    ? partial(args[0], onlinePlayerNames(sender, true))
                    : partial(args[0], knownPlayerNames(sender, true));
        }
        if (args.length == 2 && (commandName.equals("tempban") || commandName.equals("tempmute"))) {
            return partial(args[1], DURATIONS);
        }
        return java.util.Collections.emptyList();
    }

    private List<String> completeBounty(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return partial(args[0], new java.util.ArrayList<>(java.util.Arrays.asList("add",  "set",  "info",  "list")));
        }
        if (args.length == 2 && new java.util.LinkedHashSet<>(java.util.Arrays.asList("add",  "set",  "info")).contains(normalize(args[0]))) {
            return partial(args[1], knownPlayerNames(sender, false));
        }
        if (args.length == 3 && new java.util.LinkedHashSet<>(java.util.Arrays.asList("add",  "set")).contains(normalize(args[0]))) {
            return partial(args[2], AMOUNTS);
        }
        return java.util.Collections.emptyList();
    }

    private List<String> completeSpawner(CommandSender sender, String[] args) {
        boolean admin = has(sender, "ultimatedonutsmp.admin.spawner");
        if (args.length == 1) {
            List<String> options = new ArrayList<>(new java.util.ArrayList<>(java.util.Arrays.asList("info",  "split")));
            if (admin) {
                options.addAll(new java.util.ArrayList<>(java.util.Arrays.asList("give",  "panel",  "remove",  "forcebreak",  "reload")));
            }
            return partial(args[0], options);
        }
        if (normalize(args[0]).equals("split")) {
            if (args.length == 2) {
                return partial(args[1], new java.util.ArrayList<>(java.util.Arrays.asList("1",  "5",  "10",  "32",  "64")));
            }
            return java.util.Collections.emptyList();
        }
        if (!admin || !normalize(args[0]).equals("give")) {
            return java.util.Collections.emptyList();
        }
        if (args.length == 2) {
            return partial(args[1], onlinePlayerNames(sender, true));
        }
        if (args.length == 3) {
            return partial(args[2], spawnerTypeKeys());
        }
        if (args.length == 4) {
            return partial(args[3], AMOUNTS);
        }
        return java.util.Collections.emptyList();
    }

    private List<String> completeCuboid(CommandSender sender, String[] args) {
        if (!has(sender, "ultimatedonutsmp.admin.cuboid")) {
            return java.util.Collections.emptyList();
        }
        if (args.length == 1) {
            return partial(args[0], CUBOID_SUBCOMMANDS);
        }
        if (args.length == 2 && new java.util.LinkedHashSet<>(java.util.Arrays.asList("delete",  "bind",  "system")).contains(normalize(args[0]))) {
            return partial(args[1], cuboidNames());
        }
        if (args.length == 3 && new java.util.LinkedHashSet<>(java.util.Arrays.asList("bind",  "system")).contains(normalize(args[0]))) {
            return partial(args[2], CUBOID_ROLES);
        }
        if (args.length == 4 && new java.util.LinkedHashSet<>(java.util.Arrays.asList("bind",  "system")).contains(normalize(args[0]))) {
            return partial(args[3], TOGGLES);
        }
        return java.util.Collections.emptyList();
    }

    private List<String> completeReloadOnly(CommandSender sender, String[] args, String permission) {
        return args.length == 1 && has(sender, permission) ? partial(args[0], java.util.Collections.singletonList("reload")) : java.util.Collections.emptyList();
    }

    private List<String> completeShop(CommandSender sender, String[] args) {
        return completeReloadOnly(sender, args, "ultimatedonutsmp.admin.shop");
    }

    private List<String> completeOnlinePlayer(String[] args, CommandSender sender, boolean includeSelf) {
        return args.length == 1 ? partial(args[0], onlinePlayerNames(sender, includeSelf)) : java.util.Collections.emptyList();
    }

    private List<String> completeFlySpeed(String[] args, CommandSender sender) {
        if (args.length == 1) {
            return partial(args[0], new java.util.ArrayList<>(java.util.Arrays.asList("1",  "2",  "3",  "4",  "5",  "6",  "7",  "8",  "9",  "10")));
        }
        if (args.length == 2) {
            return partial(args[1], onlinePlayerNames(sender, true));
        }
        return java.util.Collections.emptyList();
    }

    private List<String> completeKnownPlayer(String[] args, CommandSender sender, boolean includeSelf) {
        return args.length == 1 ? partial(args[0], knownPlayerNames(sender, includeSelf)) : java.util.Collections.emptyList();
    }

    private List<String> singleArg(String[] args, Collection<String> options) {
        return args.length == 1 ? partial(args[0], options) : java.util.Collections.emptyList();
    }

    private List<String> onlinePlayerNames(CommandSender sender, boolean includeSelf) {
        UUID senderUuid = sender instanceof Player player ? player.getUniqueId() : null;
        List<String> names = new ArrayList<>();
        for (String name : plugin.getHideManager().onlineNames(sender)) {
            Player player = plugin.getHideManager().findOnlinePlayer(sender, name);
            if (player == null) {
                continue;
            }
            if (!includeSelf && senderUuid != null && senderUuid.equals(player.getUniqueId())) {
                continue;
            }
            names.add(name);
        }
        return names;
    }

    private List<String> knownPlayerNames(CommandSender sender, boolean includeSelf) {
        UUID senderUuid = sender instanceof Player player ? player.getUniqueId() : null;
        Set<String> names = new LinkedHashSet<>(onlinePlayerNames(sender, includeSelf));
        if (plugin.getPlayerDataManager() == null) {
            return new ArrayList<>(names);
        }
        for (PlayerData data : plugin.getPlayerDataManager().getAll()) {
            if (data == null || data.getUsername() == null || data.getUsername().isBlank()) {
                continue;
            }
            if (!includeSelf && senderUuid != null && senderUuid.equals(data.getUuid())) {
                continue;
            }
            names.add(data.getUsername());
        }
        return new ArrayList<>(names);
    }

    private List<String> homeNames(Player player) {
        if (plugin.getHomeManager() == null) {
            return java.util.Collections.emptyList();
        }
        return plugin.getHomeManager().getHomes(player.getUniqueId()).stream()
                .map(Home::getName)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<String> teamMemberNames(Player player, boolean includeSelf) {
        Team team = plugin.getTeamManager().getTeam(player);
        if (team == null) {
            return java.util.Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (UUID uuid : team.getMemberUuids()) {
            if (!includeSelf && player.getUniqueId().equals(uuid)) {
                continue;
            }
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                names.add(online.getName());
                continue;
            }
            String knownName = plugin.getDatabaseManager().getLastKnownUsername(uuid);
            if (knownName != null && !knownName.isBlank()) {
                names.add(knownName);
            }
        }
        return names;
    }

    private List<String> ownAuctionListingIds(Player player) {
        return plugin.getAuctionHouseManager()
                .getActiveListingsForSeller(player.getUniqueId(), plugin.getAuctionHouseManager().getDefaultSort())
                .stream()
                .map(AuctionListing::id)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<String> leaderboardTypes() {
        return plugin.getLeaderboardManager().getTypes().stream()
                .map(type -> type.getConfigKey().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toList());
    }

    private List<String> spawnerTypeKeys() {
        return plugin.getSpawnerManager().getTypeDefinitions().stream()
                .map(SpawnerTypeDefinition::key)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<String> cuboidNames() {
        return new ArrayList<>(plugin.getCuboidManager().getCuboidNames());
    }

    private List<String> worldNames() {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(java.util.stream.Collectors.toList());
    }

    private boolean isFeatureEnabled(String commandName) {
        if (plugin.getFeatureManager() == null) {
            return true;
        }
        for (FeatureManager.Feature feature : FeatureManager.featuresForCommand(commandName)) {
            if (feature != null && !plugin.getFeatureManager().isEnabled(feature)) {
                return false;
            }
        }
        return true;
    }

    private List<String> completeSafety(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (has(sender, "safety.reload")) {
                options.add("reload");
            }
            if (has(sender, "safety.add")) {
                options.add("add");
                options.add("give");
            }
            return partial(args[0], options);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("give")) {
                if (has(sender, "safety.add")) {
                    return partial(args[1], onlinePlayerNames(sender, true));
                }
            }
        }
        return java.util.Collections.emptyList();
    }

    private boolean has(CommandSender sender, String permission) {
        return permission == null || permission.isBlank() || PermissionUtils.has(sender, permission);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private List<String> partial(String input, Collection<String> options) {
        if (options == null || options.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option != null && !option.isBlank())
                .distinct()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(java.util.stream.Collectors.toList());
    }
}
