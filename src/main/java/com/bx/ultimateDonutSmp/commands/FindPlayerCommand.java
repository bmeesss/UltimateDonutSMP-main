package com.bx.ultimateDonutSmp.commands;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class FindPlayerCommand implements CommandExecutor {

    private static final String DEFAULT_AFK = "&7{player}'s in the &#A303F9afk";
    private static final String DEFAULT_RTP_ZONE = "&7{player}'s in the &crtpzone";
    private static final String DEFAULT_SPAWN = "&7{player}'s in the &bspawn";
    private static final String DEFAULT_OVERWORLD = "&7{player}'s in the &boverworld &7(&b{biome}&7)";
    private static final String DEFAULT_NETHER = "&7{player}'s in the &bnether &7(&b{biome}&7)";
    private static final String DEFAULT_THE_END = "&7{player}'s in the &bthe end &7(&b{biome}&7)";
    private static final String DEFAULT_UNKNOWN = "&7{player}'s in the &b{world}";

    private final UltimateDonutSmp plugin;

    public FindPlayerCommand(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only."); return true; }
        if (args.length == 0) { player.sendMessage(ColorUtils.toComponent("&cUsage: /findplayer <player>")); return true; }

        Player target = plugin.getHideManager().findOnlinePlayer(player, args[0]);
        if (target == null) { player.sendMessage(ColorUtils.toComponent("&cPlayer not online.")); return true; }

        LocationMessage locationMessage = resolveLocationMessage(target);
        String msg = plugin.getConfigManager().getMessageOrDefault(
                locationMessage.key(),
                locationMessage.fallback(),
                "{player}", plugin.getHideManager().publicName(target),
                "{world}", friendlyWorldName(target.getWorld()),
                "{biome}", formatBiome(target.getLocation())
        );
        player.sendMessage(ColorUtils.toComponent(msg));
        return true;
    }

    private LocationMessage resolveLocationMessage(Player target) {
        if (plugin.getAFKManager().isAfk(target.getUniqueId())) {
            return new LocationMessage("FINDPLAYER.AFK", DEFAULT_AFK);
        }
        if (plugin.getRtpZoneManager().isInZone(target)) {
            return new LocationMessage("FINDPLAYER.RTP_ZONE", DEFAULT_RTP_ZONE);
        }
        if (plugin.getAFKManager().isInSpawnCuboid(target)) {
            return new LocationMessage("FINDPLAYER.SPAWN", DEFAULT_SPAWN);
        }

        World world = target.getWorld();
        if (world == null) {
            return new LocationMessage("FINDPLAYER.UNKNOWN", DEFAULT_UNKNOWN);
        }

        switch (world.getEnvironment()) {
            case NORMAL:
                return new LocationMessage("FINDPLAYER.OVERWORLD", DEFAULT_OVERWORLD);
            case NETHER:
                return new LocationMessage("FINDPLAYER.NETHER", DEFAULT_NETHER);
            case THE_END:
                return new LocationMessage("FINDPLAYER.THE_END", DEFAULT_THE_END);
            case CUSTOM:
                return new LocationMessage("FINDPLAYER.UNKNOWN", DEFAULT_UNKNOWN);
            default:
                return null;
        }
    }

    private String friendlyWorldName(World world) {
        if (world == null) {
            return "unknown";
        }

        switch (world.getEnvironment()) {
            case NORMAL:
                return "overworld";
            case NETHER:
                return "nether";
            case THE_END:
                return "the end";
            case CUSTOM:
                return formatIdentifier(world.getName());
            default:
                return null;
        }
    }

    private String formatBiome(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }

        Biome biome = location.getBlock().getBiome();
        if (biome == null) {
            return "unknown";
        }

        return formatIdentifier(biome.getKey().getKey());
    }

    private String formatIdentifier(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "unknown";
        }

        String normalized = value;
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < normalized.length()) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }

        normalized = normalized.replace('-', '_').replace(' ', '_').toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("_+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.trim().isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.isEmpty() ? "unknown" : builder.toString();
    }

public final class LocationMessage {
    private final String key;
    private final String fallback;

    public LocationMessage(String key, String fallback) {
        this.key = key;
        this.fallback = fallback;
    }

    public String key() { return key; }
    public String fallback() { return fallback; }

    @Override public String toString() {
        return "LocationMessage[key=+key, fallback=+fallback]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationMessage that = (LocationMessage) o;
        return java.util.Objects.equals(key, that.key) && java.util.Objects.equals(fallback, that.fallback);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(key, fallback);
    }
}
}
