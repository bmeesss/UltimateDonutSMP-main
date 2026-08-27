package com.bx.ultimateDonutSmp.menus;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import com.bx.ultimateDonutSmp.utils.ItemUtils;
import com.bx.ultimateDonutSmp.utils.SoundUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RulesMenu extends BaseMenu {

    private static final String MENU_PATH = "RULES-MENU";
    private static final String BUTTONS_PATH = MENU_PATH + ".BUTTONS";
    private static final String CLICK_SOUND_PATH = "MENUS.BUTTON-CLICK";

    private final List<RulesButton> buttons;
    private final Map<Integer, RulesButton> slotButtons = new HashMap<>();

    public RulesMenu(UltimateDonutSmp plugin) {
        super(plugin, configuredTitle(plugin), configuredSize(plugin));
        this.buttons = loadButtons(plugin, inventory.getSize());
    }

    public boolean hasValidButtons() {
        return !buttons.isEmpty();
    }

    @Override
    public void build(Player player) {
        clear();
        fill(Material.STAINED_GLASS_PANE, (short) 15);
        slotButtons.clear();

        int renderedButtons = 0;
        for (RulesButton button : buttons) {
            if (slotButtons.containsKey(button.slot())) {
                plugin.getLogger().warning("skipping duplicate rules menu slot " + button.slot()
                        + " for button " + button.key() + ".");
                continue;
            }

            set(button.slot(), ItemUtils.createItem(button.material(), button.displayName(), button.lore()));
            slotButtons.put(button.slot(), button);
            renderedButtons++;
        }

        if (renderedButtons == 0) {
            setFallbackItem("&cNo usable rules buttons", "&7Fix rules-menu.buttons to use the GUI.");
        }
    }

    @Override
    public void handleClick(int slot, Player player) {
        RulesButton button = slotButtons.get(slot);
        if (button == null) {
            return;
        }

        SoundUtils.play(player, plugin.getConfigManager().getSound(CLICK_SOUND_PATH));
        List<String> clickMessages = button.clickMessages().isEmpty()
                ? defaultClickMessages(button.key(), button.displayName())
                : button.clickMessages();

        for (String line : clickMessages) {
            player.sendMessage(ColorUtils.toComponent(line));
        }
    }

    private void setFallbackItem(String title, String lore) {
        set(inventory.getSize() / 2, ItemUtils.createItem(Material.BARRIER, title, java.util.Collections.singletonList(lore)));
    }

    private static List<RulesButton> loadButtons(UltimateDonutSmp plugin, int inventorySize) {
        FileConfiguration menus = plugin.getConfigManager().getMenus();
        ConfigurationSection buttonsSection = menus.getConfigurationSection(BUTTONS_PATH);
        List<RulesButton> loadedButtons = new ArrayList<>();

        if (buttonsSection == null || buttonsSection.getKeys(false).isEmpty()) {
            plugin.getLogger().warning("no buttons found at " + BUTTONS_PATH + ".");
            return loadedButtons;
        }

        for (String key : buttonsSection.getKeys(false)) {
            ConfigurationSection buttonSection = buttonsSection.getConfigurationSection(key);
            if (buttonSection == null) {
                plugin.getLogger().warning("Skipping " + BUTTONS_PATH + "." + key
                        + " because it is not a section.");
                continue;
            }

            int slot = buttonSection.getInt("SLOT", -1);
            if (slot < 0 || slot >= inventorySize) {
                plugin.getLogger().warning("Skipping " + buttonSection.getCurrentPath()
                        + " because slot " + slot + " is outside menu size " + inventorySize + ".");
                continue;
            }

            String rawMaterial = buttonSection.getString("MATERIAL");
            if (rawMaterial == null || rawMaterial.trim().isEmpty()) {
                plugin.getLogger().warning("Skipping " + buttonSection.getCurrentPath()
                        + " because material is missing.");
                continue;
            }

            Material material = Material.matchMaterial(rawMaterial.trim().toUpperCase(Locale.ROOT));
            if (material == null) {
                plugin.getLogger().warning("Skipping " + buttonSection.getCurrentPath()
                        + " because material '" + rawMaterial + "' is invalid.");
                continue;
            }

            loadedButtons.add(new RulesButton(
                    key,
                    slot,
                    material,
                    buttonSection.getString("NAME", prettifyKey(key)),
                    buttonSection.getStringList("LORE"),
                    buttonSection.getStringList("CLICK-MESSAGE")
            ));
        }

        loadedButtons.sort(Comparator.comparingInt(RulesButton::slot));
        return loadedButtons;
    }

    private static String configuredTitle(UltimateDonutSmp plugin) {
        return plugin.getConfigManager().getMenus().getString(MENU_PATH + ".TITLE", "&8Rules");
    }

    private static int configuredSize(UltimateDonutSmp plugin) {
        int rawSize = plugin.getConfigManager().getMenus().getInt(MENU_PATH + ".SIZE", 27);
        if (rawSize >= 9 && rawSize <= 54 && rawSize % 9 == 0) {
            return rawSize;
        }

        plugin.getLogger().warning("invalid " + MENU_PATH + ".SIZE value '" + rawSize
                + "'. Falling back to 27.");
        return 27;
    }

    private static List<String> defaultClickMessages(String key, String displayName) {
        String normalizedKey = key.toUpperCase(Locale.ROOT);
        String strippedName = ColorUtils.strip(displayName).toLowerCase(Locale.ROOT);

        if (normalizedKey.contains("CHAT") || strippedName.contains("chat")) {
            return new java.util.ArrayList<>(java.util.Arrays.asList(
                    "&7Keep chat respectful and report serious issues in the Discord.", 
                    "&7Breaking chat rules can lead to punishments."
            ));
        }

        if (normalizedKey.contains("SERVER") || strippedName.contains("server")) {
            return new java.util.ArrayList<>(java.util.Arrays.asList(
                    "&7Read the full server rules carefully before playing.", 
                    "&7Violating server rules may result in punishments."
            ));
        }

        return java.util.Collections.singletonList("&7Make sure you understand these rules before continuing.");
    }

    private static String prettifyKey(String key) {
        String[] parts = key.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() != 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() == 0 ? "Rules" : builder.toString();
    }

    public static final class RulesButton {
    private final String key;
    private final int slot;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final List<String> clickMessages;

    public RulesButton(String key, int slot, Material material, String displayName, List<String> lore, List<String> clickMessages) {
        this.key = key;
        this.slot = slot;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.clickMessages = clickMessages;
    }

    public String key() { return key; }
    public int slot() { return slot; }
    public Material material() { return material; }
    public String displayName() { return displayName; }
    public List<String> lore() { return lore; }
    public List<String> clickMessages() { return clickMessages; }

    @Override public String toString() {
        return "RulesButton[key=+key, slot=+slot, material=+material, displayName=+displayName, lore=+lore, clickMessages=+clickMessages]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RulesButton that = (RulesButton) o;
        return java.util.Objects.equals(key, that.key) && java.util.Objects.equals(slot, that.slot) && java.util.Objects.equals(material, that.material) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(lore, that.lore) && java.util.Objects.equals(clickMessages, that.clickMessages);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(key, slot, material, displayName, lore, clickMessages);
    }
}
}
