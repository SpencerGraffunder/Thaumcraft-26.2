# Thaumcraft 6 — Minecraft 26.3 (NeoForge) Port

Port of **Thaumcraft 6** from the 1.20.1 Forge source fork
([ShobieShy/Thaumcraft-6-Source-Code-1.20.1](https://github.com/ShobieShy/Thaumcraft-6-Source-Code-1.20.1))
to **Minecraft 26.3** on **NeoForge 26.3.0.33-beta** (Java 25).

> The port was developed and verified on 26.2 ("Chaos Cubed", NeoForge
> 26.2.0.76) and migrated to 26.3 on 2026-09-28. The 26.2 notes below are
> historical — the build now targets 26.3 end-to-end (gradle.properties,
> mods.toml template, decompiled-source pipeline, CI).

## Status: 26.3 migration — build green, installed, pending in-game verification

- `./gradlew build` — **SUCCESS** → `build/libs/thaumcraft-6.2.0+26.3.jar`
  (86/86 tests green, 0 TODOs left in source).
- Full neoForm decompile→patch→recompile pipeline runs inside the build
  (see `build.gradle`); `scripts/fix_neoform_artifacts.py` repairs
  decompiler artifacts (26.3-specific fixes for `CustomPayload` codec call
  sites and `LevelEventHandler` case-2003 locals).
- **`runServer` smoke test: boots to `Done`** on a fresh 26.3 world — zero
  registry/worldgen errors, clean world save, research (148 entries) + runtime
  registration complete. This surfaced & fixed 3 runtime-only data issues the
  compile never caught:
  - **Curios dependency range:** `mods.toml` had `[17.0.0,)` but the 26.3 build
    ships as `17.0.0-beta.2+26.3`. NeoForge parses ranges with Maven's
    `VersionRange`, which sorts a pre-release *below* its release, so the beta
    was rejected at mod-load. Lowered to `[17.0.0-beta,)` (accepts the beta, a
    future stable 17.0.0, and 17.x; still rejects 16.x).
  - **Ore feature `state` format:** `BlockState.CODEC` in 26.3 accepts a plain
    string or a map with an `id` key — the old `{"Name": "..."}` block-state
    object is gone. All 3 ore features (`ore_amber`/`ore_cinnabar`/`ore_quartz`)
    now use plain-string `state` (e.g. `"thaumcraft:amber_ore"`).
  - **`crystals` item tag directory:** was in the legacy `data/thaumcraft/tags/items/`
    (plural); 26.3 (like every other tag in the mod) uses `data/thaumcraft/tags/item/`
    (singular). Moved it — the Salis Mundus recipe's `#thaumcraft:crystals`
    ingredient now resolves.
- Jar installed into the Modrinth **`NeoForge 26.3`** profile
  (NeoForge 26.3.0.33-beta). **The profile also needs
  `curios-neoforge-17.0.0-beta.2+26.3.jar`** (hard dependency, was not
  installed in the fresh profile). JEI is optional (31.7.0.47).
- **26.3 transfer-API migration:** NeoForge removed `IItemHandler` /
  `IFluidHandler`/`FluidTank` entirely; the new transaction-based
  `net.neoforged.neoforge.transfer.ResourceHandler<T>` framework replaced
  them. Thaumcraft now uses `ResourceHandler<ItemResource>` / `ResourceHandler<FluidResource>`
  everywhere, with two thin adapters restoring the old call shapes:
  `thaumcraft.api.ItemHandlers` (insert/extract/set with simulate flags) and
  `thaumcraft.api.FluidTanks` (fill/drain/getAmount). Curios 17-beta's
  `IDynamicStackHandler` was also re-based on `ResourceHandler`, so the
  focus pouch (`PouchCurios`) now extends `ItemStacksResourceHandler`.
- **Other 26.3 API breaks fixed in this pass:** `Feature` is a plain
  interface (no `ConfiguredFeature`/`PlacedFeature` classes — configured
  features moved to `data/<ns>/worldgen/feature/*.json`, 16 custom feature
  types registered in `Registries.FEATURE_TYPE`); tool `Item` subclasses
  (`AxeItem` etc.) replaced by `Item.Properties.axe/shovel/hoe/pickaxe(...)`
  builder methods; `PoseStack.mulPose(Quaternionf)` → `rotate`/`rotateDegrees`;
  `FuelValues` removed → fuel burn time read from
  `DataComponents.COOKING_FUEL` (`TileSmelter`); arm-swing sync fields
  removed → `swing(hand, SwingAnimation.DEFAULT, true)`;
  `Entity.drop` moved to `LivingEntity` with a `Prediction` param;
  `submitModel`/`submitModelPart` lost the `breakProgress` argument;
  `PushReaction.DESTROY`/`BLOCK` → `POPPED`/`IMMOVEABLE`;
  `blocksMotion()` → `isCollisionShapeFullBlock(level, pos)`;
  `hurtMarked` → `needsSync` (now public); `KeyMapping` moved to
  `net.minecraft.client` with `InputConstants.isKeyDown(int)` single-arg.
- `./gradlew runServer` — gets through mod construction and block registration
  (id-injected via `BlockRegistration` ThreadLocal helper); **server reaches
  `Done`** — world loads, aura threads run per dimension, golem parts/seals/
  research/aspect/multiblock init run on `ServerStartingEvent` (constructing
  vanilla `ItemStack`s, illegal in commonSetup on 26.2)
- Recipe data migrated to the 26.2 format: `key` ingredients are plain strings
  and `result` uses `"id"` (not `"item"`); forge + thaumcraft item tags added
  for cross-mod interop.
- **World-load abort ("Invalid data pack") & first-craft NPEs fixed (2026-09-09, macOS):**
  26.2 decodes recipes in the parallel prepare phase *before* tags bind, and it
  validates tag contents strictly — a `c:ingots/brass` tag pointing at a
  non-existent item ID aborted every world load. Recipe codecs now follow the
  26.2 rules: `create`/decode keep pure null/`Optional` logic (no
  `isEmpty()`/`getStacks()`; 26.2 also removed `Ingredient.EMPTY`), shaped
  arcane recipes use vanilla's `List<Optional<Ingredient>>` slot pattern with
  `Ingredient.testOptionalIngredient`, infusion accepts both `center` and
  `input` keys, and tag item IDs are validated against the registry
  (`brass_ingot`, not `ingot_brass`). Verified: `CI=true ./gradlew runServer`
  boots to `Done` with every recipe type decoded. Rules documented in
  `skills/minecraft-gui/SKILL.md` (Troubleshooting).
- Multiplayer fixed & verified: S2C payload handlers split into
  server-safe common classes + client handler classes
  (`thaumcraft.client.lib.network.*`), so all 38 `thaumcraft:packet*`
  channels register on both dists. A 26.2.0.75 client joins a 26.2.0.75
  dev server cleanly — NeoForge handshake passes with zero channel errors,
  player in-world (see [`TODO.md`](./TODO.md) P0 for details).
- **Multiplayer verified on macOS (Spencers-MacBook-Air, 2026-09-06):** dedicated
  server booted from the Modrinth profile (26.2.0.76, `Done` in ~8s) + client join
  via Multiplayer → Direct Connect — clean handshake, in-world play, Thaumcraft
  item give / block place / wand interactions, **zero exceptions in both logs**
  (full workflow in `skills/minecraft-gui/SKILL.md`).
- **Purple/black items fixed & verified:** the 1.20.1→26.2 port had dropped the
  per-item `models/item/<id>.json` ClientItem files, so 362 items rendered as the
  missing-texture checkerboard. All 362 ClientItem files are now generated, 25
  model texture refs re-pointed to existing textures, and 4 placeholder textures
  added — `runClient` now logs **0 "Missing item model for"** (items render
  correctly; see [`TEXTURETODO.md`](./TEXTURETODO.md)).
- **Broken item textures fixed & verified:** 69 textures that shipped as
  placeholder checkerboards (candles, elemental shards, casters, golem
  pearls/charms, void jars, arcane workbench, taint mob art, …) are now
  replaced with the real Thaumcraft art from the released TC4/TC5/TC6 jars,
  and the dangling `bucket_pure` ref is fixed. A new
  `TextureIntegrityTest` guards against regressions (dangling model refs +
  placeholder checkerboards). Rebuilt jar shows **0 placeholder textures**;
  full test suite **86 green** (see [`TODO.md`](./TODO.md) P0).
- **Thaumonomicon 1.12 parity (rounds 1–5, 2026-09-11/12):** recipe popups
  rewritten to the 1.12 overlay look, bookmarks/ribbon positions, 1.0x text
  scale at 140px wrap width, 1.12 completion semantics (pre/post stage text),
  requirement rows at the book bottom, 3D infusion-matrix + altar popups, 16
  colored nitor items rendered 1.12-style (color overlay over the core), and
  the "new research" gold-star markers (fixed: knowledge sync after research
  completion was dead, so flags/toasts never reached the client; added the
  missing category-sidebar stars).
  All 253 book recipe references resolve; see [`TODO.md`](./TODO.md).
- **Golem-parts client crash fixed & verified (macOS, 2026-09-07):** golem
  parts/seals/research were registered only on `ServerStartingEvent`, which a
  plain (integrated / Modrinth) client never sees — so opening the creative
  inventory NPE'd (`mat is null`). Runtime registration is now an idempotent
  `bootstrap()` with a component-binding readiness probe, driven from both
  `ServerStartingEvent` and `ClientTickEvent.Pre`. Verified: creative
  inventory opens clean, all 15 golem items render with real art, log shows
  `Registered golem parts` … `Thaumcraft runtime registration complete`.

Detailed task tracking lives in [`todo.md`](./todo.md)[`todo.md`](./todo.md] — 
checklists and remaining runtime-testing items are maintained there, not here.
This README covers build/run/deploy status only.

**Known-good reference:** the unmodified 1.20.1 Forge build produces
`thaumcraft-6.2.0.jar` and loads into a world — that's the behavior baseline.

## Building

Requires JDK 25.

- Ubuntu 26.04: `sudo apt-get install openjdk-25-jdk-headless` puts JDK 25 on
  the `PATH` — no `JAVA_HOME` needed.
- Otherwise: `export JAVA_HOME=/path/to/jdk-25` before running Gradle.
- macOS: `brew install openjdk@25` (keg-only — set
  `JAVA_HOME=/opt/homebrew/opt/openjdk@25`). If the repo checkout is on an SMB share,
  Gradle fails (`FileHasher: Operation not supported`) — copy the repo to local disk
  and build there. Full macOS build/launch/screenshot/click workflow is in
  [`skills/minecraft-gui/SKILL.md`](./skills/minecraft-gui/SKILL.md).

```bash
./gradlew build
```

Output jar lands in `build/libs/`. GitHub Actions CI (`.github/workflows/build.yml`)
builds on every push with JDK 25 and uploads the jar as an artifact.

## Running

```bash
CI=true ./gradlew runClient   # client
CI=true ./gradlew runServer   # dedicated server
CI=true ./gradlew runGameTestServer
```

Dev note: `run/server.properties` sets `max-tick-time=300000` (first-boot world
saves exceed the 60s default watchdog).

Dev note: the build runs the local neoForm decompile→patch→recompile pipeline
on demand; `neoFormPatchUserDev` may reject non-critical hunks, which
`build.gradle` tolerates (the recompiled jar is guarded so a valid existing
jar is never clobbered). The `runServer` console does not forward a typed
`stop` to the server process — stop it with Ctrl+C/SIGINT.

## Deployment

`deploy.sh` copies a built jar into a configurable mods folder for test-server
deployment (target: a Crafty Controller MC server on the same Unraid host).

## License

See upstream — Thaumcraft is the property of Azanor / original authors.
