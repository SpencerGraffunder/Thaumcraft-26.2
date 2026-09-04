# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Migration Status

> Tracks the active **26.2 port**. Milestones achieved so far —
> compiles, builds, server boots to "Done" with clean chunk saves and full
> research-data load — and historical one-off tickets are recorded in git
> history. Only outstanding work appears below.

## Outstanding (runtime verification on a test server)

- [ ] Entity spawning/behaviour, golem seals, bosses
- [ ] Crafting UIs (arcane workbench, crucible, infusion altar, research table)
- [ ] Worldgen (greatwood/silverwood, ores, ruins, taint biome)
- [ ] Visual QA — several renderers use compile-first approximations
      (billboard beams, block-atlas sprite substitutions, banner tint limits)

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
