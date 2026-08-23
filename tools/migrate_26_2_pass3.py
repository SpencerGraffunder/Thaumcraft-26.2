#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — migration pass 3.

  - @Mod.EventBusSubscriber bus enum: Bus.FORGE -> Bus.GAME
  - Forge event bus:          MinecraftForge.EVENT_BUS -> NeoForge.EVENT_BUS
  - Add missing NeoForge import where NeoForge.EVENT_BUS is used.
"""
import pathlib

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"
NEOFORGE_IMPORT = "import net.neoforged.neoforge.common.NeoForge;"

changed = 0
for p in SRC.rglob("*.java"):
    text = p.read_text(encoding="utf-8")
    new = text.replace("Bus.FORGE", "Bus.GAME")
    new = new.replace("MinecraftForge.EVENT_BUS", "NeoForge.EVENT_BUS")
    if "NeoForge.EVENT_BUS" in new and NEOFORGE_IMPORT not in new:
        # insert import after the package declaration
        lines = new.splitlines(keepends=True)
        for i, line in enumerate(lines):
            if line.startswith("package "):
                lines.insert(i + 1, NEOFORGE_IMPORT + "\n")
                break
        new = "".join(lines)
    if new != text:
        p.write_text(new, encoding="utf-8")
        changed += 1
        print("  " + str(p.relative_to(SRC.parent.parent)))

print(f"files changed: {changed}")
