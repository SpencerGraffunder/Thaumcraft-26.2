# Thaumcraft 6 — Minecraft 26.2 (NeoForge) Port

Port of **Thaumcraft 6** from the 1.20.1 Forge source fork
([ShobieShy/Thaumcraft-6-Source-Code-1.20.1](https://github.com/ShobieShy/Thaumcraft-6-Source-Code-1.20.1))
to **Minecraft 26.2 "Chaos Cubed"** on **NeoForge 26.2.0.59** (Java 25).

## Status: COMPILES & BUILDS — server boots to "Done" — multiplayer handshake verified

The 26.2 port is under active development.

- `./gradlew compileJava` — **GREEN (0 errors)**
- `./gradlew build` — **SUCCESS** → `build/libs/thaumcraft-6.2.0+26.2.jar`
- `./gradlew runServer` — gets through mod construction and block registration
  (id-injected via `BlockRegistration` ThreadLocal helper); **server reaches
  `Done`** — world loads, aura threads run per dimension, golem parts/seals/
  research/aspect/multiblock init run on `ServerStartingEvent` (constructing
  vanilla `ItemStack`s, illegal in commonSetup on 26.2)
- Recipe data migrated to the 26.2 format: `key` ingredients are plain strings
  and `result` uses `"id"` (not `"item"`); forge + thaumcraft item tags added
  for cross-mod interop.
- Multiplayer fixed & verified: S2C payload handlers split into
  server-safe common classes + client handler classes
  (`thaumcraft.client.lib.network.*`), so all 38 `thaumcraft:packet*`
  channels register on both dists. A 26.2.0.75 client joins a 26.2.0.75
  dev server cleanly — NeoForge handshake passes with zero channel errors,
  player in-world (see [`TODO.md`](./TODO.md) P0 for details).

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

Dev note: keep `CI=true` set for local runs — without it, NeoGradle takes the
local decompile→patch→recompile pipeline, which is broken on this toolchain
(`neoFormPatch` fails on `var1` parameter names from the decompiler). The
`runServer` console does not forward a typed `stop` to the server process —
stop it with Ctrl+C/SIGINT.

## Deployment

`deploy.sh` copies a built jar into a configurable mods folder for test-server
deployment (target: a Crafty Controller MC server on the same Unraid host).

## License

See upstream — Thaumcraft is the property of Azanor / original authors.
