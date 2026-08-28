package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.models.PlayerData;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import com.bx.ultimateDonutSmp.utils.LegacyScoreboardText;
import com.bx.ultimateDonutSmp.utils.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a player's balance attached to their username.
 *
 * <p>Minecraft 1.12.2 has no scoreboard number formats, so the balance cannot be carried by the
 * below-name slot the way newer servers can. What the 1.12.2 client does support is scoreboard
 * teams: it draws a team's suffix directly after a member's name, in the world and in the tab
 * list. Every viewer who switched the feature on gets their own private scoreboard, and every
 * visible player becomes a member of a team carrying their formatted balance in the suffix, so
 * the text is attached to the nametag itself, follows it everywhere and never touches the
 * player's actual name.</p>
 *
 * <p>Nothing is written to the server-wide main scoreboard. The board is the viewer's own — the
 * same board that carries the sidebar when the sidebar is active, because {@link ScoreboardManager}
 * owns it and hands it out here — so a player who left the setting off never hears about a team
 * at all, and the balances a viewer sees are decided entirely by which teams were sent to them.</p>
 *
 * <p>Two rules belong to the client rather than to us. The 1.12.2 protocol allows a team prefix
 * or suffix of at most sixteen raw characters, so a balance line longer than that is safely
 * truncated at the last whole colour code. And the same suffix the client draws next to the name
 * in the world also appears after the name in the viewer's tab list, because teams were the
 * mechanism 1.12.2 used to decorate tab names with.</p>
 */
public class MoneyNametagManager {

    private static final String LEGACY_DISPLAY_TAG = "uds_money_nametag";
    private static final String BALANCE_PLACEHOLDER = "{balance}";
    /** Every team this feature registers starts with this, so cleanup can find its own teams. */
    private static final String TEAM_PREFIX = "udsm";

    private final UltimateDonutSmp plugin;
    private final MoneyNametagState state = new MoneyNametagState();
    /** The scoreboard each viewer's teams were last written to, so they can be removed again. */
    private final Map<UUID, Scoreboard> viewerBoards = new ConcurrentHashMap<>();
    private boolean warnedBoardUnavailable;

    public MoneyNametagManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    /** Renders {@code format} for {@code balance}; visible for tests without a running server. */
    public static String render(String format, double balance, boolean shortFormat) {
        String amount = shortFormat ? NumberUtils.formatNice(balance) : NumberUtils.format(balance);
        return format == null ? amount : format.replace(BALANCE_PLACEHOLDER, amount);
    }

    /**
     * Fits the rendered balance into the suffix of a 1.12.2 nametag team: at most sixteen raw
     * characters, legacy colours only, never cut through a colour code, and one leading space so
     * the text does not run into the username.
     */
    static String fitTeamSuffix(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String legacy = LegacyScoreboardText.sanitize(
                text, LegacyScoreboardText.MAX_TEAM_PART_LENGTH - 1);
        return legacy.isEmpty() ? "" : " " + legacy;
    }

    public boolean isEnabled() {
        return config().getBoolean("MONEY-NAMETAGS.ENABLED", true);
    }

    public long getUpdateIntervalTicks() {
        return Math.max(1L, config().getLong("MONEY-NAMETAGS.UPDATE-INTERVAL-TICKS", 10L));
    }

    /** Whether {@code viewer} asked to see balances under other players. */
    public boolean isEnabledFor(Player viewer) {
        if (viewer == null) {
            return false;
        }
        PlayerData data = plugin.getPlayerDataManager().get(viewer);
        return data != null && data.isMoneyNametagsEnabled();
    }

    /** Sends every viewer who wants balances the ones that have changed since their last update. */
    public void updateAll() {
        if (!isEnabled()) {
            clearAll();
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (isEnabledFor(viewer)) {
                push(viewer);
            } else if (isActive(viewer.getUniqueId())) {
                clearViewer(viewer);
            }
        }
    }

    /** Pushes {@code player}'s balance out again, and gives them the objective if they want one. */
    public void update(Player player) {
        if (player == null || !isEnabled()) {
            return;
        }
        state.markChanged(player.getUniqueId());
        refreshViewer(player);
    }

    /** Installs or removes the teams on one player's client to match their own choice. */
    public void refreshViewer(Player viewer) {
        if (viewer == null) {
            return;
        }
        if (!isEnabled() || !isEnabledFor(viewer)) {
            clearViewer(viewer);
            return;
        }
        push(viewer);
    }

    /** Drops everything remembered about a player, as a viewer and as somebody being viewed. */
    public void remove(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        for (UUID viewerUuid : state.viewersFor(playerUuid)) {
            if (!viewerUuid.equals(playerUuid)) {
                removeFromViewerBoard(viewerUuid, playerUuid);
            }
        }
        state.forgetTarget(playerUuid);
        clearViewerState(playerUuid);
    }

    public void clearAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            clearViewer(viewer);
        }
        state.clear();
    }

    public void reload() {
        clearAll();
        purgeOrphanedDisplays();
    }

    public void shutdown() {
        clearAll();
    }

    /**
     * Removes the floating text entities earlier versions used for this feature. They were spawned
     * non-persistent so a restart clears them on its own, but a plugin reload leaves the previous
     * run's entities behind.
     */
    public void purgeOrphanedDisplays() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity display : world.getEntities()) {
                if (display.getScoreboardTags().contains(LEGACY_DISPLAY_TAG)) {
                    display.remove();
                }
            }
        }
    }

    private void push(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        Scoreboard board = boardFor(viewer);
        if (board == null) {
            warnBoardUnavailable();
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        for (Player target : Bukkit.getOnlinePlayers()) {
            UUID targetId = target.getUniqueId();
            if (!shouldDisplayFor(target) || !viewer.canSee(target)) {
                removeFromViewerBoard(viewerId, targetId);
                continue;
            }
            MoneyNametagState.Entry entry = state.entryFor(viewerId, targetId);
            String memberName = target.getName();
            String text = fitTeamSuffix(ColorUtils.colorize(currentText(target), target));
            if (entry.currentlyShows(text, memberName) && board.getTeam(entry.teamName) != null) {
                continue;
            }
            Team team = board.getTeam(entry.teamName);
            try {
                if (team == null) {
                    team = board.registerNewTeam(entry.teamName);
                }
                if (entry.memberName != null && !entry.memberName.equals(memberName)) {
                    team.removeEntry(entry.memberName);
                }
                if (!team.hasEntry(memberName)) {
                    team.addEntry(memberName);
                }
                team.setPrefix("");
                team.setSuffix(text);
                entry.text = text;
                entry.memberName = memberName;
            } catch (IllegalStateException | IllegalArgumentException unavailable) {
                // The board or the team was replaced under us (a reload rebuilt the board). Drop
                // the remembered state and try again on the next pass.
                entry.text = "";
                entry.memberName = null;
            }
        }
    }

    /**
     * Removes one target's entry from one viewer's board, e.g. because the target is hidden or
     * gone. The empty team is kept registered so showing the target again is one entry away.
     */
    private void removeFromViewerBoard(UUID viewerUuid, UUID targetUuid) {
        MoneyNametagState.Entry entry = state.entryOrNull(viewerUuid, targetUuid);
        if (entry == null || entry.memberName == null) {
            return;
        }
        Scoreboard board = viewerBoards.get(viewerUuid);
        if (board == null) {
            return;
        }
        Team team = board.getTeam(entry.teamName);
        if (team != null && team.hasEntry(entry.memberName)) {
            team.removeEntry(entry.memberName);
        }
        entry.memberName = null;
        entry.text = "";
    }

    /** Drops a player's own view: their board lease, its teams and the remembered state. */
    private void clearViewer(Player viewer) {
        if (viewer != null) {
            clearViewerState(viewer.getUniqueId());
        }
    }

    private void clearViewerState(UUID uuid) {
        Scoreboard board = viewerBoards.remove(uuid);
        boolean hadState = state.hasViewer(uuid);
        if (board == null && !hadState) {
            return;
        }
        state.forgetViewer(uuid);
        if (board != null) {
            for (Team team : board.getTeams()) {
                if (isOurTeam(team.getName())) {
                    try {
                        team.unregister();
                    } catch (IllegalStateException ignored) {
                        // Already unregistered.
                    }
                }
            }
        }
        ScoreboardManager scoreboards = plugin.getScoreboardManager();
        if (scoreboards != null) {
            scoreboards.releaseNametagBoard(uuid);
        }
    }

    private boolean isActive(UUID uuid) {
        return viewerBoards.containsKey(uuid) || state.hasViewer(uuid);
    }

    private Scoreboard boardFor(Player viewer) {
        ScoreboardManager scoreboards = plugin.getScoreboardManager();
        if (scoreboards == null) {
            return null;
        }
        Scoreboard board = scoreboards.getNametagBoard(viewer);
        if (board != null) {
            viewerBoards.put(viewer.getUniqueId(), board);
        }
        return board;
    }

    private String currentText(Player target) {
        return render(
                config().getString("MONEY-NAMETAGS.FORMAT", "&a${balance}"),
                plugin.getEconomyManager().getBalance(target),
                config().getBoolean("MONEY-NAMETAGS.SHORT-FORMAT", true));
    }

    /** Hidden players keep their balance to themselves. */
    private boolean shouldDisplayFor(Player target) {
        HideManager hideManager = plugin.getHideManager();
        return hideManager == null || !hideManager.isHidden(target.getUniqueId());
    }

    private boolean isOurTeam(String name) {
        return name != null && name.startsWith(TEAM_PREFIX);
    }

    private void warnBoardUnavailable() {
        if (warnedBoardUnavailable) {
            return;
        }
        warnedBoardUnavailable = true;
        plugin.getLogger().warning("Money nametags need the plugin's per-player scoreboard"
                + " manager, which is unavailable on this server. The feature will stay off.");
    }

    private FileConfiguration config() {
        return plugin.getConfigManager().getConfig();
    }
}
