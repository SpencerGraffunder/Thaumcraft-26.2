#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — migration pass 5.

  - ForgeRegistries.XXX -> BuiltInRegistries.YYY (field renames)
  - net.neoforged.neoforge.server.level.FakePlayer -> net.neoforged.neoforge.common.util.FakePlayer
  - dropCustomDeathLoot(DamageSource, int, boolean) -> (ServerLevel, DamageSource, boolean)
      (looting arg dropped; body refs -> 0)
  - ModSpawnPlacements: SpawnPlacementRegisterEvent -> RegisterSpawnPlacementsEvent
      (drop Operation.AND argument)
"""
import pathlib
import re

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"
SRV_IMPORT = "import net.minecraft.server.level.ServerLevel;"

FORGE_REG_FIELDS = [
    ("ForgeRegistries.BLOCKS", "BuiltInRegistries.BLOCK"),
    ("ForgeRegistries.ITEMS", "BuiltInRegistries.ITEM"),
    ("ForgeRegistries.ENTITY_TYPES", "BuiltInRegistries.ENTITY_TYPE"),
    ("ForgeRegistries.MOB_EFFECTS", "BuiltInRegistries.MOB_EFFECT"),
    ("ForgeRegistries.SOUND_EVENTS", "BuiltInRegistries.SOUND_EVENT"),
    ("ForgeRegistries.RECIPE_SERIALIZERS", "BuiltInRegistries.RECIPE_SERIALIZER"),
    ("ForgeRegistries.BLOCK_ENTITY_TYPES", "BuiltInRegistries.BLOCK_ENTITY_TYPE"),
    ("ForgeRegistries.FEATURES", "BuiltInRegistries.FEATURE"),
    ("ForgeRegistries.RECIPE_TYPES", "BuiltInRegistries.RECIPE_TYPE"),
    ("ForgeRegistries.MENU_TYPES", "BuiltInRegistries.MENU"),
]

DROP_SIG = re.compile(
    r"dropCustomDeathLoot\(DamageSource source, int looting\w*, boolean (\w+)\)"
)
DROP_SIG_FQN = re.compile(
    r"dropCustomDeathLoot\(net\.minecraft\.world\.damagesource\.DamageSource source, int looting\w*, boolean (\w+)\)"
)
DROP_SUPER = re.compile(
    r"super\.dropCustomDeathLoot\(source, looting\w*, (\w+)\)"
)


def ensure_import(text: str, imp: str) -> str:
    if imp in text:
        return text
    lines = text.splitlines(keepends=True)
    for i, line in enumerate(lines):
        if line.startswith("package "):
            lines.insert(i + 1, imp + "\n")
            break
    return "".join(lines)


changed = 0
for p in sorted(SRC.rglob("*.java")):
    t = p.read_text(encoding="utf-8")
    orig = t
    rel = str(p.relative_to(SRC.parent.parent))

    for old, new in FORGE_REG_FIELDS:
        t = t.replace(old, new)
    t = t.replace(
        "net.neoforged.neoforge.server.level.FakePlayer",
        "net.neoforged.neoforge.common.util.FakePlayer",
    )

    if "dropCustomDeathLoot" in t:
        t = DROP_SIG_FQN.sub(
            r"dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean \1)",
            t,
        )
        t = DROP_SIG.sub(
            r"dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean \1)",
            t,
        )
        t = DROP_SUPER.sub(r"super.dropCustomDeathLoot(level, source, \1)", t)
        t = re.sub(r"\blooting\w*\b", "0", t)
        t = ensure_import(t, SRV_IMPORT)

    if "SpawnPlacementRegisterEvent" in t:
        t = t.replace(
            "import net.neoforged.neoforge.event.entity.SpawnPlacementRegisterEvent;",
            "import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;",
        )
        t = t.replace("SpawnPlacementRegisterEvent", "RegisterSpawnPlacementsEvent")
        t = re.sub(r",\s*RegisterSpawnPlacementsEvent\.Operation\.AND", "", t)

    if t != orig:
        p.write_text(t, encoding="utf-8")
        changed += 1
        print("  ", rel)

print(f"files changed: {changed}")
