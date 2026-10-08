#!/usr/bin/env python3
"""Normalized dump of every 1.12 Thaumcraft recipe registration in ConfigRecipes.java."""
import json
import os
import re

SRC = os.environ.get("TC112_SRC", "/private/tmp/tc112full/thaumcraft/common/config/ConfigRecipes.java")
src = open(SRC).read()

_LINES = src.split("\n")
def _off(line):
    return len("\n".join(_LINES[:line - 1])) + 1 if line > 1 else 0
SECTIONS = [(_off(62), _off(185), "multiblock"), (_off(185), _off(468), "crucible"),
            (_off(468), _off(1797), "arcane"), (_off(1797), _off(2600), "infusion"),
            (_off(2600), _off(2911), "normal"), (_off(2911), _off(2972), "smelting")]

def balanced(s, i):
    depth, j = 0, i
    while j < len(s):
        c = s[j]
        if c == '"':
            j += 1
            while j < len(s) and s[j] != '"':
                if s[j] == '\\':
                    j += 1
                j += 1
        elif c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return s[i + 1:j], j + 1
        j += 1
    return s[i:], len(s)

def split_args(text):
    out, depth, cur, i = [], 0, "", 0
    while i < len(text):
        c = text[i]
        if c == '"':
            cur += c
            i += 1
            while i < len(text) and text[i] != '"':
                if text[i] == '\\':
                    cur += text[i]
                    i += 1
                cur += text[i]
                i += 1
            cur += '"'
            i += 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        if c == ',' and depth == 0:
            out.append(cur.strip())
            cur = ""
        else:
            cur += c
        i += 1
    if cur.strip():
        out.append(cur.strip())
    return out

def rid_of(arg):
    m = re.search(r'new ResourceLocation\(([^)]*)\)', arg)
    if not m:
        return arg.strip()
    parts = [p.strip().strip('"') for p in split_args(m.group(1))]
    if len(parts) == 1 and ":" in parts[0]:
        return parts[0].split(":", 1)[1]
    return "/".join(parts[1:]) if len(parts) > 1 else parts[0]

def strip_itemstack(tok):
    tok = tok.strip()
    m = re.fullmatch(r"new ItemStack\((.*)\)", tok, re.S)
    return m.group(1).strip() if m else tok

out = {}
for a, b, station in SECTIONS:
    line_start = src[:a].count("\n") + 1
    for m in re.finditer(r"(ThaumcraftApi\.add\w+Recipe|GameRegistry\.add\w+Recipe|GameRegistry\.addRecipe)\s*\(", src):
        if not (a <= m.start() < b):
            continue
        inner, after = balanced(src, m.end() - 1)
        args = split_args(inner)
        kind = m.group(1).split(".")[-1]
        rid = rid_of(args[0]) if args else "?"
        entry = {"station": station, "kind": kind, "line": src[:m.start()].count("\n") + 1}
        if "Arcane" in kind:
            body = inner
            mm = re.search(r"ShapelessArcaneRecipe\(|ShapedArcaneRecipe\(|ArcaneRecipe\(", body)
            if mm:
                inner2, _ = balanced(body, mm.end() - 1)
                a2 = split_args(inner2)
                entry["research"] = a2[1].strip('"') if len(a2) > 1 else ""
                entry["vis"] = a2[2].strip() if len(a2) > 2 else ""
                entry["aspects"] = re.findall(r"Aspect\.(\w+),\s*(\d+)", a2[3]) if len(a2) > 3 else []
                entry["result"] = strip_itemstack(a2[4]) if len(a2) > 4 else ""
                rest = a2[5:]
                if "Shapeless" in mm.group(0):
                    entry["ingredients"] = [strip_itemstack(x) for x in rest]
                else:
                    pats = [x.strip().strip('"') for x in rest if x.strip().startswith('"') and x.strip().endswith('"') and len(x.strip()) <= 6]
                    keys = {}
                    i = 0
                    while i < len(rest):
                        t = rest[i].strip()
                        if re.fullmatch(r"'.'", t) and i + 1 < len(rest):
                            keys[t.strip("'")] = strip_itemstack(rest[i + 1])
                            i += 2
                        else:
                            i += 1
                    entry["pattern"] = pats[:3]
                    entry["key"] = keys
        elif "Crucible" in kind or "Infusion" in kind:
            body = inner
            mm = re.search(r"(CrucibleRecipe|InfusionRecipe)\(", body)
            if mm:
                inner2, _ = balanced(body, mm.end() - 1)
                a2 = split_args(inner2)
                if "InfusionRecipe" in mm.group(0):
                    # (research, result, instability, aspects, center, *surrounding)
                    entry["research"] = a2[0].strip('"') if a2 else ""
                    entry["result"] = strip_itemstack(a2[1]) if len(a2) > 1 else ""
                    entry["instability"] = a2[2].strip() if len(a2) > 2 else ""
                    entry["aspects"] = re.findall(r"Aspect\.(\w+),\s*(\d+)", a2[3]) if len(a2) > 3 else []
                    entry["center"] = strip_itemstack(a2[4]) if len(a2) > 4 else ""
                    entry["ingredients"] = [strip_itemstack(x) for x in a2[5:]]
                else:
                    entry["research"] = a2[0].strip('"') if a2 else ""
                    entry["result"] = strip_itemstack(a2[1]) if len(a2) > 1 else ""
                    entry["ingredient"] = strip_itemstack(a2[2]) if len(a2) > 2 else ""
                    entry["aspects"] = re.findall(r"Aspect\.(\w+),\s*(\d+)", a2[3]) if len(a2) > 3 else []
                    tail = " ".join(a2[4:])
                    for extra in re.findall(r"\.(setMatchingItems|setInstability|setVis|setResearch)\(([^)]*)\)", tail):
                        entry.setdefault("extra", []).append(list(extra))
        else:
            entry["raw"] = " ".join(args)[:600]
        out.setdefault(rid, []).append(entry)

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "reference", "recipes_112.json")
json.dump(out, open(OUT, "w"), indent=1)
print("wrote", OUT)
from collections import Counter
print(Counter(v["station"] for vs in out.values() for v in vs))
print("total recipe ids:", len(out))
