# Audit A — invalid tags / item IDs / block IDs

Method: recursive walk of every string in all 339 recipe JSONs (1228 references
checked, including `catalyst`, `center`, `key`, `ingredients`, `components`), plus
all 52 Java `TagKey`/`BlockTags`/`ItemTags` references, plus every recipe `result`,
plus nested tag-inside-tag references.

Reference sets built from real data, not guesses:
- 900 vanilla 26.3 tag files (extracted from the Modrinth 26.3 version jar)
- 546 NeoForge 26.3 tag files (extracted from `neoforge-26.3.0.33-beta-universal.jar`)
- vanilla item/block id list from the vanilla `en_us.json`
- mod item/block ids scraped from `src/main/java/thaumcraft/init/*.java`

## Fixed

| # | Finding | Fix |
|---|---------|-----|
| A-1 | `thaumcraft:is_tainted` biome tag was **empty** while `BiomeHandler.IS_TAINTED` / `isTaintedBiome()` read it. 1.12's taint biome is `thaumcraft:eerie` (`BiomeGenEerie.setRegistryName("thaumcraft","eerie")`). | Tag now contains `thaumcraft:eerie`. |
| A-2 | `arcane_stone.json` used `#c:stones` (no such tag) | Already fixed in 4c5409d → `#c:stone`. |
| A-3 | `c:stone` tag only had `minecraft:stone`; 1.12 OreDict `stone` = stone + diorite + andesite + granite | Already fixed in 4c5409d → all four members. |

## Investigated and confirmed NOT bugs (do not "fix")

- **Empty `c:ores/tin`, `c:ores/lead`, `c:ores/silver`** used as `catalyst` in
  `metal_purification_{tin,lead,silver}.json`. In 1.12 these are OreDict names
  supplied by *other* mods — `ModConfig.java:185/215/245` guards every use with
  `OreDictionary.doesOreNameExist("oreTin")`. Thaumcraft 6 registers no tin/lead/silver
  ore blocks of its own. An empty tag that matches nothing is therefore 1.12-faithful;
  inventing TC ore blocks would *break* parity.
- **Empty `c:dusts/{gold,iron,copper}`** — same OreDict-from-other-mods situation, and
  no port recipe currently references them.
- **`minecraft:strong_healing` / `minecraft:strong_regeneration`** in
  `verdant_charm_{life,sustain}.json` `potion_contents` — verified as real vanilla 26.3
  potion ids (present in the vanilla jar and used by vanilla's own
  `recipe/brewing/potion_healing_glowstone_dust.json`). Flagged only because the checker
  knew the item/block registries, not the potion registry.
- **Broken nested refs `c:flowers/tall -> #minecraft:tall_flowers` and
  `c:natural_logs/overworld -> #minecraft:overworld_natural_logs`** — these files ship
  inside NeoForge (`nf/data/c/tags/...`), not in our datapack. Upstream issue.

## Clean

- 0 missing tags in recipes, 0 unknown item ids in recipe ingredients, 0 unregistered
  recipe results, 0 unresolved Java tag references.
- `thaumcraft:crystals` correctly holds all 7 crystal items.
- Our `data/minecraft/tags/...` overrides (planks, saplings, logs_that_burn,
  wooden_slabs, leaves, wooden_stairs, mineable/pickaxe, mineable/axe, damage_type) all
  resolve and use `replace:false` so they extend rather than clobber vanilla.
