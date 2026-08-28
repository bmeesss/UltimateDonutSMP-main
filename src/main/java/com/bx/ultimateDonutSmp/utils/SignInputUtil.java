package com.bx.ultimateDonutSmp.utils;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Collects a single line of free text from a player.
 *
 * <h3>Why there are two transports</h3>
 * <p>The sign editor is the prettier transport, but Bukkit only exposes it from 1.20 onwards
 * ({@code Player#openSign(Sign, Side)}). On every older server - including Spigot 1.12.2 - there
 * is no Bukkit API to open a sign editor at all, and the previous implementation reacted to that
 * by cancelling the input immediately and invoking the callback with {@code null}. That is the
 * root cause of "/orders price does nothing", "/orders amount does nothing", "/ah search does
 * nothing" and "/orders search does nothing": all four call sites go through this class, and on
 * 1.12.2 none of them ever received a value.</p>
 *
 * <h3>Chat transport (used on 1.12.2)</h3>
 * <p>Chat is plain protocol-47 chat for both a Java 1.12.2 client and an Eaglercraft client behind
 * EaglerXServer, so it behaves identically on both and needs no client-side block tricks. The
 * configured sign lines are replayed as the prompt so the existing {@code *_SIGN} configuration
 * keeps driving the wording, and the answer is delivered through the very same
 * {@code Consumer<String>} callback the sign transport uses, so no call site changes.</p>
 *
 * <p>The chat transport is wired into {@code ChatListener} (which runs at
 * {@code EventPriority.NORMAL}, ahead of the chat pipeline) so the answer is never broadcast to
 * the server. It is consumed on the main thread through {@link SpigotScheduler}.</p>
 */
public final class SignInputUtil {

    private static final Set<String> REGISTERED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, JavaPlugin> PLUGINS = new ConcurrentHashMap<>();
    private static final Map<UUID, Location> SIGN_LOC = new ConcurrentHashMap<>();
    private static final Map<UUID, MaterialData> OLD_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> INPUT_LINE = new ConcurrentHashMap<>();
    private static final Map<UUID, Consumer<String>> CALLBACK = new ConcurrentHashMap<>();
    private static final Map<UUID, org.bukkit.scheduler.BukkitTask> HIDE_TASK = new ConcurrentHashMap<>();
    private static final Map<UUID, String[]> EXPECTED_LINES = new ConcurrentHashMap<>();
    private static final Map<UUID, List<String>> ORIGINAL_LINES = new ConcurrentHashMap<>();
    /** Chat-transport sessions: player id -&gt; pending prompt. */
    private static final Map<UUID, ChatPrompt> CHAT_PROMPTS = new ConcurrentHashMap<>();

    private static final long INPUT_TIMEOUT_TICKS = 900L; // 45 seconds
    private static final String CANCEL_WORD = "cancel";

    private static com.bx.ultimateDonutSmp.utils.SpigotScheduler getScheduler(JavaPlugin plugin) {
        if (plugin instanceof UltimateDonutSmp) {
            UltimateDonutSmp uds = (UltimateDonutSmp) plugin;
            return uds.getSpigotScheduler();
        }
        UltimateDonutSmp uds = (UltimateDonutSmp) Bukkit.getPluginManager().getPlugin("UltimateDonutSmp");
        if (uds != null) {
            return uds.getSpigotScheduler();
        }
        return null;
    }

    /** Message lookup that degrades to the English default when the plugin/keys are unavailable. */
    private static String message(JavaPlugin plugin, String key, String fallback) {
        if (plugin instanceof UltimateDonutSmp) {
            return ((UltimateDonutSmp) plugin).getConfigManager().getMessageOrDefault(key, fallback);
        }
        return fallback;
    }

    public static final String META_SIGN_INPUT = "donutorder-sign-input";

    private SignInputUtil() {}

    public static void openFromConfig(JavaPlugin plugin, Player player, org.bukkit.configuration.ConfigurationSection config, Consumer<String> callback) {
        List<String> lines = config == null ? null : config.getStringList("lines");
        int inputLine = config == null ? 0 : config.getInt("input-line", 0);
        open(plugin, player, lines, inputLine, callback);
    }

    public static void open(JavaPlugin plugin, Player player, List<String> lines, int inputLine, Consumer<String> callback) {
        cancel(player);
        if (!player.isOnline()) {
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }

        UUID uuid = player.getUniqueId();
        int lineIndex = Math.max(0, Math.min(3, inputLine));

        List<String> list = new ArrayList<>();
        if (lines != null) {
            list.addAll(lines);
        }
        while (list.size() < 4) {
            list.add("");
        }
        if (list.size() > 4) {
            list = list.subList(0, 4);
        }

        if (list.stream().allMatch(s -> s == null || s.trim().isEmpty())) {
            list.set(1, "^^^^^^^^^^^^^^");
            list.set(2, "Enter Value");
        }

        String[] signLines = new String[4];
        for (int i = 0; i < 4; i++) {
            String cleanLine = list.get(i);
            if (cleanLine == null) {
                cleanLine = "";
            }
            cleanLine = org.bukkit.ChatColor.translateAlternateColorCodes('&', cleanLine);
            signLines[i] = org.bukkit.ChatColor.stripColor(cleanLine);
        }

        ensureRegistered(plugin);

        PLUGINS.put(uuid, plugin);
        INPUT_LINE.put(uuid, lineIndex);
        EXPECTED_LINES.put(uuid, signLines);
        ORIGINAL_LINES.put(uuid, list);
        if (callback != null) {
            CALLBACK.put(uuid, callback);
        }

        player.setMetadata(META_SIGN_INPUT, new FixedMetadataValue(plugin, true));

        SpigotScheduler scheduler = getScheduler(plugin);

        // Close inventory immediately so player is ready for input
        try {
            player.closeInventory();
        } catch (Throwable ignored) {}

        // The sign editor only exists as a Bukkit API from 1.20 onwards. On 1.12.2 (and every
        // other pre-1.20 server) falling through to finish(player, null) here is what made every
        // sign-driven input silently do nothing, so use the chat transport instead.
        if (!isSignEditorAvailable()) {
            openChatInput(plugin, player, list, lineIndex, scheduler);
            return;
        }

        Placement placement = findGroundPlacement(player);
        if (placement == null) {
            openChatInput(plugin, player, list, lineIndex, scheduler);
            return;
        }

        SIGN_LOC.put(uuid, placement.loc);
        OLD_DATA.put(uuid, placement.oldData);

        // Schedule timeout (45 seconds)
        if (scheduler != null) {
            org.bukkit.scheduler.BukkitTask task = scheduler.runEntityLater(player, () -> {
                if (PLUGINS.containsKey(uuid)) {
                    cancel(player);
                    player.sendMessage(ColorUtils.toComponent(
                            message(plugin, "SIGN_INPUT.TIMED_OUT", "&cInput timed out.")));
                }
            }, INPUT_TIMEOUT_TICKS);
            HIDE_TASK.put(uuid, task);
        }

        Location loc = placement.loc.clone();
        MaterialData oldData = placement.oldData;

        Runnable openAction = () -> {
            Block block = loc.getBlock();
            Material signMaterial = resolveSignBlockMaterial();
            if (signMaterial == null) {
                // Should not happen: isSignEditorAvailable() already verified this.
                scheduleChatFallback(plugin, player, list, lineIndex, scheduler);
                return;
            }
            block.setType(signMaterial, false);

            BlockState blockState = block.getState();
            if (!(blockState instanceof Sign)) {
                finish(player, null);
                return;
            }
            Sign sign = (Sign) blockState;

            // Set sign lines (supporting 1.20+ Side API via reflection)
            try {
                Class<?> sideClass = Class.forName("org.bukkit.block.sign.Side");
                java.lang.reflect.Method getSideMethod = sign.getClass().getMethod("getSide", sideClass);
                Object frontSide = sideClass.getEnumConstants()[0]; // FRONT is index 0
                Object sideObject = getSideMethod.invoke(sign, frontSide);
                java.lang.reflect.Method setLineMethod = sideObject.getClass().getMethod("setLine", int.class, String.class);
                for (int i = 0; i < 4; i++) {
                    setLineMethod.invoke(sideObject, i, signLines[i]);
                }
            } catch (Throwable e) {
                // Fallback to pre-1.20
                for (int i = 0; i < 4; i++) {
                    sign.setLine(i, signLines[i]);
                }
            }

            // Set editable, unwaxed and allowed editor UUID
            try {
                java.lang.reflect.Method setEditable = sign.getClass().getMethod("setEditable", boolean.class);
                setEditable.invoke(sign, true);
            } catch (Throwable ignored) {}

            try {
                java.lang.reflect.Method setWaxed = sign.getClass().getMethod("setWaxed", boolean.class);
                setWaxed.invoke(sign, false);
            } catch (Throwable ignored) {}

            try {
                java.lang.reflect.Method setEditor = sign.getClass().getMethod("setAllowedEditorUniqueId", UUID.class);
                setEditor.invoke(sign, uuid);
            } catch (Throwable ignored) {}

            sign.update(true, false);

            // Open sign for player
            player.sendBlockChange(loc, sign.getType(), sign.getRawData());
            try {
                player.sendSignChange(loc, signLines);
            } catch (Throwable ignored) {}

            if (!invokeOpenSign(player, sign)) {
                finish(player, null);
                return;
            }

            // Hide from others immediately
            startHideFromOthers(plugin, player, loc, oldData);
        };

        if (scheduler != null) {
            scheduler.runRegion(loc, openAction);
        } else {
            openAction.run();
        }
    }

    // ── Chat transport ────────────────────────────────────────────────────────────────────────

    /**
     * Starts a chat-based input session. Used whenever the sign editor cannot be opened on this
     * server build (which is always the case on Spigot 1.12.2).
     */
    private static void openChatInput(JavaPlugin plugin, Player player, List<String> lines, int inputLine,
                                      SpigotScheduler scheduler) {
        UUID uuid = player.getUniqueId();

        List<String> prompt = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.trim().isEmpty()) {
                prompt.add(line);
            }
        }
        if (prompt.isEmpty()) {
            prompt.add("Enter Value");
        }

        CHAT_PROMPTS.put(uuid, new ChatPrompt(prompt));

        for (String line : prompt) {
            player.sendMessage(ColorUtils.toComponent(line));
        }
        player.sendMessage(ColorUtils.toComponent(
                message(plugin, "SIGN_INPUT.CHAT_HINT",
                        "&7Type your value in chat, or type &ccancel &7to go back.")));

        if (scheduler != null) {
            org.bukkit.scheduler.BukkitTask task = scheduler.runEntityLater(player, () -> {
                if (CHAT_PROMPTS.remove(uuid) != null) {
                    cancel(player);
                    player.sendMessage(ColorUtils.toComponent(
                            message(plugin, "SIGN_INPUT.TIMED_OUT", "&cInput timed out.")));
                }
            }, INPUT_TIMEOUT_TICKS);
            HIDE_TASK.put(uuid, task);
        }
    }

    private static void scheduleChatFallback(JavaPlugin plugin, Player player, List<String> lines, int inputLine,
                                             SpigotScheduler scheduler) {
        Runnable action = () -> openChatInput(plugin, player, lines, inputLine, scheduler);
        if (scheduler != null) {
            scheduler.runEntity(player, action);
        } else {
            action.run();
        }
    }

    /** Whether this player currently owes this utility an answer (sign or chat transport). */
    public static boolean hasPendingInput(UUID uuid) {
        return uuid != null && CHAT_PROMPTS.containsKey(uuid);
    }

    /**
     * Feeds a chat message into an open chat-input session.
     *
     * <p>Called from {@code ChatListener} so the message is consumed before the chat pipeline can
     * broadcast it. Runs on the main thread.</p>
     *
     * @return {@code true} when the message was consumed
     */
    public static boolean handlePendingInput(Player player, String rawMessage) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (!CHAT_PROMPTS.containsKey(uuid)) {
            return false;
        }

        String text = rawMessage == null ? "" : rawMessage.trim();
        if (text.equalsIgnoreCase(CANCEL_WORD)) {
            finish(player, null);
            return true;
        }
        finish(player, text);
        return true;
    }

    // ── Sign editor capability ────────────────────────────────────────────────────────────────

    /**
     * Resolves the standing-sign block material for this server, or {@code null} when the running
     * Bukkit build has none that can be used as a block.
     *
     * <p>Named {@code Material.SIGN} is the sign <em>item</em> on 1.12.2 ({@code SIGN(323, 16)}),
     * not a block, so placing it never produced a {@code Sign} block state. That was one of the
     * two independent reasons this utility could never open an editor on 1.12.2.</p>
     */
    private static Material resolveSignBlockMaterial() {
        for (String name : new String[] {"OAK_SIGN", "SIGN_POST", "STANDING_SIGN"}) {
            Material material = Material.matchMaterial(name);
            if (material != null && material.isBlock()) {
                return material;
            }
        }
        return null;
    }

    /**
     * Whether a sign editor can actually be opened on this server.
     *
     * <p>Requires both a usable sign block material and a {@code Player#openSign} method. The
     * reflection previously ran inside the open path and swallowed its failure into
     * {@code finish(player, null)}; probing up front means the failure is handled once, by
     * choosing the transport that works.</p>
     */
    private static boolean isSignEditorAvailable() {
        if (resolveSignBlockMaterial() == null) {
            return false;
        }
        try {
            Class<?> sideClass = Class.forName("org.bukkit.block.sign.Side");
            Player.class.getMethod("openSign", Sign.class, sideClass);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean invokeOpenSign(Player player, Sign sign) {
        try {
            Class<?> sideClass = Class.forName("org.bukkit.block.sign.Side");
            Object frontSide = sideClass.getEnumConstants()[0];
            java.lang.reflect.Method openSignMethod = player.getClass().getMethod("openSign", Sign.class, sideClass);
            openSignMethod.invoke(player, sign, frontSide);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────────────────────

    public static void cancel(Player player) {
        finish(player, null);
    }

    private static void finish(Player player, String text) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        CHAT_PROMPTS.remove(uuid);

        JavaPlugin plugin = PLUGINS.remove(uuid);
        Location loc = SIGN_LOC.remove(uuid);
        MaterialData oldData = OLD_DATA.remove(uuid);
        INPUT_LINE.remove(uuid);
        EXPECTED_LINES.remove(uuid);
        ORIGINAL_LINES.remove(uuid);
        Consumer<String> callback = CALLBACK.remove(uuid);
        org.bukkit.scheduler.BukkitTask task = HIDE_TASK.remove(uuid);

        if (task != null) {
            task.cancel();
        }

        if (plugin != null && player.hasMetadata(META_SIGN_INPUT)) {
            player.removeMetadata(META_SIGN_INPUT, plugin);
        }

        SpigotScheduler scheduler = getScheduler(plugin);

        if (loc != null && oldData != null && plugin != null && scheduler != null) {
            // Restore block in the world
            scheduler.runRegion(loc, () -> {
                Block block = loc.getBlock();
                block.setTypeId(oldData.getItemTypeId());
                block.setData(oldData.getData(), false);
                player.sendBlockChange(loc, oldData.getItemType(), oldData.getData());
                sendOriginalToOthers(plugin, player, loc, oldData);
            });
        }

        if (callback != null) {
            if (scheduler != null) {
                scheduler.runEntity(player, () -> callback.accept(text));
            } else {
                callback.accept(text);
            }
        }
    }

    private static Placement findGroundPlacement(Player player) {
        Location loc = player.getLocation().clone().add(0, 2.0, 0);
        int maxHeight = loc.getWorld().getMaxHeight() - 2;
        if (loc.getY() > maxHeight) {
            loc.setY(maxHeight);
        }
        Block block = loc.getBlock();
        return new Placement(block.getLocation(), block.getState().getData());
    }

    private static void startHideFromOthers(JavaPlugin plugin, Player player, Location loc, MaterialData oldData) {
        for (Player other : loc.getWorld().getPlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId()) && other.getLocation().distanceSquared(loc) < 2500) {
                other.sendBlockChange(loc, oldData.getItemType(), oldData.getData());
            }
        }
    }

    private static void sendOriginalToOthers(JavaPlugin plugin, Player player, Location loc, MaterialData oldData) {
        startHideFromOthers(plugin, player, loc, oldData);
    }

    private static void ensureRegistered(JavaPlugin plugin) {
        String name = plugin.getName();
        if (REGISTERED.add(name)) {
            Bukkit.getPluginManager().registerEvents(new InternalListener(), plugin);
        }
    }

public static final class Placement {
    private final Location loc;
    private final MaterialData oldData;

    public Placement(Location loc, MaterialData oldData) {
        this.loc = loc;
        this.oldData = oldData;
    }

    public Location loc() { return loc; }
    public MaterialData oldData() { return oldData; }

    @Override public String toString() {
        return "Placement[loc=+loc, oldData=+oldData]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Placement that = (Placement) o;
        return java.util.Objects.equals(loc, that.loc) && java.util.Objects.equals(oldData, that.oldData);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(loc, oldData);
    }
}

    /** The prompt shown to a player waiting on the chat transport. */
    private static final class ChatPrompt {
        private final List<String> lines;

        ChatPrompt(List<String> lines) {
            this.lines = lines;
        }

        List<String> lines() {
            return lines;
        }
    }

    private static final class InternalListener implements Listener {

        @EventHandler
        public void onSignChange(SignChangeEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            Location loc = SIGN_LOC.get(uuid);
            if (loc == null || !loc.getBlock().getLocation().equals(event.getBlock().getLocation())) {
                return;
            }

            event.setCancelled(true);
            int lineIdx = INPUT_LINE.getOrDefault(uuid, 1);

            String[] expected = EXPECTED_LINES.get(uuid);
            if (expected != null) {
                for (int i = 0; i < 4; i++) {
                    if (i == lineIdx) continue;
                    String currentLine = event.getLine(i);
                    if (currentLine == null) currentLine = "";
                    currentLine = org.bukkit.ChatColor.stripColor(currentLine).trim();
                    String expectedLine = expected[i];
                    if (expectedLine == null) expectedLine = "";
                    expectedLine = expectedLine.trim();
                    if (!currentLine.equals(expectedLine)) {
                        JavaPlugin plugin = PLUGINS.get(uuid);
                        String errorMsg = "&cyou cannot delete or modify the helper lines on the sign!";
                        if (plugin instanceof com.bx.ultimateDonutSmp.UltimateDonutSmp) {
                            com.bx.ultimateDonutSmp.UltimateDonutSmp uds = (com.bx.ultimateDonutSmp.UltimateDonutSmp) plugin;
                            errorMsg = uds.getConfigManager().getMessageOrDefault("ORDERS.SIGN_INPUT_HELPER_CANNOT_DELETE", errorMsg);
                        }
                        player.sendMessage(ColorUtils.toComponent(errorMsg));

                        List<String> origLines = ORIGINAL_LINES.get(uuid);
                        int inputIdx = INPUT_LINE.getOrDefault(uuid, 1);
                        Consumer<String> cb = CALLBACK.remove(uuid);

                        finish(player, null);

                        if (plugin != null && origLines != null && cb != null) {
                            SpigotScheduler scheduler = getScheduler(plugin);
                            if (scheduler != null) {
                                scheduler.runEntity(player, () -> {
                                    open(plugin, player, origLines, inputIdx, cb);
                                });
                            }
                        }
                        return;
                    }
                }
            }

            String text = event.getLine(lineIdx);
            finish(player, text);
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            cancel(event.getPlayer());
        }
    }
}
