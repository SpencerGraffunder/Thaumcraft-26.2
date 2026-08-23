#!/usr/bin/env python3
"""
Thaumcraft 26.2 port — migration pass 1 (safe, mechanical, tree-wide).

Applies MC 26.2 API rewrites that preserve the original 1.20.1 semantics:

  1. CompoundTag getters now return Optional<T>; use the new *Or variants:
       tag.getInt("x")        -> tag.getIntOr("x", 0)
       tag.getByte("x")       -> tag.getByteOr("x", (byte)0)
       tag.getShort("x")      -> tag.getShortOr("x", (short)0)
       tag.getLong("x")       -> tag.getLongOr("x", 0L)
       tag.getFloat("x")      -> tag.getFloatOr("x", 0.0F)
       tag.getDouble("x")     -> tag.getDoubleOr("x", 0.0D)
       tag.getString("x")     -> tag.getStringOr("x", "")
       tag.getBoolean("x")    -> tag.getBooleanOr("x", false)
       tag.getCompound("x")   -> tag.getCompoundOrEmpty("x")
       tag.getList("x", type) -> tag.getListOrEmpty("x")   (type arg removed)
       tag.getIntArray("x")   -> tag.getIntArray("x").orElse(new int[0])
       tag.getByteArray("x")  -> tag.getByteArray("x").orElse(new byte[0])
       tag.getLongArray("x")  -> tag.getLongArray("x").orElse(new long[0])

  2. Level.isClientSide field is now private; use the isClientSide() getter.

Only touches files under src/main/java (NOT java_old).
"""
import pathlib
import re
import sys

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"

RULES = [
    # Order matters: specific (longer) names first where prefixes overlap.
    # Args are required (one-or-more chars) so zero-arg calls are never touched.
    (re.compile(r"\.getByteArray\(([^()]+)\)(?!\s*\.orElse)"), r".getByteArray(\1).orElse(new byte[0])"),
    (re.compile(r"\.getIntArray\(([^()]+)\)(?!\s*\.orElse)"),  r".getIntArray(\1).orElse(new int[0])"),
    (re.compile(r"\.getLongArray\(([^()]+)\)(?!\s*\.orElse)"), r".getLongArray(\1).orElse(new long[0])"),
    (re.compile(r"\.getByte\(([^()]+)\)"),        r".getByteOr(\1, (byte)0)"),
    (re.compile(r"\.getShort\(([^()]+)\)"),       r".getShortOr(\1, (short)0)"),
    (re.compile(r"\.getInt\(([^()]+)\)"),         r".getIntOr(\1, 0)"),
    (re.compile(r"\.getLong\(([^()]+)\)"),        r".getLongOr(\1, 0L)"),
    (re.compile(r"\.getFloat\(([^()]+)\)"),       r".getFloatOr(\1, 0.0F)"),
    (re.compile(r"\.getDouble\(([^()]+)\)"),      r".getDoubleOr(\1, 0.0D)"),
    (re.compile(r"\.getString\(([^()]+)\)"),      ".getStringOr(\\1, \"\")"),
    (re.compile(r"\.getBoolean\(([^()]+)\)"),     r".getBooleanOr(\1, false)"),
    (re.compile(r"\.getCompound\(([^()]+)\)"),    r".getCompoundOrEmpty(\1)"),
    # getList(x, type) and getList(x) both collapse to getListOrEmpty(x)
    (re.compile(r"\.getList\(([^(),]+),\s*[^()]+\)"), r".getListOrEmpty(\1)"),
    (re.compile(r"\.getList\(([^()]+)\)"),        r".getListOrEmpty(\1)"),
    # Level.isClientSide (field) -> isClientSide() (method); skip if already a call
    (re.compile(r"\.isClientSide(?!\()"),         ".isClientSide()"),
]


def rewrite(text: str) -> str:
    out = text
    for pattern, repl in RULES:
        out, _ = pattern.subn(repl, out)
    return out


def main() -> int:
    changed = []
    for path in sorted(SRC.rglob("*.java")):
        original = path.read_text(encoding="utf-8")
        updated = rewrite(original)
        if updated != original:
            path.write_text(updated, encoding="utf-8")
            changed.append(str(path))
    print(f"Rewrote {len(changed)} files")
    for c in changed:
        print("  " + c)
    return 0


if __name__ == "__main__":
    sys.exit(main())
