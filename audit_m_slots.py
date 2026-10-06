#!/usr/bin/env python3
"""Audit M: menu/container slot-index safety sweep.

Bug class: "Slot N is out of bounds. Size is M" - a menu (or tile-backed container)
asks for a slot index outside the backing list.  This is the class behind the user's
arcane-workbench crash report.

For every AbstractContainerMenu subclass we work out:
  * how many slots the menu registers (addSlot calls, loops expanded)
  * which container each slot reads from, and that container's declared size
  * every raw index expression handed to a container (getItem/setItem/removeItem/
    getStackInSlot/slots.get) and whether it can exceed the container size
"""
import re, glob, os, sys

ROOT = "src/main/java"

def read(p):
    return open(p, encoding="utf-8").read()

def const_table(src):
    """int constant name -> value declared in this file (public static final int ...)."""
    out = {}
    for m in re.finditer(r"\bint\s+([A-Z_0-9]+)\s*=\s*([-\w\s+*/().]+?)[;,]", src):
        name, expr = m.group(1), m.group(2)
        try:
            out[name] = int(eval(expr, {"__builtins__": {}}, dict(out)))
        except Exception:
            pass
    for m in re.finditer(r"\bint\[\]\s+([A-Z_0-9]+)\s*=\s*\{([^}]*)\}", src):
        out[m.group(1)] = len([x for x in m.group(2).split(",") if x.strip()])
    return out

def container_sizes():
    """Map container class name -> declared slot count (int, or None when dynamic)."""
    sizes = {}
    for p in glob.glob(f"{ROOT}/**/*.java", recursive=True):
        src = read(p)
        cls = os.path.basename(p)[:-5]
        m = re.search(r"getContainerSize\(\)\s*\{[^}]*return\s+([^;]+);", src)
        if m:
            consts = const_table(src)
            expr = m.group(1).strip()
            expr = re.sub(r"(\w+)\.(\w+)", lambda mm: mm.group(2), expr)
            try:
                sizes[cls] = int(eval(expr, {"__builtins__": {}}, consts))
            except Exception:
                sizes[cls] = None
    return sizes

def tile_sizes():
    """Tile*Inventory-style backing list sizes."""
    sizes = {}
    for p in glob.glob(f"{ROOT}/**/*.java", recursive=True):
        src = read(p)
        cls = os.path.basename(p)[:-5]
        for m in re.finditer(r"NonNullList\.withSize\(([^,)]+)", src):
            consts = const_table(src)
            expr = re.sub(r"(\w+)\.(\w+)", lambda mm: mm.group(2), m.group(1))
            try:
                sizes.setdefault(cls, int(eval(expr, {"__builtins__": {}}, consts)))
            except Exception:
                sizes.setdefault(cls, None)
    return sizes

def main():
    menus = sorted(glob.glob(f"{ROOT}/thaumcraft/common/menu/*Menu.java"))
    csizes, tsizes = container_sizes(), tile_sizes()
    problems = []
    for p in menus:
        src = read(p)
        cls = os.path.basename(p)[:-5]
        consts = const_table(src)
        # containers referenced by slots
        containers = set(re.findall(r"new\s+\w*Slot\w*\(\s*(\w+),", src))
        containers |= set(re.findall(r"new\s+\w*Slot\w*\(\s*\w+\s*,\s*(\w+)\s*,", src))
        print(f"=== {cls}")
        print(f"    consts: { {k:v for k,v in consts.items() if isinstance(v,int)} }")
        for c in sorted(containers):
            sz = csizes.get(c, tsizes.get(c, "?"))
            print(f"    container {c}: size {sz}")
        # addSlot count (loops expanded by their range when statically known)
        total, notes = 0, []
        def num(tok):
            tok = tok.strip()
            if tok in consts and isinstance(consts[tok], int):
                return consts[tok]
            try:
                return int(tok)
            except Exception:
                return None
        for m in re.finditer(r"for\s*\(\s*int\s+(\w+)\s*=\s*([\w]+)\s*;\s*(\w+)\s*([<>]=?)\s*([\w]+)\s*;", src):
            var, lo, _, op, hi = m.groups()
            lv, hv = num(lo), num(hi)
            if lv is not None and hv is not None:
                n = (hv - lv + 1) if op.endswith("=") else (hv - lv)
                notes.append(f"loop {var} {lv}..{hv}{op} -> {n}")
        for m in re.finditer(r"addSlot\(", src):
            total += 1
        print(f"    addSlot( occurrences: {total} (loops: {'; '.join(notes)})")
        # raw index expressions handed to a container
        raw = []
        for m in re.finditer(r"\.(getItem|setItem|removeItem|removeItemNoUpdate|getStackInSlot|setStackInSlot)\(\s*([^,)]+)", src):
            idx = m.group(2).strip()
            val = None
            try:
                val = int(eval(idx, {"__builtins__": {}}, consts))
            except Exception:
                pass
            if val is not None and m.group(1) in ("getItem", "setItem", "removeItem"):
                raw.append((m.group(1), idx, val))
            if val is not None and val < 0:
                problems.append(f"{cls}: negative slot index {idx}")
            if val is not None:
                for c in sorted(containers):
                    sz = csizes.get(c, tsizes.get(c))
                    if isinstance(sz, int) and val >= sz:
                        problems.append(f"{cls}: {m.group(1)}({idx}) on {c} (size {sz})")
    print("\n=== PROBLEMS ===")
    for x in problems:
        print("  " + x)
    if not problems:
        print("  none detected by static index evaluation")

if __name__ == "__main__":
    main()
