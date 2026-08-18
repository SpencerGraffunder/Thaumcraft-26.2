# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Port

Port of **Thaumcraft 6** from the 1.20.1 Forge source fork
([ShobieShy/Thaumcraft-6-Source-Code-1.20.1](https://github.com/ShobieShy/Thaumcraft-6-Source-Code-1.20.1))
to **Minecraft 26.2 "Chaos Cubed"** on **NeoForge 26.2.0.59** (Java 25).

## Status: IN PROGRESS — compilation nearly green

The 26.2 port is under active development on branch `port/26.2`.

**What's done**

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
- ItemStack NBT → DataComponents migration in progress
- Level `random` access → `getRandom()`

**Remaining work**

- Fix the last handful of compile errors (syntax leftovers in FX/entity/worldgen files)
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
