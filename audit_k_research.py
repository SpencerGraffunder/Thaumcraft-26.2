#!/usr/bin/env python3
"""Audit K: research JSON data integrity + 1.12 parity.

Checks every research entry/stage in src/main/resources/assets/thaumcraft/research/
against the 1.12 originals and against what the port actually registers:
  K1  entry keys present in 1.12 but missing in the port (and vice versa)
  K2  entry field drift: category, location, parents, siblings, meta, stage count
  K3  required_craft / required_item ids -> registered items or valid tags
  K4  required_research keys -> existing research keys
  K5  required_knowledge strings -> parseable TYPE;[CATEGORY;]N
  K6  stage/addendum `recipes` ids -> recipe ids that exist in the datapack
  K7  icons -> texture files that exist
  K8  stage/addendum `text` lang keys -> present in en_us.json
"""
import json
import glob
import os
import re
import collections

ROOT = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ROOT, "src/main/resources")
TC_RESEARCH = os.path.join(RES, "assets/thaumcraft/research")
TC112_RESEARCH = "/tmp/tc112data/assets/thaumcraft/research"
TC112_LANG = "/tmp/tc112data/assets/thaumcraft/lang/en_us.lang"

import subprocess


def find_registered_items():
    out = set()
    for path in ("src/main/java/thaumcraft/init/ModItems.java",
                 "src/main/java/thaumcraft/init/ModBlocks.java"):
        src = open(os.path.join(ROOT, path)).read()
        for m in re.finditer(r'register(?:Item|Block|BlockItem)?\(\s*"([^"]+)"', src):
            out.add("thaumcraft:" + m.group(1))
    # items that only exist as block items are registered through ModBlocks too
    return out


def find_tag_contents():
    import vanilla_data
    d = vanilla_data.load()
    tags = {}
    for key, vals in d["tags"].items():
        tags.setdefault(key, set(vals))
    for path in glob.glob(os.path.join(RES, "data/**/tags/**/*.json"), recursive=True):
        parts = path.split(os.sep)
        i = parts.index("tags")
        ns, kind, name = parts[i - 1], parts[i + 1], os.path.splitext(parts[-1])[0]
        if kind != "item":
            continue
        try:
            data = json.load(open(path))
        except Exception:
            continue
        vals = tags.setdefault(f"{ns}:{name}", set())
        for e in data.get("values", []):
            if isinstance(e, str):
                vals.add(e)
            elif isinstance(e, dict):
                vals.add(e.get("id", ""))
    # expand nested tags
    for _ in range(4):
        for key, vals in list(tags.items()):
            for v in list(vals):
                if v.startswith("#"):
                    vals |= tags.get(v[1:], set())
    return tags


def vanilla_items():
    import vanilla_data
    return set(vanilla_data.load()["vanilla_items"])


def load_entries(directory):
    out = {}
    for path in sorted(glob.glob(os.path.join(directory, "*.json"))):
        data = json.load(open(path))
        for entry in data.get("entries", []):
            out[entry["key"]] = entry
    return out


def norm_req(value):
    """Normalise a 1.12 oredict/item string to the port equivalent where possible."""
    if isinstance(value, list):
        return tuple(norm_req(v) for v in value)
    if not isinstance(value, str):
        return value
    v = value.strip()
    if v.startswith("oredict:"):
        name = v.split(":", 1)[1]
        return ("oredict", name)
    if v.startswith("#"):
        return ("tag", v[1:])
    if ":" in v:
        ns, path = v.split(":", 1)
        return ("id", ns, path)
    return ("raw", v)


def main():
    port = load_entries(TC_RESEARCH)
    old = load_entries(TC112_RESEARCH)

    items = find_registered_items() | vanilla_items()
    tags = find_tag_contents()
    lang = json.load(open(os.path.join(RES, "assets/thaumcraft/lang/en_us.json")))

    recipe_ids = set()
    for path in glob.glob(os.path.join(RES, "data/thaumcraft/recipe/**/*.json"), recursive=True):
        recipe_ids.add("thaumcraft:" + os.path.splitext(os.path.basename(path))[0])
    for path in glob.glob(os.path.join(RES, "data/minecraft/recipe/**/*.json"), recursive=True):
        recipe_ids.add("minecraft:" + os.path.splitext(os.path.basename(path))[0])

    textures = set()
    for path in glob.glob(os.path.join(RES, "assets/thaumcraft/textures/**/*.png"), recursive=True):
        rel = os.path.relpath(path, os.path.join(RES, "assets")).replace(os.sep, "/")
        textures.add(rel)

    # mappings the port itself applies at runtime (ResearchManager.java)
    java_src = open(os.path.join(ROOT,
        "src/main/java/thaumcraft/common/lib/research/ResearchManager.java")).read()
    legacy_map = dict(re.findall(r'LEGACY_ITEM_MAPPINGS\.put\("([^"]+)",\s*"([^"]+)"\)', java_src))
    oredict_alias = dict(re.findall(r'OREDICT_TAG_MAPPINGS\.put\("([^"]+)",\s*"([^"]+)"\)', java_src))
    print("=== K1: research entry parity (1.12 vs port) ===")
    missing = sorted(set(old) - set(port))
    extra = sorted(set(port) - set(old))
    print(f"1.12 entries: {len(old)}  port entries: {len(port)}")
    print(f"missing from port ({len(missing)}): {missing}")
    print(f"port-only ({len(extra)}): {extra}")

    print("\n=== K2: entry field drift ===")
    drift = 0
    for key in sorted(set(old) & set(port)):
        a, b = old[key], port[key]
        diffs = []
        if a.get("category") != b.get("category"):
            diffs.append(f"category {a.get('category')} -> {b.get('category')}")
        if tuple(a.get("location", [])) != tuple(b.get("location", [])):
            diffs.append(f"location {a.get('location')} -> {b.get('location')}")
        if sorted(a.get("parents", [])) != sorted(b.get("parents", [])):
            diffs.append(f"parents {sorted(a.get('parents', []))} -> {sorted(b.get('parents', []))}")
        if sorted(a.get("siblings", [])) != sorted(b.get("siblings", [])):
            diffs.append(f"siblings {sorted(a.get('siblings', []))} -> {sorted(b.get('siblings', []))}")
        if sorted(a.get("meta", [])) != sorted(b.get("meta", [])):
            diffs.append(f"meta {sorted(a.get('meta', []))} -> {sorted(b.get('meta', []))}")
        sa, sb = a.get("stages", []), b.get("stages", [])
        if len(sa) != len(sb):
            diffs.append(f"stages {len(sa)} -> {len(sb)}")
        else:
            for i, (x, y) in enumerate(zip(sa, sb), 1):
                if norm_req(x.get("required_craft", [])) != norm_req(y.get("required_craft", [])):
                    diffs.append(f"stage{i} required_craft {x.get('required_craft')} -> {y.get('required_craft')}")
                if norm_req(x.get("required_item", [])) != norm_req(y.get("required_item", [])):
                    diffs.append(f"stage{i} required_item {x.get('required_item')} -> {y.get('required_item')}")
                if sorted(x.get("required_research", [])) != sorted(y.get("required_research", [])):
                    diffs.append(f"stage{i} required_research {sorted(x.get('required_research', []))} -> {sorted(y.get('required_research', []))}")
                if sorted(x.get("required_knowledge", [])) != sorted(y.get("required_knowledge", [])):
                    diffs.append(f"stage{i} required_knowledge {sorted(x.get('required_knowledge', []))} -> {sorted(y.get('required_knowledge', []))}")
                if x.get("warp", 0) != y.get("warp", 0):
                    diffs.append(f"stage{i} warp {x.get('warp', 0)} -> {y.get('warp', 0)}")
        if diffs:
            drift += 1
            print(f"{key}:")
            for d in diffs:
                print(f"   {d}")
    print(f"entries with drift: {drift}")

    print("\n=== K3: required_craft / required_item resolve to real items ===")
    bad = 0
    for key, entry in sorted(port.items()):
        for i, stage in enumerate(entry.get("stages", []), 1):
            for field in ("required_craft", "required_item"):
                for req in stage.get(field, []):
                    base = req.split(";")[0] if isinstance(req, str) else req
                    kind = norm_req(base)
                    ok, why = False, ""
                    if kind[0] == "id":
                        ns, path = kind[1], kind[2]
                        full = f"{ns}:{path}"
                        if full not in items and full in legacy_map:
                            full = legacy_map[full]
                        ok = full in items
                        why = f"unknown item id (legacy map: {legacy_map.get(f'{ns}:{path}')})" if (not ok and full in legacy_map) else "unknown item id"
                    elif kind[0] == "tag":
                        tag = kind[1]
                        if tag in tags:
                            ok = len(tags[tag]) > 0
                            why = "empty tag" if not ok else ""
                        elif tag in oredict_alias:
                            alias = oredict_alias[tag]
                            if alias in tags:
                                ok = len(tags[alias]) > 0
                                why = f"alias {alias} empty" if not ok else ""
                            else:
                                ok = alias in items
                                why = f"alias {alias} unknown item" if not ok else ""
                        else:
                            why = "unknown tag"
                    elif kind[0] == "oredict":
                        name = kind[1]
                        if name in tags:
                            ok = len(tags[name]) > 0
                        elif name in oredict_alias:
                            alias = oredict_alias[name]
                            ok = alias in tags and len(tags[alias]) > 0 or alias in items
                            why = f"no port equivalent for oredict:{name} (alias {alias})" if not ok else ""
                        else:
                            why = f"no port equivalent for oredict:{name}"
                    else:
                        why = f"unparseable requirement {req!r}"
                    if not ok:
                        bad += 1
                        print(f"{key} stage{i} {field}: {req} -> {why}")
    print(f"unresolvable requirements: {bad}")

    print("\n=== K4: required_research / parents / siblings reference real research keys ===")
    all_keys = set(port)
    java_src2 = open(os.path.join(ROOT,
        "src/main/java/thaumcraft/common/lib/research/ResearchManager.java")).read()
    grep_out = subprocess.run(
        ["grep", "-rhoE", r'"(!|f_|m_|~)[A-Za-z0-9_]+"', os.path.join(ROOT, "src/main/java"),
         "--include=*.java"], capture_output=True, text=True).stdout
    runtime = {t.strip('"') for t in grep_out.split()}
    aspect_tags = set(re.findall(r'new Aspect\("([a-z]+)"',
        open(os.path.join(ROOT, "src/main/java/thaumcraft/api/aspects/Aspect.java")).read()))
    runtime |= {"!" + t for t in aspect_tags}
    unresolved = collections.Counter()
    for key, entry in sorted(port.items()):
        refs = []
        for field in ("parents", "siblings"):
            refs += [(field, r) for r in entry.get(field, [])]
        for i, stage in enumerate(entry.get("stages", []), 1):
            refs += [(f"stage{i} required_research", r) for r in stage.get("required_research", [])]
        for i, ad in enumerate(entry.get("addenda", []), 1):
            refs += [(f"addendum{i} required_research", r) for r in ad.get("required_research", [])]
        for where, ref in refs:
            base = ref.split(";")[0]
            base = base[1:] if base.startswith("~") else base
            if "@" in base:
                base = base.split("@")[0]
            if base in all_keys or base in runtime:
                continue
            unresolved[base] += 1
            print(f"{key} {where}: {ref} (no such research key)")
    print(f"distinct unresolved research refs: {len(unresolved)}")

    print("\n=== K5: required_knowledge strings ===")
    bad = 0
    for key, entry in sorted(port.items()):
        for i, stage in enumerate(entry.get("stages", []), 1):
            for k in stage.get("required_knowledge", []):
                parts = k.split(";")
                if len(parts) not in (2, 3):
                    bad += 1
                    print(f"{key} stage{i}: bad knowledge string {k!r}")
                    continue
                if parts[0] not in ("OBSERVATION", "THEORY"):
                    bad += 1
                    print(f"{key} stage{i}: unknown knowledge type {k!r}")
    print(f"bad knowledge strings: {bad}")

    print("\n=== K6: stage/addendum `recipes` ids exist in the datapack ===")
    bad = 0
    total = 0
    for key, entry in sorted(port.items()):
        for i, stage in enumerate(entry.get("stages", []), 1):
            for r in stage.get("recipes", []):
                total += 1
                if r not in recipe_ids:
                    bad += 1
                    print(f"{key} stage{i}: recipe {r} not in datapack")
        for i, ad in enumerate(entry.get("addenda", []), 1):
            for r in ad.get("recipes", []):
                total += 1
                if r not in recipe_ids:
                    bad += 1
                    print(f"{key} addendum{i}: recipe {r} not in datapack")
    print(f"recipe refs: {total}  broken: {bad}")

    print("\n=== K7: icons point at existing textures ===")
    bad = 0
    for key, entry in sorted(port.items()):
        for icon in entry.get("icons", []):
            loc = icon
            if ":" in loc:
                ns, path = loc.split(":", 1)
                if ns != "thaumcraft":
                    continue
            else:
                path = loc
            if not path.startswith("textures/"):
                path = "textures/" + path
            if not path.endswith(".png"):
                path += ".png"
            if path not in textures:
                bad += 1
                print(f"{key}: icon {icon} -> missing assets/{path}")
    print(f"broken icons: {bad}")

    print("\n=== K8: stage/addendum text lang keys ===")
    old_lang = {}
    for line in open(TC112_LANG):
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        old_lang[k] = v
    bad = 0
    for key, entry in sorted(port.items()):
        for i, stage in enumerate(entry.get("stages", []), 1):
            t = stage.get("text")
            if t and t not in lang:
                bad += 1
                fallback = old_lang.get(t, "")
                print(f"{key} stage{i}: missing lang key {t}  (1.12 text: {fallback[:70]!r})")
        for i, ad in enumerate(entry.get("addenda", []), 1):
            t = ad.get("text")
            if t and t not in lang:
                bad += 1
                fallback = old_lang.get(t, "")
                print(f"{key} addendum{i}: missing lang key {t}  (1.12 text: {fallback[:70]!r})")
        n = entry.get("name")
        if n and n not in lang:
            bad += 1
            print(f"{key}: missing name lang key {n}  (1.12: {old_lang.get(n, '')!r})")
    print(f"missing research lang keys: {bad}")


if __name__ == "__main__":
    main()
