# 1.12-Parity Regression Audit Plan (26.3 port)

Every bug we have actually fixed in this port falls into one of **ten recurring bug
classes**. This audit sweeps the *entire* codebase for every one of those classes,
rather than hunting individual symptoms. Each class lists the real fixed bug it came
from, the detection method, and the fix rule.

Reference source for 1.12: `/private/tmp/tc121/03_self_decompiled_check/vineflower_thaumcraft6`
(secondary: `/private/tmp/tc121/02_existing_decompiled_repo/Thaumcraft-6-Source-Code-master`).

---

## Bug classes (each is one audit dimension)

### A. Invalid data references (tags / item IDs / block IDs that do not exist)
Fixed examples: `#c:stones` (tag does not exist) in `arcane_stone.json`; `crystal_fire`
and friends (unregistered items) in infusion recipes; `c:stone` tag missing
diorite/andesite/granite.
Detection:
1. Collect every `#namespace:path` tag reference from all recipe JSONs; verify each tag
   file exists (in our datapack, vanilla, or NeoForge `c`) and is non-empty.
2. Collect every plain item id from all recipe JSONs; verify each resolves to a registered
   item (cross-check `ModItems`/`ModBlocks` registries + vanilla).
3. Collect every tag reference in Java (`BlockTags`, `ItemTags`, `TagKey`) and verify the
   tag exists / is populated.
4. Verify tags that replace 1.12 OreDict names contain the same member set as 1.12.
Fix rule: point at an existing tag/id, or add the missing members to our tag file.

### B. Raw or untranslated language keys shown to the player
Fixed examples: `gui.thaumcraft.workbench.available`, `tc.aspect.name`,
`tc.knowledge.name`, 42 + 56 missing lang keys, `%s Vis Crystal` unfilled placeholder.
Detection:
1. Every `Component.literal(` whose argument is a dotted string or a variable that can
   hold a lang key.
2. Every `setTip(` / `drawString` / `renderComponentString` call whose argument is not
   already a translated string.
3. Every lang key referenced anywhere in Java or JSON that has no entry in
   `assets/thaumcraft/lang/en_us.json`.
4. Every lang value containing `%s`/`%d` that is ever rendered without format arguments.
Fix rule: `Component.translatable(key, args)` or add the missing lang entry; never leave
an unfilled format placeholder reachable.

### C. Wrong lifecycle/override method for the 26.3 target
Fixed examples: 15 items with block-interaction logic in `useOn` instead of
`onItemUseFirst` (dispatch order: `onItemUseFirst` → `BlockState.useItemOn` →
`useWithoutItem` → `useOn`).
Detection:
1. Every `useOn(` override in an Item subclass: decide block-interaction vs air-use; move
   block-interaction to `onItemUseFirst(ItemStack, UseOnContext)`.
2. Every `@Override` that no longer exists in the 26.3 supertype (silent non-override).
3. Every 1.12 hook name still present that 26.3 renamed (`animateTick`, `randomDisplayTick`,
   `onBlockAdded`, `updateTick`, `getSubtypes`, `addInformation`, `getItemStackDisplayName`).
4. Capability API drift: `IItemHandler`/`FluidTank` vs `ResourceHandler`/
   `ItemStacksResourceHandler`/`FluidStacksResourceHandler`.
Fix rule: reimplement on the 26.3 hook that actually fires.

### D. Client/server context misuse (crashes + silent no-ops)
Fixed examples: `level.getServer().getRecipeManager()` NPE in
`ThaumcraftCraftingManager.findMatchingArcaneRecipe` from the Arcane Workbench GUI.
Detection:
1. Every `level.getServer()` / `.getServer()` call: prove it is unreachable on the client
   or guarded; otherwise route through `Level.recipeAccess()` / a null-safe helper.
2. Every `Minecraft.getInstance()` reachable from common (non-`@OnlyIn`) code.
3. Every `Dist.CLIENT`-only class referenced from a common class without a side guard.
4. Every packet handler that touches client state on the server or vice versa.
Fix rule: side-safe accessor, or move the call behind an `isClientSide` / `@OnlyIn` guard.

### E. GUI / particle render fidelity (order, size, scale, alpha, atlas)
Fixed examples: background drawn over items in 17 container screens (`extractContents` →
`extractBackground` stratum 0); know icons blitted as 256x256 when the texture is 16x16;
category badge drawn 1:1 with 0.75 alpha instead of scaled opaque; 12 particle classes
sampling the vanilla atlas; X-mirrored UV convention.
Detection:
1. Every container screen: background must be submitted in the background stratum
   (`extractBackground`) before item contents.
2. Every `blit`/`blitSprite`/`submitCustomGeometry` call: texture width/height arguments
   must match the actual PNG dimensions (audit against the real files).
3. Every icon drawn at a size different from its 1.12 counterpart (compare scale factors).
4. Every particle class: correct atlas layer, correct UV convention (TC grid = X-mirrored,
   item/block sprite = natural).
Fix rule: match the real texture size and the 1.12 transform exactly.

### F. Off-by-one and wrong index ranges
Fixed examples: workbench crystal check loop read slots 10-15 instead of 9-14 (both the
real check and the diagnostic log).
Detection:
1. Every slot-index loop in menus/screens; cross-check against the menu's slot layout and
   the 1.12 slot layout.
2. Every hardcoded `+ 1` / `- 1` in slot, page, row, column arithmetic.
3. Every `NonNullList`/`Container` size assumption.
Fix rule: derive indices from the menu layout constants, not literals.

### G. Vanilla stand-ins instead of 1.12 Thaumcraft effects
Fixed examples: `ParticleTypes.ENCHANT` in `ItemMagicDust.doSparkles` (the "witch
particles" complaint), thaumometer `use`/`highlightScannables`, `FXDispatcher` stubs,
missing crucible/altar FX.
Detection:
1. Every remaining `ParticleTypes.ENCHANT`, `WITCH`, `CRIT`, `SPELL`, `END_ROD`, `PORTAL`,
   `LARGE_EMISSION`, `TOWN_AURA` spawn in mod code — each must map to the 1.12 FX call.
2. Every `FXDispatcher` method that is empty, a no-op, or a simplified body vs 1.12.
3. Every 1.12 `SoundsTC` playback we never play.
4. Every 1.12 `addBlockEvent`/`sendParticle` path with no 26.3 packet equivalent.
Fix rule: port the 1.12 FX call with the 1.12 parameters.

### H. Missing 1.12 features, registrations, item variants
Fixed examples: `DustTriggerOre` never registered; 8 infusion enchantments unregistered;
elemental axe/hoe/pick/shovel + taint crystal cluster recipes missing.
Detection:
1. Diff 1.12 registration lists (`ConfigRecipes`, `ConfigMultiblocks`, `ConfigItems`,
   `ConfigResearch`, `ThaumcraftAPI`) against our `init/` classes.
2. Diff 1.12 recipe list against ours per category (arcane / infusion / crucible / alchemy).
3. Diff 1.12 research entries against `data/thaumcraft/research`.
4. Diff 1.12 item/block registry against `ModItems`/`ModBlocks`.
Fix rule: implement the missing feature rather than stubbing it.

### I. Research gate mismatches (keys, stages, requisites)
Fixed examples: `thaumometer.json` gated on `UNLOCKAUROMANCY` instead of `FIRSTSTEPS@2`;
`activatorrail.json` on `BASICTURRET` instead of `FIRSTSTEPS`; 5 recipes missing `@2`;
`ResearchBrowserScreen.mouseClicked` sending the wrong packet so `FIRSTSTEPS@1` was
permanently unsatisfiable.
Detection:
1. Every recipe research gate vs the 1.12 recipe's research requirement (key **and** stage).
2. Every `knowsResearch(` / `isResearchKnown(` call: does the key + stage match 1.12?
3. Every requisites check performed server-side that 1.12 performs client-side only
   (1.12 `doesPlayerHaveRequisites` checks parents, not stage requisites).
4. Every research page/browser interaction: does it send the packet 1.12 sends?
Fix rule: mirror 1.12's gate key, stage suffix, and which side evaluates it.

### J. Recipe category / type misassignment
Fixed examples: GrappleGun, Stabilizer, VisGenerator, GolemModule_Aggression,
GolemModule_Vision ported as Infusion recipes when 1.12 made them Arcane Workbench recipes.
Detection:
1. For every port recipe, compare the crafting station against the 1.12 station.
2. Flag any port recipe whose type does not match the 1.12 station for that result.
Fix rule: retype the recipe and move it to the matching datapack folder.

---

## Execution order

1. Static sweeps (A, B, F, I, J) — pure data/grep, no game needed. Fast, high yield.
2. API-drift sweeps (C, D) — compile + bytecode-level verification.
3. Render sweeps (E, G) — compare against 1.12 source and real texture dimensions.
4. Completeness sweep (H) — registry/recipe/research diffs.
5. Fix everything found, in dependency order (data → code → render).
6. VERIFY: `./gradlew build` green, 0 TODOs, jar installed in the Modrinth
   `NeoForge 26.3` profile, docs updated, committed and pushed.

Any dimension that fails to complete is retried until every possibility is covered.
