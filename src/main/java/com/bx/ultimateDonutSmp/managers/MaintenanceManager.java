package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import com.bx.ultimateDonutSmp.utils.TitleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class MaintenanceManager {

    private final UltimateDonutSmp plugin;
    private final File stateFile;
    private boolean maintenanceActive;
    private String customLobbyServer;

    public MaintenanceManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "maintenance-state.yml");
        load();
    }

    public void load() {
        if (!stateFile.exists()) {
            this.maintenanceActive = false;
            this.customLobbyServer = null;
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(stateFile);
        this.maintenanceActive = config.getBoolean("active", false);
        this.customLobbyServer = config.getString("lobby", null);
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("active", maintenanceActive);
        if (customLobbyServer != null) {
            config.set("lobby", customLobbyServer);
        }

        try {
            config.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save maintenance state file", e);
        }
    }

    public boolean isMaintenanceActive() {
        return maintenanceActive;
    }

    public void setMaintenanceActive(boolean active) {
        this.maintenanceActive = active;
        save();
    }

    public String getLobbyServer() {
        if (customLobbyServer != null && !customLobbyServer.trim().isEmpty()) {
            return customLobbyServer;
        }
        return "lobby";
    }

    public boolean isUseProxy() {
        return false;
    }

    public String getLobbyWorld() {
        return "WORLD";
    }

    public void setLobbyServer(String lobbyServer) {
        this.customLobbyServer = lobbyServer;
        save();
    }

    public void startMaintenance() {
        setMaintenanceActive(true);
        save();

        FileConfiguration config = plugin.getConfigManager().getConfig();
        String bypassPerm = config.getString("MAINTENANCE.BYPASS_PERMISSION", "ULTIMATEDONUTSMP.ADMIN.MAINTENANCE.BYPASS");
        String enteringMessage = config.getString("MAINTENANCE.MESSAGES.ENTERING", "&d[Maintenance] &7server is entering maintenance. Moving you to the lobby...");
        String lobby = getLobbyServer();
        String localServerId = "local";
        boolean useProxy = isUseProxy();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(bypassPerm)) {
                String bypassJoinMsg = config.getString("MAINTENANCE.MESSAGES.BYPASS_JOIN", "&d[Maintenance] &7you joined while maintenance mode is active.");
                player.sendMessage(ColorUtils.toComponent(bypassJoinMsg));
                continue;
            }

            // Save player position
            Location loc = player.getLocation();
            if (loc.getWorld() != null) {
                plugin.getDatabaseManager().saveMaintenanceLocation(
                        player.getUniqueId(),
                        localServerId,
                        loc.getWorld().getName(),
                        loc.getX(),
                        loc.getY(),
                        loc.getZ(),
                        loc.getYaw(),
                        loc.getPitch()
                );
            }

            player.sendMessage(ColorUtils.toComponent(enteringMessage));
            if (useProxy) {
                sendToLobby(player, lobby);
            } else {
                String worldName = getLobbyWorld();
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    plugin.getSpigotScheduler().teleport(player, world.getSpawnLocation());
                } else {
                    Location defaultSpawn = plugin.getSpawnManager().resolveCommandDestination(SpawnManager.AreaType.SPAWN);
                    if (defaultSpawn != null) {
                        plugin.getSpigotScheduler().teleport(player, defaultSpawn);
                    }
                }
            }
        }

        // Kick players who failed to transfer after 2 seconds (only in proxy mode)
        if (useProxy) {
            plugin.getSpigotScheduler().runGlobalLater(() -> {
                String kickMessage = config.getString("MAINTENANCE.MESSAGES.KICK_FALLBACK", "&cThis server is in maintenance and no lobby is available.");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.hasPermission(bypassPerm)) {
                        player.kickPlayer(ColorUtils.colorize(kickMessage));
                    }
                }
            }, 40L);
        }
    }

    public void stopMaintenance() {
        setMaintenanceActive(false);
        save();
    }

    public void sendToLobby(Player player, String lobby) {
        if (player == null || lobby == null || lobby.trim().isEmpty()) {
            return;
        }

        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteStream);
            out.writeUTF("Connect");
            out.writeUTF(lobby);
            player.sendPluginMessage(plugin, "BungeeCord", byteStream.toByteArray());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to send BungeeCord connect packet for " + player.getName(), exception);
        }
    }

}
