# UltimateDonutSMP — Spigot 1.12.2 / Java 8 Migration Status

Target (from `pom.xml`): Java 8 (`maven.compiler.source/target = 1.8`), Spigot API `1.12.2-R0.1-SNAPSHOT`,
ProtocolLib 5.3.0 (provided), Adventure MiniMessage 4.14.0 (shaded), Vault/LuckPerms/PlaceholderAPI (provided),
SQLite/MySQL/HikariCP (bundled).

Source files: **422 src/main + 69 src/test** (422 since Batch 36A added `utils/LegacyMaterialSupport.java`).

## Phase overview

| Phase | Status |
|---|---|
| Structural repair / parser cleanup | ✅ **COMPLETE** |
| Baseline recovery | ✅ **RECOVERED from master `138898fb`** (corrupt migration commit `b2e898b` discarded as base) |
| Java 8 — Batch 1 | ✅ **COMPLETE / MERGED** (PR #10, commit `d004861`) |
| Java 8 — Batch 2 | ✅ **COMPLETE / MERGED** (PR #11) |
| Java 8 — Batch 3 | ✅ **COMPLETE / MERGED** (PR #11) |
| Java 8 — Batches 4–8 | ✅ **COMPLETE / MERGED** (PR #12, commit `1c288fb`) |
| Java 8 — Batches 9–11 | ✅ **COMPLETE / MERGED** (PR #13, commit `5eb81a0`) |
| Java 8 — Batch 12 | ✅ **COMPLETE** (master checkpoint `138898fb`) |
| Java 8 — Batch 13 (javac-invalid corruption, 6 files) | ✅ **COMPLETE / MERGED** (master `ae4755b`) |
| Java 8 — Batch 14 (10 files, 27 instanceof) | ✅ **COMPLETE / MERGED** (PR #16, master `476cd17`) |
| Java 8 — Batch 15 (10 files, 17 instanceof) | ✅ **COMPLETE / MERGED** (PR #16, master `476cd17`) |
| Batch 16 — javac-invalid corruption cleanup (FriendsCommand, TPACommand, SellCommand) | ✅ **COMPLETE / MERGED** (PR #16, master `476cd17`) — **0 markers remain repo-wide** |
| Java 8 — Batch 17 (10 files, 10 instanceof) | ✅ **COMPLETE / MERGED** (master `8f5c1f4`) |
| Java 8 — Batch 18 (4 files, 5 instanceof) | ✅ **COMPLETE / MERGED** (master `8f5c1f4`) — **non-deferred A+B backlog now 0** |
| Java 8 — Batch 19 (5 deferred files: 2× `failedFuture`, 2× `String.repeat`, 1× `InputStream.readAllBytes`, 4× `var`, 3× instanceof) | ✅ **COMPLETE / MERGED** (PR #18, master `fd35a5e`) |
| Java 8 — Batch 20 (text blocks: 4 files / 13 blocks → Java 8 concatenated literals) | ✅ **COMPLETE** (this checkpoint, against master `fd35a5e`) — 13/13 runtime contents byte-identical, **text blocks now 0** |
| Java 8 — Batch 21 (`SellStatsExporter` single-file complex migration) | ✅ **COMPLETE** (this checkpoint, against master `1373e84`) — unused `java.net.http` imports removed, virtual-thread executor → `newCachedThreadPool()`, `StringBuilder.isEmpty()` → `length()`, instanceof pattern → classic cast, `FileWriter(File, Charset)` → `OutputStreamWriter`+`FileOutputStream` |
| Java 8 — Batch 22 (final instanceof conversions: `DatabaseManager` ×6, `ShardManager` ×1) | ✅ **COMPLETE** (this checkpoint, against master `68affd4`) — **instanceof patterns now 0 repo-wide** |
| Java 8 — Batch 23 (`DatabaseManager` undefined-symbol repair + `StatsWipeManager` `EnumSet` repair) | ✅ **COMPLETE** (this checkpoint) — javac-invalid `stringValue` branch and `Enumjava…` type both repaired |
| Java 8 — Batch 24 (primitive-null return cleanup, 7 sites / 4 files) | ✅ **COMPLETE** (this checkpoint) — **`null`-from-primitive returns now 0 repo-wide** |
| Java 8 — Batch 25 (final switch expressions ×3 + `RandomGenerator` bounded-long migration) | ✅ **COMPLETE** (this checkpoint) — **switch expressions 0, `RandomGenerator` 0, bounded `nextLong(origin,bound)` 0** |
| **Java 8 migration phase** | ✅ **STATICALLY COMPLETE** — 0 genuine Java 8 blockers per the validated scanner; **real javac/Maven verification remains** |
| Spigot 1.12.2 API — Batch 26 (Materials + `Material.isAir()`, 10 files) | ✅ **COMPLETE / MERGED** (PR #22, master `f18b55d`) — Category C **STARTED** |
| Spigot 1.12.2 API — Batch 27 (Simple Materials, 10 files: `CLOCK`, `SPAWNER`, `GRASS_BLOCK`, `WRITABLE_BOOK`, `EXPERIENCE_BOTTLE`, `ENDER_EYE`, `RED_DYE`) | ✅ **COMPLETE** (this checkpoint) |
| Spigot 1.12.2 API — Batch 28 (stained-glass panes, 10 files) | ✅ **COMPLETE** (this checkpoint) |
| Spigot 1.12.2 API — Batch 29 (dyes → `INK_SACK` + data, 5 files) | ✅ **COMPLETE** (this checkpoint) |
| Spigot 1.12.2 API — Batch 30 (stained-glass panes part 2, 10 files) | ✅ **COMPLETE / MERGED** (PR #26, master `68ab746`) |
| Spigot 1.12.2 API — Batch 31 (direct pane ItemStack sites, 10 files / 10 sites) | ✅ **COMPLETE** (this checkpoint) |
| Spigot 1.12.2 API — Batch 32 (direct pane fill sites part 2, 10 files / 10 sites) | ✅ **COMPLETE** (this checkpoint) |
| Spigot 1.12.2 API — Batch 33 (direct pane fill sites part 3, 10 files / 10 sites) | ✅ **COMPLETE** (this checkpoint) |
| Spigot 1.12.2 API — Batch 34 (direct pane fill sites part 4, 9 files / 9 sites) | ✅ **COMPLETE / MERGED** (PR #30, master `6515b49`) |
| Spigot 1.12.2 API — Batch 35 (config-driven pane helper investigation) | ✅ **COMPLETE — analysis only, 0 source edits** (proved the sites are config-coupled; corrected the inventory to 16 sites / 17 arguments) |
| Spigot 1.12.2 API — Batch 36A (central legacy material compatibility layer) | ✅ **COMPLETE** (1 new file, `utils/LegacyMaterialSupport.java`, no call-site churn) |
| Spigot 1.12.2 API — Batch 36B (config-driven pane helper migration) | ✅ **COMPLETE** (10 files; all 17 pane arguments migrated through the layer) |
| Spigot 1.12.2 API — Batch 37 (pane-safe Material serialization write-back) | ✅ **COMPLETE** (3 files, 4 write sites; config format unchanged — flattened pane aliases) |
| Spigot 1.12.2 API migration (remaining Materials / BlockData / PDC / Particle / Sound / entities) | 🚧 **IN PROGRESS** (Batches 26–37; write-back serialization done, remaining pane work = the 47 config/default refs + their read-path parsers) |
| NMS / ProtocolLib runtime audit | ⛔ **NOT STARTED** |
| Adventure runtime compatibility | ⛔ **NOT STARTED** |

## Structural health (verified this checkpoint)

**Baseline:** recovered from GitHub master `138898fb3fd94b78665f82e71129f5d09546d2cf` (tree `ef615ae1`).
The earlier local migration commit `b2e898b` ("…186 files") had introduced **34** tree-sitter-ERROR files and
was **discarded as the migration base**; master `138898fb` parses 421/421 clean.

- **421/421** `src/main` files parse clean (tree-sitter-java 0.23.5 via tree-sitter 0.26.0, **explicit
  large buffer** — the native default buffer silently throws on large files; a scanner that treats a throw
  as "pass" is a false negative)
- **0** delimiter imbalance (brace / paren / bracket, comment- and string-aware)
- **0** true duplicate methods/constructors/fields (same name **and** full parameter-type signature,
  scoped per declaring type; a file-wide tripwire scan is unchanged from the pre-batch baseline)
- **0** javac-invalid corruption markers repo-wide
- `git diff --check` clean
- Batch 13 removed the 6 javac-invalid corruption artifacts (tree-sitter-tolerated, javac-invalid) — see below

**Batch 19 baseline:** the branch was found checked out on the stale intermediate commit `b2e898b`
(125,411 `src/main` lines, duplicated method declarations, tree-sitter ERRORs). The 5 pending edits were
backed up outside the repository, the branch was reset to the verified `origin/master`
`8f5c1f4ab44f5aa52e9780db43659410976ecab6`, and every fix was re-derived against that clean tree.

**Batch 20 baseline/validation:** work was performed directly on the verified `origin/master`
`fd35a5e7d68b520728fcbea0133d9481ac45dde7` (Batch 19 merge) — re-verified this checkpoint:
**421/421** `src/main` tree-sitter parse clean (large-buffer), **0** ERROR/MISSING nodes in all 4
changed files, **0** delimiter imbalance, **0** type-scoped duplicate declarations, **0** javac-invalid
markers repo-wide, `git diff --check` clean, all 8 restricted diff hunks contained strictly inside the 13
original text-block spans (no non-text-block source edits), Category C markers byte-identical vs `HEAD`,
no test changes.

`src/test` has 3 pre-existing parse failures inherited from before the Java 8 phase; no test file has been
modified in Batches 1–19.

---

## Batch 13 — javac-invalid corruption cleanup (COMPLETE, this checkpoint)

6 files repaired on the recovered master baseline; each repair restores the unambiguous control flow /
declaration the corruption overwrote. No Materials / Bukkit / NMS / ProtocolLib / Adventure / test changes.

| File | Repair |
|---|---|
| `commands/AuctionHouseCommand.java` | 7 switch blocks each had ~30 unreachable `break;` statements (javac §14.21) → collapsed to the single intended `break;`, indentation restored; orphaned `String;` in the cancel-failure resolver → `String key = null;` |
| `menus/SellGui.java` | orphaned `String;` in create-listing-failure resolver → `String key = null;` |
| `commands/SocialCommand.java` | orphaned `String;` before label resolver → `String key;` (all branches assign, incl. `default → null`) |
| `managers/LuckPermsTablistRefreshBridge.java` | orphaned `Boolean;` → `Boolean value;` (null-guarded before unboxing to the primitive `boolean` API) |
| `managers/KeyAllManager.java` | dangling ternary statement + `return break;` → `return reward != null ? reward : loadOneKeyOnlyReward();` |
| `commands/CuboidCommand.java` | `break; break;` (spawn/shard cases) → single `break;`; `return; break; break;` (default) → `return;`; indentation restored |

**Build not verified — Maven/JDK unavailable.**

---

## Completed batches

### Batch 1 — merged in PR #10 (43 files)
`var` → explicit types and `instanceof` pattern matching → explicit casts, plus removal of duplicated/corrupted
blocks left over from the structural phase.

### Batch 2 — 9 files, 50 occurrences
| File | Fixes |
|---|---|
| `menus/ServerInfoMenu.java` | 17× `List.of(...)` → `Arrays.asList(...)`, 2× `StringBuilder.isEmpty()` |
| `commands/GamemodeCommand.java` | 6× instanceof pattern |
| `managers/FreezeManager.java` | 6× instanceof pattern |
| `commands/UltimateDonutSmpCommand.java` | 4× instanceof pattern |
| `listeners/ChatListener.java` | 3× `var` → `BaseComponent[]` |
| `managers/BountyManager.java` | 3× `var` → `HideState` / `EconomyTransactionResult` |
| `managers/ServerWipeManager.java` | 3× `Path.of(...)` → `Paths.get(...)` |
| `commands/FindPlayerCommand.java` | 2× `StringBuilder.isEmpty()`, 1× instanceof pattern |
| `commands/ReportCommand.java` | 1× `StringBuilder.isEmpty()`, 2× instanceof pattern |

### Batch 3 — 10 files, 70 occurrences
| File | Fixes |
|---|---|
| `managers/TablistManager.java` | 13× instanceof pattern |
| `utils/TablistComponentUpdater.java` | 11× instanceof pattern, 1× `List.getFirst()` |
| `commands/CrateCommand.java` | 7× instanceof pattern, 3× `var` |
| `managers/ShopManager.java` | 2× instanceof pattern, 7× `var` |
| `managers/SpawnerManager.java` | 3× instanceof pattern, 1× `var`, 1× `StringBuilder.isEmpty()`, 1× `Objects.requireNonNullElse()`, 1× `List.getFirst()` |
| `managers/StaffModeManager.java` | 5× instanceof pattern |
| `amethyst/AmethystToolsManager.java` | 4× instanceof pattern |
| `amethyst/AmethystToolsListener.java` | 4× instanceof pattern |
| `commands/UniversalCommandTabCompleter.java` | 4× instanceof pattern |
| `managers/KeyAllManager.java` | 2× `List.getFirst()` |

### Batch 4 — 10 files, 15 occurrences
`toArray(T[]::new)` → `toArray(new T[0])` · `StringBuilder.isEmpty()` → `length() == 0` ·
`String.repeat` → loop · `RandomGenerator` → `java.util.Random` · `List.getFirst()` → `get(0)` ·
one `instanceof Number` ternary.

Files: `AFKManager`, `ItemDropListener`, `TeleportManager`, `HoverStatsManager`, `SellStatsCommand`,
`RulesMenu`, `SellHistoryMenu`, `SellProgressMenu`, `HideIdentityPolicy`, `ServerWipeManager`.

### Batch 5 — 10 files, 10 occurrences
`sender instanceof Player player` → classic `instanceof` + explicit cast after the guard.

Files: `NightVisionCommand`, `PhantomCommand`, `TPAutoCommand`, `KillCommand`, `StaffListCommand`,
`TPAHereAutoCommand`, `RandomTeleportCommand`, `VanishCommand`, `ProfileViewerCommand`, `RulesCommand`.

### Batch 6 — 10 files, 10 occurrences
Same instanceof conversion, including `IgnoreTabCompleter` (`||` short-circuit preserved) and
`PingCommand` (cast scoped inside `args.length == 0`).

Files: `SetAfkCommand`, `SetSpawnCommand`, `IgnoreTabCompleter`, `PayCommand`, `ShardPayCommand`,
`PunishmentHistoryCommand`, `FakePlayerCommand`, `SocialCommand`, `PingCommand`, `HelpCommand`.

### Batch 7 — 10 files, 13 occurrences
`var` → types from real declarations (`EconomyManager.AccountReference`, `EconomyTransactionResult`,
`EconomyTransferResult`, `AmethystToolsManager`, `OrderUiState`, `SpawnerManager.ActionResult` /
`SellLootResult`) · one `StringBuilder.isEmpty()` · one `String.repeat`.

Files: `SpawnerInteractListener`, `AddMoneyCommand`, `SetMoneyCommand`, `RemoveMoneyCommand`,
`AmethystToolCommand`, `PaymentUtils`, `OrdersMyOrdersMenu`, `SpawnerSellConfirmMenu`, `WorthMenu`,
`CurrencyManager`.

### Batch 8 — 10 files, 10 occurrences
`sender instanceof Player` / `Player viewer` → classic `instanceof` + explicit cast after the guard.
`StatsCommand` cast stays inside `args.length == 0`.

Files: `HelpopCommand`, `SpawnCommand`, `LogsCommand`, `ShopCommand`, `AFKCommand`,
`FriendsTabCompleter`, `EnderChestCommand`, `PrivateMessageToggleCommand`, `StatsCommand`, `WarpCommand`.

Conversion rules applied in Batches 2–8: short-circuit order and control flow preserved; bindings re-declared
after the guard so scope is unchanged; where the `instanceof` operand was a snapshot-producing call
(`block.getState()`, `bsm.getBlockState()`, `item.getItemMeta()`, `event.getWhoClicked()`, `event.getEntity()`)
a local now holds the **single** invocation instead of the cast re-invoking it. Batches 4–8 did not edit
Category C APIs.

Batches 4–8 together: **50 `src/main` files**, **58 Java 8 A+B issues removed** (227 → 169).

### Batch 9 — 10 files, 22 occurrences
instanceof pattern matching → classic `instanceof` + explicit cast · `var` → exact types ·
`StringBuilder.isEmpty()` → `length()` · `" ".repeat(n)` → `StringBuilder` loop.

| File | Fixes |
|---|---|
| `commands/FlyCommand.java` | 3× instanceof (including `&&` / `||` short-circuit) |
| `commands/StaffModeCommand.java` | 3× instanceof |
| `commands/MessageCommand.java` | 2× instanceof |
| `commands/PlaytimeCommand.java` | 2× instanceof |
| `commands/AltsCommand.java` | 1× instanceof (`&&` short-circuit kept) |
| `commands/BalanceCommand.java` | 1× instanceof + 1× `var` → `EconomyManager.AccountReference` |
| `commands/CuboidCommand.java` | 1× instanceof + 2× `var` → `Map<Integer, ItemStack>` / `CuboidManager.Cuboid` |
| `commands/PunishmentCommand.java` | 2× `StringBuilder.isEmpty()` |
| `menus/CrateEditorMenu.java` | 1× instanceof (`getWhoClicked()` snapshotted) + 1× `StringBuilder.isEmpty()` |
| `managers/ScoreboardManager.java` | 2× `" ".repeat(n)` |

### Batch 10 — 10 files, 11 occurrences
Simple `if (!(sender instanceof Player …))` command guards → classic `instanceof` + cast after the guard.

Files: `InvseeCommand`, `ShopEditCommand`, `OrdersCommand`, `ChatLogCommand`, `EcseeCommand`,
`RenameCommand`, `BountyCommand`, `WorthCommand` (2), `SellCommand`, `IgnoreCommand`.

### Batch 11 — 10 files, 12 occurrences
Remaining practical `StringBuilder.isEmpty()` plus safe `var` / instanceof.

| File | Fixes |
|---|---|
| `managers/WorthManager.java` | 1× `StringBuilder.isEmpty()` |
| `menus/SellStatsAdminMenu.java` | 1× `StringBuilder.isEmpty()` |
| `managers/AntiEspManager.java` | 2× `var` → `FileConfiguration` / `List<SpawnerInstance>` |
| `menus/SpawnerMainMenu.java` | 2× `var` → `SpawnerManager.ActionResult` |
| `menus/CrateRewardMenu.java` | 1× `var` → `ItemMeta` |
| `listeners/CrateChestListener.java` | 1× `var` → `InventoryHolder` |
| `listeners/PhantomListener.java` | 1× instanceof (`getTarget()` snapshotted) |
| `listeners/InvseeListener.java` | 1× instanceof (`getPlayer()` snapshotted) |
| `listeners/PlayerSettingEffectsListener.java` | 1× instanceof (`getEntity()` snapshotted) |
| `managers/IgnoreManager.java` | 1× instanceof |

Conversion rules applied in Batches 9–11 match Batches 2–8. Deferred files (`DatabaseManager`,
`OrdersManager`, repositories, `SellStatsExporter`, `ConfigManager`, `LeaderboardManager`,
`TpaQueueMenu`) and `ShardManager` RandomGenerator were not edited. Batches 9–11 did not edit
Category C APIs.

Batches 9–11 together: **30 `src/main` files**, **45 Java 8 A+B issues removed** (169 → 124).

### Batch 12 — 10 files, 21 Java 8 A+B issues + 7 corruption repairs (this checkpoint)

`var` → exact types resolved from real declarations · instanceof pattern matching → classic
`instanceof` + explicit cast (snapshot getters invoked exactly once; negated guards keep the
binding declared after the guard; `&&` short-circuit order preserved) · plus repair of 7
**pre-existing javac-invalid corruption artifacts** inside the selected files (orphaned
`String;` / `Material;` declarations left from mangled switch-expression rewrites, one
`return break;`, one unreachable `break;` after `return;`) — each repaired to the unambiguous
Java 8 equivalent of the original construct.

| File | Fixes |
|---|---|
| `UltimateDonutSmp.java` | 2× `var` → `ConsoleCommandSender` / `java.util.List<String>` |
| `listeners/PlayerJoinQuitListener.java` | 1× `var` → `HideState` |
| `managers/HideProtocolLibBridge.java` | 1× `var` → `StructureModifier<Collection>`, 1× instanceof (local operand) |
| `menus/AuctionHouseBrowseMenu.java` | 1× `var` → `ConfigurationSection`, 1× orphaned `String;` repair |
| `menus/BillfordMenu.java` | 1× `var` → `EconomyTransactionResult` |
| `menus/PlayerAuctionGui.java` | 1× `var` → `AuctionBrowseRequest`, 3× instanceof, 2× orphaned `String;` repair |
| `menus/PlayerSettingsMenu.java` | 1× `var` → `Team`, 1× instanceof (`getItemMeta()` snapshotted), 1× unreachable `break;` removed |
| `menus/SpawnerStorageMenu.java` | 2× `var` → `ItemMeta`, 2× instanceof (`getWhoClicked()` snapshotted) |
| `menus/StatsMenu.java` | 1× `var` → `Team`, 1× instanceof, 1× `return break;` repair |
| `utils/ItemUtils.java` | 2× instanceof, 1× orphaned `Material;` + 1× orphaned `String;` repair |

`ShardManager` RandomGenerator was evaluated and **kept deferred**: `RandomGenerator.nextLong(origin, bound)`
is Java 17+; `java.util.Random` has no bounded `nextLong` in Java 8, so conversion is not behavior-trivial.
Category C APIs untouched; no tests modified. Batches 1–12: **124 → 103** remaining Java 8 A+B occurrences.

---

### Batch 14 — 10 files, 27 instanceof patterns (this checkpoint)

Fresh AST inventory on master `ae4755b` measured **83 A+B issues / 44 src/main files** (earlier scanner
figures included false positives from plain colon-form switch statements, project `ColorUtils.strip(...)`
and Java 7 `Files.readAllBytes` — all excluded by the corrected type-aware scanner). All non-deferred
issues were instanceof patterns.

Files: `CrateVisualManager` (5), `CrashProtectionManager` (3, `getItemMeta()` snapshots invoked exactly
once), `OptimizationManager` (3, primitive-array patterns with fall-through preserved), `SpawnStashManager`
(3), `WorthDisplayListener` (3), `AuctionHouseCommand` (2), `SafetyCommand` (2), `ExplosionDamageListener`
(2), `ClearLagManager` (2), `PlayerRespawnListener` (2). **83 -> 56** remaining.

### Batch 15 — 10 files, 17 instanceof patterns (this checkpoint)

Files: `LuckPermsTablistRefreshBridge` (3), `NativeGameProfileFactory` (3), `RTPManager` (2),
`SpawnStashCommand` (2), `ItemSerializationUtils` (2), `TeamCommand`, `TeleportCommand`,
`WarpManagerCommand`, `HomeManager`, `PortalManager` (1 each). `FriendsCommand` and `TPACommand` were
originally selected, found to contain the known unreachable-`break;` corruption, **reverted untouched**
and re-routed to Batch 16; `HomeManager`/`PortalManager` substituted. **56 -> 39** remaining.

Conversion rules in Batches 14–15 match Batches 2–11 (short-circuit order, scope, single evaluation of
snapshot getters, null semantics all preserved). Category C untouched; no tests modified.

### Batch 16 — javac-invalid corruption cleanup (COMPLETE, this checkpoint)

The three deferred unreachable-`break;` files are repaired; a repo-wide comment/string-aware scan
(`return break;`, duplicate-`break;` runs, orphaned `String;`/`Boolean;`-style declarations) now reports
**0 markers in all 421 src/main files**.

| File | Repair |
|---|---|
| `commands/FriendsCommand.java` | 5 runs of 31 consecutive `break;` (150 unreachable removed, 1 reachable kept per case) in `reload` / add+follow / remove+unfollow / `search` / `default`; indentation-mangled case bodies reconstructed — behavior unchanged |
| `commands/TPACommand.java` | duplicate unreachable `break;` removed in `tpacancel` and `default` |
| `commands/SellCommand.java` | same confirmed pattern found by the repo-wide scan: 3 runs of 31 `break;` (90 unreachable removed) in `sellprogress` / `sellhand` / `sellhistory`; mangled indentation reconstructed |

No blind removal: each switch was verified flat (no nested switch/loop), so exactly the first `break;`
of each run is reachable; all 12 legitimate single `break;` statements preserved.

### Batch 17 — 10 files, 10 instanceof patterns (this checkpoint)

Baseline: master `476cd17` (PR #16 merge). A fresh type-aware verification pass first **eliminated three
phantom categories** reported by the previous scanner: `Stream.toList()` "78" were all
`Collectors.toList()` (already Java 8), `String.strip()` "46" were all the project's own static
`ColorUtils.strip(...)`, and the one `String.lines()` is `SidebarSettings.lines()` (custom type). True
pre-batch inventory: **68 scanner hits / 22 files**, only actionable non-deferred category = instanceof.

Files: `FriendsCommand`, `TPACommand`, `MobSpawnListener`, `FastCrystalListener`, `PlayerDeathListener`,
`BountyConfirmMenu`, `PayConfirmMenu` (both: `getItemMeta()` snapshotted into a single `ItemMeta` local),
`SellMenu`, `OrdersInventoryItemMenu`, `ShopEditorMenu` (all three: `getWhoClicked()` guard + cast).
instanceof 24 → 14.

### Batch 18 — 4 files, 5 instanceof patterns (this checkpoint)

The 9 other remaining sites are all inside deferred files (`DatabaseManager` ×6, `OrdersManager` ×2,
`SellStatsExporter` ×1). Converted the 4 non-deferred files:

| File | Fixes |
|---|---|
| `commands/PortalManagerCommand.java` | 1× negated guard → cast after guard |
| `menus/CrateGachaMenu.java` | 1× `getHolder()` chain captured once into `InventoryHolder holder`; `menu != this` → `holder != this` (identical reference comparison, short-circuit preserved) |
| `menus/ProfileViewerMenu.java` | 1× `SkullMeta` binding → explicit cast (existing single-evaluation `ItemMeta` local kept) |
| `utils/PacketSidebarRenderer.java` | 2× generic-wildcard patterns (`Collection<?>`, `Enum<?>`) → cast at use site / inside `&&` right operand |

instanceof 14 → 9. **All remaining Java 8 issues now live in deferred files/categories.**
Conversion rules in Batches 17–18 match Batches 2–15. Category C untouched; no tests modified.

---

### Batch 19 — 5 deferred files, 12 small/medium deferred Java 8 APIs (this checkpoint)

Baseline: master `8f5c1f4ab44f5aa52e9780db43659410976ecab6`. The branch checkout was found to be sitting on
the stale intermediate checkpoint `b2e898b`; the 5 edits were backed up outside the repository, the branch
was reset to the verified `origin/master`, and every fix was **re-derived against the clean master text**
(not re-applied as an old patch). The re-derived files are byte-identical (SHA-256) to the backups.

| File | Fixes |
|---|---|
| `storage/AuctionHouseRepository.java` | `CompletableFuture.failedFuture(...)` → `new CompletableFuture<T>()` + `completeExceptionally(...)` + `return`; generic `T`, exception object and early-return control flow preserved |
| `storage/ShopPreferenceRepository.java` | same `failedFuture` conversion |
| `managers/ConfigManager.java` | 2× `" ".repeat(n)` → new `private static String repeat(String, int)` helper that mirrors the JDK semantics (`count < 0` still throws `IndexOutOfBoundsException`, `count == 0` → `""`); `InputStream.readAllBytes()` → explicit `ByteArrayOutputStream` + 8192-byte read loop inside the unchanged try-with-resources (closing, `IOException` propagation, `IllegalArgumentException` for a missing resource and byte ordering all preserved); `import java.io.ByteArrayOutputStream` added |
| `managers/OrdersManager.java` | 4× `var` → `EconomyTransactionResult` (resolved from `EconomyManager.deposit/withdraw(Player, double, EconomyReason)`; enclosing signatures are `processAutoClaims(Player)`, `createOrder(Player)`, `claim(Player, long)`); 2× instanceof patterns in `extractFromShulker` → guard + explicit cast. `getItemMeta()` is still evaluated exactly once at the same point in the short-circuit chain, and `blockStateMeta` / `shulkerBox` stay in scope for their later uses at the end of the method |
| `utils/AdventureHeadComponentBridge.java` | 1× instanceof pattern → `instanceof` + explicit `(Component)` cast in the existing ternary. **Language-level conversion only — no additional Adventure API introduced** |

Result: `failedFuture` 2 → 0, `String.repeat` 2 → 0, `InputStream.readAllBytes` 1 → 0, `var` 4 → 0,
instanceof patterns 11 → 8. All 13 text blocks verified **byte-identical** to master; no test, Material,
Bukkit/Spigot, NMS, ProtocolLib or removed-system change; `SellStatsExporter`, `DatabaseManager`,
`ShardManager` and `ScoreboardManager` untouched. Category C is unchanged except the documented `+1`
`\bComponent\b` token from the explicit cast above.

---

### Batch 20 — text blocks: 4 files, 13 blocks → Java 8 concatenated literals (this checkpoint)

Baseline: `origin/master` `fd35a5e7d68b520728fcbea0133d9481ac45dde7`. Scope was **text blocks only**;
all other Java 8 deferred constructs (instanceof, `var`, `RandomGenerator`, networking, virtual threads,
`StringBuilder.isEmpty`, Category C APIs, tests) were explicitly out of scope and are untouched.

| # | File / original lines | Containing method | Purpose |
|---|---|---|---|
| 1 | `managers/DatabaseManager.java` 1914–1931 | `savePlayer(PlayerData)` | JDBC DML: `REPLACE INTO players … VALUES (66 × ?)` |
| 2 | 〃 2023–2035 | `countPlayersWithTrackedStats()` | JDBC query: `SELECT COUNT(*) … WHERE …` |
| 3 | 〃 2040–2052 | `resetPlayerStats()` | JDBC DML: `UPDATE players SET …` |
| 4 | 〃 4986–5004 | `resetForServerWipe(…)` | JDBC DML: `UPDATE players SET …` (server wipe) |
| 5 | 〃 5182–5201 | `resetForPlayerWipe(…)` | JDBC DML: `UPDATE players SET … WHERE uuid = ?` |
| 6–9 | `managers/OrdersManager.java` 3830–3882 | `ensureTables()` | Schema: `orders`, `order_deliveries`, `order_claims`, `order_ui_preferences` |
| 10–12 | `storage/AuctionHouseRepository.java` 738–778 | `ensureTables()` | Schema: `auction_listings`, `player_auction_preferences`, `auction_claims` |
| 13 | `storage/ShopPreferenceRepository.java` 132–138 | `ensureTables()` | Schema: `shop_favorites` |

**Method (proof of semantic equivalence).** Before any edit, each block's exact runtime content was
recorded by an independent JLS §3.10.6 interpreter (content starts immediately after the opening
delimiter's line terminator; LF normalization → faithful `String.stripIndent` port incl. the
closing-delimiter-line outdent rule → `translateEscapes`). All 13 blocks were pure ASCII: no tabs,
no backslashes, no trailing whitespace, no embedded `"`. Each block was regenerated programmatically as
one `"...\n"` literal per content line (158 literals, 5,261 chars; `+` continuation lines at indent +8,
assignment sites inline, argument sites wrapped to the next line in the established Batch-19 style).
A **second, independent** verifier (JLS §3.10.5/3.10.7 string-literal escapes; `\s`/line-continuation
rejected) re-parsed each concatenation at its site and byte-compared against the pre-edit recordings.

**Result: text blocks 13 → 0 (26 → 0 raw delimiter hits); 13/13 runtime contents byte-for-byte identical**
(SHA-256 match per block), every space, internal SQL indent, `?` placeholder, quote, capitalization and
trailing newline preserved — no SQL statement content changed. All 8 diff hunks provably contained inside
the original text-block spans (no non-text-block edits). No helper methods introduced. MongoDB remains
intentionally removed — nothing restored (3 pre-existing `mongo` textual references: `DatabaseManager`,
`AutoSaveTask`, `database.yml` — untouched). No Materials/Bukkit/NMS/ProtocolLib/Adventure changes
(Category C marker counts identical vs `HEAD`), no test changes.

**Build not verified — Maven/JDK unavailable.**

---

### Batch 21 — `SellStatsExporter` single-file Java 8 migration (COMPLETE, this checkpoint)

Baseline: `origin/master` `1373e840df25b956e071fd338564723a504f3d80`. The Arena checkout sat on stale
`b2e898b`; the migrated `SellStatsExporter` was backed up, the branch was reset to current master, and
the file was restored on that clean tree. **Only this source file plus this status document** are in the
checkpoint. Category C APIs and tests were not edited.

`java.net.http` was **unused imports only** (no outbound client). Replacement architecture:

| Issue | Replacement |
|---|---|
| unused `HttpClient` / `HttpRequest` / `HttpResponse` (+ unused `URI` / `Duration`) | imports removed |
| `Executors.newVirtualThreadPerTaskExecutor()` as `HttpServer` executor | `Executors.newCachedThreadPool()` (unbounded-ish per-request workers; lifecycle still `setExecutor` + `stop(0)` + `shutdownNow()`) |
| `StringBuilder.isEmpty()` in `prettify` | `sb.length() != 0` |
| `instanceof Player player && player.isOnline()` | classic `instanceof` + explicit cast |
| `FileWriter(File, Charset)` (Java 11) | `OutputStreamWriter(new FileOutputStream(htmlFile), StandardCharsets.UTF_8.name())` — create/truncate, UTF-8, try-with-resources unchanged |

`SellStatsExporter` remaining listed Java 8 blockers: **0**. Spigot 1.12.2 API phase: **NOT STARTED**.

**Build not verified — Maven/JDK unavailable.**

---

### Batch 22 — final instanceof pattern conversions (COMPLETE, this checkpoint)

Baseline: `origin/master` `68affd465ab0e80c90ac144aabc293fc387aa365` (Batch 21 merge, PR #20). The Arena
checkout again sat on stale `b2e898b`; the branch pointer was reset to current master before any edit.

A fresh AST inventory found **7** instanceof-pattern sites, not the 8 carried in the backlog —
`SellStatsExporter`'s single pattern had already been converted by Batch 21.

| File | Sites | Conversion |
|---|---:|---|
| `managers/DatabaseManager.java` | 6 | `saveHome`/`saveTeam`/`saveWarp` `X instanceof LazyLocation lazy` → classic `instanceof` + `LazyLocation lazy = (LazyLocation) X;` as first statement of the then-block; `bindParameters`/`toSqlLiteral`/`importMongoCollection` `value instanceof Boolean booleanValue` → `Boolean booleanValue = (Boolean) value;` |
| `managers/ShardManager.java` | 1 | `numericLong`: `value instanceof Number number ? number.longValue() : null` → `value instanceof Number ? ((Number) value).longValue() : null` |

Binding scope, `else if` short-circuit chains, null guards, single evaluation of receivers and generic types
were all preserved. **instanceof patterns repo-wide: 0.**

**Build not verified — Maven/JDK unavailable.**

---

### Batch 23 — javac-invalid repairs in `DatabaseManager` + `StatsWipeManager` (COMPLETE, this checkpoint)

Both defects are tree-sitter-tolerated but reject under javac. Root causes were recovered by deepening the
shallow clone (`git fetch --deepen=100`), which exposed `7e10b03` / `c4118b8` / `9257687`.

| File | Defect | Repair |
|---|---|---|
| `managers/DatabaseManager.java` `bindParameters` | branch tested `Boolean` but its body passed `stringValue`, a symbol declared nowhere in the file → *cannot find symbol* | restored the original **String** branch: `else if (value instanceof String) { String stringValue = (String) value; ps.setString(parameterIndex, stringValue); }` |
| `managers/StatsWipeManager.java` `wipeTarget` | `wipeTargets(Enumjava.util.Collections.singleton(target), …)` → *package Enumjava does not exist* | restored `EnumSet.of(target)` |

Evidence for the String branch: the pre-repair snapshot held **two** damaged copies of `bindParameters`
whose fragments are complementary — `} else if (value instanceof String) {` / `String stringValue = (String) …`
in one, `} else if (value;` / `ps.setString(parameterIndex, stringValue);` in the other. The original dispatch
order was `null → String → Integer → Long → UUID → else setObject`, with **no** Boolean branch; every caller
(`countPunishmentHistory`, `loadPunishmentHistory`, `countAllPunishments`, `loadAllPunishments`) supplies
String/Long/Integer parameters only. Root cause of `Enumjava`: an earlier `Set.of(` → `java.util.Collections.singleton(`
replacement matched **inside** `EnumSet.of(target)`, leaving the orphaned `Enum` prefix glued to the substitution.

**Build not verified — Maven/JDK unavailable.**

---

### Batch 24 — primitive-null return cleanup (COMPLETE, this checkpoint)

7 sites across 4 files returned `null` from `int`/`boolean` methods — *incompatible types: `<null>` cannot be
converted to int/boolean*. All were residue of the earlier switch-expression → switch-statement conversion,
which invented a `default:` arm where the Java 21 original had an **exhaustive** enum switch expression with
no default. Enum coverage was re-verified against each declaration, then the invented `default:` was removed
and Java 8 definite-return satisfied by a documented, unreachable trailing statement.

| File | Method(s) | Enum (constants / all covered) | Fallback |
|---|---|---|---|
| `managers/SpawnerManager.java` | `canOpen`, `canBreak` | `SpawnerInstance.AccessMode` (3/3) | `return false;` — fail-closed permission, matches `OWNER_ONLY` + null guard |
| `managers/StatsWipeManager.java` | `getPreviewCount`, `wipeSingleTarget` | `WipeTarget` (7/7) | `return 0;` — neutral row count |
| `models/AuctionCategory.java` | `matches(Material, boolean, boolean)` | `AuctionCategory` (9/9) | `return false;` — non-matching, matches null/AIR guards |
| `utils/PlayerSettingUtils.java` | `notificationEnabled`, `soundEnabled` | `NotificationChannel` (5/5), `SoundChannel` (2/2) | `return true;` — fail-open, matches each method's own null guard |

`AuctionCategory.matches(ItemStack)` keeps its **genuine** `default: return matches(type, false, false);`
(present in the original) — untouched.

**Build not verified — Maven/JDK unavailable.**

---

### Batch 25 — final switch expressions + `RandomGenerator` migration (COMPLETE, this checkpoint)

**Part A — switch expressions (3 → 0).** All three were mangled switch-expression remnants
(`case X: expr; break;` bodies, no `yield`), i.e. Java 9+ syntax *and* javac-invalid.

| File | Method | Original | Java 8 reconstruction |
|---|---|---|---|
| `managers/LeaderboardManager.java` | `formatValue(...)` | `return switch (type) { …13 arms… };`, no default, `LeaderboardType` 13/13 → exhaustive | classic switch, one `return` per arm verbatim; trailing `return NumberUtils.format(0D);` (unreachable) |
| `managers/LeaderboardManager.java` | `numericValue(...)` | same shape, `double` | classic switch; trailing `return 0D;` matching the file's own `bountyAmount` neutral value |
| `menus/TpaQueueMenu.java` | `buildButton(...)` | switch in **argument position** inside `menus().getInt(path + ".SLOT", switch (key) {…})`, String switch **with a real `default: -1`** | hoisted to `int defaultSlot; switch (key) { … default: defaultSlot = -1; }` then `menus().getInt(path + ".SLOT", defaultSlot)`; the `default` is preserved because `slot < 0` drives the existing guard |

The `TpaQueueMenu` hoist reorders only side-effect-free sub-expressions (a `String` switch, a string concat,
and `menus()` — a plain `return plugin.getConfigManager().getMenus();` getter), so evaluation order is
unobservable; `key` only ever receives the literals `"PREVIOUS"`, `"RANDOM"`, `"NEXT"`.

**Part B — `ShardManager` `RandomGenerator` → `java.util.Random` (3 → 0).** Caller survey first: the
`public static rollKillReward(KillRewardRange, RandomGenerator)` overload had exactly **one** call site in the
tree (the internal no-arg `rollKillReward()`, which passes `ThreadLocalRandom.current()`); the external
consumer `PlayerDeathListener:76` uses the **no-arg** overload, whose signature is unchanged.

- `import java.util.random.RandomGenerator;` → `import java.util.Random;`
- parameter/local retyped to `java.util.Random`; `ThreadLocalRandom extends Random`, so the existing source
  and the `random == null` fallback still work and **no new `Random` is ever instantiated** (per-thread,
  lock-free behaviour preserved)
- `source.nextLong(min, max + 1L)` → `nextLong(source, min, max + 1L)`, a new `private static` helper that
  reimplements the JDK's own bounded algorithm (`Random.internalNextLong` / `RandomSupport.boundedNextLong`,
  which is also what `ThreadLocalRandom.nextLong(origin, bound)` ran): power-of-two masking fast path,
  otherwise **rejection sampling** (no modulo bias), plus the JDK's wide-range resample branch

Bound semantics: `nextLong(origin, bound)` is origin-inclusive / bound-exclusive, and the call site passes
`origin = min`, `bound = max + 1L`, i.e. rewards are drawn from **`[min, max]` inclusive** — unchanged. The
`+ 1L` cannot overflow because the pre-existing `if (max < Long.MAX_VALUE)` guard still wraps it, and
`min == max` / `range == null` / `max == Long.MAX_VALUE` paths are untouched. The helper consumes the same
underlying `nextLong()` draws in the same order with the same acceptance test, so the distribution — and for
a seeded `Random`, the exact value sequence — is identical to the pre-migration code.

**Build not verified — Maven/JDK unavailable.**

---

## Remaining Java 8 inventory (superseded — historical Batch 13 measurement, see current figures below)

Counts below are a **fresh re-measurement** on master `138898fb` (tree-sitter grammar + targeted regex).
Batch 13 touched only corruption (no Java 9+ constructs), so the Java 8 inventory is unchanged by Batch 13.

### Category A — Java language

| Construct | src/main occ | src/main files |
|---|---:|---:|
| instanceof pattern matching | ~64 | ~38 |
| `var` declarations (in deferred `OrdersManager`) | 4 | 1 |
| switch-in-expression-position (colon/break form, in deferred `LeaderboardManager` ×2 / `TpaQueueMenu` ×1) | 3 | 2 |
| text blocks | 13 | 4 |
| **Category A total** | **~84** | — |

(instanceof count is methodology-sensitive — pattern-`instanceof` inside generics/arrays/bindings used in
`return`/assignment positions — the prior AST-scanner figure was 70/39; treat ~64–70 occ / ~38–39 files.)

### Category B — JDK 9+ standard library

| API | src/main occ | src/main files |
|---|---:|---:|
| `StringBuilder.isEmpty()` | 0 | 0 |
| `String.repeat()` (in deferred `ConfigManager`) | 2 | 1 |
| `RandomGenerator` (in deferred `ShardManager`) | 3 | 1 |
| `java.net.http.*` (in deferred `SellStatsExporter`) | 3 | 1 |
| `CompletableFuture.failedFuture()` (in deferred repositories) | 2 | 2 |
| `InputStream.readAllBytes()` (in deferred `ConfigManager`; 2 call sites) | 2 | 1 |
| `Executors.newVirtualThreadPerTaskExecutor()` (in deferred `SellStatsExporter`) | 1 | 1 |
| **Category B total** | **~13** | — |

Verified **zero** occurrences remain of: bare `Stream.toList()` (all `.toList()` are `Collectors.toList()`),
`String.isBlank()`, `String.strip()`, `List/Set/Map.of`, `List/Set/Map.copyOf`, `List.getFirst()`,
`Path.of()`, `Objects.requireNonNullElse`, `toArray(IntFunction)`.

### Pre-existing javac-invalid corruption artifacts

The **6 Batch-13 artifacts are FIXED** (AuctionHouseCommand, SellGui, SocialCommand,
LuckPermsTablistRefreshBridge, KeyAllManager, CuboidCommand — see Batch 13 table above).

Three further files with the **same** unreachable-`break;` corruption pattern (repeated `break;` inside switch
case blocks — javac §14.21) were observed during this checkpoint and are **deferred** (out of Batch 13 scope,
tree-sitter-tolerated but javac-invalid — fix in a near-term batch):

| File | Artifact |
|---|---|
| `commands/FriendsCommand.java` | repeated unreachable `break;` in multiple switch case blocks |
| `commands/SellCommand.java` | repeated unreachable `break;` in switch case blocks |
| `commands/TPACommand.java` | unreachable `break;` after `break;`/in `default` block |

(`menus/FriendsMenu.java` continue/break interleaving was inspected and is valid Java — not an artifact.)

### Deferred within the Java 8 phase (need design, not mechanical edits)
`SellStatsExporter` `java.net.http` + virtual threads + `StringBuilder.isEmpty()` ·
`LeaderboardManager` + `TpaQueueMenu` switch-in-expression-position · `ShardManager` `RandomGenerator`
(`nextLong(origin, bound)` needs a Java 8 bounded-long reimplementation — not trivial) ·
instanceof patterns (8: `DatabaseManager` 6, `SellStatsExporter` 1, `ShardManager` 1 — deferred-file batches).
(text blocks cleared in Batch 20; `ConfigManager.readAllBytes()` cleared in Batch 19 — the remaining
`Files.readAllBytes` at `ConfigManager:506` is a Java 7 API and not a blocker.)

---

## Current Java 8 inventory (verified this checkpoint, post-Batch-25)

**Java 8 compatibility is statically complete; real javac/Maven verification remains.**

**0 genuine Java 8 blockers** across all 421 `src/main` files, per the validated scanner
(tree-sitter-java 0.23.5 via tree-sitter 0.26.0, explicit large buffer, AST-based — not regex-based —
detection, with API pattern matching applied to non-comment lines only).

| Construct / API | Hits |
|---|---:|
| instanceof pattern matching | **0** ✅ (cleared by Batch 22) |
| `var` declarations | **0** ✅ (Batch 19) |
| text blocks | **0** ✅ (Batch 20) |
| records · sealed types | **0** ✅ |
| switch expressions (expression-position **and** `->` rules) · `yield` | **0** ✅ (cleared by Batch 25) |
| `java.util.random.RandomGenerator` · bounded `nextLong(origin, bound)` | **0** ✅ (cleared by Batch 25) |
| `java.net.http` · virtual threads (`newVirtualThreadPerTaskExecutor` / `Thread.ofVirtual`) | **0** ✅ (Batch 21) |
| `String.repeat` · `StringBuilder.isEmpty()` · `String.isBlank()` · `String.strip()` | **0** ✅ |
| `CompletableFuture.failedFuture` · `InputStream.readAllBytes` | **0** ✅ (Batch 19) |
| `List/Map/Set.of` · `List/Map/Set.copyOf` · `Map.entry` / `Map.ofEntries` | **0** ✅ |
| bare `Stream.toList()` | **0** ✅ (all `.toList()` hits are `Collectors.toList()`) |
| `Files.readString` / `Files.writeString` · `Path.of` · `List.getFirst` · `Objects.requireNonNullElse` · `toArray(IntFunction)` | **0** ✅ |

Documented **false positives** (Java 8-legal, deliberately not changed): `EnumSet.of(...)` / `EnumSet.copyOf(...)`,
`Collectors.toList()`, `Files.readAllBytes` at `ConfigManager:506` (Java 7 NIO), the project's own
`SidebarSettings.lines()` and `ColorUtils.strip(...)` accessors.

### Structural + javac-invalid validation (this checkpoint)

| Check | Result |
|---|---:|
| `src/main` files parsed (large buffer) | **421 / 421 clean** |
| tree-sitter `ERROR` / `MISSING` nodes | **0 / 0** |
| delimiter balance (brace / paren / bracket) | **0 imbalances** |
| duplicate declarations (full signature, type-scoped; enum-constant bodies excluded) | **0** |
| javac-invalid corruption markers | **0** |
| unresolved simple-name references (scope-aware, project supertype graph) | **0** |
| unreachable statements after `return`/`throw`/`break`/`continue` | **0** |
| `null` returned from a primitive-returning method | **0** (cleared by Batch 24) |
| value-return in `void` / empty return in non-void | **0** |
| `git diff --check` | clean |

### Known non-blocking defects (reported, deliberately out of scope)

These are javac-invalid but are **not** Java 8 feature issues; they predate the Java 8 phase and are
candidates for a follow-up correctness batch (they do **not** affect the Java 8 statement above, which is
about language/API level):

| File | Defect |
|---|---|
| `managers/LeaderboardManager.java:137` | `getTypes()` returns `java.util.Collections.singletonList(LeaderboardType.values())` → `List<LeaderboardType[]>` vs declared `List<LeaderboardType>` (same `List.of(` → `singletonList(` regex collision; correct Java 8 form is `Arrays.asList(...)`) |
| `managers/FeatureManager.java:177` | identical defect with `Feature.values()` |
| `managers/ShardManager.java:46` | `KillRewardRange.toString()` returns the literal `"KillRewardRange[min=+min, max=+max]"` (concatenation collapsed into the string); compiles, output only |

---

## Category C — Spigot 1.12.2 API migration: IN PROGRESS (Batches 26–36B)

**Category C has officially started.** Batch 26 is the first intentional Spigot/Bukkit 1.12.2 API
migration. Scope was **modern Material constants + `Material.isAir()` only** in 10 isolated files.
BlockData, PDC, NamespacedKey, NMS, ProtocolLib, Adventure, Particle, Sound, and EntityType were
**not** edited.

1.12.2 enum sourced from the Spigot-API 1.12.2 javadoc
(`https://helpch.at/docs/1.12.2/org/bukkit/Material.html`). Local `spigot-api` JAR / Maven / JDK
were unavailable.

### Batch 26 — Materials + `isAir()` (COMPLETE, this checkpoint)

Baseline: `origin/master` `c42c3106d1a8b685f104ba99e04249fb8e0a4d8c`.

| File | Change |
|---|---|
| `listeners/AnvilModerationListener.java` | `ItemStack.getType().isAir()` → `== Material.AIR` (null guard unchanged) |
| `commands/RenameCommand.java` | same, held item |
| `amethyst/AmethystToolsTask.java` | same, inventory contents |
| `managers/FastCrystalManager.java` | `Block.getType().isAir()` → `!= Material.AIR` for space-above checks; 1.12 worlds have only `AIR` (no `CAVE_AIR`/`VOID_AIR`) |
| `menus/BaseMenu.java` | added `fill(Material, short)` using `new ItemStack(material, 1, data)`; existing `fill(Material)` still delegates to `ItemUtils.fillInventory`; `isPlaceholder` now `STAINED_GLASS_PANE` data 7 or 15 |
| `menus/KeysMenu.java` | black pane fill → `STAINED_GLASS_PANE` data **15** (ItemStack filler) |
| `menus/LeaderboardMenu.java` / `SellAllConfirmMenu.java` / `SpawnerWorldListMenu.java` | gray pane fill → `STAINED_GLASS_PANE` data **7** |
| `menus/HomeDeleteConfirmMenu.java` | light-gray fill → pane data **8**; confirm `LIME_TERRACOTTA` → `STAINED_CLAY` data **5**; cancel `RED_TERRACOTTA` → `STAINED_CLAY` data **14** (`createItem` then `setDurability`) |

`fill(Material)` callers that still pass modern pane enums are unchanged (not in this batch).
`SellAllConfirmMenu` config **string** defaults (`"RED_STAINED_GLASS_PANE"`) still go through
`ItemUtils.parseMaterial` and were left as config compatibility.

**isAir:** 108 → **103** (5 call sites in 4 files; FastCrystal had 2). Receivers were
`ItemStack.getType()` or `Block.getType()` (`Material`). Null checks preserved. No `isEmpty()` edits.

**Modern Material refs (regex vs 1.12.2 enum):** 334 → **325** (−9). Unique modern constants
76 → **73** (removed `BLACK/GRAY/LIGHT_GRAY_STAINED_GLASS_PANE` from these files; those names remain
elsewhere; terracotta names removed from `HomeDeleteConfirmMenu` only).

**Build not verified — Maven/JDK unavailable.** tree-sitter-java 0.23.5 / tree-sitter 0.26.0,
explicit large buffer: **421/421** `src/main` parse clean, 0 ERROR/MISSING, 0 delimiter imbalance,
0 javac-invalid markers, `git diff --check` clean.

### Batch 27 — Simple Materials (COMPLETE, this checkpoint)

Baseline: `origin/master` `f18b55d93d054092eaf716b5937a0eb21cf427c5` (PR #22 merge).

Scope: 17 intentional Material mappings across 10 isolated files.
No skulls, no PLAYER_HEAD, no config strings, no tests, no isAir changes, no BlockData/PDC/NamespacedKey/ProtocolLib/NMS/Adventure changes.

| File | Change |
|---|---|
| `listeners/SpawnerBlockListener.java` | 2× `block.getType() == Material.SPAWNER` → `Material.MOB_SPAWNER` |
| `managers/SpawnStashManager.java` | `block.getType() == Material.SPAWNER` → `Material.MOB_SPAWNER` |
| `menus/BillfordMenu.java` | countdown item `Material.CLOCK` → `Material.WATCH` |
| `menus/FriendsMenu.java` | refresh button `Material.CLOCK` → `Material.WATCH` |
| `menus/HideMenu.java` | scramble button `Material.ENDER_EYE` → `Material.EYE_OF_ENDER`; remove-hide `Material.RED_DYE` → `Material.INK_SACK` + `.setDurability((short) 1)` (Rose Red) |
| `menus/OrdersCollectMenu.java` | my-orders `Material.WRITABLE_BOOK` → `Material.BOOK_AND_QUILL`; refresh `Material.CLOCK` → `Material.WATCH` |
| `menus/PlayerLogsMenu.java` | spawners log `Material.SPAWNER` → `Material.MOB_SPAWNER`; messages log `Material.WRITABLE_BOOK` → `Material.BOOK_AND_QUILL` (`SKELETON_SKULL` untouched) |
| `menus/SellStatsAdminMenu.java` | sales log `Material.CLOCK` → `Material.WATCH`; export report `Material.WRITABLE_BOOK` → `Material.BOOK_AND_QUILL` (`PLAYER_HEAD` untouched) |
| `menus/SpawnerMainMenu.java` | fallback `Material.EXPERIENCE_BOTTLE` → `Material.EXP_BOTTLE` (config string `"EXPERIENCE_BOTTLE"` untouched) |
| `models/AuctionCategory.java` | `BLOCKS` icon `Material.GRASS_BLOCK` → `Material.GRASS`; `BOOKS` match `Material.WRITABLE_BOOK` → `Material.BOOK_AND_QUILL`; `UTILITIES` match `Material.CLOCK` → `Material.WATCH` |

**Material mappings summary:**
- `CLOCK` → `WATCH` (5 sites)
- `SPAWNER` → `MOB_SPAWNER` (4 sites)
- `GRASS_BLOCK` → `GRASS` (1 site)
- `WRITABLE_BOOK` → `BOOK_AND_QUILL` (4 sites)
- `EXPERIENCE_BOTTLE` → `EXP_BOTTLE` (1 site) — 0 remaining repo-wide
- `ENDER_EYE` → `EYE_OF_ENDER` (1 site) — 0 remaining repo-wide
- `RED_DYE` → `INK_SACK` + durability 1 (1 site)

**isAir:** Exactly **103** remaining across 35 files (0 changed in Batch 27).

**Modern Material refs (regex vs 1.12.2 enum):** 325 → **308** (−17). Unique modern constants 73 → **71** (−2: `EXPERIENCE_BOTTLE` and `ENDER_EYE` eliminated). Files with modern Materials: 86 → **83** (−3: `BillfordMenu`, `SpawnerBlockListener`, `SpawnStashManager` now clean).

**Deferred Category C components (NOT STARTED):**
BlockData, PDC, NamespacedKey, ProtocolLib, NMS, Adventure, Particle, Sound, EntityType remain deferred and strictly untouched.

### Batch 28 — Stained-glass panes (COMPLETE, this checkpoint)

Baseline: `origin/master` `3beb20f90f8b17ad0f74e5051c10c2e5ecf195bb` (PR #23 merge).

Scope: exactly 32 colored-pane references across 10 isolated source files. No skulls, no `PLAYER_HEAD`,
no dyes, no terracotta, no `SUNFLOWER`, no `SPAWNER`, no `CLOCK`, no config strings, no tests, no use of
`Material.isAir()` changes, and no BlockData/PDC/NamespacedKey/ProtocolLib/NMS/Adventure changes.

| Legacy Material | 1.12.2 replacement | Legacy data |
|---|---|---|
| `GRAY_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 7` |
| `BLACK_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 15` |
| `RED_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 14` |
| `LIME_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 5` |

Files changed:
`utils/ItemUtils.java`, `menus/EnchantSelectMenu.java`, `menus/ChatLogsMenu.java`,
`menus/PlayerLogsMenu.java`, `menus/SpawnerPanelMenu.java`, `menus/FriendsMenu.java`,
`menus/OrdersNewMenu.java`, `menus/WorthMenu.java`, `menus/SpawnerStorageMenu.java`,
`menus/PayConfirmMenu.java`.

`ItemUtils` added three data-aware overloads that reuse the Batch-26 `new ItemStack(material, 1, data)`
pattern without changing any existing one-argument helper:
`createItem(Material, short, String, List<String>)`, `createPlaceholder(Material, short)`, and
`fillInventory(Inventory, Material, short)`. Existing no-data `createItem`, `createPlaceholder`,
`fillInventory` and `BaseMenu.fill` callers are unchanged. `ItemUtils.parseMaterial` was **not** modified;
the modern config strings `"GRAY_STAINED_GLASS_PANE"`, `"BLACK_STAINED_GLASS_PANE"`,
`"RED_STAINED_GLASS_PANE"`, `"LIME_STAINED_GLASS_PANE"` remain byte-identical.

**Material counts:** 158 → **126** target pane references (−32): GRAY 70→51, BLACK 35→29, RED 27→23,
LIME 26→23. Modern Material references 308 → **276**, unique modern constants 71 → **71**,
files with modern Materials 83 → **78**. `Material.isAir()` remains exactly **103**.

**Config-string compatibility is explicitly deferred:** `ItemUtils.parseMaterial()` still resolves the
modern pane strings to modern enum constants in this checkpoint; a dedicated config-mapping layer is left
for a later batch.

**Category C status:** **IN PROGRESS** — Batch 26, 27, 28, and 29 are complete; the remaining pane/dye/head/
1.13+ items and the rest of Category C (BlockData, PDC, NamespacedKey, ProtocolLib, NMS, Adventure,
Particle, Sound, EntityType) remain for later batches.

**Build not verified — Maven/JDK unavailable.**

---

### Batch 29 — Dyes → `INK_SACK` + data (COMPLETE, this checkpoint)

Baseline: `origin/master` `960d8d7d6666f3b27a678e19a15dfde792867a8f` (PR #24 merge).

Scope: 23 dye `Material` references migrated to the 1.12.2 legacy `INK_SACK` + durability/data across 5
isolated source files. No skulls, no `PLAYER_HEAD`, no stained-glass panes, no terracotta, no `SUNFLOWER`,
no `SPAWNER`, no `CLOCK`, no config strings, no tests, no `Material.isAir()` changes, and no
BlockData/PDC/NamespacedKey/ProtocolLib/NMS/Adventure changes.

| Legacy Material | 1.12.2 replacement | Legacy data |
|---|---|---|
| `GRAY_DYE` | `INK_SACK` | `(short) 8` |
| `LIME_DYE` | `INK_SACK` | `(short) 10` |
| `RED_DYE` | `INK_SACK` | `(short) 1` (Rose Red) |
| `ORANGE_DYE` | `INK_SACK` | `(short) 14` |
| `PINK_DYE` | `INK_SACK` | `(short) 9` |

(`BLUE_DYE` → `INK_SACK` data **4** is the remaining dye mapping; its only site is a config fallback in
`menus/HomeMenu.java` and is deferred to config compatibility.)

Files changed:
`managers/InvseeManager.java`, `menus/FriendDetailMenu.java`, `menus/HomeActionMenu.java`,
`menus/OrdersEditMenu.java`, `menus/ServerInfoMenu.java`.

- `InvseeManager.buildStatusItem` — `Material` ternary (`ORANGE_DYE`/`LIME_DYE`/`GRAY_DYE`) replaced by a
  `short data` ternary (`14`/`10`/`8`) feeding `ItemUtils.createItem(Material.INK_SACK, data, name, lore)`.
- `FriendDetailMenu` — 2 static buttons (`LIME`→10, `GRAY`→8) and 6 toggle ternaries
  (`cond ? LIME : GRAY` → `cond ? (short) 10 : (short) 8`).
- `HomeActionMenu` — "Delete Home" `RED_DYE` → `INK_SACK` data 1.
- `OrdersEditMenu` — 3× "Edit locked" `GRAY_DYE` → `INK_SACK` data 8 via the data-aware 4-arg `createItem`.
- `ServerInfoMenu` — added a `short data` field to `ButtonDefinition` (5-arg constructor delegates to the
  new 6-arg constructor with `data=0`; `equals`/`hashCode`/`toString` include `data`; render passes `data`).
  Settings `GRAY_DYE`→8, Social&media `PINK_DYE`→9.

Every migration is an ItemStack-creation site; name/lore/amount/slot/meta are unchanged. No dye comparison
was altered, and no `INK_SACK` was introduced without an explicit data value (all 23 sites carry data 1/8/9/10/14).

**Material counts:** 31 → **8** dye references (−23): GRAY 15→3, LIME 9→1, RED 3→2, BLUE 1→1, ORANGE 1→0,
PINK 2→1. Modern Material references 276 → **253**, unique modern constants 71 → **70** (`ORANGE_DYE`
eliminated), files with modern Materials 78 → **78**. `Material.isAir()` remains exactly **103**.

**Config-string compatibility is explicitly deferred:** the remaining 8 dye `Material` references are all
config-parsing fallbacks — `StaffModeManager` (`"GRAY_DYE"`), `HomeMenu` (`BLUE_DYE`/`GRAY_DYE`/`RED_DYE`
via `ItemUtils.parseMaterial`), `MediaMenu` (`"PINK_DYE"`), `SpawnerFilterMenu` (`"LIME_DYE"`/`"RED_DYE"`).
`ItemUtils.parseMaterial()` was **not** modified; the modern dye strings in code defaults and resources YAML
(`filter.yml`, `menus.yml`, `staff-mode.yml`, `worth.yml`) remain byte-identical for a dedicated
config-compatibility batch.

**Category C status:** **IN PROGRESS** — Batch 26, 27, 28, and 29 are complete; the remaining dye/head/
1.13+ items and the rest of Category C (BlockData, PDC, NamespacedKey, ProtocolLib, NMS, Adventure,
Particle, Sound, EntityType) remain for later batches.

**Build not verified — Maven/JDK unavailable.**

---

### Batch 30 — Stained-glass panes part 2 (COMPLETE, this checkpoint)

Baseline: `origin/master` `abc52529d1676cd50f6ab58ff3c20c874b3af782` (PR #25 merge).

Scope: exactly **23** colored-pane `Material` references migrated to the 1.12.2 legacy data-aware pattern
across **10** isolated source files (all ItemStack-creation sites). No skulls, no `PLAYER_HEAD`, no dyes,
no terracotta, no `SUNFLOWER`, no `SPAWNER`, no `CLOCK`, no config strings, no tests, no
`Material.isAir()` changes, and no BlockData/PDC/NamespacedKey/ProtocolLib/NMS/Adventure changes.
No comparison was touched (**zero** comparisons of the four targets exist repo-wide, before or after).

| Legacy Material | 1.12.2 replacement | Legacy data |
|---|---|---|
| `GRAY_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 7` |
| `BLACK_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 15` |
| `RED_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 14` |
| `LIME_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 5` |

Files changed: `menus/TpaConfirmMenu.java`, `menus/OrdersCollectMenu.java`,
`menus/OrdersSelectItemMenu.java`, `menus/OrdersSearchItemMenu.java`, `menus/OrdersEditMenu.java`,
`menus/OrdersInventoryItemMenu.java`, `menus/LeaderboardTypeMenu.java`,
`menus/SpawnerSellConfirmMenu.java`, `menus/SpawnerFilterMenu.java`, `menus/DisguiseAliasMenu.java`.

All sites reuse the existing Batch-26/28 data-aware helpers (`BaseMenu.fill(Material, short)`,
`ItemUtils.createItem(Material, short, String, List)`, `ItemUtils.createPlaceholder(Material, short)`);
`ItemUtils.java` / `BaseMenu.java` / `ItemUtils.parseMaterial()` were **not** modified. Amount, name,
lore, slot and meta are preserved. `CrateManager` (13 refs, config write + `parseMaterial`/
`parseDisplayItem` fallbacks + in-memory `*Settings.defaults()` holders consumed by 4 menu classes)
was deliberately deferred as config-default behavior rather than exceeding the 10-file budget.

**Material counts:** 128 → **105** target pane references (−23): GRAY 53→41, BLACK 29→22, RED 23→20,
LIME 23→22 (substring convention; 2 of the residual `GRAY` lines are `LIGHT_GRAY_STAINED_GLASS_PANE`).
Modern Material references 253 → **230**, unique modern constants 70 → **70** (no constant eliminated),
files with modern Materials 78 → **75**. `Material.isAir()` remains exactly **103**.

**Config-driven pane compatibility is explicitly deferred:** the remaining 105 target pane
`Material` references are config parsing/string defaults (26), config-helper fallbacks (24 +
null-fallback 5 + `parse/DisplayItem` 4 + settings defaults 4), one config write, and 39 direct
ItemStack-creation sites (34 `fill(Material)` + 5 `createItem`/`createGlassPane`) noted for Batch 31.
`ItemUtils.parseMaterial()` was **not** modified; the modern pane strings in code defaults and
resources YAML remain byte-identical for a dedicated config-compatibility batch.

**Category C status:** **IN PROGRESS** — Batches 26–31 are complete; the remaining direct pane sites,
the config-driven pane/dye/head/1.13+ items and the rest of Category C (BlockData, PDC, NamespacedKey,
ProtocolLib, NMS, Adventure, Particle, Sound, EntityType) remain for later batches.

### Batch 31 — Direct pane ItemStack sites part 1 (COMPLETE, this checkpoint)

Baseline: `origin/master` `68ab7469a2ce90a326e2c4587a6f7d7bedb5909a` (PR #26 merge). Fresh inventory
found **103** target pane references: **39** direct ItemStack-creation sites (34 `fill(Material)`,
4 `createItem`, 1 `createGlassPane`), **64** config parsing/default sites, **0** comparisons.
Scope: exactly **10** direct ItemStack/fill sites migrated across **10** files — the maximum batch size.
Only direct creations were touched; every config string/parsing/default site was left byte-identical.

| Legacy Material | 1.12.2 replacement | Legacy data | Sites |
|---|---|---:|---:|
| `GRAY_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 7` | 4 |
| `BLACK_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 15` | 5 |
| `RED_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 14` | 1 |
| `LIME_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 5` | 0 (no direct LIME sites exist) |

Files changed: `menus/BountyMenu.java`, `menus/BountyConfirmMenu.java`, `menus/HideMenu.java`,
`menus/MediaMenu.java`, `menus/RulesMenu.java`, `menus/DisguiseSkinMenu.java`, `menus/HideListMenu.java`,
`menus/ShopEditorMenu.java`, `menus/CrateGachaMenu.java` (indicator item — the only direct RED site),
`managers/InvseeManager.java` (`createGlassPane(GRAY_…)` → existing `createGlassPane()` ≡ SGP+7).

All sites reuse the existing Batch-26/28 data-aware helpers (`BaseMenu.fill(Material, short)`,
`ItemUtils.createItem(Material, short, …)`, `ItemUtils.createGlassPane()`); `ItemUtils.java` /
`BaseMenu.java` / `ItemUtils.parseMaterial()` were **not** modified. Amount, name, lore, slot and meta
preserved at every site. No comparisons existed or were touched.

**Material counts:** 103 → **93** target pane references (−10, exactly the selected sites): GRAY 39→35,
BLACK 22→17, RED 20→19, LIME 22→22. `Material.isAir()` remains exactly **103**. Batch 26–30 mappings
intact and only grown additively (SGP+7 36, SGP+15 18, SGP+14 3, SGP+5 2, SGP+8 1, `STAINED_CLAY` 2,
`WATCH`/`MOB_SPAWNER`/`BOOK_AND_QUILL`/`EYE_OF_ENDER`/`EXP_BOTTLE`/`GRASS`/`INK_SACK` untouched).

**Remaining 93 target pane refs, classified:** **29** direct ItemStack-creation sites (all
`fill(Material)` — e.g. `ConfirmKillMenu`, `SellGui`, `SpawnerMainMenu`, `ShopMenu`, `TeamMenu`,
`PlayerAuctionGui`, `StatsMenu`) for later batches; **64** config parsing/default sites
(`parseMaterial` defaults, `AuctionHouseMenuSupport.control()` / `OrdersMenuSupport.button()`
fallbacks, Team/Stats `material()` helpers, `CrateManager` defaults) deliberately deferred; **0**
comparisons. **Batch 32 NOT STARTED.**

**Validation:** tree-sitter 0.26.0 runtime + tree-sitter-java 0.23.5 grammar, explicit full-buffer
parse of all **421** src/main files — **0** ERROR/MISSING, **0** token-level delimiter imbalances,
**0** duplicate declarations (full signatures) in changed files, **0** javac-invalid markers,
`git diff --check` clean, 0 forbidden-zone hits in the diff, 0 test/resource files touched.

**Build not verified — Maven/JDK unavailable.**

---

### Batch 32 — Direct pane fill sites part 2 (COMPLETE, this checkpoint)

Baseline: `origin/master` `671fd862d1659c3a92c158410c62a737df949ec2` (PR #27 merge). Scope: exactly
**10** direct `fill(Material)` sites migrated across **10** files (the maximum batch size) — the
safest tier only: one direct fill site per file, zero in-file config/default pane references, zero
comparisons. Every selected file was read in full before editing; only the one fill line per file
changed (10 files × 1 line, 10 insertions / 10 deletions).

| Legacy Material | 1.12.2 replacement | Legacy data | Sites |
|---|---|---:|---:|
| `GRAY_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 7` | 8 |
| `BLACK_STAINED_GLASS_PANE` | `STAINED_GLASS_PANE` | `(short) 15` | 2 |

Files changed: `menus/FriendDetailMenu.java`, `menus/OrdersBrowseMenu.java`,
`menus/ProfileViewerMenu.java`, `menus/PunishmentHistoryMenu.java`, `menus/PunishmentsListMenu.java`,
`menus/RTPMenu.java`, `menus/SellHistoryMenu.java`, `menus/SellStatsAdminMenu.java`,
`menus/ServerInfoMenu.java`, `menus/ShulkerPreviewGui.java`. All sites reuse the existing
`BaseMenu.fill(Material, short)` helper (amount 1, blank name, no lore, same slots — visual gray
via data 7 / black via data 15 preserved). `ItemUtils.java` / `BaseMenu.java` /
`ItemUtils.parseMaterial()` were **not** modified. No config string, config parsing/default site,
comparison, dye, skull/head, terracotta, `SPAWNER`, `CLOCK`, `BlockData`/PDC/NamespacedKey/
Particle/Sound/EntityType/ProtocolLib/NMS/Adventure or test site was touched.

**Material counts:** GRAY 35→**27**, BLACK 17→**15**, RED 19→**19**, LIME 22→**22**. True target
pane refs **92 → 82** distinct source lines (**93 → 83** constant occurrences — `SellGui.java:84`
carries both `LIME_` and `GRAY_STAINED_GLASS_PANE` on one line; the −10 delta is identical under
either unit). `Material.isAir()` remains exactly **103**. Batch 26–31 mappings intact and only
grown additively: SGP+7 36→44, SGP+15 18→20, SGP+8 1, SGP+14 3, SGP+5 2, `STAINED_CLAY` 5/14 2,
`WATCH` 5, `MOB_SPAWNER` 4, `BOOK_AND_QUILL` 4, `EYE_OF_ENDER` 1, `EXP_BOTTLE` 1, `GRASS` 4,
`INK_SACK` 16, `PLAYER_HEAD` 24 — all unchanged.

**Remaining pane refs, classified (current classifier):** **31** direct ItemStack/fill-related
sites — **19** direct `fill(Material)` (e.g. `AuctionHouseBrowseMenu`, `ConfirmKillMenu`,
`ConfirmPurchaseGui`, `SellGui`, `ShopMenu`, `TeamMenu`, `SpawnerMainMenu`) plus **12** direct
non-fill ItemStack-creation sites (`createItem`/ternary constants in `ConfirmPurchaseGui` ×2,
`FilterGui`, `OrdersDeleteConfirmMenu` ×2, `OrdersDeliverConfirmMenu` ×2, `OrdersDepositMenu`,
`SellGui` ×4); **51** config/default sites (`parseMaterial` string defaults, `control()`/
`button()`/`material()`/`getMenuPlaceholderMaterial` fallbacks, `CrateManager` defaults incl. its
6 `defaults()` constants, `== null` fallbacks, `.name()` config writes) deliberately deferred;
**0** comparisons.

**Counting note (29 vs 31, resolved without code changes):** the earlier "29 direct sites" figure
counted *direct `fill(Material)` call sites only* (29 before Batch 32; 29−10 = **19** remain). The
post-batch classifier broadens the direct bucket to *all direct ItemStack/fill-related sites*:
those 19 fill sites **plus** the 12 direct non-fill item-creation sites, which the earlier
bookkeeping had grouped inside the "64 config/default" remainder. Both taxonomies describe the
same code: 93 occurrences = 29 fill + 13 non-fill-direct occurrences + 51 config, and 82 lines =
19 fill + 12 non-fill lines (one dual-constant) + 51 config. The difference is classification
granularity and counting unit (constant occurrences vs call sites), not a migration change.

**Validation:** tree-sitter 0.26.0 runtime + tree-sitter-java 0.23.5 grammar, full-buffer parse of
all **421** src/main files — **421/421 clean**, 0 ERROR/MISSING, 0 delimiter imbalances,
0 duplicate declarations (full signatures), 0 javac-invalid/conflict markers, `git diff --check`
clean, 0 test files touched. Validator self-tested against injected malformed/duplicate cases.

**Build not verified — Maven/JDK unavailable.**

**Category C status:** **IN PROGRESS** — Batches 26–32 complete; **Batch 33 NOT STARTED**. The 31
direct sites and 51 config/default sites above, plus the wider Category C backlog (BlockData, PDC,
NamespacedKey, ProtocolLib, NMS, Adventure, Particle, Sound, EntityType), remain for later batches.

---

### Batch 33 — Direct pane fill sites part 3 (COMPLETE, this checkpoint)

Baseline: `origin/master` `bb14e38e9bce78498f2c68a73ccbc2747e53f7ad` (PR #28 merge, Batch 32). Fresh
inventory found the four target panes at **83** constant occurrences / **82** distinct source lines
(61 `Material.`-qualified + 22 config default strings): classified **31** direct sites — **19**
direct `fill(Material)` plus **12** helper-default non-fill lines — **51** config/default sites,
**0** comparisons. Of the 12 "direct non-fill" lines (Batch 32's broadened bucket), every one is a
fallback-`Material` argument to protected config-driven helpers (`AuctionHouseMenuSupport.control()`
×7 lines, `OrdersMenuSupport.button()` ×5 lines); both helpers take plain `Material` (no data
overload) and even write `fallbackMaterial.name()` into config defaults, so migrating them requires
helper changes and/or a config-compat layer — both explicitly out of scope. **Batch 33 therefore
migrated only fill sites: 10 files × 1 unconditional `fill(Material.GRAY_STAINED_GLASS_PANE)` →
`fill(Material.STAINED_GLASS_PANE, (short) 7)** (the maximum batch size; simplest tier: one direct
site per file, no comparisons, no helper changes).

Files changed: `menus/StatsMenu.java`, `menus/TeamInfoMenu.java`, `menus/TeamMenu.java`,
`menus/TeleportAreaMenu.java`, `menus/SellProgressMenu.java`, `menus/ShopMenu.java`,
`menus/ProfileViewerHomesMenu.java`, `menus/PurchaseShopMenu.java`, `menus/ConfirmKillMenu.java`,
`menus/SellGui.java` — exactly 10 insertions / 10 deletions, nothing else. In mixed files
(`SellProgressMenu`, `ShopMenu`, `ProfileViewerHomesMenu`, `PurchaseShopMenu`, `ConfirmKillMenu`,
`SellGui`) every config/default pane reference — string defaults, `parseMaterial` defaults,
`control()` fallbacks — was left byte-identical. All sites reuse the existing Batch-26
`BaseMenu.fill(Material, short)` helper (amount 1, blank name, same slots; gray visual preserved
via data 7). `ItemUtils.java` / `BaseMenu.java` / `ItemUtils.parseMaterial()` / `CrateManager`
config-default logic / dye / head / skull / terracotta / `SUNFLOWER` / `SPAWNER` / `CLOCK` /
`isAir()` / BlockData / PDC / NamespacedKey / Particle / Sound / EntityType / ProtocolLib / NMS /
Adventure / tests: untouched.

**Material counts:** GRAY 24→**14**, BLACK 12→**12**, RED 11→**11**, LIME 14→**14**
(`Material.`-qualified). True target pane refs **82 → 72** distinct lines (**83 → 73** constant
occurrences; −10 under either unit). Direct sites **31 → 21** (19→**9** fill + 12 protected
helper-default lines); config/default sites **51 → 51** (untouched); comparisons **0**. Modern
Material references **210 → 200** (ledger basis: 230 @ Batch 30 − 10 − 10 verified from the
Batch 31/32 merge diffs − 10 here); independent audited basis (minecraft-data 1.12.2 list +
hand-audit of Bukkit/vanilla name mismatches): **194 → 184** occurrences, unique constants 60 → 60,
files with modern Materials 58 → **53** (−5: `TeleportAreaMenu`, `SellProgressMenu`, `ShopMenu`,
`ProfileViewerHomesMenu`, `PurchaseShopMenu` now modern-free). Both bases agree on the exact −10
delta and 0 constants eliminated. `Material.isAir()` remains exactly **103**. Batch 26–32 mappings
intact and additively grown: SGP+7 44→**54**, SGP+15 20, SGP+14 3, SGP+5 2, SGP+8 1, `STAINED_CLAY` 2,
`WATCH` 5, `MOB_SPAWNER` 4, `BOOK_AND_QUILL` 4, `EYE_OF_ENDER` 1, `EXP_BOTTLE` 1, `GRASS` 1,
`INK_SACK` 16, `PLAYER_HEAD` 24 — all unchanged except the +10 SGP+7.

**Remaining direct fill sites (9):** `AuctionHouseBrowseMenu:60`, `ConfirmPurchaseGui:58`,
`FilterGui:38`, `OrdersDeleteConfirmMenu:41`, `OrdersDeliverConfirmMenu:69`, `PlayerAuctionGui:40`,
`SpawnerMainMenu:38` (guard-branch fill), `TeamDisbandConfirmMenu:25` (BLACK→15),
`TeamKickConfirmMenu:37` (BLACK→15). The 12 helper-default lines and 51 config/default sites stay
deferred until `control()`/`button()` data-aware overloads or a config mapping layer are
authorized. **Batch 34 NOT STARTED.**

**Validation:** tree-sitter 0.26.0 runtime + tree-sitter-java 0.23.5 grammar (PyPI, self-tested
against injected malformed/unbalanced/duplicate cases), full-buffer parse of all **421** src/main
files — **421/421 clean**, 0 ERROR/MISSING, 0 delimiter imbalances, 0 conflict/javac-invalid
markers, **0 new** duplicate full-signature declarations in changed files (changed-file duplicate
sets byte-identical to baseline; pre-existing enum-constant `@Override` patterns unaffected),
`git diff --check` clean, 0 string-literal changes in the diff, 0 test/resource files touched.

**Build not verified — Maven/JDK unavailable.**

### Batch 35 — config-driven pane helper investigation (COMPLETE, analysis only)

Baseline `origin/master` `6515b492b7c0bc3b08295076ac28243e7d7e925b`. Read every helper, every caller and
every config boundary; proved a blind fallback swap is unsafe (`control()`/`button()` take plain
`Material`, build through the non-data-aware `createItem`, and feed `fallbackMaterial.name()` into the
config read). Re-inventoried the family with a real AST: **16 call sites / 17 pane arguments**
(`control()` 11 lines / 12 arguments, `button()` 5 / 5), not the 12 lines recorded by Batch 33/34.
Both helpers are read-only (no `set()`), and no shipped `auction-house.yml` / `orders.yml` key defines
`.MATERIAL` for these paths, so the fallback decides the stock render. Result: **0 source edits**; a
data-aware overload alone would preserve defaults but silently drop admin overrides that use 1.13+ pane
names, and a shared resolver needs ≥11 files. **Category C = IN PROGRESS.**

### Batch 36A — central legacy material compatibility layer (COMPLETE, 1 new file)

Added `utils/LegacyMaterialSupport.java` (261 lines) and nothing else: `public final class`, nested
immutable `Icon { Material material(); short data(); String configuredName(); }` (equality on
Material + data, `configuredName` is the serialization label), the frozen 16-colour pane table,
`pane(String)`, `of(Material)`, `isPaneName(String)`, `resolvePane(String)`, `resolve(String)`,
`resolve(String, Icon)` and `configName(ItemStack)`.

* Additive by contract: anything unrecognised returns `null` so every boundary keeps its own fallback
  (`Material.valueOf` → caller fallback, `ItemUtils.parseMaterial` → `STONE`, `CrateManager` → logged
  fallback). `ItemUtils.parseMaterial()`, `BaseMenu`, `CrateManager` and all config strings untouched.
* 1.12.2-safe by construction: the only executable `Material` reference is `STAINED_GLASS_PANE`
  (AST-verified — no 1.13+ constant anywhere in code, none needed), calls are limited to pre-1.13
  `matchMaterial` / `getDurability`, API level is Java 7 (`Short.valueOf`, `Collections.unmodifiableMap`,
  `Locale.ROOT`, explicit generic args, no lambdas/streams).
* Mapping = the 1.12.2 stained-glass-pane damage order (white 0, orange 1, magenta 2, light blue 3,
  yellow 4, lime 5, pink 6, gray 7, light gray 8 = 1.12.2 `SILVER`, cyan 9, purple 10, blue 11,
  brown 12, green 13, red 14, black 15), corroborated four independent ways: 95 already-migrated
  `STAINED_GLASS_PANE + (short)` sites (gray 7 ×62, black 15 ×22, red 14 ×7, lime 5 ×3, light gray 8 ×1);
  `STAINED_CLAY` sites (lime 5 on `&aConfirm`, red 14 on `&cCancel`); `INK_SACK` dye sites through the
  1.12.2 `dye = 15 − ordinal` inversion (dye 1 → red 14, dye 8 → gray 7, dye 10 → lime 5); and the
  16-entry colour lists in `filter.yml`, which map to exactly 0…15 ascending.
* Rejected as unproven: depending on `DyeColor.ordinal()` / `getWoolData()` (`LIGHT_GRAY` is `SILVER` in
  1.12.2 and the getters are deprecated), `NAME:DATA` config syntax, `minecraft:`/kebab normalization
  (no shipped material value uses them), a `SILVER_*` alias, an `of(Material, short)` factory, and the
  dye / terracotta / head / `*_GLASS` families (their own batches).
* Build still unverified: no JDK/Maven in the sandbox (apt, Maven Central and Adoptium are unreachable;
  PyPI only ships a JRE), so validation is tree-sitter + AST + a source-asserted semantic model.

### Batch 36B — config-driven pane helper migration (COMPLETE, 10 files)

Wired the layer into the two config-driven helper families and migrated all **17** pane arguments
(16 call sites) through it. Helper changes are purely additive:

* `AuctionHouseMenuSupport`: kept `control(…, Material, …)` with its resolution lines byte-identical
  (`getString(path + ".MATERIAL", fallback.name())` → `valueOf` → catch → fallback) so all 25 non-pane
  callers plus `ShulkerPreviewGui` keep the old behaviour; added `control(…, LegacyMaterialSupport.Icon, …)`
  = `resolve(getString(path + ".MATERIAL", fallbackIcon.configuredName()), fallbackIcon)` →
  `ItemUtils.createItem(material, data, name, lore)`. `.NAME` / `.LORE` handling was factored verbatim
  into `controlName(FileConfiguration, …)` / `controlLore(FileConfiguration, …)` shared by both overloads,
  reusing the already-resolved config (no second `getAuctionHouse()` call through the `localized(...)`
  language hook).
* `OrdersMenuSupport`: `material(…, Material)` and `button(…, Material, …)` byte-identical to master
  (CAULDRON / HOPPER / ARROW / MAP / OAK_SIGN / CHEST / DROPPER / HOPPER callers untouched); added
  `materialIcon(…, Icon)` + `button(…, Icon, …)` with the same read order.
* Overloads differ in exactly one parameter position with unrelated types (`Material` vs
  `LegacyMaterialSupport.Icon`), so no call site is ambiguous; both helper classes are package-private,
  so there are no external or reflective callers, and `src/test` references neither.
* Callers (8 files): 12 plain arguments → `LegacyMaterialSupport.pane("<COLOUR>")`; 4 ternaries became a
  **single Icon-level ternary** (`cond ? pane(X) : of(material)`) preserving the condition, the branch
  evaluation order and the one `getCategoryIcon(...)` / `Material.BARRIER` evaluation; the 4 `FILLER`
  sites route through new private Icon overloads of the menus' `control(…)` wrappers, keeping the Material
  wrappers for non-pane controls. `OrdersDepositMenu` lost the `org.bukkit.Material` import its last
  reference made unnecessary.
* Colour identity proved mechanically: per changed file the master `*_STAINED_GLASS_PANE` multiset equals
  the new `pane("…")` multiset (BLACK ×4, LIME ×8, RED ×4, GRAY ×1 = 17), with 0 residual pane constants in
  the 10 files and 0 changed config/display string literals (0 removed, only the 23 new colour/suffix
  literals added).
* Config semantics: missing key → same default string → same icon **with colour data**; valid 1.12.2 name
  → itself + data 0; modern pane name → `STAINED_GLASS_PANE` + colour data (previously lost); invalid,
  blank or `null` → caller fallback. `configuredName()` keeps the flattened 1.13+ name, so a future
  generator writes the same bytes admins already have and `configName(ItemStack)` round-trips them.
* Data 0 equivalence: the Icon path always uses the existing data-aware `createItem`, and
  `new ItemStack(m, 1, (short) 0)` is the same item the old 3-arg factory produced, so the mixed
  `pane(LIME) : of(icon)` ternaries keep non-pane branches byte-equivalent. No `ItemUtils` or `BaseMenu`
  change, no new ItemStack factory.
* After 36B: modern pane references in executable code total **51** = 28 `Material.` field accesses +
  23 config-default strings. The 47 GRAY/BLACK/RED/LIME refs are **25 field accesses** (`CrateManager` 13
  including its `set()` writer, `TeamDisbandConfirmMenu`/`TeamKickConfirmMenu`/`TeamEditMenu material()` 6,
  post-parse `if (x == null) x = Material.…` re-fallbacks 5, `StaffListMenu getMenuPlaceholderMaterial` 1)
  plus **22 config-default strings** (`BillfordMenu`, `ConfirmKillMenu`, `FeatureToggleMenu`,
  `ProfileViewerHomesMenu`, `PurchaseShopMenu`, `SellAllConfirmMenu`, `SellMenu`, `SellProgressMenu`,
  `ShopMenu`, `SpawnerMainMenu`, `SpawnerSellConfirmMenu`, `StatsWipe*`, `TpaQueueMenu`, `menus.yml`-driven
  paths). The other 4 refs are the still-deferred `LIGHT_GRAY` 2 (`HomeActionMenu` fill, `HomeMenu` blank),
  `LIGHT_BLUE` 1 (`FrozenPlayersMenu`) and `YELLOW` 1 (`SellProgressMenu`). Config-driven
  **helper-default pane arguments: 0 remaining**.
* Left for the serialization/config batch (deliberately **not** touched here): `CrateManager:116`
  writes `Material.BLACK_STAINED_GLASS_PANE.name()` into `crates.yml`, and `CrateManager:1243/1250` plus
  `ShopManager:1549` write `item.getType().name()` — a data-bearing pane would serialize as the bare
  `STAINED_GLASS_PANE` and lose its colour on reload; `LegacyMaterialSupport.configName(ItemStack)` exists
  exactly to close that round-trip.

**Validation (36A + 36B):** tree-sitter 0.26.0 + tree-sitter-java 0.23.5, full-buffer parse of all
**422** src/main files — 0 ERROR/MISSING, 0 delimiter imbalances, 0 duplicate full-signature
declarations, 0 javac-invalid markers (`var`, text blocks, records, switch arrows, `List/Set/Map.of`,
`isBlank`/`strip`, instanceof patterns, stream `.toList()` — all 86 `.toList()` occurrences are
`Collectors.toList()`), 0 unresolved names against the layer's declared API, every `pane("…")` colour
present in the table, `git diff --check` clean, no whitespace-only diff line. `Material.isAir()` = 103;
SGP data histograms (7 ×62 / 15 ×22 / 14 ×7 / 5 ×3 / 8 ×1), `STAINED_CLAY` 5 and 14, `INK_SACK` 16,
`WATCH` 5, `MOB_SPAWNER` 4, `BOOK_AND_QUILL` 4, `EYE_OF_ENDER` 1, `EXP_BOTTLE` 1, `GRASS` 1, skulls/heads,
terracotta, `Material.SPAWNER` 6 / `Material.CLOCK` 4, BlockData 44, PDC 58, `NamespacedKey` 34,
`Particle.` 19, `Sound.` 6, `EntityType` 31, ProtocolLib 55, Kyori 25, `Tag.` 9 and `parseMaterial(` 120
are all byte-identical to master; `src/main/resources`, `src/test`, `pom.xml` and the docs other than this
file untouched.

---

### Batch 37 — pane-safe Material serialization write-back (COMPLETE, 3 files)

Closed the round-trip 36B left open: the four config write sites that could drop a legacy pane colour.

* **Format decision — no new syntax.** The config format already stores one Material name per key, and
  the shipped resources prove the flattened 1.13+ pane aliases are that format's own spelling
  (`crates.yml` `FILLER: BLACK_STAINED_GLASS_PANE`, `LIME/RED_STAINED_GLASS_PANE` buttons,
  `shop.yml:672`, all 16 pane aliases in `worth.yml`, `menus.yml`). Colour therefore serializes as the
  alias, and `LegacyMaterialSupport.resolve(String)` is the reader. `NAME:DATA` syntax was rejected —
  no consumer needs it and `Material.valueOf`/`matchMaterial` parsers could not read it.
* `LegacyMaterialSupport.configName(ItemStack)`: pane branch now guarded by `getDurability() != 0`, so
  `STAINED_GLASS_PANE + 0` serializes as the bare `STAINED_GLASS_PANE` (both spellings resolve to the
  identical data-0 icon; `getType().name()` output stays byte-identical), while any non-zero colour
  serializes as its alias (`+ 15` → `BLACK_STAINED_GLASS_PANE`). Non-pane materials are untouched —
  `HOPPER`, `WATCH`, `INK_SACK` and `STAINED_CLAY` keep `type.name()` for every data value.
* `CrateManager:116` `Material.BLACK_STAINED_GLASS_PANE.name()` →
  `LegacyMaterialSupport.pane("BLACK").configuredName()` (writes the identical `BLACK_STAINED_GLASS_PANE`
  value with no modern enum constant); `CrateManager` reward `DISPLAY.MATERIAL` + `GRANT.MATERIAL` and
  `ShopManager` `shop.yml` `.MATERIAL` now write `configName(item)`. The authoritative item round trip
  stays `GRANT.ITEM-DATA` / `ITEM-DATA` (Bukkit-serialized); the Material column is the human-readable
  fallback and now survives a pane colour.
* **Classification (repo-wide sweep):** only these 4 sites are category A (ItemStack-derived or
  pane-constant config writes). Category B left alone: `CHEST`/`TRIPWIRE_HOOK`/`BARRIER`/`PAPER`/
  `SUNFLOWER`/`AMETHYST_SHARD` constant writes, `generateItemKey`, DB `requested_material_key`
  (`ItemKey` is a type-level matching key; `matches()` ignores data by design), `SpawnManager`
  template copy, `AmethystToolsManager` read default. Category C: all display/log/suffix-matcher
  `getType().name()` usages and the read-path parsers. The worth-key derivations
  (`ShopManager`/`WorthManager` `item.getType().name()`) are read-side matchers, flagged for a later
  batch to route through `configName`.
* **Round-trip proof** (harness extracts `PANE_DATA` from the real file and asserts the edited code
  fragments): `configName` table verified for SGP +7/+15/+14/+5/+0 and `HOPPER`/`WATCH`/`INK_SACK`/
  `STAINED_CLAY`; full cycle alias → `resolve` → Material+data → `ItemStack` → `configName` → alias
  proven for GRAY, BLACK, RED, LIME; `HOPPER`, `WATCH`, `STONE` and bare `STAINED_GLASS_PANE`
  round-trip unchanged; filler write ↔ `STAINED_GLASS_PANE + 15`; unknown/blank/null → `null` so every
  caller keeps its fallback semantics.
* **Validation:** tree-sitter 0.26.0 + tree-sitter-java 0.23.5, full-buffer parse of all **422**
  src/main files — 0 parse throws, 0 ERROR/MISSING, 0 delimiter imbalance; duplicate-declaration and
  javac-invalid marker scans byte-identical to pristine master `ee5872e` (the 9 baseline hits are
  scanner false-positives: varargs overload collapse, enum-constant method bodies, `AtomicBoolean;`
  import substring, `for(;;)`); unresolved-name scan on the 3 changed files identical to master;
  `git diff --check` clean. Protections: `Material.isAir()` = 103, SGP fill data 7 ×61 / 15 ×22 /
  8 ×1 / 14 ×3 / 5 ×2, `INK_SACK` 19, `WATCH` 7, `MOB_SPAWNER` 5, `BOOK_AND_QUILL` 5, `EYE_OF_ENDER` 2,
  `EXP_BOTTLE` 2, `GRASS` 1 — all pre/post identical; `src/main/resources`, `src/test`, no dyes,
  skulls, terracotta, `SPAWNER`/`CLOCK` or `LIGHT_GRAY`/`LIGHT_BLUE`/`YELLOW` changes.
* **Follow-up inventory (not started):** the ~47 config/default refs plus their read-path parsers
  (`CrateManager.parseMaterial` raw `valueOf`, the `Material.GRAY/LIME/RED/BLACK_STAINED_GLASS_PANE`
  fallback constants — need the Icon migration with data-carrying `DisplayItem`/`*MenuSettings`);
  worth-key derivation through `configName`; `ItemKey` pane-colour identity; `SUNFLOWER`/`AMETHYST_SHARD`
  name mapping.

---

Inventory only for remaining items. Batches 1–30 did not fully migrate Category C. `Material.isAir()` is
now **103 occ / 35 files**.

The single documented exception is **not** a migration: Batch 19's Java 8 `instanceof` conversion in
`utils/AdventureHeadComponentBridge.java` adds one explicit `(Component)` cast, which raises the
regex-counted `Adventure/Kyori` bucket from 65 → 66 occurrences (file count unchanged at 4, project total
524 → 525 across the same 62 files). No Adventure API was added, replaced or removed.

| Item | src/main occ | src/main files |
|---|---:|---:|
| Modern Materials (70 distinct constants not in the 1.12.2 enum) | 230 | 75 |
| `Material.isAir()` | 103 | 35 |
| PersistentDataContainer / PersistentDataType | 68 | 7 |
| ProtocolLib 5.x API | 66 | 8 |
| NMS via reflection strings | 64 | 5 |
| Adventure / Kyori | 63 | 11 |
| Modern entity classes (`TextDisplay`, `ItemDisplay`, …) | 59 | 4 |
| `NamespacedKey` | 25 | 8 |
| Modern Bukkit/Paper methods | 23 | 11 |
| `Particle` | 16 | 4 |
| `BlockData` | 15 | 3 |
| `org.bukkit.Tag` / `Registry` | 5 | 3 |
| `EquipmentSlot.OFF_HAND` · `Sound` · `EntityType` · `Attribute` | 2 · 2 · 2 · 1 | 1 · 2 · 2 · 1 |
| **Category C total** | **830** (+16 in 5 test files) | **144** |

Stained-glass panes alone account for 105 of the remaining modern-Material occurrences (103 true
target refs + 2 `LIGHT_GRAY_STAINED_GLASS_PANE` substring matches) and will remain the largest single
piece of the 1.12.2 API phase.

---

## Removed systems — must stay removed
Duels/FFA · Discord + Discord bot · Redis/network · Floodgate/Bedrock · SkinsRestorer · Lunar/Apollo ·
MongoDB · AuctionOrderBot.

## Preserved systems — verified present
AntiESP · SpawnStash · Shards · Amethyst Tools · FakePlayer · StaffMode · Shop · Auction House · Orders ·
Worth · Crates · Homes · RTP · Hide · all remaining core managers and menus.

## Next steps
1. ~~Repair corruption artifacts~~ — **done (Batches 13 + 16); 0 markers remain repo-wide.**
2. ~~Java 8 mechanical work in non-deferred files~~ — **done (Batches 14–18); non-deferred backlog is 0.**
3. ~~Small/medium deferred Java 8 APIs~~ — **done (Batch 19): `failedFuture`, `String.repeat`,
   `InputStream.readAllBytes`, `var`, 3 instanceof patterns.**
4. ~~Remaining deferred/complex Java 8 items~~ — **done:**
   - ~~text blocks~~ — **done (Batch 20).**
   - ~~`SellStatsExporter`~~ — **done (Batch 21).**
   - ~~`DatabaseManager` ×6 + `ShardManager` ×1 instanceof~~ — **done (Batch 22).**
   - ~~`DatabaseManager` undefined-symbol + `StatsWipeManager` `Enumjava` repairs~~ — **done (Batch 23).**
   - ~~`null`-from-primitive returns (7 sites)~~ — **done (Batch 24).**
   - ~~switch expressions (`LeaderboardManager` ×2, `TpaQueueMenu` ×1) and `ShardManager` `RandomGenerator`
     bounded `nextLong`~~ — **done (Batch 25).**
5. **Run a real build.** Java 8 compatibility is statically complete; **an actual Maven/JDK build has NOT yet
   been performed** in this environment (no `mvn`, no `javac`, no JVM). The first real `mvn -q compile` is the
   next verification milestone, and it may surface the known non-blocking defects listed above plus remaining
   Category C (Spigot 1.12.2 API) resolution errors.
6. ~~Spigot 1.12.2 simple Materials (Batch 27)~~ — **done (Batch 27).**
7. ~~Spigot 1.12.2 stained-glass panes (Batch 28)~~ — **done (Batch 28).**
8. ~~Spigot 1.12.2 dyes → `INK_SACK` + data (Batch 29)~~ — **done (Batch 29).**
9. ~~Spigot 1.12.2 stained-glass panes part 2 (Batch 30, 10 files / 23 refs)~~ — **done (Batch 30).**
10. ~~Spigot 1.12.2 direct pane ItemStack sites part 1 (Batch 31, 10 files / 10 sites)~~ — **done (Batch 31,
    this checkpoint).** 93 target pane refs remained: **29** direct ItemStack-creation sites (all
    `fill(Material)`) for later batches, **64** config parsing/default sites deliberately deferred to a
    dedicated config-compatibility batch, **0** comparisons.
11. ~~Spigot 1.12.2 direct pane fill sites part 2 (Batch 32, 10 files / 10 sites)~~ — **done (Batch 32,
    this checkpoint).** 82 true pane refs remain (83 constant occurrences): **31** direct
    ItemStack/fill-related sites (19 `fill(Material)` + 12 direct non-fill item creations) for later
    batches, **51** config/default sites deliberately deferred, **0** comparisons. `Material.isAir()`
    remains exactly **103**.
12. ~~Spigot 1.12.2 direct pane fill sites part 3 (Batch 33, 10 files / 10 sites)~~ — **done (Batch 33,
    this checkpoint).** 72 true pane refs remain (73 constant occurrences): **21** direct
    ItemStack/fill-related sites (9 `fill(Material)` + 12 protected helper-default lines — the
    `control()`/`button()` fallback-`Material` arguments are config-driven, not directly migratable)
    for later batches, **51** config/default sites deliberately deferred, **0** comparisons.
    `Material.isAir()` remains exactly **103**.
13. ~~Spigot 1.12.2 direct pane fill sites part 4 (Batch 34, 9 files / 9 sites)~~ — **done (Batch 34,
    this checkpoint).** The 9 remaining direct `fill(Material)` pane sites now use the data-aware
    `BaseMenu.fill(Material, short)`: 7 × `GRAY_STAINED_GLASS_PANE` → `STAINED_GLASS_PANE` + data 7 and
    2 × `BLACK_STAINED_GLASS_PANE` → `STAINED_GLASS_PANE` + data 15 (no RED/LIME direct fill sites
    existed). The **direct-fill subcategory for GRAY/BLACK/RED/LIME is COMPLETE (0 remain)**. 63 true
    pane refs remain (64 constant occurrences): **0** direct fill sites, **12** protected
    helper-default lines (the `AuctionHouseMenuSupport.control()` ×7 and `OrdersMenuSupport.button()` ×5
    fallback-`Material` arguments are config-driven, not directly migratable), **51** config/default
    sites deliberately deferred, **0** comparisons. `Material.isAir()` remains exactly **103**.
14. ~~Spigot 1.12.2 config-driven pane helper investigation (Batch 35)~~ — **done (analysis only, 0 source
    edits).** Re-inventoried the protected helper-default family with a real AST: **16 call sites / 17 pane
    arguments** (`control()` 11 / 12, `button()` 5 / 5), superseding the "12 lines" figure recorded by
    Batches 33–34. Verdict: no safe ≤10-file migration without a shared resolver; deferred to a
    compatibility-layer batch.
15. ~~Central legacy material compatibility layer (Batch 36A)~~ — **done.** Added
    `utils/LegacyMaterialSupport.java` (1 file, no caller churn): immutable `Icon` (Material + legacy data +
    `configuredName`), the 16-colour `STAINED_GLASS_PANE` damage table (gray 7, black 15, red 14, lime 5,
    light gray 8, …), `pane/of/resolve/resolvePane/isPaneName/configName`. Unknown values return `null` so
    every existing boundary keeps its own fallback semantics; only 1.12.2 Material constants are referenced
    in code; no new config syntax. Mapping validated against migrated repo sites, the `INK_SACK`
    `15 − ordinal` inversion, `STAINED_CLAY` sites and `filter.yml`'s 16-colour order.
16. ~~Config-driven pane helper migration (Batch 36B)~~ — **done (10 files, 17 arguments).**
    `AuctionHouseMenuSupport.control()` and `OrdersMenuSupport.button()` gained additive `Icon` overloads
    (old `Material` overloads byte-identical, 25 non-pane sites + both `material()` readers unaffected); the
    4 ternary sites became single Icon-level ternaries; the 4 `FILLER` sites route through new Icon wrapper
    overloads. Missing / valid-legacy / modern-pane / invalid / blank / null config cases and the per-file
    colour multisets were proved; 0 config strings changed; target-pane references in src/main 64 → 47.
17. Continue the Spigot 1.12.2 API phase. Next: the pane **config/default** sites (47 remaining:
    25 `Material.` field accesses — `CrateManager` ×13 incl. its `set()` writer, `Team* material()` ×6,
    re-fallback assignments ×5, `getMenuPlaceholderMaterial` ×1 — plus 22 config-default strings)
    on the same resolver, the deferred `LIGHT_GRAY` (2) / `LIGHT_BLUE` (1) /
    `YELLOW` (1) mappings, and the **config write-back serialization** fix (`CrateManager:116` writes
    `Material.BLACK_STAINED_GLASS_PANE.name()`; `CrateManager:1243/1250` and `ShopManager:1549` write
    `item.getType().name()`, which loses pane colour — `LegacyMaterialSupport.configName(ItemStack)` is the
    intended remedy). Then the remaining modern dye/head/1.13+ items, 103 `isAir()` sites,
    `org.bukkit.Tag`, and a real `mvn -q compile` (JDK/Maven still unavailable here).
    BlockData / PDC / NMS / ProtocolLib / Adventure stay deferred. **Category C = IN PROGRESS.**

> **Build not verified — Maven/JDK unavailable.** All validation is tree-sitter + static scans only.
> Java 8 compatibility is statically complete; real javac/Maven verification remains.

## Runtime bug-fix batch (1.12.2 server log findings)

Fixed against the real 61,664-line Spigot 1.12.2 server log, building on commit 6e325df:

1. **Scoreboard spam (`Display name ... longer than the limit of 32 characters`).**
   New `utils/LegacyScoreboardText` converts modern RGB/hex markup (`§x§R§R§G§G§B§B`,
   `&#RRGGBB`, `{#…}`, `<#…>`, bare `#…`) to the nearest legacy `ChatColor` (perceptual
   redmean distance), collapses redundant codes, and token-safely truncates (never splits a
   colour code or surrogate pair). Applied to both objective display-name paths in
   `ScoreboardManager` (32-char limit) and the `TablistManager` legacy fallback
   (`setPlayerListName`, 16-char limit). `applyLineSpigot` now splits team lines at the real
   1.12.2 prefix/suffix limit (16 each, not 64) and re-opens the colour state in the suffix.
2. **SQLite `64 values for 57 columns`.** `savePlayer` wrote 57 columns with 64 placeholders and
   bindings that skipped indexes 39/49 and were shifted past them. The statement is now the full
   62-column schema order with 62 placeholders and sequential bindings; `loadPlayer`/`mapPlayerRow`
   were already correct and stay untouched. `ensurePlayerColumns` additionally migrates the three
   pre-`blocks_*` columns it never covered (`tpauto`, `phantom_enabled`, `payments_enabled`), so a
   database last written by an older build is saveable too. Verified against a real SQLite engine
   (fresh schema round-trip, REPLACE semantics, legacy-table migration, column/order parity), and
   the stale `duel_music_enabled` leftovers were removed from `DatabaseManagerPlayerSettingsTest`.
3. **`PlayerJoinEvent EventException: null`.** Root causes were the unhandled exceptions above
   inside the join flow (scoreboard setup throws first, tab-list fallback next). Both fixed at
   the source; no suppression added.
4. **Orders material lookup.** `filter.yml` is written in modern names and `FilterManager` used a
   bare `Material.matchMaterial`, silently starving the catalog on 1.12.2; it now resolves through
   `LegacyMaterialSupport`, which gained the flattened colour/wood/stone families, tool/food/door
   renames and disc/music-cart renames the shipped configs use. `OrdersManager.resolveMaterial`
   no longer degrades unresolvable names to STONE (STONE was being misclassified as combat); the
   firework comparison uses the real `FIREWORK_ROCKET`/`FIREWORK` pair. `/order` registered as an
   alias of `/orders`. Unsupported materials stay unsupported (null) by design.
5. **Post-1.12.2 API in touched classes.** `ItemKey` and `SpawnerBlockListener` referenced
   `org.bukkit.inventory.meta.Damageable` (1.13+); replaced with the 1.12.2-native durability
   check/write (`getMaxDurability() > 0 && getDurability() > 0` / `setDurability`), preserving
   semantics. `Material.isItem()` was verified present in 1.12.2-R0.1 and kept.

Validation in this environment (Maven/JDK repos unreachable): Janino-compiled execution of
`LegacyScoreboardText` (31 checks) and `LegacyMaterialSupport` against a stub Material enum that
contains only the genuine 1.12.2 constants (88 checks, incl. "all 300 filter.yml/orders.yml names
resolve" and "no STONE fallback"), the `applyLineSpigot` split guarantees (6 checks), a real
SQLite validation of the exact extracted SQL (13 checks), and a Janino parse of every edited
file. Repo-side JUnit coverage: `LegacyScoreboardTextTest`, `LegacyMaterialSupportTest`, updated
`ScoreboardLineSplitTest`, repaired `DatabaseManagerPlayerSettingsTest` (round-trips every
persisted player field). `mvn -q -DskipTests compile` / `mvn clean package -Dmaven.test.skip=true`
must still be run where Maven and the Spigot 1.12.2 API are reachable; no real 1.12.2 server JAR
was available here for a live test.

## Continuation: full-tree 1.12.2 API audit, search fix, mechanized DB proof

1. **Full-tree API audit (Task A).** All 149 distinct `org.bukkit.*` imports inventoried and
   verified against the 1.12.2-R0.1 API: no `block.data`, no post-1.12.2 entity classes (the one
   `entity.*` wildcard resolves only Entity/Item/Monster/Player), no modern inventory.meta (the
   `Damageable` references were removed in the previous batch), `getClickedInventory()` was
   verified present in 1.12.2 (not a modern API - do not "fix" it), `SoundCategory`,
   `Player#sendTitle`, `Enchantment#getName` are all 1.12-era. Every `Material.*` constant used
   anywhere in `src/main` (146 distinct) was checked against the genuine 1.12.2 enum constant
   list extracted from the 1.12.2 javadoc: zero mismatches; likewise the Sound/Particle/Effect/
   PotionType constants (all 1.12-era names). NMS/Adventure access is reflection-only
   (TablistComponentUpdater's Paper route reflects `playerListName(Component)` and fails closed
   to the now-sanitised `setPlayerListName` fallback), ProtocolLib paths catch `LinkageError`,
   and `ScoreboardNumberHider` disables after one warning line. No PDC/Tag/Registry/loot API
   is referenced.
2. **Orders search root-cause completion (Tasks B/E).** `ItemKey.deserialize` still resolved
   stored `requested_material_key` potion/enchanted/plain names with a bare
   `Material.matchMaterial`, bypassing the central layer; it now resolves through
   `LegacyMaterialSupport` (unresolvable stays null -> AIR, exactly as before, never a fallback
   material). Search also failed for modern-name queries: catalog entries carry the 1.12.2
   material name, so `/orders oak_door` only ever matched `DARK_OAK_DOOR_ITEM` by substring.
   `OrdersManager.resolveSearchMaterial(query)` now resolves a query once through the central
   layer and both search paths match by material equality: the catalog search
   (`getCatalogEntries`) and the browse/my-orders board searches (`OrdersBrowseMenu`,
   `OrdersMyOrdersMenu`). `/order` is an alias of `/orders` in plugin.yml and shares the same
   `PluginCommand`/executor, so both reach the identical flow.
3. **Mechanized SQLite proof (Task D).** `db_binding_check.py` parses the Java source and proves
   mechanically: 62 columns = 62 placeholders; binding indexes are exactly 1..62 with no gaps
   (the original bug had gaps at 39 and 49); every column's setter type matches the schema type;
   `mapPlayerRow` reads every saved column and no unsaved column; and every binding expression
   names the same PlayerData field the reader loads into that column (whitelisted intentional
   asymmetry: `getTotalPlaytimeSeconds()` saves the live-inclusive total while the reader
   restores the base `playtimeSeconds`; `uuid`/`username` are constructor arguments).
4. **Boundary tests (Task F).** LegacyScoreboardText now has explicit exactly-32 / exactly-16 /
   one-over / 31-chars-plus-code / 14-chars-plus-emoji boundary cases in both the sandbox suite
   (42 checks green) and the repo JUnit test.
5. **Eaglercraft/EaglerXServer (Task G).** No Eagler-specific code exists or is needed:
   EaglerXServer presents Eagler clients as ordinary 1.12.2 protocol players. The only plugin
   message channel is `BungeeCord` (maintenance lobby redirect) with a kick fallback; the join
   flow is null-safe around `getAddress()`; every change made in this batch only constrains
   strings and materials to what the vanilla 1.12.2 protocol accepts (scoreboard 32/16 limits,
   tab-list 16 limit, real 1.12.2 materials), which is exactly what an Eaglercraft 1.12.2
   client receives through EaglerXServer. Inventory GUIs, chat, commands, shops, orders,
   auction house and persistence all ride the normal Bukkit API and are unchanged in shape.

Validation rerun after these changes (Maven and all Maven repositories remain unreachable from
this sandbox, so `mvn` could not be executed): 6 suites, 175 checks, all green - sandbox
compiler runs of LegacyScoreboardText (42), LegacyMaterialSupport against a faithful 1.12.2
enum stub (92), applyLineSpigot split guarantees (6), Orders search end-to-end against the
shipped filter.yml (15), real-SQLite schema/migration/round-trip (13), and the mechanized
savePlayer binding proof (7). Janino parse of all 16 touched files: 0 failures.

## Continuation 2: real-file compiles caught a missing import

Compiling the real `ItemKey.java` (not a copy) against 1.12.2-faithful stubs caught that the new
`resolveStoredMaterial` used the simple name `LegacyMaterialSupport.Icon` without importing it -
a genuine `mvn` build breaker. Fixed by importing `com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport`
in ItemKey. The same compile pass runs the real `deserialize` on legacy keys (STONE), modern keys
(OAK_DOOR -> WOOD_DOOR, GRASS_BLOCK -> GRASS), potion/enchanted keys, book keys and junk
(junk -> AIR, never STONE): 10/10 checks green. Janino cannot compile lambda expressions, so the
ItemKey compile uses a mechanically transformed copy in which the four baseline lambda-stream
blocks become equivalent loops (`validation/make_itemkey_copy.py` verifies every region changed
by this bug-fix batch is byte-identical in the compiled copy). `FilterManager`'s changed resolve
loop also compiles as the real file; its baseline `getOrDefault` line trips Janino's weaker
generics inference only (javac-legal, left untouched). A handful of javac-identical explicit
casts were added in ItemKey where Janino erases `Map.Entry` generics.

Full validation state: 7 suites, 185 checks, all green (LegacyScoreboardText 42,
LegacyMaterialSupport 92, applyLineSpigot 6, Orders search 15, ItemKey 10, SQLite SQL/migration
13, binding alignment 7); Janino parse of all touched files: 0 failures. What could NOT run
anywhere I control: `mvn -q -DskipTests compile`, `mvn clean package -Dmaven.test.skip=true`,
`mvn clean test` and a real 1.12.2 server boot - this sandbox has no Maven/JDK distribution and
no route to Maven repositories (only PyPI is reachable), and GitHub Actions on the repository is
not enabled for the bot token (workflow files exist on master but are not registered; the API
returns 403 for workflow permissions), so the packaged JAR could not be produced or inspected
here. The repo-side JUnit suites (LegacyScoreboardTextTest, LegacyMaterialSupportTest,
ScoreboardLineSplitTest, DatabaseManagerPlayerSettingsTest, PlayerSettingUtilsTest) are ready to
run unchanged under `mvn clean test` wherever Maven and the Spigot 1.12.2 API are reachable.

---

## Batch 38 — 1.12.2 runtime/gameplay audit (this checkpoint)

Scope: runtime compatibility audit + fixes for Spigot 1.12.2 and Eaglercraft (EaglerXServer), not a
Java-8 syntax pass. Full evidence and per-bug tracing: `docs/1.12.2_RUNTIME_AUDIT.md`.
Test matrix: `docs/1.12.2_TEST_MATRIX.md`. Build/validation notes: `docs/1.12.2_VALIDATION.md`.

| # | Fix | Files |
|---|---|---|
| 1 | ProtocolLib `WrappedEnumEntityUseAction` static-init crash — read the use-action field generically instead | `managers/FakePlayerProtocolLibBridge.java` |
| 2 | `Material.SIGN` is a sign **item** on 1.12.2, never a block → sign GUIs could not open | `utils/SignInputUtil.java` |
| 3 | `Player#openSign(Sign, Side)` is 1.20+; pre-1.20 servers now use the shared **chat input** transport (orders price/amount, /ah search, /orders search) | `utils/SignInputUtil.java`, `listeners/ChatListener.java` |
| 4 | Tablist: NMS `PacketPlayOutPlayerInfo` / `IChatBaseComponent` now resolve through the relocated `net.minecraft.server.v1_12_R1` package; 16-char fallback keeps the player name instead of the badge | `utils/NmsSupport.java` (new), `utils/TablistComponentUpdater.java`, `utils/LegacyScoreboardText.java`, `managers/TablistManager.java` |
| 5 | `SkullMeta#setOwningPlayer` (1.13+) → shared `ItemUtils#applyOwnerToSkullMeta` | `utils/ItemUtils.java` + 7 menus |
| 6 | Worth catalog: 1.13+ `worth.yml` keys now resolve onto 1.12.2 material + durability | `managers/WorthManager.java` |
| 7 | `LeaderboardManager.@Nullable LeaderboardEntry` type annotation → ordinary parameter annotation | `api/LeaderboardPlaceholderResolver.java` |

**Still not verified: compilation.** No JDK and no Maven, and every Maven repository is unreachable
from this environment — see `docs/1.12.2_VALIDATION.md` for the exact commands and for the static
checks that were run instead (424/424 files parse; 0 missing Bukkit classes, event types, Material /
Sound / Particle / GameMode / InventoryType constants against a real 1.12.2 API surface).
