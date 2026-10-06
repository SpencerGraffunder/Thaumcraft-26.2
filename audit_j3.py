"""Audit J part 2: match 1.12 recipes to port recipes by (station, research key).

1.12 recipe ids are CamelCase legacy names (BootsTraveller) while the port renames its recipe files, so name
matching gives false gaps.  The research gate is stable across both, so match on (station, research key) instead.
"""
import re, json, glob, os, collections

SRC = '/tmp/tc112full/thaumcraft/common/config/ConfigRecipes.java'
src = open(SRC, errors='ignore').read()
RECIPE_DIR = 'src/main/resources/data/thaumcraft/recipe'

STATION_OF_TYPE = {
    'thaumcraft:crucible': 'crucible',
    'thaumcraft:infusion': 'infusion',
    'thaumcraft:arcane_workbench_shaped': 'arcane',
    'thaumcraft:arcane_workbench_shapeless': 'arcane',
    'thaumcraft:infusion_enchantment': 'infusion_enchant',
    'minecraft:crafting_shaped': 'crafting_table',
    'minecraft:crafting_shapeless': 'crafting_table',
}


def split_args(s):
    args, depth, cur = [], 0, ''
    for ch in s:
        if ch in '([{':
            depth += 1; cur += ch
        elif ch in ')]}':
            depth -= 1; cur += ch
        elif ch == ',' and depth == 0:
            args.append(cur.strip()); cur = ''
        else:
            cur += ch
    if cur.strip():
        args.append(cur.strip())
    return args


def call_args_at(i):
    d, j = 0, i
    while j < len(src):
        c = src[j]
        if c == '(':
            d += 1
        elif c == ')':
            d -= 1
            if d == 0:
                break
        j += 1
    return split_args(src[i + 1:j])


def research_of(arg):
    m = re.search(r'"([A-Za-z0-9_@!]+)"', arg)
    return m.group(1) if m else None


rows112 = []
for fn, ctor, station in [
    ('addCrucibleRecipe', 'CrucibleRecipe', 'crucible'),
    ('addInfusionCraftingRecipe', 'InfusionRecipe', 'infusion'),
    ('addArcaneCraftingRecipe', None, 'arcane'),
]:
    for m in re.finditer(r'ThaumcraftApi\.' + fn + r'\(', src):
        a = call_args_at(m.end() - 1)
        rid = None
        mm = re.search(r'new ResourceLocation\("thaumcraft:([^"]+)"', a[0])
        if mm:
            rid = mm.group(1)
        body = a[1] if len(a) > 1 else ''
        line = src[:m.start()].count('\n') + 1
        if ctor:
            inner = body[body.find('(') + 1:] if '(' in body else ''
            args = split_args(inner)
            key = research_of(args[0]) if args else None
        else:
            inner = body[body.find('(') + 1:] if '(' in body else ''
            args = split_args(inner)
            key = research_of(args[1]) if len(args) > 1 else None
        rows112.append((rid, station, key, line))

port = []
for f in sorted(glob.glob(RECIPE_DIR + '/*/*.json')):
    d = json.load(open(f))
    station = STATION_OF_TYPE.get(d.get('type'), d.get('type'))
    r = d.get('result')
    rid = r.get('id') if isinstance(r, dict) else r
    port.append({
        'file': os.path.relpath(f, RECIPE_DIR),
        'station': station,
        'research': d.get('research'),
        'result': rid,
    })


def base(k):
    return k.split('@')[0] if k else None


port_index = collections.defaultdict(list)
for p in port:
    port_index[(p['station'], base(p['research']))].append(p)
port_any = collections.defaultdict(list)
for p in port:
    port_any[base(p['research'])].append(p)

seen = set()
gaps = []
station_diff = []
for rid, station, key, line in rows112:
    if key is None or key.startswith('!'):
        continue
    if (station, base(key)) in port_index:
        continue
    alts = port_any.get(base(key), [])
    if alts:
        station_diff.append((rid, station, key, line, alts))
    else:
        gaps.append((rid, station, key, line))
    seen.add((station, base(key)))

print(f'1.12 recipes with a research gate: {len([r for r in rows112 if r[2]])}')
print(f'port recipes: {len(port)}')
print()
print(f'=== 1.12 recipe whose research gate exists in the port but on ANOTHER station ({len(station_diff)}) ===')
for rid, station, key, line, alts in sorted(station_diff, key=lambda x: x[2]):
    print(f'{key:24s} 1.12={station:12s} port={[a["file"] for a in alts]} (1.12 L{line})')
print()
print(f'=== 1.12 recipe with NO port recipe using its research gate ({len(gaps)}) ===')
for rid, station, key, line in sorted(gaps, key=lambda x: x[2]):
    print(f'{key:24s} {station:12s} id={rid} (1.12 L{line})')

used = {base(p['research']) for p in port}
extra = [p for p in port if base(p['research']) not in {base(k) for _, _, k, _ in rows112 if k}]
print()
print(f'=== port recipes whose research gate appears in no 1.12 recipe ({len(extra)}) ===')
for p in extra:
    print(f'{p["file"]:58s} {p["station"]:16s} research={p["research"]} result={p["result"]}')
