# UltimateDonutSMP — Spigot 1.12.2 / Java 8 Migration Status

Target (from `pom.xml`): Java 8 (`maven.compiler.source/target = 1.8`), Spigot API `1.12.2-R0.1-SNAPSHOT`,
ProtocolLib 5.3.0 (provided), Adventure MiniMessage 4.14.0 (shaded), Vault/LuckPerms/PlaceholderAPI (provided),
SQLite/MySQL/HikariCP (bundled).

Source files: **421 src/main + 69 src/test**.

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
| Java 8 — Batch 17 (10 files, 10 instanceof) | ✅ **COMPLETE** (this checkpoint) |
| Java 8 — Batch 18 (4 files, 5 instanceof) | ✅ **COMPLETE** (this checkpoint) — **non-deferred A+B backlog now 0** |
| Java 8 — remaining batches | ⏳ **DEFERRED/COMPLEX ONLY** (see inventory below) |
| Spigot 1.12.2 API migration (Materials / BlockData / PDC / Particle / Sound / entities) | ⛔ **NOT STARTED** |
| NMS / ProtocolLib runtime audit | ⛔ **NOT STARTED** |
| Adventure runtime compatibility | ⛔ **NOT STARTED** |

## Structural health (verified this checkpoint)

**Baseline:** recovered from GitHub master `138898fb3fd94b78665f82e71129f5d09546d2cf` (tree `ef615ae1`).
The earlier local migration commit `b2e898b` ("…186 files") had introduced **34** tree-sitter-ERROR files and
was **discarded as the migration base**; master `138898fb` parses 421/421 clean.

- **421/421** `src/main` files parse clean (tree-sitter-java 0.23.5, **explicit large buffer** — the native
  default buffer silently throws on large files; a scanner that treats a throw as "pass" is a false negative)
- **0** delimiter imbalance (brace / paren / bracket, comment- and string-aware)
- **0** true duplicate methods/constructors (same name **and** full parameter-type signature)
- `git diff --check` clean
- Batch 13 removed the 6 javac-invalid corruption artifacts (tree-sitter-tolerated, javac-invalid) — see below

`src/test` has 3 pre-existing parse failures inherited from before the Java 8 phase; no test file has been
modified in Batches 1–16.

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
`DatabaseManager` / `OrdersManager` / `AuctionHouseRepository` / `ShopPreferenceRepository` text blocks ·
`SellStatsExporter` `java.net.http` + virtual threads + `StringBuilder.isEmpty()` · `ConfigManager.readAllBytes()` ·
`LeaderboardManager` + `TpaQueueMenu` switch-in-expression-position · `ShardManager` `RandomGenerator`
(`nextLong(origin, bound)` needs a Java 8 bounded-long reimplementation — not trivial).

---

## Current Java 8 inventory (verified this checkpoint, corrected type-aware scanner, post-Batch-18)

**53 raw scanner hits / 8 src/main files** — equivalently **39 semantic issues** (each text block counts
2 delimiter hits: 13 blocks = 26 hits; and 1 hit is the known `SidebarSettings.lines()` false positive).
**Every remaining issue is in a deferred file or deferred category. The non-deferred mechanical backlog is 0.**

| Construct | Hits | Where (all deferred) |
|---|---:|---|
| text blocks | 26 (13 blocks) | `DatabaseManager` (5), `OrdersManager` (4), `AuctionHouseRepository` (3), `ShopPreferenceRepository` (1) |
| instanceof patterns | 9 | `DatabaseManager` (6), `OrdersManager` (2), `SellStatsExporter` (1) |
| `var` | 4 | `OrdersManager` |
| `java.net.http.*` | 3 | `SellStatsExporter` |
| `RandomGenerator` | 3 | `ShardManager` (bounded `nextLong` — needs Java 8 reimplementation) |
| `String.repeat` | 2 | `ConfigManager` |
| `InputStream.readAllBytes` / `Files.readAllBytes` | 2 | `ConfigManager` (the `Files.readAllBytes` site is Java 7 — verify before touching) |
| `CompletableFuture.failedFuture` | 2 | `AuctionHouseRepository`, `ShopPreferenceRepository` |
| `String.lines` | 1 | **false positive** — `SidebarSettings.lines()` custom type |
| virtual threads | 1 | `SellStatsExporter` |

Verified **zero** genuine occurrences of: bare `Stream.toList()` (all 78 `.toList()` hits are
`Collectors.toList()`), `String.strip()` (all 46 hits are project `ColorUtils.strip(...)`),
`String.isBlank()`, `List/Set/Map.of/copyOf`, `List.getFirst()`, `Path.of()`, `StringBuilder.isEmpty()`,
`Objects.requireNonNullElse`, `toArray(IntFunction)`.

**javac-invalid corruption cleanup: COMPLETE — 0 markers remain repo-wide** (comment/string-aware scan of
all 421 src/main files: `return break;`, duplicate-`break;` runs, orphaned type declarations).

Structural: **421/421 src/main files parse clean** (tree-sitter-java, explicit large buffer, ERROR/MISSING
treated as failures). Delimiter balance and full-signature duplicate scans clean on all changed files.

---

## Category C — Spigot 1.12.2 API migration: NOT STARTED

Inventory only. **No Category C item has been modified in Batches 1–16.** `Material.isAir()` remains
**108 occ / 39 files**, matching the pre-Java-8 baseline for that item.

| Item | src/main occ | src/main files |
|---|---:|---:|
| Modern Materials (76 distinct constants not in the 1.12.2 enum) | 334 | 92 |
| `Material.isAir()` | 108 | 39 |
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
| **Category C total** | **853** (+16 in 5 test files) | **144** |

Stained-glass panes alone account for 146 of the modern-Material occurrences and will be the largest single
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
3. Handle the deferred design-level Java 8 items (text blocks in SQL schema strings, `java.net.http` +
   virtual threads in `SellStatsExporter`, `ConfigManager` `readAllBytes`/`repeat`, `OrdersManager`
   `var`/instanceof, `DatabaseManager` instanceof, `failedFuture` in repositories, `ShardManager`
   `RandomGenerator` bounded-long reimplementation).
4. Only then begin the Spigot 1.12.2 API phase (Materials first). **Category C: NOT STARTED.**

> **Build not verified — Maven/JDK unavailable.** All structural validation is tree-sitter + static scans only.
