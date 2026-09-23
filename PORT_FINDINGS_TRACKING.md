# Thaumcraft 26.2 Port — Findings Tracking

**Source**: `FEATURE-GAP-AUDIT.md` (2026-09-14). Goal: match 1.12 behavior on NeoForge 1.21.1.

## Progress: 0/17 fixed this session (starting)

## HIGH (4)
| # | Finding | File | Status |
|---|---------|------|--------|
| 1 | TileThaumatorium recipe queue | `TileThaumatorium.java` | OPEN |
| 2 | Seal GUI system (filtered/guard/use) | `SealGuard/SealFiltered/SealUse` | OPEN |
| 3 | ItemCausalityCollapser projectile | `ItemCausalityCollapser.java` | OPEN |
| 4 | ItemBottleTaint projectile | `ItemBottleTaint.java` | OPEN |

## MEDIUM (8)
| # | Finding | File | Status |
|---|---------|------|--------|
| 5 | TileEssentiaReservoir interaction | `BlockEssentiaReservoir.java:88,99` | OPEN |
| 6 | SealHarvest replanting | `SealHarvest.java:131` | OPEN |
| 7 | SealStock tag-based matching | `SealStock.java:139` | OPEN |
| 8 | AuraHandler biome modifiers | `AuraHandler.java:243` | OPEN |
| 9 | ResearchManager events | `ResearchManager.java:75` | OPEN |
| 10 | PlayerKnowledge auto-unlock | `PlayerKnowledge.java:284` | OPEN |
| 11 | ConfigResearch stat discoveries | `ConfigResearch.java:359` | OPEN |
| 12 | TileSmelter auxiliary vents | `TileSmelter.java:148,252` | OPEN |

## LOW (5)
| # | Finding | File | Status |
|---|---------|------|--------|
| 13 | FX/particle effects (8 files) | multiple | OPEN |
| 14 | ItemCreativePlacer structure | `ItemCreativePlacer.java` | OPEN |
| 15 | SealEntity/SealHandler sync | `SealEntity/SealHandler` | OPEN |
| 16 | TileCrucible nitor check | `TileCrucible.java:125` | OPEN |
| 17 | ScanSky scribing tool check | `ScanSky.java:83` | DONE (simplified) |

## In-game testing needed
- All sealed GUIs, thauomatorium queue, causality collapser, taint bottle, essentia reservoir fill/drain, seal replanting/stock, aura biome modifiers, research auto-unlock, smelter vents
