#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — migration pass 6.

  - Block.neighborChanged: BlockPos fromPos -> Orientation orientation
  - net.neoforged.neoforge.registries.NeoBuiltInRegistries.* -> net.minecraft.core.registries.BuiltInRegistries.*
  - newly-surfaced Inventory.items field access -> getItems() (error-guided)
"""
import pathlib
import re
import sys

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"
ERRFILE = sys.argv[1] if len(sys.argv) > 1 else ""

ORIENT_IMPORT = "import net.minecraft.core.Orientation;"


def ensure_import(text: str, imp: str) -> str:
    if imp in text:
        return text
    lines = text.splitlines(keepends=True)
    for i, line in enumerate(lines):
        if line.startswith("package "):
            lines.insert(i + 1, imp + "\n")
            break
    return "".join(lines)


# items errors from javac
items_lines = {}
if ERRFILE:
    for line in open(ERRFILE, encoding="utf-8", errors="replace"):
        m = re.match(r"^\s*(.+?):(\d+): error: (.*)$", line)
        if not m:
            continue
        f, ln, msg = m.group(1), int(m.group(2)), m.group(3)
        if "items has private access in Inventory" in msg:
            items_lines.setdefault(f, set()).add(ln)

changed = 0
for p in sorted(SRC.rglob("*.java")):
    t = p.read_text(encoding="utf-8")
    orig = t
    rel = str(p.relative_to(SRC.parent.parent))

    # neighborChanged
    if "neighborChanged" in t:
        t = t.replace("BlockPos fromPos, boolean isMoving)", "Orientation orientation, boolean isMoving)")
        t = t.replace(
            "super.neighborChanged(state, level, pos, block, fromPos, isMoving)",
            "super.neighborChanged(state, level, pos, block, orientation, isMoving)",
        )
        if "Orientation orientation" in t:
            t = ensure_import(t, ORIENT_IMPORT)

    # NeoBuiltInRegistries
    t = t.replace(
        "net.neoforged.neoforge.registries.NeoBuiltInRegistries.",
        "net.minecraft.core.registries.BuiltInRegistries.",
    )

    # items (error-guided, per line)
    abs_p = str(p.resolve())
    if abs_p in items_lines:
        lines = t.splitlines(keepends=True)
        dirty = False
        for ln in sorted(items_lines[abs_p]):
            if ln <= len(lines):
                nl = re.sub(r"\.items\b", ".getItems()", lines[ln - 1])
                if nl != lines[ln - 1]:
                    lines[ln - 1] = nl
                    dirty = True
        if dirty:
            t = "".join(lines)

    if t != orig:
        p.write_text(t, encoding="utf-8")
        changed += 1
        print("  ", rel)

print(f"files changed: {changed}")
