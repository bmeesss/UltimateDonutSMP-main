# UltimateDonutSMP — Spigot 1.12.2 / Java 8 Migration Status

Target (from `pom.xml`): Java 8 (`maven.compiler.source/target = 1.8`), Spigot API `1.12.2-R0.1-SNAPSHOT`,
ProtocolLib 5.3.0 (provided), Adventure MiniMessage 4.14.0 (shaded), Vault/LuckPerms/PlaceholderAPI (provided),
SQLite/MySQL/HikariCP (bundled).

Source files: **421 src/main + 69 src/test**.

## Phase overview

| Phase | Status |
|---|---|
| Structural repair / parser cleanup | ✅ **COMPLETE** |
| Java 8 — Batch 1 | ✅ **COMPLETE / MERGED** (PR #10, commit `d004861`) |
| Java 8 — Batch 2 | ✅ **COMPLETE / MERGED** (PR #11) |
| Java 8 — Batch 3 | ✅ **COMPLETE / MERGED** (PR #11) |
| Java 8 — Batches 4–8 | ✅ **COMPLETE / MERGED** (PR #12, commit `1c288fb`) |
| Java 8 — Batches 9–11 | ✅ **COMPLETE / MERGED** (PR #13, commit `5eb81a0`) |
| Java 8 — Batch 12 | ✅ **COMPLETE** (this checkpoint) |
| Java 8 — remaining batches | ⏳ IN PROGRESS (see inventory below) |
| Spigot 1.12.2 API migration (Materials / BlockData / PDC / Particle / Sound / entities) | ⛔ **NOT STARTED** |
| NMS / ProtocolLib runtime audit | ⛔ **NOT STARTED** |
| Adventure runtime compatibility | ⛔ **NOT STARTED** |

## Structural health (verified this checkpoint)

- **421/421** `src/main` files parse clean (tree-sitter-java: 0 ERROR nodes, 0 MISSING nodes)
- **0** delimiter imbalance (brace / paren / bracket, comment- and string-aware)
- **0** new duplicate methods, constructors, classes or fields introduced by Batch 12 (per-file HEAD-vs-worktree declaration-count delta scan)
- `git diff --check` clean

`src/test` has 3 pre-existing parse failures inherited from before the Java 8 phase; no test file has been
modified in Batches 1–12.

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

## Remaining Java 8 inventory (verified scanner result, this checkpoint — after Batch 12)

**103 occurrences across 44 unique `src/main` files** (124 → 103; 54 → 44 files).

### Category A — Java language

| Construct | src/main occ | src/main files |
|---|---:|---:|
| instanceof pattern matching | 70 | 39 |
| `var` declarations (all in deferred `OrdersManager`) | 4 | 1 |
| switch-in-expression-position (colon/break form, in deferred `LeaderboardManager` ×2 / `TpaQueueMenu` ×1) | 3 | 2 |
| text blocks | 13 | 4 |
| **Category A total** | **90** | — |

### Category B — JDK 9+ standard library

| API | src/main occ | src/main files |
|---|---:|---:|
| `StringBuilder.isEmpty()` (in deferred `SellStatsExporter`) | 1 | 1 |
| `String.repeat()` (in deferred `ConfigManager`) | 2 | 1 |
| `RandomGenerator` (in deferred `ShardManager`) | 3 | 1 |
| `java.net.http.*` (in deferred `SellStatsExporter`) | 3 | 1 |
| `CompletableFuture.failedFuture()` (in deferred repositories) | 2 | 2 |
| `InputStream.readAllBytes()` (in deferred `ConfigManager`; 2 call sites) | 1 | 1 |
| `Executors.newVirtualThreadPerTaskExecutor()` (in deferred `SellStatsExporter`) | 1 | 1 |
| **Category B total** | **13** | — |

Verified **zero** occurrences remain of: `Stream.toList()`, `String.isBlank()`, `String.strip()`,
`List/Set/Map.of` in src/main, `List/Set/Map.copyOf`, `List.getFirst()`, `Path.of()`,
`Objects.requireNonNullElse`, `toArray(IntFunction)`.

### Pre-existing corruption artifacts still present (javac-invalid; discovered in Batch 12 — fix in a near-term batch)

| File | Line | Artifact |
|---|---|---|
| `commands/AuctionHouseCommand.java` | 443 | orphaned `String;` (mangled `var key = switch(…){…}`) |
| `menus/SellGui.java` | 220 | orphaned `String;` |
| `commands/SocialCommand.java` | 36 | orphaned `String;` |
| `managers/LuckPermsTablistRefreshBridge.java` | 428 | orphaned `Boolean;` |
| `managers/KeyAllManager.java` | 186 | `return break;` + bare ternary statement |
| `commands/CuboidCommand.java` | 213 | unreachable `break; break;` after `return;` |

(`menus/FriendsMenu.java` continue/break interleaving was inspected and is valid Java — not an artifact.)

### Deferred within the Java 8 phase (need design, not mechanical edits)
`DatabaseManager` / `OrdersManager` / `AuctionHouseRepository` / `ShopPreferenceRepository` text blocks ·
`SellStatsExporter` `java.net.http` + virtual threads + `StringBuilder.isEmpty()` · `ConfigManager.readAllBytes()` ·
`LeaderboardManager` + `TpaQueueMenu` switch-in-expression-position · `ShardManager` `RandomGenerator`
(`nextLong(origin, bound)` needs a Java 8 bounded-long reimplementation — not trivial).

---

## Category C — Spigot 1.12.2 API migration: NOT STARTED

Inventory only. **No Category C item has been modified in Batches 1–12.** `Material.isAir()` remains
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
1. Repair the 6 remaining pre-existing corruption artifacts (AuctionHouseCommand, SellGui, SocialCommand,
   LuckPermsTablistRefreshBridge, KeyAllManager, CuboidCommand — see table above).
2. Java 8 remaining mechanical work: instanceof (70), `var` (4, inside deferred `OrdersManager`),
   `StringBuilder.isEmpty()` in `SellStatsExporter` (1), `String.repeat()` in `ConfigManager` (2).
3. Handle the deferred design-level Java 8 items (text blocks, `java.net.http`, switch-in-expression-position,
   `ConfigManager.readAllBytes()`, `failedFuture`, `ShardManager` `RandomGenerator`).
4. Only then begin the Spigot 1.12.2 API phase (Materials first).
