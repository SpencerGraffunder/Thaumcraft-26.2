# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Migration Status

> Tracks the active **26.2 port**. Milestones achieved so far —
> compiles, builds, server boots to "Done" — and historical one-off tickets
> are recorded in git history. Only outstanding work appears below.

## Outstanding (runtime verification on a test server)

- [ ] Entity spawning/behaviour, golem seals, bosses
- [ ] Crafting UIs (arcane workbench, crucible, infusion altar, research table)
- [ ] Worldgen (greatwood/silverwood, ores, ruins, taint biome)
- [ ] Visual QA — several renderers use compile-first approximations
      (billboard beams, block-atlas sprite substitutions, banner tint limits)

## Known build warnings

- 147 warnings on `./gradlew build` (2026-09-03, OpenJDK 25.0.4): JEI compat
  code (`src/main/java/thaumcraft/compat/jei/`) uses APIs JEI marked for
  removal — `RecipeType`, `IIngredientAcceptor.addItemStack`,
  `IRecipeCatalystRegistration.addRecipeCatalyst`. Port to the new JEI API
  before the next JEI release drops them.
