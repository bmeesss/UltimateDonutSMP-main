package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.models.HideMode;
import com.bx.ultimateDonutSmp.models.HideState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseManagerHideStateTest {

    @Test
    void hideStateCrudAndCaseInsensitiveAliasUniqueness() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement statement = connection.createStatement()) {
            statement.execute(String.join("\n", "CREATE TABLE hide_states (\n", "player_uuid TEXT PRIMARY KEY,\n", "real_name_snapshot TEXT NOT NULL,\n", "mode TEXT NOT NULL,\n", "alias TEXT NOT NULL,\n", "alias_normalized TEXT NOT NULL UNIQUE,\n", "skin_key TEXT,\n", "skin_username TEXT,\n", "texture_value TEXT,\n", "texture_signature TEXT,\n", "created_at INTEGER NOT NULL,\n", "updated_at INTEGER NOT NULL\n", ")\n"));

            DatabaseManager manager = manager(connection);
            UUID firstUuid = UUID.randomUUID();
            UUID secondUuid = UUID.randomUUID();
            HideState first = state(firstUuid, "First", "StableAlias", HideMode.SCRAMBLE);

            assertTrue(manager.saveHideState(first));
            assertEquals(first, manager.loadHideState(firstUuid));
            assertEquals("original-texture", manager.loadHideState(firstUuid).textureValue());
            assertEquals(1, manager.loadAllHideStates().size());

            HideState collision = state(secondUuid, "Second", "stablealias", HideMode.DISGUISE);
            assertFalse(manager.saveHideState(collision));
            assertNull(manager.loadHideState(secondUuid));
            assertEquals(first, manager.loadHideState(firstUuid));

            HideState updated = new HideState(
                    firstUuid,
                    "First",
                    HideMode.DISGUISE,
                    "OtherAlias",
                    "otheralias",
                    "dream",
                    "Dream",
                    "texture",
                    "signature",
                    first.createdAt(),
                    3L
            );
            assertTrue(manager.saveHideState(updated));
            assertEquals(updated, manager.loadHideState(firstUuid));

            manager.deleteHideState(firstUuid);
            assertNull(manager.loadHideState(firstUuid));
        }
    }

    private DatabaseManager manager(Connection connection) throws Exception {
        DatabaseManager manager = new DatabaseManager(null);
        Field connectionField = DatabaseManager.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(manager, connection);
        return manager;
    }

    private HideState state(UUID uuid, String realName, String alias, HideMode mode) {
        return new HideState(
                uuid,
                realName,
                mode,
                alias,
                HideManager.normalize(alias),
                "",
                "",
                mode == HideMode.SCRAMBLE ? "original-texture" : "",
                mode == HideMode.SCRAMBLE ? "original-signature" : "",
                1L,
                2L
        );
    }
}
