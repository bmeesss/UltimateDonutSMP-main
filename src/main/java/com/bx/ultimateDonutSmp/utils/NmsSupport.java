package com.bx.ultimateDonutSmp.utils;

import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Version-safe NMS class-name resolution for Spigot 1.12.2 and modern Spigot/Paper builds.
 *
 * <p>The plugin ships code paths that have to reach a handful of NMS types by reflection.
 * Up to and including 1.16.5 those types live in the relocated package
 * {@code net.minecraft.server.<CraftBukkitVersion>} (for example
 * {@code net.minecraft.server.v1_12_R1.PacketPlayOutPlayerInfo}). From 1.17 onwards they live in
 * the un-versioned {@code net.minecraft.*} packages. A hard-coded {@code net.minecraft.*} name
 * therefore resolves on a modern server and throws {@link ClassNotFoundException} on 1.12.2,
 * which is exactly how several "the component route disabled itself" paths fail on this
 * server's target platform.</p>
 *
 * <p>This helper derives the legacy package prefix from the running CraftBukkit package and
 * appends it to every candidate list, so a single call site can keep working on both layouts
 * without branching on the Minecraft version.</p>
 *
 * <p>It never throws: when no CraftBukkit package can be read (for example inside a unit test)
 * the legacy prefix simply is not added and only the modern names are tried, which reproduces
 * the previous behaviour.</p>
 */
public final class NmsSupport {

    private static final String MODERN_ROOT = "net.minecraft.";
    private static final String LEGACY_ROOT = "net.minecraft.server.";
    private static final String CRAFTBUKKIT_PACKAGE = "org.bukkit.craftbukkit";

    private static final String NMS_VERSION;
    private static final String LEGACY_PREFIX;

    static {
        String version = "";
        String prefix = LEGACY_ROOT;
        try {
            Server server = Bukkit.getServer();
            if (server != null) {
                String serverPackage = server.getClass().getPackage().getName();
                if (serverPackage.startsWith(CRAFTBUKKIT_PACKAGE)) {
                    String suffix = serverPackage.substring(CRAFTBUKKIT_PACKAGE.length());
                    while (suffix.startsWith(".")) {
                        suffix = suffix.substring(1);
                    }
                    if (!suffix.isEmpty()) {
                        version = suffix;
                        prefix = LEGACY_ROOT + suffix + ".";
                    }
                }
            }
        } catch (Throwable ignored) {
            // No server available (unit tests): keep the modern-only layout.
        }
        NMS_VERSION = version;
        LEGACY_PREFIX = prefix;
    }

    private NmsSupport() {
    }

    /**
     * The CraftBukkit package suffix, e.g. {@code v1_12_R1}. Empty on 1.17+ where CraftBukkit is
     * no longer relocated.
     */
    public static String nmsVersion() {
        return NMS_VERSION;
    }

    /**
     * Whether this server uses the relocated {@code net.minecraft.server.<version>} NMS layout
     * (Spigot 1.16.5 and older). {@code false} on 1.17+.
     */
    public static boolean isRelocatedNms() {
        return !NMS_VERSION.isEmpty();
    }

    /**
     * Appends the versioned NMS name of every simple class name to {@code names}.
     *
     * <p>Only simple names are relocated. Fully qualified entries are left untouched so callers
     * can mix modern {@code net.minecraft.*} names with legacy simple names in one list.</p>
     */
    public static void addVersioned(Collection<String> names, String... simpleNames) {
        if (names == null || simpleNames == null) {
            return;
        }
        for (String simpleName : simpleNames) {
            if (simpleName == null || simpleName.isEmpty() || simpleName.indexOf('.') >= 0) {
                continue;
            }
            names.add(LEGACY_PREFIX + simpleName);
        }
    }

    /**
     * Builds an ordered candidate list: the given modern fully qualified names first, then the
     * versioned NMS name of every legacy simple name.
     */
    public static List<String> candidates(String[] modernNames, String... legacySimpleNames) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (modernNames != null) {
            for (String name : modernNames) {
                if (name != null && !name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        addVersioned(names, legacySimpleNames);
        return new java.util.ArrayList<>(names);
    }

    /**
     * Resolves the first loadable class from an ordered candidate list.
     *
     * @return the class, or {@code null} when none of the candidates exists
     */
    public static Class<?> findClass(ClassLoader loader, Collection<String> names) {
        if (names == null) {
            return null;
        }
        for (String name : names) {
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException | LinkageError ignored) {
            }
        }
        return null;
    }

    /** Resolves the first loadable class, or {@code null} when none exists. */
    public static Class<?> findClass(ClassLoader loader, String... names) {
        return findClass(loader, candidates(names));
    }
}
