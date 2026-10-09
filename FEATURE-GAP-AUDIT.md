# Feature-Gap Audit Report (2026-09-14)

Audit of Thaumcraft26 (26.3 port) vs 1.12 reference. Prioritized by impact.

## RE-AUDIT STATUS (2026-09-27)

Re-verified against the current (post compile re-green) source. **`grep -rn TODO src/ --include=*.java` = 0.**

| # | Finding | Status (2026-09-27) |
|---|---------|---------------------|
| 1 | TileThaumatorium recipe queue | ✅ RESOLVED — `recipeHash`/`maxRecipes`/`RecipeHash[i]` NBT in `TileThaumatorium` |
| 2 | Seal GUI (guard/filtered/use) | ✅ RESOLVED — no stub TODOs remain in `SealGuard`/`SealFiltered`/`SealUse` |
| 3 | ItemCausalityCollapser projectile | ✅ RESOLVED — spawns `EntityCausalityCollapser` (ItemCausalityCollapser:47) |
| 4 | ItemBottleTaint projectile | ✅ RESOLVED — spawns `EntityBottleTaint` (ItemBottleTaint:35) |
| 5 | Essentia reservoir phial I/O | ✅ RESOLVED — fill/drain phial logic (BlockEssentiaReservoir:63,87) |
| 6 | SealHarvest replanting | ✅ RESOLVED — `replantTasks`/`ReplantInfo` (SealHarvest:46) |
| 7 | SealStock tag matching | ✅ RESOLVED — `matchesItem` toggle-based filter (SealStock:130) |
| 8 | AuraHandler biome modifiers | ✅ RESOLVED — `BiomeHandler` + biome aura modifier (AuraHandler:244) |
| 9 | ResearchManager events | ✅ RESOLVED — ResearchEvent fired (ResearchManager) |
| 10 | PlayerKnowledge auto-unlock | ✅ RESOLVED — auto-unlock present (PlayerKnowledge) |
| 11 | ConfigResearch stat discoveries | ✅ RESOLVED — stat-based discoveries present |
| 12 | TileSmelter auxiliary vents | ✅ RESOLVED — vents reduce pollution (TileSmelter:267) |
| 13 | FX/particle effects | ✅ RESOLVED — all 8 sites spawn particles (TileHole, TileTube, TileCondenser, TileFocalManipulator, TileInfernalFurnace, TileWaterJug, BlockVisGenerator, BlockEffect) |
| 14 | ItemCreativePlacer structures | ✅ RESOLVED — 1.12 source shows the item only **erases** a block (`setBlockToAir`); 26.2 port matches exactly (solid-block check, face-offset, replaceable check, `setBlock(AIR)`) |
| 15 | Seal network sync | ✅ RESOLVED — `PacketSealToClient`/`syncToClient` (SealEntity:217, SealHandler:197) |
| 16 | TileCrucible nitor check | ✅ RESOLVED — `BlockNitor` heat-source check (TileCrucible:125) |
| 17 | ScanSky scribing check | ✅ RESOLVED — `hasScribingTools(player)` (ScanSky:84) |

**Net: 17/17 resolved.** (Re-verified 2026-09-28: #13 all particle sites implemented since commit 83023ba; #14 the 1.12 reference source `reference/java_old-1.12/common/items/misc/ItemCreativePlacer.java` confirms the item is a block eraser, not a structure placer — 26.2 port is 1.12-faithful.)

---

## Original findings (2026-09-14)

## HIGH Priority (core gameplay broken/missing)

### 1. TileThaumatorium recipe queue system
- **File**: `src/main/java/thaumcraft/common/tiles/crafting/TileThaumatorium.java`
- **1.12**: Full recipe queue system with `recipeHash`, `recipeEssentia`, `recipePlayer`, `recipes`, `maxRecipes`
- **26.2**: Stubbed - `PacketSelectThaumotoriumRecipeToServer.java:74` has TODO "Implement recipe queue selection when TileThaumatorium is updated with the full recipe queue system"
- **Impact**: Thaumatorium (advanced crucible) can't queue multiple recipes

### 2. Seal GUI system (filtered/guard/use seals)
- **Files**: 
  - `SealGuard.java:176,183` - TODO "Return SealBaseContainer/GUI when GUI system is implemented"
  - `SealFiltered.java:117,124` - Same TODOs
  - `SealUse.java:230,236` - Same TODOs
- **1.12**: Full GUI system with filter slots, guard settings, use target selection
- **26.2**: Only basic priority setting via `SealMenu` (wired for basic seals)
- **Impact**: Advanced seal configuration UIs unreachable

### 3. ItemCausalityCollapser projectile
- **File**: `src/main/java/thaumcraft/common/items/misc/ItemCausalityCollapser.java`
- **1.12**: Spawns `EntityCausalityCollapser` projectile that finds and collapses flux rifts
- **26.2**: TODO "Find and collapse nearby flux rifts when EntityFluxRift is implemented" (but EntityFluxRift IS implemented)
- **Impact**: Causality Collapser item doesn't work

### 4. ItemBottleTaint projectile
- **File**: `src/main/java/thaumcraft/common/items/misc/ItemBottleTaint.java`
- **1.12**: Spawns taint bottle projectile that spreads taint on hit
- **26.2**: TODO for spawning projectile
- **Impact**: Taint bottle doesn't function

## MEDIUM Priority (significant features missing)

### 5. TileEssentiaReservoir interaction
- **File**: `src/main/java/thaumcraft/common/blocks/essentia/BlockEssentiaReservoir.java:88,99`
- **1.12**: Right-click with phial to fill/drain, spawns flux pollution for lost essentia
- **26.2**: TODO "Implement interaction" and "Spawn flux pollution"
- **Impact**: Essentia reservoirs can't be filled/drained by players

### 6. SealHarvest replanting
- **File**: `src/main/java/thaumcraft/common/golems/seals/SealHarvest.java:131`
- **1.12**: Replants crops when harvested (seed system)
- **26.2**: TODO "Handle replanting when seed system is implemented"
- **Impact**: Harvest seal doesn't replant crops

### 7. SealStock tag-based matching
- **File**: `src/main/java/thaumcraft/common/golems/seals/SealStock.java:139`
- **1.12**: Can filter by item tags
- **26.2**: TODO "Implement tag-based matching"
- **Impact**: Stock seal can't use tag filters

### 8. AuraHandler biome modifiers
- **File**: `src/main/java/thaumcraft/common/world/aura/AuraHandler.java:243`
- **1.12**: Biomes have aura modifiers (forest = more vis, desert = less)
- **26.2**: TODO "implement BiomeHandler"
- **Impact**: All biomes have same aura generation rate

### 9. ResearchManager events
- **File**: `src/main/java/thaumcraft/common/lib/research/ResearchManager.java:75`
- **1.12**: Fires `ResearchEvent.Knowledge` and `ResearchEvent.Research` events for other mods to hook
- **26.2**: TODO "Fire ResearchEvent.Knowledge event when event system is implemented"
- **Impact**: Other mods can't react to research completion

### 10. PlayerKnowledge auto-unlock
- **File**: `src/main/java/thaumcraft/common/lib/capabilities/PlayerKnowledge.java:284`
- **1.12**: Auto-unlocks related research when prerequisites met
- **26.2**: TODO "Add auto-unlock research when ResearchCategories is implemented"
- **Impact**: Some research paths may be unreachable

### 11. ConfigResearch stat-based discoveries
- **File**: `src/main/java/thaumcraft/common/config/ConfigResearch.java:359`
- **1.12**: Discoveries for walking, running, jumping, swimming
- **26.2**: TODO "Add stat-based discoveries when stat tracking is implemented"
- **Impact**: Some research entries can't be discovered

### 12. TileSmelter auxiliary vents
- **File**: `src/main/java/thaumcraft/common/tiles/essentia/TileSmelter.java:148,252`
- **1.12**: Can connect auxiliary vents to reduce pollution
- **26.2**: TODO "Check for auxiliary vents and process through them too" and "reduce pollution if present"
- **Impact**: Smelters can't use vents for pollution reduction

## LOW Priority (cosmetic/minor)

### 13. FX/particle effects (multiple files) — ✅ RESOLVED (verified 2026-09-28)
All sites spawn particles (implemented in commit `83023ba`):
- `tiles/misc/TileHole.java:84,207` - ENCHANT sparkle particles (hole edges)
- `tiles/essentia/TileTube.java:145` - PORTAL vent particles + creak/extinguish sound (line 368)
- `tiles/devices/TileCondenser.java:123` - ELECTRIC_SPARK
- `tiles/crafting/TileFocalManipulator.java:190,454` - ENCHANT crafting particles
- `tiles/devices/TileInfernalFurnace.java:356` - LAVA fire particles
- `tiles/devices/TileWaterJug.java:181` - DRIPPING_WATER trail
- `blocks/devices/BlockVisGenerator.java:97` - ELECTRIC_SPARK
- `blocks/misc/BlockEffect.java:120,127` - ENCHANT + ELECTRIC_SPARK effect particles
- **Impact**: Purely visual, no gameplay impact (now fully present)

### 14. ItemCreativePlacer — ✅ RESOLVED / 1.12-faithful (verified 2026-09-28)
- **File**: `src/main/java/thaumcraft/common/items/misc/ItemCreativePlacer.java`
- **1.12 (actual)**: `reference/java_old-1.12/.../ItemCreativePlacer.java` shows the item only **erases** the target block (`world.setBlockToAir(pos)`) after a solid-block / face-offset / replaceable check. The earlier "places obelisks, nodes, casters" note was incorrect — the 1.12 item never placed structures.
- **26.2**: Matches exactly — `isSolidRender` check, face-offset `placePos`, `mayUseItemAt`, `canBeReplaced`, then `setBlock(AIR)`.
- **Impact**: Creative-only debug eraser; now fully 1.12-faithful.

### 15. SealEntity/SealHandler network sync
- **Files**: `SealEntity.java:26`, `SealHandler.java:37-38`
- **1.12**: Full network sync for seal configuration
- **26.2**: Stubbed - "Network packet sync is stubbed"
- **Impact**: Seal changes may not sync to clients (needs verification)

### 16. TileCrucible nitor check
- **File**: `src/main/java/thaumcraft/common/tiles/crafting/TileCrucible.java:125`
- **1.12**: Checks for nitor block to enable certain recipes
- **26.2**: TODO "Add nitor block check when nitor has a block tag"
- **Impact**: Some crucible recipes may not work without nitor

### 17. ScanSky scribing tool check
- **File**: `src/main/java/thaumcraft/common/lib/research/ScanSky.java:83`
- **1.12**: Proper inventory check for scribing tools
- **26.2**: TODO "Implement proper inventory check"
- **Impact**: May allow sky scanning without proper tools

## RESOLVED in this session (2026-09-14)

- ✅ Seal config GUI (basic seals) - wired via SealMenuProvider
- ✅ Taint spread cycle - TaintHelper↔EntityTaintSeed wired
- ✅ Vis-discount mundane/fancy gear - 7 items ported
- ✅ Golem DARTS arms - GolemArmDart implements IArmFunction
- ✅ Golem SMART/SCOUT heads - traits wired (XP system, range bonus)
- ✅ NeoForge transfer API - already using IFluidHandler/IItemHandler

## NOT GAPS (confirmed working)

- Golem follow mode - `setFollowingOwner()` wired
- Golem XP/ranking - SMART trait functional
- Seal priority - basic config works
- Focus engine - packages execute
- Arcane workbench - vis discount + research checks wired
- Pech wand - research progress functional
- Thaumometer - aura scanning works
- Crucible - basic crafting works
