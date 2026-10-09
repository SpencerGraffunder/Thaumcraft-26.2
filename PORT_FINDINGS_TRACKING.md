# Thaumcraft26 Port — Findings Tracking

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

## In-game testing — done (2026-10-09, SMOKE 27/27)
Every item below is now a deterministic check in the `ThaumcraftSmoke` battery
(`tools/run_smoke.sh`, run by the audit gate):

| Item | Smoke check | Result |
|---|---|---|
| Aura biome modifiers | `biome-aura` (specific/tag/default lookup + aspects) | PASS |
| Thaumatorium queue | `thaumatorium-queue` (cap 5, remove, NBT roundtrip, clear) | PASS |
| Essentia reservoir fill/drain | `reservoir-phial` (right-click fill + extract) | PASS — **found + fixed a real bug**: fill condition was inverted (`addToContainer` returns the unadded remainder; `> 0` → never filled) |
| Seal stock matching | `seal-stock` (exact / `c:stones` tag / TC-mod matching) | PASS |
| Seal config GUIs | `seal-gui` (guard/filtered/use return `SealMenuProvider`; stock intentionally none) | PASS |
| Causality collapser | `collapser-rift` (thrown projectile hits, nearby flux rift set collapsing) | PASS |
| Taint bottle | `taint-bottle` (landed, nearby mob got FLUX_TAINT, goo placement logged) | PASS |
| Smelter vents | `smelter-vents` (3 vents cut pollution well under 60% of unvented) | PASS — **found + fixed a real bug**: shared smelter BE type was registered for the base block only, so placing any variant (vent/aux/thaumium/void) threw "Invalid block entity" |
| Research auto-unlock | `research-autounlock` (NBT roundtrip + AUTOUNLOCK set == research data) | PASS |

Remaining (genuinely manual, needs a client with a player): seal replanting
against a live growing crop, and GUIs opened interactively (the providers are
verified; pixel-level interaction is not).
