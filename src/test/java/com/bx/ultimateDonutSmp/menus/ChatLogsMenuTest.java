package com.bx.ultimateDonutSmp.menus;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLogsMenuTest {

    @Test
    void aShortMessageStaysOnOneLine() {
        assertEquals(java.util.Collections.singletonList("hello everyone"), ChatLogsMenu.wrap("hello everyone"));
    }

    @Test
    void longMessagesAreBrokenOverSeveralLoreLines() {
        String message = "this is a very long chat message that will not fit on a single line of item lore";

        List<String> wrapped = ChatLogsMenu.wrap(message);

        assertTrue(wrapped.size() > 1, "a long message should span more than one line");
        for (String line : wrapped) {
            assertTrue(line.length() <= 40, "line too long: " + line);
        }
        assertEquals(message, String.join(" ", wrapped));
    }

    @Test
    void aSingleUnbrokenWordIsSplitInsteadOfOverflowing() {
        List<String> wrapped = ChatLogsMenu.wrap(repeat("a", 95));

        assertEquals(3, wrapped.size());
        assertEquals(95, String.join("", wrapped).length());
    }

    @Test
    void anEmptyMessageStillProducesALine() {
        assertEquals(java.util.Collections.singletonList(""), ChatLogsMenu.wrap("   "));
        assertEquals(java.util.Collections.singletonList(""), ChatLogsMenu.wrap(null));
    }
    private static String repeat(String unit, int count) {
        StringBuilder builder = new StringBuilder(unit.length() * Math.max(0, count));
        for (int i = 0; i < count; i++) {
            builder.append(unit);
        }
        return builder.toString();
    }
}
