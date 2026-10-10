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
| F004 | Sound event `runicshieldecharge` missing (65/65 otherwise) | sounds.json | — | **NOISE** (unused in 1.12 source too) |
| F005 | 5 infusion enchantments missing from data registry: visbattery, vischarge, swift, agile, infested (1.12 EnumInfusionEnchantment has 13, ours 8 + bore `infusion`) | lib/enchantment/EnumInfusionEnchantment.java | — | **NOISE** (no 1.12 recipe/behavior for the 5; our 8 active match) |
| F006 | `tools/audit_lang.py` added to gate (registry-id ↔ lang-key oracle) | — | Wired into run_all_audits.sh | **FIXED** (lang batch) |

## B1 — Block behaviors

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F010 | Breaking a mirror block must drop the **mirror item** preserving link coords (mirror_essentia: same + contents) | devices/BlockMirror.dropMirror | getDrops override w/ link CustomData + setPlacedBy restore | **FIXED** (d38b124) |
| F011 | Candles around an infusion matrix act as stabilizers: each candle reduces instability; symmetric placement penalty | blocks/crafting/BlockCandle (canStabaliseInfusion/getStabilizationAmount/getSymmetryPenalty) + TileInfusionMatrix | Candle/stabilizer/pedestal implement IInfusionStabiliser(Ext) | **FIXED** (d38b124) |
| F012 | Brain jars (jar_brain) give enchanting-table power bonus to nearby ench. tables | essentia/BlockJar.getEnchantPowerBonus | Added to `minecraft:enchantment_power_provider` tag (modern power system) | **FIXED** (d38b124) |
| F013 | Amber ore has 6.6% chance per drop to give a **curio** instead + amber/quartz ore 1–4 XP | world/ore/BlockOreTC.getDrops | Loot tables: curio damage-1 alt (0.066) + XP pool (silk-touch inverted) | **FIXED** (d38b124) |
| F014 | TC leaves (greatwood/silverwood) flammability + fire spread + shearing + **silverwood vis regen** + 1/75 sapling drops | blocks/basic/BlockLeavesTC | Fire odds 60/30, IShearable, vis regen in randomTick, loot-table drops (incl. quicksilver nugget) | **FIXED** (d38b124) |
| F015 | TC planks flammability | blocks/basic/BlockPlanksTC | Fire odds 20/5 | **FIXED** (d38b124) |
| F016 | TC metals are beacon bases (isBeaconBase) — arcane/eldritch/ancient stone too | blocks/basic/BlockMetalTC, BlockStoneTC | `minecraft:beacon_base_blocks` tag with all 16 TC metals/stones | **FIXED** (d38b124) |
| F017 | Eldritch/ancient stone `canEntityDestroy` (tnt-immune?) | blocks/basic/BlockStoneTC | 1.12 check is always-true (no stone sets negative slipperiness) | **NOISE** |
| F018 | Golem builder: rotate on wrench + break drops items inside | crafting/BlockGolemBuilder (destroy/rotateBlock) | Wrench rotate + break drops | OPEN |
| F019 | Infernal furnace: breaking the multiblock breaks all 27 blocks + drops contents | devices/BlockInfernalFurnace.destroyFurnace | 1.12 cascade reverts PLACEHOLDER blocks; port's multiblock uses real blocks (no placeholder system) so nothing to revert — breaking the furnace block is the full 1.12-observable behavior for the port design | **DEVIATION** (design) |
| F020 | Hungry chest: rotate on wrench; harvest rules | devices/BlockHungryChest | Wrench rotate | OPEN |
| F021 | Brain box harvest rule (drops itself unharvested?) | devices/BlockBrainBox.canHarvestBlock | Check + mirror | OPEN |
| F022 | Arcane ear: instrument property (getInstrument) — which note it plays | devices/BlockArcaneEar + TileArcaneEar.updateTone | FIXED: was DEAD — TileArcaneEar read its own static map that nothing wrote; now consumes WorldEvents' shared per-dim list (filled by NoteBlockEvent.Play), cleared once per server tick (LevelTickEvent.Post, 1.12 ServerEvents parity). Instrument map HARP..XYLOPHONE=0..9 matches 1.12 material→tone table | **FIXED** |
| F023 | Dioptra `updateState` — redstone state update on target change | devices/BlockDioptra | 1.12 updateState is EMPTY (decorative beam; no redstone); port has the same ENABLED vis/flux mode toggle — parity | **FIXED** (verified) |
| F024 | Condenser `fill` + lattice connection propagation (makeConnections/processUpdate) | devices/BlockCondenser*, TileCondenser | Verified: recursive lattice search, interval=600−15×count (min 5, cap 40), cost=4+√size, flux drain ≥1.0, 2% lattice-dirty chance — all match 1.12 | **FIXED** (verified) |
| F025 | Water jug / vis generator tick parity | devices/BlockWaterJug, TileVisGenerator | Verified: 5×3×5 zone scan, 25mB fills, cauldron 333, 1000 cap, ≤10% vis drain per 5 ticks — matches 1.12 | **FIXED** (verified) |
| F026 | Research table consumes paper from its own inventory (consumepaperFromTable) | crafting/TileResearchTable | Verified: consumePaperFromTable() (slot 1) called from ResearchTableMenu on completion — matches 1.12 ContainerResearchTable path | **FIXED** (verified) |
| F027 | Tube: connection recompute on neighbor change (makeConnections) | essentia/BlockTube | FIXED: neighbor changes already recomputed via updateShape; added missing onPlace() initial 6-face connection compute (single setBlock) | **FIXED** |
| F028 | Smelter vent/aux are plain (non-BE) blocks in 1.12 — ours share a BE type (works; deviation) | blocks/essentia/BlockSmelterVent | Low-pri cleanup | DEFERRED |
| F029 | `barrier` block (1.12 misc) — invisible barrier block | misc/BlockBarrier | Port block + behavior | OPEN |
| F030 | Effect blocks (effect_glimmer/sap/shock) light level + `run` tick | world/BlockEffect | Verify light + tick | OPEN |
| F031 | Flux goo / liquid death fluid behavior (quanta, side-solid) | world/BlockFluxGoo, BlockFluidDeath | Verify fluid props | OPEN |

## B2 — Tile behaviors

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F040 | **Thaumatorium upgrades**: brain boxes on sides add +2 queue slots each (getUpgrades) | crafting/TileThaumatorium | recountBrainBoxes() every 40 ticks, default 1 slot, queue trimmed | **FIXED** (d38b124) |
| F041 | Golem builder essentia **suction** (MECHANISM, 128 suction while building, buffered push) | crafting/TileGolemBuilder | IEssentiaTransport impl + drawEssentia() per progress tick | **FIXED** (d38b124) |
| F042 | Arcane workbench `getAura` (charger → 3×3 chunk aura drain) | crafting/TileArcaneWorkbench | Verified: auraVisServer + spendAura with charger 3×3 drain | **FIXED** (verified) |
| F043 | Instability event system + stabilizer mitigation (findInstabilityMitigator) | TileInfusionMatrix + TileStabilizer | Verified: 24-case event switch + findNearestStabilizer + mitigate(5–10) ported (search simplified vs recursive) | **FIXED** (verified) |
| F044 | Crucible `spillRemnants` on overfill | crafting/TileCrucible | Verified: spillRandom() on 150-tick cadence + overfill | **FIXED** (verified) |
| F045 | Bellows cooktime persistence + rendering | devices/TileBellows | 1.12 has no cooktime field either | **NOISE** |
| F046 | Thaumatorium recipe hash list + current output recipe (generateRecipeHashlist) | crafting/TileThaumatorium | Verified: recipeHash queue, smoke-covered | **FIXED** (verified) |
| F047 | TileThaumcraftInventory slot sync flags (isSyncedSlot/getSyncedStackInSlot) | tiles/TileThaumcraftInventory | Modern BE sync architecture covers it (writeSyncNBT per tile) | **NOISE** |

## B3 — Item behaviors

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F050 | Cultist boots: vis discount + warp protection while worn | items/armor/ItemCultistBoots (getVisDiscount/getWarp) | FIXED: implemented IVisDiscountGear + IWarpingGear (both return 1, 1.12 values); PlayerEvents already consumes both | **FIXED** |
| F051 | Goggles (ItemGoggles): see node/aura info — client; check what server-side they do | items/armor/ItemGoggles | Verified: vis discount 5 matches 1.12; IRevealer/IGoggles present (client overlay) | **FIXED** (verified) |
| F052 | Golem bell selection raycast bounds | golems/ItemGolemBell | Verified: 1.12 bell only targets seals (block hit); port's 16-block follow toggle is a deliberate extension with consistent bounds | **FIXED** (verified) |
| F053 | SealProvide `areGolemTagsValidForTask` (tag validation for provided tasks) | golems/seals/SealProvide | FIXED: was missing entirely — added 1.12 tag validation (lock/owner UUID, required traits containsAll, forbidden traits) to canGolemPerformTask | **FIXED** |
| F054 | SealUse `mayPlace/dropSomeItems` (placement rules, dropping excess) | golems/seals/SealUse | FIXED: canPlaceAt was hardcoded true; now 1.12 mayPlace semantics (block collision AABB must not contain a living entity); SealHandler re-checks every 20 ticks | **FIXED** |
| F055 | ItemThaumometer / wand: node reading (depends on nodes) | items/ItemThaumometer | DEFERRED → nodes | DEFERRED |
| F056 | Thaumostatic harness: vis storage + charging | items/ItemThaumostaticHarness | Not in 1.12 BETA26 (pre-1.12 item) | **NOISE** |
| F057 | Phial fill/empty vs reservoirs (smoke-covered) + phial→water jug? | items/ItemPhial | Smoke-covered (reservoir-phial check); 1.12 has no phial→jug path | **FIXED** (verified) |
| F058 | Grapple gun: spool/tip items + grapple entity behavior | items/ItemGrappleGun | Not in 1.12 BETA26 (port-specific utility item, self-consistent) | **NOISE** |
| F059 | Primordial pearl: mote/nodule/pearl + pearl behavior | items/ItemPrimordialPearl | FIXED: variant thresholds (<3 pearl, <6 nodule, ≥6 mote), crafting remainder +1 damage to 7, no repair/enchant all match; fixed rarity UNCOMMON always + removed foil (1.12 values) | **FIXED** |
| F060 | Causality collapser item (smoke-covered) + item tooltip | items/ItemCausalityCollapser | Done | **NOISE** |

## B4 — Entity behaviors

| # | Item | 1.12 source | Fix | Status |
|---|------|-------------|-----|--------|
| F070 | Fire bat: attacks nearest player (attackEntity/findPlayerToAttack) | monster/EntityFireBat | Verified: 12-block target search, bat-style flight steering, fire/explosion immunity, hanging — all match 1.12 | **FIXED** (verified) |
| F071 | Wisp carries an **aspect type** (affects particle color, drop on death?) | monster/EntityWisp (getType/setType) | Verified: type string + getAspect(), crystal drop of its aspect, particle color from aspect | **FIXED** (verified) |
| F072 | Eldritch crab: rides on warden/golem shoulders (getRiding/setRiding) | monster/EntityEldritchCrab | Port riding behavior | OPEN |
| F073 | Spell bat: friendly flag (setIsFriendly) — tamed spell bats don't attack | monster/EntitySpellBat | Verified: friendly flag present, set at construction | **FIXED** (verified) |
| F074 | **Pech taming**: picks up valuable items, chance=value/10 to tame, then fights player's enemies | monster/EntityPech (canPickup/pickupItem/isValued/getValue) | Ported: tamed flag (synched), PechItemPickupGoal, valued-item table; taming value check wired in pickup | **FIXED** (verified) |
| F075 | Arcane bore: silk touch, refining (getRefining), proper block harvest drops | construct/EntityArcaneBore | FIXED: bore broke blocks with destroyBlock(pos,false) and an EMPTY drops branch — it dropped nothing. Now silk touch ejects the block item, otherwise vanilla loot runs and all item entities are swept into the bore (26.3 reworked the loot API to ContextMap, so held-tool fortune is not injected — noted) | **FIXED** |
| F076 | Turret crossbow: target distance + selection | construct/EntityTurretCrossbow* | Verified: RangedAttackGoal(0.0, 20, 60, 24.0) matches 1.12 EntityAIAttackRanged exactly | **FIXED** (verified) |
| F077 | Eldritch golem boss: name generation (generateName) | monster/boss/EntityEldritchGolem | Covered by ChampionManager.makeChampion (localized mod-name prefix on spawn) | **FIXED** (verified) |
| F078 | Cultist cleric: ritualist flag + rotation update | monster/cult/EntityCultistCleric | Part of cultist AI | DEFERRED → B6 |
| F079 | Thaumic slime `createInstance` (spawns variants?) | monster/EntityThaumicSlime | Verified: extends vanilla Slime — split-on-death inherited (1.12 createInstance == vanilla split) | **FIXED** (verified) |
| F080 | Falling taint: block lookup on landing | EntityFallingTaint | Verified: carried BlockState placed on landing with canPlace guard + gore sound | **FIXED** (verified) |

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
