package com.bx.ultimateDonutSmp.commands;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.managers.HideManager;
import com.bx.ultimateDonutSmp.menus.DisguiseAliasMenu;
import com.bx.ultimateDonutSmp.menus.DisguiseSkinMenu;
import com.bx.ultimateDonutSmp.menus.HideListMenu;
import com.bx.ultimateDonutSmp.menus.HideMenu;
import com.bx.ultimateDonutSmp.models.HideState;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import com.bx.ultimateDonutSmp.utils.PermissionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HideCommand implements CommandExecutor, TabCompleter {

    private final UltimateDonutSmp plugin;

    public HideCommand(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        HideManager manager = plugin.getHideManager();
        if (label.equalsIgnoreCase("disguise")) {
            return handleDisguise(sender, args, manager);
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                send(sender, manager.message("PLAYER-ONLY", "&cOnly players can use this command."));
                return true;
            }
            Player player = (Player) sender;
            new HideMenu(plugin).open(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("status") || sub.equals("check")) {
            return handleStatus(sender, args, manager);
        } else if (sub.equals("scramble")) {
            return handleScramble(sender, manager);
        } else if (sub.equals("remove") || sub.equals("removal")) {
            return handleRemove(sender, args, manager);
        } else if (sub.equals("list")) {
            return handleList(sender, manager);
        } else {
            send(sender, "&cUsage: /hide [status|scramble|remove|check <player>|list]");
            return true;
        }
    }

    private boolean handleDisguise(CommandSender sender, String[] args, HideManager manager) {
        if (!(sender instanceof Player)) {
            send(sender, manager.message("PLAYER-ONLY", "&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            new DisguiseAliasMenu(plugin, 0).open(player);
            return true;
        }
        if (args.length == 1) {
            send(player, manager.message("SKIN-SEARCHING", "&7Searching skin for &f{skin}&7...",
                    "{skin}", args[0]));
            if (HideManager.isSkinUrl(args[0])) {
                manager.disguiseWithScrambledAlias(player, args[0], result -> sendResult(player, result));
            } else {
                manager.disguise(player, args[0], args[0], result -> sendResult(player, result));
            }
            return true;
        }
        send(player, manager.message("SKIN-SEARCHING", "&7Searching skin for &f{skin}&7...",
                "{skin}", args[1]));
        manager.disguise(player, args[0], args[1], result -> sendResult(player, result));
        return true;
    }

    private boolean handleStatus(CommandSender sender, String[] args, HideManager manager) {
        if (args.length > 1) {
            if (!PermissionUtils.has(sender, HideManager.ADMIN_PERMISSION)) {
                send(sender, manager.message("NO-PERMISSION", "&cYou do not have permission."));
                return true;
            }
            HideState state = manager.findState(args[1]);
            if (state == null) {
                send(sender, manager.message("NOT-HIDDEN", "&cThat player is not hidden."));
                return true;
            }
            send(sender, manager.message(
                    "CHECK",
                    "&bHide check\n&7Real name: &f{real}\n&7Alias: &f{alias}\n&7Mode: &f{mode}\n&7Skin: &f{skin}",
                    "{real}", state.realNameSnapshot(),
                    "{alias}", state.alias(),
                    "{mode}", state.mode().name(),
                    "{skin}", state.skinUsername().trim().isEmpty() ? "Original" : state.skinUsername()
            ));
            return true;
        }

        if (!(sender instanceof Player)) {
            send(sender, "&cUsage: /hide check <player>");
            return true;
        }
        Player player = (Player) sender;
        HideState state = manager.getState(player.getUniqueId());
        if (state == null) {
            send(player, manager.message("STATUS-NONE", "&7Hide status: &cinactive"));
        } else {
            send(player, manager.message(
                    "STATUS-ACTIVE",
                    "&7Hide status: &a{mode} &8- &f{alias}",
                    "{mode}", state.mode().name(),
                    "{alias}", manager.publicName(state)
            ));
        }
        return true;
    }

    private boolean handleScramble(CommandSender sender, HideManager manager) {
        if (!(sender instanceof Player)) {
            send(sender, manager.message("PLAYER-ONLY", "&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;
        sendResult(player, manager.scramble(player));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args, HideManager manager) {
        if (args.length > 1) {
            if (!PermissionUtils.has(sender, HideManager.ADMIN_PERMISSION)) {
                send(sender, manager.message("NO-PERMISSION", "&cYou do not have permission."));
                return true;
            }
            HideState state = manager.findState(args[1]);
            if (state == null) {
                send(sender, manager.message("NOT-HIDDEN", "&cThat player is not hidden."));
                return true;
            }
            HideManager.Result result = manager.remove(state.playerUuid());
            if (result.success()) {
                send(sender, manager.message(
                        "ADMIN-REMOVED",
                        "&aSuccessfully removed hide from &f{player}&a.",
                        "{player}", state.realNameSnapshot()
                ));
                Player online = plugin.getServer().getPlayer(state.playerUuid());
                if (online != null) {
                    send(online, manager.message("REMOVED-BY-ADMIN",
                            "&cYour hide state has been removed by an administrator."));
                }
            } else {
                sendResult(sender, result);
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            send(sender, "&cUsage: /hide remove <player>");
            return true;
        }
        Player player = (Player) sender;
        sendResult(player, manager.remove(player, false));
        return true;
    }

    private boolean handleList(CommandSender sender, HideManager manager) {
        if (!PermissionUtils.has(sender, HideManager.ADMIN_PERMISSION)) {
            send(sender, manager.message("NO-PERMISSION", "&cYou do not have permission."));
            return true;
        }
        if (!(sender instanceof Player)) {
            for (HideState state : manager.getStates()) {
                sender.sendMessage(state.realNameSnapshot() + " -> " + state.alias()
                        + " (" + state.mode().name() + ")");
            }
            return true;
        }
        Player player = (Player) sender;
        new HideListMenu(plugin, 0).open(player);
        return true;
    }

    public void sendResult(CommandSender sender, HideManager.Result result) {
        HideManager manager = plugin.getHideManager();
        switch (result.type()) {
            case SUCCESS:
                if (result.state() == null) {
                    return;
                }
                String key = result.state().mode() == com.bx.ultimateDonutSmp.models.HideMode.SCRAMBLE
                        ? "SCRAMBLED"
                        : "DISGUISED";
                if (!manager.isHidden(result.state().playerUuid())) {
                    key = "REMOVED";
                }
                send(sender, manager.message(
                        key,
                        key.equals("removed")
                                ? "&aYour hide state has been removed."
                                : "&aYour identity is now &f{alias}&a.",
                        "{alias}", manager.publicName(result.state())
                ));
                break;
            case DISABLED:
                send(sender, manager.message("DISABLED", "&cThe hide feature is disabled."));
                break;
            case DEPENDENCY_MISSING:
                send(sender, manager.message("DEPENDENCY-MISSING", "&cHide requires ProtocolLib."));
                break;
            case NO_PERMISSION:
                send(sender, manager.message("NO-PERMISSION", "&cYou do not have permission."));
                break;
            case IN_COMBAT:
                send(sender, manager.message("IN-COMBAT", "&cYou cannot change hide in combat."));
                break;
            case COOLDOWN:
                send(sender, manager.message(
                        "COOLDOWN", "&cWait &f{seconds}s &cbefore changing hide again.",
                        "{seconds}", String.valueOf(result.remainingSeconds())
                ));
                break;
            case INVALID_ALIAS:
                send(sender, manager.message("INVALID-ALIAS", "&cInvalid alias."));
                break;
            case INVALID_SKIN:
                send(sender, manager.message("INVALID-SKIN", "&cInvalid skin."));
                break;
            case ALIAS_IN_USE:
                send(sender, manager.message("ALIAS-IN-USE", "&cThat alias is already in use."));
                break;
            case NOT_HIDDEN:
                send(sender, manager.message("NOT-HIDDEN", "&cThat player is not hidden."));
                break;
            case DATABASE_ERROR:
                send(sender, "&cUnable to save hide state.");
                break;
        }
    }

    private void send(CommandSender sender, String message) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage(ColorUtils.toComponent(message, player));
        } else {
            sender.sendMessage(ColorUtils.colorize(message));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        HideManager manager = plugin.getHideManager();
        if (command.getName().equalsIgnoreCase("disguise")) {
            if (args.length == 1) {
                List<String> values = new ArrayList<>(manager.aliases().keySet());
                values.addAll(manager.skins().values().stream()
                        .map(HideManager.SkinOption::username)
                        .collect(java.util.stream.Collectors.toList()));
                return filter(values, args[0]);
            }
            if (args.length == 2) {
                List<String> values = new ArrayList<>(manager.skins().keySet());
                values.addAll(manager.skins().values().stream()
                        .map(HideManager.SkinOption::username)
                        .collect(java.util.stream.Collectors.toList()));
                return filter(values, args[1]);
            }
            return java.util.Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> values = new ArrayList<>(new java.util.ArrayList<>(java.util.Arrays.asList("status",  "scramble",  "remove")));
            if (PermissionUtils.has(sender, HideManager.ADMIN_PERMISSION)) {
                values.add("check");
                values.add("list");
                values.add("removal");
            }
            return filter(values, args[0]);
        }
        if (args.length == 2 && PermissionUtils.has(sender, HideManager.ADMIN_PERMISSION)
                && new java.util.LinkedHashSet<>(java.util.Arrays.asList("check",  "remove",  "removal")).contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> values = manager.getStates().stream()
                    .flatMap(state -> java.util.stream.Stream.of(state.alias(), state.realNameSnapshot()))
                    .collect(java.util.stream.Collectors.toList());
            return filter(values, args[1]);
        }
        return java.util.Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(java.util.stream.Collectors.toList());
    }

    private String normalizeKey(String value) {
        return HideManager.normalize(value).replace('-', '_').replace(' ', '_');
    }
}
