#!/usr/bin/env python3
"""Audit K: every reference inside the research JSONs must resolve.

Research entries (assets/thaumcraft/research/*.json) reference four kinds of thing:

  parents / required_research  -> a research key            (FUNCTIONAL gate)
  required_item / required_craft -> an item or item tag     (FUNCTIONAL gate)
  recipes                       -> a recipe id shown on the page (DISPLAY only)

A research key that no research JSON defines must be granted by code
(addResearch / completeResearch / a Scan* registration), otherwise the gate can
never be satisfied and the entry - and everything hanging off it - is stuck.
That is the same bug class as the FIRSTSTEPS deadlock.

Usage: python3 tools/audit_k_research.py [--json]
Exit status: 0 when no FUNCTIONAL problems remain.
"""
import glob
import json
import os
import re
import sys
from collections import Counter

PROJ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = PROJ + "/src/main/resources"
RESEARCH_DIR = SRC + "/assets/thaumcraft/research"
# vanilla/neoforge data cache: stable in-repo copy first (see tools/run_all_audits.sh bootstrap)
_VDATA_CANDIDATES = (PROJ + "/.reference/vdata", "/tmp/vdata")
VDATA = next((c for c in _VDATA_CANDIDATES if os.path.isdir(c + "/vanilla/data")), _VDATA_CANDIDATES[0])

# ---------------------------------------------------------------- registries
tags = {}


def load_tags(root):
    for path in glob.glob(os.path.join(root, "**/tags/**/*.json"), recursive=True):
        m = re.search(r"data/([a-z0-9_]+)/tags/(item|block|fluid)/(.+)\.json$", path)
        if not m:
            continue
        ns, cat, name = m.group(1), m.group(2), m.group(3)
        try:
            data = json.load(open(path, encoding="utf-8"))
        except Exception:
            continue
        out = []
        for e in data.get("values", []):
            if isinstance(e, str):
                out.append(e)
            elif isinstance(e, dict):
                if "tag" in e:
                    out.append("#" + e["tag"])
                elif "id" in e:
                    out.append(e["id"])
        tags.setdefault((cat, ns + ":" + name), []).extend(out)


load_tags(SRC + "/data")
for extra in (VDATA + "/vanilla/data", VDATA + "/nf/data"):
    if os.path.isdir(extra):
        load_tags(extra)

registered = set()
for java in glob.glob(PROJ + "/src/main/java/thaumcraft/init/*.java"):
    txt = open(java, encoding="utf-8", errors="ignore").read()
    for m in re.finditer(r'register\w*\(\s*"([^"]+)"', txt):
        registered.add("thaumcraft:" + m.group(1))

vanilla_ids = set()
lang_path = VDATA + "/vanilla/assets/minecraft/lang/en_us.json"
if os.path.exists(lang_path):
    lang = json.load(open(lang_path, encoding="utf-8"))
    for k in lang:
        m = re.match(r"^(?:block|item)\.minecraft\.([a-z0-9_.]+)$", k)
        if m:
            vanilla_ids.add("minecraft:" + m.group(1))

# recipe ids = file paths under data/<ns>/recipe/, plus the leaf-name index and
# alias table RecipeRenderer.findRecipe() actually resolves through.
recipe_ids = set()
recipe_leaves = set()
for path in glob.glob(SRC + "/data/*/recipe/**/*.json", recursive=True):
    rel = os.path.relpath(path, SRC + "/data")
    parts = rel.split(os.sep)
    ns, rest = parts[0], parts[2:]
    rid = ns + ":" + "/".join(rest).removesuffix(".json")
    recipe_ids.add(rid)
    recipe_leaves.add(rid.split(":")[1].split("/")[-1])

RECIPE_RENDERER = PROJ + "/src/main/java/thaumcraft/client/gui/RecipeRenderer.java"
recipe_aliases = {}
if os.path.exists(RECIPE_RENDERER):
    txt = open(RECIPE_RENDERER, encoding="utf-8").read()
    m = re.search(r"RECIPE_ALIASES = Map\.ofEntries\((.*?)\);\n", txt, re.S)
    if m:
        for a, b in re.findall(r'Map\.entry\("([^"]+)",\s*"([^"]+)"\)', m.group(1)):
            recipe_aliases[a] = b
    recipe_leaves |= set(recipe_aliases.values())

# catalog recipes registered from Java (CommonInternals.registerCatalogRecipe)
for java in glob.glob(PROJ + "/src/main/java/**/*.java", recursive=True):
    txt = open(java, encoding="utf-8", errors="ignore").read()
    for m in re.finditer(r'registerCatalogRecipe\w*\(\s*"([^"]+)"', txt):
        recipe_leaves.add(m.group(1).split(":")[-1].split("/")[-1])


def recipe_resolves(ref):
    leaf = strip_ref(ref).split(":")[-1].split("/")[-1]
    if leaf in recipe_leaves:
        return True
    stripped = re.sub(r"_fake\d*$", "", leaf)  # stripFakeSuffix()
    return stripped in recipe_leaves or recipe_aliases.get(leaf) in recipe_leaves


# ---------------------------------------------------------------- research
research_keys = set()
entries = []
for path in sorted(glob.glob(RESEARCH_DIR + "/*.json")):
    doc = json.load(open(path, encoding="utf-8"))
    for e in doc.get("entries", []):
        e["_file"] = os.path.basename(path)
        entries.append(e)
        research_keys.add(e["key"])

# keys the Java side grants at runtime (hidden flags, milestones, scans)
granted = set()

# Aspect.java auto-registers `!<tag>` for every aspect (ScanAspect), so those
# hidden keys are granted by scanning an aspect rather than by a research entry.
aspect_src = PROJ + "/src/main/java/thaumcraft/api/aspects/Aspect.java"
if os.path.exists(aspect_src):
    for m in re.finditer(r'new Aspect\("([a-z_]+)"', open(aspect_src, encoding="utf-8").read()):
        granted.add("!" + m.group(1))
scan_keys = set()
for java in glob.glob(PROJ + "/src/main/java/**/*.java", recursive=True):
    txt = open(java, encoding="utf-8", errors="ignore").read()
    for m in re.finditer(r'(?:addResearch|completeResearch|addResearchWithRemoval)\s*\([^,]+,\s*"([^"]+)"', txt):
        granted.add(m.group(1))
    for m in re.finditer(r'new Scan\w*\(\s*"([^"]+)"', txt):
        scan_keys.add(m.group(1))
    for m in re.finditer(r'addResearch\("([^"]+)"\)', txt):
        granted.add(m.group(1))

# ---------------------------------------------------------------- helpers
def strip_ref(ref):
    """'thaumcraft:crystal_essence;1;{Aspects:[...]}' -> 'thaumcraft:crystal_essence'."""
    return ref.split(";")[0].strip()


def check_id(ref, kind):
    ref = strip_ref(ref)
    if ref.startswith("#"):
        name = ref[1:]
        ns, _, path = name.partition(":")
        return (kind, name) in tags and tags[(kind, name)] or (kind, name) in tags
    if not re.match(r"^[a-z0-9_.]+:[a-z0-9_/.]+$", ref):
        return False
    ns = ref.split(":")[0]
    if ns == "minecraft":
        return ref in vanilla_ids or ref in registered or ref in recipe_ids
    if ns == "thaumcraft":
        return ref in registered or ref in recipe_ids
    return True  # other namespaces (c:, neoforge:) validated by audit A


def research_ok(key):
    base = key.split("@")[0]
    # ResearchEntry.getParentsStripped() drops the `~` prefix before the check
    if base.startswith("~"):
        base = base[1:]
    return base in research_keys or base in granted or base in scan_keys


problems = []
for e in entries:
    key, loc0 = e["key"], e["_file"]

    for p in e.get("parents", []) or []:
        if not research_ok(p):
            problems.append(("parents", p, f"{loc0}:{key}", "FUNCTIONAL"))

    for i, st in enumerate(e.get("stages", []) or [], 1):
        where = f"{loc0}:{key}:s{i}"
        for field in ("required_item", "required_craft"):
            for ref in st.get(field, []) or []:
                if ref.startswith(("tag:", "oredict:")):
                    continue  # mapped through OREDICT_TAG_MAPPINGS in ResearchManager
                if not check_id(ref, "item"):
                    problems.append((field, ref, where, "FUNCTIONAL"))
        for ref in st.get("required_research", []) or []:
            if not research_ok(ref):
                problems.append(("required_research", ref, where, "FUNCTIONAL"))
        for ref in st.get("recipes", []) or []:
            if not recipe_resolves(ref):
                problems.append(("recipes", ref, where, "DISPLAY"))

func = [p for p in problems if p[3] == "FUNCTIONAL"]
disp = [p for p in problems if p[3] == "DISPLAY"]

print(f"research files: {len(glob.glob(RESEARCH_DIR + '/*.json'))}  "
      f"entries: {len(entries)}  functional problems: {len(func)}  display problems: {len(disp)}")
for kind, ref, where, cls in problems:
    print(f"  [{cls:9} {kind:16}] {ref:60} -> unresolved   ({where})")

print("\nby kind:", dict(Counter(p[1] for p in problems)))
sys.exit(1 if func else 0)
