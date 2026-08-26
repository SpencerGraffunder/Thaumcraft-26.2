# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Port

Port of **Thaumcraft 6** from the 1.20.1 Forge source fork
([ShobieShy/Thaumcraft-6-Source-Code-1.20.1](https://github.com/ShobieShy/Thaumcraft-6-Source-Code-1.20.1))
to **Minecraft 26.2 "Chaos Cubed"** on **NeoForge 26.2.0.59** (Java 25).

## Status: COMPILES & BUILDS — server boots to "Done" — runtime testing in progress

The 26.2 port is under active development.

- `./gradlew compileJava` — **GREEN (0 errors)**
- `./gradlew build` — **SUCCESS** → `build/libs/thaumcraft-6.2.0+26.2.jar`
- `./gradlew runServer` — gets through mod construction and block registration;
  item registration now id-injected via `ItemRegistration` ThreadLocal;
  **server reaches `Done (2.4s)`** — world loads, aura threads run per dimension,
  golem parts/seals/research/aspect/multiblock init run on `ServerStartingEvent`
  (they construct vanilla `ItemStack`s, illegal in commonSetup on 26.2)
- Recipe data migrated to the 26.2 format: `key` ingredients are plain strings
  and `result` uses `"id"` (not `"item"`); forge + thaumcraft item tags added
  for cross-mod interop.

**Completed waves**

- Build pipeline: NeoGradle 7.1 userdev, Java 25, Gradle 8.14.5
- Mechanical renames (`Identifier`, `DeferredHolder`, `ToolMaterial`/`ArmorMaterial`
  records, `EntitySpawnReason`, `InteractionResult`, `EnumProperty<Direction>`)
- Armor/tools on the 26.2 component API (`humanoidArmor`, `.pickaxe()/.sword()`)
- BlockEntity sync on the 26.2 ValueIO API (`ValueOutput`/`ValueInput`)
- Entity/tile renderers, screens, widgets → render-state model
  (`EntityRenderer<T,S>`, `BlockEntityRenderer<T,S>`, `GuiGraphicsExtractor`,
  `SubmitNodeCollector`/`submitCustomGeometry`, `RenderTypes.*`)
- Recipe serializers → `MapCodec` + `StreamCodec` records
- ItemStack NBT → `DataComponents.CUSTOM_DATA` / `CustomData`
- NeoForge capabilities → 26.2 (`Capabilities.Item/Fluid/Energy.BLOCK`,
  `IItemHandler.of(ResourceHandler)`); player knowledge/warp as DataAttachments
- Entity API updates (`hurtServer`/`hurtClient`, `finalizeSpawn(EntitySpawnReason)`,
  `EntityReference`, GameRules rework, `MobEffect` tick API, `PotionContents`)
- Block API (`codec()` overrides, `noCollision()`, `isSolidRender()`, removed
  `onRemove`/`isValidRepairItem`/`PlantType`/`MobType`)
- Networking on the NeoForge payload system
- Mod metadata for 26.2: `META-INF/neoforge.mods.toml` (not `mods.toml`),
  `loaderVersion=[11,)`, dependency `type=REQUIRED/OPTIONAL`
- Server-safe common code (removed direct client-class references), block
  registration via id-injecting `DeferredRegister.createBlocks` +
  `BlockRegistration` ThreadLocal helper

**Remaining (runtime testing on a test server)**

- **In-world runtime testing** — entity spawning/behaviour, golem seals, bosses;
  a dedicated server boots clean but gameplay paths are unexercised
- Crafting UIs (arcane workbench, crucible, infusion altar, research table)
- Worldgen (greatwood/silverwood, ores, ruins, taint biome)
- Visual QA — several renderers use compile-first approximations
  (billboard beams, atlas-sprite substitutions, banner tint limits)
- Data migration — some recipe JSONs still carry old-style `"nbt"` that the
  26.2 codecs ignore (recipes load, NBT dropped)
- Dev-environment note: `run/server.properties` sets `max-tick-time=300000`
  (first-boot world saves exceed the 60s default watchdog)

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
