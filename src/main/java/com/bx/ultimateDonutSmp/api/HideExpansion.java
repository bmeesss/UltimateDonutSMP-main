package com.bx.ultimateDonutSmp.api;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.managers.HideManager;
import com.bx.ultimateDonutSmp.models.HideState;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class HideExpansion extends PlaceholderExpansion {

    private final UltimateDonutSmp plugin;

    public HideExpansion(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hide";
    }

    @Override
    public @NotNull String getAuthor() {
        return "UltimateDonutSmp";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        HideManager hideManager = plugin.getHideManager();
        HideState state = player == null || hideManager == null
                ? null
                : hideManager.getState(player.getUniqueId());

        switch (params.toLowerCase(Locale.ROOT)) {
            case "active":
                return String.valueOf(state != null);
            case "public_name":
            case "name":
                return publicName(player, hideManager, false);
            case "plain_name":
            case "plain_public_name":
                return publicName(player, hideManager, true);
            case "mode":
                return state == null ? "NONE" : state.mode().name();
            case "alias":
                return state == null ? "" : state.alias();
            case "skin":
                return state == null ? "" : state.skinUsername();
            default:
                return null;
        }
    }

    /**
     * Falls back to the raw account name when there is no player to look up or the hide system is
     * not running, so a name placeholder never resolves to an empty string.
     */
    private @Nullable String publicName(OfflinePlayer player, HideManager hideManager, boolean plain) {
        if (player == null) {
            return null;
        }
        if (hideManager == null) {
            return player.getName();
        }
        return plain
                ? hideManager.plainPublicName(player.getUniqueId(), player.getName())
                : hideManager.publicName(player.getUniqueId(), player.getName());
    }
}
