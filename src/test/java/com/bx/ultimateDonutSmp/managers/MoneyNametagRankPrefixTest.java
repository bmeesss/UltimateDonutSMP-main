package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.utils.LegacyScoreboardText;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Team prefix fitting for the rank shown in front of nametags. The 1.12.2 protocol accepts at
 * most 16 raw characters per team part (colour codes count), and a prefix that stops mid-colour
 * would paint the player name in a stray colour, so the helper must truncate on code boundaries
 * and keep the trailing reset.
 *
 * <p>The second half pins the resolution chain: the nametag prefix is produced by the same
 * resolver the tab uses, and that resolver must follow the chat pipeline's source order
 * (LuckPerms metadata, then {@code %luckperms_prefix%}, {@code %vault_prefix%} and
 * {@code %prefix%}, then the Vault chat provider). A server whose ranks come from Vault or the
 * Essentials placeholders must not come out empty-handed - that gap was the exact reason the
 * money tag showed while the rank never did. The nametag side must also re-resolve the prefix
 * on every pass so a rank change lands without a relog, while the per-pass memo keeps the
 * placeholder lookups linear in players instead of quadratic.</p>
 */
class MoneyNametagRankPrefixTest {

    @Test
    void shortPrefixIsReturnedUnchanged() {
        assertEquals("", MoneyNametagManager.fitTeamPrefix(null));
        assertEquals("", MoneyNametagManager.fitTeamPrefix(""));
        assertEquals("\u00A7c[Owner]\u00A7r", MoneyNametagManager.fitTeamPrefix("\u00A7c[Owner]"));
    }

    @Test
    void longPrefixIsCutToTheTeamPartBudget() {
        String fitted = MoneyNametagManager.fitTeamPrefix("\u00A7c[VeryLongRankNameThatNeverFits]");
        assertTrue(fitted.length() <= LegacyScoreboardText.MAX_TEAM_PART_LENGTH,
                "prefix must respect the 16-char team-part limit, got " + fitted.length());
        assertTrue(fitted.endsWith("\u00A7r"), "the name after it must start from a reset colour");
        assertTrue(fitted.startsWith("\u00A7c[VeryLong"), "the prefix keeps its head, got " + fitted);
        assertEquals(-1, indexOfBrokenCode(fitted), "no half-written colour sequence may survive");
    }

    /** A dangling section sign at the very end would be the broken-code case. */
    private static int indexOfBrokenCode(String value) {
        return value.endsWith("\u00A7") ? 0 : -1;
    }

    // ---------------------------------------------------------------- fitting behaviour

    @Test
    void resetAlreadyPresentIsNotDoubled() {
        assertEquals("\u00A7c[VIP]\u00A7r", MoneyNametagManager.fitTeamPrefix("\u00A7c[VIP]\u00A7r"));
    }

    @Test
    void hexPrefixCollapsesToLegacyWithoutColourBleed() {
        // What ColorUtils produces for a hex rank icon: a full §x sequence plus the glyph.
        // The team fields may only carry plain legacy codes, the icon must survive, and the
        // name that follows must never inherit the rank colour.
        String fitted = MoneyNametagManager.fitTeamPrefix("\u00A7x\u00A70\u00A70\u00A7f\u00A7c\u00A7f\u00A7c\u2605 \u00A7r");
        assertFalse(fitted.contains("\u00A7x"), "no §x sequences may reach a 1.12.2 team field: " + fitted);
        assertTrue(fitted.length() <= LegacyScoreboardText.MAX_TEAM_PART_LENGTH);
        assertEquals("\u2605 ", fitted.replaceAll("\u00A7.", ""), "the BMP icon and its space survive");
        assertTrue(fitted.endsWith("\u00A7r"));
        assertFalse(fitted.endsWith("\u00A7r\u00A7r"), "the reset must not be doubled");
    }

    @Test
    void truncationNeverSplitsSurrogatePairsOrCodes() {
        StringBuilder rank = new StringBuilder("\u00A7d");
        for (int i = 0; i < 8; i++) {
            rank.append("\uD83D\uDC51"); // crown emoji, one codepoint, two UTF-16 units
        }
        String fitted = MoneyNametagManager.fitTeamPrefix(rank.toString());
        assertTrue(fitted.length() <= LegacyScoreboardText.MAX_TEAM_PART_LENGTH,
                "team part budget, got " + fitted.length());
        assertTrue(fitted.startsWith("\u00A7d"));
        assertEquals(-1, indexOfBrokenCode(fitted), "no dangling colour code");
        assertFalse(fitted.length() > 0
                        && Character.isHighSurrogate(fitted.charAt(fitted.length() - 1)),
                "no half surrogate pair may end the prefix: " + fitted);
    }

    // ---------------------------------------------------------------- resolution chain

    private static String sourceOf(String relative) throws Exception {
        Path file = Paths.get("src/main/java/com/bx/ultimateDonutSmp", relative);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    /** The body of one exactly-indented method, from its signature to its closing brace. */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, signature + " expected");
        int end = source.indexOf("\n    }", start);
        assertTrue(end > start, signature + " must close at the standard indent");
        return source.substring(start, end);
    }

    @Test
    void tabResolverFollowsTheSameChainAsChat() throws Exception {
        String tablist = sourceOf("managers/TablistManager.java");
        String resolve = methodBody(tablist, "private String resolvePrefix(Player player) {");

        // Source order must match the chat resolver: LuckPerms meta first, then the PAPI tiers
        // in chat's order, then the Vault chat provider as last resort.
        int luckperms = resolve.indexOf("%luckperms_prefix%");
        int vault = resolve.indexOf("%vault_prefix%");
        int prefix = resolve.indexOf("\"%prefix%\"");
        int vaultChat = resolve.indexOf("getPlayerPrefix");
        assertTrue(0 <= luckperms && luckperms < vault, "vault_prefix comes after luckperms_prefix");
        assertTrue(vault < prefix, "%prefix% comes after %vault_prefix%");
        assertTrue(prefix < vaultChat, "the Vault chat provider is the final tier");

        // Same guards as chat: unresolved placeholders ("" / whitespace / literal %name%) are
        // skipped, and the Vault class is only touched when the plugin is actually enabled.
        assertTrue(resolve.contains("resolveLuckPermsPrefix(player)"), "LuckPerms metadata stays first");
        assertTrue(resolve.contains("startsWith(\"%\")"), "unresolved placeholders must not be shown");
        assertTrue(resolve.indexOf("isPluginEnabled(\"Vault\")") < resolve.indexOf("getRegistration"),
                "the Vault tier must be gated so the class never loads without Vault");

        // Parity with the chat resolver's tiers.
        String chat = methodBody(sourceOf("listeners/ChatListener.java"),
                "private String resolvePrefix(Player player) {");
        assertTrue(chat.indexOf("%luckperms_prefix%") < chat.indexOf("%vault_prefix%"));
        assertTrue(chat.indexOf("%vault_prefix%") < chat.indexOf("\"%prefix%\""));
        assertTrue(chat.indexOf("\"%prefix%\"") < chat.indexOf("getPlayerPrefix"));
    }

    @Test
    void resolverStaysLegacyAndJavaEight() throws Exception {
        String tablist = sourceOf("managers/TablistManager.java");
        String resolve = methodBody(tablist, "private String resolvePrefix(Player player) {");
        // The nametag pipeline never leaves legacy text; the resolver may not pull Adventure or
        // MiniMessage formatting in, and must stay on Java 8 syntax.
        assertFalse(resolve.contains("Component"), "no Adventure components in the prefix resolver");
        assertFalse(resolve.contains("MiniMessage"), "no MiniMessage in the prefix resolver");
        assertFalse(resolve.contains("var "), "Java 8 source level");
        assertFalse(resolve.contains("List.of("), "Java 8 source level");
        assertFalse(resolve.contains("net.kyori"), "no Adventure imports in the prefix path");
    }

    // ---------------------------------------------------------------- nametag side wiring

    @Test
    void rankPrefixIsReResolvedEveryPassWithOneLookupPerTarget() throws Exception {
        String manager = sourceOf("managers/MoneyNametagManager.java");

        // Push diff-checks against the tracked text, so the prefix must be computed BEFORE the
        // currentlyShows check: that is what makes a rank change rewrite the team without any
        // relog, on the next pass.
        String push = methodBody(manager, "private void push(Player viewer) {");
        assertTrue(push.indexOf("rankPrefixFor(target)") < push.indexOf("currentlyShows"),
                "the prefix must be re-resolved before the diff check");

        // Per pass, not per viewer x target: the memo is cleared at the top of updateAll, and
        // dropped for a target on quit and on full clear, so it can never go stale beyond one
        // interval.
        assertTrue(methodBody(manager, "public void updateAll() {")
                .contains("rankPrefixMemo.clear()"), "memo must be per-pass");
        assertTrue(methodBody(manager, "public void remove(UUID playerUuid) {")
                .contains("rankPrefixMemo.remove(playerUuid)"), "quit must drop the memo entry");
        assertTrue(methodBody(manager, "public void clearAll() {")
                .contains("rankPrefixMemo.clear()"), "disable must drop the memo");
        String lookup = methodBody(manager, "private String rankPrefixFor(Player target) {");
        assertTrue(lookup.contains("rankPrefixMemo.get(") && lookup.contains("rankPrefixMemo.put("),
                "the memo must be read and filled in rankPrefixFor");
        assertTrue(methodBody(manager, "private String resolveRankPrefix(Player target) {")
                .contains("fitTeamPrefix(ColorUtils.colorize("), "raw prefix must go through the fitter");
    }

    @Test
    void prefixAndSuffixAreWrittenToTheSameTeamInOneRewrite() throws Exception {
        String push = methodBody(sourceOf("managers/MoneyNametagManager.java"),
                "private void push(Player viewer) {");
        assertTrue(push.contains("String memberName = target.getName();"),
                "the team entry is the plain player name - a long rank never shortens it");
        assertTrue(push.contains("team.setPrefix(prefix);"), "rank goes to the prefix field");
        assertTrue(push.contains("team.setSuffix(text);"), "money goes to the suffix field");
        // The diff key carries both fields separated by the sentinel, so neither a rank change
        // nor a balance change can ever be "already shown" for the other field.
        assertTrue(push.contains("prefix + \"\\u0000\" + text"),
                "tracked state must combine prefix and suffix");
    }

    @Test
    void balanceMarkForcesAFullBothFieldsRewrite() {
        // state.markChanged only blanks the tracked text; the next pass then rewrites the team
        // from scratch, which re-applies the rank prefix alongside the new balance. A balance
        // update can therefore never erase the rank, and vice versa.
        MoneyNametagState state = new MoneyNametagState();
        java.util.UUID viewer = new java.util.UUID(1L, 1L);
        java.util.UUID target = new java.util.UUID(2L, 2L);
        MoneyNametagState.Entry entry = state.entryFor(viewer, target);
        entry.text = "\u00A7d[R]\u0000 \u00A7a$1K";
        entry.memberName = "Steve";
        assertTrue(entry.currentlyShows("\u00A7d[R]\u0000 \u00A7a$1K", "Steve"));
        state.markChanged(target);
        assertFalse(entry.currentlyShows("\u00A7d[R]\u0000 \u00A7a$1K", "Steve"),
                "a marked change must force the next pass to rewrite prefix and suffix together");
    }

    @Test
    void nametagTeamsNeverCollideWithSidebarTeams() throws Exception {
        // Two systems share the viewer's board: sidebar lines use "sb_" teams, nametags use
        // "udsm" teams and cleanup only ever unregisters its own namespace.
        String manager = sourceOf("managers/MoneyNametagManager.java");
        String sidebar = sourceOf("managers/ScoreboardManager.java");
        assertTrue(manager.contains("TEAM_PREFIX = \"udsm\""), "nametag namespace");
        assertTrue(sidebar.contains("registerNewTeam(\"sb_\" + i)"), "sidebar namespace");
        String cleanup = methodBody(manager, "private void clearViewerState(UUID uuid) {");
        assertTrue(cleanup.indexOf("isOurTeam(team.getName())") < cleanup.indexOf("team.unregister()"),
                "cleanup must filter by namespace before unregistering");
        // And the pure namespace rules hold under the protocol limit for many concurrent targets.
        MoneyNametagState state = new MoneyNametagState();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            String name = state.teamNameFor(new java.util.UUID(i + 1, i + 7L));
            assertTrue(name.startsWith("udsm") && !name.startsWith("sb_"), "namespace: " + name);
            assertTrue(name.length() <= MoneyNametagState.MAX_TEAM_NAME_LENGTH,
                    "16-char team name limit exceeded: " + name);
            assertTrue(names.add(name), "team names must be unique per target: " + name);
        }
    }
}
