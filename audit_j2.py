import re, json, glob, os, collections

SRC = '/tmp/tc112full/thaumcraft/common/config/ConfigRecipes.java'
src = open(SRC, errors='ignore').read()
RECIPE_DIR = '/Users/spencer/Documents/Thaumcraft-26.2/src/main/resources/data/thaumcraft/recipe'

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

# ---- 1.12 side: registry name -> (kind, research key, result-ish) ----
ADDS = {
    'addCrucibleRecipe': ('CrucibleRecipe', 'crucible'),
    'addInfusionCraftingRecipe': ('InfusionRecipe', 'infusion'),
    'addArcaneCraftingRecipe': ('ArcaneRecipe', 'arcane'),
    'addFakeCraftingRecipe': ('FakeRecipe', 'display_only'),
    'addMultiblockRecipeToCatalog': ('MultiblockRecipe', 'multiblock'),
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


rows112 = []
for fn, (kind, station) in ADDS.items():
    for m in re.finditer(r'ThaumcraftApi\.' + fn + r'\(', src):
        a = call_args_at(m.end() - 1)
        name = None
        mm = re.search(r'new ResourceLocation\("thaumcraft:([\w]+)"\)', a[0])
        if mm:
            name = mm.group(1)
        inner = a[1] if len(a) > 1 else ''
        line = src[:m.start()].count('\n') + 1
        rows112.append((name, kind, station, line, inner))

# also the ore-dict / special vanilla recipes registered directly
for kind, station, pat in [
    ('ShapedOreRecipe', 'crafting_table', r'new ShapedOreRecipe\('),
    ('ShapelessOreRecipe', 'crafting_table', r'new ShapelessOreRecipe\('),
    ('RecipeMagicDust', 'crafting_table', r'new RecipeMagicDust\('),
    ('RecipeTripleMeatTreat', 'crafting_table', r'new RecipeTripleMeatTreat\('),
    ('ShapedArcaneVoidJar', 'crafting_table', r'new ShapedArcaneVoidJar\('),
    ('RecipesRobeArmorDyes', 'crafting_table', r'new RecipesRobeArmorDyes\('),
    ('RecipesVoidRobeArmorDyes', 'crafting_table', r'new RecipesVoidRobeArmorDyes\('),
]:
    for m in re.finditer(pat, src):
        line = src[:m.start()].count('\n') + 1
        rows112.append((None, kind, station, line, ''))

# InfusionEnchantmentRecipe registry names
for m in re.finditer(r'new ResourceLocation\("thaumcraft:(IE[\w]+)"\)', src):
    rows112.append((m.group(1), 'InfusionEnchantmentRecipe', 'infusion_enchant',
                    src[:m.start()].count('\n') + 1, ''))

# ---- port side ----
port = {}
for f in sorted(glob.glob(RECIPE_DIR + '/*/*.json')):
    d = json.load(open(f))
    folder = os.path.basename(os.path.dirname(f))
    r = d.get('result')
    rid = r.get('id') if isinstance(r, dict) else r
    name = os.path.basename(f)[:-5]
    port[name] = {
        'name': name, 'folder': folder, 'type': d.get('type'),
        'station': STATION_OF_TYPE.get(d.get('type'), d.get('type')),
        'result': rid, 'research': d.get('research'),
    }


def norm(s):
    if s is None:
        return None
    s = re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s).lower()
    s = re.sub(r'_+', '_', s)
    return s


port_by_norm = collections.defaultdict(list)
for name, p in port.items():
    port_by_norm[norm(name)].append(p)

ALIAS = {
    'hedge_web': ['hedge_cobweb'],
    'hedge_leather': ['leather_from_flesh'],
    'cluster': [],
    'metal_purification_iron': ['metal_purification_iron'],
    'focus_1': ['focus_1'],
    'banner': ['banner_black'],
    'triple_meat_treat': ['triple_meat_treat'],
    'magicdust': ['salis_mundus_fake'],
}

matched = 0
missing = []
mismatch = []
for name, kind, station, line, inner in rows112:
    if name is None:
        continue
    n = norm(name)
    cands = list(port_by_norm.get(n, []))
    for a in ALIAS.get(n, []):
        cands += [port[x] for x in ([a] if a in port else [])]
    if not cands:
        missing.append((name, kind, station, line))
        continue
    matched += 1
    for p in cands:
        if p['station'] != station:
            mismatch.append((name, kind, station, p, line))

print('1.12 named recipes:', sum(1 for r in rows112 if r[0]))
print('matched by name to a port recipe file:', matched)
print()
print('=== STATION MISMATCHES (%d) ===' % len(mismatch))
for name, kind, station, p, line in sorted(mismatch, key=lambda x: x[0]):
    print(f'{name:34s} 1.12={station:12s} port={p["station"]:14s} {p["folder"]}/{p["name"]}.json  (1.12 L{line})')
print()
print('=== 1.12 RECIPES WITH NO SAME-NAMED PORT RECIPE (%d) ===' % len(missing))
for name, kind, station, line in sorted(missing, key=lambda x: x[0]):
    print(f'{name:34s} {station:12s} (1.12 L{line})')
