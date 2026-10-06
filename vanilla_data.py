#!/usr/bin/env python3
"""Extract vanilla + NeoForge registries/tags from the mapped jars used by the build.

Cached to /tmp/tc_registries.json so audits run fast.
"""
import json
import os
import re
import subprocess
import sys

MC_JAR = "/Users/spencer/Documents/Thaumcraft-26.2/build/neoForm/neoFormJoined26.3-1/steps/applyInterfaceInjections/outputs.jar"
NF_JARS = [
    "/Users/spencer/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.3.0.33-beta/cf880584d71fa251da5addaf5475de4d9ac79c5f/neoforge-26.3.0.33-beta-universal.jar",
]
CACHE = "/tmp/tc_registries.json"


def listing(jar):
    out = subprocess.run(["zipinfo", "-1", jar], capture_output=True, text=True).stdout
    return out.splitlines()


def tag_files(lines, prefix):
    return [l for l in lines if l.startswith(prefix) and l.endswith(".json")]


def read_json(jar, path):
    p = subprocess.run(["unzip", "-p", jar, path], capture_output=True, text=True)
    try:
        return json.loads(p.stdout)
    except Exception:
        return None


def build():
    data = {"vanilla_items": [], "vanilla_blocks": [], "tags": {}}
    lines = listing(MC_JAR)
    items = set()
    for l in tag_files(lines, "assets/minecraft/models/item/"):
        items.add("minecraft:" + os.path.basename(l)[:-5])
    for l in tag_files(lines, "assets/minecraft/models/block/"):
        name = "minecraft:" + os.path.basename(l)[:-5]
        data["vanilla_blocks"].append(name)
        items.add(name)
    # every recipe result is definitely an item
    for l in tag_files(lines, "data/minecraft/recipe/"):
        d = read_json(MC_JAR, l)
        if not d:
            continue
        r = d.get("result")
        if isinstance(r, str):
            items.add(r if ":" in r else "minecraft:" + r)
        elif isinstance(r, dict) and "id" in r:
            items.add(r["id"])
        elif isinstance(d.get("result"), dict):
            pass
    # vanilla item tags
    tags = {}
    for jar in [MC_JAR] + [j for j in NF_JARS if os.path.exists(j)]:
        jl = listing(jar)
        for l in tag_files(jl, "data/"):
            m = re.match(r"data/([^/]+)/tags/item/(.+)\.json$", l)
            if not m:
                continue
            ns, name = m.group(1), m.group(2)
            d = read_json(jar, l)
            if d is None:
                continue
            key = f"{ns}:{name}"
            vals = tags.setdefault(key, [])
            for e in d.get("values", []):
                if isinstance(e, str):
                    vals.append(e)
                elif isinstance(e, dict):
                    vals.append(e.get("id", ""))
    data["vanilla_items"] = sorted(items)
    data["tags"] = tags
    json.dump(data, open(CACHE, "w"))
    return data


def load():
    if os.path.exists(CACHE):
        return json.load(open(CACHE))
    return build()


if __name__ == "__main__":
    d = load()
    print(f"vanilla items: {len(d['vanilla_items'])}")
    print(f"vanilla blocks: {len(d['vanilla_blocks'])}")
    print(f"item tags: {len(d['tags'])}")
