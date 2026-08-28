package com.bx.ultimateDonutSmp.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSettingsConfigurationTest {

    private static final List<String> REQUIRED_SETTINGS = new java.util.ArrayList<>(java.util.Arrays.asList(
            "PUBLIC_CHAT", 
            "PRIVATE_MESSAGES", 
            "SERVER_BROADCASTS", 
            "HOTBAR_MESSAGES", 
            "PAY_ALERTS", 
            "BOUNTY_ALERTS", 
            "AUCTION_NOTIFICATIONS", 
            "FAST_CRYSTALS", 
            "EXPLOSION_PARTICLES", 
            "QUICK_AUCTION_PURCHASE", 
            "CHAINMAIL_ON_RESPAWN", 
            "DISABLE_MOB_SPAWN", 
            "NOTIFICATION_SOUNDS", 
            "ORDER_NOTIFICATIONS", 
            "TPA_REQUESTS", 
            "PAYMENTS", 
            "TEAM_CHAT_VISIBILITY", 
            "WORTH_DISPLAY", 
            "MONEY_NAMETAGS", 
            "DESTROY_PEARL_ON_DEATH", 
            "RANDOMIZED_COORDS", 
            "DEATH_MESSAGES", 
            "ADVANCEMENT_MESSAGES", 
            "JOIN_LEAVE_MESSAGES", 
            "TELEPORT_ALERTS", 
            "FOLLOW_ALERT_SETTINGS", 
            "EXPLOSION_SOUNDS", 
            "DISPLAY_DONUT_PLUS", 
            "QUICK_AUCTION_SELL"
    ));

    @Test
    void settingsMenuContainsEveryRequestedSettingInUniqueValidSlots() throws Exception {
        YamlConfiguration menus = load("menus.yml");
        assertEquals(54, menus.getInt("SETTINGS-MENU.SIZE"));

        ConfigurationSection buttons = menus.getConfigurationSection("SETTINGS-MENU.BUTTONS");
        assertNotNull(buttons);

        Set<Integer> usedSlots = new HashSet<>();
        for (String key : buttons.getKeys(false)) {
            assertFalse(key.startsWith("HEADER_"), key + " must not render a category header");
            int slot = buttons.getInt(key + ".SLOT", -1);
            assertTrue(slot >= 0 && slot < 54, key + " has invalid slot " + slot);
            assertTrue(usedSlots.add(slot), key + " duplicates slot " + slot);
        }

        for (String setting : REQUIRED_SETTINGS) {
            assertTrue(buttons.isConfigurationSection(setting), setting);
        }

        Map<String, Integer> centeredSlots = new java.util.LinkedHashMap(){{ put("PUBLIC_CHAT",  0); put("PRIVATE_MESSAGES",  1); put("SERVER_BROADCASTS",  2); put("TEAM_CHAT_VISIBILITY",  3); put("LUNAR_TEAMMATES",  4); put("TPA_CONFIRM_MENUS",  5); put("QUICK_AUCTION_PURCHASE",  9); put("DESTROY_PEARL_ON_DEATH",  10); put("PAY_CONFIRM_MENUS",  11); put("PAY_ALERTS",  32); put("AUTO_CONFIRM_TPAS",  12); put("HOTBAR_MESSAGES",  13); put("NOTIFICATION_SOUNDS",  14); put("FOLLOW_ALERT_SETTINGS",  15); put("DISPLAY_DONUT_PLUS",  18); put("CHAINMAIL_ON_RESPAWN",  19); put("EXPLOSION_PARTICLES",  20); put("EXPLOSION_SOUNDS",  21); put("TELEPORT_ALERTS",  22); put("FAST_CRYSTALS",  23); put("RANDOMIZED_COORDS",  24); put("TPA_REQUESTS",  27); put("TPA_HERE_REQUESTS",  28); put("PAYMENTS",  29); put("WORTH_DISPLAY",  30); put("MONEY_NAMETAGS",  33); put("JOIN_LEAVE_MESSAGES",  31); put("ADVANCEMENT_MESSAGES",  36); put("AUCTION_NOTIFICATIONS",  37); put("AMETHYST_BREAK_MESSAGES",  38); put("DEATH_MESSAGES",  40); put("KEY_ALL_NOTIFICATIONS",  41); put("QUICK_AUCTION_SELL",  45); put("ORDER_NOTIFICATIONS",  46); put("DISABLE_MOB_SPAWN",  47); put("DISABLE_PHANTOM_SPAWN",  48); put("NIGHT_VISION",  49); put("BOUNTY_ALERTS",  50); }};
        centeredSlots.forEach((key, slot) ->
                assertEquals(slot, buttons.getInt(key + ".SLOT"), key));
    }

    @Test
    void hiddenRtpTemplatesNeverExposeCoordinates() throws Exception {
        YamlConfiguration rtp = load("rtp.yml");

        for (String path : new java.util.ArrayList<>(java.util.Arrays.asList(
                "MESSAGES.SAFE-LOCATION-FOUND-HIDDEN", 
                "MESSAGES.SEARCH-FOUND-ACTIONBAR-HIDDEN"
        ))) {
            String message = rtp.getString(path);
            assertNotNull(message, path);
            assertFalse(message.contains("{x}"), path);
            assertFalse(message.contains("{y}"), path);
            assertFalse(message.contains("{z}"), path);
        }
    }

    private static YamlConfiguration load(String fileName) throws Exception {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(java.nio.file.Paths.get("src/main/resources", fileName).toFile());
        return configuration;
    }
}
