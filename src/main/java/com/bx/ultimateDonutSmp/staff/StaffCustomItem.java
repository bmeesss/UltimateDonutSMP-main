package com.bx.ultimateDonutSmp.staff;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An admin defined staff mode hotbar item that runs one or more commands when it is used.
 *
 * <p>Definitions live under {@code CUSTOM-ITEMS} in {@code staff-mode.yml}. Unlike the built-in
 * tools these are not backed by an enum constant, so the config key doubles as the identity that
 * is written into the item's persistent data container.</p>
 */
public final class StaffCustomItem {
    private final String id;
    private final int slot;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final List<String> commands;
    private final ExecuteAs executeAs;
    private final String permission;
    private final boolean requireTarget;

    public StaffCustomItem(String id, int slot, Material material, String name, List<String> lore, List<String> commands, ExecuteAs executeAs, String permission, boolean requireTarget) {
        lore = lore == null ? java.util.Collections.emptyList() : new java.util.ArrayList<>(lore);
        commands = commands == null ? java.util.Collections.emptyList() : new java.util.ArrayList<>(commands);
        this.id = id;
        this.slot = slot;
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.commands = commands;
        this.executeAs = executeAs;
        this.permission = permission;
        this.requireTarget = requireTarget;
    }

    public String id() { return id; }
    public int slot() { return slot; }
    public Material material() { return material; }
    public String name() { return name; }
    public List<String> lore() { return lore; }
    public List<String> commands() { return commands; }
    public ExecuteAs executeAs() { return executeAs; }
    public String permission() { return permission; }
    public boolean requireTarget() { return requireTarget; }



    /** Who the configured commands are dispatched as. */
    public enum ExecuteAs {
        PLAYER,
        CONSOLE;

        public static ExecuteAs parse(String raw, ExecuteAs fallback) {
            if (raw == null || raw.trim().isEmpty()) {
                return fallback;
            }
            try {
                return ExecuteAs.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
    }



    public boolean hasPermission() {
        return permission != null && !permission.trim().isEmpty();
    }

    /**
     * Reads every enabled definition under {@code section}, dropping the ones that cannot be used.
     *
     * @param section        the {@code CUSTOM-ITEMS} section, may be {@code null}
     * @param reservedSlots  hotbar slots already taken by the built-in staff tools
     * @param warningSink    receives one message per rejected definition
     * @return the usable definitions, in config order
     */
    public static List<StaffCustomItem> parseAll(ConfigurationSection section,
                                                 Set<Integer> reservedSlots,
                                                 Consumer<String> warningSink) {
        if (section == null) {
            return java.util.Collections.emptyList();
        }

        Set<Integer> taken = new HashSet<>(reservedSlots == null ? java.util.Collections.emptySet() : reservedSlots);
        List<StaffCustomItem> parsed = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection definition = section.getConfigurationSection(key);
            if (definition == null) {
                warn(warningSink, key, "it is not a configuration section");
                continue;
            }
            if (!definition.getBoolean("ENABLED", true)) {
                continue;
            }

            String id = key.trim().toUpperCase(Locale.ROOT);

            int slot = definition.getInt("SLOT", -1);
            if (slot < 0 || slot > 8) {
                warn(warningSink, key, "SLOT " + slot + " is outside the hotbar (0-8)");
                continue;
            }
            if (!taken.add(slot)) {
                warn(warningSink, key, "SLOT " + slot + " is already used by another staff mode item");
                continue;
            }

            List<String> commands = readCommands(definition);
            if (commands.isEmpty()) {
                warn(warningSink, key, "it has no COMMANDS to run");
                continue;
            }

            ExecuteAs executeAs = ExecuteAs.parse(definition.getString("EXECUTE-AS"), ExecuteAs.PLAYER);
            if (executeAs == null) {
                warn(warningSink, key, "EXECUTE-AS must be PLAYER or CONSOLE");
                continue;
            }

            Material material = Material.matchMaterial(
                    String.valueOf(definition.getString("MATERIAL", "STONE")).trim().toUpperCase(Locale.ROOT));
            if (material == null) {
                if (warningSink != null) {
                    warningSink.accept("Staff mode custom item '" + key + "' has an unknown MATERIAL '"
                            + definition.getString("MATERIAL") + "', falling back to STONE.");
                }
                material = Material.STONE;
            }

            parsed.add(new StaffCustomItem(
                    id,
                    slot,
                    material,
                    definition.getString("NAME", "&f" + id),
                    definition.getStringList("LORE"),
                    commands,
                    executeAs,
                    definition.getString("PERMISSION", ""),
                    definition.getBoolean("REQUIRE-TARGET", false)
            ));
        }

        return Collections.unmodifiableList(parsed);
    }

    private static List<String> readCommands(ConfigurationSection definition) {
        List<String> raw = new ArrayList<>(definition.getStringList("COMMANDS"));
        if (raw.isEmpty()) {
            String single = definition.getString("COMMANDS");
            if (single != null && !single.trim().isEmpty()) {
                raw.add(single);
            }
        }

        List<String> commands = new ArrayList<>();
        for (String command : raw) {
            String normalized = normalizeCommand(command);
            if (!normalized.isEmpty()) {
                commands.add(normalized);
            }
        }
        return commands;
    }

    private static String normalizeCommand(String command) {
        if (command == null) {
            return "";
        }
        String trimmed = command.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed;
    }

    private static void warn(Consumer<String> warningSink, String key, String reason) {
        if (warningSink != null) {
            warningSink.accept("Skipping staff mode custom item '" + key + "' because " + reason + ".");
        }
    }
    @Override public String toString() {
        return "StaffCustomItem[id=+id, slot=+slot, material=+material, name=+name, lore=+lore, commands=+commands, executeAs=+executeAs, permission=+permission, requireTarget=+requireTarget]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaffCustomItem that = (StaffCustomItem) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(slot, that.slot) && java.util.Objects.equals(material, that.material) && java.util.Objects.equals(name, that.name) && java.util.Objects.equals(lore, that.lore) && java.util.Objects.equals(commands, that.commands) && java.util.Objects.equals(executeAs, that.executeAs) && java.util.Objects.equals(permission, that.permission) && java.util.Objects.equals(requireTarget, that.requireTarget);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(id, slot, material, name, lore, commands, executeAs, permission, requireTarget);
    }
}
