#!/usr/bin/env python3
"""
Thaumcraft 26.2 — fix unbalanced `}` in network files (net -1 brace each).

The network migration added one extra top-level `}` per file, so the running
brace level goes negative one line before the last `}`. This uses a stack to find
the first `}` that overflows, verifies removing that line restores balance, then
strips it. Only files under .../lib/network/, and only when net delta == -1
(exactly one extra close). Idempotent.
"""
import pathlib

NET = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java" / "thaumcraft" / "common" / "lib" / "network"

fixed = 0
skipped = 0
for p in sorted(NET.rglob("*.java")):
    s = p.read_text(encoding="utf-8")
    o, c = s.count("{"), s.count("}")
    if o == c:
        continue
    if o - c != -1:
        skipped += 1
        print(f"  SKIP net={o-c:+d} (need manual): {p.relative_to(NET)}")
        continue
    lines = s.splitlines(keepends=True)
    stack = []
    removed = False
    for i, ln in enumerate(lines):
        for ch in ln:
            if ch == "{":
                stack.append(i)
            elif ch == "}":
                if stack:
                    stack.pop()
                else:
                    new_lines = lines[:i] + lines[i + 1:]
                    nl = "".join(new_lines)
                    if nl.count("{") == nl.count("}"):
                        p.write_text(nl, encoding="utf-8")
                        fixed += 1
                        removed = True
                        print(f"  removed extra '}}' line {i + 1}: {p.name}")
                        break
        if removed:
            break

print(f"fixed={fixed} skipped={skipped}")
