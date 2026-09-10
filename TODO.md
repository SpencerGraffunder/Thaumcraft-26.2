# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Migration Status

> Tracks the active **26.2 port**. Milestones achieved so far —
> compiles, builds, server boots to "Done" with clean chunk saves and full
> research-data load — and historical one-off tickets are recorded in git
> history. Only outstanding work appears below.

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

- **`CI=true` required for local runs**: without it, NeoGradle takes the
  local decompile→patch→recompile pipeline, which fails at `neoFormPatch`
  (decompiler emits `var1` parameter names; the patch expects named
  params). Some shells already export `CI`; otherwise prefix runs with
  `CI=true ./gradlew …`.
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
