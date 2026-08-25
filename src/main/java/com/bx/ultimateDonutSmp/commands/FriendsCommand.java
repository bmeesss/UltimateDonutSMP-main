package com.bx.ultimateDonutSmp.commands;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.PermissionUtils;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import com.bx.ultimateDonutSmp.menus.FriendsMenu;
import com.bx.ultimateDonutSmp.models.FollowEntry;
import com.bx.ultimateDonutSmp.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FriendsCommand implements CommandExecutor {

    private static final String PERMISSION = "ultimatedonutsmp.friends";
    private static final String COMMAND_PERMISSION = "ultimatedonutsmp.command.friends";
    private static final String FRIEND_COMMAND_PERMISSION = "ultimatedonutsmp.command.friend";
    private static final String ADMIN_PERMISSION = "donutfriends.admin";

    private final UltimateDonutSmp plugin;

    public FriendsCommand(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission(ADMIN_PERMISSION)) {
                    plugin.getConfigManager().reload();
                    sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("FRIENDS.RELOAD_SUCCESS")));
                } else {
                    sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("FRIENDS.NO_PERMISSION")));
                }
                return true;
            }
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("FRIENDS.PLAYER_ONLY")));
            return true;
        }

        if (!PermissionUtils.has(player, PERMISSION) 
                && !PermissionUtils.has(player, COMMAND_PERMISSION) 
                && !PermissionUtils.has(player, FRIEND_COMMAND_PERMISSION)) {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.NO_PERMISSION"), player));
            return true;
        }

        if (args.length == 0) {
            new FriendsMenu(plugin).open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {        case "reload": {

                            if (PermissionUtils.has(player, ADMIN_PERMISSION)) {
                                plugin.getConfigManager().reload();
                                player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.RELOAD_SUCCESS"), player));
                            } else {
                                player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.NO_PERMISSION"), player));
                            }
                        break;        }        case "list": new FriendsMenu(plugin, 0, null, FriendsMenu.FilterType.ALL).open(player)            break;        case "following": new FriendsMenu(plugin, 0, null, FriendsMenu.FilterType.FOLLOWING).open(player)            break;        case "followers": new FriendsMenu(plugin, 0, null, FriendsMenu.FilterType.FOLLOWERS).open(player)            break;        case "friends": new FriendsMenu(plugin, 0, null, FriendsMenu.FilterType.FRIENDS).open(player)            break;        case "add": case "follow": {

                            if (args.length < 2) {
                                player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.USAGE_FOLLOW"), player));
                                return true;
                            }
                            handleFollow(player, args[1]);
                        break;        }        case "remove": case "unfollow": {

                            if (args.length < 2) {
                                player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.USAGE_REMOVE"), player));
                                return true;
                            }
                            handleUnfollow(player, args[1]);
                        break;        }        case "search": {

                            if (args.length < 2) {
                                player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.USAGE_SEARCH"), player));
                                return true;
                            }
                            new FriendsMenu(plugin, 0, args[1], FriendsMenu.FilterType.ALL).open(player);
                        break;        }        default: {

                            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.UNKNOWN_SUBCOMMAND"), player));
                        break;        }
        }

        return true;
    }

    private void handleFollow(Player player, String targetName) {
        ResolvedTarget target = resolveTarget(player, targetName);
        if (target == null) {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.PLAYER_NOT_FOUND"), player));
            return;
        }

        if (player.getUniqueId().equals(target.uuid())) {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.CANNOT_FOLLOW_SELF"), player));
            return;
        }

        if (plugin.getFriendsManager().isFollowing(player.getUniqueId(), target.uuid())) {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.ALREADY_FOLLOWING", "{player}", target.name()), player));
            return;
        }

        boolean success = plugin.getFriendsManager().followPlayer(player, target.uuid(), target.name());
        if (success) {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.FOLLOW_SUCCESS", "{player}", target.name()), player));
            
            Player onlineTarget = org.bukkit.Bukkit.getPlayer(target.uuid());
            if (onlineTarget != null && onlineTarget.isOnline()) {
                PlayerData targetData = plugin.getPlayerDataManager().get(onlineTarget);
                if (targetData == null || targetData.isFollowAlertsEnabled()) {
                    String senderName = plugin.getHideManager() != null ? plugin.getHideManager().publicName(player) : player.getName();
                    onlineTarget.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessageOrDefault(
                            "FRIENDS.FOLLOW_ALERT_RECEIVED",
                            "&d&lFollow &7» &b" + senderName + " &7is now following you."
                    )));
                }
            }
        } else {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.FOLLOW_FAILURE"), player));
        }
    }

    private void handleUnfollow(Player player, String targetName) {
        ResolvedTarget target = resolveTarget(player, targetName);
        if (target == null) {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.REMOVE_NOT_FOUND"), player));
            return;
        }

        if (!plugin.getFriendsManager().isFollowing(player.getUniqueId(), target.uuid())) {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.NOT_FOLLOWING", "{player}", target.name()), player));
            return;
        }

        boolean success = plugin.getFriendsManager().unfollowPlayer(player, target.uuid());
        if (success) {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.UNFOLLOW_SUCCESS", "{player}", target.name()), player));
        } else {
            player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage("FRIENDS.UNFOLLOW_FAILURE"), player));
        }
    }

    private ResolvedTarget resolveTarget(Player player, String input) {
        if (input == null || input.isBlank()) return null;

        Player online = plugin.getHideManager().findOnlinePlayer(player, input);
        if (online != null) {
            return new ResolvedTarget(online.getUniqueId(), plugin.getHideManager().plainPublicName(online));
        }

        for (FollowEntry entry : plugin.getFriendsManager().getFollowing(player.getUniqueId())) {
            if (entry.followedNameSnapshot().equalsIgnoreCase(input) || entry.followedUuid().toString().equalsIgnoreCase(input)) {
                return new ResolvedTarget(entry.followedUuid(), entry.followedNameSnapshot());
            }
        }

        UUID uuid = plugin.getHideManager().findKnownPlayerUuid(player, input);
        if (uuid == null) return null;

        String name = plugin.getDatabaseManager().getLastKnownUsername(uuid);
        String fallback = name == null || name.isBlank() ? input : name;
        return new ResolvedTarget(uuid, plugin.getHideManager().plainPublicName(uuid, fallback));
    }

public final class ResolvedTarget {
    private final UUID uuid;
    private final String name;

    public ResolvedTarget(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }

    @Override public String toString() {
        return "ResolvedTarget[uuid=+uuid, name=+name]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedTarget that = (ResolvedTarget) o;
        return java.util.Objects.equals(uuid, that.uuid) && java.util.Objects.equals(name, that.name);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(uuid, name);
    }
}
}
