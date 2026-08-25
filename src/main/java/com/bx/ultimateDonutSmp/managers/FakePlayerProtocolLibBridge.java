package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
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
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.ARM_ANIMATION
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
        try {
            if (packet.getIntegers().size() > 0) {
                return packet.getIntegers().read(0);
            }
        } catch (RuntimeException ignored) {
        }

        try {
            Object raw = packet.getModifier().size() > 0 ? packet.getModifier().read(0) : null;
            return raw instanceof Number number ? number.intValue() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isAttackAction(PacketContainer packet) {
        try {
            if (packet.getEnumEntityUseActions().size() > 0) {
                WrappedEnumEntityUseAction action = packet.getEnumEntityUseActions().read(0);
                return action != null && action.getAction() == EnumWrappers.EntityUseAction.ATTACK;
            }
        } catch (RuntimeException | LinkageError ignored) {
        }

        try {
            if (packet.getEntityUseActions().size() > 0) {
                return packet.getEntityUseActions().read(0) == EnumWrappers.EntityUseAction.ATTACK;
            }
        } catch (RuntimeException | LinkageError ignored) {
            return true;
        }
        return true;
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

            ProfileData nativeProfile = createNativeProfile(fakeUuid, profileName, texture);
            if (nativeProfile != null && nativeProfile.hasTexture()) {
                return nativeProfile;
            }

            ProfileData sourceProfile = createSourceProfileFallback(source, texture);
            if (sourceProfile != null && sourceProfile.hasTexture()) {
                return sourceProfile;
            }

            plugin.getLogger().warning("[FakePlayer] Resolved skin texture for " + source.getName()
                    + " but could not attach it to the native GameProfile handle"
                    + " (fakeUuid=" + fakeUuid
                    + ", profileName=" + profileName
                    + ", valueLength=" + texture.value().length()
                    + ", signed=" + (texture.signature() != null && !texture.signature().isBlank()) + ").");
        }

        return new ProfileData(new WrappedGameProfile(fakeUuid, profileName), false);
    }

    @Override
    public boolean hasSkinTexture(Object profile) {
        return profile instanceof ProfileData profileData && profileData.hasTexture();
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
            plugin.getLogger().log(Level.FINE, "Unable to create ProtocolLib fakeplayer profile with skin texture.",
                    error);
        }
        return null;
    }

    private ProfileData createSourceProfileFallback(Player source, TablistManager.SkinTexture texture) {
        if (source == null || texture == null || !texture.isValid() || !supportsExplicitPlayerInfoProfileId()) {
            return null;
        }

        try {
            WrappedGameProfile sourceProfile = WrappedGameProfile.fromPlayer(source);
            if (sourceProfile == null || sourceProfile.getHandle() == null) {
                return null;
            }

            setProtocolLibPropertyCache(
                    sourceProfile,
                    new WrappedSignedProperty("textures", texture.value(), texture.signature())
            );
            plugin.getLogger().info("[FakePlayer] Using live player GameProfile fallback for " + source.getName()
                    + " because a detached profile could not be mutated.");
            return new ProfileData(sourceProfile, true);
        } catch (RuntimeException | LinkageError error) {
            plugin.getLogger().log(Level.FINE,
                    "Unable to use live player GameProfile fallback for fakeplayer skin.", error);
            return null;
        }
    }

    private boolean supportsExplicitPlayerInfoProfileId() {
        try {
            PacketContainer packet = protocolManager.createPacket(playerInfoPacketType());
            PlayerInfoData infoData = new PlayerInfoData(
                    UUID.randomUUID(),
                    0,
                    true,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    new WrappedGameProfile(UUID.randomUUID(), "FakePlayer"),
                    null
            );
            writePlayerInfoData(packet, List.of(infoData));
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private boolean applyNativeTextureToWrappedProfile(
            WrappedGameProfile targetProfile,
            TablistManager.SkinTexture texture
    ) {
        if (targetProfile == null || texture == null || !texture.isValid()) {
            return false;
        }

        Object profileHandle = targetProfile.getHandle();
        if (profileHandle == null) {
            return false;
        }

        WrappedSignedProperty wrappedProperty =
                new WrappedSignedProperty("textures", texture.value(), texture.signature());
        setProtocolLibPropertyCache(targetProfile, wrappedProperty);

        try {
            Object nativeProperty = createNativeProperty(profileHandle.getClass().getClassLoader(), texture);
            if (nativeProperty != null && addNativeTexture(profileHandle, nativeProperty)
                    && hasNativeProfileTexture(profileHandle)) {
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
        }

        try {
            Object propertyHandle = wrappedProperty.getHandle();
            if (propertyHandle != null && addNativeTexture(profileHandle, propertyHandle)
                    && hasNativeProfileTexture(profileHandle)) {
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
        }

        return false;
    }

    private boolean copyTextureToWrappedProfile(Player source, WrappedGameProfile targetProfile, TablistManager.SkinTexture texture) {
        try {
            WrappedSignedProperty property = new WrappedSignedProperty("textures", texture.value(), texture.signature());
            boolean applied = applyTextureToGameProfile(targetProfile, property);
            if (!applied || !hasNativeProfileTexture(targetProfile.getHandle())) {
                plugin.getLogger().log(Level.FINE,
                        "Resolved SkinsRestorer texture for " + source.getName()
                                + " but could only attach it through ProtocolLib's profile cache.");
                return false;
            }
            return true;
        } catch (RuntimeException | LinkageError error) {
            plugin.getLogger().log(Level.FINE,
                    "Unable to copy fakeplayer skin into ProtocolLib profile for " + source.getName() + ".", error);
            return false;
        }
    }

    private boolean putTextureInWrappedProfileProperties(
            WrappedGameProfile targetProfile,
            TablistManager.SkinTexture texture
    ) {
        if (targetProfile == null || texture == null || !texture.isValid()) {
            return false;
        }

        try {
            WrappedSignedProperty property = new WrappedSignedProperty("textures", texture.value(), texture.signature());
            com.google.common.collect.Multimap<String, WrappedSignedProperty> properties = targetProfile.getProperties();
            if (properties == null) {
                return false;
            }
            properties.removeAll("textures");
            properties.put("textures", property);
            setProtocolLibPropertyCache(targetProfile, property);
            return hasWrappedProfileTexture(targetProfile);
        } catch (RuntimeException | LinkageError error) {
            plugin.getLogger().log(Level.FINE, "Unable to set fakeplayer skin through ProtocolLib profile properties.",
                    error);
            return false;
        }
    }

    private boolean hasWrappedProfileTexture(WrappedGameProfile profile) {
        if (profile == null) {
            return false;
        }

        try {
            com.google.common.collect.Multimap<String, WrappedSignedProperty> properties = profile.getProperties();
            if (properties == null) {
                return false;
            }
            for (WrappedSignedProperty property : properties.get("textures")) {
                if (property != null && property.getValue() != null && !property.getValue().isBlank()) {
                    return true;
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return false;
    }

    private ProfileData createNativeProfile(UUID fakeUuid, String profileName, TablistManager.SkinTexture texture) {
        try {
            Object nativeProfile = instantiateNativeGameProfile(fakeUuid, profileName);
            if (nativeProfile == null) {
                return null;
            }

            Object nativeProperty = createNativeProperty(nativeProfile.getClass().getClassLoader(), texture);
            if (nativeProperty == null) {
                return null;
            }

            boolean added = addNativeTexture(nativeProfile, nativeProperty);
            if (!added || !hasNativeProfileTexture(nativeProfile)) {
                return null;
            }

            WrappedGameProfile wrappedProfile = WrappedGameProfile.fromHandle(nativeProfile);
            setProtocolLibPropertyCache(
                    wrappedProfile,
                    new WrappedSignedProperty("textures", texture.value(), texture.signature())
            );
            return new ProfileData(wrappedProfile, true);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            plugin.getLogger().log(Level.FINE, "Unable to create native fakeplayer profile with skin texture.", error);
            return null;
        }
    }

    private Object instantiateNativeGameProfile(UUID uuid, String name) throws ReflectiveOperationException {
        Class<?> profileClass = findFirstClass(
                "com.mojang.authlib.GameProfile",
                "net.minecraft.util.com.mojang.authlib.GameProfile"
        );
        if (profileClass == null) {
            return null;
        }

        for (Constructor<?> constructor : profileClass.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            constructor.setAccessible(true);
            if (parameters.length == 2 && parameters[0] == UUID.class && parameters[1] == String.class) {
                return constructor.newInstance(uuid, name);
            }
            if (parameters.length == 2 && parameters[0] == String.class && parameters[1] == String.class) {
                return constructor.newInstance(uuid.toString(), name);
            }
        }
        return null;
    }

    private Object createNativeProperty(ClassLoader preferredLoader, TablistManager.SkinTexture texture)
            throws ReflectiveOperationException {
        Class<?> propertyClass = findFirstClass(preferredLoader,
                "com.mojang.authlib.properties.Property",
                "net.minecraft.util.com.mojang.authlib.properties.Property"
        );
        if (propertyClass == null) {
            return null;
        }

        for (Constructor<?> constructor : propertyClass.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            constructor.setAccessible(true);
            if (parameters.length == 3
                    && parameters[0] == String.class
                    && parameters[1] == String.class
                    && parameters[2] == String.class) {
                return constructor.newInstance("textures", texture.value(), texture.signature());
            }
            if (parameters.length == 2
                    && parameters[0] == String.class
                    && parameters[1] == String.class) {
                return constructor.newInstance("textures", texture.value());
            }
        }
        return null;
    }

    private boolean addNativeTexture(Object nativeProfile, Object nativeProperty) throws ReflectiveOperationException {
        for (String methodName : List.of("getProperties", "properties")) {
            for (Method method : findMethods(nativeProfile.getClass(), methodName, 0)) {
                method.setAccessible(true);
                Object propertyContainer = method.invoke(nativeProfile);
                if (mutateNativePropertyContainer(propertyContainer, nativeProperty)) {
                    return true;
                }
            }
        }

        for (Class<?> current = nativeProfile.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!isLikelyPropertyContainerField(field)) {
                    continue;
                }
                field.setAccessible(true);
                Object propertyContainer = field.get(nativeProfile);
                if (mutateNativePropertyContainer(propertyContainer, nativeProperty)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean mutateNativePropertyContainer(Object propertyContainer, Object nativeProperty) {
        if (propertyContainer == null || nativeProperty == null) {
            return false;
        }

        if (putNativeIntoGuavaMultimap(propertyContainer, nativeProperty)) {
            return true;
        }

        invokeCompatible(propertyContainer, "removeAll", "textures");
        invokeCompatible(propertyContainer, "remove", "textures");

        if (invokeCompatible(propertyContainer, "put", "textures", nativeProperty)) {
            return true;
        }
        return invokeCompatible(propertyContainer, "add", nativeProperty);
    }

    private boolean hasNativeProfileTexture(Object nativeProfile) {
        if (nativeProfile == null) {
            return false;
        }

        for (String methodName : List.of("getProperties", "properties")) {
            try {
                for (Method method : findMethods(nativeProfile.getClass(), methodName, 0)) {
                    method.setAccessible(true);
                    if (hasNativeTexture(method.invoke(nativeProfile))) {
                        return true;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        for (Class<?> current = nativeProfile.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!isLikelyPropertyContainerField(field)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    if (hasNativeTexture(field.get(nativeProfile))) {
                        return true;
                    }
                } catch (IllegalAccessException | RuntimeException ignored) {
                }
            }
        }
        return false;
    }

    @SuppressWarnings("rawtypes")
    private boolean putNativeIntoGuavaMultimap(Object propertyContainer, Object nativeProperty) {
        if (!(propertyContainer instanceof com.google.common.collect.Multimap multimap) || nativeProperty == null) {
            return false;
        }

        try {
            multimap.removeAll("textures");
            return multimap.put("textures", nativeProperty);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private Class<?> findFirstClass(String... classNames) {
        return findFirstClass(null, classNames);
    }

    private Class<?> findFirstClass(ClassLoader preferredLoader, String... classNames) {
        java.util.ArrayList<ClassLoader> loaders = new java.util.ArrayList<>();
        if (preferredLoader != null) {
            loaders.add(preferredLoader);
        }
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null && !loaders.contains(contextLoader)) {
            loaders.add(contextLoader);
        }
        if (!loaders.contains(getClass().getClassLoader())) {
            loaders.add(getClass().getClassLoader());
        }
        if (!loaders.contains(Player.class.getClassLoader())) {
            loaders.add(Player.class.getClassLoader());
        }

        for (ClassLoader loader : loaders) {
            for (String className : classNames) {
                try {
                    return Class.forName(className, false, loader);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }

        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private boolean applyTextureToGameProfile(WrappedGameProfile targetProfile, WrappedSignedProperty property) {
        if (targetProfile == null || property == null) {
            return false;
        }

        boolean appliedToHandle = applyTextureToPropertyContainer(targetProfile.getHandle(), property.getHandle(), property);
        setProtocolLibPropertyCache(targetProfile, property);
        return appliedToHandle;
    }

    private boolean applyTextureToPropertyContainer(Object source, Object propertyHandle, WrappedSignedProperty property) {
        if (source == null) {
            return false;
        }

        for (String methodName : List.of("getProperties", "properties")) {
            try {
                for (Method method : findMethods(source.getClass(), methodName, 0)) {
                    method.setAccessible(true);
                    Object propertyContainer = method.invoke(source);
                    if (mutatePropertyContainer(propertyContainer, propertyHandle, property)) {
                        return true;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        for (Class<?> current = source.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(source);
                    if (mutatePropertyContainer(value, propertyHandle, property)) {
                        return true;
                    }
                    if (value == null && isLikelyPropertyContainerField(field)) {
                        Object propertyContainer = createPropertyContainer(field.getType(), property);
                        if (propertyContainer != null) {
                            field.set(source, propertyContainer);
                            return true;
                        }
                    }
                } catch (IllegalAccessException | RuntimeException ignored) {
                }
            }
        }
        return false;
    }

    private boolean isLikelyPropertyContainerField(Field field) {
        String fieldName = field.getName().toLowerCase(java.util.Locale.ROOT);
        String typeName = field.getType().getName().toLowerCase(java.util.Locale.ROOT);
        return fieldName.contains("properties")
                || fieldName.contains("property")
                || typeName.contains("propertymap")
                || typeName.contains("multimap");
    }

    private Object createPropertyContainer(Class<?> type, WrappedSignedProperty property) {
        if (type == null) {
            return null;
        }

        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() != 0) {
                continue;
            }
            try {
                constructor.setAccessible(true);
                Object container = constructor.newInstance();
                return mutatePropertyContainer(container, property.getHandle(), property) ? container : null;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        if (com.google.common.collect.Multimap.class.isAssignableFrom(type)) {
            com.google.common.collect.Multimap<String, Object> map = com.google.common.collect.HashMultimap.create();
            map.put("textures", property.getHandle());
            return map;
        }
        return null;
    }

    private List<Method> findMethods(Class<?> type, String methodName, int parameterCount) {
        java.util.ArrayList<Method> methods = new java.util.ArrayList<>();
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                methods.add(method);
            }
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount
                        && !methods.contains(method)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private boolean mutatePropertyContainer(Object propertyContainer, Object propertyHandle, WrappedSignedProperty property) {
        if (propertyContainer == null) {
            return false;
        }

        if (putIntoGuavaMultimap(propertyContainer, propertyHandle, property)) {
            return true;
        }

        invokeCompatible(propertyContainer, "removeAll", "textures");
        invokeCompatible(propertyContainer, "remove", "textures");

        boolean added = invokeCompatible(propertyContainer, "put", "textures", propertyHandle);
        if (!added) {
            added = invokeCompatible(propertyContainer, "put", "textures", property);
        }
        if (!added) {
            added = invokeCompatible(propertyContainer, "add", propertyHandle);
        }
        if (!added) {
            added = invokeCompatible(propertyContainer, "add", property);
        }
        if (!added) {
            added = invokePropertyPut(propertyContainer, property);
        }
        if (!added) {
            added = invokePropertyAdd(propertyContainer, property);
        }
        return added;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean putIntoGuavaMultimap(
            Object propertyContainer,
            Object propertyHandle,
            WrappedSignedProperty property
    ) {
        if (!(propertyContainer instanceof com.google.common.collect.Multimap multimap)) {
            return false;
        }

        Object value = propertyHandle != null ? propertyHandle : property;
        if (value == null) {
            return false;
        }

        try {
            multimap.removeAll("textures");
            return multimap.put("textures", value);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean invokePropertyPut(Object propertyContainer, WrappedSignedProperty property) {
        for (Method method : findMethods(propertyContainer.getClass(), "put", 2)) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!box(parameterTypes[0]).isInstance("textures")) {
                continue;
            }
            Object adaptedProperty = createCompatibleProperty(parameterTypes[1], property);
            if (adaptedProperty == null) {
                continue;
            }
            try {
                method.setAccessible(true);
                method.invoke(propertyContainer, "textures", adaptedProperty);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return false;
    }

    private boolean invokePropertyAdd(Object propertyContainer, WrappedSignedProperty property) {
        for (Method method : findMethods(propertyContainer.getClass(), "add", 1)) {
            Object adaptedProperty = createCompatibleProperty(method.getParameterTypes()[0], property);
            if (adaptedProperty == null) {
                continue;
            }
            try {
                method.setAccessible(true);
                method.invoke(propertyContainer, adaptedProperty);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return false;
    }

    private Object createCompatibleProperty(Class<?> parameterType, WrappedSignedProperty property) {
        Object handle = property.getHandle();
        if (handle != null && box(parameterType).isInstance(handle)) {
            return handle;
        }
        if (box(parameterType).isInstance(property)) {
            return property;
        }

        for (Constructor<?> constructor : parameterType.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            try {
                constructor.setAccessible(true);
                if (parameters.length == 3
                        && parameters[0] == String.class
                        && parameters[1] == String.class
                        && parameters[2] == String.class) {
                    return constructor.newInstance("textures", property.getValue(), property.getSignature());
                }
                if (parameters.length == 2
                        && parameters[0] == String.class
                        && parameters[1] == String.class) {
                    return constructor.newInstance("textures", property.getValue());
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private boolean setProtocolLibPropertyCache(WrappedGameProfile targetProfile, WrappedSignedProperty property) {
        try {
            com.google.common.collect.Multimap<String, WrappedSignedProperty> propertyMap =
                    com.google.common.collect.HashMultimap.create();
            propertyMap.put("textures", property);
            Field field = WrappedGameProfile.class.getDeclaredField("propertyMap");
            field.setAccessible(true);
            field.set(targetProfile, propertyMap);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private boolean invokeCompatible(Object target, String methodName, Object... args) {
        if (target == null) {
            return false;
        }

        for (Method method : target.getClass().getMethods()) {
            if (tryInvokeCompatible(target, method, methodName, args)) {
                return true;
            }
        }
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (tryInvokeCompatible(target, method, methodName, args)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryInvokeCompatible(Object target, Method method, String methodName, Object... args) {
        if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
            return false;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int index = 0; index < parameterTypes.length; index++) {
            Object arg = args[index];
            if (arg != null && !box(parameterTypes[index]).isInstance(arg)) {
                return false;
            }
        }

        try {
            method.setAccessible(true);
            method.invoke(target, args);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return Void.class;
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
        PacketContainer packet = protocolManager.createPacket(playerInfoPacketType());
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
        writePlayerInfoData(packet, List.of(infoData));
        return packet;
    }

    private WrappedGameProfile unwrapProfile(Object profile) {
        if (profile instanceof ProfileData profileData) {
            return profileData.profile();
        }
        return (WrappedGameProfile) profile;
    }

    private PacketContainer createPlayerInfoRemove(FakePlayerSession fakePlayer) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        if (packet.getUUIDLists().size() > 0) {
            packet.getUUIDLists().write(0, List.of(fakePlayer.fakeUuid()));
        } else if (packet.getUUIDs().size() > 0) {
            packet.getUUIDs().write(0, fakePlayer.fakeUuid());
        }
        return packet;
    }

    private PacketContainer createSpawnEntity(FakePlayerSession fakePlayer) {
        PacketContainer namedSpawn = createNamedEntitySpawn(fakePlayer);
        if (namedSpawn != null) {
            return namedSpawn;
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
        writeByte(packet, 2, angle(location.getYaw()));
        return packet;
    }

    private PacketContainer createNamedEntitySpawn(FakePlayerSession fakePlayer) {
        try {
            if (!PacketType.Play.Server.NAMED_ENTITY_SPAWN.isSupported()) {
                return null;
            }
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
        } else if (!writeModernTeleport(packet, location, onGround)) {
            if (packet.getVectors().size() > 0) {
                packet.getVectors().write(0, location.toVector());
            } else {
                throw new IllegalStateException("ENTITY_TELEPORT packet has no supported position fields.");
            }
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

    private boolean writeModernTeleport(PacketContainer packet, Location location, boolean onGround) {
        try {
            Object handle = packet.getHandle();
            if (handle == null) {
                return false;
            }

            ClassLoader loader = handle.getClass().getClassLoader();
            Class<?> vec3Class = findFirstClass(loader, "net.minecraft.world.phys.Vec3");
            Class<?> positionMoveRotationClass = findFirstClass(loader,
                    "net.minecraft.world.entity.PositionMoveRotation",
            );