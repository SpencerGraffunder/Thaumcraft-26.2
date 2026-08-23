#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — migration pass 2.

A) Error-file-guided fixes (only touches lines javac flagged):
     - Level.random field            -> .getRandom()
     - Inventory.items field         -> .getItems()
     - CompoundTag.contains(n, type) -> .contains(n)   (2-arg overload removed)

B) Tree-wide Forge -> NeoForge package/class renames (1:1 mappings).

Usage: python3 tools/migrate_26_2_pass2.py <javac-errors.txt> [--xprint]
  --xprint : only print the flagged ChunkPos x/z source lines (no edits).
"""
import pathlib
import re
import sys

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"
ERRFILE = sys.argv[1] if len(sys.argv) > 1 else ""
XPRINT = "--xprint" in sys.argv

# ---------- B) tree-wide Forge -> NeoForge renames ----------
FORGE_SWAPS = [
    ("net.minecraftforge.common.util.INBTSerializable", "net.neoforged.neoforge.common.util.INBTSerializable"),
    ("net.minecraftforge.common.Tags",                  "net.neoforged.neoforge.common.Tags"),
    ("net.minecraftforge.common.ToolActions",           "net.neoforged.neoforge.common.ToolActions"),
    ("net.minecraftforge.registries.ForgeRegistries",   "net.neoforged.neoforge.registries.NeoForgeRegistries"),
    ("net.minecraftforge.registries.DeferredHolder",    "net.neoforged.neoforge.registries.DeferredHolder"),
    ("net.minecraftforge.event.ForgeEventFactory",      "net.neoforged.neoforge.event.NeoForgeEventFactory"),
    ("net.minecraftforge.event.server.ServerStoppingEvent", "net.neoforged.neoforge.event.server.ServerStoppingEvent"),
    ("net.minecraftforge.event.entity.player.PlayerEvent", "net.neoforged.neoforge.event.entity.player.PlayerEvent"),
    ("net.minecraftforge.client.event.EntityRenderersEvent", "net.neoforged.neoforge.client.event.EntityRenderersEvent"),
    ("net.minecraftforge.client.event.RegisterKeyMappingsEvent", "net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent"),
    ("net.minecraftforge.client.event.RegisterColorHandlersEvent", "net.neoforged.neoforge.client.event.RegisterColorHandlersEvent"),
    ("net.minecraftforge.client.event.InputEvent",      "net.neoforged.neoforge.client.event.InputEvent"),
]

# ---------- A) parse javac errors ----------
random_lines = {}
items_lines = {}
contains_lines = {}
xz_lines = {}
if ERRFILE:
    for line in open(ERRFILE, encoding="utf-8", errors="replace"):
        m = re.match(r"^\s*(.+?):(\d+): error: (.*)$", line)
        if not m:
            continue
        f, ln, msg = m.group(1), int(m.group(2)), m.group(3)
        if "random has protected access in Level" in msg:
            random_lines.setdefault(f, set()).add(ln)
        elif "items has private access in Inventory" in msg:
            items_lines.setdefault(f, set()).add(ln)
        elif "method contains in class CompoundTag cannot be applied" in msg:
            contains_lines.setdefault(f, set()).add(ln)
        elif "x has private access in ChunkPos" in msg or "z has private access in ChunkPos" in msg:
            xz_lines.setdefault(f, set()).add(ln)

if XPRINT:
    print("=== flagged ChunkPos x/z source lines ===")
    for f, lines in sorted(xz_lines.items()):
        p = pathlib.Path(f)
        if not p.exists():
            print(f"(missing) {f}")
            continue
        src = p.read_text(encoding="utf-8").splitlines()
        for ln in sorted(lines):
            if ln <= len(src):
                print(f"{f}:{ln}: {src[ln-1].strip()}")
    raise SystemExit(0)

# ---------- apply ----------
changed = 0
for p in SRC.rglob("*.java"):
    text = p.read_text(encoding="utf-8")
    new = text

    # B) forge renames
    for old, repl in FORGE_SWAPS:
        new = new.replace(old, repl)

    # A) per-line fixes
    lines = new.splitlines(keepends=True)
    abs_p = str(p.resolve())
    dirty = False
    for i, l in enumerate(lines):
        ln = i + 1
        if abs_p in random_lines and ln in random_lines[abs_p]:
            nl = re.sub(r"\.random\b", ".getRandom()", l)
            if nl != l:
                lines[i] = nl; dirty = True
        if abs_p in items_lines and ln in items_lines[abs_p]:
            nl = re.sub(r"\.items\b", ".getItems()", l)
            if nl != l:
                lines[i] = nl; dirty = True
        if abs_p in contains_lines and ln in contains_lines[abs_p]:
            nl = re.sub(r"\.contains\(([^(),]+),\s*[^()]+\)", r".contains(\1)", l)
            if nl != l:
                lines[i] = nl; dirty = True
        if abs_p in xz_lines and ln in xz_lines[abs_p]:
            nl = re.sub(r"\.x\b", ".x()", l)
            nl = re.sub(r"\.z\b", ".z()", nl)
            if nl != l:
                lines[i] = nl; dirty = True
    new = "".join(lines)

    if new != text:
        p.write_text(new, encoding="utf-8")
        changed += 1
        print("  " + str(p.relative_to(SRC.parent.parent)))

print(f"files changed: {changed}")
