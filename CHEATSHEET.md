# Thaumcraft 26.2 — Compile-Fix API Cheat-Sheet

**Goal**: fix ALL compile errors in `src/main/java/thaumcraft/**`. Vanilla recompile is GREEN.
**RULES**:
1. NO gradle runs (no `./gradlew` at all — the orchestrator compiles). Edit files only.
2. No TODOs, no stubs, no comment placeholders. Behavior-preserving ports.
3. Exact signatures MUST be verified by grepping the decompiled vanilla source below — do not guess.
4. Keep Thaumcraft's public API stable where other TC classes use it.

## Decompiled vanilla source (THE API REFERENCE)
```
T=/Users/spencer/Documents/Thaumcraft-26.2/build/neoForm/neoFormJoined26.2-2/steps/transformSource/transformed
grep -rn "methodName" $T/net/minecraft/...   # find exact signatures
```

## Core renames (1.12/old-port → 26.2)
| Old | New |
|---|---|
| `ResourceLocation` | `net.minecraft.resources.Identifier` — factories: `Identifier.parse("ns:path")`, `Identifier.fromNamespaceAndPath(ns, path)`, `Identifier.withDefaultNamespace(path)` |
| `RegistryKeys.X` / `Registries.X.location("a")` | `ResourceKey.create(Registries.X, Identifier.parse("ns:a"))` — `ResourceKey.create(ResourceKey<? extends Registry<T>>, Identifier)` |
| `entity.serverLevel()` / `getLevel()` | `entity.level()` (returns Level) |
| `isDeadOrDropped()` | `isRemoved()` |
| `player.addCooldown(item, n)` | `player.getCooldowns().addCooldown(ItemStack, int)` or `addCooldown(Identifier, int)` (net.minecraft.world.item.ItemCooldowns) |
| `level.sendParticles(type, x,y,z, count, dx,dy,dz, speed)` | NO count variant. Loop N× `level.addParticle((ParticleOptions) ParticleTypes.X, x, y, z, dx, dy, dz)` (add jitter per particle). ClientLevel also has it. |
| `level.addParticle` (old 1.12 style) | `level.addParticle(ParticleOptions, double x, y, z, double xd, yd, zd)` |
| `state.getMapColor(level, pos)` | `state.getMapColor(state, level, pos, MapColor.NONE)` — level must be LevelReader (cast if param is BlockGetter) |
| `block.getCloneItemStack(level, pos, includeData)` | 4-arg `getCloneItemStack(LevelReader, BlockPos, BlockState, boolean)`; state-level 3-arg: `state.getCloneItemStack(level, pos, includeData)` |
| `LivingEntity.drop(itemStack)` | `drop(ItemStack, boolean randomly, boolean thrownFromHand)` → returns ItemEntity |
| `MenuProvider` (net.minecraft.world.container) | `net.minecraft.world.MenuProvider` (no generics): `getDisplayName()` + `createMenu(int, Player)` from MenuConstructor |
| `AbstractContainerMenu<T>` generics | `AbstractContainerMenu` (no type param) |
| `Component.location(...)` style | verify per-usage; Component API: `Component.translatable(key)`, `Component.literal(str)` |
| `RegistryAccess` passed where `Inventory` expected | fix the call site to pass the right object |
| `Holder<Enchantment>` vs `ResourceKey<Enchantment>` | use `ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse("minecraft:x"))`; to get Holder: `registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getResource(key)` |

## Thaumcraft-internal APIs
- Player capabilities: `ThaumcraftCapabilities.getKnowledge(player)` → `IPlayerKnowledge` (has `isResearchKnown(String)`, `getResearchKnownList()` etc.); `getWarp(player)` → `IPlayerWarp`.
- Research: `thaumcraft.api.research.ResearchManager` (has `completeResearch(Player, String)`), `ResearchCategories` (has `getResearchCategory(String key)` → `ResearchCategory`).
- Aura: classes live in `thaumcraft.common.world.aura` (AuraChunk, AuraHandler, AuraManager, AuraWorld, AuraChunkHandler, AuraScheduler). AuraChunk has `getVis()` (verify with grep) and is keyed by (dimension, x, z).
- Registration: `thaumcraft.init.ModItems` (DeferredHolder<Item, Item>), `thaumcraft.init.ModBlocks`, `thaumcraft.init.ModSounds`, `thaumcraft.init.ModEffects` (NOT ModPotionEffects), `thaumcraft.init.ModEntities`, `thaumcraft.init.ModMenuTypes`.
- Item ids: `thaumcraft.init.ItemRegistration.id(...)`; block ids: `BlockRegistration.id(...)`.
- MISSING CLASSES to handle (grep refs first, then create minimal correct class OR rewire to existing API):
  - `thaumcraft.init.ModEnchantments` — referenced by EntityArcaneBore (12 `ENCHANTMENT` vars). Create with DeferredRegister<Enchantment> if refs need it; check 1.12 original at /Users/spencer/tcbuild/src/main/java_old/ if needed.
  - `thaumcraft.client.ThaumcraftClient` — client entry point; check what it must expose (grep its usages: 8 refs) and create/alias correctly.
  - `StatDiscovery`, `ResearchCard`, `ResearchAid` — research-config classes (ConfigResearch.java, 72 errors). See 1.12 originals under /Users/spencer/tcbuild/src/main/java_old/thaumcraft/lib/research/ (or grep java_old for their 1.12 behavior).

## Event bus (NeoForge 26.2)
- `@EventBusSubscriber` default bus = MOD bus. Game-bus events (LevelTickEvent, ClientTickEvent, LivingEvent.*, EntityJoinLevelEvent...) REQUIRE `bus = EventBusSubscriber.Bus.GAME`.
- Check every @SubscribeEvent class in your group.

## 1.12 reference source (for behavior)
```
/Users/spencer/tcbuild/src/main/java_old/
```

## Verification pattern (per file)
1. `grep -c "cannot find symbol" your file's errors` from /tmp/tc_errors_full.txt (orchestrator maintains it).
2. Grep decompiled source for each missing symbol before writing the fix.
3. Java syntax check only: `javac -proc:only -d /tmp/synchk -nowarn $(file) 2>&1 | head` is NOT reliable without classpath — skip it. Trust careful reading; the orchestrator runs the real compile after your batch.
