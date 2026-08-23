#!/usr/bin/env python3
"""Find lines in src/main/java that contain a suspicious literal backslash."""
import pathlib

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"

n = 0
for p in SRC.rglob("*.java"):
    for i, line in enumerate(p.read_text(encoding="utf-8").splitlines(), 1):
        if "\\" in line and '"' in line:
            print(f"{p}:{i}: {line.strip()}")
            n += 1
            if n >= 25:
                raise SystemExit(0)
print(f"total suspicious lines: {n}")
