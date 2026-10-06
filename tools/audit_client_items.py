#!/usr/bin/env python3
"""Audit 26.3 client item definitions (assets/thaumcraft/items/<id>.json).

In NeoForge 26.3 every item resolves its model through a client item definition
in assets/<ns>/items/<name>.json. A missing definition makes the client log
"Missing item model for location thaumcraft:<name>" and the item renders as the
missing-model sprite.

This script derives the authoritative item id list from the registration source
(ModItems.registerItem + ModBlocks.registerBlock/registerBlockItem, minus
registerBlockNoItem) and reports:
  * items with no client item definition
  * client item definitions pointing at a model file that does not exist
  * client item definitions / models with no registered item (stale leftovers)

Usage:
  python3 tools/audit_client_items.py          # report
  python3 tools/audit_client_items.py --fix    # write missing definitions
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources/assets/thaumcraft")
ITEMS_DIR = os.path.join(RES, "items")
MODELS_ITEM = os.path.join(RES, "models/item")
MODELS_BLOCK = os.path.join(RES, "models/block")
BLOCKSTATES = os.path.join(RES, "blockstates")
NS = "thaumcraft"


def registered_items():
    items = set()
    blocks_no_item = set()

    mi = open(os.path.join(ROOT, "src/main/java/thaumcraft/init/ModItems.java")).read()
    items |= set(re.findall(r'registerItem\(\s*"([a-z0-9_]+)"', mi))

    mb = open(os.path.join(ROOT, "src/main/java/thaumcraft/init/ModBlocks.java")).read()
    # registerBlockNoItem(...) names must NOT have an item
    for m in re.finditer(r'registerBlockNoItem\(\s*"([a-z0-9_]+)"', mb):
        blocks_no_item.add(m.group(1))
    for m in re.finditer(r'registerBlock\w*\(\s*"([a-z0-9_]+)"', mb):
        name = m.group(1)
        if name not in blocks_no_item:
            items.add(name)

    # Anything registered straight on BLOCK_ITEMS
    for m in re.finditer(r'BLOCK_ITEMS\.register\(\s*"([a-z0-9_]+)"', mb):
        items.add(m.group(1))
    return items


def main():
    fix = "--fix" in sys.argv
    items = registered_items()
    have_def = {f[:-5] for f in os.listdir(ITEMS_DIR) if f.endswith(".json")} if os.path.isdir(ITEMS_DIR) else set()
    have_item_model = {f[:-5] for f in os.listdir(MODELS_ITEM) if f.endswith(".json")}
    have_block_model = {f[:-5] for f in os.listdir(MODELS_BLOCK) if f.endswith(".json")} if os.path.isdir(MODELS_BLOCK) else set()

    missing_def = sorted(items - have_def)
    stale_def = sorted(have_def - items)

    bad_target = []
    for name in sorted(have_def):
        path = os.path.join(ITEMS_DIR, name + ".json")
        try:
            data = json.load(open(path))
        except Exception as e:
            bad_target.append((name, f"unparseable: {e}"))
            continue
        model = (data.get("model") or {}).get("model", "")
        if not model:
            bad_target.append((name, "no model reference"))
            continue
        loc = model.split(":", 1)
        target = loc[1] if len(loc) == 2 else model
        base = target.split("/")[-1]
        kind = target.split("/")[0]
        exists = have_item_model if kind == "item" else have_block_model
        if base not in exists:
            bad_target.append((name, f"model {model} does not exist"))

    print(f"registered items: {len(items)}")
    print(f"client item definitions: {len(have_def)}")
    print(f"MISSING definitions: {len(missing_def)}")
    for n in missing_def:
        print(f"   - {n}   (models/item/{n}.json {'present' if n in have_item_model else 'MISSING'})")
    print(f"definitions whose model target does not exist: {len(bad_target)}")
    for n, why in bad_target:
        print(f"   - {n}: {why}")
    print(f"stale definitions (no registered item): {len(stale_def)}")
    print(f"   {stale_def}")

    if fix:
        os.makedirs(ITEMS_DIR, exist_ok=True)
        written = 0
        for name in missing_def:
            target = f"{NS}:item/{name}" if name in have_item_model else f"{NS}:block/{name}"
            with open(os.path.join(ITEMS_DIR, name + ".json"), "w") as fh:
                json.dump({"model": {"type": "minecraft:model", "model": target}}, fh, indent=2)
                fh.write("\n")
            written += 1
            if name not in have_item_model and name not in have_block_model:
                print(f"   !! {name}: wrote definition but no model file exists")
        print(f"wrote {written} client item definitions")


if __name__ == "__main__":
    main()
