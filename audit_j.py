import re, json, glob, os, collections

SRC = '/tmp/tc112full/thaumcraft/common/config/ConfigRecipes.java'
src = open(SRC, errors='ignore').read()
ALIAS = json.load(open('/tmp/audit_j_alias.json'))

RECIPE_DIR = '/Users/spencer/Documents/Thaumcraft-26.2/src/main/resources/data/thaumcraft/recipe'


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


def find_calls(pat):
    out = []
    for m in re.finditer(pat, src):
        i = m.end() - 1
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
        out.append((src[:m.start()].count('\n') + 1, split_args(src[i + 1:j])))
    return out


KINDS = {
    'CrucibleRecipe': (1, 'crucible'),
    'InfusionRecipe': (1, 'infusion'),
    'ShapelessArcaneRecipe': (4, 'arcane'),
    'ShapedArcaneRecipe': (4, 'arcane'),
}

STATION_OF_TYPE = {
    'thaumcraft:crucible': 'crucible',
    'thaumcraft:infusion': 'infusion',
    'thaumcraft:arcane_workbench_shaped': 'arcane',
    'thaumcraft:arcane_workbench_shapeless': 'arcane',
    'thaumcraft:infusion_enchantment': 'infusion_enchant',
    'minecraft:crafting_shaped': 'crafting_table',
    'minecraft:crafting_shapeless': 'crafting_table',
    'minecraft:smelting': 'furnace',
}

port = []
for f in sorted(glob.glob(RECIPE_DIR + '/*/*.json')):
    d = json.load(open(f))
    folder = os.path.basename(os.path.dirname(f))
    r = d.get('result')
    rid = r.get('id') if isinstance(r, dict) else r
    port.append({
        'file': os.path.basename(f)[:-5],
        'folder': folder,
        'type': d.get('type'),
        'station': STATION_OF_TYPE.get(d.get('type'), d.get('type')),
        'result': rid,
        'research': d.get('research'),
    })

by_result = collections.defaultdict(list)
for p in port:
    if p['result']:
        by_result[p['result']].append(p)


def normalize(raw):
    r = raw.strip()
    m = re.match(r'^new\s+ItemStack\(\s*([A-Za-z0-9_.]+)', r)
    if m:
        return m.group(1)
    m = re.match(r'^([A-Za-z0-9_]+\.[A-Za-z0-9_]+)', r)
    if m:
        return m.group(1)
    m = re.search(r'getSealStack\("thaumcraft:(\w+)"\)', r)
    if m:
        return 'seal_' + m.group(1)
    m = re.search(r'makeCrystal', r)
    if m:
        return 'makeCrystal'
    m = re.search(r'getFilledBucket', r)
    if m:
        return 'bucket_liquid_death'
    m = re.search(r'new Object\[\]\{"(\w+)"', r)
    if m:
        return 'nbtout_' + m.group(1)
    m = re.match(r'^([A-Za-z][A-Za-z0-9]*)$', r)
    if m:
        return m.group(1)
    m = re.match(r'^([A-Za-z0-9_]+)\b', r)
    if m:
        return m.group(1)
    return r


def alias_for(raw):
    n = normalize(raw)
    if n in ALIAS:
        return ALIAS[n], n
    best = None
    for k in ALIAS:
        if n.startswith(k) and (best is None or len(k) > len(best)):
            best = k
    return (ALIAS[best], n) if best else (None, n)


rows = []
unmapped = []
for kind, (idx, station) in KINDS.items():
    for line, a in find_calls('new ' + kind + r'\('):
        raw = a[idx] if len(a) > idx else ''
        key = a[1] if len(a) > 1 else a[0]
        ids, norm = alias_for(raw)
        if not ids:
            unmapped.append((kind, line, key, raw[:60]))
            continue
        for iid in ids:
            for p in by_result.get(iid, []):
                rows.append((kind, station, line, key, iid, p))

mismatch = []
for kind, station, line, key, iid, p in rows:
    if p['station'] != station:
        mismatch.append((iid, kind, station, key, p))

print('1.12 recipes mapped to port recipes:', len(rows))
print('unmapped 1.12 recipes:', len(unmapped))
print()
print('=== STATION MISMATCHES ===')
seen = set()
for iid, kind, station, key, p in sorted(mismatch, key=lambda x: x[0]):
    k = (p['folder'], p['file'], kind)
    if k in seen:
        continue
    seen.add(k)
    print(f'{iid:42s} 1.12 {kind:20s} research={key:24s} port={p["station"]:14s} {p["folder"]}/{p["file"]}.json')
print()
print('=== UNMAPPED 1.12 RECIPES ===')
for kind, line, key, raw in unmapped:
    print(f'{kind:20s} L{line:<5} {key:26s} {raw}')
