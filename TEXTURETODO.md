# Thaumcraft 6 - Missing Textures and Models

This document lists all placeholder textures and missing assets that need to be created or sourced from the original Thaumcraft mod.

---

## Resolved (2026-09-05) — purple/black items (ClientItem root cause)

**Root cause:** 362 Thaumcraft items rendered **purple/black** because their
`assets/thaumcraft/models/item/<id>.json` ClientItem files were **missing entirely** — a
1.20.1→26.2 port gap. With no ClientItem, the item model never resolved, so every one of
those items drew as the missing-texture purple/black checkerboard.

**Fix (applied):**
- Generated the missing `models/item/<id>.json` ClientItem files
  (`tools/gen_client_items.py`).
- Re-pointed 25 model `textures` refs that used wrong/missing texture ids to the textures
  that actually exist (`tools/fix_item_textures.py`).
- Generated 4 placeholder textures so their models resolve (real art still needed, see
  below): `thaumometer.png`, `research_notes.png`, `complete_notes.png`, `primal_charm.png`.
- Added a `particle` texture ref to the 10 concrete item models that lacked one
  (casters, grapple gun, primordial pearl, verdant charm) — clears the residual
  "Missing texture references … : particle" bake warnings (cosmetic; block-break particles).

**Verified (2026-09-05):** `runClient` log shows **0 "Missing item model for"**
(previously 362) — items no longer render purple/black. The `particle` refs above resolve the
missing particle texture (it is no longer enumerated as missing). Note: 5 residual
`Missing texture references in model …:` warnings still print with an **empty** list — a
benign 26.2 MaterialBaker quirk for `overrides` models, with **no** rendering impact.

> These items render correctly now; the entries below are **placeholder textures that still
> need real art**, not rendering breakage.

---


## Missing Item Textures

These item models reference textures that don't exist:

### High Priority (Commonly Used Items)

| Model File | Missing Texture | Notes |
|------------|-----------------|-------|
| `thaumometer.json` | `thaumometer.png` | Core gameplay item |
| `phial_empty.json` | `phial_empty.png` | Common resource item |
| `phial_filled.json` | `phial_filled.png` | Common resource item |
| `research_notes.json` | `research_notes.png` | Research system item |
| `complete_notes.json` | `research_complete.png` | Research system item |
| `label_blank.json` | `label_blank.png` | Labeling system |
| `label_filled.json` | `label_filled.png` | Labeling system |
| `seal_provide.json` | `seals/seal_provide.png` | Golem seal |
| `blank_seal.json` | `seal_blank.png` | Base seal item |

### Medium Priority (Special Items)

| Model File | Missing Texture | Notes |
|------------|-----------------|-------|
| `primal_charm.json` | `primal_charm.png` | Bauble item |
| `bucket_pure.json` | `bucket_pure.png` | Liquid bucket |
| `curiosity.json` | `curio_common.png` | Curio item base |
| `brain_curious.json` | `mind_curious.png` | Golem brain variant |
| `caster_master.json` | `caster_basic.png` | Casting gauntlet variant |

### Low Priority (Celestial Notes Variants)

| Model File | Missing Texture | Notes |
|------------|-----------------|-------|
| `celestial_notes_moon_1.json` | `celestial/moon_1.png` | Moon phase 1 |
| `celestial_notes_moon_2.json` | `celestial/moon_2.png` | Moon phase 2 |
| `celestial_notes_moon_3.json` | `celestial/moon_3.png` | Moon phase 3 |
| `celestial_notes_moon_4.json` | `celestial/moon_4.png` | Moon phase 4 |
| `celestial_notes_moon_5.json` | `celestial/moon_5.png` | Moon phase 5 |
| `celestial_notes_moon_6.json` | `celestial/moon_6.png` | Moon phase 6 |
| `celestial_notes_moon_7.json` | `celestial/moon_7.png` | Moon phase 7 |
| `celestial_notes_moon_8.json` | `celestial/moon_8.png` | Moon phase 8 |
| `celestial_notes_stars_1.json` | `celestial/stars_1.png` | Star pattern 1 |
| `celestial_notes_stars_2.json` | `celestial/stars_2.png` | Star pattern 2 |
| `celestial_notes_stars_3.json` | `celestial/stars_3.png` | Star pattern 3 |
| `celestial_notes_stars_4.json` | `celestial/stars_4.png` | Star pattern 4 |

### Placeholder Textures (Need Real Assets)

These use a generic `placeholder.png` texture:

| Model File | Item | Notes |
|------------|------|-------|
| `golem_module_aggression.json` | Golem Module (Aggression) | Needs dedicated texture |
| `golem_module_vision.json` | Golem Module (Vision) | Needs dedicated texture |
| `grapple_gun_spool.json` | Grapple Gun Spool | Component item |
| `grapple_gun_tip.json` | Grapple Gun Tip | Component item |

---

## Missing Block Textures

All block models have been fixed to use existing textures. The following blocks use substitute textures:

| Block | Current Texture | Ideal Texture |
|-------|-----------------|---------------|
| `barrier.json` | `empty.png` | Should be invisible (effect block) |
| `effect_sap.json` | `empty.png` | Should be invisible (effect block) |
| `effect_shock.json` | `empty.png` | Should be invisible (effect block) |
| `essentia_reservoir.json` | `metal_alchemical.png` | Needs dedicated reservoir texture |
| `flux_scrubber.json` | `metal_alchemical_advanced.png` | Needs dedicated scrubber texture |
| `focal_manipulator.json` | `arcane_workbench_*.png` | Needs dedicated manipulator texture |
| `ancient_pedestal_doorway.json` | `ancient_stone_0.png` | Needs doorway-specific texture |

---

## GUI Textures

### Research Browser Category Icons

The research browser uses category icons that may need paths fixed:

| Category | Expected Path | Notes |
|----------|---------------|-------|
| Basics | `textures/gui/category/basics.png` | Check path |
| Alchemy | `textures/gui/category/alchemy.png` | Check path |
| Artifice | `textures/gui/category/artifice.png` | Check path |
| Infusion | `textures/gui/category/infusion.png` | Check path |
| Golemancy | `textures/gui/category/golemancy.png` | Check path |
| Eldritch | `textures/gui/category/eldritch.png` | Check path |
| Auromancy | `textures/gui/category/auromancy.png` | Check path |

---

## Entity Textures

Entity textures should be verified:

| Entity | Texture Path | Status |
|--------|--------------|--------|
| Thaumcraft Golem | `textures/entity/golem/` | Verify all material variants |
| Wisp | `textures/entity/wisp.png` | Check existence |
| Pech | `textures/entity/pech.png` | Check existence |
| Cultists | `textures/entity/cultist/` | Check all variants |
| Eldritch creatures | `textures/entity/eldritch/` | Check all variants |

---

## How to Fix

### Option 1: Extract from Original Mod
1. Obtain original Thaumcraft 6 JAR for MC 1.12.2
2. Extract textures from `assets/thaumcraft/textures/`
3. Copy to corresponding paths in this project
4. Some textures may need resizing (original was likely 16x16 or 32x32)

### Option 2: Create New Textures
1. Create 16x16 PNG textures for items/blocks
2. Follow the existing art style (magical, purple/gold accents)
3. Save to appropriate `textures/item/` or `textures/block/` folder

### Option 3: Use Existing Textures as Fallback
Many models can use similar existing textures:
- `phial_empty.png` / `phial_filled.png` -> Could use a glass bottle variant
- `research_notes.png` -> Could use paper/book texture
- Celestial notes -> Could all use same base texture with tint

---

## Statistics

- **Total Item Textures**: 212 files
- **Total Block Textures**: 250 files
- **Purple/black items**: **0** (362 ClientItem files generated — resolved 2026-09-05)
- **Placeholder Item Textures (need real art)**: 4 (thaumometer, research_notes,
  complete_notes, primal_charm) + the High/Medium-priority lists above
- **Substitute Block Textures**: 7

---

## Quick Fixes Applied

The following quick fixes have been applied to allow the mod to run:

1. Effect blocks (barrier, sap, shock) use `empty.png` - these are invisible blocks
2. Device blocks use similar metal/workbench textures
3. Golem modules use `placeholder.png` - functional but ugly

---

*Last Updated: September 5, 2026*
