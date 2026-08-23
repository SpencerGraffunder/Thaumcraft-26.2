#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — migration pass 9: block state property generics.

TC declared untyped `EnumProperty FACING = ...` but 26.2 properties are
`EnumProperty<Direction>`, so `state.getValue(FACING)` returned raw Comparable.
Fix: `EnumProperty NAME` -> `EnumProperty<Direction> NAME` (all TC RHS are
Direction-typed, verified against decompiled 26.2 sources).
"""
import pathlib
import re

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"

changed = 0
hits = 0
for p in sorted(SRC.rglob("*.java")):
    t = p.read_text(encoding="utf-8")
    # match untyped `EnumProperty <UPPERCASE NAME>` (not already parameterized)
    new = re.sub(r"\b(EnumProperty)(\s+)([A-Z][A-Z0-9_]*)", r"\1<Direction>\2\3", t)
    if new != t:
        n = len(re.findall(r"\bEnumProperty(\s+[A-Z][A-Z0-9_]*)", t))
        p.write_text(new, encoding="utf-8")
        changed += 1
        hits += n

print(f"pass9 EnumProperty<Direction>: files changed={changed}, declarations={hits}")
