package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player ping resolution and updates.
 */
public final class PingManager implements Listener {

    /** Sentinel for "no measurement exists yet"; displays must render it as {@link #UNKNOWN_DISPLAY}. */
    public static final int UNKNOWN = -1;
    /** The honest text shown wherever an unmeasured ping would otherwise pretend to be a number. */
    public static final String UNKNOWN_DISPLAY = "?";

    private final UltimateDonutSmp plugin;
    private final Map<UUID, Integer> pingCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pendingKeepAlives = new ConcurrentHashMap<>();
    private boolean protocolLibEnabled = false;

    public PingManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        initProtocolLibListener();
        startPeriodicPingTask();
    }

    private void initProtocolLibListener() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            return;
        }

        try {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            protocolManager.addPacketListener(new PacketAdapter(
                    plugin,
                    ListenerPriority.MONITOR,
                    PacketType.Play.Client.KEEP_ALIVE,
                    PacketType.Play.Server.KEEP_ALIVE
            ) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    if (event.isCancelled() || event.getPlayer() == null) return;
                    Player player = event.getPlayer();
                    try {
                        PacketContainer packet = event.getPacket();
                        // Spigot 1.12.2 serialises the keep-alive id as a long, but forks and
                        // the 1.8-style packets send it as an int. Either way the send time is
                        // what the reply is matched against, so accept both layouts; refusing
                        // the int layout left every 1.12.2 player pinned at the 1 ms fallback.
                        if (packet.getLongs().size() > 0 || packet.getIntegers().size() > 0) {
                            long now = System.currentTimeMillis();
                            pendingKeepAlives.put(player.getUniqueId(), now);
                        }
                    } catch (Throwable ignored) {}
                }

                @Override
                public void onPacketReceiving(PacketEvent event) {
                    if (event.isCancelled() || event.getPlayer() == null) return;
                    Player player = event.getPlayer();
                    UUID uuid = player.getUniqueId();
                    Long sentTime = pendingKeepAlives.remove(uuid);
                    if (sentTime != null) {
                        long elapsed = System.currentTimeMillis() - sentTime.longValue();
                        if (elapsed >= 0 && elapsed < 10000) {
                            // Monitor-only supplement: the server's own EntityPlayer#ping
                            // average stays authoritative; this cache exists so servers with
                            // exotic connection layouts (a proxy answering keep-alives) still
                            // have a measured sample, and so a 0 ms hop is reported as measured
                            // instead of unknown. Writing the NMS field here would fight the
                            // server's own moving average - deliberately not done.
                            pingCache.put(uuid, (int) elapsed);
                        }
                    }
                }
            });
            protocolLibEnabled = true;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.FINE, "ProtocolLib keep-alive ping measurement not available.", t);
        }
    }

    private void startPeriodicPingTask() {
        plugin.getSpigotScheduler().runGlobalTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                refreshPlayerPing(player);
            }
        }, 100L, 100L); // Every 5 seconds
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Immediately attempt resolution, then retry after a short delay for Bedrock handshake completion
        refreshPlayerPing(player);
        plugin.getSpigotScheduler().runEntityLater(player, () -> refreshPlayerPing(player), 20L);
        plugin.getSpigotScheduler().runEntityLater(player, () -> refreshPlayerPing(player), 60L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pingCache.remove(uuid);
        pendingKeepAlives.remove(uuid);
    }

    public void refreshPlayerPing(Player player) {
        if (player == null || !player.isOnline()) return;
        getPing(player);
    }

    /**
     * Resolves the real ping for a player.
     *
     * <p>Return contract: any positive value is a measured latency in milliseconds;
     * {@code 0} is the no-player sentinel (kept for callers/tests that pass null or offline
     * handles); {@link #UNKNOWN} means "not measured yet" and must never be shown as a number.
     */
    public int getPing(Player player) {
        if (player == null || !player.isOnline()) {
            return 0;
        }

        // 1. The server's own connection latency. Spigot 1.12.2's PlayerConnection already
        //    measures every keep-alive reply and folds it into EntityPlayer#ping as a moving
        //    average (modern servers expose the same value as Player#getPing()). That is the
        //    authoritative number for both a direct Java client and an Eaglercraft session,
        //    and it is what the vanilla tab signal bars are built from - reading it instead of
        //    writing to it keeps scoreboard and tab in sync by construction.
        int measured = readServerLatency(player);
        if (measured > 0) {
            return measured;
        }

        // 2. The ProtocolLib keep-alive monitor's own sample. A cached sample is authoritative
        //    even when it is 0 (a proxy on the same machine genuinely measures 0ms - reporting
        //    it is honest, hiding it would not be).
        Integer probed = pingCache.get(player.getUniqueId());
        if (probed != null && probed >= 0) {
            return probed;
        }

        // 3. Nothing measured yet (the first keep-alive exchange lands within ~20 s of the
        //    join). Report "unknown"; never a stand-in number. The previous 1 ms fallback made
        //    every player look like a perfect LAN connection and, once also written into the
        //    NMS field, overwrote the real values the tab bars were showing.
        return UNKNOWN;
    }

    /**
     * Formats a ping measurement for display. Only ever yields the real number or a question
     * mark - no invented latencies.
     */
    public static String formatPing(int ping) {
        return ping >= 0 ? String.valueOf(ping) : UNKNOWN_DISPLAY;
    }

    /** Reads the server-side latency value for this connection, or {@link #UNKNOWN}. */
    private int readServerLatency(Player player) {
        try {
            // Modern API first (Player#getPing exists from 1.16 on); looked up reflectively so
            // the plugin still compiles and runs against the 1.12.2 API jar.
            Method modern = player.getClass().getMethod("getPing");
            Object value = modern.invoke(player);
            if (value instanceof Integer && (Integer) value > 0) {
                return (Integer) value;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // 1.12.2 does not have the method; the NMS field below is the source there.
        }

        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            if (handle != null) {
                for (Class<?> clazz = handle.getClass(); clazz != null && clazz != Object.class;
                        clazz = clazz.getSuperclass()) {
                    for (String name : new String[]{"ping", "latency"}) {
                        try {
                            Field field = clazz.getDeclaredField(name);
                            if (field.getType() == int.class) {
                                field.setAccessible(true);
                                int value = field.getInt(handle);
                                if (value > 0) {
                                    return value;
                                }
                            }
                        } catch (NoSuchFieldException ignored) {
                            // try the next candidate / superclass
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // No readable field on this build; the caller falls back to the measured cache.
        }
        return UNKNOWN;
    }

    public boolean isProtocolLibEnabled() {
        return protocolLibEnabled;
    }
}
