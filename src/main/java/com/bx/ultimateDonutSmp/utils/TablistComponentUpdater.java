package com.bx.ultimateDonutSmp.utils;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class TablistComponentUpdater {

    /*
     * Modern (1.17+) Mojang-mapped names first, then the relocated 1.16.5-and-older names.
     * NmsSupport derives the "net.minecraft.server.<version>." prefix from the running
     * CraftBukkit package, so on Spigot 1.12.2 these resolve to
     * net.minecraft.server.v1_12_R1.PacketPlayOutPlayerInfo / IChatBaseComponent. Without the
     * versioned entries every lookup below throws ClassNotFoundException on 1.12.2, the whole
     * component route disables itself and the tablist falls back to the 16-character truncated
     * Bukkit player-list-name path (which is what made player names disappear).
     */
    private static final String[] PLAYER_INFO_UPDATE_PACKET_CLASS_NAMES = {
            "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket",
            "net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo"
    };
    private static final String[] PLAYER_INFO_UPDATE_PACKET_LEGACY_NAMES = {
            "PacketPlayOutPlayerInfo"
    };
    private static final String[] PLAYER_INFO_REMOVE_PACKET_CLASS_NAMES = {
            "net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket",
            "net.minecraft.network.protocol.game.PacketPlayOutPlayerInfoRemove"
    };
    private static final String[] NATIVE_COMPONENT_CLASS_NAMES = {
            "net.minecraft.network.chat.Component",
            "net.minecraft.network.chat.IChatBaseComponent"
    };
    private static final String[] NATIVE_COMPONENT_LEGACY_NAMES = {
            "IChatBaseComponent"
    };

    /** Ordered candidate list for the modern + relocated player-info packet class. */
    private static List<String> playerInfoUpdatePacketClassNames() {
        return NmsSupport.candidates(PLAYER_INFO_UPDATE_PACKET_CLASS_NAMES,
                PLAYER_INFO_UPDATE_PACKET_LEGACY_NAMES);
    }

    /** Ordered candidate list for the modern + relocated player-info-removal packet class. */
    private static List<String> playerInfoRemovePacketClassNames() {
        return NmsSupport.candidates(PLAYER_INFO_REMOVE_PACKET_CLASS_NAMES);
    }

    private static final String[] HEADER_FOOTER_PACKET_CLASS_NAMES = {
            "net.minecraft.network.protocol.game.ClientboundPlayerListHeaderFooterPacket",
            "net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter"
    };
    private static final String[] HEADER_FOOTER_PACKET_LEGACY_NAMES = {
            "PacketPlayOutPlayerListHeaderFooter"
    };

    /**
     * Ordered candidate list for the modern + relocated player-list header/footer packet.
     * On Spigot 1.12.2 only the relocated {@code net.minecraft.server.v1_12_R1} name exists;
     * the fully qualified modern entries are never linked, just probed.
     */
    private static List<String> headerFooterPacketClassNames() {
        return NmsSupport.candidates(HEADER_FOOTER_PACKET_CLASS_NAMES,
                HEADER_FOOTER_PACKET_LEGACY_NAMES);
    }

    /** Ordered candidate list for the modern + relocated chat component class. */
    private static List<String> nativeComponentClassNames() {
        return NmsSupport.candidates(NATIVE_COMPONENT_CLASS_NAMES, NATIVE_COMPONENT_LEGACY_NAMES);
    }

    private final UltimateDonutSmp plugin;
    private boolean warned;
    private boolean disabled;
    private boolean avatarWarned;
    private boolean avatarDisabled;
    private boolean objectComponentWarned;
    private boolean headerFooterWarned;

    public TablistComponentUpdater(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    public boolean updateName(Player target, net.kyori.adventure.text.Component adventureComponent) {
        if (disabled || target == null || !target.isOnline() || adventureComponent == null) {
            return false;
        }

        if (updateNameWithPaperApi(target, adventureComponent)) {
            return true;
        }

        try {
            Object handle = invokeNoArg(target, "getHandle");
            Object component = toNativeComponent(adventureComponent);
            if (component == null) {
                return false;
            }
            setTabListDisplayName(handle, component);
            Object packet = createDisplayNamePacket(handle, component);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer != null && viewer.isOnline()) {
                    sendPacket(viewer, packet);
                }
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disableWithWarning(exception);
            return false;
        }
    }

    private boolean updateNameWithPaperApi(Player target, net.kyori.adventure.text.Component adventureComponent) {
        try {
            Method method = target.getClass().getMethod("playerListName", net.kyori.adventure.text.Component.class);
            method.setAccessible(true);
            method.invoke(target, adventureComponent);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private Object toNativeComponent(net.kyori.adventure.text.Component adventureComponent) {
        try {
            Class<?> paperAdventureClass = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            Method asVanillaMethod = paperAdventureClass.getMethod("asVanilla", net.kyori.adventure.text.Component.class);
            asVanillaMethod.setAccessible(true);
            return asVanillaMethod.invoke(null, adventureComponent);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }

        boolean hasObjectComponent = containsObjectComponent(adventureComponent);
        if (hasObjectComponent) {
            Object direct = toNativeComponentDirect(adventureComponent);
            if (direct != null) {
                return direct;
            }
        }

        try {
            String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(adventureComponent);
            Object component = parseNativeComponent(json);
            if (component != null) {
                return component;
            }
        } catch (RuntimeException ignored) {
        }

        Object direct = hasObjectComponent ? null : toNativeComponentDirect(adventureComponent);
        if (direct != null) {
            return direct;
        }

        // Neither the Paper bridge nor the JSON routes produced anything - the situation on a
        // stock 1.12.2 Spigot server, where ChatSerializer only knows the sixteen named colours
        // and the static literal factories appeared with 1.16. Flatten the tree into a legacy
        // string and let CraftBukkit's own ChatComponentText bridge wrap it.
        if (!hasObjectComponent) {
            Object legacy = toNativeComponentViaLegacyText(adventureComponent);
            if (legacy != null) {
                return legacy;
            }
        }

        if (hasObjectComponent) {
            warnObjectComponentUnsupported();
        }
        return null;
    }

    /**
     * Converts a text-only adventure component into a native chat component by way of a legacy
     * {@code §}-coded string. Hex colours are reduced to their nearest legacy match, which is
     * the best any 1.12.2-era client can do anyway; the result feeds CraftBukkit's
     * {@code CraftChatMessage.toComponent(String)} (or, failing that, the relocated
     * {@code ChatComponentText(String)} constructor), neither of which needs JSON parsing.
     */
    private Object toNativeComponentViaLegacyText(net.kyori.adventure.text.Component adventureComponent) {
        String legacy;
        try {
            legacy = componentToLegacyText(adventureComponent);
        } catch (RuntimeException ignored) {
            return null;
        }
        if (legacy == null || legacy.isEmpty()) {
            return null;
        }

        ClassLoader loader = plugin.getServer().getClass().getClassLoader();
        for (String className : craftChatMessageClassNames()) {
            Class<?> type;
            try {
                type = Class.forName(className, false, loader);
            } catch (ClassNotFoundException ignored) {
                continue;
            }
            for (Method method : type.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())
                        || !"toComponent".equals(method.getName())
                        || method.getParameterCount() != 1
                        || method.getParameterTypes()[0] != String.class
                        || method.getReturnType() == void.class
                        || method.getReturnType() == String.class) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    Object component = method.invoke(null, legacy);
                    if (component != null && !component.getClass().isArray()) {
                        return component;
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                    // try the next candidate
                }
            }
        }

        try {
            Class<?> textType = NmsSupport.findClass(loader,
                    "net.minecraft.network.chat.ChatComponentText",
                    "ChatComponentText");
            if (textType != null && !textType.isInterface() && !textType.isEnum()) {
                Object component = textType.getConstructor(String.class).newInstance(legacy);
                if (textType.isInstance(component)) {
                    return component;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // fall through
        }
        return null;
    }

    /**
     * Flattens a text-only adventure component to a legacy {@code §}-coded string: hex
     * colours collapse to their nearest sixteen-colour match and styling becomes the familiar
     * legacy codes. Visible to tests because the conversion is pure logic and the reflective
     * CraftBukkit lookup around it is not.
     */
    static String componentToLegacyText(net.kyori.adventure.text.Component component) {
        StringBuilder raw = new StringBuilder();
        appendLegacyText(component, raw);
        return LegacyScoreboardText.toLegacyColors(raw.toString());
    }

    private static void appendLegacyText(net.kyori.adventure.text.Component component, StringBuilder out) {
        TextColor color = component.color();
        if (color != null) {
            out.append("&#").append(String.format("%06X", color.value() & 0xFFFFFF));
        }
        appendLegacyDecoration(component, TextDecoration.BOLD, 'l', out);
        appendLegacyDecoration(component, TextDecoration.ITALIC, 'o', out);
        appendLegacyDecoration(component, TextDecoration.UNDERLINED, 'n', out);
        appendLegacyDecoration(component, TextDecoration.STRIKETHROUGH, 'm', out);
        appendLegacyDecoration(component, TextDecoration.OBFUSCATED, 'k', out);
        if (component instanceof TextComponent) {
            out.append(((TextComponent) component).content());
        }
        for (net.kyori.adventure.text.Component child : component.children()) {
            appendLegacyText(child, out);
        }
    }

    private static void appendLegacyDecoration(
            net.kyori.adventure.text.Component component,
            TextDecoration decoration,
            char code,
            StringBuilder out
    ) {
        TextDecoration.State state = component.decorations().get(decoration);
        if (state == TextDecoration.State.TRUE) {
            out.append('\u00A7').append(code);
        } else if (state == TextDecoration.State.FALSE) {
            out.append('\u00A7').append('r');
        }
    }

    private boolean containsObjectComponent(net.kyori.adventure.text.Component component) {
        if (isAdventureObjectComponent(component)) {
            return true;
        }

        for (net.kyori.adventure.text.Component child : component.children()) {
            if (containsObjectComponent(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdventureObjectComponent(Object component) {
        return isNamedClassInstance("net.kyori.adventure.text.ObjectComponent", component);
    }

    private boolean isPlayerHeadObjectContents(Object contents) {
        return isNamedClassInstance("net.kyori.adventure.text.object.PlayerHeadObjectContents", contents);
    }

    private boolean isNamedClassInstance(String className, Object value) {
        if (value == null) {
            return false;
        }

        try {
            Class<?> type = Class.forName(className, false, value.getClass().getClassLoader());
            return type.isInstance(value);
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class<?> type = Class.forName(className);
            return type.isInstance(value);
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private Object toNativeComponentDirect(net.kyori.adventure.text.Component adventureComponent) {
        try {
            ClassLoader loader = plugin.getServer().getClass().getClassLoader();
            Class<?> nativeComponentType = NmsSupport.findClass(loader, nativeComponentClassNames());
            if (nativeComponentType == null) {
                return null;
            }

            return createNativeComponent(adventureComponent, loader, nativeComponentType);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private void warnObjectComponentUnsupported() {
        if (objectComponentWarned) {
            return;
        }
        objectComponentWarned = true;
        plugin.getLogger().warning("[Tablist] This Spigot build did not expose native object text components "
                + "for inline player heads. <head:...> requires a Minecraft/Spigot build with "
                + "net.minecraft.network.chat.contents.objects.PlayerSprite support. Server: "
                + Bukkit.getBukkitVersion());
    }

    private Object createNativeComponent(
            net.kyori.adventure.text.Component adventureComponent,
            ClassLoader loader,
            Class<?> nativeComponentType
    ) throws ReflectiveOperationException {
        Object nativeComponent = createNativeBaseComponent(adventureComponent, loader, nativeComponentType);
        if (nativeComponent == null) {
            nativeComponent = createNativeLiteralComponent("", nativeComponentType);
        }

        applyNativeColor(nativeComponent, adventureComponent.color());
        for (net.kyori.adventure.text.Component child : adventureComponent.children()) {
            Object nativeChild = createNativeComponent(child, loader, nativeComponentType);
            appendNativeComponent(nativeComponent, nativeChild, nativeComponentType);
        }
        return nativeComponent;
    }

    private Object createNativeBaseComponent(
            net.kyori.adventure.text.Component adventureComponent,
            ClassLoader loader,
            Class<?> nativeComponentType
    ) throws ReflectiveOperationException {
        if (adventureComponent instanceof TextComponent) {
            TextComponent textComponent = (TextComponent) adventureComponent;
            return createNativeLiteralComponent(textComponent.content(), nativeComponentType);
        }

        if (isAdventureObjectComponent(adventureComponent)) {
            Object object = createNativeObjectInfo(invokeNoArg(adventureComponent, "contents"), loader);
            if (object != null) {
                return createNativeObjectComponent(object, loader, nativeComponentType);
            }
        }

        return createNativeLiteralComponent("", nativeComponentType);
    }

    private Object createNativeLiteralComponent(String text, Class<?> nativeComponentType)
            throws ReflectiveOperationException {
        List<String> preferredNames = new java.util.ArrayList<>(java.util.Arrays.asList("literal",  "b",  "m_237113_",  "method_43470"));
        for (String preferredName : preferredNames) {
            Object component = invokeStaticStringComponentFactory(nativeComponentType, preferredName, text);
            if (component != null) {
                return component;
            }
        }

        for (Method method : nativeComponentType.getMethods()) {
            Object component = invokeStringComponentFactory(nativeComponentType, method, text);
            if (component != null) {
                return component;
            }
        }
        for (Method method : nativeComponentType.getDeclaredMethods()) {
            Object component = invokeStringComponentFactory(nativeComponentType, method, text);
            if (component != null) {
                return component;
            }
        }

        throw new NoSuchMethodException("literal component factory");
    }

    private Object invokeStaticStringComponentFactory(Class<?> nativeComponentType, String methodName, String text) {
        for (Method method : nativeComponentType.getMethods()) {
            if (method.getName().equals(methodName)) {
                Object component = invokeStringComponentFactory(nativeComponentType, method, text);
                if (component != null) {
                    return component;
                }
            }
        }
        for (Method method : nativeComponentType.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                Object component = invokeStringComponentFactory(nativeComponentType, method, text);
                if (component != null) {
                    return component;
                }
            }
        }
        return null;
    }

    private Object invokeStringComponentFactory(Class<?> nativeComponentType, Method method, String text) {
        if (!Modifier.isStatic(method.getModifiers())
                || method.getParameterCount() != 1
                || method.getParameterTypes()[0] != String.class
                || !nativeComponentType.isAssignableFrom(method.getReturnType())) {
            return null;
        }

        try {
            method.setAccessible(true);
            return method.invoke(null, text == null ? "" : text);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private Object createNativeObjectInfo(Object contents, ClassLoader loader) throws ReflectiveOperationException {
        if (isPlayerHeadObjectContents(contents)) {
            return createNativePlayerHeadObjectInfo(contents, loader);
        }
        return null;
    }

    private Object createNativePlayerHeadObjectInfo(Object playerHead, ClassLoader loader)
            throws ReflectiveOperationException {
        Class<?> playerSpriteType = Class.forName(
                "net.minecraft.network.chat.contents.objects.PlayerSprite",
                false,
                loader
        );
        Class<?> resolvableProfileType = Class.forName(
                "net.minecraft.world.item.component.ResolvableProfile",
                false,
                loader
        );
        Object gameProfile = createGameProfile(playerHead);
        Object resolvableProfile = createResolvableProfile(resolvableProfileType, gameProfile);
        boolean hat = getBooleanValue(playerHead, "hat");

        for (Constructor<?> constructor : playerSpriteType.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 2
                    && parameters[0].isAssignableFrom(resolvableProfile.getClass())
                    && (parameters[1] == boolean.class || parameters[1] == Boolean.class)) {
                constructor.setAccessible(true);
                return constructor.newInstance(resolvableProfile, hat);
            }
        }

        throw new NoSuchMethodException("PlayerSprite(ResolvableProfile, boolean)");
    }

    private Object createGameProfile(Object playerHead) throws ReflectiveOperationException {
        ClassLoader loader = plugin.getServer().getClass().getClassLoader();
        Class<?> gameProfileType = getFirstAvailableClassOrNull(
                loader,
                "com.mojang.authlib.GameProfile"
        );
        if (gameProfileType == null) {
            gameProfileType = Class.forName("com.mojang.authlib.GameProfile");
        }

        UUID id = getUuidValue(playerHead, "id");
        String name = getStringValue(playerHead, "name");
        if ((name == null || name.trim().isEmpty()) && id != null) {
            name = id.toString().replace("-", "").substring(0, 16);
        }
        if (name == null || name.trim().isEmpty()) {
            name = "Player";
        }

        Constructor<?> constructor = gameProfileType.getConstructor(UUID.class, String.class);
        Object profile = constructor.newInstance(id, name);
        Object propertyMap = gameProfileType.getMethod("getProperties").invoke(profile);
        Object profileProperties = unwrapOptional(invokeNoArg(playerHead, "profileProperties"));
        if (!(profileProperties instanceof Iterable<?>)) {
            return profile;
        }

        Iterable<?> iterableProperties = (Iterable<?>) profileProperties;

        for (Object profileProperty : iterableProperties) {
            String propertyName = getStringValue(profileProperty, "name");
            String propertyValue = getStringValue(profileProperty, "value");
            if (profileProperty == null || propertyValue == null || propertyValue.trim().isEmpty()) {
                continue;
            }
            if (propertyName == null || propertyName.trim().isEmpty()) {
                propertyName = "textures";
            }
            Object property = createMojangProfileProperty(profileProperty, gameProfileType.getClassLoader());
            invokeCompatibleIfPresent(propertyMap, "put", propertyName, property);
        }
        return profile;
    }

    private Object createMojangProfileProperty(
            Object profileProperty,
            ClassLoader preferredLoader
    ) throws ReflectiveOperationException {
        String propertyName = getStringValue(profileProperty, "name");
        String propertyValue = getStringValue(profileProperty, "value");
        String propertySignature = getStringValue(profileProperty, "signature");
        if (propertyName == null || propertyName.trim().isEmpty()) {
            propertyName = "textures";
        }

        List<ClassLoader> loaders = new ArrayList<>();
        if (preferredLoader != null) {
            loaders.add(preferredLoader);
        }
        ClassLoader serverLoader = plugin.getServer().getClass().getClassLoader();
        if (serverLoader != null && !loaders.contains(serverLoader)) {
            loaders.add(serverLoader);
        }
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null && !loaders.contains(contextLoader)) {
            loaders.add(contextLoader);
        }

        ClassNotFoundException missing = null;
        for (ClassLoader loader : loaders) {
            try {
                Class<?> propertyType = Class.forName("com.mojang.authlib.properties.Property", false, loader);
                for (Constructor<?> constructor : propertyType.getDeclaredConstructors()) {
                    Class<?>[] parameters = constructor.getParameterTypes();
                    if (parameters.length == 3
                            && parameters[0] == String.class
                            && parameters[1] == String.class
                            && parameters[2] == String.class) {
                        constructor.setAccessible(true);
                        return constructor.newInstance(
                                propertyName,
                                propertyValue,
                                propertySignature
                        );
                    }
                    if (parameters.length == 2
                            && parameters[0] == String.class
                            && parameters[1] == String.class) {
                        constructor.setAccessible(true);
                        return constructor.newInstance(propertyName, propertyValue);
                    }
                }
            } catch (ClassNotFoundException exception) {
                missing = exception;
            }
        }

        throw missing == null
                ? new ClassNotFoundException("com.mojang.authlib.properties.Property")
                : missing;
    }

    private Object createResolvableProfile(Class<?> resolvableProfileType, Object gameProfile)
            throws ReflectiveOperationException {
        List<String> preferredNames = new java.util.ArrayList<>(java.util.Arrays.asList(
                "createResolved", 
                "ofStatic", 
                "a", 
                "m_416870_", 
                "method_73307"
        ));
        for (String preferredName : preferredNames) {
            Object profile = invokeStaticSingleArgFactory(resolvableProfileType, preferredName, gameProfile);
            if (profile != null) {
                return profile;
            }
        }

        for (Method method : resolvableProfileType.getMethods()) {
            Object profile = invokeSingleArgFactory(resolvableProfileType, method, gameProfile);
            if (profile != null) {
                return profile;
            }
        }
        for (Method method : resolvableProfileType.getDeclaredMethods()) {
            Object profile = invokeSingleArgFactory(resolvableProfileType, method, gameProfile);
            if (profile != null) {
                return profile;
            }
        }

        throw new NoSuchMethodException("ResolvableProfile.createResolved(GameProfile)");
    }

    private Object createNativeObjectComponent(
            Object objectInfo,
            ClassLoader loader,
            Class<?> nativeComponentType
    ) throws ReflectiveOperationException {
        Object component = invokeStaticObjectComponentFactory(nativeComponentType, objectInfo);
        if (component != null) {
            return component;
        }

        Class<?> objectContentsType = Class.forName(
                "net.minecraft.network.chat.contents.ObjectContents",
                false,
                loader
        );
        Object objectContents = null;
        for (Constructor<?> constructor : objectContentsType.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 1 && parameters[0].isAssignableFrom(objectInfo.getClass())) {
                constructor.setAccessible(true);
                objectContents = constructor.newInstance(objectInfo);
                break;
            }
        }
        if (objectContents == null) {
            throw new NoSuchMethodException("ObjectContents(ObjectInfo)");
        }

        Class<?> mutableComponentType = getFirstAvailableClassOrNull(
                loader,
                "net.minecraft.network.chat.MutableComponent",
                "net.minecraft.network.chat.IChatMutableComponent"
        );
        if (mutableComponentType == null) {
            throw new ClassNotFoundException("MutableComponent");
        }

        for (Method method : mutableComponentType.getMethods()) {
            component = invokeSingleArgFactory(mutableComponentType, method, objectContents);
            if (component != null) {
                return component;
            }
        }
        for (Method method : mutableComponentType.getDeclaredMethods()) {
            component = invokeSingleArgFactory(mutableComponentType, method, objectContents);
            if (component != null) {
                return component;
            }
        }

        throw new NoSuchMethodException("MutableComponent.create(ObjectContents)");
    }

    private Object invokeStaticObjectComponentFactory(Class<?> nativeComponentType, Object objectInfo) {
        List<String> preferredNames = new java.util.ArrayList<>(java.util.Arrays.asList("object",  "a",  "m_418787_",  "method_74062"));
        for (String preferredName : preferredNames) {
            for (Method method : nativeComponentType.getMethods()) {
                if (method.getName().equals(preferredName)) {
                    Object component = invokeSingleArgFactory(nativeComponentType, method, objectInfo);
                    if (component != null) {
                        return component;
                    }
                }
            }
            for (Method method : nativeComponentType.getDeclaredMethods()) {
                if (method.getName().equals(preferredName)) {
                    Object component = invokeSingleArgFactory(nativeComponentType, method, objectInfo);
                    if (component != null) {
                        return component;
                    }
                }
            }
        }
        return null;
    }

    private Object invokeStaticSingleArgFactory(Class<?> returnType, String methodName, Object arg) {
        for (Method method : returnType.getMethods()) {
            if (method.getName().equals(methodName)) {
                Object value = invokeSingleArgFactory(returnType, method, arg);
                if (value != null) {
                    return value;
                }
            }
        }
        for (Method method : returnType.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                Object value = invokeSingleArgFactory(returnType, method, arg);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private Object invokeSingleArgFactory(Class<?> returnType, Method method, Object arg) {
        if (!Modifier.isStatic(method.getModifiers())
                || method.getParameterCount() != 1
                || arg == null
                || !method.getParameterTypes()[0].isAssignableFrom(arg.getClass())
                || !returnType.isAssignableFrom(method.getReturnType())) {
            return null;
        }

        try {
            method.setAccessible(true);
            return method.invoke(null, arg);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private void appendNativeComponent(
            Object nativeComponent,
            Object nativeChild,
            Class<?> nativeComponentType
    ) throws ReflectiveOperationException {
        if (nativeComponent == null || nativeChild == null) {
            return;
        }

        List<String> preferredNames = new java.util.ArrayList<>(java.util.Arrays.asList("append",  "b",  "m_7220_",  "method_10852"));
        for (String preferredName : preferredNames) {
            if (invokeAppendNativeComponent(nativeComponent, nativeChild, nativeComponentType, preferredName)) {
                return;
            }
        }

        for (Method method : nativeComponent.getClass().getMethods()) {
            if (invokeAppendNativeComponent(nativeComponent, nativeChild, nativeComponentType, method)) {
                return;
            }
        }
        for (Method method : nativeComponent.getClass().getDeclaredMethods()) {
            if (invokeAppendNativeComponent(nativeComponent, nativeChild, nativeComponentType, method)) {
                return;
            }
        }

        throw new NoSuchMethodException("MutableComponent.append(Component)");
    }

    private boolean invokeAppendNativeComponent(
            Object nativeComponent,
            Object nativeChild,
            Class<?> nativeComponentType,
            String methodName
    ) {
        for (Method method : nativeComponent.getClass().getMethods()) {
            if (method.getName().equals(methodName)
                    && invokeAppendNativeComponent(nativeComponent, nativeChild, nativeComponentType, method)) {
                return true;
            }
        }
        for (Method method : nativeComponent.getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodName)
                    && invokeAppendNativeComponent(nativeComponent, nativeChild, nativeComponentType, method)) {
                return true;
            }
        }
        return false;
    }

    private boolean invokeAppendNativeComponent(
            Object nativeComponent,
            Object nativeChild,
            Class<?> nativeComponentType,
            Method method
    ) {
        if (method.getParameterCount() != 1
                || !method.getParameterTypes()[0].isAssignableFrom(nativeChild.getClass())
                || !nativeComponentType.isAssignableFrom(method.getParameterTypes()[0])) {
            return false;
        }

        try {
            method.setAccessible(true);
            method.invoke(nativeComponent, nativeChild);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private void applyNativeColor(Object nativeComponent, TextColor color) {
        if (nativeComponent == null || color == null) {
            return;
        }

        List<String> preferredNames = new java.util.ArrayList<>(java.util.Arrays.asList("withColor",  "b",  "m_306658_",  "method_54663"));
        for (String preferredName : preferredNames) {
            if (invokeNativeColor(nativeComponent, color.value(), preferredName)) {
                return;
            }
        }
    }

    private boolean invokeNativeColor(Object nativeComponent, int color, String methodName) {
        for (Method method : nativeComponent.getClass().getMethods()) {
            if (method.getName().equals(methodName) && invokeNativeColor(nativeComponent, color, method)) {
                return true;
            }
        }
        for (Method method : nativeComponent.getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodName) && invokeNativeColor(nativeComponent, color, method)) {
                return true;
            }
        }
        return false;
    }

    private boolean invokeNativeColor(Object nativeComponent, int color, Method method) {
        if (method.getParameterCount() != 1
                || !(method.getParameterTypes()[0] == int.class || method.getParameterTypes()[0] == Integer.class)
                || !method.getReturnType().isAssignableFrom(nativeComponent.getClass())) {
            return false;
        }

        try {
            method.setAccessible(true);
            method.invoke(nativeComponent, color);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private boolean invokeCompatibleIfPresent(Object target, String methodName, Object... args)
            throws ReflectiveOperationException {
        if (target == null) {
            return false;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName)
                    || method.getParameterCount() != args.length
                    || !canAccept(method.getParameterTypes(), args)) {
                continue;
            }

            method.setAccessible(true);
            method.invoke(target, args);
            return true;
        }

        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(methodName)
                        || method.getParameterCount() != args.length
                        || !canAccept(method.getParameterTypes(), args)) {
                    continue;
                }

                method.setAccessible(true);
                method.invoke(target, args);
                return true;
            }
        }

        return false;
    }

    private boolean canAccept(Class<?>[] parameterTypes, Object[] args) {
        for (int index = 0; index < parameterTypes.length; index++) {
            Object arg = args[index];
            if (arg == null) {
                continue;
            }

            Class<?> parameterType = wrapPrimitive(parameterTypes[index]);
            if (!parameterType.isAssignableFrom(arg.getClass())) {
                return false;
            }
        }
        return true;
    }

    private Class<?> wrapPrimitive(Class<?> type) {
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
        return type;
    }

    public boolean refreshAvatar(Player target) {
        if (avatarDisabled || target == null || !target.isOnline()) {
            return false;
        }

        try {
            Object handle = invokeNoArg(target, "getHandle");
            Object removePacket = createRemovePlayerPacket(target.getUniqueId(), handle);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer != null && viewer.isOnline()) {
                    sendPacket(viewer, removePacket);
                }
            }
            scheduleAvatarAdd(target.getUniqueId(), 2L);
            scheduleAvatarAdd(target.getUniqueId(), 6L);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disableAvatarWithWarning(exception);
            return false;
        }
    }

    public boolean refreshEntry(Player target, net.kyori.adventure.text.Component adventureComponent) {
        if (avatarDisabled || target == null || !target.isOnline() || adventureComponent == null) {
            return false;
        }

        try {
            Object handle = invokeNoArg(target, "getHandle");
            Object displayName = toNativeComponent(adventureComponent);
            if (displayName == null) {
                return false;
            }

            setTabListDisplayName(handle, displayName);
            Object removePacket = createRemovePlayerPacket(target.getUniqueId(), handle);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer != null && viewer.isOnline()) {
                    sendPacket(viewer, removePacket);
                }
            }
            scheduleEntryAdd(target.getUniqueId(), adventureComponent, 2L);
            scheduleEntryAdd(target.getUniqueId(), adventureComponent, 8L);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disableAvatarWithWarning(exception);
            return false;
        }
    }

    /**
     * Sends the tab menu header and footer to a single viewer using the mechanism 1.12.2
     * clients actually understand: {@code PacketPlayOutPlayerListHeaderFooter} carrying the
     * optional {@code IChatBaseComponent} header and footer fields. The components go through
     * the same rendering pipeline that colours tab display names, so configured hex colours
     * flatten to their nearest legacy match identically for a vanilla 1.12.2 client and for an
     * Eaglercraft client (EaglerXServer forwards this play packet verbatim and the client
     * renders it exactly like vanilla 1.12.2 does - one legacy representation for both).
     *
     * <p>This route exists because the Bukkit String-based API
     * ({@code setPlayerListHeader(String)}, {@code setPlayerListFooter(String)},
     * {@code setPlayerListHeaderFooter(String, String)}) was introduced only after 1.12.2 and
     * is absent on the target platform; without this fallback the configured header and footer
     * would silently never reach the tab menu.
     */
    public boolean updateHeaderFooter(Player viewer, net.kyori.adventure.text.Component header,
                                      net.kyori.adventure.text.Component footer) {
        if (viewer == null || !viewer.isOnline() || (header == null && footer == null)) {
            return false;
        }

        try {
            Object nativeHeader = header == null ? null : toNativeComponent(header);
            Object nativeFooter = footer == null ? null : toNativeComponent(footer);
            if ((header != null && nativeHeader == null) || (footer != null && nativeFooter == null)) {
                return false;
            }

            Class<?> packetClass = getFirstAvailableClass(headerFooterPacketClassNames());
            Object packet = createHeaderFooterPacket(packetClass, nativeHeader, nativeFooter);
            sendPacket(viewer, packet);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            disableHeaderFooterWithWarning(exception);
            return false;
        }
    }

    private void scheduleAvatarAdd(UUID targetId, long delayTicks) {
        plugin.getSpigotScheduler().runGlobalLater(() -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                return;
            }

            try {
                Object handle = invokeNoArg(target, "getHandle");
                Object addPacket = createAddPlayerPacket(handle);
                Object displayNamePacket = createDisplayNamePacket(handle);
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (viewer != null && viewer.isOnline()) {
                        sendPacket(viewer, addPacket);
                        sendPacket(viewer, displayNamePacket);
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                disableAvatarWithWarning(exception);
            }
        }, delayTicks);
    }

    private void scheduleEntryAdd(
            UUID targetId,
            net.kyori.adventure.text.Component adventureComponent,
            long delayTicks
    ) {
        plugin.getSpigotScheduler().runGlobalLater(() -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                return;
            }

            try {
                Object handle = invokeNoArg(target, "getHandle");
                Object displayName = toNativeComponent(adventureComponent);
                if (displayName == null) {
                    return;
                }
                setTabListDisplayName(handle, displayName);
                Object addPacket = createAddPlayerPacket(handle, displayName);
                Object displayNamePacket = createDisplayNamePacket(handle, displayName);
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (viewer != null && viewer.isOnline()) {
                        sendPacket(viewer, addPacket);
                        sendPacket(viewer, displayNamePacket);
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                disableAvatarWithWarning(exception);
            }
        }, delayTicks);
    }

    private Object parseNativeComponent(String json) {
        try {
            Object component = parseWithCraftChatMessage(json);
            if (component != null) {
                return component;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }

        try {
            return parseWithMinecraftSerializer(json);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private Object parseWithCraftChatMessage(String json) throws ReflectiveOperationException {
        ClassLoader loader = plugin.getServer().getClass().getClassLoader();
        for (String className : craftChatMessageClassNames()) {
            Class<?> type;
            try {
                type = Class.forName(className, false, loader);
            } catch (ClassNotFoundException ignored) {
                continue;
            }

            for (String name : new java.util.ArrayList<>(java.util.Arrays.asList("fromJSON",  "fromJson",  "fromJSONOrNull",  "fromJsonOrNull"))) {
                for (Method method : type.getDeclaredMethods()) {
                    if (!Modifier.isStatic(method.getModifiers())
                            || !method.getName().equals(name)
                            || method.getParameterCount() != 1
                            || method.getParameterTypes()[0] != String.class) {
                        continue;
                    }
                    method.setAccessible(true);
                    Object value = unwrapOptional(method.invoke(null, json));
                    // CraftChatMessage.fromJSON(String) on 1.12.2 answers with
                    // IChatBaseComponent[]; only a single component can label a player, so
                    // reject arrays here and let the next candidate (fromJSONOrNull) produce one.
                    if (value != null && !value.getClass().isArray()) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private Object parseWithMinecraftSerializer(String json) throws ReflectiveOperationException {
        ClassLoader loader = plugin.getServer().getClass().getClassLoader();
        Class<?> componentType = NmsSupport.findClass(loader, nativeComponentClassNames());
        if (componentType == null) {
            return null;
        }

        Class<?> serializerType = getFirstAvailableClassOrNull(
                loader,
                componentType.getName() + "$Serializer",
                componentType.getName() + "$ChatSerializer"
        );
        if (serializerType == null) {
            serializerType = findNestedSerializerType(componentType);
        }
        if (serializerType == null) {
            return null;
        }

        for (Method method : serializerType.getDeclaredMethods()) {
            Object component = parseWithSerializerMethod(componentType, method, json);
            if (component != null) {
                return component;
            }
        }
        for (Method method : serializerType.getMethods()) {
            Object component = parseWithSerializerMethod(componentType, method, json);
            if (component != null) {
                return component;
            }
        }

        return null;
    }

    private List<String> craftChatMessageClassNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        String serverPackage = plugin.getServer().getClass().getPackage().getName();
        if (serverPackage.startsWith("org.bukkit.craftbukkit")) {
            names.add(serverPackage + ".util.CraftChatMessage");
        }
        names.add("org.bukkit.craftbukkit.util.CraftChatMessage");
        return new ArrayList<>(names);
    }

    private Class<?> findNestedSerializerType(Class<?> componentType) {
        for (Class<?> nested : componentType.getDeclaredClasses()) {
            String simpleName = nested.getSimpleName().toLowerCase(Locale.ROOT);
            if (simpleName.contains("serializer") || simpleName.contains("chatserializer")) {
                return nested;
            }
        }
        return null;
    }

    private Object parseWithSerializerMethod(Class<?> componentType, Method method, String json) {
        if (!Modifier.isStatic(method.getModifiers())
                || method.getParameterCount() != 1
                || method.getParameterTypes()[0] != String.class) {
            return null;
        }

        String name = method.getName().toLowerCase(Locale.ROOT);
        if (!name.contains("json") && !name.equals("a") && !name.contains("deserialize")) {
            return null;
        }

        try {
            method.setAccessible(true);
            Object value = unwrapOptional(method.invoke(null, json));
            return componentType.isInstance(value) ? value : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private void setTabListDisplayName(Object handle, Object component) throws ReflectiveOperationException {
        Method method = findDisplayNameSetter(handle.getClass(), component);
        if (method != null) {
            method.setAccessible(true);
            method.invoke(handle, component);
            return;
        }

        Field field = findDisplayNameField(handle, component);
        if (field != null) {
            field.setAccessible(true);
            field.set(handle, component);
            return;
        }

        throw new NoSuchFieldException("ServerPlayer tablist display name");
    }

    private Method findDisplayNameSetter(Class<?> type, Object component) {
        Method fallback = null;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() != 1
                        || !isVoidLike(method.getReturnType())
                        || !method.getParameterTypes()[0].isAssignableFrom(component.getClass())) {
                    continue;
                }
                String name = method.getName().toLowerCase(Locale.ROOT);
                if (name.contains("tab") && name.contains("list") && name.contains("name")) {
                    return method;
                }
                if ((name.contains("display") && name.contains("name"))
                        || (name.contains("list") && name.contains("name"))) {
                    fallback = method;
                }
            }
        }
        return fallback;
    }

    private Field findDisplayNameField(Object handle, Object component) {
        Field fallback = null;
        Field nullableFallback = null;
        Field componentFallback = null;
        Class<?> type = handle.getClass();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isFinal(field.getModifiers())
                        || !field.getType().isAssignableFrom(component.getClass())) {
                    continue;
                }
                if (componentFallback == null) {
                    componentFallback = field;
                }
                String name = field.getName().toLowerCase(Locale.ROOT);
                if (name.contains("tab") && name.contains("list") && name.contains("name")) {
                    return field;
                }
                if ((name.contains("display") && name.contains("name"))
                        || (name.contains("list") && name.contains("name"))) {
                    fallback = field;
                    continue;
                }
                if (nullableFallback == null && isNullField(handle, field)) {
                    nullableFallback = field;
                }
            }
        }
        if (fallback != null) {
            return fallback;
        }
        return nullableFallback != null ? nullableFallback : componentFallback;
    }

    private boolean isNullField(Object handle, Field field) {
        try {
            field.setAccessible(true);
            return field.get(handle) == null;
        } catch (RuntimeException exception) {
            return false;
        } catch (IllegalAccessException exception) {
            return false;
        }
    }

    private Object createDisplayNamePacket(Object handle) throws ReflectiveOperationException {
        return createDisplayNamePacket(handle, null);
    }

    /**
     * Builds the legacy player-list header/footer packet. On 1.12.2 that is
     * {@code PacketPlayOutPlayerListHeaderFooter} with a no-arg constructor plus two
     * {@code IChatBaseComponent} fields in declaration order: {@code a} = header,
     * {@code b} = footer. The constructor scan (and the single-component header
     * constructor) keep the same route working if it ever runs against a different
     * relocated NMS layout; resolution failures surface through the caller's warning.
     */
    private Object createHeaderFooterPacket(Class<?> packetClass, Object header, Object footer)
            throws ReflectiveOperationException {
        Constructor<?> emptyConstructor = null;
        Constructor<?> headerConstructor = null;
        Class<?> nativeComponentType = NmsSupport.findClass(
                plugin.getServer().getClass().getClassLoader(), nativeComponentClassNames());

        for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 0) {
                emptyConstructor = constructor;
            } else if (parameters.length == 1 && isChatComponentLike(parameters[0], nativeComponentType)) {
                headerConstructor = constructor;
            }
        }

        Object packet;
        boolean headerFilled;
        if (emptyConstructor != null) {
            emptyConstructor.setAccessible(true);
            packet = emptyConstructor.newInstance();
            headerFilled = false;
        } else if (headerConstructor != null && header != null) {
            headerConstructor.setAccessible(true);
            packet = headerConstructor.newInstance(header);
            headerFilled = true;
        } else {
            throw new NoSuchMethodException("usable " + packetClass.getSimpleName() + " constructor");
        }

        setHeaderFooterFields(packet, header, footer, headerFilled, nativeComponentType);
        return packet;
    }

    private void setHeaderFooterFields(
            Object packet,
            Object header,
            Object footer,
            boolean skipHeader,
            Class<?> nativeComponentType
    ) throws ReflectiveOperationException {
        List<Field> componentFields = new ArrayList<>();
        for (Class<?> current = packet.getClass();
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())
                        && isChatComponentLike(field.getType(), nativeComponentType)) {
                    componentFields.add(field);
                }
            }
        }

        int index = 0;
        if (!skipHeader) {
            if (header != null && index < componentFields.size()) {
                Field field = componentFields.get(index);
                field.setAccessible(true);
                field.set(packet, header);
            }
            index++;
        }
        if (footer != null && index < componentFields.size()) {
            Field field = componentFields.get(index);
            field.setAccessible(true);
            field.set(packet, footer);
        }
    }

    private static boolean isChatComponentLike(Class<?> type, Class<?> nativeComponentType) {
        if (nativeComponentType != null && nativeComponentType.isAssignableFrom(type)) {
            return true;
        }

        String name = type.getName();
        return name.endsWith("IChatBaseComponent")
                || name.endsWith("chat.Component")
                || name.endsWith(".Component")
                || name.endsWith("ChatComponentText");
    }

    private Object createDisplayNamePacket(Object handle, Object displayName) throws ReflectiveOperationException {
        Class<?> packetClass = getFirstAvailableClass(playerInfoUpdatePacketClassNames());
        Object action = findAction(packetClass, "UPDATE_DISPLAY_NAME", "UPDATE_DISPLAY");
        if (action == null) {
            throw new NoSuchFieldException("UPDATE_DISPLAY_NAME action");
        }
        Object packet = instantiateActionPacket(packetClass, action, handle);
        if (displayName != null) {
            replacePlayerInfoDisplayName(packet, displayName);
        }
        return packet;
    }

    private Object createAddPlayerPacket(Object handle) throws ReflectiveOperationException {
        Class<?> packetClass = getFirstAvailableClass(playerInfoUpdatePacketClassNames());
        List<Object> actions = findActions(
                packetClass,
                "ADD_PLAYER",
                "INITIALIZE_CHAT",
                "UPDATE_LISTED",
                "UPDATE_GAME_MODE",
                "UPDATE_LATENCY",
                "UPDATE_DISPLAY_NAME"
        );
        if (actions.isEmpty()) {
            throw new NoSuchFieldException("ADD_PLAYER action");
        }
        return instantiateActionPacket(packetClass, actions.toArray(), handle);
    }

    private Object createAddPlayerPacket(Object handle, Object displayName) throws ReflectiveOperationException {
        Object packet = createAddPlayerPacket(handle);
        if (displayName != null) {
            replacePlayerInfoDisplayName(packet, displayName);
        }
        return packet;
    }

    private Object createRemovePlayerPacket(UUID playerId, Object handle) throws ReflectiveOperationException {
        try {
            Class<?> packetClass = getFirstAvailableClass(playerInfoRemovePacketClassNames());
            List<UUID> playerIds = java.util.Collections.singletonList(playerId);
            for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                constructor.setAccessible(true);
                if (parameters.length == 1 && Collection.class.isAssignableFrom(parameters[0])) {
                    return constructor.newInstance(playerIds);
                }
                if (parameters.length == 1 && Iterable.class.isAssignableFrom(parameters[0])) {
                    return constructor.newInstance(playerIds);
                }
                if (parameters.length == 1 && parameters[0].isAssignableFrom(UUID.class)) {
                    return constructor.newInstance(playerId);
                }
            }
        } catch (ClassNotFoundException ignored) {
        }

        Class<?> packetClass = getFirstAvailableClass(playerInfoUpdatePacketClassNames());
        Object action = findAction(packetClass, "REMOVE_PLAYER");
        if (action == null) {
            throw new NoSuchFieldException("REMOVE_PLAYER action");
        }
        return instantiateActionPacket(packetClass, action, handle);
    }

    private void replacePlayerInfoDisplayName(Object packet, Object displayName) throws ReflectiveOperationException {
        Object entriesObject = readPlayerInfoEntries(packet);
        if (!(entriesObject instanceof List<?>)) {
            return;
        }

        List<?> entries = (List<?>) entriesObject;
        if (entries.isEmpty()) {
            return;
        }

        List<Object> replaced = new ArrayList<>(entries.size());
        boolean changed = false;
        for (Object entry : entries) {
            Object newEntry = replacePlayerInfoEntryDisplayName(entry, displayName);
            replaced.add(newEntry);
            changed |= newEntry != entry;
        }
        if (!changed) {
            return;
        }

        if (replaceListContents(entriesObject, replaced)) {
            return;
        }

        setPlayerInfoEntries(packet, replaced);
    }

    private Object readPlayerInfoEntries(Object packet) throws ReflectiveOperationException {
        for (Method method : packet.getClass().getMethods()) {
            Object entries = invokeEntriesAccessor(packet, method);
            if (entries != null) {
                return entries;
            }
        }
        for (Method method : packet.getClass().getDeclaredMethods()) {
            Object entries = invokeEntriesAccessor(packet, method);
            if (entries != null) {
                return entries;
            }
        }

        for (Class<?> current = packet.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(packet);
                if (value instanceof List<?>) {
                    List<?> list = (List<?>) value;
                    if (!list.isEmpty()) {
                        return list;
                    }
                }
            }
        }
        return null;
    }

    private Object invokeEntriesAccessor(Object packet, Method method) {
        if (method.getParameterCount() != 0 || !List.class.isAssignableFrom(method.getReturnType())) {
            return null;
        }

        String name = method.getName().toLowerCase(Locale.ROOT);
        if (!name.contains("entr") && !name.equals("e") && !name.equals("f") && !name.equals("c")) {
            return null;
        }

        try {
            method.setAccessible(true);
            Object value = method.invoke(packet);
            if (value instanceof List<?> && !((List<?>) value).isEmpty()) {
                return value;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return null;
    }

    private Object replacePlayerInfoEntryDisplayName(Object entry, Object displayName)
            throws ReflectiveOperationException {
        if (entry == null || displayName == null) {
            return entry;
        }

        Object byRecord = replaceRecordDisplayName(entry, displayName);
        if (byRecord != null) {
            return byRecord;
        }

        if (setDisplayNameField(entry, displayName)) {
            return entry;
        }
        return entry;
    }

    private Object replaceRecordDisplayName(Object entry, Object displayName)
            throws ReflectiveOperationException {
        Class<?> entryClass = entry.getClass();
        Boolean isRecord = invokeBooleanMethod(entryClass, "isRecord");
        if (!Boolean.TRUE.equals(isRecord)) {
            return null;
        }

        Object[] components = invokeRecordComponents(entryClass);
        if (components == null || components.length == 0) {
            return null;
        }

        Constructor<?> constructor = null;
        for (Constructor<?> candidate : entryClass.getDeclaredConstructors()) {
            if (candidate.getParameterCount() == components.length) {
                constructor = candidate;
                break;
            }
        }
        if (constructor == null) {
            return null;
        }

        Object[] args = new Object[components.length];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        boolean replaced = false;
        for (int index = 0; index < components.length; index++) {
            Method accessor = invokeRecordAccessor(components[index]);
            if (accessor == null) {
                return null;
            }
            accessor.setAccessible(true);
            Object value = accessor.invoke(entry);
            if (!replaced && parameterTypes[index].isAssignableFrom(displayName.getClass())) {
                value = displayName;
                replaced = true;
            }
            args[index] = value;
        }

        if (!replaced) {
            return null;
        }

        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }

    private static Boolean invokeBooleanMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            Object result = method.invoke(target);
            return result instanceof Boolean ? (Boolean) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object[] invokeRecordComponents(Class<?> entryClass) {
        try {
            Method method = Class.class.getMethod("getRecordComponents");
            method.setAccessible(true);
            Object value = method.invoke(entryClass);
            return value instanceof Object[] ? (Object[]) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method invokeRecordAccessor(Object component) {
        try {
            Method method = component.getClass().getMethod("getAccessor");
            method.setAccessible(true);
            Object accessor = method.invoke(component);
            return accessor instanceof Method ? (Method) accessor : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean setDisplayNameField(Object entry, Object displayName) {
        for (Class<?> current = entry.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.getType().isAssignableFrom(displayName.getClass())) {
                    continue;
                }

                String name = field.getName().toLowerCase(Locale.ROOT);
                if (!name.contains("display") && !name.contains("name") && !name.equals("f")) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    field.set(entry, displayName);
                    return true;
                } catch (RuntimeException | IllegalAccessException ignored) {
                }
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean replaceListContents(Object entriesObject, List<Object> replaced) {
        if (!(entriesObject instanceof List)) {
            return false;
        }

        List entries = (List) entriesObject;

        try {
            entries.clear();
            entries.addAll(replaced);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void setPlayerInfoEntries(Object packet, List<Object> replaced) throws ReflectiveOperationException {
        for (Class<?> current = packet.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(packet);
                if (value instanceof List<?> && !((List<?>) value).isEmpty()) {
                    field.set(packet, replaced);
                    return;
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object instantiateActionPacket(Class<?> packetClass, Object action, Object handle)
            throws ReflectiveOperationException {
        return instantiateActionPacket(packetClass, new Object[]{action}, handle);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object instantiateActionPacket(Class<?> packetClass, Object[] actions, Object handle)
            throws ReflectiveOperationException {
        EnumSet actionSet = null;
        Object action = actions.length == 0 ? null : actions[0];
        if (action instanceof Enum) {
            Enum enumAction = (Enum) action;
            actionSet = EnumSet.noneOf(enumAction.getDeclaringClass());
            for (Object candidate : actions) {
                if (candidate instanceof Enum) {
                    Enum candidateAction = (Enum) candidate;
                    if (candidateAction.getDeclaringClass() == enumAction.getDeclaringClass()) {
                        actionSet.add(candidateAction);
                    }
                }
            }
        }
        List<Object> handles = java.util.Collections.singletonList(handle);

        for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != 2) {
                continue;
            }
            constructor.setAccessible(true);

            if (actionSet != null
                    && EnumSet.class.isAssignableFrom(parameters[0])
                    && (Collection.class.isAssignableFrom(parameters[1])
                    || Iterable.class.isAssignableFrom(parameters[1]))) {
                return constructor.newInstance(actionSet, handles);
            }
            if (actionSet != null
                    && EnumSet.class.isAssignableFrom(parameters[1])
                    && (Collection.class.isAssignableFrom(parameters[0])
                    || Iterable.class.isAssignableFrom(parameters[0]))) {
                return constructor.newInstance(handles, actionSet);
            }
            if (parameters[0].isAssignableFrom(action.getClass()) && parameters[1].isArray()) {
                Object array = Array.newInstance(parameters[1].getComponentType(), 1);
                Array.set(array, 0, handle);
                return constructor.newInstance(action, array);
            }
            if (parameters[1].isAssignableFrom(action.getClass()) && parameters[0].isArray()) {
                Object array = Array.newInstance(parameters[0].getComponentType(), 1);
                Array.set(array, 0, handle);
                return constructor.newInstance(array, action);
            }
            if (parameters[0].isAssignableFrom(action.getClass())
                    && (Collection.class.isAssignableFrom(parameters[1])
                    || Iterable.class.isAssignableFrom(parameters[1]))) {
                return constructor.newInstance(action, handles);
            }
            if (parameters[1].isAssignableFrom(action.getClass())
                    && (Collection.class.isAssignableFrom(parameters[0])
                    || Iterable.class.isAssignableFrom(parameters[0]))) {
                return constructor.newInstance(handles, action);
            }
        }

        throw new NoSuchMethodException(packetClass.getName() + "(UPDATE_DISPLAY_NAME,ServerPlayer)");
    }

    private Object findAction(Class<?> packetClass, String... names) {
        List<Object> actions = findActions(packetClass, names);
        return actions.isEmpty() ? null : actions.get(0);
    }

    private List<Object> findActions(Class<?> packetClass, String... names) {
        java.util.ArrayList<Object> actions = new java.util.ArrayList<>();
        for (Class<?> nested : packetClass.getDeclaredClasses()) {
            if (!nested.isEnum()) {
                continue;
            }
            Object[] constants = nested.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (String name : names) {
                for (Object constant : constants) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(name) && !actions.contains(constant)) {
                        actions.add(constant);
                    }
                }
            }
        }
        return actions;
    }

    private void sendPacket(Player player, Object packet) throws ReflectiveOperationException {
        Object handle = invokeNoArg(player, "getHandle");
        PacketSender sender = findPacketSender(handle, packet);
        if (sender == null) {
            throw new NoSuchMethodException("packet sender");
        }
        sender.send(packet);
    }

    private PacketSender findPacketSender(Object root, Object packet) throws ReflectiveOperationException {
        PacketSender sender = findSenderOn(root, packet);
        if (sender != null) {
            return sender;
        }

        for (Class<?> current = root.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(root);
                if (value == null) {
                    continue;
                }
                sender = findSenderOn(value, packet);
                if (sender != null) {
                    return sender;
                }
            }
        }

        return null;
    }

    private PacketSender findSenderOn(Object target, Object packet) {
        PacketSender fallback = null;
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length == 1 && acceptsPacket(parameters[0], packet)) {
                    PacketSender sender = new PacketSender(target, method, false);
                    if (isPreferredSendMethod(method)) {
                        return sender;
                    }
                    fallback = sender;
                }
                if (parameters.length == 2 && acceptsPacket(parameters[0], packet)) {
                    PacketSender sender = new PacketSender(target, method, true);
                    if (isPreferredSendMethod(method)) {
                        return sender;
                    }
                    fallback = sender;
                }
            }
        }
        return fallback;
    }

    private boolean acceptsPacket(Class<?> parameterType, Object packet) {
        return parameterType.isAssignableFrom(packet.getClass())
                || "net.minecraft.network.protocol.Packet".equals(parameterType.getName())
                || "Packet".equals(parameterType.getSimpleName());
    }

    private boolean isPreferredSendMethod(Method method) {
        String name = method.getName().toLowerCase(Locale.ROOT);
        return "send".equals(name) || "sendpacket".equals(name) || "a".equals(name);
    }

    private String getStringValue(Object target, String methodName) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }

        Object value = unwrapOptional(invokeNoArg(target, methodName));
        return value instanceof String ? (String) value : null;
    }

    private UUID getUuidValue(Object target, String methodName) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }

        Object value = unwrapOptional(invokeNoArg(target, methodName));
        return value instanceof UUID ? (UUID) value : null;
    }

    private boolean getBooleanValue(Object target, String methodName) throws ReflectiveOperationException {
        if (target == null) {
            return false;
        }

        Object value = unwrapOptional(invokeNoArg(target, methodName));
        return value instanceof Boolean && (Boolean) value;
    }

    private Object invokeNoArg(Object target, String name) throws ReflectiveOperationException {
        Method method = findNoArgMethod(target.getClass(), name);
        if (method == null) {
            throw new NoSuchMethodException(name);
        }
        method.setAccessible(true);
        return method.invoke(target);
    }

    private Method findNoArgMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && method.getName().equals(name)) {
                    return method;
                }
            }
        }
        return null;
    }

    private Class<?> getFirstAvailableClass(Collection<String> classNames) throws ClassNotFoundException {
        ClassLoader loader = plugin.getServer().getClass().getClassLoader();
        for (String className : classNames) {
            try {
                return Class.forName(className, false, loader);
            } catch (ClassNotFoundException | LinkageError ignored) {
            }
        }
        throw new ClassNotFoundException(String.join(", ", classNames));
    }

    private Class<?> getFirstAvailableClassOrNull(ClassLoader loader, String... classNames) {
        for (String className : classNames) {
            try {
                return Class.forName(className, false, loader);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private Object unwrapOptional(Object value) {
        if (value instanceof Optional<?>) {
            Optional<?> optional = (Optional<?>) value;
            return optional.orElse(null);
        }
        return value;
    }

    private boolean isVoidLike(Class<?> type) {
        return type == Void.TYPE || type == Void.class;
    }

    private void disableWithWarning(Exception exception) {
        if (warned) {
            return;
        }
        warned = true;
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        plugin.getLogger().warning("[Tablist] Unable to send Adventure tablist name components on this Spigot build: "
                + cause.getClass().getSimpleName() + ": " + cause.getMessage()
                + ". Future tablist updates will retry component rendering.");
    }

    private void disableAvatarWithWarning(Exception exception) {
        if (avatarWarned) {
            return;
        }
        avatarWarned = true;
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        plugin.getLogger().warning("[Tablist] Unable to refresh tablist skin avatars on this Spigot build: "
                + cause.getClass().getSimpleName() + ": " + cause.getMessage()
                + ". Future skin refreshes will retry avatar packets.");
    }

    private void disableHeaderFooterWithWarning(Exception exception) {
        if (headerFooterWarned) {
            return;
        }
        headerFooterWarned = true;
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        plugin.getLogger().warning("[Tablist] Unable to send tab header/footer packets on this Spigot build: "
                + cause.getClass().getSimpleName() + ": " + cause.getMessage()
                + ". Header/footer updates will keep retrying.");
    }

    private static final class PacketSender {

        private final Object target;
        private final Method method;
        private final boolean trailingNull;

        private PacketSender(Object target, Method method, boolean trailingNull) {
            this.target = target;
            this.method = method;
            this.trailingNull = trailingNull;
        }

        private void send(Object packet) throws ReflectiveOperationException {
            method.setAccessible(true);
            if (trailingNull) {
                method.invoke(target, packet, null);
            } else {
                method.invoke(target, packet);
            }
        }
    }
}
