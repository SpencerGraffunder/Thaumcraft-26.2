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

**Current wave — BlockEntity sync NBT → ValueIO (in progress)**

The `writeSyncNBT(CompoundTag)` / `readSyncNBT(CompoundTag)` sync chain (cascading
from `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)`) is being
converted to `ValueOutput` / `ValueInput`. The API surface is fully mapped
(`ValueOutput`/`ValueInput` interfaces, `TagValueOutput`, MC `ContainerHelper`,
`ItemStack` codecs, NeoForge `ValueIOExtension`s, `FluidTank` as
`ValueIOSerializable`). Progress:

- ✅ `AspectList` now has `ValueIO` overloads of `writeToNBT`/`readFromNBT`
  alongside the legacy `CompoundTag` ones
- 🔄 Base `TileThaumcraft` is mid-transition — `saveAdditional`/`loadAdditional`
  call the `ValueIO` variants but the `writeSyncNBT`/`readSyncNBT` overloads are
  still `CompoundTag`-typed (compile is intentionally red here until the wave lands)
- ⏳ All 36 tile subclasses still declare the `CompoundTag`-typed overloads
  (migration script + hand-fixes queued, see `TODO.md` → 26.2 section)

**Remaining work**

- Finish the sync ValueIO wave: flip the 36 `writeSyncNBT`/`readSyncNBT` bodies
  to `ValueOutput`/`ValueInput`, migrate special cases (byte[] grids, focus-node
  list, `ResearchTableData`/`FocusElementNode`, `ItemStack` codecs), fix
  `TileBanner`/`getUpdateTag(HolderLookup.Provider)` signature
- Wave 2 — remove the old `ItemStack` CompoundTag API (`hasTag`/`setTag`/
  `getOrCreateTag`/`ItemStack.of`) across ~50 files in favor of the 26.2 codecs
- Port GUI screens to the new 26.2 render API (`GuiGraphics` → `GuiGraphicsExtractor`)
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
