# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Port

Port of **Thaumcraft 6** from the 1.20.1 Forge source fork
([ShobieShy/Thaumcraft-6-Source-Code-1.20.1](https://github.com/ShobieShy/Thaumcraft-6-Source-Code-1.20.1))
to **Minecraft 26.2 "Chaos Cubed"** on **NeoForge 26.2.0.59** (Java 25).

## Status: COMPILES & BUILDS — `./gradlew build` produces `thaumcraft-6.2.0+26.2.jar`

The 26.2 port is under active development on branch `master`. As of this update
`./gradlew compileJava` is GREEN (0 errors) and `./gradlew build` succeeds.

**Completed waves**

- Build pipeline migrated to NeoGradle 7.1 userdev (Java 25, Gradle 8.14.5 wrapper)
- Mechanical migration of the full source tree (`Identifier`, `DeferredHolder`,
  `ToolMaterial`/`ArmorMaterial` records, `EntitySpawnReason`, `InteractionResult`,
  `EnumProperty<Direction>`)
- Armor/tools on the 26.2 component API (`humanoidArmor(material, ArmorType)`,
  `.pickaxe()`/`.sword()` properties; `ArmorItem`/`PickaxeItem`/`DiggerItem` removed)
- Particle system on the render-state model (`SingleQuadParticle.extract()` +
  `QuadParticleRenderState`); FX beams/arcs/bolts and all custom particles ported
- BlockEntity sync on the 26.2 ValueIO API (`ValueOutput`/`ValueInput`)
- **Entity/tile renderers, screens, and GUI ported to the render-state model**
  (`EntityRenderer<T, S>`, `BlockEntityRenderer<T, S>`, `GuiGraphicsExtractor`,
  `SubmitNodeCollector`/`submitCustomGeometry`, `RenderTypes.*`)
- **Recipe serializers rewritten as codecs** (`RecipeSerializer` record with
  `MapCodec` + `StreamCodec` for arcane/crucible/infusion/enchantment recipes)
- **ItemStack NBT → DataComponents** (`DataComponents.CUSTOM_DATA`/`CustomData`,
  `isSameItemSameComponents`, codecs)
- **NeoForge capabilities → 26.2** (`Capabilities.Item/Fluid/Energy.BLOCK`,
  `IItemHandler.of(ResourceHandler)`, player knowledge/warp as DataAttachments)
- Entity API updates (`hurtServer`/`hurtClient`, `finalizeSpawn(EntitySpawnReason)`,
  `getDefaultGravity`, `EntityReference`, GameRules rework, etc.)
- Networking on the NeoForge payload system (`CustomPacketPayload` +
  `PayloadRegistrar`)

**Current status — compiles & builds; runtime testing in progress**

- `./gradlew compileJava` — GREEN
- `./gradlew build` — SUCCESS (jar: `build/libs/thaumcraft-6.2.0+26.2.jar`)
- Remaining: full in-game testing on a test server (entity spawns, crafting UIs,
  infusion/crucible logic, worldgen); several rendering approximations are
  compile-first (billboard beams, atlas-sprite substitutions) and some JSON
  recipes still carry old-style `"nbt"` that the 26.2 codecs ignore

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
