#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — REVERT: undo pass-8 over-apply in client FX classes.

Pass 8 (error-guided) replaced .getRandom() -> .random() anywhere javac flagged
"cannot find symbol getRandom()". That was correct for worldgen contexts but
WRONG for client FX classes (FXVoidStream, ThaumcraftParticle, ...), which use a
`random` field (protected RandomSource random) — not a random() method. This
reverts .random() -> .getRandom() only under src/.../client/fx/.

Idempotent. Run before the next recompile.
"""
import pathlib
import re

FX = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java" / "thaumcraft" / "client" / "fx"

changed = 0
hits = 0
for p in sorted(FX.rglob("*.java")):
    t = p.read_text(encoding="utf-8")
    new = t.replace(".random()", ".getRandom()")
    if new != t:
        n = t.count(".random()")
        p.write_text(new, encoding="utf-8")
        changed += 1
        hits += n

print(f"revert fx random(): files changed={changed}, replacements={hits}")
