#!/usr/bin/env python3
"""Audit: every registry id (entities, effects, items, blocks, sounds, enchantments,
potions...) must have a matching modern lang key in en_us.json.

Modern (26.x) key conventions:
  entity.<ns>.<id>            (display name)
  effect.<ns>.<id>
  item.<ns>.<id>              (item display; block-item uses item.<ns>.<blockid>)
  block.<ns>.<id>             (block display name, used by /data get and blocks screen)
  fluid.<ns>.<id>
  enchantment.<ns>.<id>
  subtitle.<ns>.<id>          (sound events)

Registry ids are parsed from the init classes:
  ModEntities.ENTITY_TYPES.register("<id>", ...)
  ModEffects.MOB_EFFECTS.register("<id>", ...)
  ModItems.ITEMS.register("<id>", ...)
  ModBlocks.registerBlock("<id>", ...) / registerBlockX(...)
  ModSounds ... register("<id>")
  ModEnchantments ... register("<id>")

Usage: python3 tools/audit_lang.py [--fix]
  --fix rewrites en_us.json: for each missing key, adds it (value copied from the
  1.12 ref lang file when a mapping exists, else "<Prettified Id>"). Old
  mismatched keys are renamed. Exit 1 if any gap (before --fix).
"""
import json, os, re, sys, unicodedata

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANG = os.path.join(ROOT, "src/main/resources/assets/thaumcraft/lang/en_us.json")
REF_LANG = os.path.join(ROOT, ".reference/thaumcraft-1.12/assets/thaumcraft/lang/en_us.lang")

NS = "thaumcraft"

def reg_ids(path, patterns):
    ids = []
    if not os.path.exists(path):
        return ids
    txt = open(path, encoding="utf-8").read()
    for pat in patterns:
        for m in re.finditer(pat, txt):
            ids.append(m.group(1))
    return ids

def collect():
    J = "src/main/java/thaumcraft/init/"
    entities = reg_ids(J + "ModEntities.java", [
        r'ENTITY_TYPES\.register\("([a-z0-9_]+)"',
    ])
    effects = reg_ids(J + "ModEffects.java", [
        r'MOB_EFFECTS\.register\("([a-z0-9_]+)"',
    ])
    items = reg_ids(J + "ModItems.java", [
        r'register(?:Item)?\("([a-z0-9_]+)"',
    ])
    blocks = reg_ids(J + "ModBlocks.java", [
        r'register(?:Block|BlockX|BlockSlab|BlockStairs|BlockBanner|BlockButton|BlockDoor|BlockFence|BlockPane|BlockPressurePlate|BlockTrapDoor|BlockWall)?\("([a-z0-9_]+)"',
    ])
    sounds = reg_ids(J + "ModSounds.java", [
        r'register(?:SoundEvent)?\("([a-z0-9_]+)"',
    ])
    enchants = reg_ids(J + "ModEnchantments.java", [
        r'register(?:Enchantment)?\("([a-z0-9_]+)"',
    ])
    return entities, effects, items, blocks, sounds, enchants

def ref_lang():
    d = {}
    if os.path.exists(REF_LANG):
        for line in open(REF_LANG, encoding="utf-8", errors="replace"):
            if "=" in line and not line.strip().startswith("#"):
                k, v = line.split("=", 1)
                d[k.strip()] = v.strip()
    return d

def prettify(id_):
    parts = re.split(r'[._]', id_)
    return " ".join(p.capitalize() for p in parts if p)

def main():
    fix = "--fix" in sys.argv
    ours = json.load(open(LANG))
    ref = ref_lang()
    entities, effects, items, blocks, sounds, enchants = collect()

    # 1.12 ref key mapping (legacy name -> value)
    def ref_value(legacy_key):
        return ref.get(legacy_key)

    problems = []
    additions = {}   # new key -> value
    renames = {}     # old key -> (new key)

    # Manual aliases: registered id -> legacy PascalCase entity name (where the
    # mechanical snake->Pascal transform does not recover the 1.12 class name).
    ENTITY_ALIASES = {
        "thaumcraft_golem": "Golem",
        "taintacle_small": "TaintacleTiny",
        "thaumic_slime": "ThaumSlime",
        "following_item": "FollowItem",
        "turret_crossbow": "TurretBasic",
        "turret_crossbow_advanced": "TurretAdvanced",
        "homing_shard": "FrostShard",
        "rift_blast": "ExplosiveOrb",
        "grapple": "SpecialItem",
        "arcane_bore": "ArcaneBore",
    }

    def pascal(id_):
        return ENTITY_ALIASES.get(id_, "".join(p.capitalize() for p in id_.split("_")))

    def need(key, value_hint=None, legacy=None):
        if key not in ours:
            val = None
            if legacy and legacy in ref:
                val = ref[legacy]
            if val is None:
                val = value_hint or prettify(key.split(f"{NS}.")[-1])
            additions[key] = val
            problems.append(f"MISSING {key}")

    for e in entities:
        newk = f"entity.{NS}.{e}"
        if newk not in ours:
            oldk = f"entity.{NS}.{pascal(e)}"
            legacy = f"entity.{pascal(e)}.name"
            if oldk in ours:
                renames[oldk] = newk
                problems.append(f"RENAME {oldk} -> {newk}")
                continue
            need(newk, legacy=legacy)
    for e in effects:
        need(f"effect.{NS}.{e}")
    for i in items:
        need(f"item.{NS}.{i}", legacy=f"item.{i}.name")
    for b in blocks:
        need(f"block.{NS}.{b}", legacy=f"tile.{b}.name")
        need(f"item.{NS}.{b}", legacy=f"tile.{b}.name")
    for s in sounds:
        need(f"subtitle.{NS}.{s}")
    for e in enchants:
        need(f"enchantment.{NS}.{e}")

    if additions:
        print(f"{len(problems)} lang gaps found.")
        for p in problems:
            print("  ", p)
        if fix:
            for oldk, newk in renames.items():
                ours[newk] = ours.pop(oldk)
                print(f"  renamed {oldk} -> {newk}")
            for k, v in additions.items():
                ours[k] = v
            with open(LANG, "w") as f:
                json.dump(ours, f, indent=2, ensure_ascii=False)
                f.write("\n")
            print(f"FIXED: added {len(additions)} keys to en_us.json")
            return 0
        return 1
    print("lang audit: all registry ids have lang keys")
    return 0

if __name__ == "__main__":
    sys.exit(main())
