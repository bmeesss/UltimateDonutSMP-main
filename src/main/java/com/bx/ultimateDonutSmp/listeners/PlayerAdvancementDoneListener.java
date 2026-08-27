package com.bx.ultimateDonutSmp.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.world.WorldLoadEvent;

/** Keeps advancement announcements disabled on the 1.12 API. */
public class PlayerAdvancementDoneListener implements Listener {
    public PlayerAdvancementDoneListener(Object plugin) {
        for (World world : Bukkit.getWorlds()) disable(world);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) { disable(event.getWorld()); }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) { }

    private void disable(World world) {
        if (world != null) world.setGameRuleValue("announceAdvancements", "false");
    }
}
