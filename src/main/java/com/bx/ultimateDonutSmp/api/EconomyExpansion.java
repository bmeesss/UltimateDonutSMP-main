package com.bx.ultimateDonutSmp.api;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.managers.CurrencyManager;
import com.bx.ultimateDonutSmp.models.PlayerData;
import com.bx.ultimateDonutSmp.utils.NumberUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides %economy_*% placeholders for scoreboards, chat, holograms, etc.
 *
 * Supported:
 *   %economy_money%                raw money
 *   %economy_nicestMoney%          compact money amount (1,5K, 2,3M, ...)
 *   %economy_money_short%          compact money amount
 *   %economy_money_formatted%      configured money display
 *   %economy_money_symbol%         configured money symbol
 *   %economy_money_symbol_color%   configured money symbol color
 *   %economy_money_color%          configured money amount/display color
 *   %economy_money_name%           configured singular money name
 *   %economy_top_money_1_name%     leaderboard name for rank 1
 *   %economy_top_money_1_value%    full leaderboard value for rank 1
 *   %economy_top_money_1_value_short% compact leaderboard value for rank 1
 *   %economy_top_money_1_display%  ready-to-render leaderboard line for rank 1
 *   %economy_shards%               shard count
 *   %economy_nicestShards%         compact shard count
 *   %economy_shards_short%         compact shard count
 *   %economy_shards_formatted%     configured shard display
 *   %economy_shards_symbol%        configured shard symbol
 *   %economy_shards_symbol_color%  configured shard symbol color
 *   %economy_shards_color%         configured shard amount/display color
 *   %economy_shards_name%          configured singular shard name
 *   %economy_kills%                kill count
 *   %economy_deaths%               death count
 *   %economy_playtime%             formatted playtime
 *   %economy_team%                 team name (or "none")
 *   %economy_ping%                 player ping in ms
 *   %economy_username%             player name
 *   %economy_keyall_countdown%     time until next key-all
 *   %economy_booster_countdown%    time until booster expires (or "inactive")
 *   %economy_shard_cuboid_display% shard cuboid HUD text for scoreboard/action info
 *   %economy_shard_cuboid_status%  current shard cuboid state
 *   %economy_shard_cuboid_name%    active shard cuboid name
 *   %economy_x%                    player display X coordinate (randomized if setting enabled)
 *   %economy_y%                    player display Y coordinate
 *   %economy_z%                    player display Z coordinate (randomized if setting enabled)
 *   %economy_coords%               player display formatted coordinates (X, Y, Z)
 *   %economy_randomized_coords%    boolean setting state
 */
public class EconomyExpansion extends PlaceholderExpansion {

    private final UltimateDonutSmp plugin;
    private final LeaderboardPlaceholderResolver leaderboardPlaceholderResolver;

    public EconomyExpansion(UltimateDonutSmp plugin) {
        this.plugin = plugin;
        this.leaderboardPlaceholderResolver = new LeaderboardPlaceholderResolver(plugin);
    }

    @Override
    public @NotNull String getIdentifier() { return "economy"; }

    @Override
    public @NotNull String getAuthor() { return "UltimateDonutSmp"; }

    @Override
    public @NotNull String getVersion() { return "1.1"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        String leaderboardValue = leaderboardPlaceholderResolver.resolve(offlinePlayer, params);
        if (leaderboardValue != null) {
            return leaderboardValue;
        }

        // Key-all and booster don't need player data
        if (params.equalsIgnoreCase("keyall_countdown")) {
            java.util.UUID uuid = offlinePlayer != null ? offlinePlayer.getUniqueId() : null;
            return plugin.getKeyAllManager().getFormattedCountdown(uuid);
        }

        // Booster countdown (needs uuid)
        if (params.equalsIgnoreCase("booster_countdown")) {
            if (offlinePlayer == null || !offlinePlayer.isOnline()) return "inactive";
            long secs = plugin.getShardManager().getBoosterRemainingSeconds(offlinePlayer.getUniqueId());
            return secs > 0 ? NumberUtils.formatCountdown(secs) : "inactive";
        }

        if (params.equalsIgnoreCase("rtp_countdown")) {
            if (!plugin.getFeatureManager().isEnabled(com.bx.ultimateDonutSmp.managers.FeatureManager.Feature.RTP_ZONE)) {
                return "disabled";
            }
            java.util.UUID uuid = offlinePlayer != null ? offlinePlayer.getUniqueId() : null;
            return plugin.getRtpZoneManager().getFormattedCountdown(uuid);
        }

        if (params.equalsIgnoreCase("billford_countdown")) {
            if (!plugin.getFeatureManager().isEnabled(com.bx.ultimateDonutSmp.managers.FeatureManager.Feature.BILLFORD)) {
                return "disabled";
            }
            return plugin.getBillfordManager().getFormattedCountdown();
        }

        if (params.equalsIgnoreCase("shard_cuboid_display")) {
            if (offlinePlayer == null || !offlinePlayer.isOnline()) return "-";
            return plugin.getShardManager().getShardCuboidDisplay(offlinePlayer.getUniqueId());
        }

        if (params.equalsIgnoreCase("shard_cuboid_status")) {
            if (offlinePlayer == null || !offlinePlayer.isOnline()) return "outside";
            return plugin.getShardManager().getShardCuboidStatus(offlinePlayer.getUniqueId());
        }

        if (params.equalsIgnoreCase("shard_cuboid_name")) {
            if (offlinePlayer == null || !offlinePlayer.isOnline()) return "none";
            return plugin.getShardManager().getShardCuboidName(offlinePlayer.getUniqueId());
        }

        // Ping (online only)
        if (params.equalsIgnoreCase("ping")) {
            if (offlinePlayer == null || !offlinePlayer.isOnline()) return "?";
            return com.bx.ultimateDonutSmp.managers.PingManager.formatPing(
                    plugin.getPingManager().getPing(offlinePlayer.getPlayer()));
        }

        // Username
        if (params.equalsIgnoreCase("username")) {
            if (offlinePlayer == null) return "unknown";
            return plugin.getHideManager() == null
                    ? (offlinePlayer.getName() != null ? offlinePlayer.getName() : "unknown")
                    : plugin.getHideManager().publicName(offlinePlayer.getUniqueId(), offlinePlayer.getName());
        }

        CurrencyManager currencyManager = plugin.getCurrencyManager();
        if (params.equalsIgnoreCase("money_symbol")) {
            return currencyManager.symbol(CurrencyManager.CurrencyType.MONEY);
        }
        if (params.equalsIgnoreCase("money_symbol_color")) {
            return currencyManager.symbolColor(CurrencyManager.CurrencyType.MONEY);
        }
        if (params.equalsIgnoreCase("money_symbol_colored")) {
            return currencyManager.coloredSymbol(CurrencyManager.CurrencyType.MONEY);
        }
        if (params.equalsIgnoreCase("money_color")) {
            return currencyManager.color(CurrencyManager.CurrencyType.MONEY);
        }
        if (params.equalsIgnoreCase("money_name")) {
            return currencyManager.singular(CurrencyManager.CurrencyType.MONEY);
        }
        if (params.equalsIgnoreCase("money_name_plural")) {
            return currencyManager.plural(CurrencyManager.CurrencyType.MONEY);
        }
        if (params.equalsIgnoreCase("shards_symbol")) {
            return currencyManager.symbol(CurrencyManager.CurrencyType.SHARDS);
        }
        if (params.equalsIgnoreCase("shards_symbol_color")) {
            return currencyManager.symbolColor(CurrencyManager.CurrencyType.SHARDS);
        }
        if (params.equalsIgnoreCase("shards_symbol_colored")) {
            return currencyManager.coloredSymbol(CurrencyManager.CurrencyType.SHARDS);
        }
        if (params.equalsIgnoreCase("shards_color")) {
            return currencyManager.color(CurrencyManager.CurrencyType.SHARDS);
        }
        if (params.equalsIgnoreCase("shards_name")) {
            return currencyManager.singular(CurrencyManager.CurrencyType.SHARDS);
        }
        if (params.equalsIgnoreCase("shards_name_plural")) {
            return currencyManager.plural(CurrencyManager.CurrencyType.SHARDS);
        }

        // Team
        if (params.equalsIgnoreCase("team")) {
            if (offlinePlayer == null) return "none";
            String team = offlinePlayer.isOnline()
                    ? plugin.getTeamManager().getTeamName(offlinePlayer.getPlayer())
                    : null;
            return team != null ? team.toUpperCase() : "none";
        }

        // All others require player data. A caller that hands us no player still gets the zeroed
        // defaults below rather than an empty string, so an unresolved placeholder is never
        // mistaken for a real blank value.
        PlayerData data = null;
        if (offlinePlayer != null) {
            data = plugin.getPlayerDataManager().get(offlinePlayer.getUniqueId());
            if (data == null && offlinePlayer.isOnline()) {
                data = plugin.getPlayerDataManager().get(offlinePlayer.getPlayer());
            }
            if (data == null) {
                data = plugin.getDatabaseManager().loadPlayer(offlinePlayer.getUniqueId());
            }
        }
        if (data == null) {
            switch (params) {
                case "nicestMoney":
                case "money_short":
                case "money_amount_short":
                case "nicestmoney":
                    return currencyManager.formatCompactAmount(CurrencyManager.CurrencyType.MONEY, 0D);
                case "money_formatted":
                case "money-formatted":
                    return currencyManager.formatMoney(0D);
                case "money_short_formatted":
                case "nicestMoney_formatted":
                case "nicestmoney_formatted":
                    return currencyManager.formatMoneyCompact(0D);
                case "nicestShards":
                case "shards_short":
                case "shards_amount_short":
                case "nicestshards":
                    return currencyManager.formatCompactAmount(CurrencyManager.CurrencyType.SHARDS, 0D);
                case "shards_formatted":
                case "shards-formatted":
                    return currencyManager.formatShards(0L);
                case "shards_short_formatted":
                    return currencyManager.formatShardsCompact(0L);
                default:
                    return "0";
            }
        }

        switch (params) {
            case "donutplus":
            case "display_donutplus": {
                if (offlinePlayer == null || !offlinePlayer.isOnline()) {
                    return "";
                }
                org.bukkit.entity.Player p = offlinePlayer.getPlayer();
                if (p != null && data.isDisplayDonutPlusEnabled() && (p.hasPermission("ultimatedonutsmp.donutplus") || p.hasPermission("donutplus"))) {
                    return "&d&lDonut+ &r";
                }
                return "";
            }
            case "money":
                return NumberUtils.format(data.getMoney());
            case "nicestMoney":
            case "money_short":
            case "money_amount_short":
            case "nicestmoney":
                return currencyManager.formatCompactAmount(CurrencyManager.CurrencyType.MONEY, data.getMoney());
            case "money_formatted":
            case "money-formatted":
                return currencyManager.formatMoney(data.getMoney());
            case "money_short_formatted":
            case "nicestMoney_formatted":
            case "nicestmoney_formatted":
                return currencyManager.formatMoneyCompact(data.getMoney());
            case "shards":
                return String.valueOf(data.getShards());
            case "nicestShards":
            case "shards_short":
            case "shards_amount_short":
            case "nicestshards":
                return currencyManager.formatCompactAmount(CurrencyManager.CurrencyType.SHARDS, data.getShards());
            case "shards_formatted":
            case "shards-formatted":
                return currencyManager.formatShards(data.getShards());
            case "shards_short_formatted":
                return currencyManager.formatShardsCompact(data.getShards());
            case "kills":
                return String.valueOf(data.getKills());
            case "deaths":
                return String.valueOf(data.getDeaths());
            case "playtime":
                return NumberUtils.formatTimeLong(data.getTotalPlaytimeSeconds());
            case "killStreak":
            case "killstreak":
                return String.valueOf(data.getKillStreak());
            case "highestKillStreak":
            case "highestkillstreak":
                return String.valueOf(data.getHighestKillStreak());
            case "blocksPlaced":
            case "blocksplaced":
                return String.valueOf(data.getBlocksPlaced());
            case "blocksBroken":
            case "blocksbroken":
                return String.valueOf(data.getBlocksBroken());
            case "mobsKilled":
            case "mobskilled":
                return String.valueOf(data.getMobsKilled());
            case "moneySpent":
            case "moneyspent":
                return NumberUtils.format(data.getMoneySpent());
            case "moneyMade":
            case "moneymade":
                return NumberUtils.format(data.getMoneyMade());
            case "x":
            case "coord_x":
            case "coords_x": {
                if (offlinePlayer == null || !offlinePlayer.isOnline() || offlinePlayer.getPlayer() == null) {
                    return "0";
                }
                return String.valueOf(data.getDisplayX(offlinePlayer.getPlayer().getLocation().getBlockX()));
            }
            case "y":
            case "coord_y":
            case "coords_y": {
                if (offlinePlayer == null || !offlinePlayer.isOnline() || offlinePlayer.getPlayer() == null) {
                    return "0";
                }
                return String.valueOf(data.getDisplayY(offlinePlayer.getPlayer().getLocation().getBlockY()));
            }
            case "z":
            case "coord_z":
            case "coords_z": {
                if (offlinePlayer == null || !offlinePlayer.isOnline() || offlinePlayer.getPlayer() == null) {
                    return "0";
                }
                return String.valueOf(data.getDisplayZ(offlinePlayer.getPlayer().getLocation().getBlockZ()));
            }
            case "coords":
            case "location":
            case "formatted_coords": {
                if (offlinePlayer == null || !offlinePlayer.isOnline() || offlinePlayer.getPlayer() == null) {
                    return "0, 0, 0";
                }
                return data.getDisplayCoords(offlinePlayer.getPlayer().getLocation());
            }
            case "randomized_coords":
            case "randomized_coords_enabled":
                return String.valueOf(data.isRandomizedCoords());
            default:
                return null;
        }
    }
}
