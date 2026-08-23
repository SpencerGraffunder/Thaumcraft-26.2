#!/usr/bin/env python3
"""
Repair two bugs introduced by migrate_26_2.py:

 A) Zero-arg getters (e.g. Component.getString()) were mangled into
    getStringOr(, ...). Restore any zero-argument getter to its original form:
        getStringOr(, ...) -> getString()
        getIntOr(, 0)      -> getInt()
        etc.
 B) The getString default was written with a literal backslash:
        getStringOr("x", \")  ->  getStringOr("x", "")
"""
import pathlib
import re

SRC = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "java"

# A) zero-arg getter restoration (only known getter names)
ZERO_ARG = re.compile(
    r"(get(?:Byte|Short|Int|Long|Float|Double|String|Boolean))Or\(,\s*[^()]*\)"
)
# zero-arg compound/list restoration
COMPOUND_EMPTY = re.compile(r"getCompoundOrEmpty\(\)")
LIST_EMPTY = re.compile(r"getListOrEmpty\(\)")
# B) literal backslash-quote pair written by the getString rule:
#    getStringOr("x", \"\")  ->  getStringOr("x", "")
BAD_QUOTE = re.compile(r', \\"\\"\)')

stats = {"zero_arg": 0, "compound": 0, "list": 0, "bad_quote": 0, "files": 0}
for path in SRC.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    new = text
    new, n1 = ZERO_ARG.subn("\\1()", new)
    new, n2 = COMPOUND_EMPTY.subn("getCompound()", new)
    new, n3 = LIST_EMPTY.subn("getList()", new)
    new, n4 = BAD_QUOTE.subn(', "")', new)
    if new != text:
        path.write_text(new, encoding="utf-8")
        stats["files"] += 1
        stats["zero_arg"] += n1
        stats["compound"] += n2
        stats["list"] += n3
        stats["bad_quote"] += n4

print(stats)
