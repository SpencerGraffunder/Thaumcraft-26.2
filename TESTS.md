# Thaumcraft 26.2 — Unit Test Suite

Pure-logic unit tests that run in a **plain JVM** (no Minecraft/NeoForge client or
server required). They are wired into Gradle's `test` task, so `./gradlew build`
compiles, runs them, and only ships the jar if they pass.

## Run

```bash
CI=true ./gradlew test          # run the suite
CI=true ./gradlew build         # compile + test + package jar
CI=true ./gradlew test --tests thaumcraft.api.aspects.AspectListTest   # one class
```

## Coverage (75 tests, 10 classes)

| Class | Tests | What it verifies |
|---|---|---|
| `AspectTest` | 8 | Aspect registration, lookup (`getAspect`), duplicate rejection, name derivation, component/mixture registration, blend & image storage |
| `AspectListTest` | 12 | add/merge/remove/reduce semantics, `size` vs `visSize`, `contains`, add/remove/merge of lists, copy independence, sort-by-name & sort-by-amount |
| `MatrixTest` | 6 | Multiblock blueprint rotation: 0/90/180/270/360°, square + rectangular grids |
| `PartTest` | 5 | DustTrigger blueprint cell: constructors, default priority (50), accessors/setters, chaining |
| `FocusPackageTest` | 5 | **Regression**: `getComplexity()` sums each node's declared complexity (not a flat 5/node), zero-complexity floor, non-`FocusNode` fallback, caching |
| `PosXYTest` | 5 | Grid position: construction, copy, equality/hash, `getDistanceSquared`, `Comparable` ordering |
| `NodeSettingTest` | 11 | Focus node settings: int-range & int-list clamping, increment/decrement, `setValue` search, value-text mapping, defaults |
| `ResearchEntryTest` | 8 | Research metadata: parent `~`/`@` prefix/postfix stripping, meta-flag checks, accessors, the 6 meta flags |
| `TaskTest` | 11 | Golem task: block/entity types, lifespan bookkeeping, reserved/completed/suspended state, priority, UUID, id/equals identity |
| `SealPosTest` | 4 | Golem seal position: equality/hash, and **`toLong()`/`fromLong()` round-trip** (see bug fix below) |

## Bug found & fixed by the suite

`SealPos.toLong()` packed the face ordinal into **bit 60** (`pos.asLong() ^ (face << 60)`).
But `BlockPos.asLong()` stores `x` in bits **32–63** as a sign-extended int, so for **any
negative x** bits 60–63 are all 1 and the face got corrupted — `fromLong()` then read
`Direction.values()[10]` and threw `ArrayIndexOutOfBoundsException` (round-trip of
`(-5,10,-7)` reproduced it). The `toLong`/`fromLong` pair is currently unused in the codebase,
so this was a **latent** bug. `SealPosTest.toLong_fromLong_round_trips_small_positions` caught it.

Fixed by replacing the XOR-with-`asLong` trick with an explicit **non-overlapping** field
packing that covers the full valid MC world (26-bit signed x, 9-bit y in [-64,512],
26-bit signed z, 3-bit face). Round-trips for negative coordinates, all 6 faces, and
boundary values now pass.

## What is NOT here (and why)

Blocks, tiles, entities, GUIs, network packets, and event handlers all depend on the
Minecraft/NeoForge runtime (`Level`, `ServerLevel`, `BlockState`, `Entity`,
`ServerPlayer`, etc.) and can only be exercised **in-game** — those belong in
integration / boot tests (the running dev server + client are the live check for them).
This suite targets the framework-agnostic core where logic bugs actually live — and
it already found a real one.
