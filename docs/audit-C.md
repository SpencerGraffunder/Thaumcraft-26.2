# Audit C — registered IDs vs models vs names (invisible-item bug class)

Method: scraped every `registerItem(...)` / `registerBlock(...)` id from
`thaumcraft/init/ModItems.java` and `ModBlocks.java`, then diffed against the
resource pack:

- 205 registered items vs `assets/thaumcraft/models/item/*.json`
- 193 registered blocks vs `assets/thaumcraft/blockstates/*.json`
- every `textures.layerN` in every item model vs the real PNG set
- every `"model"` reference in every blockstate vs `models/block/*.json`
- every registered id vs its `item.thaumcraft.<id>` / `block.thaumcraft.<id>`
  lang name key

## Fixed

| # | Finding | Fix |
|---|---------|-----|
| C-1 | **9 registered items had no item model JSON** and rendered as the missing-model placeholder: `amulet_mundane`, `amulet_fancy`, `ring_mundane`, `ring_apprentice`, `ring_fancy`, `girdle_mundane`, `girdle_fancy` (all 7 base baubles) plus `chunks_rabbit` and `chunks_mutton`. The textures all existed (`textures/item/amulet_fancy.png`, `chunk_rabbit.png`, …) — only the model files were missing. This is the cause of the "missing item model" warnings in the client log. | Added the 9 `models/item/*.json` files (`minecraft:item/generated` + `layer0` pointing at the existing texture). |

## Verified clean (no action needed)

- 193/193 registered blocks have a blockstate file.
- 0 item models point at a texture that does not exist.
- 0 blockstate model references point at a missing block model
  (`thaumcraft:empty`, `thaumcraft:banner`, `thaumcraft:candle`,
  `thaumcraft:arcane_stone`, `thaumcraft:eldritch_stone`,
  `thaumcraft:ancient_stone`, `thaumcraft:activator_rail_*` all resolve to
  files in `models/block/`).
- 205/205 items and 193/193 blocks have a defined lang name key — no raw
  `tile.thaumcraft.x` / `item.thaumcraft.x` can appear.

## Still open (found by this audit, tracked in the todo)

- The golem **part** API exists (`api/golems/parts/GolemHead|Arm|Leg|Addon|Material`)
  but the port registers **no concrete part implementations**, so the golem
  press cannot offer the 1.12 parts (`basic`, `scout`, `smart`, `smart_scout`,
  `fine`, `claws`, `breakers`, `darts`, `walker`, `roller`, `climber`, `flyer`,
  `armored`, `fighter`, `hauler`, wood/iron/brass/clay/thaumium/void). The lang
  keys for all of them are already present.
