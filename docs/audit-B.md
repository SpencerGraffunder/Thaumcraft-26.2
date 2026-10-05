# Audit B — language keys that render as raw text in-game

Method: extracted every string literal from all 1013 Java sources, kept the
ones shaped like translation keys, and diffed them against the 1906 keys
defined in `src/main/resources/assets/thaumcraft/lang/en_us.json`. Then every
*dynamic* key (`"prefix" + expr`) was resolved by enumerating the real runtime
values from the code and data (seal keys from `SealHandler`, `NoteType` ids,
curio suffixes, mask ints, champion modifier names, warp levels, knowledge
types, research categories, `EnumGolemTrait`, golem part keys, all 148 research
entries' `name` + stage `text` keys) and diffing those concrete keys too.

## Fixed

| # | Finding | Fix |
|---|---------|-----|
| B-1 | **Every placed control seal was named with a raw key.** Seal keys are namespaced (`thaumcraft:breaker`), but `ItemSealPlacer.getName()` did `sealKey.replace(":", ".")`, producing `item.thaumcraft.seal.thaumcraft.breaker`. The lang file already had the 1.12 wording under `item.thaumcraft.seal.breaker` ("Control Seal: Block Breaker"). | `stripNamespace()` helper; name now resolves for all 16 seals. Blank seal uses `item.thaumcraft.seal.blank`. |
| B-2 | Seal item tooltip used `seal.<key>.desc`, a key family that exists nowhere (1.12's `ItemSealPlacer` adds no tooltip at all). | Tooltip line removed — matches 1.12. |
| B-3 | Seal configuration GUI title was `golem.seal.config`, an undefined key. | `SealMenuProvider` now titles the GUI with the seal being configured (`item.thaumcraft.seal.<key>`), as in 1.12. |
| B-4 | **7 container/GUI titles were undefined `container.thaumcraft.*` keys**: focal manipulator, potion sprayer, hungry chest, void siphon, hand mirror, focus pouch, pech trade. 1.12 has no `container.*` keys at all — its titles came from the tile/item name. | Pointed at the keys that already exist with 1.12 wording: `block.thaumcraft.focal_manipulator`, `block.thaumcraft.potion_sprayer`, `block.thaumcraft.hungry_chest`, `block.thaumcraft.void_siphon`, `item.thaumcraft.hand_mirror`, `item.thaumcraft.focus_pouch`, `gui.thaumcraft.pech.trade`. |
| B-5 | Celestial-notes tooltip built `item.thaumcraft.celestial_notes.<id>.text`; the 1.12 keys are `item.celestial_notes.<id>.text` ("Lunar, Full", "Stellar, Northern Quadrant", …). | Key prefix corrected in `ItemCelestialNotes`. |
| B-6 | Elemental shovel orientation tooltip had 3 undefined keys. | Added `item.thaumcraft.elemental_shovel.orientation.{horizontal,vertical,mixed}`. |
| B-7 | `champion.mod.tainted` undefined (1.12 has 13 champion modifiers, none named tainted — this one is a port addition). | Added `champion.mod.tainted = "Tainted"`. |

## Verified clean (no action needed)

- All 148 research entries: every `name` (`research.<KEY>.title`) and every
  stage `text` (`research.<KEY>.stage.N`) key is defined.
- All 7 research categories have `tc.research_category.*`.
- `tc.aspect.*` (40), `tc.type.{theory,observation}`, `tc.forbidden.level.1-5`,
  `golem.{trait,head,arm,leg,addon,material}.*` + `.text.*`,
  `item.thaumcraft.curio.*` (7 suffixes), `item.thaumcraft.fortress_helm.mask.0-3`
  all defined.
- `Component.literal()` with a key-looking string: **0 occurrences**.
- Remaining audit hits are false positives: `attribute.name.generic.attack_damage`
  (vanilla key), `thaumcraft.common.lib.events.EssentiaHandler` (a Java class
  name in a log message), and bare prefix fragments picked up by the scanner.
