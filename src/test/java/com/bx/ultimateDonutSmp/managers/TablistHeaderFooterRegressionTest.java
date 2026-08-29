package com.bx.ultimateDonutSmp.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for tab menu completeness (bug B3).
 *
 * <p>Original design (verified against the upstream UltimateDonutSMP implementation and this
 * fork's byte-identical TABLIST config section): a multi-line per-player HEADER and FOOTER above
 * and below the player list, plus per-entry display names and skin-head icons. The names and
 * icons survived the 1.12.2 port; the HEADER and FOOTER did not, because the ported call sites
 * used the String-based Bukkit methods (setPlayerListHeader/Footer/HeaderFooter) that were only
 * introduced in LATER APIs. On 1.12.2 the reflective lookup finds nothing and the old code simply
 * returned - so the tab menu lost its entire header/footer while the config still carried it.
 *
 * <p>The fix keeps the API attempts for newer builds and adds the 1.12.2-correct legacy route:
 * the versioned {@code PacketPlayOutPlayerListHeaderFooter} carrying the two
 * {@code IChatBaseComponent} fields, rendered through the same legacy colour pipeline as the
 * tab names (one representation for vanilla 1.12.2 and Eaglercraft alike).
 */
public class TablistHeaderFooterRegressionTest {

    private static final String MAIN_ROOT =
            System.getProperty("basedir", ".") + "/src/main/";
    private static final String MANAGERS = MAIN_ROOT + "java/com/bx/ultimateDonutSmp/managers/";
    private static final String UTILS = MAIN_ROOT + "java/com/bx/ultimateDonutSmp/utils/";

    private static String read(File file) throws Exception {
        assertTrue(file.isFile(), "expected source file " + file);
        try (FileInputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String readStrippingComments(String path) throws Exception {
        String raw = read(new File(path));
        return raw.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }

    /** Original configured content must survive in the shipped seed config, unchanged. */
    @Test
    public void configuredHeaderAndFooterSurviveThePort() throws Exception {
        File config = new File(MAIN_ROOT + "resources/config.yml");
        assertTrue(config.isFile(), "seed config.yml must exist");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(config);

        List<String> header = yaml.getStringList("TABLIST.HEADER");
        List<String> footer = yaml.getStringList("TABLIST.FOOTER");

        assertEquals(Arrays.asList(
                "",
                "<#00ADFC>&lServer Name</#00FCFC>",
                "&f%online% Players",
                ""
        ), header, "TABLIST.HEADER must keep the original four-line layout");
        assertEquals(Arrays.asList(
                "",
                "   &#37BFF9/discord  /guide  /store   ",
                ""
        ), footer, "TABLIST.FOOTER must keep the original links line");

        assertTrue(yaml.getBoolean("TABLIST.ENABLED", false), "TABLIST.ENABLED must stay on");
        assertEquals("<media_badge>&f<nick>%team_suffix%",
                yaml.getString("TABLIST.NAME-FORMAT"),
                "entry name format must keep badge + nick + team suffix");
        assertEquals("<head:%player_name%>",
                yaml.getString("TABLIST.ICON-HEAD-SKIN"),
                "skin-head icon configuration must remain");
    }

    /**
     * The root cause proof: the String-based header/footer methods the modern path uses do not
     * exist in the 1.12.2 API this project compiles against, so a packet fallback is not
     * optional. If a future API adds them, this test fails on purpose and the compatibility
     * layer can be re-evaluated deliberately.
     */
    @Test
    public void legacyApiLacksStringHeaderFooterSoFallbackIsRequired() throws Exception {
        assertThrows(NoSuchMethodException.class,
                () -> Player.class.getMethod("setPlayerListHeaderFooter", String.class, String.class));
        assertThrows(NoSuchMethodException.class,
                () -> Player.class.getMethod("setPlayerListHeader", String.class));
        assertThrows(NoSuchMethodException.class,
                () -> Player.class.getMethod("setPlayerListFooter", String.class));
        // Sanity: the name side of the same API DOES exist (the port's working path), so the
        // negative assertions above measure a real gap and not a broken classpath.
        assertNotNull(Player.class.getMethod("setPlayerListName", String.class),
                "setPlayerListName must exist on the 1.12.2 API");
    }

    /** The manager must keep the API attempts AND wire the legacy packet route as fallback. */
    @Test
    public void managerFallsBackToTheLegacyHeaderFooterPacketRoute() throws Exception {
        String source = readStrippingComments(MANAGERS + "TablistManager.java");
        assertTrue(source.contains("componentUpdater.updateHeaderFooter("),
                "TablistManager.update() must fall back to the legacy packet route when the "
                        + "String-based Bukkit API is missing, or header/footer is lost again");
        assertTrue(source.contains("setHeaderFooterViaBukkitApi("),
                "the modern API attempts must stay (the port keeps dual-layout compatibility)");
    }

    /**
     * The fallback must use the 1.12.2-era packet through the project's reflective NmsSupport
     * architecture - never a linked modern packet class - and must render both sides through the
     * same legacy component pipeline used for display names.
     */
    @Test
    public void updaterUsesTheLegacyPacketThroughNmsSupportWithoutLinkingModernClasses() throws Exception {
        String source = readStrippingComments(UTILS + "TablistComponentUpdater.java");

        // The 1.12.2 packet is targeted by name (resolved via NmsSupport candidates).
        assertTrue(source.contains("\"PacketPlayOutPlayerListHeaderFooter\""),
                "the updater must target the 1.12.2 PacketPlayOutPlayerListHeaderFooter by name");
        assertTrue(source.contains("NmsSupport.candidates(headerFooterPacketClassNames()")
                        || source.contains("headerFooterPacketClassNames()"),
                "packet resolution must go through the NmsSupport candidate architecture");
        assertFalse(source.contains("import net.minecraft."),
                "no NMS class may be linked at compile time (breaks against the 1.12.2 API jar)");

        // Both halves go through the shared component pipeline (toNativeComponent), which is
        // what gives vanilla 1.12.2 and Eaglercraft the identical legacy representation.
        int footerStart = source.indexOf("public boolean updateHeaderFooter(");
        assertTrue(footerStart >= 0, "updateHeaderFooter must be public on the updater");
        String footerMethod = source.substring(footerStart,
                source.indexOf("\n    }", footerStart) > footerStart
                        ? source.indexOf("\n    }", footerStart)
                        : source.length());
        assertTrue(footerMethod.contains("toNativeComponent(header)"),
                "header must be rendered via the shared native component pipeline");
        assertTrue(footerMethod.contains("toNativeComponent(footer)"),
                "footer must be rendered via the shared native component pipeline");
    }

    /**
     * The original header/footer is a per-viewer player-list packet; it must not be entangled
     * with the scoreboard teams the Money Nametag feature uses. Neither changed file may touch
     * teams; the LuckPerms bridge (which re-refreshes the tab continuously) must keep its hands
     * off the header/footer caches entirely so repeated refreshes cannot erase them.
     */
    @Test
    public void tablistRefreshStaysOutOfScoreboardTeamsAndNametagPath() throws Exception {
        String manager = readStrippingComments(MANAGERS + "TablistManager.java");
        assertFalse(manager.contains("Scoreboard") && manager.contains("registerNewTeam"),
                "the tablist must not start managing scoreboard teams (Money Nametag owns those)");
        assertFalse(manager.contains("updateInventory("),
                "the tablist refresh path must never resync inventories (B2 rule)");

        String bridge = readStrippingComments(MANAGERS + "LuckPermsTablistRefreshBridge.java");
        assertFalse(bridge.contains("Header"),
                "the LuckPerms refresh bridge must not touch header/footer state at all");
        assertTrue(bridge.contains("updateTablistName("),
                "the bridge must still refresh the tablist entry it exists for");
    }

    /**
     * Everything the port and these fixes put in these files must stay compilable by javac 8:
     * no records, no var, no Java 11+ String/Stream conveniences, no Java 14+ pattern matching
     * or switch expressions. (The CI compiles with source/target 1.8; this pins it in tests too.)
     */
    @Test
    public void touchedFilesStayJavaEightCompatible() throws Exception {
        String[] files = {
                MANAGERS + "TablistManager.java",
                UTILS + "TablistComponentUpdater.java"
        };
        for (String file : files) {
            String source = readStrippingComments(file);
            String name = file.substring(file.lastIndexOf('/') + 1);
            assertFalse(source.matches("(?s).*(\\b|\\s)record\\s+[A-Z].*"), name + ": no records");
            assertFalse(source.matches("(?s).*(\\b|\\s)var\\s+[a-zA-Z].*"), name + ": no var");
            assertFalse(source.contains(".isBlank("), name + ": isBlank() is Java 11");
            // Stream.toList() itself is Java 16; the Java-8 spelling Collectors.toList()
            // lives inside collect(...) and is preceded by an identifier, not a ')'.
            assertFalse(source.matches("(?s).*\\)\\.toList\\(\\).*"), name + ": Stream.toList() is Java 16");
            assertFalse(source.matches("(?s).*case\\s+[^:\\n]*->.*"), name + ": no arrow switches");
            assertFalse(source.matches("(?s).*instanceof\\s+[A-Z][\\w.]*\\s*[<&]?[\\w>]*\\s+\\w+\\s*\\)"),
                    name + ": no pattern-matching instanceof (Java 16)");
        }
    }
}
