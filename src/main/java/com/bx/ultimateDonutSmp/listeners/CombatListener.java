package com.bx.ultimateDonutSmp.listeners;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.models.Team;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class CombatListener implements Listener {

    enum DamageSource {
        PLAYER,
        MOB,
        ENDER_CRYSTAL,
        OTHER
    }

    private final UltimateDonutSmp plugin;

    public CombatListener(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getCombatManager().isEnabled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        DamageSource source = resolveDamageSource(event.getDamager());
        if (!shouldTagVictim(
                source,
                plugin.getCombatManager().isMobCombatEnabled(),
                plugin.getCombatManager().isEnderCrystalCombatEnabled()
        )) return;

        Player attacker = resolvePlayerAttacker(event.getDamager());

        if (plugin.getCombatManager().isExcludedWorld(victim.getWorld().getName())) return;

        if (attacker != null && !attacker.getUniqueId().equals(victim.getUniqueId())
                && plugin.getTeamManager().areTeammates(attacker.getUniqueId(), victim.getUniqueId())) {
            Team team = plugin.getTeamManager().getTeam(attacker);
            if (team != null && !team.isFriendlyFireEnabled()) {
                event.setCancelled(true);
                return;
            }
        }

        plugin.getCombatManager().tag(victim);
        if (attacker != null) plugin.getCombatManager().tag(attacker);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawnAnchorDamage(EntityDamageByBlockEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        if (!plugin.getCombatManager().isEnabled()
                || !plugin.getCombatManager().isRespawnAnchorCombatEnabled()
                || !isRespawnAnchorDamage(event)
                || plugin.getCombatManager().isExcludedWorld(victim.getWorld().getName())) {
            return;
        }

        plugin.getCombatManager().tag(victim);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {
        if (!plugin.getCombatManager().isEnabled()
                || !plugin.getCombatManager().isEnderPearlCombatEnabled()
                || event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        Player player = event.getPlayer();
        if (plugin.getCombatManager().isExcludedWorld(player.getWorld().getName())) {
            return;
        }

        plugin.getCombatManager().tag(player);
    }

    static boolean shouldTagVictim(DamageSource source, boolean mobsEnabled, boolean enderCrystalsEnabled) {
        switch (source) {
            case PLAYER:
                return true;
            case MOB:
                return mobsEnabled;
            case ENDER_CRYSTAL:
                return enderCrystalsEnabled;
            case OTHER:
            default:
                return false;
        }
    }

    private DamageSource resolveDamageSource(Entity damager) {
        if (damager instanceof EnderCrystal) {
            return DamageSource.ENDER_CRYSTAL;
        }
        if (damager instanceof EnderPearl) {
            return plugin.getCombatManager().isEnderPearlCombatEnabled() ? DamageSource.PLAYER : DamageSource.OTHER;
        }
        if (resolvePlayerAttacker(damager) != null) {
            return DamageSource.PLAYER;
        }
        if (damager instanceof LivingEntity) {
            return DamageSource.MOB;
        }
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity) {
                return DamageSource.MOB;
            }
        }
        return DamageSource.OTHER;
    }

    private Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (!(damager instanceof EnderPearl) && projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }

    private boolean isRespawnAnchorDamage(EntityDamageByBlockEvent event) {
        Block damager = event.getDamager();
        if (damager != null && damager.getType() == Material.RESPAWN_ANCHOR) {
            return true;
        }
        BlockState state = event.getDamagerBlockState();
        return state != null && state.getType() == Material.RESPAWN_ANCHOR;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!plugin.getCombatManager().isEnabled()) return;
        Player player = event.getPlayer();
        if (!plugin.getCombatManager().isInCombat(player.getUniqueId())) return;
        if (plugin.getCombatManager().isExcludedWorld(player.getWorld().getName())) return;

        String cmd = event.getMessage().split(" ")[0].toLowerCase();
        if (plugin.getCombatManager().isBlockedCommand(cmd)) {
            event.setCancelled(true);
            String msg = plugin.getConfigManager().getConfig()
                    .getString("COMBAT-MANAGER.BLOCK-MESSAGE",
                            "&cyou can't use this command in your current status.");
            player.sendMessage(ColorUtils.toComponent(msg));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getCombatManager().isEnabled()) return;
        if (!plugin.getCombatManager().isKillOnLogoutEnabled()) return;
        if (player.isDead()) return;
        if (plugin.getCombatManager().isExcludedWorld(player.getWorld().getName())) return;
        if (!plugin.getCombatManager().isInCombat(player.getUniqueId())) return;

        player.setHealth(0.0D);
    }
}
