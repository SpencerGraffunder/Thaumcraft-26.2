# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Migration Status

> Tracks the active **26.2 port**. Milestones achieved so far —
> compiles, builds, server boots to "Done" with clean chunk saves and full
> research-data load — and historical one-off tickets are recorded in git
> history. Only outstanding work appears below.

## 1.12 Book Parity — Star Markers (2026-09-12, shipped 3c157f6)

User asked for the 1.12 "new" star markers (gold star on a research icon /
category when something new happened; clears when the page is opened).
Audit of the port found the system 90% present (flag set on server,
synced via PacketSyncKnowledge, map-icon stars + hover tooltips drawn,
flags cleared on open) but **broken end-to-end**:

- **Root cause**: `ResearchManager` sets RESEARCH/PAGE flags and marks
  `ResearchManager.syncList` (ResearchManager.java:91/178/273), but
  `PlayerEvents.livingTick` only checked `PlayerEvents.syncList` (its own
  HashSet, never populated) → `PacketSyncKnowledge` never sent after
  research completion → stars/toasts only appeared after rejoin.
  1.12 reference (PlayerEvents:123) removes from `ResearchManager.syncList`.
- **Fix** (`PlayerEvents.java` livingTick): now consumes both sets —
  `syncList.remove(name) || ResearchManager.syncList.remove(name) != null`.
- **Missing 1.12 feature added**: category-sidebar stars. `CategoryButton`
  (ResearchBrowserScreen.java) now computes per-category nr/np (any known
  research in the category with RESEARCH/PAGE flag) and draws the gold star
  (texture 176,16 / 208,16 @ 0.25 scale, alpha 0.7) at (x-2, y-2) / (x-2, y+9)
  + hover lines "Newly discovered research" / "New page added" — 1:1 with
  GuiResearchBrowser:1118-1148. (26.2 pose API is 2D: `scale(0.25f)`.
  Lambda can't mutate locals → `boolean[] newFlags` holder.)

Verified: build green, **86/86 tests**. Jar `thaumcraft-6.2.0+26.2.jar`
reinstalled into the `NeoForge 26.2` Modrinth profile.

## 1.12 Book Parity — Round 5 (2026-09-12, built & installed; in-game check pending)

- **Nitor item display (16 colors)**: item models were parented to the 3D
  `cube_all` block model, which all rendered as the plain `nitor_core` cube.
  Now 1.12-style flat generated items: `minecraft:item/generated` with
  `layer0: thaumcraft:block/nitor_<color>` (new 16×16 real-art PNGs, verified
  non-placeholder) over `layer1: thaumcraft:block/nitor_core`. Block models
  unchanged (placed blocks stay 3D cubes).
- **Research page text width**: `TEXT_WIDTH` 104 → **140** font-pixels (104
  wrapped too aggressively vs 1.12);
  `ResearchPageScreen.java:55-62`.
- **Research completion semantics** (`ResearchPageScreen.java:162-169`):
  `isComplete` was `playerKnowledge.isResearchComplete(key)`; now
  `isComplete = currentStage > stages.length` — 1.12 shows the current stage's
  "pre" text until the player has progressed *past* the last stage, then the
  final "post" text + addenda. Matches the map-view blink (blinking = not
  complete).
- **Requirement icon rows** (`ResearchPageScreen.java:597-608`): start moved
  from `sh + PANE_HEIGHT - 22` to `sh + PANE_HEIGHT + 13` — 1.12 puts the
  lowest (research) row at ~`sh+176`, near the scaled book's bottom; the old
  position floated the rows up into the text area (the ALUMENTUM "icons over
  the text" bug).
- **Scan entity names** (`research/scans.json`): 12 `name` keys renamed from
  1.12 legacy `entity.<Name>.name` to the 26.2 namespaced
  `entity.thaumcraft.<Name>` keys that the lang files already define
  (Wisp, ThaumSlime, Firebat, TaintSeed, …).

Verified: `CI=true ./gradlew build` green, **86/86 tests**, jar
`thaumcraft-6.2.0+26.2.jar` (md5 `12bcc08ec26caccc05dcce5b829e0566`) installed
into the `NeoForge 26.2` Modrinth profile. **User in-game verification pending:**
nitor items show the colored overlay in inventory; book text wraps at ~140px;
requirement rows sit at the book bottom, not over the text; scans show entity
names; research text flips pre→post on completion.

## P0: Thaumonomicon recipe pages broken — RESOLVED (2026-09-09)

Symptom (user-reported): in the book, the arcane-workbench recipe popup
showed a bare white ring instead of the workbench, with no arrow to the
output; the crucible (Salis Mundus) popup had no crucible image in the
center; several pages said `Recipe not found` (e.g.
`thaumcraft:nitor_color`, all infusion-enchantment + runic-shielding +
multiblock pages); page text overflowed the bottom and titles sat too high.

Root causes:
- The 1.20.1 fork registered **display-only "fake" recipes** (IE
  enchantments, runic shielding, multiblocks) in a code-side catalog; the
  26.2 port lost that registration, so 16 book references had nothing to
  render.
- 18 legacy book ids never matched a recipe file name (`nitor_color`,
  `brass_stuff`, …) — the port's alias table was incomplete.
- Popup renderers predated the user-facing layout expectations.

Fix:
- `ConfigRecipes` (ported from the 1.20.1 fork, commit 4d3cde8): 8 IE
  enchantment fakes + 3 runic-shielding fakes with the correct base tools,
  aspects, instability — render as infusion recipe popups.
- `FakeRecipes` + `FakeRecipe` (new, commit 73411d8): 5 multiblock display
  recipes (infusion altar x3, thaumatorium, golem press) with a dedicated
  title + item-row popup.
- `RecipeRenderer`: `RECIPE_ALIASES` for 18 legacy ids, `_fake[_N]` suffix
  stripping, `FakeRecipe` dispatch + `resolveOutput` (bookmarks).
- Registered both catalogs in `bootstrap()` (server start + first client
  tick).
- `BookPopupRenderer` full rewrite (2026-09-10) to the 1.12 overlay look:
  gilded-paper panels blitted from the **full 256x256** texture (the old
  code blitted only its top-left quarter, so panels had no right/bottom
  frame); station line-art blitted from `gui_researchbook_overlay.png`
  (crucible pot + flame/arrow, infusion altar diamond, arcane workbench
  grid); every recipe popup shows the output item with an arrow, aspect
  rows, and (arcane) crystal row + vis cost; aspect popup is a 256x256
  paper page with 1.5x aspect icons, names, and primal/secondary type
  text; titles sit below the panel frame (old `scale`-then-`translate`
  pose order also offset them left/up by 10% of the screen position).
- `ResearchPageScreen`: text-block vertical centering uses the exact
  scaled line height (11.25px) instead of the truncated int (11px).
- `ShapedArcaneRecipe` / `ShapelessArcaneRecipe` / `CrucibleRecipeType` /
  `InfusionRecipeType`: override `isSpecial()` -> `true`. 26.2 vanilla
  `finalizeRecipeLoading` drops any non-special recipe whose
  `placementInfo()` is `NOT_PLACEABLE` ("can't be placed due to empty
  ingredients"), which silently removed **every** arcane/infusion/crucible
  recipe from the recipe map — so the Thaumonomicon's leaf-name index (built
  from the recipe manager) had nothing to resolve and pages showed
  `Recipe not found`. Marking them special keeps them in the map (station
  matching, JEI, Thaumonomicon all resolve them) while hiding them from the
  vanilla recipe book.

Verified (2026-09-10): compiles clean; **all 253 book recipe references
resolve** (218 recipe files, 16 fake-catalog, 4 `_fake`-suffix, 14 alias);
dev server boots clean (`Thaumcraft runtime registration complete`, 148
research entries, populated recipe manager, no data-pack errors). Jar
`thaumcraft-6.2.0+26.2.jar` (md5 8622cc983c43656e082f43dd7005be0b) installed
into the Modrinth 26.2 profile — user in-game verification pending.



Symptom: picking up the **Thaumonomicon** hung the dedicated server thread
until the `ServerWatchdog` killed it (`crash-2026-09-07_10.15.10-server.txt`).
The thread dump showed the server thread stuck in
`ResearchManager.progressResearch` → `completeResearch` (called from
`PlayerEvents.onItemPickup`), spinning with no forward progress.

Root cause: `completeResearch` loops on `while (progressResearch(...))`.
`progressResearch` ends with an **unconditional `return true`**, and the loop
only exits once `isResearchComplete(researchKey)` becomes true. The
`!gotthaumonomicon` flag (and its siblings `!gotcrystal`, `!gotdream`,
`!BATHSALTS`, `!INSTABILITY`) are **undefined** research keys — they have no
`ResearchEntry`, so `getStages()` is never set and `isResearchComplete` can
never flip to true. The loop therefore ran forever.

Fix (`ResearchManager.progressResearch`): when there is **no staged entry to
progress** (key undefined, or entry with no stages), mark the flag complete
(`setResearchStage(key, 1)`) so prerequisite/flag checks pass, sync if
requested, and `return false` so the `completeResearch` while-loop
terminates. This preserves the intended "flag unlocked" semantics while
guaranteeing termination.

Verified (2026-09-07): rebuilt jar on local disk (repo is on SMB, where
Gradle's `FileHasher` fails), installed into the Modrinth profile, server
restarted clean (`Thaumcraft setup complete!`, 148 research entries loaded),
and the compiled class was inspected to confirm the new
`entry==null / stages==null / stages.length==0` guard is present. Player
research state confirmed clean (no `!gotthaumonomicon`), so a real
Thaumonomicon pickup exercises the fixed path.


## P0: Golem parts crash on plain client (creative inventory NPE) — RESOLVED (2026-09-07)

Symptom: on a **plain (integrated / Modrinth) client**, opening the creative
inventory (or any screen that renders Thaumcraft golem items) crashed with
`Cannot read field "id" because "mat" is null` — a `NullPointerException` in
golem-material resolution. A dedicated server + client was unaffected, which
hid the bug from the earlier multiplayer verification.

Root cause: golem parts/seals/research/aspects/multiblocks are registered only
on `ServerStartingEvent`. That event fires on a **server** (dedicated or the
integrated server of a `runClient` dev run) but **never on a plain client**.
On 26.2 those registrations construct vanilla `ItemStack`s, which is illegal
before the registry set has bound its data-component initializers
(`Components not bound yet`), so the original port deferred them to
`ServerStartingEvent` — which a standalone Modrinth client never sees, leaving
`GolemMaterial` empty and the creative-inventory render path to NPE.

Fix:
- `Thaumcraft.java`: extract the runtime registration into an idempotent
  `bootstrap()` with a **readiness probe** (`new ItemStack(Items.IRON_INGOT)`
  succeeds only once components are bound; otherwise it defers and retries the
  next tick). `bootstrap()` is now called from **both**:
  - `onServerStarting` (server, as before), and
  - a new `onClientTick(ClientTickEvent.Pre)` — a plain client registers on
    its first tick, before any in-game screen can read the data.
  Guarded by a `volatile boolean bootstrapped` (double-checked, synchronized)
  so it runs exactly once per side and is safe to call from both.
- `GolemProperties.java`: `registerDefaultParts()` now returns early if
  `GolemMaterial.getMaterials()[0] != null` (already registered), making the
  both-sides call path idempotent.

Verified (2026-09-07, macOS Modrinth client, 26.2.0.76): rebuilt jar installed
into the profile → client boots, **creative inventory opens with no crash**
(previously NPE), creative **search finds all 15 golem items** (builders,
placers, seals, bells, pearls) and they **render with real art** (not
checkerboard), and the client log shows
`Registered golem parts` → `Registered golem seals` → … →
`Thaumcraft runtime registration complete`.

## P0: Purple/black items — ClientItem files missing — RESOLVED (2026-09-05)

Symptom: 362 Thaumcraft items rendered **purple/black** (missing-texture
checkerboard) in the client.

Root cause: the 1.20.1→26.2 port dropped the per-item
`assets/thaumcraft/models/item/<id>.json` ClientItem files, so those item
models never resolved.

Fix:
- Generated the 362 missing ClientItem files (`tools/gen_client_items.py`).
- Re-pointed 25 model `textures` refs to the textures that exist
  (`tools/fix_item_textures.py`); generated 4 placeholder textures
  (thaumometer, research_notes, complete_notes, primal_charm).
- Added a `particle` texture ref to the 10 concrete item models that lacked
  one (casters, grapple gun, primordial pearl, verdant charm) so the missing
  particle reference resolves.

Verified (2026-09-05): `runClient` log shows **0 "Missing item model for"**
(was 362) — items render correctly. 5 residual `Missing texture references …:`
warnings print with an **empty** list (benign 26.2 MaterialBaker quirk for
`overrides` models; no rendering impact). Full detail in `TEXTURETODO.md`.


## P0: Broken item textures (placeholder checkerboards) — RESOLVED (2026-09-06)

Symptom: **69** Thaumcraft item/block/entity textures rendered as the
missing-texture **purple/black checkerboard** placeholder (not real art) —
e.g. `item/candle`, `item/shard_*`, `item/bucket_pure`, all casters/placers/
grapple gun, golem pearls/charms, the void jars, arcane-workbench block, and
the taint-spider/slime/dart entity art. (Distinct from the 362 missing
ClientItem files above — this is the textures themselves being placeholders.)

Root cause: the 1.20.1→26.2 port generated placeholder textures (a
deterministic 8x8 purple/black grid) in place of the real Thaumcraft art,
which was not carried over.

Fix:
- Mapped all 69 placeholder textures to their real art in the released
  Thaumcraft jars (TC4 `1.7.10`, TC5 `1.8.9`, TC6 `1.12.2`) and copied the
  real PNGs into `src/main/resources/assets/thaumcraft/textures/`.
- Fixed the missing `bucket_pure` texture (referenced by
  `models/item/bucket_pure.json` but absent) — copied real `bucket_pure` from TC5.
- Added a regression test: `thaumcraft.common.resources.TextureIntegrityTest`
  (2 tests) — (a) every texture ref in every JSON model/blockstate exists as
  a file (catches `bucket_pure`-style dangling refs), (b) no PNG is a
  placeholder checkerboard (catches this regression).

Verified (2026-09-06): `TextureIntegrityTest` green; full suite **86 tests,
0 failures**; rebuilt jar installed into the Modrinth 26.2 profile shows
**0 placeholder textures** (was 69) with `candle`/`shard_fire`/`bucket_pure`
confirmed as real art; `runClient` log shows **0 "Missing item model for"**.
The 5 residual `Missing texture references …: particle` warnings are the
cosmetic block-break particle texture only (items render fine).

## P0: Multiplayer broken — S2C payload channels missing on server — RESOLVED (2026-09-05)

Symptom: any client connecting to a Thaumcraft dedicated server is
rejected during the NeoForge 26.2 channel handshake:
`Channel [thaumcraft:packet<name>] failed to connect: This channel is
missing on the server side, but required on the client!` followed by
`Incompatible client!` disconnect.

Root cause: `PacketHandler.register` wraps all 26 `playToClient`
registrations in `if (FMLEnvironment.getDist() == Dist.CLIENT)`, because
the S2C packet classes carry their client handler inline
(`handleClient`/`handleOnClient` referencing `net.minecraft.client.*`),
which cannot load on a dedicated server. Skipping registration leaves the
server's channel list incomplete, so the handshake always fails — the mod
has no working dedicated-server multiplayer.

Fix (applied): split each S2C payload class into a server-safe common
part (data + encode/decode + `TYPE`/`STREAM_CODEC`) and a client handler
class in `thaumcraft.client.lib.network.*`:
- common class gains `public static Consumer<P> CLIENT_HANDLER = msg -> {};`
  and its `handle(msg, ctx)` delegates to it;
- client class `<Name>Client` holds the extracted handler logic;
- `thaumcraft.client.lib.network.PacketClientWiring.init()` assigns all 26;
- `Thaumcraft.ClientModEvents.onClientSetup` calls `PacketClientWiring.init()`;
- `PacketHandler.register` registers all 26 `playToClient` unconditionally.

Verified (2026-09-05): `build` green → jar installed into the Modrinth
26.2 profile (`sha256` matches build artifact) → client 26.2.0.75 joins
the dev server 26.2.0.75 (localhost:25565): NeoForge handshake passes,
**zero channel errors**, connection ESTABLISHED and stable; player in-world
(server log: "joined the game", "was slain by Phantom"; pause menu shows
"Disconnect"). All 38 `thaumcraft:packet*` channels register on both dists.
`tools/` holds the evdev/Ding input drivers used for the GUI join test.

## P1: Focus package execution + arcane workbench validation — RESOLVED (2026-09-14)

Focus projectiles were spawning but never executing their focus packages —
the `FocusEngine.runFocusPackage` calls were commented out as TODOs.
Arcane workbench had no validation (vis discount, crystals, research).

Fixes:
- **EntityFocusProjectile**: `executeFocusPackage` now calls
  `FocusEngine.runFocusPackage(focusPackage, trajectory, hit)` instead of TODO.
- **EntityFocusMine**: Executes focus package for each entity in range with
  proper trajectory direction from center to entity position.
- **EntityFocusCloud**: Executes focus package for both entity hits and
  block hits (random directions for block interactions).
- **ArcaneWorkbenchMenu**: Added three validation checks before crafting:
  1. **Vis discount**: Applied player's gear discount via
     `CasterManager.getTotalVisDiscount(player)` before checking availability.
  2. **Crystal requirements**: Scans crystal slots (10-15) for required
     aspects via `IEssentiaContainerItem`, checks available count ≥ required.
  3. **Research knowledge**: Checks player has researched the recipe's
     requirement via `ThaumcraftCapabilities.getKnowledge(player)`.
- **ItemGrappleGun**: Now spawns `EntityGrapple` projectile on use instead
  of TODO comment.

All changes build green (`BUILD SUCCESSFUL`). In-game verification pending.

## P1: Golem parts (DARTS arms, SMART heads, advanced seals, modules) — RESOLVED (2026-09-14)

Golems were missing several 1.12 parts. Added:
- **DARTS arms**: `GolemArmDart` — ranged attack spawning `EntityGolemDart`
  projectiles. Registered in `GolemProperties` with `GOLEMRANGED` tag and
  FIGHTER/DEFT/RANGED traits. Build green.
- **SMART_ARMORED head**: Registered in `GolemProperties` with MIND +
  IRON_CHESTPLATE + LEATHER requirements, SMART trait. Build green.
- **SMART_SCOUT head**: Registered in `GolemProperties` with MIND +
  GOLEM_MODULE_AGGRESSION requirements, SCOUT/SMART/FRAGILE traits. Build green.
- **SealEmptyAdvanced**: Advanced seal with 9 filter slots, requires SMART
  trait. Registered in `SealHandler.registerDefaultSeals()`. Build green.
- **ItemGolemModule**: Right-click on golem to add trait (FIGHTER for
  AGGRESSION module, SCOUT for VISION module). Consumes item on success.
  Registered in `ModItems` replacing placeholder items. Build green.

All changes build green (`BUILD SUCCESSFUL`). In-game verification pending.

## Outstanding (runtime verification on a test server)

- [ ] Entity spawning/behaviour, golem seals, bosses
- [ ] Crafting UIs (arcane workbench, crucible, infusion altar, research table)
- [ ] Worldgen (greatwood/silverwood, ores, ruins, taint biome)
- [ ] Visual QA — several renderers use compile-first approximations
      (billboard beams, block-atlas sprite substitutions, banner tint limits)
- [ ] Focus projectile behavior (impact, cloud, mine) — newly wired
- [ ] Arcane workbench crafting with validation — newly wired
- [ ] Golem DARTS arms — newly wired (need in-game test)
- [ ] Golem SMART heads (ARMORED, SCOUT) — newly wired (need in-game test)
- [ ] SealEmptyAdvanced — newly wired (need in-game test)
- [ ] GolemModule item (AGGRESSION/VISION) — newly wired (need in-game test)

## New gaps found in feature-gap audit (2026-09-14)

### Golem system — RESOLVED (2026-09-14)
- **Golem part functions missing** (MEDIUM): RESOLVED — `GolemArmDart` implements `IArmFunction.onRangedAttack()` spawning `EntityGolemDart` projectiles. Registered in `GolemProperties` with FIGHTER/DEFT/RANGED traits.
- **Seal config GUI unwired** (MEDIUM): RESOLVED — Created `SealMenuProvider` (MenuProvider implementation). `ItemGolemBell` now opens `SealMenu` when right-clicking a seal. Both `useOn()` and `use()` paths wired.
- **Golem components use placeholder vanilla items** (MEDIUM): Still open — WOOD→oak_planks, BRASS→gold_ingot, THAUMIUM→diamond, VOID→obsidian, mechanism→clock.

### Taint system — RESOLVED (2026-09-14)
- **Taint spread cycle disconnected** (MEDIUM): RESOLVED — `TaintHelper.isNearTaintSeed` now verifies `EntityTaintSeed` exists (removes stale entries). `spreadFibres` spawns new seeds at edge of influence when flux >= 5. Uses `setPos` + `addFreshEntity` (1.21 API).

### Entity wiring
- **EntityCausalityCollapser↔EntityFluxRift** (MEDIUM): `EntityCausalityCollapser.java:83` — "TODO: Find and collapse nearby flux rifts when EntityFluxRift is implemented" (EntityFluxRift exists — wiring gap).
- **EntityFluxRift wisp spawning** (LOW): `EntityFluxRift.java:380` — wisp spawning TODO.

### Items — RESOLVED (2026-09-14)
- **Vis-discount mundane/fancy gear missing** (MEDIUM): RESOLVED — Ported from 1.12 `ItemBaubles.java`: `ItemMundaneGear` (2% discount), `ItemApprenticeRing` (5% discount), `ItemFancyGear` (3% discount). All 7 items registered in `ModItems.java`, implement `IVisDiscountGear`.
- **ItemFocusPouch Curios integration** (LOW): Still open — `ItemFocusPouch.java:131` — "TODO: Implement Curios integration for belt slot".

## P1: Functional gaps — RESOLVED (2026-09-04)

All three fixed and build-verified (`BUILD SUCCESSFUL`); in-game
verification still outstanding.

Diagnosis was verified against the NeoForge 26.2.0.59 universal jar and
the official NeoForge 26.2.x source before fixing.

### 1. Worldgen dead — biome modifiers never load

- 13 files under `src/main/resources/data/thaumcraft/forge/biome_modifier/`
  (1.20.1 Forge layout), all with `"type": "forge:add_features"`.
- NeoForge 26.2 registers modifier codecs under namespace `neoforge`
  (`NeoForgeMod.MOD_ID = "neoforge"`); `AddFeaturesBiomeModifier` javadoc
  requires `"type": "neoforge:add_features"`; datapack directory is
  `data/<ns>/neoforge/biome_modifier/`.
- Effect: no greatwood/silverwood trees, no TC ores, no crystal clusters,
  no ruined towers / eldritch obelisks / barrows / ancient stone circles,
  no cinderpearl / vishroom — none of the worldgen features ever apply.
- Fix:

  ```bash
  git mv src/main/resources/data/thaumcraft/forge/biome_modifier \
         src/main/resources/data/thaumcraft/neoforge/biome_modifier
  sed -i 's/"type": "forge:add_features"/"type": "neoforge:add_features"/' \
         src/main/resources/data/thaumcraft/neoforge/biome_modifier/*.json
  ```

- Verify: fresh world — forest biomes grow greatwood/silverwood trees;
  TC ore veins appear in stone/deepslate.

### 2. `#forge:` tags dead — recipes uncraftable, cross-mod tag membership invisible

- NeoForge 26.2 ships 593 `data/c/tags/` files and **zero** `data/forge/tags/`;
  the `forge:` namespace is not loaded at all.
- Port has 40 tag definitions under `data/forge/tags/{items,blocks}/` (dead)
  and 26 distinct `#forge:` ingredient refs in recipes (~90 usages) that match
  nothing → those recipes are uncraftable (rods, ingots, gems, dyes, …).
- Fix:
  1. `git mv src/main/resources/data/forge/tags/items src/main/resources/data/c/tags/items`
  2. `git mv src/main/resources/data/forge/tags/blocks src/main/resources/data/c/tags/block`
     (26.2 uses singular `block` for block tags)
  3. Rewrite recipe refs `#forge:X` → `#c:X`. 18 of the 26 exist in the
     convention set: `rods/wooden`, `ingots/{gold,iron}`,
     `gems/{quartz,emerald,diamond}`, `glass_panes`, `dyes/{black,red}`,
     `nuggets/gold`, `dusts/{redstone,glowstone}`, `ores/{iron,gold,copper}`.
  4. Renames: `#forge:leather` (7 refs) → `#c:leathers`; `#forge:glass`
     (2 refs) → `#c:glass_blocks/colorless` (both convention tags verified
     to contain `minecraft:` equivalents in the 26.2 jar). Then drop the
     now-redundant port definitions `data/c/tags/items/{leather,glass}.json`.
  5. Custom tags (not in the convention set) resolve via the port's own
     moved definitions: `string`, `stone`, `trapdoors/wooden`, `slimeballs`,
     `ingots/brass`, `gems/amber`, `workbenches`, `cobblestone`.
- Verify: JEI — arcane-workbench recipes with rod/ingot/gem ingredients are
  craftable; TC ores appear under `c:ores/{iron,gold,copper}`; other mods'
  recipes matching `c:ingots/thaumium` etc. now work.

### 3. Six block classes never create their block entities (machines dead)

Tile classes exist and are implemented; the blocks return `null` with
`// TODO: Return Tile… when implemented`:

| Block (`common/blocks/…`) | Tile(s) to return (`common/tiles/…`) |
|---|---|
| `crafting/BlockInfusionMatrix` | `crafting/TileInfusionMatrix` |
| `essentia/BlockTube` | `essentia/TileTube` + `TileTubeFilter` / `TileTubeBuffer` / `TileTubeValve` / `TileTubeOneway` / `TileTubeRestrict` (pick by tube type) |
| `essentia/BlockAlembic` | `essentia/TileAlembic` |
| `devices/BlockLamp` | `devices/TileLampArcane` / `TileLampFertility` / `TileLampGrowth` (pick by lamp type) |
| `devices/BlockMirror` | `devices/TileMirror` / `TileMirrorEssentia` |
| `devices/BlockStabilizer` | `devices/TileStabilizer` |

Effect: infusion altar, stabilizers, the entire essentia transport network
(tubes/valves/filters/buffers), alembic, lamps, mirrors — none function.
Copy the 26.2 constructor pattern from a working block, e.g. `BlockCondenser`:
`new TileCondenser(ModBlockEntities.CONDENSER.get(), pos, state)` — 26.2
`BlockEntity` constructors take the `BlockEntityType` first.

## Suggested order for the next session

1. In-game QA of the P1 fixes — fresh world: greatwood/silverwood trees +
   TC ores generate; JEI shows the rod/ingot/gem recipes craftable;
   infusion altar + stabilizers craft; a tube network moves essentia;
   alembic/lamps/mirrors/stabilizer tiles function
2. Re-run the Outstanding list — entity spawning/behaviour, crafting UIs,
   taint biome
3. Full feature-gap audit vs the 1.20.1 reference — a subagent pass was
   aborted before writing its report (partial transcript:
   `history://GapAudit`; reference tarball may still be at `/tmp/refrepo`)

## Verified (2026-09-04)

- Fresh-world server boots to "Done"; aura scheduler ticks continuously
  (moon phase now read via the 26.2 positional environment-attribute API)
- Initial chunk save completes for all 3 dimensions (no OOM)
- Research data loads: **148 entries / 8 categories** (alchemy 22, eldritch 5,
  basics 20, auromancy 23, scans 12, artifice 20, infusion 17, golemancy 29)
- Tag-based aspect registration runs without errors — 26.2 changed
  `Identifier.withDefaultNamespace` to path-only; all id call sites now use
  `Identifier.parse`
- **Spawn categories fixed**: `minecraft:bat` (CREATURE→PASSIVE) and
  `thaumcraft:pech` (MONSTER→CREATURE) now register in the correct
  `SpawnPlacements` buckets — zero "Detected … wrong category" warnings
- **Research icon mappings added**: `minecraft:noteblock→note_block`,
  `minecraft:web→cobweb`, `thaumcraft:brain→brain_normal`,
  `thaumcraft:biothaumic_mind→brain_curious`, `thaumcraft:leather→
  minecraft:leather` (all verified against the 1.20.1 reference data)
- **Aspect + smelting-bonus data wired**: `CommonInternals.initAspects()`
  (101 vanilla-item aspects) and `initSmeltingBonuses()` now run on
  `ServerStartingEvent`; `TileInfernalFurnace.getSmeltingBonus` delegates to
  `CommonInternals` (bellows bonus drops functional)

## Known issues & environment

- **`CI=true` required for local runs**: without it, NeoGradle takes the
  local decompile→patch→recompile pipeline, which fails at `neoFormPatch`
  (decompiler emits `var1` parameter names; the patch expects named
  params). Some shells already export `CI`; otherwise prefix runs with
  `CI=true ./gradlew …`.
- **Dev-server console**: `stop` typed into the `runServer` console is not
  forwarded to the server process — stop with SIGINT/Ctrl+C.

## Known build warnings

- **Resolved (2026-09-04)**: JEI compat layer migrated off the JEI 30.x
  removal-marked API — `RecipeType` → `mezz.jei.api.recipe.types.IRecipeType`,
  `IIngredientAcceptor.addIngredients/addItemStack` → `.add(...)`,
  `addRecipeCatalyst` → `addCraftingStation`. Zero `compat/jei` warnings.
- **Deferred project — NeoForge handler API migration (125 warnings)**:
  the entire `net.neoforged.neoforge.items` package
  (`IItemHandler`, `IItemHandlerModifiable`, `ItemStackHandler`,
  `ItemHandlerHelper`) and `IFluidHandler`/`FluidTank` are
  `@Deprecated(since="1.21.9", forRemoval=true)`. The replacement is the
  transaction-based `net.neoforged.neoforge.transfer` API
  (`ResourceHandler<T extends Resource>`, `ItemAccess`,
  `transfer.item`/`transfer.fluid`/`transfer.energy`).
  - Still fully functional at runtime in 26.2 (vanilla itself still uses the
    `Container` model), so this is a planned rewrite, not an urgency.
  - Affected files: `TilePatternCrafter`, `SealProvide`, `SealEmpty`,
    `SealFill`, `SealStock`, `BlockCrucible`, `ThaumcraftInvHelper`,
    `InventoryUtils`, `LogisticsMenu`, `TileWaterJug`, `TileCrucible`,
    `BlockWaterJug`, others.
  - Plan: dedicated pass migrating to `StacksResourceHandler`/`ItemAccess`
    + in-game testing of every machine (item + fluid transfer, serialization,
    capability exposure).
