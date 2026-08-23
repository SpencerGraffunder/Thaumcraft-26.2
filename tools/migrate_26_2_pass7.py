#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — migration pass 7.

  1) Remap moved vanilla imports (verified against decompiled 26.2 tree) —
     entities/models were re-packaged into sub-packages.
  2) ResourceKey.location() -> identifier()  (verified: 26.2 ResourceKey has identifier())
  3) Entity/Level stray `random` field -> getRandom() in entity contexts is
     handled elsewhere; here: Level getRandom already done. (no-op safety)

Each transform is idempotent.
"""
import pathlib
import re
import sys

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"
ERRFILE = sys.argv[1] if len(sys.argv) > 1 else ""


def error_lines_for(pattern: str, in_symbol: bool = False):
    """Return {abs_file: set(line)}. If in_symbol, match `pattern` on the
    following `symbol:` detail line rather than the error message."""
    out = {}
    if not ERRFILE:
        return out
    lines = open(ERRFILE, encoding="utf-8", errors="replace").read().splitlines()
    for i, line in enumerate(lines):
        m = re.match(r"^\s*(.+?):(\d+): error: (.*)$", line)
        if not m:
            continue
        hit = pattern in m.group(3) if not in_symbol else any(
            p.strip().startswith("symbol:") and pattern in p for p in lines[i+1:i+5])
        if hit:
            out.setdefault(m.group(1), set()).add(int(m.group(2)))
    return out


loc_lines = error_lines_for("location()", in_symbol=True)

# OLD -> NEW  (verbatim import target replacement, only for `import X;` lines)
IMPORT_REMAP = {
    "net.minecraft.client.model.SilverfishModel": "net.minecraft.client.model.monster.silverfish.SilverfishModel",
    "net.minecraft.client.model.SlimeModel": "net.minecraft.client.model.monster.slime.SlimeModel",
    "net.minecraft.client.model.SpiderModel": "net.minecraft.client.model.monster.spider.SpiderModel",
    "net.minecraft.client.model.ZombieModel": "net.minecraft.client.model.monster.zombie.ZombieModel",
    "net.minecraft.core.Orientation": "net.minecraft.world.level.redstone.Orientation",
    "net.minecraft.world.ContainerListener": "net.minecraft.world.inventory.ContainerListener",
    "net.minecraft.world.entity.animal.Parrot": "net.minecraft.world.entity.animal.parrot.Parrot",
    "net.minecraft.world.entity.animal.horse.AbstractHorse": "net.minecraft.world.entity.animal.equine.AbstractHorse",
    "net.minecraft.world.entity.monster.Slime": "net.minecraft.world.entity.monster.cubemob.Slime",
    "net.minecraft.world.entity.monster.Spider": "net.minecraft.world.entity.monster.spider.Spider",
    "net.minecraft.world.entity.monster.Zombie": "net.minecraft.world.entity.monster.zombie.Zombie",
    "net.minecraft.world.entity.projectile.AbstractArrow": "net.minecraft.world.entity.projectile.arrow.AbstractArrow",
    "net.minecraft.world.entity.projectile.Arrow": "net.minecraft.world.entity.projectile.arrow.Arrow",
    "net.minecraft.world.entity.projectile.Fireball": "net.minecraft.world.entity.projectile.hurtingprojectile.Fireball",
    "net.minecraft.world.entity.projectile.SmallFireball": "net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball",
    "net.minecraft.world.inventory.RecipeHolder": "net.minecraft.world.item.crafting.RecipeHolder",
    "net.minecraft.world.item.ArmorMaterials": "net.minecraft.world.item.equipment.ArmorMaterials",
}

changed = 0
imp_hits = 0
loc_hits = 0
for p in sorted(SRC.rglob("*.java")):
    t = p.read_text(encoding="utf-8")
    orig = t

    # 1) import remaps — line-scoped so we don't touch unrelated code
    out_lines = []
    for line in t.splitlines(keepends=True):
        st = line.strip()
        if st.startswith("import "):
            for old, new in IMPORT_REMAP.items():
                if line.endswith(old + ";") or line.endswith(old + ".") or old + ";" in line:
                    line = line.replace(old, new)
                    imp_hits += 1
                    break
        out_lines.append(line)
    t = "".join(out_lines)

    # 2) ResourceKey.location() -> identifier() (error-guided, per line)
    abs_p = str(p.resolve())
    if abs_p in loc_lines:
        lines = t.splitlines(keepends=True)
        dirty = False
        for ln in sorted(loc_lines[abs_p]):
            if ln <= len(lines):
                nl = lines[ln - 1].replace(".location()", ".identifier()")
                if nl != lines[ln - 1]:
                    lines[ln - 1] = nl
                    dirty = True
                    loc_hits += 1
        t = "".join(lines)

    if t != orig:
        p.write_text(t, encoding="utf-8")
        changed += 1

print(f"pass7: files changed={changed}, import remaps={imp_hits}, location()->identifier()={loc_hits}")
