#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — migration pass 8: worldgen context accessors.

26.2 worldgen contexts (FeaturePlaceContext, structure GenerationContext, etc.)
expose randomness via record-style accessors, not getters:
    context.getRandom()     -> context.random()
    context.getRandomState() -> context.randomState()
    context.getSeed()       -> context.seed()
Error-guided: only applied on lines javac flagged.
"""
import pathlib
import re
import sys

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"
ERRFILE = sys.argv[1] if len(sys.argv) > 1 else ""


def error_lines_for(pattern: str):
    out = {}
    if not ERRFILE:
        return out
    lines = open(ERRFILE, encoding="utf-8", errors="replace").read().splitlines()
    for i, line in enumerate(lines):
        m = re.match(r"^\s*(.+?):(\d+): error: (.*)$", line)
        if not m:
            continue
        if any(p.strip().startswith("symbol:") and pattern in p for p in lines[i+1:i+5]):
            out.setdefault(m.group(1), set()).add(int(m.group(2)))
    return out


getrandom_lines = error_lines_for("getRandom()")
getrstate_lines = error_lines_for("getRandomState()")
getseed_lines = error_lines_for("getSeed()")

changed = 0
hits = 0
for p in sorted(SRC.rglob("*.java")):
    t = p.read_text(encoding="utf-8")
    orig = t
    abs_p = str(p.resolve())
    lines = t.splitlines(keepends=True)
    dirty = False

    for ln in sorted(getrandom_lines.get(abs_p, ())):
        if ln <= len(lines):
            nl = re.sub(r"\.getRandom\(\)", ".random()", lines[ln-1])
            if nl != lines[ln-1]:
                lines[ln-1] = nl; dirty = True; hits += 1
    for ln in sorted(getrstate_lines.get(abs_p, ())):
        if ln <= len(lines):
            nl = re.sub(r"\.getRandomState\(\)", ".randomState()", lines[ln-1])
            if nl != lines[ln-1]:
                lines[ln-1] = nl; dirty = True; hits += 1
    for ln in sorted(getseed_lines.get(abs_p, ())):
        if ln <= len(lines):
            nl = re.sub(r"\.getSeed\(\)", ".seed()", lines[ln-1])
            if nl != lines[ln-1]:
                lines[ln-1] = nl; dirty = True; hits += 1

    if dirty:
        p.write_text("".join(lines), encoding="utf-8")
        changed += 1

print(f"pass8 worldgen-context: files changed={changed}, replacements={hits}")
