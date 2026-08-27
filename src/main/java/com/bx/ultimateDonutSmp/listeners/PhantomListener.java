package com.bx.ultimateDonutSmp.listeners;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.models.PlayerData;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

public class PhantomListener implements Listener {

    private final UltimateDonutSmp plugin;

    public PhantomListener(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPhantomTarget(EntityTargetEvent event) {
        if (!plugin.getFeatureManager().isEnabled(com.bx.ultimateDonutSmp.managers.FeatureManager.Feature.PHANTOM)) return;
        if (!(event.getEntity() instanceof Phantom)) return;
        org.bukkit.entity.Entity target = event.getTarget();
        if (!(target instanceof Player)) return;
        Player player = (Player) target;

        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null && !data.isPhantomEnabled()) {
            event.setCancelled(true);
        }
    }
}
