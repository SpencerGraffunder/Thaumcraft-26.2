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
- Research data loads: **148 entries / 7 categories** (golemancy 29/29)
- Tag-based aspect registration runs without errors — 26.2 changed
  `Identifier.withDefaultNamespace` to path-only; all id call sites now use
  `Identifier.parse`

## Known issues & environment

- **`CI=true` required for local runs**: without it, NeoGradle takes the
  local decompile→patch→recompile pipeline, which fails at `neoFormPatch`
  (decompiler emits `var1` parameter names; the patch expects named
  params). Some shells already export `CI`; otherwise prefix runs with
  `CI=true ./gradlew …`.
- **Dev-server console**: `stop` typed into the `runServer` console is not
  forwarded to the server process — stop with SIGINT/Ctrl+C.
- **Legacy item names in research icons**: `minecraft:noteblock`,
  `thaumcraft:brain`, `thaumcraft:biothaumic_mind` have no 26.2 mapping
  (icons fall back to texture ids — add mappings to `LEGACY_ITEM_MAPPINGS`).
- **Dead code**: `CommonInternals.initAspects()` / `initSmeltingBonuses()`
  are never called (aspects come from `ConfigAspects` tag registration;
  `TileInfernalFurnace` still TODOs the smelting-bonus port).

## Known build warnings

- 147 warnings on `./gradlew build` (2026-09-03, OpenJDK 25.0.4): JEI compat
  code (`src/main/java/thaumcraft/compat/jei/`) uses APIs JEI marked for
  removal — `RecipeType`, `IIngredientAcceptor.addItemStack`,
  `IRecipeCatalystRegistration.addRecipeCatalyst`. Port to the new JEI API
  before the next JEI release drops them.
