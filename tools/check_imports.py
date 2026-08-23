#!/usr/bin/env python3
"""For EVERY import in TC source, check whether that exact FQN exists in the
decompiled 26.2 tree. Report:
  - BROKEN (FQN not in 26.2) with candidate new FQNs by simple name
This is independent of javac cascade errors, so it's a reliable signal.
"""
import pathlib, re
from collections import defaultdict

T = pathlib.Path("/Users/spencer/Documents/Thaumcraft-26.2/build/neoForm/neoFormJoined26.2-2/steps/transformSource/transformed")
SRC = pathlib.Path("/Users/spencer/Documents/Thaumcraft-26.2/src/main/java")

# index all 26.2 classes: simple -> set(fqn)
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
    simple = p.stem
    if pkg:
        fqn = pkg + "." + simple
        name_to_fqns[simple].add(fqn)
        fqn_exists.add(fqn)

# scan TC imports
broken = defaultdict(lambda: {"count":0, "files":set()})   # old_fqn -> info
for p in SRC.rglob("*.java"):
    for ln in p.read_text(encoding="utf-8", errors="replace").splitlines():
        m = re.match(r"^import (static )?([\w.]+);", ln)
        if not m: continue
        fqn = m.group(2)
        # skip java.*, javax.*, and our own thaumcraft.* (may not be in 26.2 tree)
        if fqn.startswith(("java.","javax.")): continue
        if fqn in fqn_exists: continue
        # it's broken (or is a neoforge/thaumcraft class not in this tree)
        simple = fqn.split(".")[-1]
        broken[fqn]["count"] += 1
        broken[fqn]["files"].add(p.name)

# separate: neoforge/thaumcraft (not expected in vanilla tree) vs vanilla-moved
print("=== BROKEN VANILLA IMPORTS (moved/removed in 26.2) ===")
remap = {}
for fqn, info in sorted(broken.items()):
    if fqn.startswith(("net.neoforged.", "thaumcraft.")): continue
    cands = name_to_fqns.get(fqn.split(".")[-1], set())
    if len(cands) == 1:
        remap[fqn] = next(iter(cands))
    tag = "REMAP->" + next(iter(cands)) if len(cands)==1 else (f"CANDS={sorted(cands)}" if cands else "GONE")
    print(f"{info['count']:4d}  {fqn:60s} {tag}")

print(f"\nTotal broken vanilla imports (unique FQN): "
      f"{sum(1 for f in broken if not f.startswith(('net.neoforged.','thaumcraft.')))}")
print(f"Auto-remappable (unique candidate): {len(remap)}")

with open("/tmp/import_remap.txt","w") as f:
    for old,new in sorted(remap.items()):
        f.write(f"{old}\t{new}\n")

print("\n=== BROKEN NEOFORGE/TC IMPORTS (need manual handling) ===")
for fqn, info in sorted(broken.items()):
    if not fqn.startswith(("net.neoforged.","thaumcraft.")): continue
    print(f"{info['count']:4d}  {fqn}")
