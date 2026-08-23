#!/usr/bin/env python3
"""Extract distinct missing CLASSES from cannot-find-symbol errors, with the
import line that fails (so we know what to rename/replace)."""
import re, sys
from collections import Counter, defaultdict

log = "/tmp/tc_compile.log"
lines = open(log, encoding="utf-8", errors="replace").read().splitlines()

# Map: class name -> Counter of (import package) and count
cls_imports = defaultdict(Counter)   # class -> import line counter
cls_count = Counter()                # class -> total error count

i = 0
while i < len(lines):
    m = re.match(r"^(.*\.java):(\d+): error: cannot find symbol$", lines[i])
    if m:
        f = m.group(1)
        # find symbol line within next 4 lines
        for j in range(i+1, min(i+5, len(lines))):
            s = lines[j].strip()
            if s.startswith("symbol:"):
                sym = s.split(":",1)[1].strip()
                if sym.startswith("class "):
                    name = sym.split(None,1)[1].split("<")[0]
                    cls_count[name]+=1
                break
            if re.match(r"^.*\.java:\d+: error:", lines[j]):
                break
    i+=1

# Now find import statements that reference these missing classes across source
import pathlib
SRC = pathlib.Path("/Users/spencer/Documents/Thaumcraft-26.2/src/main/java")
imp_map = defaultdict(Counter)  # class -> import FQN counter
for p in SRC.rglob("*.java"):
    for ln in p.read_text(encoding="utf-8", errors="replace").splitlines():
        mm = re.match(r"^import (static )?([\w.]+);", ln)
        if mm:
            fqn = mm.group(2)
            clsname = fqn.split(".")[-1]
            if clsname in cls_count:
                imp_map[clsname][fqn]+=1

print(f"{'COUNT':>6}  MISSING CLASS -> import FQN(s) in source")
for name, c in cls_count.most_common(60):
    imps = ", ".join(f"{k}({v})" for k,v in imp_map[name].most_common(3))
    print(f"{c:6d}  {name:<40} <- {imps}")
