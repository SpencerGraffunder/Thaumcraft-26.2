# Thaumcraft26 — Minecraft 26.3 (NeoForge) Port

> Project renamed **Thaumcraft-26.2 → Thaumcraft26** on 2026-10-08 (repo:
> github.com/SpencerGraffunder/Thaumcraft26; working dir:
> `~/Documents/Thaumcraft26`). All 26.2 references below are historical.

Port of **Thaumcraft 6** from the 1.20.1 Forge source fork
([ShobieShy/Thaumcraft-6-Source-Code-1.20.1](https://github.com/ShobieShy/Thaumcraft-6-Source-Code-1.20.1))
to **Minecraft 26.3** on **NeoForge 26.3.0.33-beta** (Java 25).

> The port was developed and verified on 26.2 ("Chaos Cubed", NeoForge
> 26.2.0.76) and migrated to 26.3 on 2026-09-28. The 26.2 notes below are
> historical — the build now targets 26.3 end-to-end (gradle.properties,
> mods.toml template, decompiled-source pipeline, CI).

## Status: 26.3 migration — complete (audit gate GREEN, build green, verified in-game, installed)

- **Verification layer (2026-10-09): `tools/run_all_audits.sh` — GATE GREEN.**
  One command runs all 8 static 1.12-vs-port oracles (aspects 242/242,
  recipe pairs 152/152, research refs, recipe<->research cross-refs,
  client item defs, models/textures, Java→resource refs, inventory
  read-while-write loops) **plus a headless in-game smoke** — a real
  dedicated server boots with `TC_SMOKE=1` and runs a **27-check**
  assertion battery at `ServerStartedEvent`: static-state checks
  (registrations, recipe load, research gates, enchantments, loot
  modifiers, aspects, lang coverage) plus 14 deterministic behavioral
  loops (research progression, crucible craft, infusion assembly,
  REFINING mining loot, golem tick, phial fill, biome aura modifiers,
  thaumatorium queue, reservoir phial I/O, seal matching + config GUIs,
  causality-collapser→rift collapse, taint bottle, smelter vents, research
  auto-unlock); exits non-zero on any red row. The battery caught and
  fixed: dedicated-server client-class leaks (focus FX moved to
  `@OnlyIn(CLIENT)` `FocusFX`), 5 recipe JSONs with invalid 26.3
  ingredient formats (fatal on data load), the DeferredRegister
  enchantment collision (now 9 data-driven `data/thaumcraft/enchantment/*`
  files), the 1.12 `nitorcolor` research-page display recipe, 3 stale
  texture refs, 3 live-inventory read-while-write loops, a missing
  `cluster_quartz` client item definition, an inverted reservoir
  phial-fill condition (`addToContainer` returns the unadded remainder),
  and a smelter BE type registered for only the base block (placing a
  vent/aux/thaumium/void smelter threw "Invalid block entity").
- `./gradlew build` — **SUCCESS** → `build/libs/thaumcraft-6.2.0+26.3.jar`
  (86/86 tests green, 0 TODOs left in source).
- Full neoForm decompile→patch→recompile pipeline runs inside the build
  (see `build.gradle`); `scripts/fix_neoform_artifacts.py` repairs
  decompiler artifacts (26.3-specific fixes for `CustomPayload` codec call
  sites and `LevelEventHandler` case-2003 locals).
- **`runServer` smoke test: boots to `Done`** on a fresh 26.3 world — zero
  registry/worldgen errors, clean world save, research (148 entries) + runtime
  registration complete. This surfaced & fixed 3 runtime-only data issues the
  compile never caught:
  - **Curios dependency range:** `mods.toml` had `[17.0.0,)` but the 26.3 build
    ships as `17.0.0-beta.2+26.3`. NeoForge parses ranges with Maven's
    `VersionRange`, which sorts a pre-release *below* its release, so the beta
    was rejected at mod-load. Lowered to `[17.0.0-beta,)` (accepts the beta, a
    future stable 17.0.0, and 17.x; still rejects 16.x).
  - **Ore feature `state` format:** `BlockState.CODEC` in 26.3 accepts a plain
    string or a map with an `id` key — the old `{"Name": "..."}` block-state
    object is gone. All 3 ore features (`ore_amber`/`ore_cinnabar`/`ore_quartz`)
    now use plain-string `state` (e.g. `"thaumcraft:amber_ore"`).
  - **`crystals` item tag directory:** was in the legacy `data/thaumcraft/tags/items/`
    (plural); 26.3 (like every other tag in the mod) uses `data/thaumcraft/tags/item/`
    (singular). Moved it — the Salis Mundus recipe's `#thaumcraft:crystals`
    ingredient now resolves.
- Jar installed into the Modrinth **`NeoForge 26.3`** profile
  (NeoForge 26.3.0.33-beta). **The profile also needs
  `curios-neoforge-17.0.0-beta.2+26.3.jar`** (hard dependency, was not
  installed in the fresh profile). JEI is optional (31.7.0.47).
- **26.3 transfer-API migration:** NeoForge removed `IItemHandler` /
  `IFluidHandler`/`FluidTank` entirely; the new transaction-based
  `net.neoforged.neoforge.transfer.ResourceHandler<T>` framework replaced
  them. Thaumcraft now uses `ResourceHandler<ItemResource>` / `ResourceHandler<FluidResource>`
  everywhere, with two thin adapters restoring the old call shapes:
  `thaumcraft.api.ItemHandlers` (insert/extract/set with simulate flags) and
  `thaumcraft.api.FluidTanks` (fill/drain/getAmount). Curios 17-beta's
  `IDynamicStackHandler` was also re-based on `ResourceHandler`, so the
  focus pouch (`PouchCurios`) now extends `ItemStacksResourceHandler`.
- **Other 26.3 API breaks fixed in this pass:** `Feature` is a plain
  interface (no `ConfiguredFeature`/`PlacedFeature` classes — configured
  features moved to `data/<ns>/worldgen/feature/*.json`, 16 custom feature
  types registered in `Registries.FEATURE_TYPE`); tool `Item` subclasses
  (`AxeItem` etc.) replaced by `Item.Properties.axe/shovel/hoe/pickaxe(...)`
  builder methods; `PoseStack.mulPose(Quaternionf)` → `rotate`/`rotateDegrees`;
  `FuelValues` removed → fuel burn time read from
  `DataComponents.COOKING_FUEL` (`TileSmelter`); arm-swing sync fields
  removed → `swing(hand, SwingAnimation.DEFAULT, true)`;
  `Entity.drop` moved to `LivingEntity` with a `Prediction` param;
  `submitModel`/`submitModelPart` lost the `breakProgress` argument;
  `PushReaction.DESTROY`/`BLOCK` → `POPPED`/`IMMOVEABLE`;
  `blocksMotion()` → `isCollisionShapeFullBlock(level, pos)`;
  `hurtMarked` → `needsSync` (now public); `KeyMapping` moved to
  `net.minecraft.client` with `InputConstants.isKeyDown(int)` single-arg.
- `./gradlew runServer` — gets through mod construction and block registration
  (id-injected via `BlockRegistration` ThreadLocal helper); **server reaches
  `Done`** — world loads, aura threads run per dimension, golem parts/seals/
  research/aspect/multiblock init run on `ServerStartingEvent` (constructing
  vanilla `ItemStack`s, illegal in commonSetup on 26.2)
- Recipe data migrated to the 26.2 format: `key` ingredients are plain strings
  and `result` uses `"id"` (not `"item"`); forge + thaumcraft item tags added
  for cross-mod interop.
- **World-load abort ("Invalid data pack") & first-craft NPEs fixed (2026-09-09, macOS):**
  26.2 decodes recipes in the parallel prepare phase *before* tags bind, and it
  validates tag contents strictly — a `c:ingots/brass` tag pointing at a
  non-existent item ID aborted every world load. Recipe codecs now follow the
  26.2 rules: `create`/decode keep pure null/`Optional` logic (no
  `isEmpty()`/`getStacks()`; 26.2 also removed `Ingredient.EMPTY`), shaped
  arcane recipes use vanilla's `List<Optional<Ingredient>>` slot pattern with
  `Ingredient.testOptionalIngredient`, infusion accepts both `center` and
  `input` keys, and tag item IDs are validated against the registry
  (`brass_ingot`, not `ingot_brass`). Verified: `CI=true ./gradlew runServer`
  boots to `Done` with every recipe type decoded. Rules documented in
  `skills/minecraft-gui/SKILL.md` (Troubleshooting).
- Multiplayer fixed & verified: S2C payload handlers split into
  server-safe common classes + client handler classes
  (`thaumcraft.client.lib.network.*`), so all 38 `thaumcraft:packet*`
  channels register on both dists. A 26.2.0.75 client joins a 26.2.0.75
  dev server cleanly — NeoForge handshake passes with zero channel errors,
  player in-world (see [`TODO.md`](./TODO.md) P0 for details).
- **Multiplayer verified on macOS (Spencers-MacBook-Air, 2026-09-06):** dedicated
  server booted from the Modrinth profile (26.2.0.76, `Done` in ~8s) + client join
  via Multiplayer → Direct Connect — clean handshake, in-world play, Thaumcraft
  item give / block place / wand interactions, **zero exceptions in both logs**
  (full workflow in `skills/minecraft-gui/SKILL.md`).
- **Purple/black items fixed & verified:** the 1.20.1→26.2 port had dropped the
  per-item `models/item/<id>.json` ClientItem files, so 362 items rendered as the
  missing-texture checkerboard. All 362 ClientItem files are now generated, 25
  model texture refs re-pointed to existing textures, and 4 placeholder textures
  added — `runClient` now logs **0 "Missing item model for"** (items render
  correctly; see [`TEXTURETODO.md`](./TEXTURETODO.md)).
- **Broken item textures fixed & verified:** 69 textures that shipped as
  placeholder checkerboards (candles, elemental shards, casters, golem
  pearls/charms, void jars, arcane workbench, taint mob art, …) are now
  replaced with the real Thaumcraft art from the released TC4/TC5/TC6 jars,
  and the dangling `bucket_pure` ref is fixed. A new
  `TextureIntegrityTest` guards against regressions (dangling model refs +
  placeholder checkerboards). Rebuilt jar shows **0 placeholder textures**;
  full test suite **86 green** (see [`TODO.md`](./TODO.md) P0).
- **Thaumonomicon 1.12 parity (rounds 1–5, 2026-09-11/12):** recipe popups
  rewritten to the 1.12 overlay look, bookmarks/ribbon positions, 1.0x text
  scale at 140px wrap width, 1.12 completion semantics (pre/post stage text),
  requirement rows at the book bottom, 3D infusion-matrix + altar popups, 16
  colored nitor items rendered 1.12-style (color overlay over the core), and
  the "new research" gold-star markers (fixed: knowledge sync after research
  completion was dead, so flags/toasts never reached the client; added the
  missing category-sidebar stars).
  All 253 book recipe references resolve; see [`TODO.md`](./TODO.md).
- **Thaumometer HUD + research sync (2026-10-06, 1.12 parity):** the in-hand
  thaumometer HUD was rebuilt against the 1.12 `HudHandler` — aura gauge from
  `hud.png` (full 16x42 caster frame), scan highlight driven every 5 client
  ticks (was dead code), numeric read-outs gated on sneaking, gauges hidden
  while a screen is open, plus the sanity-checker warp gauge and the
  knowledge-gain book animation (server now sends `PacketKnowledgeGain` per
  point; the 1.12-absent "Knowledge gained!" chat spam is gone). Research
  knowledge is pushed to the client immediately on change (fixes client-side
  prediction opening the crafting GUI before the Salis Mundus conversion
  lands). Audit K2: 6 invented crucible recipes dropped and 3 bogus research
  gates removed. The temporary `[TC-DIAG]` trace used to diagnose the Salis
  chain was removed once the chain was fixed.
- **3D in-hand Thaumometer model (2026-10-06, commit 92b0ecd):** 26.3 dropped
  OBJ item models, so the flat 16x16 icon was the best the port could do. The
  real 1.12 model is `scanner.obj` — a hexagonal washer body (`scanner.png`
  UV-unrolled) with a translucent `scanscreen` hexagon pane. It is now
  reconstructed as a cuboid slab: a front face of the actual 1.12 model baked
  to `textures/item/thaumometer_face.png` (gold frame + shaded blue window,
  transparent corners for the hex silhouette) on the up/down faces, a dark-gold
  rim on the sides, and a first-person transform (x=90, scale 1.4) that faces
  the window at the player like 1.12. The original `scanner.obj`/`mtl`/`png`
  remain in the jar as reference assets. `runClient` bakes it with 0 model/
  texture errors.
- **1.12 recipe-parity fixes (2026-10-06, commits 6121e55…eaffaee):** arcane
  stone used the non-existent `#c:stone` tag (real tag: `c:stones`) and a
  plain-crystal ingredient that could never match — in 1.12 aspect crystals
  were NBT on ONE item, so the plain ingredient matched every aspect; the
  port split them into separate `vis_crystal_*` items. Arcane stone now
  accepts any crystal; vis amulet/cloud ring use their specific 1.12
  crystals; the missing non-primal crystals (life/plant/man/desire) were
  added 1:1 (items + retinted textures + crucible recipes, incl. the
  flux crystal which had no recipe at all); focus_1's catalyst was the
  crystal *block* instead of the crystal *item*; verdant charms rebuilt to
  the 1.12 ingredient sets (base: rareearth nugget + LIFE/PLANT crystals +
  milk; life: LIFE+MAN; sustain: DESIRE+AIR). Headless `runClient` loads
  all recipes with 0 errors.
- **1.12 recipe-parity round 2 (2026-10-07):** five more 1:1 fixes against the
  1.12.2 BETA26 source/jar — (1) nitor crucible aspect cost potentia 5→10
  (1.12: ENERGY 10 + FIRE 10 + LIGHT 10); (2) goggles: the port had an
  *invented* infusion recipe (thaumometer/diamond/salis) alongside the
  correct 1.12 arcane-workbench recipe (LGL/L L/TGT, 50 vis, UNLOCKARTIFICE)
  — the duplicate was deleted; (3) enchanted fabric: same story — invented
  crucible recipe deleted, the 1.12 arcane-workbench recipe (" S/SCS/ S",
  5 vis, UNLOCKINFUSION) kept; (4) bath salts aspect sensus→cognitio
  (1.12: MIND 40 + AIR 40 + ORDER 40 + LIFE 40); (5) the five advanced
  golem-seal crucible recipes now carry 1.12's compound gates
  (`SEALX&&MINDBIOTHAUMIC`) — the recipe-gate check gained 1.12's
  `&&`/`||` semantics (previously a compound gate could never match).
  (Round 2 also kept nitor's gate at `BASEALCHEMY` believing 1.12's
  `UNLOCKALCHEMY@3` gate was circular — **overturned in round 3** after
  bytecode verification, see below.)
- **1.12 recipe-parity round 3 (2026-10-07):** nitor system made 1:1 — the
  one crucible recipe now produces the **yellow** nitor with the real 1.12
  gate `UNLOCKALCHEMY@3` (the earlier "circular gate" concern was wrong:
  `progressResearch` checks only PARENTS server-side and the client Complete
  button gates on the *current* stage's requisites, so stage 3's "craft
  nitor" requirement gates nothing); 16 shapeless dye recipes added
  (any nitor + dye → colored nitor — 15 colors were previously
  unobtainable); levitator accepts any nitor (`#thaumcraft:nitor`, 1.12
  oredict). Phantom ids fixed: voidseer charm's `brain_normal` (unregistered,
  recipe could never match) and the golemancy JARBRAIN display item →
  `zombie_brain` (1.12's `ItemsTC.brain`); dead `brain.json` model removed.
  Recipe data aligned to 1.12: gate stage suffixes (brass/leather/tallow/
  thaumium ingots), inlay aspect ordo→aqua, sanitizing soap aspects
  (MIND 75 / ALIENIS 50 / ORDER 75 / LIFE 50), void metal (void_seed
  catalyst, METALLUM 10 + VITIUM 5, BASEELDRITCH gate), the five elemental
  tools' missing aspects, and primal crusher + voidseer charm rewritten to
  the 1.12 ingredient/aspect/instability sets (voidseer input is
  `golden_carrot` — documented stand-in for the baubles item the port does
  not model). The missing 1.12 `slab_eldritch` block was restored (block +
  1.12 blockstate textures + 3-tile→6-slab recipe + creative tab) and the
  Eldritch Pedestal recipe now uses slab + tile like 1.12; the other slabs'
  face textures were corrected to the 1.12 blockstates. Full static audit:
  all 345 recipes + research refs resolve — ALL CLEAN.
- **1.12 recipe-parity round 4 (2026-10-07/08, final audit pass):** new
  static audit tool `tools/recipe_parity.py` (checks every port recipe 1:1
  against the 1.12.2 BETA26 reference; `reference/recipes_112.json` expanded
  with improved extraction, +393 lines) — final result: **152 matched pairs,
  2 documented deviations**. Fixes this round: gold **nuggets** (1.12) in
  filter, focus pouch and the three fortress armor pieces (port had ingots);
  golem aggression module uses the alchemical **block** (1.12
  `BlocksTC.metalAlchemical`); inlay = shapeless redstone + gold ingot (1.12
  RedstoneInlay); mirrored glass recipe restored (quicksilver + glass panes,
  BASEARTIFICE@1); seal blank uses the clay **ball**; tube uses full
  quicksilver; advanced crossbow uses the base `mind` brain; crucible metal
  purification for tin/silver/lead restored (1.12 recipes, with new
  `#thaumcraft:ores/{tin,silver,lead}` catalyst tags — 26.3 has no common
  tags for these ores); invented `sanitizing_soap`/`void_ingot` duplicates
  deleted (1.12 names `sanity_soap`/`void_metal_ingot` kept); elemental tools,
  fertility/growth lamps, masks and traveller boots aligned to 1.12
  ingredients/aspects; iron/gold ore **and** cluster smelting bonus is the
  **ingot** (1.12 `field_191525_da`/`field_151074_bl` — the port had nuggets);
  cluster smelts yield 2 nuggets; the arcane activator rail gets its 1.12
  aspect tag (MECHANISM 5). Build green (86/86 tests), headless `runClient`
  clean, jar `8f06e7522ea6676c9e9892d93813fef14fefe33bd8f69ce248ab4fc5f28d19d4`
  installed in the Modrinth `NeoForge 26.3` profile.
- **1.12 aspect-value parity audited & completed (2026-10-08, `tools/audit_aspects.py`):**
  every `ConfigAspects.java` registration in 1.12.2 BETA26 is now compared
  against the port (blocks, items, per-meta, oreDicts via the `ORE112` map,
  potions, curios) — **242 pairs compared, 0 divergent values**. Added to the
  port: taint fibre/crust/rock/crystal + flux crystal block values, 6 eldritch
  blocks, 3 pedestal variants, ancient stone doorways/glyphs, 7 damage-value
  curio variants (26.3: `DataComponents.DAMAGE`), cinnabar/iron/gold/copper
  clusters (new `cluster_tin`/`cluster_silver`/`cluster_lead` items + crucible
  recipes), quicksilver/amber, 24 vanilla item tags (dirt, cobblestone,
  granite/diorite/andesite, obsidian, sand/gravel, netherrack, saplings, golden
  dandelion, wither rose, spawners, potions, …), 22 common tags
  (`c:stones`, `c:sands`, tin/silver/lead/brass/bronze/uranium ores-ingots-
  dusts-nuggets, …) and 6 new `thaumcraft:ores|ingots|dusts|nuggets` tag files.
  Documented 1.12→26.3 deviations: dyes are one `DataComponents`-based item in
  26.3 (no per-color registration possible); `treeLeaves`/`logWood` oreDicts
  have no 26.3 equivalent. Build green (86/86 tests), headless `runClient`
  clean, jar `2a1fe09f8065f9668a9c4377fb61937795ebc4b004bd28242a424b78755ed6f8`
  installed in the Modrinth `NeoForge 26.3` profile.
- **Golem-parts client crash fixed & verified (macOS, 2026-09-07):** golem
  parts/seals/research were registered only on `ServerStartingEvent`, which a
  plain (integrated / Modrinth) client never sees — so opening the creative
  inventory NPE'd (`mat is null`). Runtime registration is now an idempotent
  `bootstrap()` with a component-binding readiness probe, driven from both
  `ServerStartingEvent` and `ClientTickEvent.Pre`. Verified: creative
  inventory opens clean, all 15 golem items render with real art, log shows
  `Registered golem parts` … `Thaumcraft runtime registration complete`.

Detailed task tracking lives in [`todo.md`](./todo.md) — 
checklists and remaining runtime-testing items are maintained there, not here.
This README covers build/run/deploy status only.

**Known-good reference:** the unmodified 1.20.1 Forge build produces
`thaumcraft-6.2.0.jar` and loads into a world — that's the behavior baseline.

## Building

Requires JDK 25.

- Ubuntu 26.04: `sudo apt-get install openjdk-25-jdk-headless` puts JDK 25 on
  the `PATH` — no `JAVA_HOME` needed.
- Otherwise: `export JAVA_HOME=/path/to/jdk-25` before running Gradle.
- macOS: `brew install openjdk@25` (keg-only — set
  `JAVA_HOME=/opt/homebrew/opt/openjdk@25`). If the repo checkout is on an SMB share,
  Gradle fails (`FileHasher: Operation not supported`) — copy the repo to local disk
  and build there. Full macOS build/launch/screenshot/click workflow is in
  [`skills/minecraft-gui/SKILL.md`](./skills/minecraft-gui/SKILL.md).

```bash
./gradlew build
```

Output jar lands in `build/libs/`. GitHub Actions CI (`.github/workflows/build.yml`)
builds on every push with JDK 25 and uploads the jar as an artifact.

## Running

```bash
CI=true ./gradlew runClient   # client
CI=true ./gradlew runServer   # dedicated server
CI=true ./gradlew runGameTestServer
```

Dev note: `run/server.properties` sets `max-tick-time=300000` (first-boot world
saves exceed the 60s default watchdog).

Dev note: the build runs the local neoForm decompile→patch→recompile pipeline
on demand; `neoFormPatchUserDev` may reject non-critical hunks, which
`build.gradle` tolerates (the recompiled jar is guarded so a valid existing
jar is never clobbered). The `runServer` console does not forward a typed
`stop` to the server process — stop it with Ctrl+C/SIGINT.

## Deployment

`deploy.sh` copies a built jar into a configurable mods folder for test-server
deployment (target: a Crafty Controller MC server on the same Unraid host).

## License

See upstream — Thaumcraft is the property of Azanor / original authors.
