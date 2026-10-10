# Thaumaturge Comparison (2026-10-09)

[Thaumaturge](https://github.com/Leclowndu93150/Thaumaturge) ("a port of Thaumcraft
by Azanor") is a complete, actively maintained TC6 port for **MC 26.1.2 / NeoForge**
(2,417 Java files, pushed 2026-10-10). Cloned at `~/Documents/Thaumaturge`. It is a
modern *re-architecture* of 1.12 TC (content/ + api/ + mixin/ + compat/), not a
line-port — so it is useful to us as a **second source of truth for 1.12 behavior**,
especially for the parts of our 1.12 decompile reference that are missing.

**Key context: `.reference/thaumcraft-1.12/` is a PARTIAL decompile.** It lacks the
aura-node system, the full AuraHandler chunk-tick loop, golem AI wiring, several
block packages, and node research data. Our port is 1:1 against what the reference
*does* contain (148/148 research keys, all block/tile/entity names, aspect and
recipe parity enforced by the gate) — so the real fidelity gaps are **subsystems the
reference never showed us**, which Thaumaturge makes visible.

## 1. Major 1.12 subsystems missing from our port (biggest fidelity gaps)

| # | Subsystem | 1.12 role | Thaumaturge evidence | Our state |
|---|-----------|-----------|----------------------|-----------|
| 1 | **Aura nodes** — node worldgen, node blocks (per-aspect, size 1–3, hungry), node stabilizers, transducers, node jars, node tappers (wand), vis relays, node orbs, AURAPRESERVE | The heart of 1.12 auromancy gameplay | `content/aura/node/` — 26 files, ~3,300 lines (NodeGenerator, NodeHunger, NodeUpkeep, NodeFeature, BlockEntityNode*, BlockNode*, NodeWandTap, NodeBiomeSpread) + `data/.../research_entry/node*.json` research set | **Absent.** No node blocks, no node data in AuraChunk, no node research (our auromancy.json is 1:1 with the *partial* reference, which lacks it too) |
| 2 | **Golem AI + movement** — ground/air pathfinding, arrow attack, follow-owner, flight/wheel movement | Golems work autonomously; levitator legs = flying golems, wheels = haulers | golem content + commit "rewrite golem darts, flyer legs and return home" | **Absent.** 1.12 ref has 9 AI files (`ai/AIArrowAttack`, `PathNavigateGolemAir/Ground`, `GolemNodeProcessor`, …) — none ported; `GolemLegLevitator`/`GolemLegWheels` parts missing. Our golems are stationary (they tick, but never move or fight) |
| 3 | **Flux pressure events** — chunk flux high → lightning, flux rain, wisps, warp, rift spawns | Iconic 1.12 flux consequences | `content/aura/pressure/` — 11 files (FluxLightning, RainPressureEvent, WispPressureEvent, WarpPressureEvent, NodeMutationPressureEvent, …) | **Absent.** We accumulate flux (smelter/vent check verifies) but nothing ever *does* with it |
| 4 | **Cultist / Pech / altar-focus AI** | Crimson Cult raiders, Pech trading, altar focus | `content/pech/`, altar content | Entities exist; the 7 AI files (`ai/pech/*`, `ai/combat/AICultist*`, `AIAltarFocus`) are unported — mobs are inert |

## 2. Verified PARITY (we match 1.12 — no action)

- `AURA_CEILING = 500`, base = mean of 5 biome modifiers × 500 × `(1 + 0.1·gaussian)`,
  clamp 0..500 — identical structure to 1.12 `AuraHandler.generateAura` and Thaumaturge.
- Smelter vent absorption = per-flux `nextFloat() < 0.333` roll per facing vent —
  matches 1.12 `TileSmelter` (line 266) exactly (smoke-verified).
- Research data: 148/148 keys 1:1 with the 1.12 reference set.
- Crucible mechanics (strict research gate, 50 mb water drain, dissolve-vs-craft) —
  smoke-verified against the 1.12 ref this week.
- Taint spread config shape (`taintSpreadRate`/`taintSpreadArea`, fibre spread rates
  `rate/100·mod` and `rate/100·0.33`) matches 1.12 `TaintHelper`.

## 3. Deviations found & fixed from this comparison

- **Aura base was NOT seed-deterministic** (fixed 2026-10-09): we used the shared
  `level.getRandom()`, so a chunk's base aura depended on *chunk-generation order*.
  1.12 used the chunk-gen random (deterministic per world seed) and Thaumaturge uses
  `worldSeed ^ chunkKey`. Now `AuraHandler.chunkAuraRandom(worldSeed, chunkPos)` +
  pure `computeBaseAura(...)`; verified by the `aura-determinism` smoke check. Also
  removed dead swamp/desert `biomeMod` code in `generateAura`.
- **1.12 smelter vents are plain (non-BE) blocks** (`BlockSmelterVent extends
  BlockTC`, no tile). We model vent/aux/thaumium/void as `BlockSmelter` variants
  sharing one BE type — it works (fixed + smoke-verified 2026-10-09) but the vents
  carry a useless BE. Known deviation, low priority.
- **Biome aura modifiers**: 1.12 averaged over *all BiomeDictionary types* of a
  biome (a biome can carry several, e.g. forest+jungle). We approximate with a
  smaller vanilla-tag set (`IS_FOREST`, `IS_JUNGLE`, …) + specific-biome overrides.
  Close in spirit; exact 1.12 values for multi-type biomes differ slightly.

## 4. Thaumaturge techniques worth borrowing (if we do the big items)

- **Deterministic per-chunk randoms** everywhere (`seed ^ chunkPos key`) — adopted
  for aura; apply the same pattern to node gen and flux pressure if ported.
- **Aura data as a data component / per-chunk `AuraData`** with lazy init +
  pristine detection — cleaner than our `AuraWorld`/`AuraChunkHandler` split.
- **Flux pressure as typed events** (`FluxPressureEvent` + registry of types) —
  extensible shape for our future pressure system.
- **Node rules/hunger/upkeep split** (`NodeRules`, `NodeHunger`, `NodeUpkeep`) —
  a good decomposition to copy when porting nodes.
- They use **mixins for vanilla integration** (we avoid mixins — fine, but note it).

## 5. Suggested order of work (by fidelity impact / effort)

1. ~~Aura base determinism~~ — **done** (2026-10-09, `aura-determinism` smoke).
2. **Flux pressure events** (lightning + rift spawn on saturated chunks) — medium
   effort, high iconic-1.12 value, plugs into the flux we already track.
3. **Aura node system** — the largest single gap; needs blocks + BEs + worldgen +
   wand tap + research set; port from 1.12 semantics using Thaumaturge as the
   second reference.
4. **Golem AI + levitator/wheels** — large (modern navigation API); golems are core
   1.12 gameplay.
5. **Cultist/Pech AI** — medium; needed for eldritch content to function.
6. Smelter vent/aux as plain blocks (deviation cleanup) — low priority.
