#!/usr/bin/env python3
"""
Fix a bug from migrate_26_2.py: the getString rule wrote a literal backslash
before the closing quote (`getStringOr(key, \")`). This corrects every
` , \"` (comma-space-backslash-quote) that came from that rule to ` , "`.
"""
import pathlib

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"
BAD = ', \\")'   # comma, space, backslash, quote, close-paren
GOOD = ', ")'

changed = 0
for path in SRC.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    if BAD in text:
        path.write_text(text.replace(BAD, GOOD), encoding="utf-8")
        changed += 1
print(f"Fixed {changed} files")
