# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Migration Status

> Everything below this header tracks the **26.2 NeoForge port** (NeoForge
> 26.2.0.59, Java 25). The historical 1.20.1 plan is preserved further down.

## 26.2 Status: COMPILES & BUILDS — server boots to "Done" — runtime testing in progress

`./gradlew compileJava` is GREEN (0 errors); `./gradlew build` produces
`thaumcraft-6.2.0+26.2.jar`. `runServer` reaches **Done (2.4s)** — world loads,
aura threads run per dimension, and golem parts/seals/research/aspect/
multiblock init run on `ServerStartingEvent` (they build vanilla `ItemStack`s,
illegal in commonSetup on 26.2). Item registration is id-injected via
`ItemRegistration` ThreadLocal.

### Completed

- [x] Build pipeline: NeoGradle 7.1 userdev, Java 25, Gradle 8.14.5
- [x] Mechanical renames: `Identifier`, `DeferredHolder`, `ToolMaterial`/
      `ArmorMaterial` records, `EntitySpawnReason`, `InteractionResult`,
      `EnumProperty<Direction>`, `getRandom()`
- [x] Armor/tools on 26.2 component API (`humanoidArmor`, `.pickaxe()/.sword()`)
- [x] Particles + FX on render-state model (`SingleQuadParticle.extract()`,
      `QuadParticleRenderState`; beams/arcs/bolts and all FX ported)
- [x] BlockEntity sync → ValueIO (`ValueOutput`/`ValueInput`)
- [x] Entity renderers (~36), tile renderers (~23), screens (~19) and widgets →
      `EntityRenderer<T,S>` / `BlockEntityRenderer<T,S>` / `GuiGraphicsExtractor`
      with `SubmitNodeCollector.submitCustomGeometry` and `RenderTypes.*`
- [x] Recipe serializers → `MapCodec` + `StreamCodec` records
- [x] ItemStack NBT → `DataComponents.CUSTOM_DATA` / `CustomData`
- [x] NeoForge capabilities → 26.2 (`Capabilities.Item/Fluid/Energy.BLOCK`,
      `IItemHandler.of(ResourceHandler)`); player knowledge/warp as DataAttachments
- [x] Entities → `hurtServer`/`hurtClient`, `finalizeSpawn(EntitySpawnReason)`,
      `getDefaultGravity`, `EntityReference`, GameRules rework, `MobEffect`
      tick API, `PotionContents`
- [x] Block API → `codec()` overrides, `noCollision()`, `isSolidRender()`,
      removed `onRemove`/`isValidRepairItem`/`PlantType`/`MobType`
- [x] Networking → NeoForge payload system
- [x] 26.2 mod metadata: `META-INF/neoforge.mods.toml`, `loaderVersion=[11,)`,
      dep `type=REQUIRED/OPTIONAL`; removed legacy mcmod.info
- [x] Server-safe common code (no direct client-class refs in common classes)
- [x] Block registration id-injection: `DeferredRegister.createBlocks` +
      `thaumcraft.init.BlockRegistration` ThreadLocal; `Properties.setId` in
      every block constructor before `super(...)`
- [x] Recipe codecs: empty `Ingredient` defaults → `Optional<Ingredient>`
      (avoids the "Ingredients can't be empty" class-init crash)
- [x] Item registration id-injection: `ItemRegistration` ThreadLocal around
      every item `Properties` before `super(...)` (same pattern as blocks)
- [x] Recipe data → 26.2 format: `key` ingredients as plain strings, `result`
      uses `"id"` not `"item"`; forge + thaumcraft item tags added
- [x] Data migration — all recipe JSONs audited; zero old-style `"nbt"` fields
      remain (450/450 parse; `result` uses `"id"`, keys plain strings)

### Remaining (runtime testing on a test server)

- [ ] Entity spawning/behaviour, golem seals, bosses
- [ ] Crafting UIs (arcane workbench, crucible, infusion altar, research table)
- [ ] Worldgen (greatwood/silverwood, ores, ruins, taint biome)
- [ ] Visual QA — several renderers use compile-first approximations
      (billboard beams, block-atlas sprite substitutions, banner tint limits)
