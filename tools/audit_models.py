#!/usr/bin/env python3
"""Audit model/texture wiring for the Thaumcraft 26.3 port.

Checks, statically (no game launch needed):
  1. every registered item has a client item definition  (see audit_client_items.py)
  2. every client item definition points at a model that exists
  3. every model referenced by an item definition resolves (parent chain) and
     every thaumcraft: texture it needs exists on disk
  4. every registered block has a blockstate file whose model(s) exist and whose
     textures exist

Usage: python3 tools/audit_models.py
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources/assets/thaumcraft")
NS = "thaumcraft"

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from audit_client_items import registered_items  # noqa: E402


def load_json_tree(subdir):
    out = {}
    base = os.path.join(RES, subdir)
    if not os.path.isdir(base):
        return out
    for p in glob_all(base, ".json"):
        rel = os.path.relpath(p, RES)[:-5].replace(os.sep, "/")
        try:
            out[rel] = json.load(open(p))
        except Exception as e:
            out[rel] = {"__error__": str(e)}
    return out


def glob_all(base, suffix):
    for dirpath, _dirs, files in os.walk(base):
        for f in files:
            if f.endswith(suffix):
                yield os.path.join(dirpath, f)


def main():
    textures = {os.path.relpath(p, RES)[:-4].replace(os.sep, "/")
                for p in glob_all(os.path.join(RES, "textures"), ".png")}
    models = load_json_tree("models")
    blockstates = load_json_tree("blockstates")
    item_defs = load_json_tree("items")

    def model_textures(rel, seen):
        if rel in seen or rel not in models:
            return set()
        seen.add(rel)
        m = models[rel]
        out = set()
        parent = m.get("parent")
        if isinstance(parent, str) and parent.startswith(NS + ":"):
            out |= model_textures("models/" + parent.split(":", 1)[1], seen)
        for v in (m.get("textures") or {}).values():
            if isinstance(v, str):
                if v.startswith(NS + ":"):
                    out.add(v.split(":", 1)[1])
                elif ":" not in v:
                    out.add(v)
        return out

    problems = []

    # 2 + 3: item definitions
    for name, data in sorted(item_defs.items()):
        model = (data.get("model") or {}).get("model", "")
        if not model:
            problems.append(f"item def {name}: no model reference")
            continue
        rel = "models/" + model.split(":", 1)[1] if model.startswith(NS + ":") else None
        if rel is None:
            continue  # vanilla model reference
        if rel not in models:
            problems.append(f"item def {name}: model {model} does not exist")
            continue
        for tex in model_textures(rel, set()):
            if "textures/" + tex not in textures:
                problems.append(f"item def {name}: model {rel} needs missing texture {tex}")

    def check_entry(name, entry):
        if isinstance(entry, list):
            for sub in entry:
                check_entry(name, sub)
            return
        if not isinstance(entry, dict):
            return
        m = entry.get("model")
        if isinstance(m, str) and m.startswith(NS + ":"):
            rel = "models/" + m.split(":", 1)[1]
            if rel not in models:
                problems.append(f"blockstate {name}: model {m} does not exist")
            else:
                for tex in model_textures(rel, set()):
                    if "textures/" + tex not in textures:
                        problems.append(f"blockstate {name}: model {rel} needs missing texture {tex}")
        apply = entry.get("apply")
        if isinstance(apply, dict):
            check_entry(name, apply)
        elif isinstance(apply, list):
            for child in apply:
                check_entry(name, child)
        models_list = entry.get("models")
        if isinstance(models_list, list):
            for child in models_list:
                check_entry(name, child)

    # 4: blockstates
    for name, data in sorted(blockstates.items()):
        for _variant, entry in (data.get("variants") or {}).items():
            check_entry(name, entry)
        for part in (data.get("multipart") or []):
            check_entry(name, part)

    # registered blocks should have a blockstate
    import re
    mb = open(os.path.join(ROOT, "src/main/java/thaumcraft/init/ModBlocks.java")).read()
    no_item = set(re.findall(r'registerBlockNoItem\(\s*"([a-z0-9_]+)"', mb))
    blocks = set(re.findall(r'registerBlock\w*\(\s*"([a-z0-9_]+)"', mb)) - no_item
    missing_bs = sorted(b for b in blocks if "blockstates/" + b not in blockstates)

    print(f"item definitions: {len(item_defs)}  models: {len(models)}  blockstates: {len(blockstates)}  textures: {len(textures)}")
    print(f"registered blocks without a blockstate file: {len(missing_bs)}")
    for b in missing_bs:
        print(f"   - {b}")
    print(f"model/texture problems: {len(problems)}")
    for p in problems:
        print("   -", p)


if __name__ == "__main__":
    main()
