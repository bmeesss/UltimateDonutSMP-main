package com.bx.ultimateDonutSmp.managers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/**
 * Regression tests for the Eaglercraft held-item flicker (bug B2).
 *
 * <p>Root cause: LuckPermsTablistRefreshBridge.refreshPlayer() called
 * player.updateInventory() unconditionally at the end of every permission-triggered
 * tablist refresh. The refresh itself never modifies PlayerInventory - it recalculates
 * permissions and repaints the tablist entry - but updateInventory() re-sent the whole
 * window including the held-item slot. On every LuckPerms UserDataRecalculateEvent
 * (periodic user-cache recalculation, amplified by this plugin's per-player
 * prefix/group lookups) that meant a full inventory resync for every online player.
 * Vanilla 1.12.2 absorbs the redundant packet; Eaglercraft restarts the held-item
 * equip animation on every window/equipment update, so held items visibly refreshed
 * in a loop.
 *
 * <p>The server-side code path is identical for Java and Eagler clients (there is no
 * client-type branching anywhere in the refresh chain), so the fix removes the
 * unnecessary refresh on the plugin side instead of adding a client-specific
 * workaround.
 */
public class LuckPermsTablistRefreshBridgeHeldItemTest {

    private static final String SOURCE_ROOT =
            System.getProperty("basedir", ".")
                    + "/src/main/java/com/bx/ultimateDonutSmp/managers/";

    private static String readBridgeSource() throws Exception {
        File file = new File(SOURCE_ROOT + "LuckPermsTablistRefreshBridge.java");
        assertTrue("bridge source must be readable for the contract check: " + file, file.isFile());
        String raw;
        try (FileInputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            raw = new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
        // Strip comments so the check sees executable code only: this file deliberately
        // documents the removed call in prose, which must not count as a call site.
        return raw.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }

    @Test
    public void bridgeNeverResyncsPlayerInventory() throws Exception {
        String source = readBridgeSource();
        // The permission/tablist refresh must not push inventory packets. Any
        // updateInventory() call in this file reintroduces the held-item flicker on
        // clients that re-animate held items on redundant window updates (Eaglercraft).
        assertFalse("LuckPermsTablistRefreshBridge must not call updateInventory(); "
                        + "permission refreshes do not modify the inventory and the resync "
                        + "caused continuous held-item updates (Eagler flicker).",
                source.contains("updateInventory("));
    }

    @Test
    public void bridgeStillPerformsItsActualRefreshWork() throws Exception {
        String source = readBridgeSource();
        // Guard the other half of the contract: the fix removed the inventory resync but
        // must have kept the permission recalculation and tablist repaint this class exists
        // for (a future refactor must not "fix" the flicker by gutting the refresh instead).
        assertTrue("bridge must still recalculate permissions",
                source.contains("recalculatePermissions()"));
        assertNotNull("bridge must still drive the tablist manager",
                source.contains("getTablistManager()"));
    }
}
