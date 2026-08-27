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
| Spigot 1.12.2 API — Batch 30 (stained-glass panes part 2, 10 files) | ✅ **COMPLETE** (this checkpoint) |
| Spigot 1.12.2 API migration (remaining Materials / BlockData / PDC / Particle / Sound / entities) | 🚧 **IN PROGRESS** (Batches 26–30; later batches not started) |
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

## Category C — Spigot 1.12.2 API migration: IN PROGRESS (Batches 26–30)

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

**Category C status:** **IN PROGRESS** — Batches 26–30 are complete; the remaining config-driven
pane/dye/head/1.13+ items and the rest of Category C (BlockData, PDC, NamespacedKey, ProtocolLib, NMS,
Adventure, Particle, Sound, EntityType) remain for later batches.

**Build not verified — Maven/JDK unavailable.**

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
10. Continue the Spigot 1.12.2 API phase (Materials / `isAir` remaining files). **Do not start Batch 31 in
    this checkpoint.** The 39 remaining direct pane ItemStack-creation sites (34 `fill`, 5 create*), the
    config-driven pane compatibility layer, and the remaining modern dye/head/1.13+ items plus 103
    `isAir()` sites are later batches. BlockData / PDC / NMS / ProtocolLib stay deferred.

> **Build not verified — Maven/JDK unavailable.** All validation is tree-sitter + static scans only.
> Java 8 compatibility is statically complete; real javac/Maven verification remains.
