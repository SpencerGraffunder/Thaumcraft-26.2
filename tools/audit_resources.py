#!/usr/bin/env python3
"""
audit_resources.py — Static reference audit for the Thaumcraft 26.2 mod.

Checks that every resource a model or Java class points at actually exists on disk:

  1. MODEL -> TEXTURE   every texture referenced by a model JSON exists as a .png
  2. JAVA  -> RESOURCE  every file-resource Identifier built in Java code resolves
  3. MODEL PARENT       every "parent" reference resolves to an existing model (or minecraft:)
  4. ORPHAN TEXTURES    (report only) textures never referenced by any model

Skips: template/variable refs (#side, $x, <x>), minecraft: vanilla refs,
       'missingtexture', and non-file Java refs (packet IDs, block/item IDs).

Usage:
  python3 tools/audit_resources.py            # full report
  python3 tools/audit_resources.py --missing  # only missing refs
  python3 tools/audit_resources.py --json     # machine-readable output
"""
import json
import os
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets"
MODELS_DIR = ASSETS / "thaumcraft" / "models"
JAVA_DIR = ROOT / "src" / "main" / "java"

# Java refs whose path starts with one of these are file resources worth checking.
RESOURCE_DIRS = ("textures", "sounds", "models", "research", "blocks", "items",
                 "blockstates", "loot_tables", "lang")

# --------------------------------------------------------------------------- helpers

def tex_path_for(ref: str):
    """Return (ns, Path) for a texture ref like 'thaumcraft:block/foo' or 'block/foo'."""
    if ref.count(":") == 1:
        ns, path = ref.split(":", 1)
    elif ref.count(":") == 0:
        ns, path = "thaumcraft", ref
    else:
        return None
    p = ASSETS / ns / "textures" / (ref.rsplit(":", 1)[-1] + ("" if ref.endswith(".png") else ".png"))
    return ns, p

def is_template(ref: str) -> bool:
    """Template/variable texture refs can't be checked as files."""
    return ref.startswith(("$", "<", "#")) or "{" in ref or ref == "missingtexture"

# --------------------------------------------------------------------------- 1+3: models

def load_models():
    models = {}
    for sub in ("block", "item", "obj"):
        for mp in MODELS_DIR.glob(f"{sub}/*.json"):
            rel = "thaumcraft:" + str(mp.relative_to(ASSETS / "thaumcraft")).replace(os.sep, "/")
            try:
                models[rel] = json.loads(mp.read_text())
            except Exception as e:
                models[rel] = {"__parse_error__": str(e)}
    return models

def _check_tex(ref, models_ref, texs, missing_tex, referenced):
    if not isinstance(ref, str) or is_template(ref):
        return
    res = tex_path_for(ref)
    if res is None:
        return
    ns, p = res
    if ns == "minecraft":
        return  # vanilla texture, assume exists
    if ns == "thaumcraft":
        referenced.add(ref)
    if not p.exists():
        missing_tex[ref].append(models_ref)

def audit_models(models):
    missing_tex = defaultdict(list)
    missing_parent = defaultdict(list)
    referenced = set()
    for mref, m in models.items():
        if "__parse_error__" in m:
            continue
        parent = m.get("parent")
        if isinstance(parent, str) and parent:
            if parent.startswith("minecraft:"):
                pass
            elif parent.count(":") == 1:
                ns, ppath = parent.split(":", 1)
                if not (ASSETS / ns / "models" / (ppath + ".json")).exists():
                    missing_parent[parent].append(mref)
            else:
                if not (ASSETS / "thaumcraft" / "models" / (parent + ".json")).exists():
                    missing_parent["thaumcraft:" + parent].append(mref)
        texs = m.get("textures") if isinstance(m.get("textures"), dict) else {}
        for slot, ref in texs.items():
            _check_tex(ref, mref, texs, missing_tex, referenced)
        for el in (m.get("elements") or []):
            for face in (el.get("faces") or {}).values():
                t = face.get("texture") if isinstance(face, dict) else None
                if isinstance(t, str):
                    _check_tex(t, mref, texs, missing_tex, referenced)
    return missing_tex, missing_parent, referenced

# --------------------------------------------------------------------------- 2: java

JAVA_IDENT_RE = re.compile(
    r'(?:Identifier\.fromNamespaceAndPath|new\s+Identifier|new\s+ResourceLocation)\s*\(\s*([^,]+?)\s*,\s*"([^"]+)"'
)
JAVA_STR_RE = re.compile(r'"(thaumcraft:[A-Za-z0-9_/\-]+\.(?:png|jpg|ogg|json))"')

def _is_file_resource(path: str) -> bool:
    return any(path.startswith(d + "/") for d in RESOURCE_DIRS)

def _has_ext(path: str) -> bool:
    return bool(re.search(r'\.(png|jpg|jpeg|ogg|json)$', path))

def _resolve_ns(expr: str):
    e = expr.strip()
    if e in ('"thaumcraft"', 'Thaumcraft.MODID', 'MODID', 'Thaumcraft.MOD_ID'):
        return 'thaumcraft'
    if e == '"minecraft"':
        return 'minecraft'
    return None

def audit_java():
    missing = defaultdict(list)
    checked = 0
    for jp in JAVA_DIR.rglob("*.java"):
        try:
            text = jp.read_text(errors="ignore")
        except Exception:
            continue
        rel = str(jp.relative_to(ROOT))
        for m in JAVA_IDENT_RE.finditer(text):
            ns = _resolve_ns(m.group(1))
            path = m.group(2)
            if ns is None or ns == 'minecraft':
                continue
            if not _is_file_resource(path) or not _has_ext(path):
                continue  # skip non-file refs & concatenation prefixes
            checked += 1
            if not (ASSETS / ns / path).exists():
                missing[path].append(rel)
        for m in JAVA_STR_RE.finditer(text):
            ref = m.group(1)
            ns, path = ref.split(":", 1)
            if not _is_file_resource(path):
                continue
            checked += 1
            if not (ASSETS / ns / path).exists():
                missing[ref].append(rel)
    return missing, checked

# --------------------------------------------------------------------------- 4: orphans

def audit_orphans(referenced):
    orphans = []
    for tp in (ASSETS / "thaumcraft" / "textures").rglob("*.png"):
        ref = "thaumcraft:" + str(tp.relative_to(ASSETS / "thaumcraft" / "textures")).rsplit(".png", 1)[0]
        if ref not in referenced:
            orphans.append(ref)
    return orphans

# --------------------------------------------------------------------------- main

def main():
    args = set(sys.argv[1:])
    models = load_models()
    missing_tex, missing_parent, referenced = audit_models(models)
    java_missing, java_checked = audit_java()
    orphans = audit_orphans(referenced)

    result = {
        "model_count": len(models),
        "model_missing_textures": dict(sorted(missing_tex.items())),
        "model_missing_parents": dict(sorted(missing_parent.items())),
        "java_missing": dict(sorted(java_missing.items())),
        "java_checked": java_checked,
        "orphan_textures": sorted(orphans),
    }

    if "--json" in args:
        print(json.dumps(result, indent=2))
        return

    print(f"=== RESOURCE AUDIT (models={len(models)}) ===\n")
    print(f"[1] MODEL -> TEXTURE  missing: {len(missing_tex)}")
    for ref, refs in sorted(missing_tex.items()):
        print(f"    MISSING {ref}   (used by {len(refs)}: {refs[0]}{' …' if len(refs)>1 else ''})")
    print(f"\n[2] JAVA -> RESOURCE  checked {java_checked} file-refs, missing {len(java_missing)}")
    for ref, refs in sorted(java_missing.items()):
        print(f"    MISSING {ref}   ({refs[0]}{' …' if len(refs)>1 else ''})")
    print(f"\n[3] MODEL PARENT  missing: {len(missing_parent)}")
    for ref, refs in sorted(missing_parent.items()):
        print(f"    MISSING parent {ref}   (used by {len(refs)}: {refs[0]}{' …' if len(refs)>1 else ''})")
    print(f"\n[4] ORPHAN textures (never in a model): {len(orphans)}  [many are loaded via Java; informational only]")
    for ref in orphans[:20]:
        print(f"    {ref}")
    if len(orphans) > 20:
        print(f"    … and {len(orphans)-20} more")

    total = len(missing_tex) + len(java_missing) + len(missing_parent)
    print(f"\nTOTAL missing file references: {total}")

if __name__ == "__main__":
    main()
