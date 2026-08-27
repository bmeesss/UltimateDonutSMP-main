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
| Java 8 — Batch 2 | ✅ **COMPLETE** |
| Java 8 — Batch 3 | ✅ **COMPLETE** |
| Java 8 — remaining batches | ⏳ IN PROGRESS (see inventory below) |
| Spigot 1.12.2 API migration (Materials / BlockData / PDC / Particle / Sound / entities) | ⛔ **NOT STARTED** |
| NMS / ProtocolLib runtime audit | ⛔ **NOT STARTED** |
| Adventure runtime compatibility | ⛔ **NOT STARTED** |

## Structural health (verified this checkpoint)

- **421/421** `src/main` files parse clean (tree-sitter-java: 0 ERROR nodes, 0 MISSING nodes)
- **0** delimiter imbalance (brace / paren / bracket, comment- and string-aware)
- **0** duplicate methods, constructors, classes or fields (varargs-aware signature comparison)
- `git diff --check` clean

`src/test` has 3 pre-existing parse failures inherited from before the Java 8 phase; no test file has been
modified in Batch 1, 2 or 3.

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

Conversion rules applied in Batches 2–3: short-circuit order and control flow preserved; bindings re-declared
after the guard so scope is unchanged; where the `instanceof` operand was a snapshot-producing call
(`block.getState()`, `bsm.getBlockState()`, `item.getItemMeta()`, `event.getWhoClicked()`, `event.getEntity()`)
a local now holds the **single** invocation instead of the cast re-invoking it.

---

## Remaining Java 8 inventory (verified scanner result, this checkpoint)

### Category A — Java language

| Construct | src/main occ | src/main files | src/test occ | src/test files |
|---|---:|---:|---:|---:|
| instanceof pattern matching | 140 | 98 | 6 | 2 |
| `var` declarations | 35 | 24 | 5 | 4 |
| switch expressions | 3 | 2 | 0 | 0 |
| text blocks | 13 | 4 | 18 | 5 |
| arrow switch cases / records / sealed types | **0** | 0 | 0 | 0 |
| **Category A total** | **191** | **118** | **29** | **11** |

### Category B — JDK 9+ standard library

| API | src/main occ | src/main files | src/test occ | src/test files |
|---|---:|---:|---:|---:|
| `StringBuilder.isEmpty()` (15) | 13 | 11 | 0 | 0 |
| `String.repeat()` (11) | 6 | 4 | 12 | 2 |
| `RandomGenerator` / `Random.nextInt(a,b)` (17) | 5 | 3 | 0 | 0 |
| `Collection.toArray(IntFunction)` (11) | 4 | 3 | 0 | 0 |
| `java.net.http.*` (11) | 3 | 1 | 0 | 0 |
| `CompletableFuture.failedFuture()` (9) | 2 | 2 | 0 | 0 |
| `InputStream.readAllBytes()` (9) | 1 | 1 | 0 | 0 |
| `Executors.newVirtualThreadPerTaskExecutor()` (21) | 1 | 1 | 0 | 0 |
| `List.getFirst()` — SequencedCollection (21) | 1 | 1 | 1 | 1 |
| `Path.of()` (11) | 0 | 0 | 19 | 12 |
| `Files.readString/writeString` (11) | 0 | 0 | 4 | 3 |
| `String.lines()` (11) | 0 | 0 | 1 | 1 |
| **Category B total** | **36** | **23** | **37** | **16** |

Verified **zero** occurrences: `Stream.toList()` (all 86 `.toList()` are `Collectors.toList()`),
`String.isBlank()` (the only `isBlank` is a project method in `CuboidCommand`), `String.strip()`,
`List/Set/Map.of` in src/main, `List/Set/Map.copyOf` (all `copyOf` are `Arrays.copyOf*` / `EnumSet.copyOf`),
`Map.ofEntries/entry`, `Objects.requireNonNullElse`, `Optional` 9+ methods, `Predicate.not`,
`Collectors.toUnmodifiable*`, `Stream.takeWhile/dropWhile/ofNullable`. All 26 `.reversed()` calls are
`Comparator.reversed()` (Java 8) and are **not** SequencedCollection usages.

**Combined A + B remaining: 227 occurrences across 135 unique `src/main` files** (plus 66 occurrences in
27 test files, deliberately deferred — tests are not on the shaded-jar compile path).

### Deferred within the Java 8 phase (need design, not mechanical edits)
`DatabaseManager` / `OrdersManager` / `AuctionHouseRepository` / `ShopPreferenceRepository` text blocks ·
`SellStatsExporter` `java.net.http` + virtual threads · `ConfigManager.readAllBytes()` ·
`LeaderboardManager` + `TpaQueueMenu` switch expressions.

---

## Category C — Spigot 1.12.2 API migration: NOT STARTED

Inventory only. **No Category C item has been modified in Batch 1, 2 or 3** — counts below are byte-identical
to the pre-Java-8 baseline.

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
1. Java 8 Batch 4 — continue mechanical work: remaining `StringBuilder.isEmpty()` (13), `toArray(IntFunction)` (4),
   `String.repeat()` (6), `RandomGenerator` (5), the last `List.getFirst()` in `ServerWipeManager`.
2. Continue the instanceof-pattern / `var` sweep in controlled ≤10-file batches.
3. Handle the deferred design-level Java 8 items.
4. Only then begin the Spigot 1.12.2 API phase (Materials first).
