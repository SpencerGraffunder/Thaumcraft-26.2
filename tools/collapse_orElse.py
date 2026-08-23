#!/usr/bin/env python3
"""Collapse doubled .orElse(new X[0]).orElse(new X[0]) from the array-getter pass."""
import pathlib
import re

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"
PAT = re.compile(r"(\.orElse\(new (?:byte|int|long)\[0\]\))(\.orElse\(new (?:byte|int|long)\[0\]\))")

changed = 0
for path in SRC.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    new, n = PAT.subn(r"\1", text)
    if n:
        path.write_text(new, encoding="utf-8")
        changed += 1
        print(f"collapsed {n} in {path.name}")
print(f"files changed: {changed}")
