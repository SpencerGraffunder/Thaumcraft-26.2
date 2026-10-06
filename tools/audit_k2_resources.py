#!/usr/bin/env python3
"""Audit K2: research <-> recipe cross-reference integrity.

Checks, against the ids actually registered by the mod:

  1. Every `research` gate on a recipe JSON names a research key that exists in a
     research JSON (otherwise the recipe can never be crafted).
  2. Every `stages[].recipes[]` entry in a research JSON resolves to something the
     client can actually draw (recipe file id, FakeRecipes catalog entry, or a
     RECIPE_ALIASES entry) - otherwise the Thaumonomicon page shows a blank icon.
  3. Every registered item has a client item model definition (assets/thaumcraft/items).

Usage: python3 tools/audit_k2_resources.py [--jar path/to.jar]
"""
import argparse
import collections
import json
import os
import re
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src/main/java/thaumcraft")
RESEARCH_DIR = os.path.join(ROOT, "src/main/resources/assets/thaumcraft/research")
RECIPE_GLOB = os.path.join(ROOT, "src/main/resources/data/thaumcraft/recipe")

REGISTER_RE = re.compile(r'register[A-Za-z]*\(\s*"([a-z0-9_]+)"')
FAKE_ID_RE = re.compile(r'Identifier\.fromNamespaceAndPath\(\s*Thaumcraft\.MODID,\s*"([a-z0-9_]+)"')
ALIAS_RE = re.compile(r'Map\.entry\("([a-z0-9_]+)",\s*"([a-z0-9_]+)"\)')


def registered_ids():
    ids = set()
    for path in sorted(collections.OrderedDict.fromkeys(
            os.path.join(dirpath, name)
            for dirpath, _, names in os.walk(SRC)
            for name in names if name.endswith(".java"))):
        if "/init/" not in path:
            continue
        with open(path, encoding="utf-8", errors="ignore") as handle:
            for match in REGISTER_RE.finditer(handle.read()):
                ids.add(match.group(1))
    return ids


def registered_items_from_jar(jar_path):
    with zipfile.ZipFile(jar_path) as archive:
        return {
            name.rsplit("/", 1)[-1][:-5]
            for name in archive.namelist()
            if name.startswith("assets/thaumcraft/items/") and name.endswith(".json")
        }


def research_entries():
    entries = []
    for path in sorted(os.listdir(RESEARCH_DIR)):
        if not path.endswith(".json"):
            continue
        with open(os.path.join(RESEARCH_DIR, path), encoding="utf-8") as handle:
            entries.extend(json.load(handle).get("entries", []))
    return entries


def recipe_files():
    for dirpath, _, names in os.walk(RECIPE_GLOB):
        for name in names:
            if name.endswith(".json"):
                yield os.path.join(dirpath, name)


def fake_recipe_ids():
    """Ids in the runtime fake catalog: FakeRecipes.java plus every ConfigRecipes
    addFake("<id>", ...) call (which ends up in ThaumcraftApi.addFakeCraftingRecipe)."""
    fakes = set()
    for rel in ("common/lib/crafting/FakeRecipes.java", "common/config/ConfigRecipes.java"):
        path = os.path.join(SRC, rel)
        with open(path, encoding="utf-8", errors="ignore") as handle:
            source = handle.read()
        fakes.update(m.group(1) for m in FAKE_ID_RE.finditer(source))
        fakes.update(m.group(1) for m in re.finditer(r'addFake\(\s*"([a-z0-9_]+)"', source))
    return fakes


def recipe_aliases():
    path = os.path.join(SRC, "client/gui/RecipeRenderer.java")
    with open(path, encoding="utf-8", errors="ignore") as handle:
        return {m.group(1): m.group(2) for m in ALIAS_RE.finditer(handle.read())}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", help="installed jar, to check client item definitions")
    args = parser.parse_args()

    registered = registered_ids()
    entries = research_entries()
    keys = {e.get("key") for e in entries}
    fakes = fake_recipe_ids()
    aliases = recipe_aliases()
    leaves = {os.path.basename(p)[:-5] for p in recipe_files()}
    alias_targets = set(aliases.values())

    print(f"registered ids: {len(registered)}   research entries: {len(entries)}"
          f"   recipe files: {len(leaves)}   fake catalog: {len(fakes)}"
          f"   aliases: {len(aliases)}")

    # 1. recipe research gates
    gates = collections.defaultdict(list)
    for path in recipe_files():
        with open(path, encoding="utf-8") as handle:
            gate = json.load(handle).get("research")
        if gate:
            gates[gate].append(os.path.relpath(path, RECIPE_GLOB))
    bad_gates = {k: v for k, v in gates.items() if k.split("@")[0] not in keys}
    print(f"\n[1] recipe research gates: {len(gates)} distinct,"
          f" {len(bad_gates)} naming an unknown research key")
    for key in sorted(bad_gates):
        files = bad_gates[key]
        print(f"    {key:26s} {len(files)} recipe(s): {', '.join(files[:3])}"
              f"{' ...' if len(files) > 3 else ''}")

    # 2. research stage.recipes display ids
    bad_recipes = collections.defaultdict(set)
    for entry in entries:
        for index, stage in enumerate(entry.get("stages", []) or []):
            for recipe_id in stage.get("recipes") or []:
                leaf = recipe_id.split(":")[-1]
                stripped = re.sub(r"_fake(_\d+)?$", "", leaf)
                if (leaf in leaves or leaf in fakes or leaf in aliases
                        or stripped in leaves or leaf in alias_targets):
                    continue
                bad_recipes[leaf].add(f"{entry.get('key')}@{index + 1}")
    print(f"\n[2] research stage 'recipes' ids that resolve to nothing: {len(bad_recipes)}")
    for leaf in sorted(bad_recipes):
        print(f"    {leaf:26s} {', '.join(sorted(bad_recipes[leaf]))}")

    # 3. client item definitions
    if args.jar:
        have = registered_items_from_jar(args.jar)
        mod_items = set()
        with open(os.path.join(SRC, "init/ModItems.java"), encoding="utf-8", errors="ignore") as handle:
            for match in REGISTER_RE.finditer(handle.read()):
                mod_items.add(match.group(1))
        missing = sorted(mod_items - have)
        print(f"\n[3] registered items: {len(mod_items)}   with client item definition: "
              f"{len(mod_items) - len(missing)}   missing: {len(missing)}")
        if missing:
            print("    " + ", ".join(missing))

    return 1 if (bad_gates or bad_recipes) else 0


if __name__ == "__main__":
    sys.exit(main())
