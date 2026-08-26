package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.utils.PermissionUtils;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.models.PunishmentQuery;
import com.bx.ultimateDonutSmp.models.PunishmentRecord;
import com.bx.ultimateDonutSmp.models.PunishmentScope;
import com.bx.ultimateDonutSmp.models.PunishmentState;
import com.bx.ultimateDonutSmp.models.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class PunishmentManager {

    public static final String VIEW_PERMISSION = "ultimatedonutsmp.staff.punishments.view";
    public static final String DELETE_PERMISSION = "ultimatedonutsmp.staff.punishments.delete";
    private static final Pattern MINECRAFT_USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private final UltimateDonutSmp plugin;

    public PunishmentManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    public boolean canView(Player viewer) {
        return viewer != null && PermissionUtils.has(viewer, VIEW_PERMISSION);
    }

    public Optional<UUID> resolveTargetUuid(String username) {
        return resolveTargetUuid(username, false);
    }

    public Optional<UUID> resolveTargetUuid(String username, boolean allowOfflineFallback) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }

        UUID parsedUuid = parseUuid(username);
        if (parsedUuid != null) {
            return Optional.of(parsedUuid);
        }

        Player online = findOnlinePlayer(username);
        if (online != null) {
            return Optional.of(online.getUniqueId());
        }

        UUID playerUuid = plugin.getDatabaseManager().findPlayerUuidByUsername(username);
        if (playerUuid != null) {
            return Optional.of(playerUuid);
        }

        UUID punishedUuid = plugin.getDatabaseManager().findPunishmentTargetUuidByName(username);
        if (punishedUuid != null) {
            return Optional.of(punishedUuid);
        }

        if (!allowOfflineFallback || !isValidUsername(username)) {
            return Optional.empty();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(username);
        return Optional.ofNullable(offline.getUniqueId());
    }

    public String resolveTargetName(UUID uuid) {
        if (uuid == null) {
            return "unknown";
        }

        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }

        String knownName = plugin.getDatabaseManager().getLastKnownUsername(uuid);
        if (knownName != null && !knownName.trim().isEmpty()) {
            return knownName;
        }

        String punishmentName = plugin.getDatabaseManager().getLatestPunishmentTargetName(uuid);
        if (punishmentName != null && !punishmentName.trim().isEmpty()) {
            return punishmentName;
        }

        return uuid.toString().substring(0, 8);
    }

    public String resolveTargetName(UUID uuid, String fallbackName) {
        String resolvedName = resolveTargetName(uuid);
        if (uuid == null || !resolvedName.equals(uuid.toString().substring(0, 8))) {
            return resolvedName;
        }
        return isValidUsername(fallbackName) ? fallbackName : resolvedName;
    }

    public Optional<PunishmentRecord> getRecord(long punishmentId) {
        return Optional.ofNullable(plugin.getDatabaseManager().loadPunishmentRecord(punishmentId));
    }

    public int countHistory(UUID targetUuid, PunishmentQuery query) {
        return countHistory(targetUuid, null, query);
    }

    public int countHistory(UUID targetUuid, String targetName, PunishmentQuery query) {
        return plugin.getDatabaseManager().countPunishmentHistory(
                targetUuid,
                targetName,
                query == null ? PunishmentQuery.defaultQuery() : query,
                System.currentTimeMillis()
        );
    }

    public List<PunishmentRecord> getHistory(UUID targetUuid, int limit, int offset, PunishmentQuery query) {
        return getHistory(targetUuid, null, limit, offset, query);
    }

    public List<PunishmentRecord> getHistory(UUID targetUuid, String targetName, int limit, int offset, PunishmentQuery query) {
        return plugin.getDatabaseManager().loadPunishmentHistory(
                targetUuid,
                targetName,
                query == null ? PunishmentQuery.defaultQuery() : query,
                Math.max(1, limit),
                Math.max(0, offset),
                System.currentTimeMillis()
        );
    }

    public int countAll(PunishmentQuery query, String search) {
        return plugin.getDatabaseManager().countAllPunishments(
                query == null ? PunishmentQuery.defaultQuery() : query,
                search,
                System.currentTimeMillis()
        );
    }

    public List<PunishmentRecord> getAll(PunishmentQuery query, String search, int limit, int offset) {
        return plugin.getDatabaseManager().loadAllPunishments(
                query == null ? PunishmentQuery.defaultQuery() : query,
                search,
                Math.max(1, limit),
                Math.max(0, offset),
                System.currentTimeMillis()
        );
    }

    /**
     * Reads one page of the server-wide punishment list off the main thread. The whole table is in
     * play here rather than a single indexed target, so a synchronous read would stall the server on
     * anything but a small history.
     */
    public CompletableFuture<PunishmentPage> getAllAsync(PunishmentQuery query, String search, int limit, int offset) {
        int pageSize = Math.max(1, limit);
        CompletableFuture<PunishmentPage> future = new CompletableFuture<>();
        plugin.getDatabaseManager().executeAsync(() -> {
            try {
                int total = countAll(query, search);
                // Filters and deletions can shrink the result set under a viewer who is deep into the
                // pages, so snap the offset back to the last real page boundary instead of paging past
                // the end.
                int lastPageOffset = Math.max(0, (Math.max(0, total - 1) / pageSize) * pageSize);
                int safeOffset = Math.max(0, Math.min(offset, lastPageOffset));
                future.complete(new PunishmentPage(getAll(query, search, pageSize, safeOffset), total, safeOffset));
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to load server-wide punishment page", e);
                future.complete(new PunishmentPage(java.util.Collections.emptyList(), 0, 0));
            }
        });
        return future;
    }

    public Optional<PunishmentRecord> getActiveRecord(UUID targetUuid, PunishmentType type) {
        return getActiveRecord(targetUuid, null, type);
    }

    public Optional<PunishmentRecord> getActiveRecord(UUID targetUuid, String targetName, PunishmentType type) {
        if ((targetUuid == null && (targetName == null || targetName.trim().isEmpty())) || type == null) {
            return Optional.empty();
        }

        List<PunishmentRecord> records = getHistory(
                targetUuid,
                targetName,
                1,
                0,
                new PunishmentQuery(type, com.bx.ultimateDonutSmp.models.PunishmentFilterState.ACTIVE, null)
        );
        return records.stream().findFirst();
    }

    public Optional<PunishmentRecord> getActiveRecord(UUID targetUuid, PunishmentType... types) {
        return getActiveRecord(targetUuid, null, types);
    }

    public Optional<PunishmentRecord> getActiveRecord(UUID targetUuid, String targetName, PunishmentType... types) {
        if ((targetUuid == null && (targetName == null || targetName.trim().isEmpty())) || types == null || types.length == 0) {
            return Optional.empty();
        }

        PunishmentRecord newest = null;
        for (PunishmentType type : types) {
            PunishmentRecord record = getActiveRecord(targetUuid, targetName, type).orElse(null);
            if (record != null && (newest == null || record.getIssuedAt() > newest.getIssuedAt())) {
                newest = record;
            }
        }
        return Optional.ofNullable(newest);
    }

    public boolean markActiveRecordsRemoved(UUID targetUuid, PunishmentType type, PunishmentRemovalRequest request) {
        return markActiveRecordsRemoved(targetUuid, null, type, request);
    }

    public boolean markActiveRecordsRemoved(UUID targetUuid, String targetName, PunishmentType type, PunishmentRemovalRequest request) {
        if ((targetUuid == null && (targetName == null || targetName.trim().isEmpty())) || type == null || request == null) {
            return false;
        }

        List<PunishmentRecord> activeRecords = getHistory(
                targetUuid,
                targetName,
                100,
                0,
                new PunishmentQuery(type, com.bx.ultimateDonutSmp.models.PunishmentFilterState.ACTIVE, null)
        );
        boolean changed = false;
        for (PunishmentRecord record : activeRecords) {
            changed |= markRemoved(record.getId(), request);
        }
        return changed;
    }

    public PunishmentRecord createRecord(PunishmentCreateRequest request) {
        if (request == null || request.targetUuid() == null) {
            return null;
        }

        String targetName = resolveNameSnapshot(request.targetUuid(), request.targetNameSnapshot(), false);
        String issuerName = resolveNameSnapshot(request.issuerUuid(), request.issuerNameSnapshot(), true);
        long issuedAt = request.issuedAt() > 0L ? request.issuedAt() : System.currentTimeMillis();
        Long expiresAt = normalizeTimestamp(request.expiresAt());

        PunishmentRecord unsaved = new PunishmentRecord(
                0L,
                request.targetUuid(),
                targetName,
                request.type() == null ? PunishmentType.WARN : request.type(),
                request.reason(),
                request.issuerUuid(),
                issuerName,
                issuedAt,
                expiresAt,
                null,
                "",
                null,
                "",
                request.sourceServer() == null || request.sourceServer().trim().isEmpty() ? "local" : request.sourceServer(),
                request.scope() == null ? PunishmentScope.SERVER : request.scope()
        );

        long id = plugin.getDatabaseManager().createPunishmentRecord(unsaved);
        if (id <= 0L) {
            return null;
        }

        return new PunishmentRecord(
                id,
                unsaved.getTargetUuid(),
                unsaved.getTargetNameSnapshot(),
                unsaved.getType(),
                unsaved.getReason(),
                unsaved.getIssuerUuid(),
                unsaved.getIssuerNameSnapshot(),
                unsaved.getIssuedAt(),
                unsaved.getExpiresAt(),
                null,
                "",
                null,
                "",
                unsaved.getSourceServer(),
                unsaved.getScope()
        );
    }

    public boolean markRemoved(long punishmentId, PunishmentRemovalRequest request) {
        if (punishmentId <= 0L || request == null) {
            return false;
        }

        PunishmentRecord existing = plugin.getDatabaseManager().loadPunishmentRecord(punishmentId);
        if (existing == null) {
            return false;
        }

        String removedByName = resolveNameSnapshot(request.removedByUuid(), request.removedByNameSnapshot(), true);
        long removedAt = request.removedAt() > 0L ? request.removedAt() : System.currentTimeMillis();
        String removalReason = request.removalReason();
        if (removalReason == null || removalReason.trim().isEmpty()) {
            removalReason = getState(existing) == PunishmentState.EXPIRED ? "Expired" : "Removed";
        }

        return plugin.getDatabaseManager().markPunishmentRemoved(
                punishmentId,
                request.removedByUuid(),
                removedByName,
                removedAt,
                removalReason
        );
    }

    public boolean deleteRecord(long punishmentId) {
        return plugin.getDatabaseManager().deletePunishmentRecord(punishmentId);
    }

    public PunishmentState getState(PunishmentRecord record) {
        if (record == null) {
            return PunishmentState.REMOVED;
        }
        if (record.isRemoved()) {
            return PunishmentState.REMOVED;
        }
        if (record.hasExpiry() && record.getExpiresAt() != null && record.getExpiresAt() <= System.currentTimeMillis()) {
            return PunishmentState.EXPIRED;
        }
        return PunishmentState.ACTIVE;
    }

    public boolean isActive(PunishmentRecord record) {
        return getState(record).isActive();
    }

    public String getDisplayType(PunishmentRecord record) {
        if (record == null) {
            return "UNKNOWN";
        }
        if (!record.isTemporary()) {
            return record.getType().name();
        }
        switch (record.getType()) {
            case BAN:
                return "TEMPBAN";
            case MUTE:
                return "TEMPMUTE";
            default:
                return record.getType().name();
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isValidUsername(String username) {
        return username != null && MINECRAFT_USERNAME.matcher(username).matches();
    }

    private String resolveNameSnapshot(UUID uuid, String providedSnapshot, boolean fallbackToConsole) {
        if (providedSnapshot != null && !providedSnapshot.trim().isEmpty()) {
            return providedSnapshot;
        }

        if (uuid != null) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                return online.getName();
            }

            String knownName = plugin.getDatabaseManager().getLastKnownUsername(uuid);
            if (knownName != null && !knownName.trim().isEmpty()) {
                return knownName;
            }

            String punishmentName = plugin.getDatabaseManager().getLatestPunishmentTargetName(uuid);
            if (punishmentName != null && !punishmentName.trim().isEmpty()) {
                return punishmentName;
            }

            return uuid.toString().substring(0, 8);
        }

        return fallbackToConsole ? "console" : "unknown";
    }

    private Long normalizeTimestamp(Long timestamp) {
        return timestamp == null || timestamp <= 0L ? null : timestamp;
    }

    private Player findOnlinePlayer(String username) {
        Player exact = Bukkit.getPlayerExact(username);
        if (exact != null) {
            return exact;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(username)) {
                return player;
            }
        }
        return null;
    }

public final class PunishmentPage {
    private final List<PunishmentRecord> records;
    private final int total;
    private final int offset;

    public PunishmentPage(List<PunishmentRecord> records, int total, int offset) {
        this.records = records;
        this.total = total;
        this.offset = offset;
    }

    public List<PunishmentRecord> records() { return records; }
    public int total() { return total; }
    public int offset() { return offset; }

    @Override public String toString() {
        return "PunishmentPage[records=+records, total=+total, offset=+offset]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PunishmentPage that = (PunishmentPage) o;
        return java.util.Objects.equals(records, that.records) && java.util.Objects.equals(total, that.total) && java.util.Objects.equals(offset, that.offset);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(records, total, offset);
    }
}

public final class PunishmentCreateRequest {
    private final UUID targetUuid;
    private final String targetNameSnapshot;
    private final PunishmentType type;
    private final String reason;
    private final UUID issuerUuid;
    private final String issuerNameSnapshot;
    private final long issuedAt;
    private final Long expiresAt;
    private final String sourceServer;
    private final PunishmentScope scope;

    public PunishmentCreateRequest(UUID targetUuid, String targetNameSnapshot, PunishmentType type, String reason, UUID issuerUuid, String issuerNameSnapshot, long issuedAt, Long expiresAt, String sourceServer, PunishmentScope scope) {
        this.targetUuid = targetUuid;
        this.targetNameSnapshot = targetNameSnapshot;
        this.type = type;
        this.reason = reason;
        this.issuerUuid = issuerUuid;
        this.issuerNameSnapshot = issuerNameSnapshot;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.sourceServer = sourceServer;
        this.scope = scope;
    }

    public UUID targetUuid() { return targetUuid; }
    public String targetNameSnapshot() { return targetNameSnapshot; }
    public PunishmentType type() { return type; }
    public String reason() { return reason; }
    public UUID issuerUuid() { return issuerUuid; }
    public String issuerNameSnapshot() { return issuerNameSnapshot; }
    public long issuedAt() { return issuedAt; }
    public Long expiresAt() { return expiresAt; }
    public String sourceServer() { return sourceServer; }
    public PunishmentScope scope() { return scope; }

    @Override public String toString() {
        return "PunishmentCreateRequest[targetUuid=+targetUuid, targetNameSnapshot=+targetNameSnapshot, type=+type, reason=+reason, issuerUuid=+issuerUuid, issuerNameSnapshot=+issuerNameSnapshot, issuedAt=+issuedAt, expiresAt=+expiresAt, sourceServer=+sourceServer, scope=+scope]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PunishmentCreateRequest that = (PunishmentCreateRequest) o;
        return java.util.Objects.equals(targetUuid, that.targetUuid) && java.util.Objects.equals(targetNameSnapshot, that.targetNameSnapshot) && java.util.Objects.equals(type, that.type) && java.util.Objects.equals(reason, that.reason) && java.util.Objects.equals(issuerUuid, that.issuerUuid) && java.util.Objects.equals(issuerNameSnapshot, that.issuerNameSnapshot) && java.util.Objects.equals(issuedAt, that.issuedAt) && java.util.Objects.equals(expiresAt, that.expiresAt) && java.util.Objects.equals(sourceServer, that.sourceServer) && java.util.Objects.equals(scope, that.scope);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(targetUuid, targetNameSnapshot, type, reason, issuerUuid, issuerNameSnapshot, issuedAt, expiresAt, sourceServer, scope);
    }
}

public final class PunishmentRemovalRequest {
    private final UUID removedByUuid;
    private final String removedByNameSnapshot;
    private final long removedAt;
    private final String removalReason;

    public PunishmentRemovalRequest(UUID removedByUuid, String removedByNameSnapshot, long removedAt, String removalReason) {
        this.removedByUuid = removedByUuid;
        this.removedByNameSnapshot = removedByNameSnapshot;
        this.removedAt = removedAt;
        this.removalReason = removalReason;
    }

    public UUID removedByUuid() { return removedByUuid; }
    public String removedByNameSnapshot() { return removedByNameSnapshot; }
    public long removedAt() { return removedAt; }
    public String removalReason() { return removalReason; }

    @Override public String toString() {
        return "PunishmentRemovalRequest[removedByUuid=+removedByUuid, removedByNameSnapshot=+removedByNameSnapshot, removedAt=+removedAt, removalReason=+removalReason]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PunishmentRemovalRequest that = (PunishmentRemovalRequest) o;
        return java.util.Objects.equals(removedByUuid, that.removedByUuid) && java.util.Objects.equals(removedByNameSnapshot, that.removedByNameSnapshot) && java.util.Objects.equals(removedAt, that.removedAt) && java.util.Objects.equals(removalReason, that.removalReason);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(removedByUuid, removedByNameSnapshot, removedAt, removalReason);
    }
}
}
