package com.bx.ultimateDonutSmp.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

final class FakePlayerProtocolLibBridge implements FakePlayerPacketBridge {

    private static final double VANILLA_KNOCKBACK_HORIZONTAL = 0.4D;
    private static final double VANILLA_KNOCKBACK_VERTICAL = 0.4D;
    private static final long DEFAULT_KNOCKBACK_RESET_TICKS = 20L;
    private static final long DEFAULT_HARD_POSITION_LOCK_INTERVAL_TICKS = 1L;

    private final UltimateDonutSmp plugin;
    private final FakePlayerManager fakePlayerManager;
    private final ProtocolManager protocolManager;
    private PacketListener attackListener;
    private boolean metadataWarned;
    private boolean teleportWarned;
    private boolean velocityWarned;
    private boolean noGravityWarned;

    FakePlayerProtocolLibBridge(UltimateDonutSmp plugin, FakePlayerManager fakePlayerManager) {
        this.plugin = plugin;
        this.fakePlayerManager = fakePlayerManager;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerAttackListener();
    }

    private void registerAttackListener() {
        attackListener = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Client.USE_ENTITY
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }

                if (event.getPacketType() == PacketType.Play.Client.ARM_ANIMATION) {
                    fakePlayerManager.handleSwingPacket(player);
                    return;
                }

                PacketContainer packet = event.getPacket();
                Integer entityId = readTargetEntityId(packet);
                if (entityId == null || !isAttackAction(packet)) {
                    return;
                }

                if (fakePlayerManager.handleAttackPacket(player, entityId)) {
                    event.setCancelled(true);
                }
            }
        };
        protocolManager.addPacketListener(attackListener);
    }

    private Integer readTargetEntityId(PacketContainer packet) {
        if (packet.getIntegers().size() > 0) {
            return packet.getIntegers().read(0);
        }
        return null;
    }

    private boolean isAttackAction(PacketContainer packet) {
        try {
            if (packet.getEnumEntityUseActions().size() > 0) {
                WrappedEnumEntityUseAction action = packet.getEnumEntityUseActions().read(0);
                return action != null && action.getAction() == EnumWrappers.EntityUseAction.ATTACK;
            }
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    @Override
    public Object createProfile(Player source, UUID fakeUuid, String profileName) {
        if (source == null) {
            return new ProfileData(new WrappedGameProfile(fakeUuid, profileName), false);
        }
        return createProfile(source, fakeUuid, profileName, resolveSkinTexture(source));
    }

    @Override
    public Object createProfile(Player source, UUID fakeUuid, String profileName, TablistManager.SkinTexture texture) {
        if (texture != null && texture.isValid()) {
            ProfileData wrappedNativeProfile = createWrappedNativeProfile(fakeUuid, profileName, texture);
            if (wrappedNativeProfile != null && wrappedNativeProfile.hasTexture()) {
                return wrappedNativeProfile;
            }
        }

        return new ProfileData(new WrappedGameProfile(fakeUuid, profileName), false);
    }

    @Override
    public boolean hasSkinTexture(Object profile) {
        if (profile instanceof ProfileData) {
            ProfileData profileData = (ProfileData) profile;
            return profileData.hasTexture();
        }
        return false;
    }

    private TablistManager.SkinTexture resolveSkinTexture(Player source) {
        TablistManager tablistManager = plugin.getTablistManager();
        if (tablistManager == null) {
            return null;
        }
        try {
            return tablistManager.resolveCurrentSkinTexture(source);
        } catch (RuntimeException | LinkageError error) {
            plugin.getLogger().log(Level.FINE,
                    "Unable to resolve fakeplayer skin from SkinsRestorer for " + source.getName() + ".", error);
            return null;
        }
    }

    private ProfileData createWrappedNativeProfile(UUID fakeUuid, String profileName, TablistManager.SkinTexture texture) {
        try {
            WrappedGameProfile profile = new WrappedGameProfile(fakeUuid, profileName);
            if (applyNativeTextureToWrappedProfile(profile, texture)) {
                return new ProfileData(profile, true);
            }
        } catch (RuntimeException | LinkageError error) {
            plugin.getLogger().log(Level.FINE, "Unable to create ProtocolLib fakeplayer profile with skin texture.", error);
        }
        return null;
    }

    @Override
    public void spawn(Player viewer, FakePlayerSession fakePlayer) {
        send(viewer, createPlayerInfoAdd(fakePlayer));
        long delayTicks = Math.max(0L, plugin.getConfigManager().getStaffMode()
                .getLong("FAKE-PLAYER.SPAWN-DELAY-TICKS", 20L));
        if (delayTicks <= 0L) {
            sendSpawnPackets(viewer, fakePlayer);
            return;
        }

        plugin.getSpigotScheduler().runEntityLater(viewer, () -> {
            if (viewer != null
                    && viewer.isOnline()
                    && fakePlayer.viewers().contains(viewer.getUniqueId())
                    && System.currentTimeMillis() < fakePlayer.expiresAtMillis()) {
                sendSpawnPackets(viewer, fakePlayer);
            }
        }, delayTicks);
    }

    private void sendSpawnPackets(Player viewer, FakePlayerSession fakePlayer) {
        send(viewer, createSpawnEntity(fakePlayer));
        sendNoGravityMetadata(viewer, fakePlayer);
        sendMetadata(viewer, fakePlayer);
        scheduleMetadataRefresh(viewer, fakePlayer);
        sendOptionalSpawnPackets(viewer, fakePlayer);
        if (isAirPositionLockEnabled()) {
            refreshPosition(viewer, fakePlayer);
        }
    }

    @Override
    public void removeFromTablist(Player viewer, FakePlayerSession fakePlayer) {
        send(viewer, createPlayerInfoRemove(fakePlayer));
    }

    @Override
    public void destroy(Player viewer, FakePlayerSession fakePlayer) {
        send(viewer, createEntityDestroy(fakePlayer));
        send(viewer, createPlayerInfoRemove(fakePlayer));
    }

    @Override
    public void refreshPosition(Player viewer, FakePlayerSession fakePlayer) {
        try {
            sendNoGravityMetadata(viewer, fakePlayer);
            send(viewer, createEntityTeleport(fakePlayer));
            if (isPhysicsSimulationEnabled()) {
                send(viewer, createEntityVelocity(fakePlayer.entityId(), fakePlayer.visualVelocity()));
            } else {
                send(viewer, createEntityVelocity(fakePlayer.entityId(), new Vector(0D, 0D, 0D)));
                sendHardPositionLock(viewer, fakePlayer);
            }
        } catch (RuntimeException error) {
            if (!teleportWarned) {
                teleportWarned = true;
                plugin.getLogger().log(Level.WARNING,
                        "Unable to send fakeplayer position lock packets on this ProtocolLib/server build.", error);
            }
        }
    }

    @Override
    public void playHitReaction(Player attacker, FakePlayerSession fakePlayer) {
        boolean damage = plugin.getConfigManager().getStaffMode()
                .getBoolean("FAKE-PLAYER.HIT-RESPONSE.DAMAGE", true);
        boolean knockback = plugin.getConfigManager().getStaffMode()
                .getBoolean("FAKE-PLAYER.HIT-RESPONSE.KNOCKBACK", true);
        if (!damage && !knockback) {
            return;
        }

        boolean physicsSimulation = isPhysicsSimulationEnabled();
        Vector velocity = knockback ? hitKnockback(attacker, fakePlayer) : null;
        Location knockbackTarget = !physicsSimulation && velocity != null ? knockbackLocation(fakePlayer, velocity) : null;
        long visualMotionSequence = 0L;
        long resetTicks = Math.max(0L, plugin.getConfigManager().getStaffMode()
                .getLong("FAKE-PLAYER.HIT-RESPONSE.RESET-POSITION-TICKS", DEFAULT_KNOCKBACK_RESET_TICKS));
        boolean resetToSpawn = plugin.getConfigManager().getStaffMode()
                .getBoolean("FAKE-PLAYER.HIT-RESPONSE.RESET-TO-SPAWN", false);
        if (velocity != null) {
            if (physicsSimulation) {
                applyVanillaLikeKnockback(fakePlayer, velocity);
            } else {
                fakePlayer.setVisualLocation(knockbackTarget);
                fakePlayer.pausePositionLock(Math.max(1L, resetTicks));
            }
            visualMotionSequence = fakePlayer.nextVisualMotionSequence();
        }

        for (UUID viewerId : new HashSet<>(fakePlayer.viewers())) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }

            try {
                sendNoGravityMetadata(viewer, fakePlayer);
                sendMetadata(viewer, fakePlayer);
                if (damage) {
                    send(viewer, createEntityStatus(fakePlayer.entityId(), (byte) 2));
                    send(viewer, createHurtAnimation(fakePlayer, attacker));
                    playHurtSound(viewer, fakePlayer);
                }
                if (velocity != null) {
                    send(viewer, createEntityVelocity(fakePlayer.entityId(), fakePlayer.visualVelocity()));
                }
            } catch (RuntimeException error) {
                if (!velocityWarned) {
                    velocityWarned = true;
                    plugin.getLogger().log(Level.WARNING,
                            "Unable to send fakeplayer hit reaction packets on this ProtocolLib/server build.", error);
                }
            }
        }

        if (physicsSimulation) {
            plugin.getSpigotScheduler().runEntityLater(attacker, () -> {
                if (System.currentTimeMillis() < fakePlayer.expiresAtMillis()) {
                    fakePlayerManager.refreshVisualPosition(fakePlayer);
                }
            }, 1L);
            return;
        }

        if (resetTicks <= 0L) {
            return;
        }

        long expectedVisualMotionSequence = visualMotionSequence;
        plugin.getSpigotScheduler().runEntityLater(attacker, () -> {
            if (expectedVisualMotionSequence != 0L && !fakePlayer.isVisualMotionSequence(expectedVisualMotionSequence)) {
                return;
            }
            if (resetToSpawn) {
                fakePlayer.resetVisualLocation();
            }
            for (UUID viewerId : new HashSet<>(fakePlayer.viewers())) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null
                        && viewer.isOnline()
                        && fakePlayer.viewers().contains(viewer.getUniqueId())
                        && System.currentTimeMillis() < fakePlayer.expiresAtMillis()) {
                    refreshPosition(viewer, fakePlayer);
                }
            }
        }, resetTicks);
    }

    private PacketContainer createPlayerInfoAdd(FakePlayerSession fakePlayer) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        EnumSet<EnumWrappers.PlayerInfoAction> actions = EnumSet.of(
                EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                EnumWrappers.PlayerInfoAction.UPDATE_LISTED
        );

        if (packet.getPlayerInfoActions().size() > 0) {
            packet.getPlayerInfoActions().write(0, actions);
        } else if (packet.getPlayerInfoAction().size() > 0) {
            packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        }

        PlayerInfoData infoData = new PlayerInfoData(
                fakePlayer.fakeUuid(),
                0,
                !isHideFromTablistEnabled(),
                EnumWrappers.NativeGameMode.SURVIVAL,
                unwrapProfile(fakePlayer.profile()),
                null
        );
        writePlayerInfoData(packet, Collections.singletonList(infoData));
        return packet;
    }

    private WrappedGameProfile unwrapProfile(Object profile) {
        if (profile instanceof ProfileData) {
            ProfileData profileData = (ProfileData) profile;
            return profileData.profile();
        }
        return (WrappedGameProfile) profile;
    }

    @Override
    public void removeFromTablist(Player viewer, FakePlayerSession fakePlayer) {
        send(viewer, createPlayerInfoRemove(fakePlayer));
    }

    private PacketContainer createPlayerInfoRemove(FakePlayerSession fakePlayer) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        if (packet.getUUIDLists().size() > 0) {
            packet.getUUIDLists().write(0, Collections.singletonList(fakePlayer.fakeUuid()));
        } else if (packet.getUUIDs().size() > 0) {
            packet.getUUIDs().write(0, fakePlayer.fakeUuid());
        }
        return packet;
    }

    private PacketContainer createSpawnEntity(FakePlayerSession fakePlayer) {
        try {
            if (PacketType.Play.Server.NAMED_ENTITY_SPAWN.isSupported()) {
                return createNamedEntitySpawn(fakePlayer);
            }
        } catch (RuntimeException ignored) {
        }

        Location location = fakePlayer.visualLocation();
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, fakePlayer.entityId());
        if (packet.getIntegers().size() > 1) {
            packet.getIntegers().write(1, 0);
        }
        packet.getUUIDs().write(0, fakePlayer.fakeUuid());
        if (packet.getEntityTypeModifier().size() > 0) {
            packet.getEntityTypeModifier().write(0, EntityType.PLAYER);
        }
        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());
        writeByte(packet, 0, angle(location.getPitch()));
        writeByte(packet, 1, angle(location.getYaw()));
        return packet;
    }

    private PacketContainer createNamedEntitySpawn(FakePlayerSession fakePlayer) {
        try {
            Location location = fakePlayer.visualLocation();
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            packet.getModifier().writeDefaults();
            packet.getIntegers().write(0, fakePlayer.entityId());
            if (packet.getUUIDs().size() > 0) {
                packet.getUUIDs().write(0, fakePlayer.fakeUuid());
            }
            if (packet.getDoubles().size() >= 3) {
                packet.getDoubles().write(0, location.getX());
                packet.getDoubles().write(1, location.getY());
                packet.getDoubles().write(2, location.getZ());
            }
            writeByte(packet, 0, angle(location.getYaw()));
            writeByte(packet, 1, angle(location.getPitch()));
            return packet;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private PacketContainer createEntityHeadRotation(FakePlayerSession fakePlayer) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        packet.getIntegers().write(0, fakePlayer.entityId());
        writeByte(packet, 0, angle(fakePlayer.visualLocation().getYaw()));
        return packet;
    }

    private PacketContainer createEntityLook(FakePlayerSession fakePlayer) {
        Location location = fakePlayer.visualLocation();
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_LOOK);
        packet.getIntegers().write(0, fakePlayer.entityId());
        writeByte(packet, 0, angle(location.getYaw()));
        writeByte(packet, 1, angle(location.getPitch()));
        if (packet.getBooleans().size() > 0) {
            packet.getBooleans().write(0, true);
        }
        return packet;
    }

    private PacketContainer createEntityTeleport(FakePlayerSession fakePlayer) {
        return createEntityTeleport(fakePlayer, fakePlayer.visualLocation(), fakePlayer.visualOnGround());
    }

    private PacketContainer createEntityTeleport(FakePlayerSession fakePlayer, Location location) {
        return createEntityTeleport(fakePlayer, location, fakePlayer.visualOnGround());
    }

    private PacketContainer createEntityTeleport(FakePlayerSession fakePlayer, Location location, boolean onGround) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, fakePlayer.entityId());
        if (packet.getDoubles().size() >= 3) {
            packet.getDoubles().write(0, location.getX());
            packet.getDoubles().write(1, location.getY());
            packet.getDoubles().write(2, location.getZ());
        }
        writeByte(packet, 0, angle(location.getYaw()));
        writeByte(packet, 1, angle(location.getPitch()));
        if (packet.getFloat().size() >= 2) {
            packet.getFloat().write(0, location.getYaw());
            packet.getFloat().write(1, location.getPitch());
        }
        if (packet.getBooleans().size() > 0) {
            packet.getBooleans().write(0, onGround);
        }
        return packet;
    }

    private PacketContainer createEntityStatus(int entityId, byte status) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_STATUS);
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, entityId);
        if (packet.getBytes().size() > 0) {
            writeByte(packet, 0, status);
            return packet;
        }
        if (packet.getModifier().size() > 1) {
            packet.getModifier().write(1, status);
            return packet;
        }
        throw new IllegalStateException("ENTITY_STATUS packet has no supported status field.");
    }

    private PacketContainer createHurtAnimation(FakePlayerSession fakePlayer, Player attacker) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.HURT_ANIMATION);
        packet.getModifier().writeDefaults();
        if (packet.getIntegers().size() > 0) {
            packet.getIntegers().write(0, fakePlayer.entityId());
        }
        if (packet.getFloat().size() > 0) {
            float yaw = attacker == null ? fakePlayer.visualLocation().getYaw() : attacker.getLocation().getYaw();
            packet.getFloat().write(0, yaw);
        }
        return packet;
    }

    private PacketContainer createEntityVelocity(int entityId, Vector velocity) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_VELOCITY);
        packet.getModifier().writeDefaults();
        if (packet.getIntegers().size() >= 4) {
            packet.getIntegers().write(0, entityId);
            packet.getIntegers().write(1, velocityToProtocol(velocity.getX()));
            packet.getIntegers().write(2, velocityToProtocol(velocity.getY()));
            packet.getIntegers().write(3, velocityToProtocol(velocity.getZ()));
            return packet;
        }
        if (packet.getIntegers().size() > 0) {
            packet.getIntegers().write(0, entityId);
        }
        if (packet.getVectors().size() > 0) {
            packet.getVectors().write(0, velocity);
            return packet;
        }
        if (packet.getDoubles().size() >= 3) {
            packet.getDoubles().write(0, velocity.getX());
            packet.getDoubles().write(1, velocity.getY());
            packet.getDoubles().write(2, velocity.getZ());
            return packet;
        }
        if (packet.getShorts().size() >= 3) {
            packet.getShorts().write(0, (short) velocityToProtocol(velocity.getX()));
            packet.getShorts().write(1, (short) velocityToProtocol(velocity.getY()));
            packet.getShorts().write(2, (short) velocityToProtocol(velocity.getZ()));
            return packet;
        }
        if (packet.getBytes().size() >= 3) {
            packet.getBytes().write(0, (byte) velocityToProtocol(velocity.getX()));
            packet.getBytes().write(1, (byte) velocityToProtocol(velocity.getY()));
            packet.getBytes().write(2, (byte) velocityToProtocol(velocity.getZ()));
            return packet;
        }
        throw new IllegalStateException("ENTITY_VELOCITY packet has no supported velocity fields.");
    }

    private void applyVanillaLikeKnockback(FakePlayerSession fakePlayer, Vector knockback) {
        Vector velocity = fakePlayer.visualVelocity();
        velocity.setX(velocity.getX() * 0.5D + knockback.getX());
        velocity.setZ(velocity.getZ() * 0.5D + knockback.getZ());
        if (fakePlayer.visualOnGround()) {
            velocity.setY(Math.min(0.4D, velocity.getY() * 0.5D + VANILLA_KNOCKBACK_VERTICAL));
        }
        fakePlayer.setVisualVelocity(velocity);
        fakePlayer.setVisualOnGround(false);
    }

    private Vector hitKnockback(Player attacker, FakePlayerSession fakePlayer) {
        Location origin = attacker.getLocation();
        Location target = fakePlayer.visualLocation();
        Vector direction = target.toVector().subtract(origin.toVector());
        direction.setY(0D);
        if (direction.lengthSquared() < 0.0001D) {
            direction = origin.getDirection().multiply(-1D);
            direction.setY(0D);
        }
        if (direction.lengthSquared() > 0.0001D) {
            direction.normalize();
        }

        return direction.multiply(VANILLA_KNOCKBACK_HORIZONTAL).setY(VANILLA_KNOCKBACK_VERTICAL);
    }

    private Location knockbackLocation(FakePlayerSession fakePlayer, Vector velocity) {
        Location location = fakePlayer.visualLocation();
        location.add(velocity.getX(), Math.min(0.35D, velocity.getY()), velocity.getZ());
        return location;
    }

    private int velocityToProtocol(double value) {
        double clamped = Math.max(-3.9D, Math.min(3.9D, value));
        return (int) Math.round(clamped * 8000.0D);
    }

    private PacketContainer createNoGravityMetadata(FakePlayerSession fakePlayer) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, fakePlayer.entityId());

        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Boolean.class);
        WrappedDataValue noGravityData = new WrappedDataValue(5, serializer, true);
        if (packet.getDataValueCollectionModifier().size() > 0) {
            packet.getDataValueCollectionModifier().write(0, Collections.singletonList(noGravityData));
            return packet;
        }

        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(5, serializer),
                true
        );
        if (packet.getWatchableCollectionModifier().size() > 0) {
            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
            return packet;
        }

        throw new IllegalStateException("ENTITY_METADATA packet has no supported metadata collection fields.");
    }

    private PacketContainer createEntityMetadata(FakePlayerSession fakePlayer) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, fakePlayer.entityId());

        byte allSkinLayers = (byte) plugin.getConfigManager().getStaffMode()
                .getInt("FAKE-PLAYER.SKIN-LAYERS-BITMASK", 0x7F);

        int skinLayerIndex = configuredSkinLayersMetadataIndex();
        WrappedDataValue skinLayerData = new WrappedDataValue(
                skinLayerIndex,
                WrappedDataWatcher.Registry.get(Byte.class),
                allSkinLayers
        );
        if (packet.getDataValueCollectionModifier().size() > 0) {
            packet.getDataValueCollectionModifier().write(0, Collections.singletonList(skinLayerData));
            return packet;
        }

        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(
                        skinLayerIndex,
                        WrappedDataWatcher.Registry.get(Byte.class)
                ),
                allSkinLayers
        );
        if (packet.getWatchableCollectionModifier().size() > 0) {
            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
            return packet;
        }

        throw new IllegalStateException("ENTITY_METADATA packet has no supported metadata collection fields.");
    }

    private int configuredSkinLayersMetadataIndex() {
        return plugin.getConfigManager().getStaffMode()
                .getInt("FAKE-PLAYER.SKIN-LAYERS-METADATA-INDEX", -1);
    }

    @Override
    public void shutdown() {
        if (attackListener != null) {
            protocolManager.removePacketListener(attackListener);
            attackListener = null;
        }
    }

    private static final class SkinLayerMetadata {
        private final int index;
        private final WrappedDataWatcher.Serializer serializer;

        SkinLayerMetadata(int index, WrappedDataWatcher.Serializer serializer) {
            this.index = index;
            this.serializer = serializer;
        }

        public int index() {
            return index;
        }

        public WrappedDataWatcher.Serializer serializer() {
            return serializer;
        }
    }

    private static final class ProfileData {
        private final WrappedGameProfile profile;
        private final boolean hasTexture;

        ProfileData(WrappedGameProfile profile, boolean hasTexture) {
            this.profile = profile;
            this.hasTexture = hasTexture;
        }

        public WrappedGameProfile profile() {
            return profile;
        }

        public boolean hasTexture() {
            return hasTexture;
        }
    }
}