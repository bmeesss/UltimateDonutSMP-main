package com.bx.ultimateDonutSmp.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure bookkeeping for the money nametag teams: which viewer sees which target, the team name
 * allocated for each target, and the text last written into that team's suffix.
 *
 * <p>The class holds no Bukkit types on purpose, so the rules behind one viewer's board — a team
 * per target, diffing against the last written text, removal on quit — can be unit tested without
 * a server. {@link MoneyNametagManager} owns the actual {@code Scoreboard} and {@code Team}
 * handles and consults this class for what needs to exist and what has changed.</p>
 */
final class MoneyNametagState {

    /** Bukkit 1.12.2 protocol limit for a team name. */
    static final int MAX_TEAM_NAME_LENGTH = 16;

    /** What one target currently looks like on one viewer's board. */
    static final class Entry {
        final String teamName;
        String memberName;
        String text = "";

        Entry(String teamName) {
            this.teamName = teamName;
        }

        boolean currentlyShows(String text, String memberName) {
            return this.text.equals(text) && memberName != null && memberName.equals(this.memberName);
        }
    }

    private final Map<UUID, String> teamNames = new HashMap<>();
    private final Map<UUID, Map<UUID, Entry>> viewers = new HashMap<>();
    private int nextTeamId = 1;

    /**
     * The short team name assigned to a target, unique across every board this manager owns.
     * Team names are limited to {@value #MAX_TEAM_NAME_LENGTH} raw characters by the 1.12.2
     * protocol, so targets get an opaque sequence number instead of a UUID.
     */
    synchronized String teamNameFor(UUID targetUuid) {
        String name = teamNames.get(targetUuid);
        if (name == null) {
            name = "udsm" + Integer.toHexString(nextTeamId++);
            teamNames.put(targetUuid, name);
        }
        return name;
    }

    /** The entry for {@code target} on {@code viewer}'s board, created on first sight. */
    synchronized Entry entryFor(UUID viewerUuid, UUID targetUuid) {
        return viewers.computeIfAbsent(viewerUuid, key -> new HashMap<>())
                .computeIfAbsent(targetUuid, key -> new Entry(teamNameFor(targetUuid)));
    }

    /** The entry if one already exists; {@code null} when the target is unknown to this viewer. */
    synchronized Entry entryOrNull(UUID viewerUuid, UUID targetUuid) {
        Map<UUID, Entry> perViewer = viewers.get(viewerUuid);
        return perViewer == null ? null : perViewer.get(targetUuid);
    }

    synchronized boolean hasViewer(UUID viewerUuid) {
        return viewers.containsKey(viewerUuid);
    }

    /** Every viewer currently tracking {@code targetUuid}. */
    synchronized List<UUID> viewersFor(UUID targetUuid) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Entry>> perViewer : viewers.entrySet()) {
            if (perViewer.getValue().containsKey(targetUuid)) {
                result.add(perViewer.getKey());
            }
        }
        return result;
    }

    /**
     * Forces the next pass to rewrite {@code target}'s line for every viewer, e.g. because their
     * balance changed or they joined/left.
     */
    synchronized void markChanged(UUID targetUuid) {
        for (Map<UUID, Entry> perViewer : viewers.values()) {
            Entry entry = perViewer.get(targetUuid);
            if (entry != null) {
                entry.text = "";
            }
        }
    }

    /** Drops {@code target} from every viewer, as if they had left. */
    synchronized void forgetTarget(UUID targetUuid) {
        teamNames.remove(targetUuid);
        for (Map<UUID, Entry> perViewer : viewers.values()) {
            perViewer.remove(targetUuid);
        }
    }

    /** Drops everything {@code viewer} sees, as if they had turned the feature off. */
    synchronized void forgetViewer(UUID viewerUuid) {
        viewers.remove(viewerUuid);
    }

    synchronized void clear() {
        viewers.clear();
        teamNames.clear();
        nextTeamId = 1;
    }
}
