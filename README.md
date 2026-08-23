# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Port

Port of **Thaumcraft 6** from the 1.20.1 Forge source fork
([ShobieShy/Thaumcraft-6-Source-Code-1.20.1](https://github.com/ShobieShy/Thaumcraft-6-Source-Code-1.20.1))
to **Minecraft 26.2 "Chaos Cubed"** on **NeoForge 26.2.0.59** (Java 25).

## Status: IN PROGRESS — ValueIO serialization migration

The 26.2 port is under active development on branch `master` (the working tree
currently carries ~340 modified files across multiple migration waves).

**Completed waves**

- Build pipeline migrated to NeoGradle 7.1 userdev (Java 25, Gradle 8.14.5 wrapper)
- Mechanical migration of the full source tree:
  - `ResourceLocation` → `Identifier`
  - `RegistryObject` → `DeferredHolder<R, T>`
  - `net.minecraftforge.api.distmarker` → `net.neoforged.api.distmarker`
  - `Tier` → `ToolMaterial` (record), `ArmorMaterial` → `world.item.equipment` records
  - `MobSpawnType` → `EntitySpawnReason`
  - `InteractionResultHolder` → `InteractionResult`
  - `DirectionProperty` → `EnumProperty<Direction>`
- Armor system ported to the 26.2 component API: all armor items now use
  `Item.Properties().humanoidArmor(material, ArmorType)`; pickaxes/swords use
  `.pickaxe()` / `.sword()` properties (no more `ArmorItem`/`PickaxeItem`/`DiggerItem` subclasses)
- Particle system ported to the new render-state model
  (`SingleQuadParticle` + `extract()`), custom `ThaumcraftParticle` base class
- Entity save/load migrated to the 26.2 ValueIO API
  (`saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)`)
- Level `random` access → `getRandom()`

**Current wave — BlockEntity sync NBT → ValueIO (COMPLETE)**

The `writeSyncNBT(CompoundTag)` / `readSyncNBT(CompoundTag)` sync chain (cascading
from `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)`) has been fully
converted to `ValueOutput` / `ValueInput`:

- ✅ `AspectList` ValueIO overloads of `writeToNBT`/`readFromNBT` (added earlier)
- ✅ Base `TileThaumcraft` — `writeSyncNBT(ValueOutput)` / `readSyncNBT(ValueInput)`
  decls, `getUpdateTag(HolderLookup.Provider)` → `saveWithoutMetadata(registries)`
- ✅ All 36 tile subclasses migrated (primitive ops, `AspectList` stores,
  `FluidTank` → `putChild`/`readChild`, `ItemStack` → `OPTIONAL_CODEC`,
  byte[] grids → `putIntArray`, focus-node list via `CompoundTag.CODEC.listOf()`,
  `contains` → `keySet().contains`, `getShortOr` returns `int` → `(short)` casts)
- ✅ `TileBanner`/`TileSmelter` `getUpdateTag`/serialization fixed
- ✅ Pre-existing `loadAdditional` leftovers in `TileMemory`/`TileMirror`/
  `TileInfusionMatrix`/`TileWaterJug` migrated to codecs

**Current status — compilation still red (~3.8k errors)**

Compile errors are down from ~4.1k and now concentrate in a few deep 26.2
subsystem rewrites (each tracked in `TODO.md` → 26.2 section):

- **Render/GUI subsystem (~1.2k errors)** — the 26.2 render-state model:
  `GuiGraphics` → `GuiGraphicsExtractor`, `MultiBufferSource`/`VertexConsumer.vertex`
  → new submit-node pipeline, `EntityRenderer<T, S extends EntityRenderState>`
  with `extractRenderState`/`submit` replacing `getTextureLocation`/`render`
  (~30 entity + tile renderers, FX beams, screens)
- **Wave 2 — ItemStack old NBT removal (~340 errors)** — `hasTag`/`getTag`/
  `getOrCreateTag`/`ItemStack.of` across ~50 files → `DataComponents`/codecs
- **NeoForge capabilities rework** — `LazyOptional`/`Capability`/
  `ForgeCapabilities`/`getCapability`/`invalidateCaps` are gone in 26.2
- **Recipe codec rewrite** — `RecipeSerializer` is now a record
  (`MapCodec` + `StreamCodec`); old `fromJson`/`fromNetwork` serializers obsolete
- **Block API** — `Block.onRemove` removed; `updateShape`/`isSolidRender`/
  `isValidRepairItem`/`getItems`/`displayClientMessage` signatures changed
- Networking to the NeoForge payload system
- Full in-game testing on a test server

**Known-good reference:** the unmodified 1.20.1 Forge build produces
`thaumcraft-6.2.0.jar` and loads into a world — that's the behavior baseline
for this port.

## Building

Requires JDK 25.

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew build
```

Output jar lands in `build/libs/`. GitHub Actions CI (`.github/workflows/build.yml`)
builds on every push with JDK 25 and uploads the jar as an artifact.

## Running

```bash
./gradlew runClient   # client
./gradlew runServer   # dedicated server
./gradlew runGameTestServer
```

## Deployment

`deploy.sh` copies a built jar into a configurable mods folder for test-server
deployment (target: a Crafty Controller Minecraft server on the same Unraid host).

## License

See upstream — Thaumcraft is the property of Azanor / original authors.
