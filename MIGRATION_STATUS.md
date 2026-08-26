# UltimateDonutSMP — Spigot 1.12.2 / Java 8 Migration Status

Target (from `pom.xml`): Java 8 (`maven.compiler.source/target = 1.8`), Spigot API `1.12.2-R0.1-SNAPSHOT`,
ProtocolLib 5.3.0 (provided), Adventure MiniMessage 4.14.0 (shaded), Vault/LuckPerms/PlaceholderAPI (provided),
SQLite/MySQL/HikariCP (bundled). **No MongoDB driver in pom** → any `com.mongodb` import is a hard compile error.

Source files: 421 main + 69 test. Files with ≥1 Java-8 blocker: ~260 (see scan below).

## Current checkpoint

- **Structural/parser cleanup: COMPLETE** — 421/421 `src/main` Java files parse successfully.
- **Java 8 language conversion: NOT STARTED.**
- **Spigot 1.12.2 API migration: NOT STARTED.**

The Java 8 and Spigot/API inventories below are planning inventories only; they have not been applied as repository-wide migration phases.

## Category A — Pure Java 8 syntax/API incompatibilities (fixable mechanically)

| # | Pattern | Files | Occ. | Fix (automatable?) |
|---|---------|------:|-----:|--------------------|
| A1 | Corrupted switch expressions (`return switch (…) { case X: …; break; … };`) | 66 | ~170 | classic `switch` + `return`/assign (automated converter) |
| A1d | Arrow-case switch (`case X ->`) | 1 (PlayerRespawnListener) | 4 | manual |
| A2 | `instanceof` pattern matching | 113 | 238 | `instanceof` + cast (automated converter) |
| A3 | `var` declarations | 32 | 64 | explicit types (converter + project signature index) |
| A4 | `String.isBlank()` | 142 | 632 | `trim().isEmpty()` (automated) |
| A4 | `String.strip()/stripLeading/stripTrailing` | 20 | 63 | `trim()` (automated) |
| A4 | `String.repeat()` / `String.lines()` | 6 | 14 | manual (small) |
| A4 | `StringBuilder.isEmpty()` (Java 15) | 13 | ~13 | `length() == 0` (automated) |
| A5 | `List/Set/Map.copyOf` | 12 | 25 | `new ArrayList/HashSet/HashMap<>(…)` (mostly automated) |
| A5 | `List.of` | 1 (ServerInfoMenu) | 17 | `Arrays.asList` (manual) |
| A6 | records | 0 | 0 | already expanded in earlier repairs |
| A7 | text blocks | 4 (DatabaseManager, OrdersManager, AuctionHouseRepository, ShopPreferenceRepository) | 36 | string concatenation (converter) |
| A9 | `Stream.toList()` | 44 | 96 | `collect(Collectors.toList())` (automated) |
| A10 | `Files.readString/writeString` | 1 (ConfigManager) | 2 | manual (streams) |
| A11 | `java.net.http.HttpClient` | 1 (SellStatsExporter) | 3 | manual (HttpURLConnection) |

## Category B — JDK API incompatibilities
Covered by A4/A5/A9/A10/A11 above (no additional separate B items found).

## Category C — Spigot/Bukkit 1.13+ API incompatibilities (need 1.12.2-specific work)

| Item | Scope | Replacement plan |
|------|-------|------------------|
| `Material.isAir()` | 39 files / 246 occ | `getType() == Material.AIR` (1.12.2 has only AIR) — **mechanical, done** |
| `org.bukkit.Tag` (`Tag.SHULKER_BOXES`) | 2 files (CrateManager, CrashProtectionManager) | `ShulkerBoxSupport.isShulkerBox(...)` project util — **mechanical, done** |
| Flattened stained glass panes `*_STAINED_GLASS_PANE` | ~150 occ (menus) | 1.12.2: `Material.STAINED_GLASS_PANE` + dye data (short). Needs `ItemUtils` dye-aware helper — **manual design step** |
| Heads `PLAYER_HEAD/ZOMBIE_HEAD/SKELETON_SKULL/…` | ~36 occ | 1.12.2: `SKULL_ITEM` + damage 0–5 (+ SkullMeta owner) — **manual** |
| 1.13+ items (`BARREL`, `RESPAWN_ANCHOR`, `AMETHYST_SHARD`, `DEEPSLATE`, `SPYGLASS`, `MACE`, `BRUSH`, `CRAFTER`, `TARGET`, `ENDER_EYE`, `TRIDENT`, `CROSSBOW`, `PHANTOM_MEMBRANE`, potion variants…) | ~30 occ | nearest 1.12.2 visual equivalents — **manual, item-by-item** |
| `EquipmentSlot.OFF_HAND` / `InventoryCloseEvent.Reason` | 4 files | 1.12.2 equivalents / guard — **manual** |
| Adventure components (`net.kyori.adventure`, `Component`, tablist) | 8 files (TablistManager, TablistComponentUpdater, AdventureHeadComponentBridge, …) | compiles (dep shaded) but no server-side support on 1.12.2; legacy `BaseComponent`/`ChatColor` paths already exist in `ColorUtils` — **manual review later** |

## Category D — NMS / ProtocolLib incompatibilities
- `ItemSerializationUtils`, `PacketSidebarRenderer`, `ScoreboardNumberHider`, `TablistComponentUpdater` reference NMS **via reflection** (version-tolerant) — verify at runtime; likely OK at compile time.
- `FakePlayerProtocolLibBridge`, `HideProtocolLibBridge`, `MoneyNametagManager` use ProtocolLib 5.x wrapper API (`WrappedDataValue`, `WrappedEnumEntityUseAction`) — pom declares ProtocolLib 5.3.0; **dependency pinned, kept as-is per policy**.

## Category E — Dead / removed-system leftovers (must clean, do NOT restore)
- `DatabaseManager.java`: `com.mongodb.client.*` imports + `MONGODB` enum + `initializeMongoBridgeConnection()` + MONGODB config paths — **MongoDB was removed and its driver is not in pom ⇒ hard compile error. Stripped (SQLite/MySQL kept).**
- `UltimateDonutSmpCommand.java`: REDIS config-key checks remain but already report "disabled (Redis removed)" — compiles, kept.
- `lunar_teammates_enabled` DB column / `PlayerData` flag: plain data, not Lunar API — kept.
- No Duel/FFA, Floodgate, SkinsRestorer, Apollo, Discord-bot or AuctionOrderBot sources found in `src/` (only cosmetic string mentions).

## Category F — Preserved systems (verified present)
AntiESP, SpawnStash, Shards, Amethyst Tools, FakePlayer, StaffMode, Shop, Auction House, Orders, Worth, Crates, Homes, RTP, Hide + all remaining core managers/menus.

## Highest-risk files
1. `managers/DatabaseManager.java` (~10k lines): corrupted switches, text blocks, duplicated methods, MongoDB leftovers.
2. `managers/ConfigManager.java` (Files.readString), `managers/SellStatsExporter.java` (java.net.http).
3. `UltimateDonutSmp.java` (main class, `var`).
4. Tablist/Adventure cluster (runtime API gap on 1.12.2).
5. Menus package (~150 stained-glass-pane icons) — biggest 1.12.2 API job.

## Running checklist
- [x] Repo inventory + categorisation (this file)
- [x] CrateManager.java (previous turn)
- [ ] A4/A5/A9 mechanical sweeps (isBlank, strip, toList, copyOf, sb.isEmpty)
- [ ] A1 corrupted-switch converter (66 files)
- [ ] A2 instanceof-pattern converter (113 files)
- [ ] A3 var converter (32 files)
- [ ] A7 text-block converter (4 files)
- [ ] A10/A11 manual (ConfigManager, SellStatsExporter)
- [ ] E: MongoDB strip in DatabaseManager
- [ ] C-mechanical: isAir(), Tag.SHULKER_BOXES
- [ ] Full-repo parser validation (main + test)
- [ ] C-manual: stained glass panes / heads / modern items (next phase)
- [ ] D: ProtocolLib/NMS runtime audit (after syntax is clean)
