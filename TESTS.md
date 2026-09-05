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

## Coverage (41 tests, 7 classes)

| Class | Tests | What it verifies |
|---|---|---|
| `AspectTest` | 8 | Aspect registration, lookup (`getAspect`), duplicate rejection, name derivation, component/mixture registration, blend & image storage |
| `AspectListTest` | 13 | add/merge/remove/reduce semantics, `size` vs `visSize`, `contains`, add/remove/merge of lists, copy independence, sort-by-name & sort-by-amount |
| `MatrixTest` | 6 | Multiblock blueprint rotation: 0/90/180/270/360°, square + rectangular grids |
| `PartTest` | 5 | DustTrigger blueprint cell: constructors, default priority (50), all accessors/setters, chaining |
| `FocusPackageTest` | 5 | **Regression**: `getComplexity()` sums each node's declared complexity (not a flat 5/node), zero-complexity floor, non-`FocusNode` fallback, caching |
| `PosXYTest` | 5 | Grid position: construction, copy, equality/hash, `getDistanceSquared`, `Comparable` ordering |

## What is NOT here (and why)

Blocks, tiles, entities, GUIs, network packets, and event handlers all depend on the
Minecraft/NeoForge runtime (`Level`, `ServerLevel`, `BlockState`, `Entity`,
`ServerPlayer`, etc.) and can only be exercised **in-game** — those belong in
integration / boot tests (the running dev server + client are the live check for them).
This suite targets the framework-agnostic core where logic bugs actually live.
