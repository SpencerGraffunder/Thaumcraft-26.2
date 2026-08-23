#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — migration pass 4 (entity API signature changes).

  - defineSynchedData() -> defineSynchedData(SynchedEntityData.Builder builder)
    (super(builder), this.entityData.define -> builder.define)
  - doHurtTarget(Entity) -> doHurtTarget(ServerLevel, Entity)
    (super(level, target); bare calls -> ((ServerLevel) this.level(), target))
  - customServerAiStep() -> customServerAiStep(ServerLevel level)
  - isInvulnerableTo(DamageSource) -> isInvulnerableTo(ServerLevel, DamageSource)
  - spawnAtLocation(stack[, y]) -> spawnAtLocation((ServerLevel) this.level(), ...)
  - ensure `import net.minecraft.server.level.ServerLevel;` present where needed.
"""
import pathlib
import re

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"
SRV_IMPORT = "import net.minecraft.server.level.ServerLevel;"
SYNC_IMPORT = "import net.minecraft.network.syncher.SynchedEntityData;"


def ensure_import(text: str, imp: str) -> str:
    if imp in text:
        return text
    lines = text.splitlines(keepends=True)
    for i, line in enumerate(lines):
        if line.startswith("package "):
            lines.insert(i + 1, imp + "\n")
            break
    return "".join(lines)


def edit(path, fn):
    p = SRC / path
    t = p.read_text(encoding="utf-8")
    new = fn(t)
    if new != t:
        p.write_text(new, encoding="utf-8")
        print("  ", path)


changed = 0
for p in sorted(SRC.rglob("*.java")):
    t = p.read_text(encoding="utf-8")
    orig = t
    rel = str(p.relative_to(SRC.parent.parent))

    # ---------- defineSynchedData ----------
    if "defineSynchedData()" in t:
        t = t.replace(
            "protected void defineSynchedData()",
            "protected void defineSynchedData(SynchedEntityData.Builder builder)",
        )
        t = t.replace("super.defineSynchedData()", "super.defineSynchedData(builder)")
        t = t.replace("this.entityData.define(", "builder.define(")
        t = t.replace("entityData.define(", "builder.define(")
        t = ensure_import(t, SYNC_IMPORT)

    # ---------- doHurtTarget ----------
    if "doHurtTarget" in t:
        t = t.replace(
            "public boolean doHurtTarget(Entity target)",
            "public boolean doHurtTarget(ServerLevel level, Entity target)",
        )
        t = t.replace("super.doHurtTarget(target)", "super.doHurtTarget(level, target)")
        t = re.sub(
            r"(?<![\w.])doHurtTarget\(([^(),]+)\)",
            r"doHurtTarget((ServerLevel) this.level(), \1)",
            t,
        )

    # ---------- customServerAiStep ----------
    if "customServerAiStep" in t:
        t = t.replace(
            "protected void customServerAiStep()",
            "protected void customServerAiStep(ServerLevel level)",
        )
        t = t.replace("super.customServerAiStep()", "super.customServerAiStep(level)")

    # ---------- isInvulnerableTo ----------
    if "isInvulnerableTo" in t:
        t = t.replace(
            "public boolean isInvulnerableTo(DamageSource source)",
            "public boolean isInvulnerableTo(ServerLevel level, DamageSource source)",
        )
        t = t.replace("super.isInvulnerableTo(source)", "super.isInvulnerableTo(level, source)")
        t = re.sub(
            r"(?<![\w.])isInvulnerableTo\(([^(),]+)\)",
            r"isInvulnerableTo((ServerLevel) this.level(), \1)",
            t,
        )

    # ---------- spawnAtLocation ----------
    if "spawnAtLocation" in t:
        # explicit receiver, 2-arg then 1-arg
        t = re.sub(
            r"([\w]+)\.spawnAtLocation\(([^(),]+),\s*([^()]+)\)",
            r"\1.spawnAtLocation((ServerLevel) \1.level(), \2, \3)",
            t,
        )
        t = re.sub(
            r"([\w]+)\.spawnAtLocation\(([^(),]+)\)",
            r"\1.spawnAtLocation((ServerLevel) \1.level(), \2)",
            t,
        )
        # bare calls, 2-arg then 1-arg
        t = re.sub(
            r"(?<![\w.])spawnAtLocation\(([^(),]+),\s*([^()]+)\)",
            r"spawnAtLocation((ServerLevel) this.level(), \1, \2)",
            t,
        )
        t = re.sub(
            r"(?<![\w.])spawnAtLocation\(([^(),]+)\)",
            r"spawnAtLocation((ServerLevel) this.level(), \1)",
            t,
        )

    # ensure ServerLevel import if we introduced casts
    if "(ServerLevel) " in t:
        t = ensure_import(t, SRV_IMPORT)

    if t != orig:
        p.write_text(t, encoding="utf-8")
        changed += 1

print(f"files changed: {changed}")
