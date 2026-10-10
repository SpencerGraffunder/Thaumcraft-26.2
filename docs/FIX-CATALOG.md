# Fix Catalog — 1.12 fidelity sweep (2026-10-10)

Tracking document for the full-fidelity push. Every item: **OPEN** until a working
implementation lands and the gate is green, then **FIXED (commit)**. Source of truth
is the 1.12 decompile (`.reference/thaumcraft-1.12/`), with Thaumaturge
(`~/Documents/Thaumaturge`) as second reference where the decompile is partial.

Raw material: `/tmp/method_diff.txt` (method-level diff, 274 class pairs, 913 names),
data sweeps (lang/sounds/enchantments/models). Items marked **NOISE** were checked
and are 1.12-API artifacts or already ported under a different shape — do not re-check.

Status legend: OPEN / FIXED / NOISE / DEFERRED (needs a bigger subsystem first)

---

## B0 — Data (registry, lang, sounds, enchantments)

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F001 | 48 entity lang keys used PascalCase (`entity.thaumcraft.Golem`) but ids are snake_case → all mob names rendered as raw ids | lang/en_us.lang `entity.*.name` | Renamed to `entity.thaumcraft.<registered_id>` (tools/audit_lang.py) | **FIXED** (lang batch) |
| F002 | 12 entities had no lang key at all (focus_projectile, alumentum, grapple, homing_shard, special_item, …) | lang | Added with 1.12 values | **FIXED** (lang batch) |
| F003 | 10 effect + 4 plate + 3 fluid lang keys missing/mismatched (`warpward`→`warp_ward`, `plate.iron`→`plate_iron`, …) | lang | audit_lang.py --fix | **FIXED** (lang batch) |
| F004 | Sound event `runicshieldecharge` missing (65/65 otherwise) | sounds.json | Add event + file | OPEN |
| F005 | 5 infusion enchantments missing from data registry: visbattery, vischarge, swift, agile, infested (1.12 EnumInfusionEnchantment has 13, ours 8 + bore `infusion`) | lib/enchantment/EnumInfusionEnchantment.java | Add data/thaumcraft/enchantment/*.json + lang + behavior hooks | OPEN |
| F006 | `tools/audit_lang.py` added to gate (registry-id ↔ lang-key oracle) | — | Wire into run_all_audits.sh | OPEN |

## B1 — Block behaviors

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F010 | Breaking a mirror block must drop the **mirror item** preserving link coords (mirror_essentia: same + contents) | devices/BlockMirror.dropMirror | Implement onRemove→drop item w/ link tag; place item→block restores | OPEN |
| F011 | Candles around an infusion matrix act as stabilizers: each candle reduces instability; symmetric placement penalty | blocks/crafting/BlockCandle (canStabaliseInfusion/getStabilizationAmount/getSymmetryPenalty) + TileInfusionMatrix | Add candle scan to instability calc | OPEN |
| F012 | Brain jars (jar_brain) give enchanting-table power bonus to nearby ench. tables | essentia/BlockJar.getEnchantPowerBonus | Hook enchantment power query | OPEN |
| F013 | Amber ore has 6.6% chance per drop to give a random **curio** instead | world/ore/BlockOreTC.getDrops | Add loot modifier / custom drops | OPEN |
| F014 | TC leaves (greatwood/silverwood) flammability + fire spread + shearing drops | blocks/basic/BlockLeavesTC (getFlammability/getFireSpreadSpeed/onSheared) | Set fire properties + shear behavior | OPEN |
| F015 | TC planks flammability | blocks/basic/BlockPlanksTC | Fire properties | OPEN |
| F016 | TC metals are beacon bases (isBeaconBase) — arcane/eldritch/ancient stone too | blocks/basic/BlockMetalTC, BlockStoneTC | beacon_base property | OPEN |
| F017 | Eldritch/ancient stone `canEntityDestroy` (tnt-immune?) | blocks/basic/BlockStoneTC | Check 1.12 impl, mirror | OPEN |
| F018 | Golem builder: rotate on wrench + break drops items inside | crafting/BlockGolemBuilder (destroy/rotateBlock) | Wrench rotate + break drops | OPEN |
| F019 | Infernal furnace: breaking the multiblock breaks all 27 blocks + drops contents | devices/BlockInfernalFurnace.destroyFurnace | Multiblock break cascade | OPEN |
| F020 | Hungry chest: rotate on wrench; harvest rules | devices/BlockHungryChest | Wrench rotate | OPEN |
| F021 | Brain box harvest rule (drops itself unharvested?) | devices/BlockBrainBox.canHarvestBlock | Check + mirror | OPEN |
| F022 | Arcane ear: instrument property (getInstrument) — which note it plays | devices/BlockArcaneEar + TileArcaneEar.updateTone | Verify note selection logic vs 1.12 | OPEN |
| F023 | Dioptra `updateState` — redstone state update on target change | devices/BlockDioptra | Check our dioptra emits redstone correctly | OPEN |
| F024 | Condenser `fill` + lattice connection propagation (makeConnections/processUpdate) | devices/BlockCondenser*, TileCondenser | Verify condenser lattice I/O | OPEN |
| F025 | Water jug / vis generator tick parity | devices/BlockWaterJug, TileVisGenerator | Diff tick logic vs 1.12 | OPEN |
| F026 | Research table consumes paper from its own inventory (consumepaperFromTable) | crafting/TileResearchTable | Verify paper consumption path | OPEN |
| F027 | Tube: connection recompute on neighbor change (makeConnections) | essentia/BlockTube | Verify tube network rebuild | OPEN |
| F028 | Smelter vent/aux are plain (non-BE) blocks in 1.12 — ours share a BE type (works; deviation) | blocks/essentia/BlockSmelterVent | Low-pri cleanup | DEFERRED |
| F029 | `barrier` block (1.12 misc) — invisible barrier block | misc/BlockBarrier | Port block + behavior | OPEN |
| F030 | Effect blocks (effect_glimmer/sap/shock) light level + `run` tick | world/BlockEffect | Verify light + tick | OPEN |
| F031 | Flux goo / liquid death fluid behavior (quanta, side-solid) | world/BlockFluxGoo, BlockFluidDeath | Verify fluid props | OPEN |

## B2 — Tile behaviors

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F040 | **Thaumatorium upgrades**: items in upgrade slots modify speed/quality (getUpgrades/updateRecipes) | crafting/TileThaumatorium | Port upgrade items + effects | OPEN |
| F041 | Golem builder essentia **suction** (type/amount settings pulled from aura) | crafting/TileGolemBuilder | Port suction | OPEN |
| F042 | Arcane workbench `getAura` (aura draw for enchanting?) | crafting/TileArcaneWorkbench | Verify | OPEN |
| F043 | Pedestal: findInstabilityMitigator (candles/golems around reduce instability), seekSourceRecursive (essentia draw from aura/tanks) | crafting/TilePedestal | Verify/extend | OPEN |
| F044 | Crucible `spillRemnants` on overfill | crafting/TileCrucible | Verify | OPEN |
| F045 | Bellows cooktime persistence + rendering | devices/TileBellows | Verify | OPEN |
| F046 | Thaumatorium recipe hash list + current output recipe (generateRecipeHashlist) | crafting/TileThaumatorium | Verify queue uses hashes (done in smoke) | OPEN (verify) |
| F047 | TileThaumcraftInventory slot sync flags (isSyncedSlot/getSyncedStackInSlot) | tiles/TileThaumcraftInventory | Verify synced slots match 1.12 per-tile | OPEN |

## B3 — Item behaviors

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F050 | Cultist boots: vis discount + warp protection while worn | items/armor/ItemCultistBoots (getVisDiscount/getWarp) | Add tick/hook | OPEN |
| F051 | Goggles (ItemGoggles): see node/aura info — client; check what server-side they do | items/armor/ItemGoggles | Mostly client; verify | OPEN |
| F052 | Golem bell selection raycast bounds | golems/ItemGolemBell | Verify bounds logic | OPEN |
| F053 | SealProvide `areGolemTagsValidForTask` (tag validation for provided tasks) | golems/seals/SealProvide | Verify | OPEN |
| F054 | SealUse `mayPlace/dropSomeItems` (placement rules, dropping excess) | golems/seals/SealUse | Verify | OPEN |
| F055 | ItemThaumometer / wand: node reading (depends on nodes) | items/ItemThaumometer | DEFERRED → nodes | DEFERRED |
| F056 | Thaumostatic harness: vis storage + charging | items/ItemThaumostaticHarness | Verify vs 1.12 | OPEN |
| F057 | Phial fill/empty vs reservoirs (smoke-covered) + phial→water jug? | items/ItemPhial | Verify | OPEN |
| F058 | Grapple gun: spool/tip items + grapple entity behavior | items/ItemGrappleGun | Verify | OPEN |
| F059 | Primordial pearl: mote/nodule/pearl + pearl behavior | items/ItemPrimordialPearl | Verify | OPEN |
| F060 | Causality collapser item (smoke-covered) + item tooltip | items/ItemCausalityCollapser | Done | **NOISE** |

## B4 — Entity behaviors

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F070 | Fire bat: attacks nearest player (attackEntity/findPlayerToAttack) | monster/EntityFireBat | Add target AI | OPEN |
| F071 | Wisp carries an **aspect type** (affects particle color, drop on death?) | monster/EntityWisp (getType/setType) | Port type + effects | OPEN |
| F072 | Eldritch crab: rides on warden/golem shoulders (getRiding/setRiding) | monster/EntityEldritchCrab | Port riding behavior | OPEN |
| F073 | Spell bat: friendly flag (setIsFriendly) — tamed spell bats don't attack | monster/EntitySpellBat | Port flag | OPEN |
| F074 | **Pech taming**: picks up valuable items, chance=value/10 to tame, then fights player's enemies | monster/EntityPech (canPickup/pickupItem/isValued/getValue) | Port taming + value table | OPEN |
| F075 | Arcane bore: silk touch, refining (getRefining), proper block harvest drops | construct/EntityArcaneBore | Port bore mining parity | OPEN |
| F076 | Turret crossbow: target distance + selection | construct/EntityTurretCrossbow* | Verify targeting | OPEN |
| F077 | Eldritch golem boss: name generation (generateName) | monster/boss/EntityEldritchGolem | Port naming | OPEN |
| F078 | Cultist cleric: ritualist flag + rotation update | monster/cult/EntityCultistCleric | Part of cultist AI | DEFERRED → B6 |
| F079 | Thaumic slime `createInstance` (spawns variants?) | monster/EntityThaumicSlime | Verify | OPEN |
| F080 | Falling taint: block lookup on landing | EntityFallingTaint | Verify | OPEN |

## B5 — Flux pressure events (was todo #12)

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F090 | Chunk flux saturation → **lightning** strike at random pos in chunk | Thaumaturge content/aura/pressure/FluxLightning (1.12 ref partial) | Implement in AuraScheduler | OPEN |
| F091 | Saturated flux → **flux rain** (falling taint / goo particles + damage) | RainPressureEvent | Implement | OPEN |
| F092 | Saturated flux → **wisps** spawn with random aspect types | WispPressureEvent | Implement (uses F071) | OPEN |
| F093 | Saturated flux → **warp** nearby players (warp points scale with flux) | WarpPressureEvent | Implement | OPEN |
| F094 | Saturated flux → **rifts** spawn (EntityFluxRift already exists) | 1.12 aura logic | Implement | OPEN |
| F095 | Deterministic smoke: force chunk flux to saturation → assert event fires | — | New smoke check | OPEN |

## B6 — Aura node system (was todo #11)

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F100 | Node worldgen in aura chunks (aspect/size/density from aura base) | Thaumaturge NodeGenerator | Port | OPEN |
| F101 | Node data in AuraChunk (aspect, size 1–3, stability) | AuraChunk/Node | Port | OPEN |
| F102 | Node **tapper**: wand+focus drains node → vis into wand (or node jar) | 1.12 wand logic + Thaumaturge NodeWandTap | Port | OPEN |
| F103 | **Node stabilizer** (stone): raises stability, slows decay | Thaumaturge BlockEntityNodeStabilizer | Port block+BE | OPEN |
| F104 | **Node transducer**: pipes vis to a pedestal | Thaumaturge BlockEntityNodeTransducer | Port block+BE | OPEN |
| F105 | **Node jar**: captures small nodes, stores vis | Thaumaturge BlockEntityNodeJar | Port block+BE | OPEN |
| F106 | Hungry nodes (rare, high output, unstable) | Thaumaturge NodeHunger | Port | OPEN |
| F107 | Node orbs (visual/interaction helper) | Thaumaturge BlockEntityNodeOrb | Port | OPEN |
| F108 | Thaumometer reads node aura when aimed (uses F055) | ItemThaumometer | Port | OPEN |
| F109 | Node research set (NODES, NODES2, NODESTAB, NODETRANSDUCER, NODEJARS, HUNGRY_NODES, AURAPRESERVE) | Thaumaturge research_entry/node*.json | Port research | OPEN |
| F110 | Node smoke checks (gen determinism, tapper drain, stabilizer effect) | — | New smoke checks | OPEN |

## B7 — Golem AI + legs (was todo #13)

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F120 | Golem ground pathfinding (PathNavigateGolemGround) | golems/ai | Port modern navigation | OPEN |
| F121 | Golem air pathfinding + **levitator legs** (flying golems) | golems/ai/PathNavigateGolemAir, parts/GolemLegLevitator | Port | OPEN |
| F122 | **Wheel legs** (hauler golems, ground speed) | parts/GolemLegWheels | Port | OPEN |
| F123 | Golem arrow/dart attack AI (AIArrowAttack, GolemArmDart.getRangedAttackAI) | golems/ai/AIArrowAttack | Port | OPEN |
| F124 | Golem follow-owner + home-return logic | golems/ai (GolemNodeProcessor) | Port | OPEN |
| F125 | Golem smoke check (moves toward a target deterministically) | — | New smoke check | OPEN |

## B8 — Cultist / Pech / altar AI (was todo #14)

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F130 | Cultist combat AI (melee knight / bow / cleric heals) | ai/combat/AICultist* | Port | OPEN |
| F131 | Cultist portal: spawns cultists at altar ritual | cult/EntityCultistPortal* | Verify + wire | OPEN |
| F132 | Pech trading/value behavior (ties into F074) | ai/pech/* | Port | OPEN |
| F133 | Altar focus AI (altar focus targets nearest cultist/enemy) | ai/AIAltarFocus | Port | OPEN |
| F134 | Altar: summoning ritual (eldritch altar + focus + blood) | devices/BlockAltar* | Verify + wire | OPEN |

---

## NOISE log (checked, no action — do not re-investigate)

- `readSpawnData`/`writeSpawnData`, `isSideSolid`, `getPickBlock`, `isAir`, `hasTileEntity`,
  `getVariantMeta`, `getCustomMesh`, `getArmorTexture/Model`, `onDataPacket`, `func_*` —
  1.12-API surface with no modern equivalent.
- Crucible fluid I/O (drain/fill/capabilities) — ported (FluidStacksResourceHandler).
- Infusion failure events (eject/warp/zap/harm) — ported (instability* methods).
- Traveller boots speed — ported.
- Infernal furnace drop-on-break — ported.
- Block model coverage: 0 real gaps (state models resolve); item models: 0 missing.
- `entity.thaumcraft.Pech.1/.2` lang keys — unused (pech variants are data variants); harmless.
