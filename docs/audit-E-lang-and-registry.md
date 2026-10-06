# Audit E — language keys and registry snapshot

## 1. Registry snapshot (audit 3)

`reg_audit.py` enumerates every `registerItem(...)` / `registerBlock(...)` call and cross-checks names and models:

| check | result |
|---|---|
| registered items | 202 |
| registered blocks | 177 |
| registered names with **no lang entry** (would render a raw key) | **0** |
| items with **no item model / blockstate entry** | **0** |
| blocks with **no blockstate or model file** | **0** |

So nothing is unregistered, unnamed or unmodelled. (The earlier "missing item model" class of bug — 7 baubles + 2 meat
chunks — is closed.)

## 2. Language keys

Static keys: every `Component.translatable("literal")` / `I18n.get("literal")` in the codebase was collected
(161 distinct) and matched against `en_us.json` (1894 entries). The 23 that do not match are all *prefix
concatenations* (`"golem.arm." + key`, `"tc.aspect." + tag`, …), so each concrete value was enumerated from the
registries and checked:

| family | concrete values | missing |
|---|---|---|
| `tc.aspect.<name>` | 37 aspects | 0 |
| `tc.research_category.<id>` | ALCHEMY, ARTIFICE, AUROMANCY, BASICS, ELDRITCH, GOLEMANCY, INFUSION | 0 |
| `golem.material[.text].<id>` | wood, iron, clay, brass, thaumium, void | 0 |
| `golem.head[.text].<id>` | basic, smart, scout, smart_armored, smart_scout | 0 |
| `golem.arm[.text].<id>` | basic, fine, claws, breakers, darts | 0 |
| `golem.leg[.text].<id>` | walker, roller, climber, flyer | 0 |
| `golem.addon[.text].<id>` | armored, fighter, hauler, **none** | **2 → added** |
| `item.thaumcraft.seal.<key>` | 16 registered seal keys | 0 |
| `golem.prop.*` | 17 seal toggle/property labels | 0 |
| `champion.mod.<name>` | 14 champion modifiers | 0 |
| `item.celestial_notes.<id>.text` | sun, moon_1..8, stars_1..4 | 0 |
| `item.thaumcraft.curio.<suffix>` | arcane, preserved, ancient, eldritch, knowledge, twisted, rites | 0 |
| `item.thaumcraft.fortress_helm.mask.<n>` | 0..3 | 0 |
| `tc.forbidden.level.<n>` | 1..5 | 0 |
| `tc.type.<knowledge type>` | observation, theory | 0 |

Added: `golem.addon.none` = "None", `golem.addon.text.none` = "No addon installed." (the golem builder GUI was
showing the raw key for an empty addon slot).

## 3. Research text is data, not lang

All 148 research entries carry an inline English `name` and stage `text` in
`assets/thaumcraft/research/*.json`; none of them have `research.<key>.name/.text` lang entries — and that is
correct for this port's data layout.

One place still assumed the 1.12 layout: the required-research icon tooltip on a research page asked for
`research.<key>.text`, which does not exist here, so hovering such an icon printed the raw key.
`ResearchPageScreen.researchDescription()` now reads the entry's first stage text from the research JSON and only
falls back to the lang key.

## 4. Crystal names

`ItemVisCrystal.getName` → `item.thaumcraft.vis_crystal` = "%s Vis Crystal" filled with the aspect name
("Aer Vis Crystal"); an un-aspected crystal uses `item.thaumcraft.crystal_essence.generic`. No raw `%s` reaches
the screen.
