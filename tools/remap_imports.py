#!/usr/bin/env python3
"""Build a simple-name -> [FQN] index of all classes in the decompiled 26.2 tree,
then for each TC import that javac flagged as a missing class, find the new FQN.

Writes /tmp/import_remap.txt with lines:  OLD_FQN -> NEW_FQN
and prints a summary. Also handles classes that are genuinely GONE (no match).
"""
import pathlib, re, sys
from collections import defaultdict

T = pathlib.Path("/Users/spencer/Documents/Thaumcraft-26.2/build/neoForm/neoFormJoined26.2-2/steps/transformSource/transformed")
SRC = pathlib.Path("/Users/spencer/Documents/Thaumcraft-26.2/src/main/java")

# 1) index all classes in 26.2
name_to_fqns = defaultdict(set)
for p in T.rglob("*.java"):
    try:
        first = p.read_text(encoding="utf-8", errors="replace").splitlines()[:3]
    except Exception:
        continue
    pkg = ""
    for ln in first:
        m = re.match(r"package ([\w.]+);", ln)
        if m:
            pkg = m.group(1); break
    simple = p.stem
    if pkg:
        name_to_fqns[simple].add(pkg + "." + simple)

# 2) collect TC imports that are flagged missing (class X cannot find symbol)
log = open("/tmp/tc_compile.log", encoding="utf-8", errors="replace").read().splitlines()
missing = set()
for i, ln in enumerate(log):
    if re.match(r"^.*\.java:\d+: error: cannot find symbol$", ln):
        for j in range(i+1, min(i+5, len(log))):
            s = log[j].strip()
            if s.startswith("symbol:"):
                sym = s.split(":",1)[1].strip()
                if sym.startswith("class "):
                    missing.add(sym.split(None,1)[1].split("<")[0])
                break

# 3) for each TC import, if its simple name is in `missing`, try to remap
remap = {}   # old_fqn -> new_fqn
gone = defaultdict(int)
ambiguous = {}
for p in SRC.rglob("*.java"):
    for ln in p.read_text(encoding="utf-8", errors="replace").splitlines():
        m = re.match(r"^import (static )?([\w.]+);", ln)
        if not m: continue
        fqn = m.group(2)
        simple = fqn.split(".")[-1]
        if simple not in missing: continue
        cands = name_to_fqns.get(simple, set())
        if len(cands) == 1:
            new = next(iter(cands))
            if new != fqn:
                remap[fqn] = new
        elif len(cands) > 1:
            ambiguous.setdefault(simple, set()).update(cands)

print("=== REMAPPABLE (unique new location) ===")
for old, new in sorted(remap.items()):
    print(f"{old}  ->  {new}")
print(f"\nTotal remappable imports: {len(remap)}")

print("\n=== AMBIGUOUS (multiple candidates) ===")
for simple, cands in sorted(ambiguous.items()):
    print(f"{simple}: {sorted(cands)}")

# write remap file
with open("/tmp/import_remap.txt", "w") as f:
    for old, new in sorted(remap.items()):
        f.write(f"{old}\t{new}\n")
