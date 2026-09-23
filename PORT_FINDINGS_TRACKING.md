# Thaumcraft 26.2 Port — Findings Tracking

**Source**: `FEATURE-GAP-AUDIT.md` (2026-09-14). Goal: match 1.12 behavior on NeoForge 1.21.1.

## Progress: 10/17 fully done · 5/17 partial · 2/17 not started (verified 2026-09-23)

## HIGH (4) — 3/4 done
| # | Finding | File | Status |
|---|---------|------|--------|
| 1 | TileThaumatorium recipe queue | `TileThaumatorium.java` | ✅ DONE — `recipeHash` list, `maxRecipes=5`, full serialization, queue cap check |
| 2 | Seal GUI system (filtered/guard/use) | `SealGuard/SealFiltered/SealUse` | ⚠️ PARTIAL — basic seals work via `SealMenu`; advanced GUI methods return `null` (stub) |
| 3 | ItemCausalityCollapser projectile | `ItemCausalityCollapser.java` | ✅ DONE — no TODOs, projectile wired |
| 4 | ItemBottleTaint projectile | `ItemBottleTaint.java` | ✅ DONE — no TODOs, projectile wired |

## MEDIUM (8) — 5/8 done
| # | Finding | File | Status |
|---|---------|------|--------|
| 5 | TileEssentiaReservoir interaction | `BlockEssentiaReservoir.java` | ✅ DONE — phial fill/drain wired (lines 63, 87) |
| 6 | SealHarvest replanting | `SealHarvest.java:131` | ⚠️ PARTIAL — replant task system exists, but seed-drop step still TODO |
| 7 | SealStock tag-based matching | `SealStock.java:139` | ✅ DONE — tag matching via `builtInRegistryHolder().tags()` (lines 137-141) |
| 8 | AuraHandler biome modifiers | `AuraHandler.java:243` | ✅ DONE — `BiomeHandler.getAuraModifier(biome)` implemented |
| 9 | ResearchManager events | `ResearchManager.java:75` | ✅ DONE — `ResearchEvent.Knowledge` fires (lines 75-77) |
| 10 | PlayerKnowledge auto-unlock | `PlayerKnowledge.java:284` | ⚠️ PARTIAL — auto-unlock loop exists (line 284), but ResearchCategories lookup still TODO (line 53) |
| 11 | ConfigResearch stat discoveries | `ConfigResearch.java:359` | ⚠️ PARTIAL — core scan system works; ScanEnchantment/ScanPotion + some aids/cards still TODO |
| 12 | TileSmelter auxiliary vents | `TileSmelter.java:148,252` | ❌ OPEN — both TODOs still present (auxiliary vent processing + pollution reduction) |

## LOW (5) — 2/5 done
| # | Finding | File | Status |
|---|---------|------|--------|
| 13 | FX/particle effects (8 files) | multiple | ⚠️ PARTIAL — 6/8 files done; `TileTube.java:210` (color index) + `TileTube.java:358` (creak sound) still TODO |
| 14 | ItemCreativePlacer structure | `ItemCreativePlacer.java` | ❌ OPEN — placeholder only; sends "not yet implemented" message |
| 15 | SealEntity/SealHandler sync | `SealEntity/SealHandler` | ✅ DONE — `syncToClient()` + `PacketSealToClient` wired |
| 16 | TileCrucible nitor check | `TileCrucible.java:125` | ✅ DONE — heat check includes nitor (line 120) |
| 17 | ScanSky scribing tool check | `ScanSky.java:83` | ✅ DONE — inventory check + consume (line 166) |

## Remaining work (7 items)
- **#2** Seal advanced GUIs: implement `SealGuard`/`SealFiltered`/`SealUse` container menus (filter slots, guard settings, use-target selection)
- **#6** SealHarvest: wire seed-drop on harvest (golem carries seeds → places on break)
- **#10** PlayerKnowledge: complete ResearchCategories lookup for auto-unlock
- **#11** ConfigResearch: port ScanEnchantment + ScanPotion + remaining aids/cards
- **#12** TileSmelter: implement auxiliary vent detection + pollution reduction
- **#13** TileTube: add aspect-color particle index + creak sound
- **#14** ItemCreativePlacer: implement structure placement (obelisk, node, caster)

## In-game testing needed
- All sealed GUIs, thaumatorium queue, causality collapser, taint bottle, essentia reservoir fill/drain, seal replanting/stock, aura biome modifiers, research auto-unlock, smelter vents
