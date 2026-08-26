package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.models.PlayerData;
import com.bx.ultimateDonutSmp.models.Team;
import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Clears everything the plugin has stored about one player, in contrast to {@link StatsWipeManager}
 * which clears a single category across everybody. Moderation records — punishments, IP history,
 * freeze and staff-mode state — survive a player wipe, and so do world objects such as the
 * spawners they placed.
 */
public class PlayerWipeManager {

    /**
     * The order the command prints wipe counts in. Every key here is produced by
     * {@link DatabaseManager#previewPlayerWipe(UUID)}.
     */
    public static final List<String> COUNT_KEYS = new java.util.ArrayList<>(java.util.Arrays.asList(
            "stats", 
            "homes", 
            "team", 
            "ender_chest", 
            "crate_keys", 
            "shop_favorites", 
            "sell_records", 
            "bounties", 
            "auctions", 
            "orders", 
            "friends", 
            "ignores", 
            "logs"
    ));

    private static final Map<String, String> COUNT_LABELS = new java.util.LinkedHashMap(){{ put("stats",  "Stats and balance"); put("homes",  "Homes"); put("team",  "Team membership"); put("ender_chest",  "Ender chest"); put("crate_keys",  "Crate keys"); put("shop_favorites",  "Shop favourites"); put("sell_records",  "Sell records"); put("bounties",  "Bounties"); put("auctions",  "Auction house"); put("orders",  "Orders"); put("friends",  "Friends"); put("ignores",  "Ignores"); put("logs",  "Activity logs"); }};

public final class Target {
    private final UUID uuid;
    private final String name;

    public Target(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }

    @Override public String toString() {
        return "Target[uuid=+uuid, name=+name]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Target that = (Target) o;
        return java.util.Objects.equals(uuid, that.uuid) && java.util.Objects.equals(name, that.name);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(uuid, name);
    }
}

public final class WipeResult {
    private final boolean success;
    private final boolean busy;
    private final DatabaseManager.PlayerWipeResult counts;
    private final String errorMessage;

    public WipeResult(boolean success, boolean busy, DatabaseManager.PlayerWipeResult counts, String errorMessage) {
        this.success = success;
        this.busy = busy;
        this.counts = counts;
        this.errorMessage = errorMessage;
    }

    public boolean success() { return success; }
    public boolean busy() { return busy; }
    public DatabaseManager.PlayerWipeResult counts() { return counts; }
    public String errorMessage() { return errorMessage; }


        public static WipeResult alreadyRunning() {
            return new WipeResult(false, true, null, null);
        }

        public static WipeResult failure(String errorMessage) {
            return new WipeResult(false, false, null, errorMessage);
        }

    @Override public String toString() {
        return "WipeResult[success=+success, busy=+busy, counts=+counts, errorMessage=+errorMessage]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WipeResult that = (WipeResult) o;
        return java.util.Objects.equals(success, that.success) && java.util.Objects.equals(busy, that.busy) && java.util.Objects.equals(counts, that.counts) && java.util.Objects.equals(errorMessage, that.errorMessage);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(success, busy, counts, errorMessage);
    }
}

    private final UltimateDonutSmp plugin;
    private final AtomicBoolean wipeInProgress = new AtomicBoolean(false);

    public PlayerWipeManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    public static String label(String countKey) {
        return COUNT_LABELS.getOrDefault(countKey, countKey);
    }

    public boolean isWipeInProgress() {
        return wipeInProgress.get();
    }

    /**
     * Finds the player an admin typed, online or not. Hidden players resolve by their real name so
     * a disguise cannot dodge a wipe.
     */
    public Target resolveTarget(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String trimmed = input.trim();
        Player online = Bukkit.getPlayerExact(trimmed);
        if (online != null) {
            return new Target(online.getUniqueId(), online.getName());
        }

        UUID storedUuid = plugin.getDatabaseManager().findPlayerUuidByUsername(trimmed);
        if (storedUuid != null) {
            String storedName = plugin.getDatabaseManager().getLastKnownUsername(storedUuid);
            return new Target(storedUuid, storedName == null || storedName.isBlank() ? trimmed : storedName);
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(trimmed);
        if (offlinePlayer.isOnline() || offlinePlayer.hasPlayedBefore()) {
            String offlineName = offlinePlayer.getName();
            return new Target(
                    offlinePlayer.getUniqueId(),
                    offlineName == null || offlineName.isBlank() ? trimmed : offlineName
            );
        }

        return null;
    }

    public DatabaseManager.PlayerWipePreview preview(UUID playerUuid) {
        return plugin.getDatabaseManager().previewPlayerWipe(playerUuid);
    }

    public WipeResult wipe(Target target, String actorName) {
        if (target == null) {
            return WipeResult.failure("no wipe target selected.");
        }
        if (!wipeInProgress.compareAndSet(false, true)) {
            return WipeResult.alreadyRunning();
        }

        try {
            Team team = plugin.getTeamManager().getTeam(target.uuid());
            boolean wasLeader = team != null && team.isLeader(target.uuid());

            discardOpenState(target.uuid());
            DatabaseManager.PlayerWipeResult counts = plugin.getDatabaseManager()
                    .resetForPlayerWipe(target.uuid(), defaultMoney());
            applyTeamRemoval(team, target.uuid(), wasLeader);
            clearCaches(target.uuid());
            resetLiveData(target.uuid());
            refreshDisplays(target.uuid());

            plugin.getLogger().info("Player wipe completed by " + actorName + " for "
                    + target.name() + " (" + target.uuid() + ").");
            return new WipeResult(true, false, counts, null);
        } catch (SQLException | RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "player wipe failed for " + target.uuid(), exception);
            return WipeResult.failure(exception.getMessage());
        } finally {
            wipeInProgress.set(false);
        }
    }

    private double defaultMoney() {
        return plugin.getConfigManager().getConfig().getDouble("SETTINGS.MONEY-PER-DEFAULT", 1000.0);
    }

    /**
     * Drops anything still holding the player's old contents in memory, so nothing writes itself
     * back over the rows the wipe is about to delete.
     */
    private void discardOpenState(UUID playerUuid) {
        if (plugin.getEnderChestManager() != null) {
            plugin.getEnderChestManager().discardForPlayerWipe(playerUuid);
        }
        plugin.getCrateManager().clearSession(playerUuid);
        plugin.getCrateManager().clearPendingBind(playerUuid);
    }

    private void applyTeamRemoval(Team team, UUID playerUuid, boolean wasLeader) {
        if (team == null) {
            return;
        }
        if (wasLeader) {
            plugin.getTeamManager().disbandTeam(team);
            return;
        }
        plugin.getTeamManager().kickMember(team, playerUuid);
    }

    private void clearCaches(UUID playerUuid) {
        plugin.getCrateManager().unloadKeyBalanceCache(playerUuid);
        plugin.getShopManager().cleanupPlayer(playerUuid);
        plugin.getHomeManager().unloadHomes(playerUuid);
        plugin.getIgnoreManager().unloadPlayer(playerUuid);
        plugin.getBountyManager().removeBounty(playerUuid);
        if (plugin.getFriendsManager() != null) {
            plugin.getFriendsManager().unloadPlayer(playerUuid);
        }
        if (plugin.getAuctionHouseManager() != null) {
            plugin.getAuctionHouseManager().cleanupPlayer(playerUuid);
        }
        if (plugin.getOrdersManager() != null) {
            plugin.getOrdersManager().forgetUiState(playerUuid);
        }
        if (plugin.getLeaderboardManager() != null) {
            plugin.getLeaderboardManager().invalidateAll();
        }

        Player online = Bukkit.getPlayer(playerUuid);
        if (online != null) {
            plugin.getHomeManager().loadHomes(online);
            plugin.getIgnoreManager().loadPlayer(playerUuid);
            if (plugin.getFriendsManager() != null) {
                plugin.getFriendsManager().loadPlayer(playerUuid);
            }
        }
    }

    /**
     * Resets the in-memory copy of a player who is online, otherwise the next autosave would write
     * their old totals straight back into the row that was just cleared.
     */
    private void resetLiveData(UUID playerUuid) {
        PlayerData data = plugin.getPlayerDataManager().get(playerUuid);
        if (data == null) {
            return;
        }

        data.resetTrackedStats(System.currentTimeMillis());
        data.setMoney(defaultMoney());
        data.setShards(0L);
        plugin.getDatabaseManager().savePlayer(data);
    }

    private void refreshDisplays(UUID playerUuid) {
        plugin.getScoreboardManager().updateAll();
        plugin.getTablistManager().updateAll();

        Player online = Bukkit.getPlayer(playerUuid);
        if (online != null) {
            plugin.getTablistManager().updateTablistName(online);
        }
    }
}
