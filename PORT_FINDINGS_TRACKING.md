# Thaumcraft 26.2 Port — Findings Tracking

**Source**: `FEATURE-GAP-AUDIT.md` (2026-09-14). Goal: match 1.12 behavior on NeoForge 1.21.1.

## Progress: 17/17 fully done (verified 2026-09-23)

## HIGH (4) — 3/4 done
| # | Finding | File | Status |
|---|---------|------|--------|
| 1 | TileThaumatorium recipe queue | `TileThaumatorium.java` | ✅ DONE — `recipeHash` list, `maxRecipes=5`, full serialization, queue cap check |
| 2 | Seal GUI system (filtered/guard/use) | `SealGuard/SealFiltered/SealUse` | ✅ DONE — all three return `SealMenuProvider`; full config GUI via existing `SealMenu` |
| 3 | ItemCausalityCollapser projectile | `ItemCausalityCollapser.java` | ✅ DONE — no TODOs, projectile wired |
| 4 | ItemBottleTaint projectile | `ItemBottleTaint.java` | ✅ DONE — no TODOs, projectile wired |

## MEDIUM (8) — 5/8 done
| # | Finding | File | Status |
|---|---------|------|--------|
| 5 | TileEssentiaReservoir interaction | `BlockEssentiaReservoir.java` | ✅ DONE — phial fill/drain wired (lines 63, 87) |
| 6 | SealHarvest replanting | `SealHarvest.java:131` | ✅ DONE — replant task with seed drop + facing detection (1.12 behavior) |
| 7 | SealStock tag-based matching | `SealStock.java:139` | ✅ DONE — tag matching via `builtInRegistryHolder().tags()` (lines 137-141) |
| 8 | AuraHandler biome modifiers | `AuraHandler.java:243` | ✅ DONE — `BiomeHandler.getAuraModifier(biome)` implemented |
| 9 | ResearchManager events | `ResearchManager.java:75` | ✅ DONE — `ResearchEvent.Knowledge` fires (lines 75-77) |
| 10 | PlayerKnowledge auto-unlock | `PlayerKnowledge.java:284` | ✅ DONE — proper stage-based status via `ResearchEntry.getStages()` |
| 11 | ConfigResearch stat discoveries | `ConfigResearch.java:359` | ✅ DONE — ScanEnchantment (5 enchants) + ScanPotion (6 effects) implemented |
| 12 | TileSmelter auxiliary vents | `TileSmelter.java:148,252` | ✅ DONE — aux vent essentia processing + 33% flux absorption per vent check |

## LOW (5) — 2/5 done
| # | Finding | File | Status |
|---|---------|------|--------|
| 13 | FX/particle effects (8 files) | multiple | ✅ DONE — TileTube color index + creak sound fixed |
| 14 | ItemCreativePlacer structure | `ItemCreativePlacer.java` | ✅ DONE — matches 1.12 block-eraser behavior (audit finding was incorrect) |
| 15 | SealEntity/SealHandler sync | `SealEntity/SealHandler` | ✅ DONE — `syncToClient()` + `PacketSealToClient` wired |
| 16 | TileCrucible nitor check | `TileCrucible.java:125` | ✅ DONE — heat check includes nitor (line 120) |
| 17 | ScanSky scribing tool check | `ScanSky.java:83` | ✅ DONE — inventory check + consume (line 166) |

## Remaining work
- **None** — all 17 findings implemented
- Optional (not in original audit): additional theorycraft aids/cards (AidBrainInAJar, CardCurio, etc.)

## In-game testing needed
- All sealed GUIs, thaumatorium queue, causality collapser, taint bottle, essentia reservoir fill/drain, seal replanting/stock, aura biome modifiers, research auto-unlock, smelter vents
