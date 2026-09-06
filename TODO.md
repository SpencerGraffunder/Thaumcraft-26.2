# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Migration Status

> Tracks the active **26.2 port**. Milestones achieved so far —
> compiles, builds, server boots to "Done" with clean chunk saves and full
> research-data load — and historical one-off tickets are recorded in git
> history. Only outstanding work appears below.

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

## Outstanding (runtime verification on a test server)

- [ ] Entity spawning/behaviour, golem seals, bosses
- [ ] Crafting UIs (arcane workbench, crucible, infusion altar, research table)
- [ ] Worldgen (greatwood/silverwood, ores, ruins, taint biome)
- [ ] Visual QA — several renderers use compile-first approximations
      (billboard beams, block-atlas sprite substitutions, banner tint limits)

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
3. Port `wand_workbench` (Known issues)
4. Full feature-gap audit vs the 1.20.1 reference — a subagent pass was
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

- **`CI=true` required for local runs**: without it, NeoGradle takes the
  local decompile→patch→recompile pipeline, which fails at `neoFormPatch`
  (decompiler emits `var1` parameter names; the patch expects named
  params). Some shells already export `CI`; otherwise prefix runs with
  `CI=true ./gradlew …`.
- **Dev-server console**: `stop` typed into the `runServer` console is not
  forwarded to the server process — stop with SIGINT/Ctrl+C.
- **Missing content**: `thaumcraft:wand_workbench` — the wand workbench
  block/item was never ported; research icons for it fall back to the
  texture id until the block is ported.

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
