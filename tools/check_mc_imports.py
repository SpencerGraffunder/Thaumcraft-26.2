#!/usr/bin/env python3
"""Reliable broken-import check, scoped to net.minecraft.* only
(the decompiled transformSource tree only contains vanilla + com.mojang).
Outputs /tmp/mc_remap.txt:  OLD -> NEW  for uniquely-movable vanilla imports,
and lists GONE vanilla imports separately.
"""
import pathlib, re
from collections import defaultdict

T = pathlib.Path("/Users/spencer/Documents/Thaumcraft-26.2/build/neoForm/neoFormJoined26.2-2/steps/transformSource/transformed")
SRC = pathlib.Path("/Users/spencer/Documents/Thaumcraft-26.2/src/main/java")

# index 26.2 classes
name_to_fqns = defaultdict(set)
fqn_exists = set()
for p in T.rglob("*.java"):
    try:
        head = p.read_text(encoding="utf-8", errors="replace").splitlines()[:3]
    except Exception:
        continue
    pkg = ""
    for ln in head:
        m = re.match(r"package ([\w.]+);", ln)
        if m:
            pkg = m.group(1); break
    if pkg:
        fqn = pkg + "." + p.stem
        name_to_fqns[p.stem].add(fqn)
        fqn_exists.add(fqn)

broken = defaultdict(int)
for p in SRC.rglob("*.java"):
    for ln in p.read_text(encoding="utf-8", errors="replace").splitlines():
        m = re.match(r"^import (?:static )?(net\.minecraft\.[\w.]+);", ln)
        if not m: continue
        fqn = m.group(1)
        if fqn in fqn_exists: continue
        broken[fqn] += 1

remap, gone = {}, []
for fqn, c in sorted(broken.items()):
    simple = fqn.split(".")[-1]
    cands = name_to_fqns.get(simple, set())
    if len(cands) == 1:
        remap[fqn] = next(iter(cands))
    else:
        gone.append((fqn, c, sorted(cands)))

with open("/tmp/mc_remap.txt","w") as f:
    for old,new in sorted(remap.items()):
        f.write(f"{old}\t{new}\n")

print("=== REMAPPABLE vanilla imports (OLD -> NEW) ===")
for old,new in sorted(remap.items()):
    print(f"  {old}  ->  {new}")
print(f"\nAuto-remappable: {len(remap)}")
print("\n=== GONE vanilla imports (no vanilla replacement; need API rewrite) ===")
for fqn, c, cands in gone:
    print(f"  {c:4d}  {fqn}" + (f"   [cands: {cands}]" if cands else "   [GONE]"))
