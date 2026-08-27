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
| Java 8 — Batches 4–8 | ✅ **COMPLETE** (this checkpoint) |
| Java 8 — remaining batches | ⏳ IN PROGRESS (see inventory below) |
| Spigot 1.12.2 API migration (Materials / BlockData / PDC / Particle / Sound / entities) | ⛔ **NOT STARTED** |
| NMS / ProtocolLib runtime audit | ⛔ **NOT STARTED** |
| Adventure runtime compatibility | ⛔ **NOT STARTED** |

## Structural health (verified this checkpoint)

- **421/421** `src/main` files parse clean (tree-sitter-java: 0 ERROR nodes, 0 MISSING nodes)
- **0** delimiter imbalance (brace / paren / bracket, comment- and string-aware)
- **0** new duplicate methods, constructors, classes or fields introduced by Batches 4–8
- `git diff --check` clean

`src/test` has 3 pre-existing parse failures inherited from before the Java 8 phase; no test file has been
modified in Batches 1–8.

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

---

## Remaining Java 8 inventory (verified scanner result, this checkpoint)

### Category A — Java language

| Construct | src/main occ | src/main files | src/test occ | src/test files |
|---|---:|---:|---:|---:|
| instanceof pattern matching | 109 | 67 | 6 | 2 |
| `var` declarations | 24 | 16 | 5 | 4 |
| switch expressions | 3 | 2 | 0 | 0 |
| text blocks | 13 | 4 | 18 | 5 |
| arrow switch cases / records / sealed types | **0** | 0 | 0 | 0 |
| **Category A total** | **149** | — | **29** | **11** |

### Category B — JDK 9+ standard library

| API | src/main occ | src/main files | src/test occ | src/test files |
|---|---:|---:|---:|---:|
| `StringBuilder.isEmpty()` (15) | 6 | 5 | 0 | 0 |
| `String.repeat()` (11) | 4 | 2 | 12 | 2 |
| `RandomGenerator` (17) | 3 | 1 | 0 | 0 |
| `Collection.toArray(IntFunction)` (11) | **0** | 0 | 0 | 0 |
| `java.net.http.*` (11) | 3 | 1 | 0 | 0 |
| `CompletableFuture.failedFuture()` (9) | 2 | 2 | 0 | 0 |
| `InputStream.readAllBytes()` (9) | 1 | 1 | 0 | 0 |
| `Executors.newVirtualThreadPerTaskExecutor()` (21) | 1 | 1 | 0 | 0 |
| `List.getFirst()` — SequencedCollection (21) | **0** | 0 | 1 | 1 |
| `Path.of()` (11) | 0 | 0 | 19 | 12 |
| `Files.readString/writeString` (11) | 0 | 0 | 4 | 3 |
| `String.lines()` (11) | 0 | 0 | 1 | 1 |
| **Category B total** | **20** | — | **37** | **16** |

Verified **zero** occurrences: `Stream.toList()` (remaining `.toList()` are `Collectors.toList()`),
`String.isBlank()` (the only `isBlank` is a project method in `CuboidCommand`), `String.strip()`,
`List/Set/Map.of` in src/main, `List/Set/Map.copyOf` (all `copyOf` are `Arrays.copyOf*` / `EnumSet.copyOf`),
`Map.ofEntries/entry`, `Objects.requireNonNullElse`, `Optional` 9+ methods, `Predicate.not`,
`Collectors.toUnmodifiable*`, `Stream.takeWhile/dropWhile/ofNullable`. All `.reversed()` calls checked
are `Comparator.reversed()` (Java 8).

**Combined A + B remaining: 169 occurrences across 84 unique `src/main` files** (plus test-file occurrences,
deliberately deferred — tests are not on the shaded-jar compile path).

### Deferred within the Java 8 phase (need design, not mechanical edits)
`DatabaseManager` / `OrdersManager` / `AuctionHouseRepository` / `ShopPreferenceRepository` text blocks ·
`SellStatsExporter` `java.net.http` + virtual threads · `ConfigManager.readAllBytes()` ·
`LeaderboardManager` + `TpaQueueMenu` switch expressions.

---

## Category C — Spigot 1.12.2 API migration: NOT STARTED

Inventory only. **No Category C item has been modified in Batches 1–8.** `Material.isAir()` remains
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
1. Java 8 remaining mechanical work: instanceof (109), `var` (24), `StringBuilder.isEmpty()` (6),
   `String.repeat()` (4), `RandomGenerator` in `ShardManager` (3).
2. Handle the deferred design-level Java 8 items (text blocks, `java.net.http`, switch expressions).
3. Only then begin the Spigot 1.12.2 API phase (Materials first).
