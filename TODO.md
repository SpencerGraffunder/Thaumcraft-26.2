# Thaumcraft 6 — Minecraft 26.3 (NeoForge) Migration Status

> Tracks the active **26.3 port** (migrated from 26.2 on 2026-09-28). The 26.2
> notes below are historical milestones; 26.3-specific work is recorded in the
> section at the top of this file.

## 2026-10-02 — arcane-category 1:1 fixes (commit c8dbbcd)

Continued the 1.12-vs-port audit into **crucible** (clean — 0 deviations) and
**arcane** (the audit's 19 flags were mostly false positives from item-naming
order + the 1.12 parser missing ingredients; the port was usually correct or
more complete). The genuine deviations, fixed:
- **morphic_resonator**: center was quicksilver → rare-earth nuggets (1.12).
- **brain_box**: center was brain_clockwork → the Mind item (1.12).
- **grapple_gun_tip**: port invented iron+mechanism → 1.12's 4× brass plate +
  rare-earth nuggets + tripwire hook (`BRB/RHR/BRB`).

Unfixable 1:1: **pedestal_eldritch** — 1.12 uses 8× eldritch **slab** + 1× tile,
but the port has no eldritch slab (only `eldritch_stone_tile`), so the all-tile
recipe is the best available.

---

## 2026-10-02 — IE* load crash fixed + infusion recipes restored to 1.12 (commit 4b48005)

User loaded the world and hit a **data-pack load failure**: all 8 IE* recipes
threw `IllegalStateException: Item must be non-empty`. Root cause: the
`InfusionEnchantmentRecipe` constructor called `ItemStackTemplate.fromStack(
ItemStack.EMPTY)` (the output is dynamic — the input tool + enchantment — so
there is no fixed result), and `fromStack` throws on an empty stack. The build
doesn't catch recipe-load errors, so this shipped silently and only surfaced on
world load. Fix: pass a `null` result from the IE* constructor and null-guard
`assemble()`/`getResultItem()` in `InfusionRecipeType`. (The IE* serializer never
serializes the result field, so null is safe.)

Then a full 1.12-vs-port infusion audit found the port had **invented** several
recipes (wrong central / ingredients / aspects / instability). Restored to exact
1.12:
- **traveller_boots**: leather boots (not thaumium) + 2× air crystal + 2×
  enchanted fabric + feather + cod; aspects volatus/motus 100; instability 1.
- **charm_undying**: totem of undying + 1× plate brass; aspect victus 25; inst 2
  (port had golden apple + shards + void seed).
- **lamp_fertility**: fire crystal (port had essence crystal).
- **lamp_growth**: green dye + earth crystal (port had bone meal + essence crystal).
- **void_robe_chest/helm/legs**: 1.12 materials (cloth armor + void plates +
  salis + fabric + leather / goggles + fabric; port had fabric + void seed +
  nether star); instability 6 (port had 4).
- **5 elemental tools**: removed the extra crystal-aspect I'd wrongly added (1.12
  essentia costs don't include the crystal's aspect).

Also added the missing **enchanted-book catalyst** to all 8 IE* recipes (1.12
surrounding components are `[enchanted_book, ingredient]`; the port had only the
ingredient). Corrected the earlier note: 1.12 IE* is **tool-based** (tool in the
matrix center), not book-based.

Note: the port's `Ingredient` codec has no per-slot `count` field (only
`HolderSet<Item>`), and the altar has 8 pedestals, so 1.12's "10 nuggets in one
slot" can't be expressed 1:1 — the elemental tools use 1 nugget (best available).

---

## 2026-10-01 — 1:1 recipe parity vs 1.12 (user: "make it just like 1.12") — DONE (commits a9dd2a6, 250de6c, b6bb2c1, 1ce474e)

User directive (2026-10-01): restore 1:1 faithfulness with 1.12 — no design
changes, same experience. Audited every 1.12 recipe (arcane 74 / infusion 57 /
crucible 43) against the port. Most "missing" names were just **renames** the port
uses (all 17 seals, foci `focus_basic/advanced/greater`, fortress armor, verdant
charms, `voidseer_charm`, hand mirror, sanity soap, all 7 crystal clusters, masks,
amulets, and the brain / biothaumic-mind / cloud-ring / curiosity-band /
arcane-bore recipes all already exist). The genuine gaps, all now fixed:

**1. Five recipes in the wrong category (a9dd2a6).** 1.12 makes these
arcane-workbench recipes; the port had them as infusion. Moved to
`data/thaumcraft/recipe/arcane_workbench/` (deleted the infusion copies):
`grapple_gun`, `stabilizer`, `vis_generator`, `golem_module_aggression`
(1.12 `modaggression`), `golem_module_vision` (1.12 `modvision`).

**2. Elemental tools (250de6c + b6bb2c1).** Added the 4 missing infusion recipes
(`elemental_axe/hoe/pick/shovel`) and corrected `elemental_sword` (the port had
invented a shards+blaze-powder recipe). All 5 now match 1.12: input = thaumium
tool, 2× essence crystal + `nugget_rareearth` + `greatwood_planks`, per-tool
aspects, research `ELEMENTALTOOLS`, instability 1.

**3. Eight infusion-enchantment (IE*) recipes (1ce474e).** The port had the recipe
type (`InfusionEnchantmentRecipe` + serializer), the `EnumInfusionEnchantment`
enum, the effects (`ToolEvents`), and the `INFUSIONENCHANTMENT` research, but zero
recipes. Added all 8 to `data/thaumcraft/recipe/infusion_enchantment/` matching
1.12 (enchantment enum, 1.12 aspects mapped to port IDs, 1.12 ingredient each):
burrowing (rabbit_foot), collector (lead), destructive (tnt), refining (salis),
sounding (map), arcing (redstone_block), essence (crystal_essence), lamplight
(nitor). Note: the port's IE* is **tool-based** (infuse the tool directly in the
matrix) vs 1.12's book-based (infuse a book, then apply) — a pre-existing port
design choice, left as-is.

Build green; all recipe references (enum/ingredient/aspect/research) cross-checked
against the registry. In-game test pending (user's turn — recipe changes only need a
reload, no restart).

## 2026-10-01 — GUI render order + arcane-workbench recipe research gates — FIXED (commits 597e330, a713318, 452c17c)

Two systemic bugs found while testing the arcane workbench after the
recipe-manager fix (456ee22). Build green, 86/86 tests, jar installed in the
Modrinth **NeoForge 26.3** profile.

**1. Items hidden behind the GUI background (all 17 container screens).**
In MC 26.3 screens render in *strata*: `Screen.extractBackground` (stratum 0,
drawn first) → `extractRenderState`→`extractContents` (stratum 1, draws the
slot items via `extractSlots`). Every Thaumcraft container screen overrode
**`extractContents`** to blit its GUI texture, so the background (which includes
the player-inventory area) painted in the *items* stratum — on top of the items
— hiding them (movable but invisible). Fix: renamed each screen's
`extractContents`→`extractBackground` (now calls `super.extractBackground`
first) for all 17 container screens, matching vanilla's `AbstractFurnaceScreen`.
The base `extractContents` now runs unoverridden and draws the items on top.
Nested inner-class `extractContents` in ResearchBrowser/Seal are separate
renderables and were left untouched.

**2. Wrong research gates on arcane-workbench recipes (10 fixed).**
The thaumometer (and activator rail) had the WRONG research gate, so players
following FIRSTSTEPS ("Getting Started" shows the thaumometer as the next
craft) could never craft it — it was locked behind `UNLOCKAUROMANCY` (needs
alchemy + deep/high discovery) instead of 1.12's `FIRSTSTEPS@2`. Audited every
ported arcane-workbench recipe against the 1.12 `ConfigRecipes` source and
fixed 10 gates:
- thaumometer `UNLOCKAUROMANCY`→`FIRSTSTEPS@2`
- activatorrail `BASICTURRET`→`FIRSTSTEPS`
- caster_basic / vis_resonator `UNLOCKAUROMANCY`→`UNLOCKAUROMANCY@2` (dropped @stage)
- infusion_matrix `INFUSION`→`INFUSION@2`
- mind_clockwork `MINDCLOCKWORK`→`MINDCLOCKWORK@2`
- wand_workbench `BASEAUROMANCY`→`BASEAUROMANCY@2`
- inlay `INFUSION`→`INFUSIONSTABLE`
- smelter_basic `ESSENTIASMELTER`→`ESSENTIASMELTER@2`
- metal_alchemical_advanced `ESSENTIASMELTERVOID`→`ESSENTIASMELTERVOID@1`

`isResearchKnown` already parses the `@stage` suffix (`getResearchStage(key) >= n`)
and the recipe CODEC reads `research` as a plain string, so `@2` is preserved
and enforced. The other ~90 recipes match 1.12 (renames like
`ArcanePedestal`→`pedestal_arcane` keep the correct gate). Golem modules +
grapple gun exist as **infusion** recipes in the port (design difference, not a
gate bug); banner recipes are port-only (no 1.12 arcane equivalent).

## 2026-09-30 — Salis Mundus follow-ups: dust sparkles + research deadlock — FIXED (commit 283af73)

Two follow-up bugs from testing the Salis Mundus / arcane-workbench chain after
the FX audit. Both now fixed; build green, 86/86 tests, jar installed in the
Modrinth **NeoForge 26.3** profile.

**1. Bookshelf/dust "witch particles" (ItemMagicDust.doSparkles).**
The dust-sparkle effect was still a vanilla `ParticleTypes.ENCHANT` (enchant/
witch sparkle) stub with the real FX call commented out. Rewrote it to the
1.12-faithful `FXDispatcher.drawSimpleSparkle` (50-pt hand→block line, white/
green/blue, "floaty" first third) + `drawBlockSparkles` per trigger sparkle
position + the `DUST` sound. (The FX audit covered `FXDispatcher` methods but
missed this separate caller.)

**2. Crafting table → arcane workbench never fires (research deadlock).**
Root cause: the dust trigger is gated on `FIRSTSTEPS@1` (stage ≥ 1), but
`FIRSTSTEPS` could **never reach stage 1**. The `ResearchBrowserScreen` click
handler only sent `PacketSyncResearchFlagsToServer` (flags/popups) and **never**
the `PacketSyncProgressToServer(first=true, checks=false)` that 1.12's
`GuiResearchBrowser.mouseClicked` sends when you click an *unknown-but-
unlockable* research. So research stayed "unknown" (map entry kept blinking),
the stage-1 gate was unsatisfiable, and the whole chain was a circular
deadlock (need the workbench to research `FIRSTSTEPS`, need `FIRSTSTEPS` to
make the workbench).

Fix (matches 1.12 `GuiResearchBrowser.mouseClicked`): clicking an unknown +
`canUnlockResearch` research now sends `PacketSyncProgressToServer(key, true)`
(server: `doesPlayerHaveRequisites` checks **parents only** → `addResearch` →
stage 1) + the "Researching: <name>" popup; clicking a known research clears
the RESEARCH/PAGE flags (stops blinking) and, if at the final stage, sends a
progress packet to complete it. This unblocks `FIRSTSTEPS@1` (and every other
first-stage research), so the Salis→crafting-table→workbench conversion works.

Note: the `[SALIS-DBG]` diagnostics added to `ItemMagicDust.useOn` /
`DustTriggerSimple.getValidFace` are still in place (server-side logs) —
remove once the user confirms the full chain works in-game.

## 2026-09-30 — Full FX/animation audit vs 1.12 — ALL stubs fixed (todo #40–#43 done)

User directive: audit **every** FX/animation in the 26.3 port against 1.12
(called out cauldron, infusion altar, wand/gauntlet foci) and make all
appearances match 1.12. Result: the ~30 `FXDispatcher` methods that were
vanilla-`ParticleTypes` stubs (plus the core particle plumbing) are now
1.12-faithful ports. Build green, 86/86 tests pass, jar installed in the
Modrinth **NeoForge 26.3** profile.

**Core particle plumbing (FXGeneric):**
- **X-mirror UV fix:** 26.3 `renderRotatedQuad` maps −x→`getU0()`, +x→`getU1()`
  (unmirrored), but 1.12 `FXGeneric` renders its sprite X-**mirrored** by default
  (−x corner gets the right sprite edge). `getU0`/`getU1` now compensate so the
  default is 1.12-mirrored and `setFlipped(true)` restores the unmirrored look.
- **Angled particles (1.12 `setAngles`):** new `extract()` override builds the
  1.12 GL transform `R_cam · Ry(−yaw+90) · Rx(pitch+90) · Rz(roll)` as a quaternion
  (1.12 rotated in view space; the 26.3 pipeline rotates in world space, so the
  camera's camera→world rotation is prepended). Replaces 1.12's GL11 billboard
  bypass, which is unavailable in the modern pipeline.
- **Wind:** `setWindStrength` replaced by 1.12-faithful `setWind(d)` — magnitude
  `0.1·d` (1.12's internal source magnitude folded in) and the Z-sign corrected
  to match `Utils.rotateAroundY`. Call sites now pass the raw 1.12 wind value.
- **Rotation 2π factor + green-channel lerp typo** (both latent bugs) fixed in a
  prior commit; `tick()` does `roll += rotationSpeed·2π` and lerps green→endG.

**FXDispatcher method rewrites (1.12-exact sprites/colors/lifetimes/motion):**
`drawFireMote`, `drawAlumentum`, `drawTaintParticles`, `drawLightningFlash`,
`spark`, `sparkle`, `drawGenericParticles` (18-arg + `drawGenericParticles16` +
the `GenPart` overload with full color-range/grid/rotstart/slowDown/grav/delay),
`crucibleBubble`/`Boil`/`Froth`/`FrothDown`, `drawBamf` (all overloads),
`drawWispyMotes*`, `scanHighlight`, `drawBlockSparkles`, `drawSimpleSparkle`,
`drawLineSparkle`, `drawBlockMistParticles`(+Flat), `drawFocusCloudParticle`,
`visSparkle`, `drawLevitatorParticles`, `drawStabilizerParticles`,
`drawGolemFlyParticles`, `drawPollutionParticles`, `essentiaTrailFx`/`DropFx`,
`drawVentParticles`(×2)/`2`, `jarSplashFx`, `waterTrailFx`,
`drawInfusionParticles1–4`, `burst`, `excavateFX` (now real
`level.destroyBlockProgress`), `blockRunes`/`2` (+0.5 offset restored),
`drawPedestalShield`, `voidStreak`, `furnaceLavaFx`, `bottleTaintBreak` (8× item
particles + splash sound), `cultistSpawn`, `pechsCurseTick` (angled FXGeneric +
wisp motes), `wispFXEG`, `drawSlash`, `boreDigFx`, `boreTrailFx`,
`splooshFX`/`taintsplosionFX`/`tentacleAriseFX`/`slimeJumpFX`/`taintLandFX`
(`FXBreakingFade` + slime-ball item), `drawNitorCore`/`Flames`,
`drawSimpleSparkleGui`, `drawWispyMotesEntity`, `drawWispParticles`. `beamCont`/
`beamBore`/`arcLightning`/`arcBolt` were already 1.12-faithful (verified). `sonicBoom`
is port-specific (no 1.12 equivalent).

**Delay queue:** 1.12's `ParticleEngine.addEffectWithDelay` is reproduced by a
static per-particle delay list in `FXDispatcher`, drained by
`ClientTickHandler` → `FXDispatcher.tickDelayed()` each client tick.

**Particle-class API additions:** `FXBoreSparkle.setColor`,
`FXBoreParticles.setAlphaF`, `FXBreakingFade.setColor`/`setAlphaF`,
`FXVent`/`FXVent2.setAlphaF`, and a position-based `FXShieldRunes` constructor
(origin-orbiting) for the stationary pedestal shield.

**Crucible FX (was entirely missing on the client):** `TileCrucible` now has a
`drawEffects(rand)` client method (froth when heat>150, 8× frothDown at the rim
when aspect vis>500, a colored bubble every ~6 ticks) called from
`BlockCrucible.animateTick` on the client when fluid is present. Event-driven FX
(boil on smelt/bubble, bamf on craft/spill) travel via a new
`PacketFXCrucible` (pos, type, data) — server sends `(99,0)`/`(2,1)`/`(2,5)` from
`attemptSmelt`/dissolve/`spillAll`; the client handler plays the 1.12 bamf +
spill sound + 10× `crucibleBoil`.

**Infusion altar FX (was missing in `doEffects`):** now spawns 1.12's
`blockRunes` (every tick while crafting), per-source particles
(`drawInfusionParticles1–4` for player/pedestal/block sources), the instability
spark (crafting & stability<0) and problem-block spark. `clientTick` also runs
`scanSurroundings` when `checkSurroundings` is set (mirrors 1.12 `update()`).

**1:1 UV parity sweep (2026-09-30, all grid particle classes):** every TC
grid-based particle now matches 1.12's UV convention. Findings + fixes:
- **X-mirror applied** (1.12 renders grid sprites X-mirrored; 26.3's
  `renderRotatedQuad` is unmirrored): `FXWisp`, `FXBoreSparkle`, `FXFireMote`,
  `FXSwarm`, `FXSmokeSpiral`, `FXSlimyBubble`, `FXPlane`, `FXBlockRunes`,
  `FXVent`, `FXVent2`, `FXVisSparkle` — the `state.add` call now passes
  `(u1, u0)` so the −x corner gets the right sprite edge.
- **Full-texture → 1/64 grid cell** (these sampled the whole atlas before):
  `FXVent` (cell = `part%16, part/64`), `FXVent2` (same formula), `FXVisSparkle`
  (cell = `age%16, row 8`), `FXBlockRunes` (cell = `runeIndex%16, row 6`; ctor
  now sets `runeIndex = rand 0–15`).
- **Missing TC-atlas binding fixed:** 9 grid classes (`FXVent`, `FXVent2`,
  `FXBlockRunes`, `FXBoreSparkle`, `FXSlimyBubble`, `FXSwarm`, `FXSmokeSpiral`,
  `FXPlane`, + `FXVent2`'s new `extract`) had **no `getLayer()` override**, so
  they sampled the *vanilla* particle atlas with TC grid UVs. All now return
  `TC_PARTICLES_LAYER_TRANSLUCENT` (the dedicated 1024×1024 TC atlas).
- **Item/block sprites are NOT mirrored** in 1.12 (`FXBoreParticles`,
  `FXBreakingFade`) — left natural (correct).
- **`FXBlockWard`** V-flip applied (1.12 full-texture: top→V=1.0, bottom→0.0).

**Known residual simplifications (pipeline limits, documented not fixed):**
- `FXBlockRunes`/`FXPlane` rotate their sprite 90° in 1.12 (sprite-U aligns with
  world-Y); the 26.3 axis-aligned `state.add(u0,u1,v0,v1)` cannot express a
  rotated UV, so they render axis-aligned (X-mirrored) instead.
- `FXBlockWard` in 1.12 is a 15-frame animated particle (`hemis1–15.png`,
  additive); the port renders a single billboard quad — the frame animation is
  not ported.

## 2026-09-29 — Salis Mundus bookshelf bug — ROOT CAUSE FOUND + FIXED (todo #36 done)

User report: right-clicking a bookshelf with Salis Mundus (in the 26.3
profile, after the dream was received at 08:10:22 per latest.log) produced no
sparkle animation and no Thaumonomicon drop.

**Root cause (confirmed via [SALIS-DBG] in-game test, 09:57 log):** the whole
trigger chain works (client sync received `!gotdream=true`, trigger valid on
both sides, swapper dequeued and EXECUTING) — but in `ServerEvents.tickBlockSwap`
the item-placement logic called `Block.byItem(target.getItem())`, which returns
`Blocks.AIR` for non-block items like the Thaumonomicon. It then fell into
`level.removeBlock(pos, false)` — destroying the bookshelf with no loot and
never spawning an `ItemEntity`. The "animation" the user saw was only the
`PacketFXBlockBamf` packet, whose ported `drawBamf` used placeholder particles
and `drawCurlyWisp` was a `ParticleTypes.WITCH` stub (the "enchanting glyphs").

**Fixes (this commit):**
1. `ServerEvents.tickBlockSwap`: non-block target results now spawn a
   `EntitySpecialItem` (floating item entity, 1.12 behavior) at
   `pos + (0.5, 0.1, 0.5)` with zeroed motion, after removing the block.
   (The ported `EntitySpecialItem` cancels `ItemEntity`'s 0.04 gravity for a
   net-zero hover, identical to 1.12.)
2. `FXDispatcher.drawBamf` rewritten 1.12-faithful: 8–10 smoke sprites
   (texture 123, 5-sprite anim, alpha 1.0→0.1, scale 0.3→0.4–0.7 block units
   (= 1.12 3.0→4.0–7.0 × 0.1), slowDown 0.7, random ±1°/tick spin), flair:
   2–4 wispy motes + white flash (texture 77, 1.0–1.2→0 size), plus
   2–4 curly wisps (texture 60–63, scale 0.5→1.0–1.4, ±2–4°/tick spin,
   color → dark purple 0.1/0.0/0.1). Port quad size is block units = 1.12
   scale × 0.1; 1.12 sprite indices carry over (particles.png is
   byte-identical).
3. `FXDispatcher.drawWispyMotes*` rewritten 1.12-faithful: grid 64, sprites
   512–527, alpha keyframes 0→0.6→0.6→0, scale 0.1→0.05, wind 0.0001
   (1.12 setWind(0.001) × 0.1 source-magnitude), random movement 0.0025,
   OnBlock colors 0.4+0.6r / 0.6+0.4r / 0.6+0.4r.
4. `FXGeneric.tick()` pre-existing bugs fixed: missing 2π rotation factor
   (`roll += rotationSpeed * 2π` now, matching 1.12) and green-channel lerp
   typo (was lerping toward endB). Both were latent — only scan-source FX use
   rotation and it passes 0.
5. `setRotationSpeedWithStart` startAngle is in TURNS (×2π), matching 1.12's
   `start * 2π` — so 1.12 `setRotationSpeed(rand, ±speed)` maps 1:1.

**Status:** build green (86/86 tests), jar (14,467,001 bytes) installed in the
Modrinth `NeoForge 26.3` profile. **NEXT: user retests the bookshelf click —
expect poof sound, purple smoke + wisps + white flash, and a hovering
Thaumonomicon. [SALIS-DBG] logs are still in place (todo #37 removes them
after this verification).**

## 2026-09-29 — Salis Mundus bookshelf bug (diagnosis history, resolved above)

User report: right-clicking a bookshelf with Salis Mundus (in the 26.3
profile, after the dream was received at 08:10:22 per latest.log) produced no
sparkle animation and no Thaumonomicon drop.

**What is verified working (code + world inspection):**
- Trigger registered: `DustTriggerSimple("!gotdream", Blocks.BOOKSHELF,
  thaumonomicon)` at ConfigMultiblocks (log: "Registered 9 dust triggers").
- Server-side flag granted: `!gotcrystal` (08:08:17 crystal pickup) and
  `!gotdream` (08:10:22 wake-up) are both present in the player's saved NBT
  (`saves/New World/players/data/<uuid>.dat` → `neoforge:attachments` →
  `thaumcraft:knowledge` → research list). So `progressResearch`/syncList/
  capability storage all work server-side.
- Chain traced clean: `ItemMagicDust.useOn` → `IDustTrigger.triggers` loop →
  `DustTriggerSimple.getValidFace` (block match + `knowsResearch("!gotdream")`)
  → `execute` → `ServerEvents.addRunnableServer(50 ticks)` → `addSwapper` →
  `tickBlockSwap` (bookshelf → Thaumonomicon item, bamf FX).

**Remaining suspects (client-side only — server state is proven OK):**
1. Client knowledge never received `!gotdream` (PacketSyncKnowledge not
   delivered/applied) → client `getValidFace` fails → no sparkles AND no
   UseItemOn packet → server never executes.
2. Client `useOn` not reached (e.g. crouch early-return) or client knowledge
   capability null.

**Instrumentation (temporary, [SALIS-DBG] prefix — remove after diagnosis,
todo #37):** commits b0fd157 + 5d033d5. Logging at: ItemMagicDust.useOn
(entry/valid/no-trigger, both sides), DustTriggerSimple.getValidFace
(research-fail) + execute, PlayerEvents.livingTick (sync fire),
PacketSyncKnowledgeClient (received: research count + knows-!gotdream;
dropped case), ServerEvents.tickBlockSwap (swapper dequeue/reject/execute).
Jar installed in profile 08:50 (14,466,017 bytes). **NEXT: user reproduces
the click → grep [SALIS-DBG] in
`~/Library/Application Support/ModrinthApp/profiles/NeoForge 26.3/logs/latest.log`.**

Also fixed in b0fd157 (permanent): creative-search tooltip NPE spam
(`ItemVoidseerCharm` → `ThaumcraftCapabilities.getWarp(null)` via
`SessionSearchTrees` tooltip generation on ForkJoinPool threads) —
null-player guards in ThaumcraftCapabilities.getKnowledge/getWarp,
Thaumcraft.onItemTooltip (hasPlayer gate), ItemVoidseerCharm.

## 26.3 Migration — full API port (2026-09-28/29, in progress → build green)

User-directed migration: 26.2 → **NeoForge 26.3.0.33-beta** (MC 26.3, Java 25,
pack format 121). Fresh Modrinth profile `NeoForge 26.3`.

- **Build plumbing:** `gradle.properties` (neo_version 26.3.0.33-beta, pack
  format 121, mod_version 6.2.0+26.3, Curios 17.0.0-beta.2+26.3, JEI 31.7.0.47);
  `build.gradle` now globs the patched MC jar dynamically (no more hardcoded
  `neoFormJoined26.2-2` path); `scripts/fix_neoform_artifacts.py` gained 2
  26.3-only decompiler-artifact fixes (CustomPayload 4-arg codec call sites,
  LevelEventHandler case-2003 variable renames); mods.toml template updated
  (Curios range [17.0.0,)).
- **Transfer API (biggest break):** NeoForge deleted `IItemHandler`,
  `IFluidHandler`, `FluidTank`, `ItemHandlerHelper`, `InvWrapper`,
  `SidedInvWrapper`, `SlotItemHandler`. New transaction-based
  `net.neoforged.neoforge.transfer.ResourceHandler<T>` framework. Migrated ~40
  files: capabilities (`Capabilities.Item.BLOCK` / `Capabilities.Fluid.BLOCK`),
  `TileCapabilityRegistration`, 5 tile inventories, 8 seal classes, menus,
  `ThaumcraftInvHelper` (now `WorldlyContainerWrapper` /
  `VanillaContainerWrapper.of`), `PouchCurios` (extends
  `ItemStacksResourceHandler`, Curios 17's `IDynamicStackHandler` is
  ResourceHandler-based), `ItemFocusPouch` (writeTag/readTag →
  serialize/deserialize). Two new adapters keep old call shapes:
  `thaumcraft.api.ItemHandlers` + `thaumcraft.api.FluidTanks` (nesting-safe
  Transaction handling).
- **Worldgen:** `Feature` is now a plain interface; `ConfiguredFeature` /
  `PlacedFeature` classes gone → 16 custom feature types registered in
  `Registries.FEATURE_TYPE` (per-variant, `MapCodec.unit(...)`), 19 configured
  feature JSONs moved to `data/thaumcraft/worldgen/feature/` (config wrapper
  stripped), 19 placed-feature JSONs + 10 biome modifiers unchanged.
- **Tools:** `AxeItem`/`ShovelItem`/`HoeItem`/`PickaxeItem`/`SwordItem`
  removed → 12 tool items rebuilt on `Item.Properties.axe/shovel/hoe/pickaxe/
  sword(ToolMaterial, ...)`. `ToolMaterial` is now a record (already ported).
- **Rendering:** `mulPose(Quaternionf)` → `rotate`/`rotateDegrees` (16 sites);
  `submitModel`/`submitModelPart` lost the trailing `breakProgress` arg
  (8 tile renderers); `CameraRenderState` exposes `orientation` not
  `rotation()`; `Block.codec()` overrides deleted (7 blocks).
- **Entities:** `Entity.drop` → `LivingEntity.drop(ItemStack, boolean,
  Prediction)` (12 sites); arm-swing fields (`swinging`/`swingTime`/
  `handleEntityEvent(4)`) removed → `swing(hand, SwingAnimation.DEFAULT, true)`;
  `hurtMarked` → public `needsSync`; `blocksMotion()` →
  `isCollisionShapeFullBlock(level, pos)`; `invulnerableTime` →
  `setInvulnerableTime(0)`.
- **Fuel:** `FuelValues`/`level.fuelValues()` removed → `TileSmelter` reads
  `DataComponents.COOKING_FUEL` via `CookingFuel.burnTime().get(lootContext, 0)`;
  `ItemPrimordialPearl.getBurnTime()` (dead override) deleted.
- **Misc:** `PushReaction.DESTROY`→`POPPED`, `BLOCK`→`IMMOVEABLE`; GLFW key
  constants → literal ints + single-arg `InputConstants.isKeyDown` (GLFW not on
  compile classpath); `LeavesBlock` ambient-sound float →
  `AmbientLeavesBlockSoundPlayer.noAmbientSound()`; `KeyMapping` moved to
  `net.minecraft.client`; `Block.playerDestroy` 6-arg.

- **`runServer` smoke test (fresh 26.3 world): boots to `Done`** — zero
  registry/worldgen errors, clean `level.dat`, research (148) + runtime
  registration complete. Surfaced & fixed 3 runtime-only data issues the
  compile never caught:
  - **Curios dep range:** `mods.toml` `[17.0.0,)` rejected the installed
    `17.0.0-beta.2+26.3` — NeoForge uses Maven `VersionRange`, which sorts a
    pre-release below its release. → `[17.0.0-beta,)` (accepts beta + future
    17.x, still rejects 16.x).
  - **Ore feature `state`:** 26.3 `BlockState.CODEC` takes a plain string or an
    `id`-keyed map; the old `{"Name":"..."}` object is gone. 3 ore features
    (`ore_amber`/`ore_cinnabar`/`ore_quartz`) → plain-string `state`.
  - **`crystals` item tag dir:** legacy `tags/items/` (plural) → `tags/item/`
    (singular), matching every other tag in the mod. The Salis Mundus recipe's
    `#thaumcraft:crystals` ingredient now resolves.

**Status: BUILD GREEN (86/86 tests, 0 TODOs), `runServer` boots to `Done`.**
Jar `thaumcraft-6.2.0+26.3.jar` installed into the `NeoForge 26.3` Modrinth
profile; **Curios 17-beta jar added to the profile** (hard dep). The 26.2
branch (`feat/26.2-port`, 26.2.0.76 / 6.2.0+26.2 / pack 108) was re-verified
**build green with 86/86 tests** the same day, so both branches ship from a
verified tree. **Pending: user in-game launch verification** — remaining
first-boot risks are client-side (rendering, focus pouch, crossbow turret
animation, smelter fuel) and in-world golem/seal item transfer; server-side
registration & worldgen are verified.

### CI build fixed + local build switched to the binary-patch pipeline (2026-09-29)

GitHub Actions was failing on **every** push since the 26.2-era `doFirst` jar
swap (commit 2005d6e) because CI (GitHub Actions exports `CI=true`) makes
NeoGradle auto-disable the decompiler and take the **binary-patch pipeline**
(PMJ `--apply-patches` from `patches.lzma` → `neoFormApplyAccessTransformer`
→ `neoFormApplyInterfaceInjections`). That pipeline publishes a fully-patched
jar straight into `ng_dummy` and produces **no** `build/neoForm/.../recompile/
outputs.jar`, so the old unconditional swap threw `GradleException`.

- **`build.gradle` swap is now conditional (3-branch):** if `ng_dummy` is
  already NeoForge-patched (checked by looking for `neoforged` in
  `Item.class`'s constant pool via `isPatchedMcJar`) → skip the swap
  (binary mode, both CI and local). Else if a patched recompile jar exists
  under `build/neoForm/` → swap it in (source mode, where `selectRawArtifact`
  only publishes a 197-byte stub into `ng_dummy`). Else throw. Also fixed a
  silent Groovy bug in `isPatchedMcJar` (`readBytes()` → `getBytes()` on the
  `InputStream`; the old form threw `MissingMethodException` swallowed by the
  catch, making the check always return false).
- **`gradle.properties` now sets
  `neogradle.subsystems.decompiler.enabled=false`** so local builds use the
  same binary-patch pipeline as CI and compile against the exact API the game
  runs with. **Gotcha:** the property is prefixed — a bare
  `decompiler.enabled` is silently ignored (`WithPropertyLookup` prepends
  `neogradle.subsystems.`).
- **`TaintHelper.java:155`** was compiled against a stale **4-arg**
  `BlockStateBase.getMapColor(BlockState, LevelReader, BlockPos, MapColor)`
  that only existed in the local source-pipeline recompile jar; the true 26.3
  runtime API (verified in the Modrinth runtime jar and the CI binary-patched
  jar) is the **2-arg** `getMapColor(BlockGetter, BlockPos)` (NeoForge routes
  it through the block). Fixed to the 2-arg form — the old code was a latent
  `NoSuchMethodError` waiting to fire on taint spread.
- **`~/.gradle/caches/ng_execute` trap:** NeoGradle's `ExecuteTask` cache is
  keyed on declared task inputs only — the PMJ `--apply-patches` flag is not
  one of them — so on a machine that ran the source pipeline first, the
  binary-mode `neoFormSetup` restores the **unpatched clean-join** cache entry
  (and `--rerun-tasks` does not override it; the task checks this cache inside
  its action and calls `setDidWork(false)`). Fix applied on this machine:
  moved the poisoned cache to `~/.gradle/caches/ng_execute.bak-20260929` and
  re-ran the pipeline so a correct patched entry was written. If you ever
  re-enable the decompiler here, clear `ng_execute` again or source mode will
  serve the binary-patched jar as its "clean join".

## In-Game Bug Fixes — 3 follow-up issues (2026-09-28, shipped 4e63df2)

User follow-ups to the 6-bug batch; supersedes the crystal-texture and
Salis-Mundus items below.

- **Salis Mundus recipe**: restored the authentic 1.12 **shapeless
crafting-table** recipe (3 crystals + flint + bowl + redstone) using a new
`thaumcraft:crystals` item tag. Removed the crucible recipe — it created a
circular dependency (a crucible itself requires Salis Mundus). Reverted the
Strange Dreams book text to describe the crafting recipe (bowl/flint/crystals/
redstone), matching 1.12.
- **Crystal textures**: the generated gem sprites did not match 1.12. Regenerated
the 6 `vis_crystal_*` item sprites from the authentic 16×16 1.12
`crystal_essence.png` gem art (faceted silhouette + specular highlights), tinted
per-aspect with the exact 1.12 Forge multiplicative formula
(`final = pixel * tint / 255`).
- **Aspect tooltip icons**: essentia items now render an aspect **icon row**
(symbols + amounts), port of the 1.12 `RenderTooltipEvent.PostBackground` aspect
row — alongside the existing text lines. New files:
`common/tooltip/AspectTooltipComponent` (server marker),
`client/tooltip/AspectClientTooltipComponent` (renderer),
`client/tooltip/AspectTooltipEvents` (factory registration + gather hook).
- **Build hardening**: `build.gradle` now tolerates the 3 rejected neoForm
  userdev patch hunks and reuses the good recompiled jar, so the build no longer
  breaks when the decompile cache is invalidated.

Verified: `./gradlew build` green, **86/86 tests**, 0 TODOs in src/. Jar
reinstalled into the `NeoForge 26.2` Modrinth profile (MD5 matches built jar).

## In-Game Bug Fixes — 6 reported issues (2026-09-27, shipped d6d8158)

User-reported in-game defects, all fixed + audited for similar cases:

- **Keybind names**: `key.thaumcraft.focus` / `key.thaumcraft.misc` lang keys were
  missing → controls menu showed raw keys. Added with the 1.12 names ("Change
  Caster Focus" / "Misc Caster Toggle").
- **Pillar transparency**: all 3 pillar block textures (ancient/normal/eldritch)
  were 64×64 with only ~41% opaque (designed for a 1.12 3D OBJ model, now on a
  flat `cube_column`) → visible see-through holes. Filled the transparent grooves
  to 100% opacity (darkened stone shade) so the pillars render solid.
- **Crystal items looked like candles**: the 6 `vis_crystal_*` items used a thin
  3D tapered column (`vis_crystal_base`) with a sparse texture. Replaced with
  per-aspect 2D gem sprites colored via `Aspect.getColor()`.
- **Essentia/aspect symbols not shown**: the 1.12 `ItemTooltipEvent` handler was
  never ported. Re-added in `ClientModEvents.onItemTooltip` — runic charge, warp,
  vis discount, charge, and `aspect x amount` on `IEssentiaContainerItem`.
- **Strange Dreams book showed literal `\n`**: the lang values used `\\n` (escaped
  backslash-n). Fixed to real newlines.
- **Salis Mundus hint mismatched the recipe**: the book described the 1.12
  bowl+flint crafting recipe, but 26.2 uses a crucible recipe (redstone catalyst
  + 6 primal aspects + praecantatio). Rewrote the dream text to match the actual
  crucible recipe.

Verified: `./gradlew build` green, 0 TODOs in src/, **86/86 tests**. Jar
reinstalled into the `NeoForge 26.2` Modrinth profile.

## 26.2 Port — Compile Re-Green (2026-09-25, shipped 63a9215)

A corrupted session left the working tree mid-port with ~441 compile errors
(HEAD `c21694e` itself was not green). Recovered by grouping errors into 10
file-batches, fixing in parallel (worker subagents + orchestrator), and
iterating `compileJava` checkpoints until clean.

Notable 26.2 API migrations in this final batch:
- `ValueIOSerializable` (NeoForge) requires `serialize(ValueOutput)` /
  `deserialize(ValueInput)`; `ValueInput.read(name, codec)` / `ValueOutput.store(name, codec, value)`
- `CompoundTag.getList(String)` returns `Optional<ListTag>` — use `getListOrEmpty`
- `ResourceKey.getLocation()` → `identifier()`
- `NonNullList` moved `net.minecraft.util` → `net.minecraft.core`; `withSize(int, E)`
  takes a fill value, not a supplier
- `Collections.unmodifiableSet` requires a `Set` (`Map.values()` is a `Collection`)
- `DeferredRegister.create(Registries.X, modid)` infers T; `DeferredHolder<R, T>`
- `Holder.unwrapKey()` → `Optional<ResourceKey<T>>`
- `DamageSource.MAGIC` static removed → `level.damageSources().magic()`
- `Level.setBlockEntityDirty(pos, be)` → `level.blockEntityChanged(pos)`

`./gradlew build`: **BUILD SUCCESSFUL**, 0 TODOs in src/.

## 1.12 Book Parity — Star Markers (2026-09-12, shipped 3c157f6)

User asked for the 1.12 "new" star markers (gold star on a research icon /
category when something new happened; clears when the page is opened).
Audit of the port found the system 90% present (flag set on server,
synced via PacketSyncKnowledge, map-icon stars + hover tooltips drawn,
flags cleared on open) but **broken end-to-end**:

- **Root cause**: `ResearchManager` sets RESEARCH/PAGE flags and marks
  `ResearchManager.syncList` (ResearchManager.java:91/178/273), but
  `PlayerEvents.livingTick` only checked `PlayerEvents.syncList` (its own
  HashSet, never populated) → `PacketSyncKnowledge` never sent after
  research completion → stars/toasts only appeared after rejoin.
  1.12 reference (PlayerEvents:123) removes from `ResearchManager.syncList`.
- **Fix** (`PlayerEvents.java` livingTick): now consumes both sets —
  `syncList.remove(name) || ResearchManager.syncList.remove(name) != null`.
- **Missing 1.12 feature added**: category-sidebar stars. `CategoryButton`
  (ResearchBrowserScreen.java) now computes per-category nr/np (any known
  research in the category with RESEARCH/PAGE flag) and draws the gold star
  (texture 176,16 / 208,16 @ 0.25 scale, alpha 0.7) at (x-2, y-2) / (x-2, y+9)
  + hover lines "Newly discovered research" / "New page added" — 1:1 with
  GuiResearchBrowser:1118-1148. (26.2 pose API is 2D: `scale(0.25f)`.
  Lambda can't mutate locals → `boolean[] newFlags` holder.)

Verified: build green, **86/86 tests**. Jar `thaumcraft-6.2.0+26.2.jar`
reinstalled into the `NeoForge 26.2` Modrinth profile.

## 1.12 Book Parity — Round 5 (2026-09-12, built & installed; in-game check pending)

- **Nitor item display (16 colors)**: item models were parented to the 3D
  `cube_all` block model, which all rendered as the plain `nitor_core` cube.
  Now 1.12-style flat generated items: `minecraft:item/generated` with
  `layer0: thaumcraft:block/nitor_<color>` (new 16×16 real-art PNGs, verified
  non-placeholder) over `layer1: thaumcraft:block/nitor_core`. Block models
  unchanged (placed blocks stay 3D cubes).
- **Research page text width**: `TEXT_WIDTH` 104 → **140** font-pixels (104
  wrapped too aggressively vs 1.12);
  `ResearchPageScreen.java:55-62`.
- **Research completion semantics** (`ResearchPageScreen.java:162-169`):
  `isComplete` was `playerKnowledge.isResearchComplete(key)`; now
  `isComplete = currentStage > stages.length` — 1.12 shows the current stage's
  "pre" text until the player has progressed *past* the last stage, then the
  final "post" text + addenda. Matches the map-view blink (blinking = not
  complete).
- **Requirement icon rows** (`ResearchPageScreen.java:597-608`): start moved
  from `sh + PANE_HEIGHT - 22` to `sh + PANE_HEIGHT + 13` — 1.12 puts the
  lowest (research) row at ~`sh+176`, near the scaled book's bottom; the old
  position floated the rows up into the text area (the ALUMENTUM "icons over
  the text" bug).
- **Scan entity names** (`research/scans.json`): 12 `name` keys renamed from
  1.12 legacy `entity.<Name>.name` to the 26.2 namespaced
  `entity.thaumcraft.<Name>` keys that the lang files already define
  (Wisp, ThaumSlime, Firebat, TaintSeed, …).

Verified: `CI=true ./gradlew build` green, **86/86 tests**, jar
`thaumcraft-6.2.0+26.2.jar` (md5 `12bcc08ec26caccc05dcce5b829e0566`) installed
into the `NeoForge 26.2` Modrinth profile. **User in-game verification pending:**
nitor items show the colored overlay in inventory; book text wraps at ~140px;
requirement rows sit at the book bottom, not over the text; scans show entity
names; research text flips pre→post on completion.

## P0: Thaumonomicon recipe pages broken — RESOLVED (2026-09-09)

Symptom (user-reported): in the book, the arcane-workbench recipe popup
showed a bare white ring instead of the workbench, with no arrow to the
output; the crucible (Salis Mundus) popup had no crucible image in the
center; several pages said `Recipe not found` (e.g.
`thaumcraft:nitor_color`, all infusion-enchantment + runic-shielding +
multiblock pages); page text overflowed the bottom and titles sat too high.

Root causes:
- The 1.20.1 fork registered **display-only "fake" recipes** (IE
  enchantments, runic shielding, multiblocks) in a code-side catalog; the
  26.2 port lost that registration, so 16 book references had nothing to
  render.
- 18 legacy book ids never matched a recipe file name (`nitor_color`,
  `brass_stuff`, …) — the port's alias table was incomplete.
- Popup renderers predated the user-facing layout expectations.

Fix:
- `ConfigRecipes` (ported from the 1.20.1 fork, commit 4d3cde8): 8 IE
  enchantment fakes + 3 runic-shielding fakes with the correct base tools,
  aspects, instability — render as infusion recipe popups.
- `FakeRecipes` + `FakeRecipe` (new, commit 73411d8): 5 multiblock display
  recipes (infusion altar x3, thaumatorium, golem press) with a dedicated
  title + item-row popup.
- `RecipeRenderer`: `RECIPE_ALIASES` for 18 legacy ids, `_fake[_N]` suffix
  stripping, `FakeRecipe` dispatch + `resolveOutput` (bookmarks).
- Registered both catalogs in `bootstrap()` (server start + first client
  tick).
- `BookPopupRenderer` full rewrite (2026-09-10) to the 1.12 overlay look:
  gilded-paper panels blitted from the **full 256x256** texture (the old
  code blitted only its top-left quarter, so panels had no right/bottom
  frame); station line-art blitted from `gui_researchbook_overlay.png`
  (crucible pot + flame/arrow, infusion altar diamond, arcane workbench
  grid); every recipe popup shows the output item with an arrow, aspect
  rows, and (arcane) crystal row + vis cost; aspect popup is a 256x256
  paper page with 1.5x aspect icons, names, and primal/secondary type
  text; titles sit below the panel frame (old `scale`-then-`translate`
  pose order also offset them left/up by 10% of the screen position).
- `ResearchPageScreen`: text-block vertical centering uses the exact
  scaled line height (11.25px) instead of the truncated int (11px).
- `ShapedArcaneRecipe` / `ShapelessArcaneRecipe` / `CrucibleRecipeType` /
  `InfusionRecipeType`: override `isSpecial()` -> `true`. 26.2 vanilla
  `finalizeRecipeLoading` drops any non-special recipe whose
  `placementInfo()` is `NOT_PLACEABLE` ("can't be placed due to empty
  ingredients"), which silently removed **every** arcane/infusion/crucible
  recipe from the recipe map — so the Thaumonomicon's leaf-name index (built
  from the recipe manager) had nothing to resolve and pages showed
  `Recipe not found`. Marking them special keeps them in the map (station
  matching, JEI, Thaumonomicon all resolve them) while hiding them from the
  vanilla recipe book.

Verified (2026-09-10): compiles clean; **all 253 book recipe references
resolve** (218 recipe files, 16 fake-catalog, 4 `_fake`-suffix, 14 alias);
dev server boots clean (`Thaumcraft runtime registration complete`, 148
research entries, populated recipe manager, no data-pack errors). Jar
`thaumcraft-6.2.0+26.2.jar` (md5 8622cc983c43656e082f43dd7005be0b) installed
into the Modrinth 26.2 profile — user in-game verification pending.



Symptom: picking up the **Thaumonomicon** hung the dedicated server thread
until the `ServerWatchdog` killed it (`crash-2026-09-07_10.15.10-server.txt`).
The thread dump showed the server thread stuck in
`ResearchManager.progressResearch` → `completeResearch` (called from
`PlayerEvents.onItemPickup`), spinning with no forward progress.

Root cause: `completeResearch` loops on `while (progressResearch(...))`.
`progressResearch` ends with an **unconditional `return true`**, and the loop
only exits once `isResearchComplete(researchKey)` becomes true. The
`!gotthaumonomicon` flag (and its siblings `!gotcrystal`, `!gotdream`,
`!BATHSALTS`, `!INSTABILITY`) are **undefined** research keys — they have no
`ResearchEntry`, so `getStages()` is never set and `isResearchComplete` can
never flip to true. The loop therefore ran forever.

Fix (`ResearchManager.progressResearch`): when there is **no staged entry to
progress** (key undefined, or entry with no stages), mark the flag complete
(`setResearchStage(key, 1)`) so prerequisite/flag checks pass, sync if
requested, and `return false` so the `completeResearch` while-loop
terminates. This preserves the intended "flag unlocked" semantics while
guaranteeing termination.

Verified (2026-09-07): rebuilt jar on local disk (repo is on SMB, where
Gradle's `FileHasher` fails), installed into the Modrinth profile, server
restarted clean (`Thaumcraft setup complete!`, 148 research entries loaded),
and the compiled class was inspected to confirm the new
`entry==null / stages==null / stages.length==0` guard is present. Player
research state confirmed clean (no `!gotthaumonomicon`), so a real
Thaumonomicon pickup exercises the fixed path.


## P0: Golem parts crash on plain client (creative inventory NPE) — RESOLVED (2026-09-07)

Symptom: on a **plain (integrated / Modrinth) client**, opening the creative
inventory (or any screen that renders Thaumcraft golem items) crashed with
`Cannot read field "id" because "mat" is null` — a `NullPointerException` in
golem-material resolution. A dedicated server + client was unaffected, which
hid the bug from the earlier multiplayer verification.

Root cause: golem parts/seals/research/aspects/multiblocks are registered only
on `ServerStartingEvent`. That event fires on a **server** (dedicated or the
integrated server of a `runClient` dev run) but **never on a plain client**.
On 26.2 those registrations construct vanilla `ItemStack`s, which is illegal
before the registry set has bound its data-component initializers
(`Components not bound yet`), so the original port deferred them to
`ServerStartingEvent` — which a standalone Modrinth client never sees, leaving
`GolemMaterial` empty and the creative-inventory render path to NPE.

Fix:
- `Thaumcraft.java`: extract the runtime registration into an idempotent
  `bootstrap()` with a **readiness probe** (`new ItemStack(Items.IRON_INGOT)`
  succeeds only once components are bound; otherwise it defers and retries the
  next tick). `bootstrap()` is now called from **both**:
  - `onServerStarting` (server, as before), and
  - a new `onClientTick(ClientTickEvent.Pre)` — a plain client registers on
    its first tick, before any in-game screen can read the data.
  Guarded by a `volatile boolean bootstrapped` (double-checked, synchronized)
  so it runs exactly once per side and is safe to call from both.
- `GolemProperties.java`: `registerDefaultParts()` now returns early if
  `GolemMaterial.getMaterials()[0] != null` (already registered), making the
  both-sides call path idempotent.

Verified (2026-09-07, macOS Modrinth client, 26.2.0.76): rebuilt jar installed
into the profile → client boots, **creative inventory opens with no crash**
(previously NPE), creative **search finds all 15 golem items** (builders,
placers, seals, bells, pearls) and they **render with real art** (not
checkerboard), and the client log shows
`Registered golem parts` → `Registered golem seals` → … →
`Thaumcraft runtime registration complete`.

## P0: Purple/black items — ClientItem files missing — RESOLVED (2026-09-05)

Symptom: 362 Thaumcraft items rendered **purple/black** (missing-texture
checkerboard) in the client.

Root cause: the 1.20.1→26.2 port dropped the per-item
`assets/thaumcraft/models/item/<id>.json` ClientItem files, so those item
models never resolved.

Fix:
- Generated the 362 missing ClientItem files (`tools/gen_client_items.py`).
- Re-pointed 25 model `textures` refs to the textures that exist
  (`tools/fix_item_textures.py`); generated 4 placeholder textures
  (thaumometer, research_notes, complete_notes, primal_charm).
- Added a `particle` texture ref to the 10 concrete item models that lacked
  one (casters, grapple gun, primordial pearl, verdant charm) so the missing
  particle reference resolves.

Verified (2026-09-05): `runClient` log shows **0 "Missing item model for"**
(was 362) — items render correctly. 5 residual `Missing texture references …:`
warnings print with an **empty** list (benign 26.2 MaterialBaker quirk for
`overrides` models; no rendering impact). Full detail in `TEXTURETODO.md`.


## P0: Broken item textures (placeholder checkerboards) — RESOLVED (2026-09-06)

Symptom: **69** Thaumcraft item/block/entity textures rendered as the
missing-texture **purple/black checkerboard** placeholder (not real art) —
e.g. `item/candle`, `item/shard_*`, `item/bucket_pure`, all casters/placers/
grapple gun, golem pearls/charms, the void jars, arcane-workbench block, and
the taint-spider/slime/dart entity art. (Distinct from the 362 missing
ClientItem files above — this is the textures themselves being placeholders.)

Root cause: the 1.20.1→26.2 port generated placeholder textures (a
deterministic 8x8 purple/black grid) in place of the real Thaumcraft art,
which was not carried over.

Fix:
- Mapped all 69 placeholder textures to their real art in the released
  Thaumcraft jars (TC4 `1.7.10`, TC5 `1.8.9`, TC6 `1.12.2`) and copied the
  real PNGs into `src/main/resources/assets/thaumcraft/textures/`.
- Fixed the missing `bucket_pure` texture (referenced by
  `models/item/bucket_pure.json` but absent) — copied real `bucket_pure` from TC5.
- Added a regression test: `thaumcraft.common.resources.TextureIntegrityTest`
  (2 tests) — (a) every texture ref in every JSON model/blockstate exists as
  a file (catches `bucket_pure`-style dangling refs), (b) no PNG is a
  placeholder checkerboard (catches this regression).

Verified (2026-09-06): `TextureIntegrityTest` green; full suite **86 tests,
0 failures**; rebuilt jar installed into the Modrinth 26.2 profile shows
**0 placeholder textures** (was 69) with `candle`/`shard_fire`/`bucket_pure`
confirmed as real art; `runClient` log shows **0 "Missing item model for"**.
The 5 residual `Missing texture references …: particle` warnings are the
cosmetic block-break particle texture only (items render fine).

## P0: Multiplayer broken — S2C payload channels missing on server — RESOLVED (2026-09-05)

Symptom: any client connecting to a Thaumcraft dedicated server is
rejected during the NeoForge 26.2 channel handshake:
`Channel [thaumcraft:packet<name>] failed to connect: This channel is
missing on the server side, but required on the client!` followed by
`Incompatible client!` disconnect.

Root cause: `PacketHandler.register` wraps all 26 `playToClient`
registrations in `if (FMLEnvironment.getDist() == Dist.CLIENT)`, because
the S2C packet classes carry their client handler inline
(`handleClient`/`handleOnClient` referencing `net.minecraft.client.*`),
which cannot load on a dedicated server. Skipping registration leaves the
server's channel list incomplete, so the handshake always fails — the mod
has no working dedicated-server multiplayer.

Fix (applied): split each S2C payload class into a server-safe common
part (data + encode/decode + `TYPE`/`STREAM_CODEC`) and a client handler
class in `thaumcraft.client.lib.network.*`:
- common class gains `public static Consumer<P> CLIENT_HANDLER = msg -> {};`
  and its `handle(msg, ctx)` delegates to it;
- client class `<Name>Client` holds the extracted handler logic;
- `thaumcraft.client.lib.network.PacketClientWiring.init()` assigns all 26;
- `Thaumcraft.ClientModEvents.onClientSetup` calls `PacketClientWiring.init()`;
- `PacketHandler.register` registers all 26 `playToClient` unconditionally.

Verified (2026-09-05): `build` green → jar installed into the Modrinth
26.2 profile (`sha256` matches build artifact) → client 26.2.0.75 joins
the dev server 26.2.0.75 (localhost:25565): NeoForge handshake passes,
**zero channel errors**, connection ESTABLISHED and stable; player in-world
(server log: "joined the game", "was slain by Phantom"; pause menu shows
"Disconnect"). All 38 `thaumcraft:packet*` channels register on both dists.
`tools/` holds the evdev/Ding input drivers used for the GUI join test.

## P1: Focus package execution + arcane workbench validation — RESOLVED (2026-09-14)

Focus projectiles were spawning but never executing their focus packages —
the `FocusEngine.runFocusPackage` calls were commented out as TODOs.
Arcane workbench had no validation (vis discount, crystals, research).

Fixes:
- **EntityFocusProjectile**: `executeFocusPackage` now calls
  `FocusEngine.runFocusPackage(focusPackage, trajectory, hit)` instead of TODO.
- **EntityFocusMine**: Executes focus package for each entity in range with
  proper trajectory direction from center to entity position.
- **EntityFocusCloud**: Executes focus package for both entity hits and
  block hits (random directions for block interactions).
- **ArcaneWorkbenchMenu**: Added three validation checks before crafting:
  1. **Vis discount**: Applied player's gear discount via
     `CasterManager.getTotalVisDiscount(player)` before checking availability.
  2. **Crystal requirements**: Scans crystal slots (10-15) for required
     aspects via `IEssentiaContainerItem`, checks available count ≥ required.
  3. **Research knowledge**: Checks player has researched the recipe's
     requirement via `ThaumcraftCapabilities.getKnowledge(player)`.
- **ItemGrappleGun**: Now spawns `EntityGrapple` projectile on use instead
  of TODO comment.

All changes build green (`BUILD SUCCESSFUL`). In-game verification pending.

## P1: Golem parts (DARTS arms, SMART heads, advanced seals, modules) — RESOLVED (2026-09-14)

Golems were missing several 1.12 parts. Added:
- **DARTS arms**: `GolemArmDart` — ranged attack spawning `EntityGolemDart`
  projectiles. Registered in `GolemProperties` with `GOLEMRANGED` tag and
  FIGHTER/DEFT/RANGED traits. Build green.
- **SMART_ARMORED head**: Registered in `GolemProperties` with MIND +
  IRON_CHESTPLATE + LEATHER requirements, SMART trait. Build green.
- **SMART_SCOUT head**: Registered in `GolemProperties` with MIND +
  GOLEM_MODULE_AGGRESSION requirements, SCOUT/SMART/FRAGILE traits. Build green.
- **SealEmptyAdvanced**: Advanced seal with 9 filter slots, requires SMART
  trait. Registered in `SealHandler.registerDefaultSeals()`. Build green.
- **ItemGolemModule**: Right-click on golem to add trait (FIGHTER for
  AGGRESSION module, SCOUT for VISION module). Consumes item on success.
  Registered in `ModItems` replacing placeholder items. Build green.

All changes build green (`BUILD SUCCESSFUL`). In-game verification pending.

## Outstanding (runtime verification on a test server)

- [ ] Entity spawning/behaviour, golem seals, bosses
- [ ] Crafting UIs (arcane workbench, crucible, infusion altar, research table)
- [ ] Worldgen (greatwood/silverwood, ores, ruins, taint biome)
- [ ] Visual QA — several renderers use compile-first approximations
      (billboard beams, block-atlas sprite substitutions, banner tint limits)
- [ ] Focus projectile behavior (impact, cloud, mine) — newly wired
- [ ] Arcane workbench crafting with validation — newly wired
- [ ] Golem DARTS arms — newly wired (need in-game test)
- [ ] Golem SMART heads (ARMORED, SCOUT) — newly wired (need in-game test)
- [ ] SealEmptyAdvanced — newly wired (need in-game test)
- [ ] GolemModule item (AGGRESSION/VISION) — newly wired (need in-game test)

## Feature-Gap Audit (2026-09-14) — Full Report: `FEATURE-GAP-AUDIT.md`

### HIGH Priority (core gameplay broken/missing)
- **TileThaumatorium recipe queue** (HIGH): RESOLVED — Recipe queue system implemented with `recipeHash`, `recipeEssentia`, `recipePlayer`, `maxRecipes`, `currentCraft`. NBT serialization added. Packet handler wired (clear queue supported; full recipe lookup needs client-side aspect list).
- **Seal GUI system (filtered/guard/use)** (HIGH): RESOLVED — All seal GUIs accessible via ItemGolemBell → SealMenuProvider → SealMenu. Filter slots, guard toggles, and use settings all functional through the existing SealMenu.
- **ItemCausalityCollapser projectile** (HIGH): RESOLVED — Projectile spawning implemented (EntityCausalityCollapser).
- **ItemBottleTaint projectile** (HIGH): RESOLVED — Projectile spawning implemented (EntityBottleTaint).

### MEDIUM Priority
- **TileEssentiaReservoir interaction** (MEDIUM): RESOLVED — Phial fill/drain wired. Flux pollution on break still TODO.
- **SealHarvest replanting** (MEDIUM): Still open — requires seed system (`ThaumcraftApi.getSeed`). Complex API differences between 1.12 and 26.2.
- **SealStock tag-based matching** (MEDIUM): RESOLVED — Tag-based matching implemented using `ItemStack.is(TagKey)` API.
- **AuraHandler biome modifiers** (MEDIUM): RESOLVED — `BiomeHandler.registerBiomeInfo()` called during mod init. Biome aura modifiers now active.
- **ResearchManager events** (MEDIUM): RESOLVED — ResearchEvent system implemented. Knowledge and Research events fired via NeoForge.EVENT_BUS.
- **PlayerKnowledge auto-unlock** (MEDIUM): RESOLVED — Auto-unlock research implemented. Research entries with AUTOUNLOCK meta are automatically unlocked when knowledge is loaded.
- **ConfigResearch stat-based discoveries** (MEDIUM): Still open — requires modern stats API (walking/running/jumping/swimming thresholds). Complex API differences between 1.12 and 26.2.
- **TileSmelter auxiliary vents** (MEDIUM): Still open — requires new SmelterVent block (not in 26.2 port).

### LOW Priority (cosmetic/minor)
- **FX/particle effects** (LOW): RESOLVED — Particle effects implemented for all 8 files: TileHole, TileTube, TileCondenser, TileFocalManipulator, TileInfernalFurnace, TileWaterJug, BlockVisGenerator, BlockEffect.
- **ItemCreativePlacer** (LOW): RESOLVED — 1.12 source confirms the item is a block **eraser** (not a structure placer); 26.2 port matches exactly (solid-block/face-offset/replaceable checks + `setBlock(AIR)`).
- **SealEntity/SealHandler network sync** (LOW): RESOLVED — Sync implemented via PacketSealToClient.
- **TileCrucible nitor check** (LOW): RESOLVED — Nitor block tag created (`c:nitor`) and check added to `isFireSource()`.
- **ScanSky scribing tool check** (LOW): RESOLVED — Scribing tool (book + paper) check and consumption implemented.

### RESOLVED in this session (2026-09-14)
- ✅ Seal config GUI (basic seals) — wired via SealMenuProvider
- ✅ Taint spread cycle — TaintHelper↔EntityTaintSeed wired
- ✅ Vis-discount mundane/fancy gear — 7 items ported from 1.12
- ✅ Golem DARTS arms — GolemArmDart implements IArmFunction
- ✅ Golem SMART/SCOUT heads — traits wired (XP system, range bonus)
- ✅ NeoForge transfer API — already using IFluidHandler/IItemHandler

### Resolved / 1.12-faithful (re-verified 2026-09-28)
- **Golem components use vanilla items** (MEDIUM): NOT a gap — 1.12 golems are assembled from vanilla items too (e.g. BREAKERS arms = 2× diamond + 2× piston, `GolemProperties.java:214`). 26.2 mapping (WOOD→oak_planks, BRASS→gold_ingot, THAUMIUM→diamond, VOID→obsidian, mechanism→clock) is 1.12-consistent.
- **ItemFocusPouch Curios integration** (LOW): RESOLVED — `ItemFocusPouch implements ICuriosItemHandler` (line 56) backed by `PouchCurios` (18-slot `ICurioStacksHandler`/`IDynamicStackHandler` pair). Matches 1.12's `ItemFocusPouch implements IBauble` (Baubles being Curios' predecessor).

## P1: Functional gaps — RESOLVED (2026-09-04)

All three fixed and build-verified (`BUILD SUCCESSFUL`); in-game
verification still outstanding.

Diagnosis was verified against the NeoForge 26.2.0.59 universal jar and
the official NeoForge 26.2.x source before fixing.

### 1. Worldgen dead — biome modifiers never load

- 13 files under `src/main/resources/data/thaumcraft/forge/biome_modifier/`
  (1.20.1 Forge layout), all with `"type": "forge:add_features"`.
- NeoForge 26.2 registers modifier codecs under namespace `neoforge`
  (`NeoForgeMod.MOD_ID = "neoforge"`); `AddFeaturesBiomeModifier` javadoc
  requires `"type": "neoforge:add_features"`; datapack directory is
  `data/<ns>/neoforge/biome_modifier/`.
- Effect: no greatwood/silverwood trees, no TC ores, no crystal clusters,
  no ruined towers / eldritch obelisks / barrows / ancient stone circles,
  no cinderpearl / vishroom — none of the worldgen features ever apply.
- Fix:

  ```bash
  git mv src/main/resources/data/thaumcraft/forge/biome_modifier \
         src/main/resources/data/thaumcraft/neoforge/biome_modifier
  sed -i 's/"type": "forge:add_features"/"type": "neoforge:add_features"/' \
         src/main/resources/data/thaumcraft/neoforge/biome_modifier/*.json
  ```

- Verify: fresh world — forest biomes grow greatwood/silverwood trees;
  TC ore veins appear in stone/deepslate.

### 2. `#forge:` tags dead — recipes uncraftable, cross-mod tag membership invisible

- NeoForge 26.2 ships 593 `data/c/tags/` files and **zero** `data/forge/tags/`;
  the `forge:` namespace is not loaded at all.
- Port has 40 tag definitions under `data/forge/tags/{items,blocks}/` (dead)
  and 26 distinct `#forge:` ingredient refs in recipes (~90 usages) that match
  nothing → those recipes are uncraftable (rods, ingots, gems, dyes, …).
- Fix:
  1. `git mv src/main/resources/data/forge/tags/items src/main/resources/data/c/tags/items`
  2. `git mv src/main/resources/data/forge/tags/blocks src/main/resources/data/c/tags/block`
     (26.2 uses singular `block` for block tags)
  3. Rewrite recipe refs `#forge:X` → `#c:X`. 18 of the 26 exist in the
     convention set: `rods/wooden`, `ingots/{gold,iron}`,
     `gems/{quartz,emerald,diamond}`, `glass_panes`, `dyes/{black,red}`,
     `nuggets/gold`, `dusts/{redstone,glowstone}`, `ores/{iron,gold,copper}`.
  4. Renames: `#forge:leather` (7 refs) → `#c:leathers`; `#forge:glass`
     (2 refs) → `#c:glass_blocks/colorless` (both convention tags verified
     to contain `minecraft:` equivalents in the 26.2 jar). Then drop the
     now-redundant port definitions `data/c/tags/items/{leather,glass}.json`.
  5. Custom tags (not in the convention set) resolve via the port's own
     moved definitions: `string`, `stone`, `trapdoors/wooden`, `slimeballs`,
     `ingots/brass`, `gems/amber`, `workbenches`, `cobblestone`.
- Verify: JEI — arcane-workbench recipes with rod/ingot/gem ingredients are
  craftable; TC ores appear under `c:ores/{iron,gold,copper}`; other mods'
  recipes matching `c:ingots/thaumium` etc. now work.

### 3. Six block classes never create their block entities (machines dead)

Tile classes exist and are implemented; the blocks return `null` with
`// TODO: Return Tile… when implemented`:

| Block (`common/blocks/…`) | Tile(s) to return (`common/tiles/…`) |
|---|---|
| `crafting/BlockInfusionMatrix` | `crafting/TileInfusionMatrix` |
| `essentia/BlockTube` | `essentia/TileTube` + `TileTubeFilter` / `TileTubeBuffer` / `TileTubeValve` / `TileTubeOneway` / `TileTubeRestrict` (pick by tube type) |
| `essentia/BlockAlembic` | `essentia/TileAlembic` |
| `devices/BlockLamp` | `devices/TileLampArcane` / `TileLampFertility` / `TileLampGrowth` (pick by lamp type) |
| `devices/BlockMirror` | `devices/TileMirror` / `TileMirrorEssentia` |
| `devices/BlockStabilizer` | `devices/TileStabilizer` |

Effect: infusion altar, stabilizers, the entire essentia transport network
(tubes/valves/filters/buffers), alembic, lamps, mirrors — none function.
Copy the 26.2 constructor pattern from a working block, e.g. `BlockCondenser`:
`new TileCondenser(ModBlockEntities.CONDENSER.get(), pos, state)` — 26.2
`BlockEntity` constructors take the `BlockEntityType` first.

## Suggested order for the next session

1. In-game QA of the P1 fixes — fresh world: greatwood/silverwood trees +
   TC ores generate; JEI shows the rod/ingot/gem recipes craftable;
   infusion altar + stabilizers craft; a tube network moves essentia;
   alembic/lamps/mirrors/stabilizer tiles function
2. Re-run the Outstanding list — entity spawning/behaviour, crafting UIs,
   taint biome
3. Full feature-gap audit vs the 1.20.1 reference — a subagent pass was
   aborted before writing its report (partial transcript:
   `history://GapAudit`; reference tarball may still be at `/tmp/refrepo`)

## Verified (2026-09-04)

- Fresh-world server boots to "Done"; aura scheduler ticks continuously
  (moon phase now read via the 26.2 positional environment-attribute API)
- Initial chunk save completes for all 3 dimensions (no OOM)
- Research data loads: **148 entries / 8 categories** (alchemy 22, eldritch 5,
  basics 20, auromancy 23, scans 12, artifice 20, infusion 17, golemancy 29)
- Tag-based aspect registration runs without errors — 26.2 changed
  `Identifier.withDefaultNamespace` to path-only; all id call sites now use
  `Identifier.parse`
- **Spawn categories fixed**: `minecraft:bat` (CREATURE→PASSIVE) and
  `thaumcraft:pech` (MONSTER→CREATURE) now register in the correct
  `SpawnPlacements` buckets — zero "Detected … wrong category" warnings
- **Research icon mappings added**: `minecraft:noteblock→note_block`,
  `minecraft:web→cobweb`, `thaumcraft:brain→brain_normal`,
  `thaumcraft:biothaumic_mind→brain_curious`, `thaumcraft:leather→
  minecraft:leather` (all verified against the 1.20.1 reference data)
- **Aspect + smelting-bonus data wired**: `CommonInternals.initAspects()`
  (101 vanilla-item aspects) and `initSmeltingBonuses()` now run on
  `ServerStartingEvent`; `TileInfernalFurnace.getSmeltingBonus` delegates to
  `CommonInternals` (bellows bonus drops functional)

## Known issues & environment

- **Binary-patch pipeline is the local default** (since the 2026-09-29 CI
  fix): `neogradle.subsystems.decompiler.enabled=false` in `gradle.properties`
  forces the same PMJ/`patches.lzma` pipeline CI uses, so no `CI=true` prefix
  is needed anymore. `build.gradle`'s `compileJava` swap detects the mode
  automatically (patched `ng_dummy` → skip; stub `ng_dummy` → swap in the
  source-pipeline recompile jar).
- **Dev-server console**: `stop` typed into the `runServer` console is not
  forwarded to the server process — stop with SIGINT/Ctrl+C.

## Known build warnings

- **Resolved (2026-09-04)**: JEI compat layer migrated off the JEI 30.x
  removal-marked API — `RecipeType` → `mezz.jei.api.recipe.types.IRecipeType`,
  `IIngredientAcceptor.addIngredients/addItemStack` → `.add(...)`,
  `addRecipeCatalyst` → `addCraftingStation`. Zero `compat/jei` warnings.
- **Deferred project — NeoForge handler API migration (125 warnings)**:
  the entire `net.neoforged.neoforge.items` package
  (`IItemHandler`, `IItemHandlerModifiable`, `ItemStackHandler`,
  `ItemHandlerHelper`) and `IFluidHandler`/`FluidTank` are
  `@Deprecated(since="1.21.9", forRemoval=true)`. The replacement is the
  transaction-based `net.neoforged.neoforge.transfer` API
  (`ResourceHandler<T extends Resource>`, `ItemAccess`,
  `transfer.item`/`transfer.fluid`/`transfer.energy`).
  - Still fully functional at runtime in 26.2 (vanilla itself still uses the
    `Container` model), so this is a planned rewrite, not an urgency.
  - Affected files: `TilePatternCrafter`, `SealProvide`, `SealEmpty`,
    `SealFill`, `SealStock`, `BlockCrucible`, `ThaumcraftInvHelper`,
    `InventoryUtils`, `LogisticsMenu`, `TileWaterJug`, `TileCrucible`,
    `BlockWaterJug`, others.
  - Plan: dedicated pass migrating to `StacksResourceHandler`/`ItemAccess`
    + in-game testing of every machine (item + fluid transfer, serialization,
    capability exposure).
