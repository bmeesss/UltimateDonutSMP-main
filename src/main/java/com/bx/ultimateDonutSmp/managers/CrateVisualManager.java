package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.SoundUtils;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.EulerAngle;
import com.bx.ultimateDonutSmp.managers.CrateManager.CrateReward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CrateVisualManager {

    private static final String HOLOGRAM_TAG = "uds_crate_hologram";
    private static final String PERSONAL_TAG = "uds_crate_personal_hologram";
    private static final String PREVIEW_TAG = "uds_crate_preview";
    private static final String META_CRATE_HOLOGRAM = "uds_meta_crate_hologram";
    private static final String META_CRATE_PERSONAL_HOLOGRAM = "uds_meta_crate_personal_hologram";
    private static final String META_CRATE_HOLOGRAM_OWNER = "uds_meta_crate_hologram_owner";
    private static final String META_CRATE_PREVIEW = "uds_meta_crate_preview";
    private static final long GATE_RECHECK_INTERVAL_MS = 1000L;

    private final UltimateDonutSmp plugin;
    private final Map<CrateManager.CrateBlockKey, List<UUID>> holograms = new HashMap<>();
    private final Map<UUID, Map<CrateManager.CrateBlockKey, List<UUID>>> personalLineDisplays = new HashMap<>();
    private final Map<UUID, Map<CrateManager.CrateBlockKey, UUID>> personalKeyDisplays = new HashMap<>();
    private final Map<UUID, Map<CrateManager.CrateBlockKey, String>> personalKeyDisplayTexts = new HashMap<>();

    private final Map<CrateManager.CrateBlockKey, UUID> previews = new HashMap<>();
    private final Map<CrateManager.CrateBlockKey, Integer> previewIndices = new HashMap<>();
    private final Map<CrateManager.CrateBlockKey, Integer> previewCycleTicks = new HashMap<>();

    private BukkitTask visualTask;
    private int visualPulse;

    private BukkitTask animationTask;
    private int animationPulse;

    private long gateCheckedAtMillis;
    private boolean gateOpen = true;

    public CrateVisualManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        shutdown();
        if (!isCratesEnabled()) {
            return;
        }
        purgeNearbyDisplaysForAllBoundCrates();
        purgeAllCrateHologramsInWorlds();
        spawnAllHolograms();
        visualPulse = 0;
        startVisualTask();
    }

    public void shutdown() {
        if (visualTask != null) {
            visualTask.cancel();
            visualTask = null;
        }
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
        clearAllPersonalLineDisplays();
        clearAllPersonalDisplays();
        clearAllHolograms();
        purgeNearbyDisplaysForAllBoundCrates();
        purgeAllCrateHologramsInWorlds();
    }

    public void handleJoin(Player player) {
        // Personal crate displays are hidden by default, so joins do not need per-entity hide calls.
    }

    public void handleQuit(UUID playerId) {
        if (playerId != null) {
            clearPersonalLineDisplays(playerId);
            clearPersonalDisplays(playerId);
        }
    }

    public void handleWorldChange(Player player) {
        if (player != null) {
            clearPersonalLineDisplays(player.getUniqueId());
            clearPersonalDisplays(player.getUniqueId());
        }
    }

    public void refreshHologram(Block block) {
        if (block == null || block.getWorld() == null || !isCratesEnabled()) {
            return;
        }

        CrateManager.CrateBlockKey key = toKey(block);
        removeHologram(key);
        purgeNearbyDisplaysForCrate(key);
    }

    public void removeHologram(Block block) {
        if (block == null || block.getWorld() == null) {
            return;
        }
        removeHologram(toKey(block));
    }

    public void playOpenEffects(Player player, Block block, CrateManager.CrateDefinition crate) {
        if (player == null || block == null || crate == null || block.getWorld() == null || !isCratesEnabled()) {
            return;
        }

        SoundUtils.play(player, plugin.getConfigManager().getSounds().getString(
                "CRATES.OPEN",
                "minecraft:block.ender_chest.open|1.0|1.05"
        ));

        Location origin = block.getLocation().add(0.5, 1.0, 0.5);
        block.getWorld().spawnParticle(Particle.END_ROD, origin, 10, 0.25, 0.35, 0.25, 0.01);
        block.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, origin.clone().add(0, 0.25, 0), 8, 0.3, 0.3, 0.3, 0.0);
        animateLid(block);
    }

    public void playNoKeyEffects(Player player) {
        if (player == null || !isCratesEnabled()) {
            return;
        }
        SoundUtils.play(player, plugin.getConfigManager().getSounds().getString(
                "CRATES.NO-KEY",
                "minecraft:entity.villager.no|1.0|1.0"
        ));
    }

    public void playClaimEffects(Player player, CrateManager.CrateDefinition crate) {
        if (player == null || crate == null || player.getWorld() == null || !isCratesEnabled()) {
            return;
        }

        SoundUtils.play(player, plugin.getConfigManager().getSounds().getString(
                "CRATES.CLAIM",
                "minecraft:entity.player.levelup|1.0|1.25"
        ));

        Location origin = player.getLocation().add(0, 1.0, 0);
        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, origin, 20, 0.4, 0.6, 0.4, 0.05);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, origin, 15, 0.35, 0.5, 0.35, 0.0);
    }

    private void startVisualTask() {
        long updateTicks = getHologramUpdateTicks();
        visualTask = plugin.getSpigotScheduler().runGlobalTimer(() -> {
            if (!isCratesEnabled()) {
                shutdown();
                return;
            }
            visualPulse++;
            spawnIdleParticles();
            ensureGlobalHolograms();
            updatePersonalKeyDisplays();
        }, updateTicks, updateTicks);

        if (isPreviewEnabled()) {
            startAnimationTask();
        }
    }

    private boolean isCratesEnabled() {
        return plugin.getFeatureManager().isEnabled(FeatureManager.Feature.CRATES);
    }

    private void spawnAllHolograms() {
        for (Map.Entry<CrateManager.CrateBlockKey, String> entry : plugin.getCrateManager().getBoundBlockIds().entrySet()) {
            CrateManager.CrateBlockKey key = entry.getKey();
            World world = Bukkit.getWorld(key.world());
            if (world == null) {
                continue;
            }

            Location loc = new Location(world, key.x() + 0.5, key.y() + 0.5, key.z() + 0.5);
            plugin.getSpigotScheduler().runRegion(loc, () -> {
                Block block = world.getBlockAt(key.x(), key.y(), key.z());
                CrateManager.CrateDefinition crate = plugin.getCrateManager().getCrate(entry.getValue());
                if (crate == null) {
                    return;
                }

                synchronized (holograms) {
                    spawnHologram(block, crate);
                    if (isPreviewEnabled()) {
                        spawnPreview(block, crate);
                    }
                }
            });
        }
    }

    private void spawnHologram(Block block, CrateManager.CrateDefinition crate) {
        CrateManager.CrateBlockKey key = toKey(block);
        removeTrackedGlobalHologram(key);

        List<String> lines = getHologramLines(crate);
        if (lines.isEmpty()) {
            return;
        }

        List<UUID> entityIds = new ArrayList<>();
        double baseY = block.getY() + getHologramOffsetY();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Location location = new Location(block.getWorld(), block.getX() + 0.5, baseY - (i * 0.27), block.getZ() + 0.5);
            ArmorStand display = spawnTextStand(location, line);
            configureHologramDisplay(display);
            display.addScoreboardTag(HOLOGRAM_TAG);
            setMetadata(display, META_CRATE_HOLOGRAM, formatBlockKey(key));
            entityIds.add(display.getUniqueId());
        }

        synchronized (holograms) {
            holograms.put(key, entityIds);
        }
    }

    private void ensureGlobalHolograms() {
        for (Map.Entry<CrateManager.CrateBlockKey, String> entry : plugin.getCrateManager().getBoundBlockIds().entrySet()) {
            CrateManager.CrateBlockKey key = entry.getKey();
            CrateManager.CrateDefinition crate = plugin.getCrateManager().getCrate(entry.getValue());
            if (crate == null) {
                continue;
            }

            World world = Bukkit.getWorld(key.world());
            if (world == null) {
                continue;
            }

            Location center = new Location(world, key.x() + 0.5, key.y() + getHologramOffsetY() - 0.35, key.z() + 0.5);
            plugin.getSpigotScheduler().runRegion(center, () -> {
                if (!world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) {
                    return;
                }

                Block block = world.getBlockAt(key.x(), key.y(), key.z());

                synchronized (holograms) {
                    if (!hasValidGlobalHologram(key, getHologramLines(crate).size())) {
                        spawnHologram(block, crate);
                    }

                    if (isPreviewEnabled() && !hasValidPreview(key)) {
                        spawnPreview(block, crate);
                    }
                }
            });
        }
    }

    private boolean hasValidGlobalHologram(CrateManager.CrateBlockKey key, int requiredLines) {
        List<UUID> trackedIds;
        synchronized (holograms) {
            trackedIds = holograms.get(key);
        }

        List<ArmorStand> displays = new ArrayList<>();
        if (trackedIds != null && trackedIds.size() == requiredLines) {
            for (UUID entityId : trackedIds) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity instanceof ArmorStand && entity.isValid()) {
                    ArmorStand td = (ArmorStand) entity;
                    if (td.getScoreboardTags().contains(HOLOGRAM_TAG)) {
                        displays.add(td);
                    }
                }
            }
        }

        if (displays.size() != requiredLines) {
            World world = Bukkit.getWorld(key.world());
            if (world != null) {
                Location center = new Location(world, key.x() + 0.5, key.y() + getHologramOffsetY() - 0.35, key.z() + 0.5);
                String expectedKey = formatBlockKey(key);

                List<ArmorStand> found = new ArrayList<>();
                for (Entity entity : world.getNearbyEntities(center, 0.5, 1.0, 0.5)) {
                    if (!(entity instanceof ArmorStand)) {
                        continue;
                    }
                    if (!entity.getScoreboardTags().contains(HOLOGRAM_TAG) || entity.getScoreboardTags().contains(PERSONAL_TAG)) {
                        continue;
                    }
                    if (hasMetadata(entity, META_CRATE_HOLOGRAM_OWNER)
                            || hasMetadata(entity, META_CRATE_PERSONAL_HOLOGRAM)) {
                        continue;
                    }
                    String attachedKey = getMetadata(entity, META_CRATE_HOLOGRAM);
                    if (expectedKey.equals(attachedKey) && entity.isValid()) {
                        found.add((ArmorStand) entity);
                    }
                }

                found.sort((a, b) -> Double.compare(b.getLocation().getY(), a.getLocation().getY()));

                if (found.size() >= requiredLines) {
                    displays.clear();
                    for (int i = 0; i < requiredLines; i++) {
                        displays.add(found.get(i));
                    }
                    for (int i = requiredLines; i < found.size(); i++) {
                        found.get(i).remove();
                    }

                    List<UUID> newIds = new ArrayList<>();
                    for (ArmorStand td : displays) {
                        newIds.add(td.getUniqueId());
                    }
                    synchronized (holograms) {
                        holograms.put(key, newIds);
                    }
                } else {
                    for (ArmorStand td : found) {
                        td.remove();
                    }
                }
            }
        }

        if (displays.size() == requiredLines) {
            return true;
        }

        removeTrackedGlobalHologram(key);
        return false;
    }

    private void updatePersonalKeyDisplays() {
        double viewDistance = getHologramViewDistance();
        boolean unlimitedDistance = viewDistance <= 0D;
        double maxDistanceSquared = unlimitedDistance ? 0D : Math.pow(viewDistance, 2);
        Map<UUID, Set<CrateManager.CrateBlockKey>> activeDisplays = new HashMap<>();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Set<CrateManager.CrateBlockKey> playerActive = new HashSet<>();
            activeDisplays.put(player.getUniqueId(), playerActive);

            for (Map.Entry<CrateManager.CrateBlockKey, String> entry : plugin.getCrateManager().getBoundBlockIds().entrySet()) {
                CrateManager.CrateBlockKey key = entry.getKey();
                if (!player.getWorld().getName().equals(key.world())) {
                    continue;
                }

                double distanceSquared = player.getLocation().distanceSquared(new Location(player.getWorld(), key.x() + 0.5, key.y() + 0.5, key.z() + 0.5));
                if (!unlimitedDistance && distanceSquared > maxDistanceSquared) {
                    continue;
                }

                CrateManager.CrateDefinition crate = plugin.getCrateManager().getCrate(entry.getValue());
                if (crate == null) {
                    continue;
                }

                playerActive.add(key);
                
                Location crateLoc = new Location(player.getWorld(), key.x() + 0.5, key.y() + 0.5, key.z() + 0.5);
                plugin.getSpigotScheduler().runRegion(crateLoc, () -> {
                    synchronized (holograms) {
                        upsertPersonalKeyDisplay(player, key, crate);
                    }
                });
            }
        }

        Map<UUID, Map<CrateManager.CrateBlockKey, UUID>> copy;
        synchronized (holograms) {
            copy = new HashMap<>(personalKeyDisplays);
        }

        for (Map.Entry<UUID, Map<CrateManager.CrateBlockKey, UUID>> entry : copy.entrySet()) {
            UUID viewerId = entry.getKey();
            Set<CrateManager.CrateBlockKey> active = activeDisplays.getOrDefault(viewerId, java.util.Collections.emptySet());
            for (CrateManager.CrateBlockKey key : new HashSet<>(entry.getValue().keySet())) {
                if (!active.contains(key)) {
                    World world = Bukkit.getWorld(key.world());
                    if (world != null) {
                        Location crateLoc = new Location(world, key.x() + 0.5, key.y() + 0.5, key.z() + 0.5);
                        plugin.getSpigotScheduler().runRegion(crateLoc, () -> {
                            synchronized (holograms) {
                                removePersonalKeyDisplay(viewerId, key);
                            }
                        });
                    }
                }
            }
        }
    }

    private void upsertPersonalLineDisplays(Player player, CrateManager.CrateBlockKey key, CrateManager.CrateDefinition crate) {
        removePersonalLineDisplays(player.getUniqueId(), key);
    }

    private void upsertPersonalKeyDisplay(Player player, CrateManager.CrateBlockKey key, CrateManager.CrateDefinition crate) {
        World world = Bukkit.getWorld(key.world());
        if (world == null) {
            return;
        }

        Map<CrateManager.CrateBlockKey, UUID> playerDisplays = personalKeyDisplays.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new HashMap<>()
        );
        Map<CrateManager.CrateBlockKey, String> playerDisplayTexts = personalKeyDisplayTexts.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new HashMap<>()
        );

        ArmorStand display = null;
        UUID existingId = playerDisplays.get(key);
        if (existingId != null) {
            Entity entity = Bukkit.getEntity(existingId);
            if (entity instanceof ArmorStand && entity.isValid()) {
                display = (ArmorStand) entity;
            } else {
                playerDisplays.remove(key);
                playerDisplayTexts.remove(key);
            }
        }

        if (display == null) {
            double baseY;
            double configuredKeyLineOffset = plugin.getConfigManager().getCrates().getDouble("SETTINGS.HOLOGRAM.KEY-LINE-OFFSET-Y", -999.0D);
            if (configuredKeyLineOffset != -999.0D) {
                baseY = key.y() + configuredKeyLineOffset;
            } else {
                baseY = key.y() + getHologramOffsetY() - (getHologramLines(crate).size() * 0.27D);
            }
            Location location = new Location(world, key.x() + 0.5, baseY, key.z() + 0.5);
            display = spawnTextStand(location, "");
            configureHologramDisplay(display);
            display.addScoreboardTag(PERSONAL_TAG);
            setMetadata(display, META_CRATE_PERSONAL_HOLOGRAM, formatBlockKey(key));
            setMetadata(display, META_CRATE_HOLOGRAM_OWNER, player.getUniqueId().toString());
            playerDisplays.put(key, display.getUniqueId());
        }

        String text = getKeyLine(crate, plugin.getCrateManager().getKeyBalance(player, crate.id()));
        if (!text.equals(playerDisplayTexts.get(key))) {
            display.setCustomName(com.bx.ultimateDonutSmp.utils.ColorUtils.toComponent(text, player));
            playerDisplayTexts.put(key, text);
        }
    }

    public void removeHologram(String worldName, int x, int y, int z) {
        removeHologram(new CrateManager.CrateBlockKey(worldName, x, y, z));
    }

    private void removeHologram(CrateManager.CrateBlockKey key) {
        World world = Bukkit.getWorld(key.world());
        if (world == null) {
            return;
        }

        Location center = new Location(world, key.x() + 0.5, key.y() + getHologramOffsetY() - 0.35, key.z() + 0.5);
        plugin.getSpigotScheduler().runRegion(center, () -> {
            synchronized (holograms) {
                removeTrackedGlobalHologram(key);
                removeTrackedPreview(key);

                for (UUID viewerId : new HashSet<>(personalLineDisplays.keySet())) {
                    removePersonalLineDisplays(viewerId, key);
                }

                for (UUID viewerId : new HashSet<>(personalKeyDisplays.keySet())) {
                    removePersonalKeyDisplay(viewerId, key);
                }

                String expected = formatBlockKey(key);
                for (Entity entity : world.getNearbyEntities(center, 1.0, 3.0, 1.0)) {
                    if (!(entity instanceof ArmorStand)) {
                        continue;
                    }
                    String attachedKey = getMetadata(entity, META_CRATE_HOLOGRAM);
                    if (attachedKey == null) {
                        attachedKey = getMetadata(entity, META_CRATE_PERSONAL_HOLOGRAM);
                    }
                    if (attachedKey == null) {
                        attachedKey = getMetadata(entity, META_CRATE_PREVIEW);
                    }
                    if (expected.equals(attachedKey)) {
                        entity.remove();
                    }
                }
            }
        });
    }

    private void removeTrackedGlobalHologram(CrateManager.CrateBlockKey key) {
        List<UUID> entityIds;
        synchronized (holograms) {
            entityIds = holograms.remove(key);
        }
        if (entityIds == null) {
            return;
        }

        for (UUID entityId : entityIds) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
    }

    private void clearAllHolograms() {
        if (plugin.getSpigotScheduler().isFolia()) {
            clearAllHologramsFolia();
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(ArmorStand.class)) {
                if (entity.getScoreboardTags().contains(HOLOGRAM_TAG)) {
                    entity.remove();
                }
            }
            for (Entity entity : world.getEntitiesByClass(ArmorStand.class)) {
                if (entity.getScoreboardTags().contains(PREVIEW_TAG)) {
                    entity.remove();
                }
            }
        }
        synchronized (holograms) {
            holograms.clear();
            previews.clear();
            previewIndices.clear();
            previewCycleTicks.clear();
        }
    }

    private void clearAllHologramsFolia() {
        synchronized (holograms) {
            for (Map.Entry<CrateManager.CrateBlockKey, List<UUID>> entry : holograms.entrySet()) {
                CrateManager.CrateBlockKey key = entry.getKey();
                World world = Bukkit.getWorld(key.world());
                if (world != null) {
                    Location center = new Location(world, key.x() + 0.5, key.y() + getHologramOffsetY() - 0.35, key.z() + 0.5);
                    List<UUID> ids = new ArrayList<>(entry.getValue());
                    plugin.getSpigotScheduler().runRegion(center, () -> {
                        for (UUID id : ids) {
                            Entity entity = Bukkit.getEntity(id);
                            if (entity != null) {
                                entity.remove();
                            }
                        }
                    });
                }
            }
            for (Map.Entry<CrateManager.CrateBlockKey, UUID> entry : previews.entrySet()) {
                CrateManager.CrateBlockKey key = entry.getKey();
                World world = Bukkit.getWorld(key.world());
                if (world != null) {
                    Location center = new Location(world, key.x() + 0.5, key.y() + getHologramOffsetY() - 0.35, key.z() + 0.5);
                    UUID id = entry.getValue();
                    plugin.getSpigotScheduler().runRegion(center, () -> {
                        Entity entity = Bukkit.getEntity(id);
                        if (entity != null) {
                            entity.remove();
                        }
                    });
                }
            }
            holograms.clear();
            previews.clear();
            previewIndices.clear();
            previewCycleTicks.clear();
        }
    }

    private void purgeAllCrateHologramsInWorlds() {
        if (plugin.getSpigotScheduler().isFolia()) {
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(ArmorStand.class)) {
                if (!isManagedCrateHologram(entity)) {
                    continue;
                }
                entity.remove();
            }
            for (Entity entity : world.getEntitiesByClass(ArmorStand.class)) {
                if (!isManagedCratePreview(entity)) {
                    continue;
                }
                entity.remove();
            }
        }
    }

    private void purgeNearbyDisplaysForAllBoundCrates() {
        for (CrateManager.CrateBlockKey key : plugin.getCrateManager().getBoundBlockIds().keySet()) {
            purgeNearbyDisplaysForCrate(key);
        }
    }

    private void purgeUntrackedDisplaysForBoundCrates() {
        for (CrateManager.CrateBlockKey key : plugin.getCrateManager().getBoundBlockIds().keySet()) {
            purgeUntrackedDisplaysForCrate(key);
        }
    }

    private void purgeNearbyDisplaysForCrate(CrateManager.CrateBlockKey key) {
        World world = Bukkit.getWorld(key.world());
        if (world == null) {
            return;
        }

        Location center = new Location(world, key.x() + 0.5, key.y() + getHologramOffsetY() - 0.35, key.z() + 0.5);
        if (!plugin.isEnabled()) {
            if (world.isChunkLoaded(center.getBlockX() >> 4, center.getBlockZ() >> 4)) {
                for (Entity entity : world.getNearbyEntities(center, 0.4, 2.5, 0.4)) {
                    if (!(entity instanceof ArmorStand)) {
                        continue;
                    }
                    if (isAtCrateHologramColumn(entity, key) || isAtCratePreviewColumn(entity, key)) {
                        entity.remove();
                    }
                }
            }
            return;
        }
        plugin.getSpigotScheduler().runRegion(center, () -> {
            for (Entity entity : world.getNearbyEntities(center, 0.4, 2.5, 0.4)) {
                if (!(entity instanceof ArmorStand)) {
                    continue;
                }
                if (isAtCrateHologramColumn(entity, key) || isAtCratePreviewColumn(entity, key)) {
                    entity.remove();
                }
            }
        });
    }

    private void purgeUntrackedDisplaysForCrate(CrateManager.CrateBlockKey key) {
        World world = Bukkit.getWorld(key.world());
        if (world == null) {
            return;
        }

        Set<UUID> trackedIds = collectTrackedDisplayIds(key);
        Location center = new Location(world, key.x() + 0.5, key.y() + getHologramOffsetY() - 0.35, key.z() + 0.5);
        plugin.getSpigotScheduler().runRegion(center, () -> {
            for (Entity entity : world.getNearbyEntities(center, 0.4, 2.5, 0.4)) {
                if (!(entity instanceof ArmorStand)) {
                    continue;
                }
                if (isAtCrateHologramColumn(entity, key)) {
                    if (!trackedIds.contains(entity.getUniqueId())) {
                        entity.remove();
                    }
                } else if (isAtCratePreviewColumn(entity, key)) {
                    if (!trackedIds.contains(entity.getUniqueId())) {
                        entity.remove();
                    }
                }
            }
        });
    }

    private void clearAllPersonalLineDisplays() {
        for (Map<CrateManager.CrateBlockKey, List<UUID>> displays : personalLineDisplays.values()) {
            for (List<UUID> entityIds : displays.values()) {
                for (UUID entityId : entityIds) {
                    Entity entity = Bukkit.getEntity(entityId);
                    if (entity != null && entity.isValid()) {
                        entity.remove();
                    }
                }
            }
        }
        personalLineDisplays.clear();
    }

    private void clearAllPersonalDisplays() {
        for (Map<CrateManager.CrateBlockKey, UUID> displays : personalKeyDisplays.values()) {
            for (UUID entityId : displays.values()) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
            }
        }
        personalKeyDisplays.clear();
        personalKeyDisplayTexts.clear();
    }

    private void clearPersonalLineDisplays(UUID viewerId) {
        Map<CrateManager.CrateBlockKey, List<UUID>> displays = personalLineDisplays.remove(viewerId);
        if (displays == null) {
            return;
        }

        for (List<UUID> entityIds : displays.values()) {
            for (UUID entityId : entityIds) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
            }
        }
    }

    private void clearPersonalDisplays(UUID viewerId) {
        Map<CrateManager.CrateBlockKey, UUID> displays = personalKeyDisplays.remove(viewerId);
        if (displays == null) {
            personalKeyDisplayTexts.remove(viewerId);
            return;
        }

        for (UUID entityId : displays.values()) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        personalKeyDisplayTexts.remove(viewerId);
    }

    private void removePersonalLineDisplays(UUID viewerId, CrateManager.CrateBlockKey key) {
        Map<CrateManager.CrateBlockKey, List<UUID>> displays = personalLineDisplays.get(viewerId);
        if (displays == null) {
            return;
        }

        List<UUID> entityIds = displays.remove(key);
        if (entityIds != null) {
            for (UUID entityId : entityIds) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
            }
        }

        if (displays.isEmpty()) {
            personalLineDisplays.remove(viewerId);
        }
    }

    private void removePersonalKeyDisplay(UUID viewerId, CrateManager.CrateBlockKey key) {
        Map<CrateManager.CrateBlockKey, UUID> displays = personalKeyDisplays.get(viewerId);
        if (displays == null) {
            return;
        }

        UUID entityId = displays.remove(key);
        if (entityId != null) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        Map<CrateManager.CrateBlockKey, String> textCache = personalKeyDisplayTexts.get(viewerId);
        if (textCache != null) {
            textCache.remove(key);
            if (textCache.isEmpty()) {
                personalKeyDisplayTexts.remove(viewerId);
            }
        }

        if (displays.isEmpty()) {
            personalKeyDisplays.remove(viewerId);
        }
    }

    private void spawnIdleParticles() {
        if (!plugin.getConfigManager().getCrates().getBoolean("SETTINGS.PARTICLES.ENABLED", true)) {
            return;
        }

        Particle particle = parseParticle(
                plugin.getConfigManager().getCrates().getString("SETTINGS.PARTICLES.TYPE", "ENCHANTMENT_TABLE")
        );
        int count = Math.max(1, plugin.getConfigManager().getCrates().getInt("SETTINGS.PARTICLES.COUNT", 4));

        for (CrateManager.CrateBlockKey key : plugin.getCrateManager().getBoundBlockIds().keySet()) {
            World world = Bukkit.getWorld(key.world());
            if (world == null) {
                continue;
            }

            Location location = new Location(world, key.x() + 0.5, key.y() + 1.05, key.z() + 0.5);
            plugin.getSpigotScheduler().runRegion(location, () -> {
                if (!world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) {
                    return;
                }
                world.spawnParticle(particle, location, count, 0.18, 0.25, 0.18, 0.02);
                world.spawnParticle(Particle.FIREWORKS_SPARK, location.clone().add(0, 0.15, 0), 1, 0.06, 0.06, 0.06, 0.0);
            });
        }
    }

    private void animateLid(Block block) {
        // 1.12.2 has no Lidded API; open sound/effects are handled elsewhere.
    }

    private void configureHologramDisplay(ArmorStand stand) {
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setCustomNameVisible(true);
        stand.setSmall(true);
        try {
            stand.setMarker(true);
        } catch (Throwable ignored) {
        }
        try {
            stand.setBasePlate(false);
            stand.setArms(false);
        } catch (Throwable ignored) {
        }
    }

    private ArmorStand spawnTextStand(Location location, String text) {
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        stand.setCustomName(com.bx.ultimateDonutSmp.utils.ColorUtils.toComponent(text));
        configureHologramDisplay(stand);
        return stand;
    }

    private ArmorStand spawnPreviewStand(Location location) {
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setCustomNameVisible(false);
        stand.setSmall(true);
        try {
            stand.setMarker(true);
            stand.setBasePlate(false);
            stand.setArms(false);
        } catch (Throwable ignored) {
        }
        return stand;
    }

    private List<String> getHologramLines(CrateManager.CrateDefinition crate) {
        FileConfiguration cratesConfig = plugin.getConfigManager().getCrates();
        List<String> lines = cratesConfig.getStringList("SETTINGS.HOLOGRAM.LINES");
        if (lines.isEmpty()) {
            lines = new java.util.ArrayList<>(java.util.Arrays.asList(
                    "{crate}", 
                    "&7Right-click to open"
            ));
        }

        List<String> resolved = new ArrayList<>();
        for (String line : lines) {
            resolved.add(line
                    .replace("{crate}", crate.display().displayName())
                    .replace("{crate_id}", crate.id()));
        }
        return resolved;
    }

    private String getKeyLine(CrateManager.CrateDefinition crate, int keys) {
        String template = plugin.getConfigManager().getCrates().getString(
                "SETTINGS.HOLOGRAM.KEY-LINE",
                "&7keys: &f{keys}"
        );
        return template
                .replace("{crate}", crate.display().displayName())
                .replace("{crate_id}", crate.id())
                .replace("{keys}", String.valueOf(keys));
    }

    private double getHologramOffsetY() {
        return plugin.getConfigManager().getCrates().getDouble("SETTINGS.HOLOGRAM.OFFSET-Y", 1.6D);
    }

    private long getHologramUpdateTicks() {
        return Math.max(1L, plugin.getConfigManager().getCrates().getLong("SETTINGS.HOLOGRAM.UPDATE-TICKS", 20L));
    }

    private double getHologramViewDistance() {
        return plugin.getConfigManager().getCrates().getDouble("SETTINGS.HOLOGRAM.VIEW-DISTANCE", 24D);
    }

    private Particle parseParticle(String particleName) {
        if (particleName == null || particleName.trim().isEmpty()) {
            return Particle.ENCHANTMENT_TABLE;
        }

        try {
            return Particle.valueOf(particleName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Particle.ENCHANTMENT_TABLE;
        }
    }

    private CrateManager.CrateBlockKey toKey(Block block) {
        return new CrateManager.CrateBlockKey(
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ()
        );
    }

    private String formatBlockKey(CrateManager.CrateBlockKey key) {
        return key.world().toLowerCase(Locale.ROOT) + ":" + key.x() + ":" + key.y() + ":" + key.z();
    }

    private void setMetadata(Entity entity, String key, String value) {
        entity.setMetadata(key, new FixedMetadataValue(plugin, value));
    }

    private String getMetadata(Entity entity, String key) {
        for (MetadataValue metadataValue : entity.getMetadata(key)) {
            if (metadataValue.getOwningPlugin() == plugin) {
                return metadataValue.asString();
            }
        }
        return null;
    }

    private boolean hasMetadata(Entity entity, String key) {
        return getMetadata(entity, key) != null;
    }

    private boolean isManagedCrateHologram(Entity entity) {
        if (!(entity instanceof ArmorStand)) {
            return false;
        }

        if (entity.getScoreboardTags().contains(HOLOGRAM_TAG) || entity.getScoreboardTags().contains(PERSONAL_TAG)) {
            return true;
        }

        return hasMetadata(entity, META_CRATE_HOLOGRAM)
                || hasMetadata(entity, META_CRATE_PERSONAL_HOLOGRAM)
                || hasMetadata(entity, META_CRATE_HOLOGRAM_OWNER);
    }

    private boolean isAtCrateHologramColumn(Entity entity, CrateManager.CrateBlockKey key) {
        if (entity == null || entity.getWorld() == null || !entity.getWorld().getName().equals(key.world())) {
            return false;
        }

        Location location = entity.getLocation();
        double deltaX = Math.abs(location.getX() - (key.x() + 0.5D));
        double deltaZ = Math.abs(location.getZ() - (key.z() + 0.5D));
        double minY = key.y() - 1.5D;
        double maxY = key.y() + getHologramOffsetY() + 1.0D;

        return deltaX <= 0.2D
                && deltaZ <= 0.2D
                && location.getY() >= minY
                && location.getY() <= maxY;
    }

    private Set<UUID> collectTrackedDisplayIds(CrateManager.CrateBlockKey key) {
        synchronized (holograms) {
            Set<UUID> ids = new HashSet<>();
            List<UUID> globalHolo = holograms.get(key);
            if (globalHolo != null) {
                ids.addAll(globalHolo);
            }
            UUID globalPreview = previews.get(key);
            if (globalPreview != null) {
                ids.add(globalPreview);
            }

            for (Map<CrateManager.CrateBlockKey, List<UUID>> displays : personalLineDisplays.values()) {
                List<UUID> list = displays.get(key);
                if (list != null) {
                    ids.addAll(list);
                }
            }

            for (Map<CrateManager.CrateBlockKey, UUID> displays : personalKeyDisplays.values()) {
                UUID id = displays.get(key);
                if (id != null) {
                    ids.add(id);
                }
            }

            return ids;
        }
    }

    private boolean isManagedCratePreview(Entity entity) {
        if (!(entity instanceof ArmorStand)) {
            return false;
        }

        if (entity.getScoreboardTags().contains(PREVIEW_TAG)) {
            return true;
        }

        return hasMetadata(entity, META_CRATE_PREVIEW);
    }

    private boolean isAtCratePreviewColumn(Entity entity, CrateManager.CrateBlockKey key) {
        if (entity == null || entity.getWorld() == null || !entity.getWorld().getName().equals(key.world())) {
            return false;
        }

        Location location = entity.getLocation();
        double deltaX = Math.abs(location.getX() - (key.x() + 0.5D));
        double deltaZ = Math.abs(location.getZ() - (key.z() + 0.5D));
        double minY = key.y() + getPreviewOffsetY() - 0.5D;
        double maxY = key.y() + getPreviewOffsetY() + 0.5D;

        return deltaX <= 0.2D
                && deltaZ <= 0.2D
                && location.getY() >= minY
                && location.getY() <= maxY;
    }

    private boolean isPreviewEnabled() {
        return plugin.getConfigManager().getCrates().getBoolean("SETTINGS.PREVIEW.ENABLED", true);
    }

    /**
     * The animation task ticks every couple of ticks, and walking the config tree for both toggles
     * cost far more than the animation it guards. One second of staleness on a toggle is fine.
     */
    private boolean previewGateOpen() {
        long now = System.currentTimeMillis();
        if (now - gateCheckedAtMillis >= GATE_RECHECK_INTERVAL_MS) {
            gateCheckedAtMillis = now;
            gateOpen = isCratesEnabled() && isPreviewEnabled();
        }
        return gateOpen;
    }

    private double getPreviewOffsetY() {
        return plugin.getConfigManager().getCrates().getDouble("SETTINGS.PREVIEW.OFFSET-Y", 1.25D);
    }

    private int getPreviewUpdateIntervalTicks() {
        return Math.max(1, plugin.getConfigManager().getCrates().getInt("SETTINGS.PREVIEW.UPDATE-INTERVAL-TICKS", 2));
    }

    private int getPreviewCycleIntervalTicks() {
        return plugin.getConfigManager().getCrates().getInt("SETTINGS.PREVIEW.CYCLE-INTERVAL-TICKS", 40);
    }

    private void spawnPreview(Block block, CrateManager.CrateDefinition crate) {
        CrateManager.CrateBlockKey key = toKey(block);
        removeTrackedPreview(key);

        if (!isPreviewEnabled() || crate.rewards().isEmpty()) {
            return;
        }

        Location location = new Location(block.getWorld(), block.getX() + 0.5, block.getY() + getPreviewOffsetY(), block.getZ() + 0.5);
        ArmorStand display = spawnPreviewStand(location);
        CrateReward reward = crate.rewards().get(0);
        ItemStack item = plugin.getCrateManager().createRewardDisplayItem(null, crate, reward);
        display.getEquipment().setHelmet(item);
        display.addScoreboardTag(PREVIEW_TAG);
        setMetadata(display, META_CRATE_PREVIEW, formatBlockKey(key));

        synchronized (holograms) {
            previews.put(key, display.getUniqueId());
            previewIndices.put(key, 0);
            previewCycleTicks.put(key, 0);
        }
    }

    private void removeTrackedPreview(CrateManager.CrateBlockKey key) {
        UUID entityId;
        synchronized (holograms) {
            entityId = previews.remove(key);
            previewIndices.remove(key);
            previewCycleTicks.remove(key);
        }
        if (entityId == null) {
            return;
        }

        Entity entity = Bukkit.getEntity(entityId);
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    private boolean hasValidPreview(CrateManager.CrateBlockKey key) {
        UUID entityId;
        synchronized (holograms) {
            entityId = previews.get(key);
        }

        ArmorStand display = null;
        if (entityId != null) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity instanceof ArmorStand && entity.isValid()) {
                ArmorStand id = (ArmorStand) entity;
                if (id.getScoreboardTags().contains(PREVIEW_TAG)) {
                    display = id;
                }
            }
        }

        if (display == null) {
            World world = Bukkit.getWorld(key.world());
            if (world != null) {
                Location center = new Location(world, key.x() + 0.5, key.y() + getPreviewOffsetY(), key.z() + 0.5);
                String expectedKey = formatBlockKey(key);
                List<ArmorStand> found = new ArrayList<>();
                for (Entity entity : world.getNearbyEntities(center, 0.5, 0.5, 0.5)) {
                    if (!(entity instanceof ArmorStand)) {
                        continue;
                    }
                    String attachedKey = getMetadata(entity, META_CRATE_PREVIEW);
                    if (expectedKey.equals(attachedKey) && entity.isValid()) {
                        found.add((ArmorStand) entity);
                    }
                }

                if (!found.isEmpty()) {
                    display = found.get(0);
                    for (int i = 1; i < found.size(); i++) {
                        found.get(i).remove();
                    }
                    synchronized (holograms) {
                        previews.put(key, display.getUniqueId());
                    }
                }
            }
        }

        if (display != null) {
            return true;
        }

        removeTrackedPreview(key);
        return false;
    }

    private void startAnimationTask() {
        if (animationTask != null) {
            animationTask.cancel();
        }
        animationPulse = 0;
        gateCheckedAtMillis = 0L;
        long updateInterval = getPreviewUpdateIntervalTicks();
        animationTask = plugin.getSpigotScheduler().runGlobalTimer(() -> {
            if (!previewGateOpen()) {
                if (animationTask != null) {
                    animationTask.cancel();
                    animationTask = null;
                }
                return;
            }
            animationPulse += updateInterval;
            animatePreviews();
        }, updateInterval, updateInterval);
    }

    private void animatePreviews() {
        if (previews.isEmpty()) {
            return;
        }

        double rotationSpeed = plugin.getConfigManager().getCrates().getDouble("SETTINGS.PREVIEW.ROTATION-SPEED", 4.0D);
        double hoverAmplitude = plugin.getConfigManager().getCrates().getDouble("SETTINGS.PREVIEW.HOVER-AMPLITUDE", 0.08D);
        double hoverSpeed = plugin.getConfigManager().getCrates().getDouble("SETTINGS.PREVIEW.HOVER-SPEED", 0.08D);
        int cycleInterval = getPreviewCycleIntervalTicks();
        int updateInterval = getPreviewUpdateIntervalTicks();

        Map<CrateManager.CrateBlockKey, UUID> copy;
        synchronized (holograms) {
            if (previews.isEmpty()) {
                return;
            }
            copy = new HashMap<>(previews);
        }

        for (Map.Entry<CrateManager.CrateBlockKey, UUID> entry : copy.entrySet()) {
            CrateManager.CrateBlockKey key = entry.getKey();
            UUID entityId = entry.getValue();

            World world = Bukkit.getWorld(key.world());
            if (world == null || world.getPlayers().isEmpty()) {
                continue;
            }

            Entity entity = Bukkit.getEntity(entityId);
            if (!(entity instanceof ArmorStand) || !entity.isValid()) {
                continue;
            }
            ArmorStand display = (ArmorStand) entity;

            plugin.getSpigotScheduler().runEntity(display, () -> {
                if (!display.isValid()) {
                    return;
                }
                float yaw = (float) ((animationPulse * rotationSpeed) % 360.0D);
                double hover = Math.sin(animationPulse * hoverSpeed) * hoverAmplitude;

                Location previewBase = new Location(display.getWorld(), key.x() + 0.5, key.y() + getPreviewOffsetY(), key.z() + 0.5);
                previewBase.add(0.0D, hover, 0.0D);
                previewBase.setYaw(yaw);
                display.teleport(previewBase);
                display.setHeadPose(new EulerAngle(0.0D, Math.toRadians(yaw), 0.0D));

                CrateManager.CrateDefinition crate = plugin.getCrateManager().getCrate(plugin.getCrateManager().getBoundBlockIds().get(key));
                if (crate != null && !crate.rewards().isEmpty()) {
                    synchronized (holograms) {
                        int cycleTicks = previewCycleTicks.getOrDefault(key, 0) + updateInterval;
                        if (cycleTicks >= cycleInterval) {
                            cycleTicks = 0;
                            int nextIndex = (previewIndices.getOrDefault(key, 0) + 1) % crate.rewards().size();
                            previewIndices.put(key, nextIndex);

                            CrateReward nextReward = crate.rewards().get(nextIndex);
                            ItemStack nextItem = plugin.getCrateManager().createRewardDisplayItem(null, crate, nextReward);
                            if (display.getEquipment() != null) {
                                display.getEquipment().setHelmet(nextItem);
                            }
                        }
                        previewCycleTicks.put(key, cycleTicks);
                    }
                }
            });
        }
    }
}
