package com.bx.ultimateDonutSmp.commands;

import com.bx.ultimateDonutSmp.utils.PermissionUtils;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.managers.PunishmentManager;
import com.bx.ultimateDonutSmp.models.PunishmentRecord;
import com.bx.ultimateDonutSmp.models.PunishmentScope;
import com.bx.ultimateDonutSmp.models.PunishmentType;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import com.bx.ultimateDonutSmp.utils.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishmentCommand implements CommandExecutor {

    private static final String CREATE_PERMISSION = "ultimatedonutsmp.staff.punishments.create";
    private static final String BAN_PERMISSION = "ultimatedonutsmp.staff.punishments.ban";
    private static final String UNBAN_PERMISSION = "ultimatedonutsmp.staff.punishments.unban";
    private static final String MUTE_PERMISSION = "ultimatedonutsmp.staff.punishments.mute";
    private static final String UNMUTE_PERMISSION = "ultimatedonutsmp.staff.punishments.unmute";
    private static final String BLACKLIST_PERMISSION = "ultimatedonutsmp.staff.punishments.blacklist";
    private static final String UNBLACKLIST_PERMISSION = "ultimatedonutsmp.staff.punishments.unblacklist";
    private static final Pattern DURATION_TOKEN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> USAGE_MESSAGES = new java.util.LinkedHashMap(){{ put("ban",  "&cusage: /ban <player> [reason]"); put("tempban",  "&cusage: /tempban <player> <time> [reason] &7(time: 30s, 15m, 2h, 5d, or 5d 15m 30s)"); put("mute",  "&cusage: /mute <player> [reason]"); put("tempmute",  "&cusage: /tempmute <player> <time> [reason] &7(time: 30s, 15m, 2h, 5d, or 5d 15m 30s)"); put("warn",  "&cusage: /warn <player> [reason]"); put("kick",  "&cusage: /kick <player> [reason]"); put("blacklist",  "&cusage: /blacklist <player> [reason]"); put("unban",  "&cusage: /unban <player> [reason]"); put("pardon",  "&cusage: /pardon <player> [reason]"); put("unmute",  "&cusage: /unmute <player> [reason]"); put("unblacklist",  "&cusage: /unblacklist <player> [reason]"); }};

    private final UltimateDonutSmp plugin;

    public PunishmentCommand(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String action = normalizeLabel(label, command);

        if ("ban".equals(action)) {
            return handleCreate(sender, PunishmentType.BAN, args, false, false, action);
        } else if ("tempban".equals(action)) {
            return handleCreate(sender, PunishmentType.BAN, args, true, false, action);
        } else if ("mute".equals(action)) {
            return handleCreate(sender, PunishmentType.MUTE, args, false, false, action);
        } else if ("tempmute".equals(action)) {
            return handleCreate(sender, PunishmentType.MUTE, args, true, false, action);
        } else if ("warn".equals(action)) {
            return handleCreate(sender, PunishmentType.WARN, args, false, false, action);
        } else if ("kick".equals(action)) {
            return handleCreate(sender, PunishmentType.KICK, args, false, true, action);
        } else if ("blacklist".equals(action)) {
            return handleCreate(sender, PunishmentType.BLACKLIST, args, false, false, action);
        } else if ("unban".equals(action) || "pardon".equals(action)) {
            return handleRemove(sender, PunishmentType.BAN, args, action);
        } else if ("unmute".equals(action)) {
            return handleRemove(sender, PunishmentType.MUTE, args, action);
        } else if ("unblacklist".equals(action)) {
            return handleRemove(sender, PunishmentType.BLACKLIST, args, action);
        }
        return false;
    }

    private String normalizeLabel(String label, Command command) {
        String normalized = label == null || label.isBlank() ? command.getName() : label;
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < normalized.length()) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    static String permissionForAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }

        String lower = action.toLowerCase(Locale.ROOT);
        if ("ban".equals(lower) || "tempban".equals(lower)) {
            return BAN_PERMISSION;
        } else if ("unban".equals(lower) || "pardon".equals(lower)) {
            return UNBAN_PERMISSION;
        } else if ("mute".equals(lower) || "tempmute".equals(lower)) {
            return MUTE_PERMISSION;
        } else if ("unmute".equals(lower)) {
            return UNMUTE_PERMISSION;
        } else if ("blacklist".equals(lower)) {
            return BLACKLIST_PERMISSION;
        } else if ("unblacklist".equals(lower)) {
            return UNBLACKLIST_PERMISSION;
        } else if ("warn".equals(lower) || "kick".equals(lower)) {
            return CREATE_PERMISSION;
        }
        return null;
    }

    static boolean hasPermissionForAction(Permissible permissible, String action) {
        String permission = permissionForAction(action);
        return permission != null && PermissionUtils.has(permissible, permission);
    }

    private boolean handleCreate(CommandSender sender,
                                 PunishmentType type,
                                 String[] args,
                                 boolean temporary,
                                 boolean onlineOnly,
                                 String usageLabel) {
        if (!hasPermission(sender, usageLabel)) {
            send(sender, plugin.getConfigManager().getMessageOrDefault(
                    "PUNISHMENTS.NO-CREATE-PERMISSION",
                    "&cYou do not have permission to create punishments."
            ));
            return true;
        }

        int minimumArgs = temporary ? 2 : 1;
        if (args.length < minimumArgs) {
            sendUsage(sender, usageLabel);
            return true;
        }

        ResolvedTarget target = resolveTarget(args[0]);
        if (target == null || target.uuid() == null) {
            send(sender, plugin.getConfigManager().getMessageOrDefault("PUNISHMENTS.NOT-FOUND", "&cPlayer not found."));
            return true;
        }

        Player onlineTarget = Bukkit.getPlayer(target.uuid());
        if (onlineOnly && onlineTarget == null) {
            send(sender, plugin.getConfigManager().getMessageOrDefault(
                    "PUNISHMENTS.TARGET-OFFLINE",
                    "&cThat player is not online."
            ));
            return true;
        }

        Long expiresAt = null;
        int reasonStart = 1;
        if (temporary) {
            DurationParseResult duration = parseDuration(args, 1);
            if (duration.millis() <= 0L) {
                send(sender, plugin.getConfigManager().getMessageOrDefault(
                        "PUNISHMENTS.INVALID-DURATION",
                        "&cInvalid time. Use values like 30s, 15m, 2h, 5d, or combine: 5d 15m 30s."
                ));
                return true;
            }
            expiresAt = System.currentTimeMillis() + duration.millis();
            reasonStart = duration.nextArgIndex();
        }

        String reason = joinReason(args, reasonStart);
        Actor actor = resolveActor(sender);
        PunishmentRecord record = plugin.getPunishmentManager().createRecord(new PunishmentManager.PunishmentCreateRequest(
                target.uuid(),
                target.name(),
                type,
                reason,
                actor.uuid(),
                actor.name(),
                System.currentTimeMillis(),
                expiresAt,
                "local",
                PunishmentScope.SERVER
        ));

        if (record == null) {
            send(sender, plugin.getConfigManager().getMessageOrDefault(
                    "PUNISHMENTS.CREATE-FAILED",
                    "&cFailed to create punishment record."
            ));
            return true;
        }

        applyRuntimeEffect(type, onlineTarget, record);
        send(sender, plugin.getConfigManager().getMessageOrDefault(
                "PUNISHMENTS.CREATED",
                "&aCreated &f{type} &apunishment for &b{player}&a. ID: &f#{id}",
                "{type}", plugin.getPunishmentManager().getDisplayType(record),
                "{player}", target.name(),
                "{id}", String.valueOf(record.getId())
        ));
        return true;
    }

    private boolean handleRemove(CommandSender sender, PunishmentType type, String[] args, String label) {
        if (!hasPermission(sender, label)) {
            send(sender, plugin.getConfigManager().getMessageOrDefault(
                    "PUNISHMENTS.NO-REMOVE-PERMISSION",
                    "&cYou do not have permission to remove punishments."
            ));
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender, label);
            return true;
        }

        ResolvedTarget target = resolveTarget(args[0]);
        UUID targetUuid = target != null ? target.uuid() : null;
        String targetName = target != null ? target.name() : args[0];

        if (targetUuid == null && (targetName == null || targetName.isBlank())) {
            send(sender, plugin.getConfigManager().getMessageOrDefault("PUNISHMENTS.NOT-FOUND", "&cPlayer not found."));
            return true;
        }

        String reason = joinReason(args, 1);
        if (reason.equals("no reason specified")) {
            reason = "removed by staff";
        }

        Actor actor = resolveActor(sender);
        boolean removed = plugin.getPunishmentManager().markActiveRecordsRemoved(
                targetUuid,
                targetName,
                type,
                new PunishmentManager.PunishmentRemovalRequest(
                        actor.uuid(),
                        actor.name(),
                        System.currentTimeMillis(),
                        reason
                )
        );

        if (!removed) {
            send(sender, plugin.getConfigManager().getMessageOrDefault(
                    "PUNISHMENTS.NO-ACTIVE",
                    "&cNo active {type} punishment found for {player}.",
                    "{type}", type.name(),
                    "{player}", target.name()
            ));
            return true;
        }

        send(sender, plugin.getConfigManager().getMessageOrDefault(
                "PUNISHMENTS.REMOVED",
                "&aRemoved active &f{type} &apunishment(s) for &b{player}&a.",
                "{type}", type.name(),
                "{player}", target.name()
        ));
        return true;
    }

    private void applyRuntimeEffect(PunishmentType type, Player onlineTarget, PunishmentRecord record) {
        if (onlineTarget == null) {
            return;
        }

        if (type == PunishmentType.BAN || type == PunishmentType.BLACKLIST || type == PunishmentType.KICK) {
            onlineTarget.kickPlayer(ColorUtils.toComponent(buildPunishmentMessage(record)));
        } else if (type == PunishmentType.WARN) {
            onlineTarget.sendMessage(ColorUtils.toComponent(
                    plugin.getConfigManager().getMessageOrDefault(
                            "PUNISHMENTS.WARN-RECEIVED",
                            "&cWarning: &f{reason}",
                            "{reason}", record.getReason()
                    )
            ));
        } else if (type == PunishmentType.MUTE) {
            onlineTarget.sendMessage(ColorUtils.toComponent(buildPunishmentMessage(record)));
        }
    }

    private String buildPunishmentMessage(PunishmentRecord record) {
        return plugin.getConfigManager().getMessageOrDefault(
                punishmentMessagePath(record.getType()),
                defaultPunishmentMessage(record.getType()),
                punishmentPlaceholders(record)
        );
    }

    private String punishmentMessagePath(PunishmentType type) {
        if (type == PunishmentType.BAN) {
            return "PUNISHMENTS.BAN";
        } else if (type == PunishmentType.KICK) {
            return "PUNISHMENTS.KICK";
        } else if (type == PunishmentType.MUTE) {
            return "PUNISHMENTS.MUTE";
        } else if (type == PunishmentType.BLACKLIST) {
            return "PUNISHMENTS.BLACKLIST";
        } else if (type == PunishmentType.WARN) {
            return "PUNISHMENTS.WARN-RECEIVED";
        }
        return "PUNISHMENTS." + type.name();
    }

    private String defaultPunishmentMessage(PunishmentType type) {
        if (type == PunishmentType.BAN) {
            return "&c&lyou have been banned!\n&8&m----------------------------\n&7reason: &f%reason%\n&7expires: &f%nicest_expiration%\n&7banned by: &f%issuer%\n&8&m----------------------------";
        } else if (type == PunishmentType.KICK) {
            return "&c&lyou have been kicked!\n&8&m----------------------------\n&7reason: &f%reason%\n&7kicked by: &f%issuer%\n&8&m----------------------------\n&7you may reconnect";
        } else if (type == PunishmentType.MUTE) {
            return "&c&lyou have been muted!\n&8&m----------------------------\n&7reason: &f%reason%\n&7expires: &f%nicest_expiration%\n&7muted by: &f%issuer%\n&8&m----------------------------\n&7you cannot speak in chat";
        } else if (type == PunishmentType.BLACKLIST) {
            return "&4&lyou have been blacklisted!\n&8&m----------------------------\n&7reason: &f%reason%\n&7blacklisted by: &f%issuer%\n&8&m----------------------------\n&4you cannot join the server";
        } else if (type == PunishmentType.WARN) {
            return "&cwarning: &f{reason}";
        }
        return "";
    }

    private String[] punishmentPlaceholders(PunishmentRecord record) {
        String expires = formatExpires(record);
        String issuer = formatIssuer(record);
        String reason = record == null || record.getReason() == null ? "" : record.getReason();
        String player = record == null || record.getTargetNameSnapshot() == null ? "" : record.getTargetNameSnapshot();
        String id = record == null ? "" : String.valueOf(record.getId());
        String type = record == null || record.getType() == null ? "" : record.getType().name();

        return new String[]{
                "%reason%", reason,
                "{reason}", reason,

                "%nicest_expiration%", expires,
                "{nicest_expiration}", expires,
                "%expires%", expires,
                "{expires}", expires,
                "%expires_at%", expires,
                "{expires_at}", expires,
                "%expiration%", expires,
                "{expiration}", expires,
                "%expiry%", expires,
                "{expiry}", expires,
                "%duration%", expires,
                "{duration}", expires,

                "%issuer%", issuer,
                "{issuer}", issuer,
                "%staff%", issuer,
                "{staff}", issuer,
                "%by%", issuer,
                "{by}", issuer,

                "%player%", player,
                "{player}", player,
                "%target%", player,
                "{target}", player,

                "%id%", id,
                "{id}", id,

                "%type%", type,
                "{type}", type
        };
    }

    private String formatExpires(PunishmentRecord record) {
        if (record == null || record.getExpiresAt() == null) {
            return "Permanent";
        }
        long remainingSeconds = Math.max(0L, (record.getExpiresAt() - System.currentTimeMillis()) / 1000L);
        if (remainingSeconds <= 0L) {
            return "Expired";
        }
        if (plugin != null && plugin.getLanguageManager() != null) {
            return plugin.getLanguageManager().formatDuration(remainingSeconds, true);
        }
        return NumberUtils.formatCountdown(remainingSeconds);
    }

    private String formatIssuer(PunishmentRecord record) {
        if (record == null) return "unknown";
        String issuer = record.getIssuerNameSnapshot();
        return issuer == null || issuer.isBlank() ? "unknown" : issuer;
    }

    private ResolvedTarget resolveTarget(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online == null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().equalsIgnoreCase(input)) {
                    online = player;
                    break;
                }
            }
        }

        if (online != null) {
            return new ResolvedTarget(online.getUniqueId(), online.getName());
        }

        UUID knownUuid = plugin.getPunishmentManager().resolveTargetUuid(input, true).orElse(null);
        if (knownUuid != null) {
            return new ResolvedTarget(knownUuid, plugin.getPunishmentManager().resolveTargetName(knownUuid, input));
        }
        return null;
    }

    private Actor resolveActor(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return new Actor(player.getUniqueId(), player.getName());
        }
        return new Actor(null, "console");
    }

    private boolean hasPermission(CommandSender sender, String action) {
        return !(sender instanceof Player) || hasPermissionForAction((Permissible) sender, action);
    }

    private void sendUsage(CommandSender sender, String label) {
        String normalizedLabel = label.toLowerCase(Locale.ROOT);
        String fallback = USAGE_MESSAGES.getOrDefault(normalizedLabel, "&cUsage: /" + normalizedLabel + " <player> [reason]");
        send(sender, plugin.getConfigManager().getMessageOrDefault(
                "PUNISHMENTS.USAGE-" + normalizedLabel.toUpperCase(Locale.ROOT),
                fallback
        ));
    }

    private long parseDurationMillis(String input) {
        if (input == null || input.isBlank()) {
            return -1L;
        }

        Matcher matcher = DURATION_TOKEN.matcher(input.trim());
        long totalMillis = 0L;
        int matchedCharacters = 0;
        while (matcher.find()) {
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1L;
            }

            long multiplier;
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);
            if ("s".equals(unit)) {
                multiplier = 1_000L;
            } else if ("m".equals(unit)) {
                multiplier = 60_000L;
            } else if ("h".equals(unit)) {
                multiplier = 3_600_000L;
            } else if ("d".equals(unit)) {
                multiplier = 86_400_000L;
            } else if ("w".equals(unit)) {
                multiplier = 604_800_000L;
            } else {
                multiplier = -1L;
            }
            if (multiplier <= 0L) {
                return -1L;
            }

            totalMillis += amount * multiplier;
            matchedCharacters += matcher.group(0).length();
        }

        return matchedCharacters == input.trim().length() ? totalMillis : -1L;
    }

    private DurationParseResult parseDuration(String[] args, int startIndex) {
        long totalMillis = 0L;
        int index = startIndex;
        while (index < args.length) {
            long tokenMillis = parseDurationMillis(args[index]);
            if (tokenMillis <= 0L) {
                break;
            }
            totalMillis += tokenMillis;
            index++;
        }
        return new DurationParseResult(totalMillis, index);
    }

    private String joinReason(String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return "no reason specified";
        }

        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < args.length; index++) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.isEmpty() ? "no reason specified" : builder.toString();
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(ColorUtils.toComponent(message));
    }

    public static final class ResolvedTarget {
        private final UUID uuid;
        private final String name;

        public ResolvedTarget(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public UUID uuid() { return uuid; }
        public String name() { return name; }

        @Override public String toString() {
            return "ResolvedTarget[uuid=" + uuid + ", name=" + name + "]";
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

    public static final class Actor {
        private final UUID uuid;
        private final String name;

        public Actor(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public UUID uuid() { return uuid; }
        public String name() { return name; }

        @Override public String toString() {
            return "Actor[uuid=" + uuid + ", name=" + name + "]";
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Actor that = (Actor) o;
            return java.util.Objects.equals(uuid, that.uuid) && java.util.Objects.equals(name, that.name);
        }
        @Override public int hashCode() {
            return java.util.Objects.hash(uuid, name);
        }
    }

    public static final class DurationParseResult {
        private final long millis;
        private final int nextArgIndex;

        public DurationParseResult(long millis, int nextArgIndex) {
            this.millis = millis;
            this.nextArgIndex = nextArgIndex;
        }

        public long millis() { return millis; }
        public int nextArgIndex() { return nextArgIndex; }

        @Override public String toString() {
            return "DurationParseResult[millis=" + millis + ", nextArgIndex=" + nextArgIndex + "]";
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DurationParseResult that = (DurationParseResult) o;
            return millis == that.millis && nextArgIndex == that.nextArgIndex;
        }
        @Override public int hashCode() {
            return java.util.Objects.hash(millis, nextArgIndex);
        }
    }
}
