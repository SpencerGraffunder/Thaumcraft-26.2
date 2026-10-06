#!/usr/bin/env python3
"""Audit 3: every registered item/block must have a lang name and a model."""
import json, re, os, glob

PORT = "src/main/java/thaumcraft/init"
def registered(path, pat):
    src = open(path).read()
    return [m.group(1) for m in re.finditer(pat, src)]

items = registered(f"{PORT}/ModItems.java", r'registerItem\(\s*"([^"]+)"')
blocks = registered(f"{PORT}/ModBlocks.java", r'register(?:Block|BlockItem)\(\s*"([^"]+)"')
print(f"registered items: {len(items)}  registered blocks: {len(blocks)}")

lang = json.load(open("src/main/resources/assets/thaumcraft/lang/en_us.json"))
def has_name(n):
    return f"item.thaumcraft.{n}" in lang or f"block.thaumcraft.{n}" in lang \
        or f"item.thaumcraft.{n}.name" in lang or f"block.thaumcraft.{n}.name" in lang

missing_lang = [n for n in items + blocks if not has_name(n)]
print(f"\n=== {len(missing_lang)} registered names with no lang entry (raw key shown in game) ===")
for n in missing_lang: print(" ", n)

# models
item_models = {os.path.relpath(p, "src/main/resources/assets/thaumcraft/items")[:-5]
               for p in glob.glob("src/main/resources/assets/thaumcraft/items/*.json")}
block_models = {os.path.relpath(p, "src/main/resources/assets/thaumcraft/models/item")[:-5]
                for p in glob.glob("src/main/resources/assets/thaumcraft/models/item/*.json")}
blockstates = {os.path.relpath(p, "src/main/resources/assets/thaumcraft/blockstates")[:-5]
               for p in glob.glob("src/main/resources/assets/thaumcraft/blockstates/*.json")}
known = item_models | block_models | blockstates
no_model = [n for n in items if n not in known]
print(f"\n=== {len(no_model)} items with no item model / blockstate entry ===")
for n in no_model: print(" ", n)
no_block_state = [n for n in blocks if n not in known]
print(f"\n=== {len(no_block_state)} blocks with no blockstate/model file ===")
for n in no_block_state: print(" ", n)
json.dump({"missing_lang": missing_lang, "no_model": no_model, "no_blockstate": no_block_state},
          open("/tmp/reg_gaps.json","w"), indent=1)
