# Audit D — data validation (recipes, tags, research requirements)

Audited every JSON under `src/main/resources/data/` against the items, blocks and tags
actually registered by the mod (332 recipes, 96 tags, 194 loot tables, 8 research files).

## What was checked

1. **Recipe ingredients** — every `item`, `tag` and nested-tag reference resolved against
   registered items + all tag definitions (vanilla `minecraft`, NeoForge `c`, and mod).
2. **Research stage requirements** — every `required_item` / `required_craft` id.
3. **Biome tags** — `thaumcraft:is_tainted` referenced by `BiomeTC` must name a biome.
4. **Loot tables** — every block and entity registered has a matching loot-table file
   (`BlockOreTC` and `EntityBase` both default to the vanilla `BlockLootTables` name
   convention, so a missing file means the block drops nothing).

## Findings and fixes

| Finding | Fix |
|---|---|
| `thaumcraft:is_tainted` biome tag was empty (`replace:true`, no values) — tainted biomes never matched | names `thaumcraft:eerie` |
| Loot tables lived in `loot_tables/` (1.12 path); 26.3 reads `loot_table/` — ores and mobs dropped nothing | directory renamed |
| 3 crucible recipes (`metal_purification_{tin,lead,silver}`) required empty catalyst tags `#c:ores/{tin,lead,silver}` | recipes dropped; 1.12 purified via ore→nugget smelting instead (already ported) |
| 43 unknown ids in research stage requirements (`thaumcraft:brain`, `thaumcraft:baubles`, `thaumcraft:StoneArcane`, `thaumcraft:LogGreatwood`, `thaumcraft:essence`, `oredict:chest`, …) — those researches could never complete and their requirement icons rendered blank | mapped to registered equivalents (`brain_normal`, `ring_mundane`, `ancient_stone`, `greatwood_log`, `crystal_essence`, `#minecraft:chests`, …) |
| `thaumcraft:cluster` (singular) in `golemsoil` required_craft | `thaumcraft:cluster_tin` |

## Known remaining gaps (blocked on missing content, not data)

- **Turret arcane variants** (`turret{,Advanced,Greater}Armor`, `turretBore*`): the port has
  one `turret` block with no bore/armor variants, so those 6 1.12 recipes have no 1:1 target.
- **Infused-book outputs** (`IE*` recipes): 1.12 infused a *book* carrying the thaumic
  enchantment; the port models thaumic enchantments as data components on the tool itself,
  so the recipes output the tool. Behaviour is equivalent; the item differs.
- **`minecraft:tall_flowers` / `minecraft:overworld_natural_logs`** report as unknown tags in
  the 26.3 registry — these come from NeoForge's own tag set, not from our data.
