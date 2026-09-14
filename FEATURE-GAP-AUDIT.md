# Feature-Gap Audit Report (2026-09-14)

Audit of Thaumcraft 26.2 vs 1.12 reference. Prioritized by impact.

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

### 13. FX/particle effects (multiple files)
- `TileHole.java:82,165` - sparkle particles
- `TileTube.java:144,353` - vent particles, creak sound
- `TileCondenser.java:117` - spark particles
- `TileFocalManipulator.java:186,445` - crafting particles
- `TileInfernalFurnace.java:351` - fire particles
- `TileWaterJug.java:176` - water trail FX
- `BlockVisGenerator.java:96` - spark particles
- `BlockEffect.java:114` - effect particles
- **Impact**: Purely visual, no gameplay impact

### 14. ItemCreativePlacer structure placement
- **File**: `src/main/java/thaumcraft/common/items/misc/ItemCreativePlacer.java`
- **1.12**: Can place obelisks, nodes, casters
- **26.2**: Partial - places blocks but TODO for full structure placement
- **Impact**: Creative-only item, minor

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
