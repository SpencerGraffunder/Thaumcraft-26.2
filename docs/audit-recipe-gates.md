# Audit: why recipes "don't work" (2026-10-06)

Diagnosis only — **no fixes applied yet** (user asked to check first).

Evidence base: `latest.log` from the Modrinth `NeoForge 26.3` profile (session 08:32–08:33),
the installed jar, and a static audit of all 332 recipe JSONs + 148 research entries.

## 1. Research progression is healthy

```
[TC-DIAG] knowledge sync applied client research=20 FIRSTSTEPS_stage=4
[TC-DIAG] nomicon click key=FIRSTSTEPS known=true canUnlock=true clientStage=4
[TC-DIAG] progressResearch REFUSED key=FIRSTSTEPS complete=true requisitesMet=true serverStage=4
```

FIRSTSTEPS is fully complete (stage 4 = all 3 stages done) and KNOWLEDGETYPES is complete.
The research-deadlock fix is working. So "recipe not working" is **not** a research-sync bug.

## 2. Eight recipes are gated on research keys that do not exist  ← hard blocker

`research` gate keys referenced by recipe JSONs that appear in **no** research JSON
(and in no 1.12 source either — they were invented during porting):

| bogus gate key | recipes it blocks |
|---|---|
| `CRYSTAL_ESSENCE` | 6 crucible recipes `crucible/crystal_essence_from_shard_*.json` |
| `JARS` | `arcane_workbench/label_filled.json` |
| `NUGGETS_FROM_CRUCIBLE` | `crucible/nugget_rareearth.json` |

`IPlayerKnowledge.isResearchKnown(key)` can never return true for a key that was never
loaded → these 8 recipes are permanently uncraftable.

## 3. Sixteen research-page "unlocked recipe" icons resolve to nothing

`ResearchPageScreen` (lines 970-982) draws `stage.getRecipes()` through
`RecipeRenderer.getCatalogRecipe`, which resolves: real recipe id → `FakeRecipes`
catalog → `RECIPE_ALIASES` → leaf-name index. These 13 resolve by none of those routes,
so the research page shows a blank/air icon and the player is told they unlocked a
recipe that does not exist:

```
lamp_arcane                ARCANELAMP@2
metal_purification_lead    METALPURIFICATION@1     (recipe file was deleted as uncraftable)
metal_purification_silver  METALPURIFICATION@1     (idem)
metal_purification_tin     METALPURIFICATION@1     (idem)
mind                       MINDBIOTHAUMIC@2
mirror_item                MIRROR@2
paving_stone_barrier       PAVINGSTONES@2
paving_stone_travel        PAVINGSTONES@2
smelter                    ESSENTIASMELTER@3, ESSENTIASMELTER@4
tube_normal                TUBES@2
turret_placer_basic        BASICTURRET@2
turret_placer_advanced     ADVANCEDTURRET@2
turret_placer_bore         ARCANEBORE@2
```

Reproduce with `python3 tools/audit_k2_resources.py --jar <installed jar>`.

Already handled correctly (do not re-fix): `infusion_altar`, `infusion_altar_ancient`,
`infusion_altar_eldritch`, `golem_press`, `thaumatorium` (FakeRecipes) and
`nitor_color/group`, `vis_crystal_group`, `brass_stuff`, `thaumium_stuff`, `void_stuff`,
`voidingot`, `mnemonic_matrix`, `jar_label`, `jar_label_essentia`, `banners`,
`baubles_stuff`, `arcane_brick`, `hedge_*`, `phial`, `salis_mundus_fake`, `ie_*_fake`
(aliases / `_fake` stripping / matching recipe file names).

## 4. Two problems from the last launch log are already fixed in the installed jar

* `IllegalStateException: out of bounds slot index 15` thrown from
  `thaumcraft.common.tiles.TileThaumatorium.swapItems` → that class path and method no
  longer exist. The installed jar contains `thaumcraft/common/tiles/crafting/TileThaumatorium`
  with no `swapItems` (verified with `javap`). The log predates the current jar.
* 18 `Missing item model for location thaumcraft:*` warnings (7 baubles, 3 masks,
  `cluster_tin/silver/lead`, `curio`, 3 `seal_*_advanced`, `wand_workbench`,
  `chunks_rabbit/mutton`) → all 202 registered items now have an
  `assets/thaumcraft/items/<id>.json` client definition in the installed jar (verified
  against the zip).

## 5. Recipe data itself is clean

* All 8 recipe types used by the 332 recipe files are registered
  (`arcane_workbench_shaped` 93, `crucible` 64, `infusion` 62, `crafting_shaped` 58,
  `crafting_shapeless` 31, `infusion_enchantment` 8, `arcane_workbench_shapeless` 8,
  `smelting` 8).
* No recipe file references an unregistered `thaumcraft:` item/block
  (the only hits are `hedge_duplication` / `hedge_transmutation` research keys and the
  `thaumcraft:vis_crystal` recipe **group** name).

## Next actions (tracked in the todo)

* #97 — repoint the 3 bogus gates to real research keys.
* #91 — add aliases or recipe files for the 13 unresolvable `stage.recipes` ids.
