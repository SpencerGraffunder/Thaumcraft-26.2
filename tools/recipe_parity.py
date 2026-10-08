#!/usr/bin/env python3
"""Side-by-side dump: 26.3 port recipe JSON vs the 1.12 recipe that produces the same result.

Usage:
  python3 tools/recipe_parity.py            # only recipes whose ingredient sets differ
  python3 tools/recipe_parity.py --all      # every matched pair
  python3 tools/recipe_parity.py --unmatched# port recipes with no 1.12 counterpart and vice versa
"""
import collections
import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REF = os.path.join(ROOT, "reference", "recipes_112.json")
RECIPE_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "thaumcraft", "recipe")

STATION_DIR = {"arcane_workbench": "arcane", "crucible": "crucible", "infusion": "infusion",
               "crafting": "normal", "smelting": "smelting"}
DIR_STATION = {v: k for k, v in STATION_DIR.items()}

# 1.12 recipe id -> port result id, for results whose symbol does not snake-case automatically
RESULT_OVERRIDE = {
    # crucible
    "SealBreakAdv": "thaumcraft:seal_breaker_advanced", "SealCollect": "thaumcraft:seal_pickup",
    "SealCollectAdv": "thaumcraft:seal_pickup_advanced", "SealEmpty": "thaumcraft:seal_empty",
    "SealEmptyAdv": "thaumcraft:seal_empty_advanced", "SealGuard": "thaumcraft:seal_guard",
    "SealGuardAdv": "thaumcraft:seal_guard_advanced", "SealLumber": "thaumcraft:seal_lumber",
    "SealProvide": "thaumcraft:seal_provide", "SealStock": "thaumcraft:seal_stock",
    "SealStore": "thaumcraft:seal_fill", "SealStoreAdv": "thaumcraft:seal_fill_advanced",
    "SealUse": "thaumcraft:seal_use", "brassingot": "thaumcraft:brass_ingot",
    "thaumiumingot": "thaumcraft:thaumium_ingot", "voidingot": "thaumcraft:void_metal_ingot",
    "hedge_leather": "minecraft:leather", "focus_1": "thaumcraft:focus_1",
    "metal_purification_iron": "thaumcraft:cluster_iron",
    "metal_purification_gold": "thaumcraft:cluster_gold",
    "metal_purification_copper": "thaumcraft:cluster_copper",
    "metal_purification_cinnabar": "thaumcraft:cluster_cinnabar",
    # arcane
    "AdvAlchemyConstruct": "thaumcraft:alchemical_brass_advanced_block",
    "AlchemicalConstruct": "thaumcraft:alchemical_brass_block",
    "AdvancedCrossbow": "thaumcraft:turret_placer_advanced",
    "AutomatedCrossbow": "thaumcraft:turret_placer_basic",
    "CondenserLattice": "thaumcraft:condenser_lattice",
    "EnchantedFabric": "thaumcraft:enchanted_fabric",
    "EssentiaSmelter": "thaumcraft:smelter",
    "EssentiaTransportIn": "thaumcraft:essentia_input",
    "EssentiaTransportOut": "thaumcraft:essentia_output",
    "Filter": "thaumcraft:filter", "MindClockwork": "thaumcraft:mind",
    "PaveBarrier": "thaumcraft:paving_stone_barrier",
    "PaveTravel": "thaumcraft:paving_stone_travel", "RedstoneInlay": "thaumcraft:inlay",
    "RobeBoots": "thaumcraft:cloth_boots", "RobeChest": "thaumcraft:cloth_chest",
    "RobeLegs": "thaumcraft:cloth_legs", "SealBlank": "thaumcraft:seal_blank",
    "Tube": "thaumcraft:tube_normal", "TubeRestrict": "thaumcraft:tube_restricted",
    "modaggression": "thaumcraft:golem_module_aggression",
    "modvision": "thaumcraft:golem_module_vision",
    # infusion
    "ArcaneBore": "thaumcraft:turret_placer_bore", "CLOUDRING": "thaumcraft:cloud_ring",
    "CuriosityBand": "thaumcraft:curiosity_band", "CrystalClusterFlux": "thaumcraft:crystal_flux",
    "HelmGoggles": "thaumcraft:goggles", "MaskAngryGhost": "thaumcraft:mask_angry_ghost",
    "MaskGrinningDevil": "thaumcraft:mask_grinning_devil",
    "MaskSippingFiend": "thaumcraft:mask_sipping_fiend", "Mirror": "thaumcraft:mirror_essentia",
    "PrimalCrusher": "thaumcraft:primal_crusher", "SealBreak": "thaumcraft:seal_breaker",
    "SealButcher": "thaumcraft:seal_butcher", "SealHarvest": "thaumcraft:seal_harvest",
    "VerdantHeart": "thaumcraft:verdant_charm", "VerdantHeartLife": "thaumcraft:verdant_charm_life",
    "VerdantHeartSustain": "thaumcraft:verdant_charm_sustain",
    "VisAmulet": "thaumcraft:amulet_vis_crafted", "VoidseerPearl": "thaumcraft:voidseer_charm",
    "focus_2": "thaumcraft:focus_2", "focus_3": "thaumcraft:focus_3",
    "ElementalAxe": "thaumcraft:elemental_axe", "ElementalPick": "thaumcraft:elemental_pick",
    "ElementalShovel": "thaumcraft:elemental_shovel", "ElementalSword": "thaumcraft:elemental_sword",
    "ElementalHoe": "thaumcraft:elemental_hoe",
}

# 1.12 OreDict names -> the 26.3 tag (or item) the port uses for the same thing
OREDICT = {
    "plateIron": "#c:plates/iron", "plateBrass": "#c:plates/brass",
    "plateThaumium": "#thaumcraft:plates/thaumium", "plateVoid": "#thaumcraft:plates/void",
    "stickWood": "#c:rods/wooden", "rodWood": "#c:rods/wooden",
    "gemQuartz": "#c:gems/quartz", "nuggetQuartz": "#c:nuggets/quartz",
    "ingotGold": "minecraft:gold_ingot", "ingotIron": "minecraft:iron_ingot",
    "nuggetGold": "minecraft:gold_nugget", "nuggetIron": "minecraft:iron_nugget",
    "nuggetBrass": "#c:nuggets/brass", "nuggetThaumium": "#c:nuggets/thaumium",
    "nuggetVoid": "#c:nuggets/void", "nuggetCopper": "#c:nuggets/copper",
    "nuggetTin": "#c:nuggets/tin", "nuggetLead": "#c:nuggets/lead",
    "nuggetSilver": "#c:nuggets/silver", "nuggetQuicksilver": "thaumcraft:quicksilver_nugget",
    "nuggetRareEarth": "thaumcraft:nugget_rareearth", "nuggetTaintedIron": "#c:nuggets/tainted_iron",
    "gemCinnabar": "#c:gems/cinnabar", "gemSulfur": "#c:gems/sulfur",
    "gemSalis": "#c:gems/salis", "gemAmber": "thaumcraft:amber",
    "gemTaintedAmber": "thaumcraft:amber", "shardPotato": "minecraft:poisonous_potato",
    "dustSulfur": "#c:dusts/sulfur", "dustSalis": "#c:dusts/salis",
    "dustGlowstone": "minecraft:glowstone_dust", "dustRedstone": "minecraft:redstone",
    "dustGold": "#c:dusts/gold", "dustIron": "#c:dusts/iron", "dustCopper": "#c:dusts/copper",
    "dustTin": "#c:dusts/tin", "dustIronEssentia": "#c:dusts/iron",
    "dustGoldEssentia": "#c:dusts/gold", "dustCopperEssentia": "#c:dusts/copper",
    "dustSilverEssentia": "#c:dusts/silver", "dustLeadEssentia": "#c:dusts/lead",
    "dustTinEssentia": "#c:dusts/tin", "dustQuartz": "#c:dusts/quartz",
    "dustTaintedAmber": "thaumcraft:amber",
    "oreIron": "#c:ores/iron", "oreGold": "#c:ores/gold", "oreCopper": "#c:ores/copper",
    "oreTin": "#c:ores/tin", "oreLead": "#c:ores/lead", "oreSilver": "#c:ores/silver",
    "oreCinnabar": "#c:ores/cinnabar", "oreQuartz": "#c:ores/quartz",
    "oreSalis": "#c:ores/salis", "oreSulfur": "#c:ores/sulfur",
    "oreTaintedIron": "#thaumcraft:ores/tainted_iron", "oreTaintedGold": "#thaumcraft:ores/tainted_gold",
    "logWood": "#minecraft:logs", "plankWood": "#minecraft:planks",
    "slabWood": "#minecraft:wooden_slabs", "stairWood": "#minecraft:wooden_stairs",
    "treeSapling": "#minecraft:saplings", "cropWheat": "minecraft:wheat",
    "cropPotato": "minecraft:potato", "cropCarrot": "minecraft:carrot",
    "listAllwater": "minecraft:water_bucket", "listAllmilk": "minecraft:milk_bucket",
    "listAllgrass": "#minecraft:grass_blocks", "blockGlass": "#minecraft:glass_blocks",
    "blockIron": "minecraft:iron_block", "blockGold": "minecraft:gold_block",
    "blockBrass": "thaumcraft:brass_block", "blockThaumium": "thaumcraft:thaumium_block",
    "blockVoid": "thaumcraft:void_metal_block", "blockQuartz": "minecraft:quartz_block",
    "blockRedstone": "minecraft:redstone_block", "blockTorch": "minecraft:torch",
    "blockSlime": "minecraft:slime_block", "blockCloud": "thaumcraft:cloud_block",
    "blockSnow": "minecraft:snow_block", "blockChest": "#c:chests", "chestWood": "#c:chests/wooden",
    "stone": "#c:stone", "cobblestone": "minecraft:cobblestone",
    "sand": "minecraft:sand", "gravel": "minecraft:gravel", "clay": "minecraft:clay",
    "glass": "#minecraft:glass_blocks", "string": "minecraft:string",
    "feather": "minecraft:feather", "leather": "minecraft:leather",
    "paper": "minecraft:paper", "book": "minecraft:book", "flint": "minecraft:flint",
    "gunpowder": "minecraft:gunpowder", "bone": "minecraft:bone",
    "enderpearl": "minecraft:ender_pearl", "blazepowder": "minecraft:blaze_powder",
    "mushroom": "#minecraft:mushrooms", "mushroomBrown": "minecraft:brown_mushroom",
    "mushroomRed": "minecraft:red_mushroom", "vine": "minecraft:vine",
    "watermelon": "minecraft:watermelon", "cactus": "minecraft:cactus",
    "wool": "#minecraft:wool", "dyeBlue": "minecraft:lapis_lazuli",
    "dyeBlack": "minecraft:ink_sac", "dyeRed": "minecraft:red_dye",
    "dyeWhite": "minecraft:white_dye", "dyePurple": "minecraft:purple_dye",
    "dyeGreen": "minecraft:green_dye", "dyeCyan": "minecraft:cyan_dye",
    "itemFlintAndSteel": "minecraft:flint_and_steel", "itemShears": "minecraft:shears",
    "itemBucket": "minecraft:bucket", "itemMinecart": "minecraft:minecart",
    "itemBoat": "minecraft:oak_boat", "itemArmorBoots": "minecraft:leather_boots",
    "itemArmorLegs": "minecraft:leather_leggings", "itemArmorChest": "minecraft:leather_chestplate",
    "itemArmorHelm": "minecraft:leather_helmet", "itemAxeWood": "minecraft:wooden_axe",
    "itemPickaxeWood": "minecraft:wooden_pickaxe", "itemShovelWood": "minecraft:wooden_shovel",
    "itemHoeWood": "minecraft:wooden_hoe", "itemSwordWood": "minecraft:wooden_sword",
    "itemBow": "minecraft:bow", "itemArrow": "minecraft:arrow",
    "itemEnderEye": "minecraft:ender_eye", "itemSkull": "minecraft:skeleton_skull",
    "itemSkullHumanoid": "minecraft:zombie_head", "itemRecord": "minecraft:music_disc_13",
    "itemEgg": "minecraft:egg", "itemSnowball": "minecraft:snowball",
    "itemStick": "#c:rods/wooden", "itemPiston": "minecraft:piston",
    "itemCompass": "minecraft:compass", "itemClock": "minecraft:clock",
    "itemGoldNugget": "minecraft:gold_nugget", "itemIronNugget": "minecraft:iron_nugget",
    "itemSlimeball": "minecraft:slime_ball", "itemEnderpearl": "minecraft:ender_pearl",
    "itemGlowstoneDust": "minecraft:glowstone_dust", "itemNetherrack": "minecraft:netherrack",
    "itemSoulSand": "minecraft:soul_sand", "itemObsidian": "minecraft:obsidian",
    "itemHardenedClay": "minecraft:hardened_clay", "itemPumpkin": "minecraft:pumpkin",
    "itemMelon": "minecraft:melon", "itemReeds": "minecraft:sugar_cane",
    "itemSugar": "minecraft:sugar", "itemChestMinecart": "minecraft:minecart",
    "itemLadder": "minecraft:ladder", "itemTorch": "minecraft:torch",
    "itemSign": "#minecraft:signs", "itemBed": "minecraft:red_bed",
    "itemCake": "minecraft:cake", "itemAnvil": "minecraft:anvil",
    "itemCauldron": "minecraft:cauldron", "itemBrewingStand": "minecraft:brewing_stand",
    "itemBottle": "minecraft:glass_bottle", "itemBlazeRod": "minecraft:blaze_rod",
    "itemGhastTear": "minecraft:ghast_tear", "itemMagmaCream": "minecraft:magma_cream",
    "itemSpiderEye": "minecraft:spider_eye", "itemFermentedSpiderEye": "minecraft:fermented_spider_eye",
    "itemGoldCarrot": "minecraft:golden_carrot", "itemApple": "minecraft:apple",
    "itemMushroomStew": "minecraft:mushroom_stew", "itemBread": "minecraft:bread",
    "itemCookedChicken": "minecraft:cooked_chicken", "itemCookedBeef": "minecraft:cooked_beef",
    "itemCookedPork": "minecraft:cooked_porkchop", "itemRawChicken": "minecraft:chicken",
    "itemRawBeef": "minecraft:beef", "itemRawPork": "minecraft:porkchop",
    "itemRottenFlesh": "minecraft:rotten_flesh", "itemFishRaw": "minecraft:cod",
    "itemFishCooked": "minecraft:cooked_cod", "itemWheatSeed": "minecraft:wheat_seeds",
    "itemLadder": "minecraft:ladder", "itemIronBars": "minecraft:iron_bars",
    "itemGlassBottle": "minecraft:glass_bottle", "itemBook": "minecraft:book",
    "itemWrittenBook": "minecraft:written_book", "itemMap": "minecraft:map",
    "itemPaper": "minecraft:paper", "itemNameTag": "minecraft:name_tag",
    "itemLead": "minecraft:lead", "itemNameTag": "minecraft:name_tag",
    "blockTNT": "minecraft:tnt", "blockWeb": "minecraft:cobweb", "blockCobweb": "minecraft:cobweb",
    "blockStone": "#c:stone", "blockDirt": "minecraft:dirt", "blockGrass": "#minecraft:grass_blocks",
    "blockSand": "minecraft:sand", "blockGravel": "minecraft:gravel",
    "blockObsidian": "minecraft:obsidian", "blockNetherrack": "minecraft:netherrack",
    "blockSoulSand": "minecraft:soul_sand", "blockGlowstone": "minecraft:glowstone",
    "blockLadder": "minecraft:ladder", "blockFence": "#minecraft:fences",
    "blockFenceGate": "#minecraft:fence_gates", "blockWall": "#minecraft:walls",
    "blockPaneGlass": "#minecraft:glass_panes", "paneGlass": "#minecraft:glass_panes",
    "blockHardenedClay": "minecraft:hardened_clay", "blockStickyMob": "minecraft:slime_block",
    "blockHopper": "minecraft:hopper", "blockDispenser": "minecraft:dispenser",
    "blockDropper": "minecraft:dropper", "blockFurnace": "minecraft:furnace",
    "blockEnchantTable": "minecraft:enchanting_table", "blockBeacon": "minecraft:beacon",
    "blockNoteblock": "minecraft:note_block", "blockJukebox": "minecraft:jukebox",
    "blockBedrock": "minecraft:bedrock", "blockMobSpawner": "minecraft:spawner",
    "blockBookshelf": "minecraft:bookshelf", "blockTorch": "minecraft:torch",
    "blockRedstoneLamp": "minecraft:redstone_lamp", "blockPumpkin": "minecraft:pumpkin",
    "blockMelon": "minecraft:melon", "blockSapling": "#minecraft:saplings",
    "blockLeaves": "#minecraft:leaves", "blockLog": "#minecraft:logs",
    "blockPlank": "#minecraft:planks", "blockChest": "#c:chests",
    "blockWorkbench": "minecraft:crafting_table", "blockCraftingTable": "minecraft:crafting_table",
    "blockSnow": "minecraft:snow_block", "blockIce": "minecraft:ice",
    "blockPackice": "minecraft:packed_ice", "blockFarmland": "minecraft:farmland",
    "blockAir": "", "blockWater": "minecraft:water_bucket", "blockLava": "minecraft:lava_bucket",
    "blockFire": "minecraft:flint_and_steel", "blockLadder": "minecraft:ladder",
    "blockIronFence": "minecraft:iron_bars", "blockStoneSlab": "minecraft:stone_slab",
    "blockStonebrick": "minecraft:stone_bricks", "blockMossStone": "minecraft:mossy_cobblestone",
    "blockBrick": "minecraft:brick_block", "blockQuartz": "minecraft:quartz_block",
    "blockTaint": "#thaumcraft:taint_blocks",
    "oreDictChest": "#c:chests",
}

TC_META = {  # 1.12 meta-variant items -> the 26.3 item the port split them into
    "ItemsTC.ingots, 1, 0": "thaumcraft:thaumium_ingot",
    "ItemsTC.ingots, 1, 1": "thaumcraft:void_metal_ingot",
    "ItemsTC.ingots, 1, 2": "thaumcraft:brass_ingot",
    "ItemsTC.nuggets, 1, 0": "thaumcraft:thaumium_nugget",
    "ItemsTC.nuggets, 1, 1": "thaumcraft:void_metal_nugget",
    "ItemsTC.nuggets, 1, 2": "thaumcraft:brass_nugget",
    "ItemsTC.clusters, 1, 0": "thaumcraft:cluster_iron",
    "ItemsTC.clusters, 1, 1": "thaumcraft:cluster_gold",
    "ItemsTC.clusters, 1, 2": "thaumcraft:cluster_copper",
    "ItemsTC.clusters, 1, 3": "thaumcraft:cluster_tin",
    "ItemsTC.clusters, 1, 4": "thaumcraft:cluster_silver",
    "ItemsTC.clusters, 1, 5": "thaumcraft:cluster_lead",
    "ItemsTC.clusters, 1, 6": "thaumcraft:cluster_cinnabar",
    "ItemsTC.modules, 1, 0": "thaumcraft:golem_module_vision",
    "ItemsTC.modules, 1, 1": "thaumcraft:golem_module_aggression",
    "ItemsTC.turretPlacer, 1, 0": "thaumcraft:turret_placer_basic",
    "ItemsTC.turretPlacer, 1, 1": "thaumcraft:turret_placer_advanced",
    "ItemsTC.turretPlacer, 1, 2": "thaumcraft:turret_placer_bore",
    "ItemsTC.mind, 1, 0": "thaumcraft:mind",
    "ItemsTC.mind, 1, 1": "thaumcraft:mind",
    "ItemsTC.seals, 3": "thaumcraft:seal_blank",
    "ItemsTC.fabric": "thaumcraft:enchanted_fabric",
    "ItemsTC.focus1": "thaumcraft:focus_1",
    "ItemsTC.focus2": "thaumcraft:focus_2",
    "ItemsTC.focus3": "thaumcraft:focus_3",
    "ItemsTC.clothBoots, 1": "thaumcraft:cloth_boots",
    "ItemsTC.clothChest, 1": "thaumcraft:cloth_chest",
    "ItemsTC.clothLegs, 1": "thaumcraft:cloth_legs",
    "ItemsTC.filter, 2, 0": "thaumcraft:filter",
    "ItemsTC.tube, 8, 0": "thaumcraft:tube_normal",
    "ItemsTC.metalAlchemical, 2": "thaumcraft:alchemical_brass_block",
    "ItemsTC.metalAlchemicalAdvanced": "thaumcraft:alchemical_brass_advanced_block",
    "ItemsTC.smelterBasic": "thaumcraft:smelter",
    "ItemsTC.essentiaTransportInput": "thaumcraft:essentia_input",
    "ItemsTC.essentiaTransportOutput": "thaumcraft:essentia_output",
    "ItemsTC.inlay, 2": "thaumcraft:inlay",
    "ItemsTC.pavingStoneBarrier, 4": "thaumcraft:paving_stone_barrier",
    "ItemsTC.pavingStoneTravel, 4": "thaumcraft:paving_stone_travel",
    "ItemsTC.condenserlattice": "thaumcraft:condenser_lattice",
    "ItemsTC.tubeRestrict": "thaumcraft:tube_restricted",
    "ItemsTC.banneLight": "thaumcraft:arcane_bannelight",
    "ItemsTC.crystalTaint": "thaumcraft:crystal_flux",
    "ItemsTC.mirror": "thaumcraft:mirror_essentia",
    "ItemsTC.ringCloud": "thaumcraft:cloud_ring",
    "ItemsTC.bandCuriosity": "thaumcraft:curiosity_band",
    "ItemsTC.charmVerdant": "thaumcraft:verdant_charm",
    "ItemsTC.charmVoidseer": "thaumcraft:voidseer_charm",
    "ItemsTC.amuletVis, 1, 1": "thaumcraft:amulet_vis_crafted",
    "ItemsTC.primalCrusher": "thaumcraft:primal_crusher",
    "ItemsTC.brain, 1, 0": "thaumcraft:brain_normal",
    "ItemsTC.brain, 1, 1": "thaumcraft:brain_curious",
    "ItemsTC.brain": "thaumcraft:brain_normal",
    "ItemsTC.alumentum": "thaumcraft:alumentum",
    "ItemsTC.visResonator": "thaumcraft:vis_resonator",
    "ItemsTC.thaumometer": "thaumcraft:thaumometer",
    "ItemsTC.sanityChecker": "thaumcraft:sanity_checker",
    "ItemsTC.plateFeetIron": "thaumcraft:armor_iron_feet",
    "ItemsTC.plateLegsIron": "thaumcraft:armor_iron_legs",
    "ItemsTC.plateBodyIron": "thaumcraft:armor_iron_chest",
    "ItemsTC.plateHeadIron": "thaumcraft:armor_iron_helm",
}


def load_aspects():
    """1.12 enum name -> 26.3 aspect tag, read from the port's own Aspect registry."""
    src = open(os.path.join(ROOT, "src", "main", "java", "thaumcraft", "api", "aspects", "Aspect.java")).read()
    pairs = re.findall(r"public static final Aspect (\w+)\s*=\s*new Aspect\(\"(\w+)\"", src)
    return {name: tag for name, tag in pairs}, {tag for _, tag in pairs}


ASPECT_MAP, VALID_ASPECTS = load_aspects()


VANILLA_SRG = {  # 1.12.2 SRG field names -> modern names (rail fields, derived from the 1.12 ConfigAspects redstone entries)
    "Blocks.field_150408_cc": "minecraft:activator_rail",
    "Blocks.field_150448_aq": "minecraft:rail",
    "Blocks.field_150318_D": "minecraft:golden_rail",
    "Blocks.field_150319_E": "minecraft:detector_rail",
}


def snake(sym):
    s = re.sub(r"\.get\(.*", "", sym)
    s = re.sub(r"func_\d+\w*", "", s)
    s = s.replace("BlocksTC.", "").replace("ItemsTC.", "").replace("Blocks.", "").replace("Items.", "")
    s = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", s).lower()
    return s


def map_token(tok):
    """Map a 1.12 ingredient token to the 26.3 id/tag the port should use."""
    tok = re.sub(r"\s+", " ", tok.strip())
    for k, v in TC_META.items():
        if tok.startswith(k):
            return v
    if tok in VANILLA_SRG:
        return VANILLA_SRG[tok]
    m = re.match(r"^(ItemsTC|BlocksTC)\.([A-Za-z0-9_]+)", tok)
    if m:
        return "thaumcraft:" + snake(m.group(2))
    m = re.match(r'^(Items|Blocks)\.([A-Za-z0-9_]+)', tok)
    if m:
        return "minecraft:" + snake(m.group(2))
    if tok.startswith('"') and tok.endswith('"'):
        name = tok.strip('"')
        if name in OREDICT:
            return OREDICT[name]
        return "OREDICT:" + name
    if tok.startswith("Ingredient.fromTag"):
        return "TAG?" + tok
    if "getSealStack" in tok:
        m = re.search(r'thaumcraft:([a-z_]+)', tok)
        return "thaumcraft:seal_" + m.group(1) if m else tok
    if "makeCrystal" in tok:
        return "thaumcraft:crystal_essence"
    return "RAW:" + tok


def norm_112(e):
    """Return (station, research, vis, aspects, ingredients multiset, center) for a 1.12 entry."""
    st = e["station"]
    res = {"station": st, "research": e.get("research", ""), "vis": e.get("vis", "")}
    asp = {}
    for a, n in e.get("aspects", []):
        asp[ASPECT_MAP.get(a.upper(), a.lower())] = int(n)
    res["aspects"] = asp
    ings = collections.Counter()
    if st == "arcane":
        if "key" in e:
            for letter, tok in e.get("key", {}).items():
                cnt = "".join(e.get("pattern", [])).count(letter)
                ings[map_token(tok)] += cnt or 1
        else:
            for tok in e.get("ingredients", []):
                ings[map_token(tok)] += 1
    elif st in ("crucible",):
        if e.get("ingredient"):
            ings[map_token(e["ingredient"])] += 1
    elif st == "infusion":
        mm = re.search(r"new InfusionRecipe\((.*)\)", e.get("raw", ""), re.S)
        if mm:
            import shlex
            inner = mm.group(1)
            # crude split
            depth, cur, args = 0, "", []
            i = 0
            while i < len(inner):
                c = inner[i]
                if c == '"':
                    cur += c
                    i += 1
                    while i < len(inner) and inner[i] != '"':
                        if inner[i] == '\\':
                            cur += inner[i]
                            i += 1
                        cur += inner[i]
                        i += 1
                    cur += '"'
                    continue
                if c in "([{":
                    depth += 1
                elif c in ")]}":
                    depth -= 1
                if c == ',' and depth == 0:
                    args.append(cur.strip())
                    cur = ""
                else:
                    cur += c
                i += 1
            if cur.strip():
                args.append(cur.strip())
            if len(args) > 4:
                res["vis"] = args[3].strip()
                center = args[4]
                res["center"] = map_token(re.sub(r"new ItemStack\((.*)\)", r"\1", center.strip()))
                for tok in re.findall(r"new ItemStack\(\s*([^,)]+)(?:,\s*\d+)?\s*\)", " ".join(args[5:])):
                    ings[map_token(tok)] += 1
                for tok in re.findall(r'"([A-Za-z][A-Za-z0-9_]+)"', " ".join(args[5:])):
                    ings[map_token('"%s"' % tok)] += 1
        if e.get("extra"):
            for fn, arg in e["extra"]:
                if fn == "setInstability":
                    res["instability"] = arg.strip()
    res["ingredients"] = ings
    return res


def norm_port(d):
    st = d.get("type", "")
    out = {"research": d.get("research", ""), "vis": d.get("vis", ""), "aspects": {},
           "ingredients": collections.Counter()}
    out["aspects"] = dict(d.get("aspects", {}) or {})
    bad = [k for k in out["aspects"] if k not in VALID_ASPECTS]
    if bad:
        out["bad_aspects"] = bad
    if "pattern" in d:
        key = d.get("key", {})
        for letter in "".join(d["pattern"]):
            if letter == " ":
                continue
            tok = key.get(letter)
            out["ingredients"][norm_port_tok(tok)] += 1
    for tok in d.get("ingredients", []) or []:
        out["ingredients"][norm_port_tok(tok)] += 1
    if d.get("catalyst"):
        out["ingredients"][norm_port_tok(d["catalyst"])] += 1
    if d.get("center"):
        out["center"] = norm_port_tok(d["center"])
    if d.get("crystals"):
        out["crystals"] = d["crystals"]
    if d.get("instability"):
        out["instability"] = d["instability"]
    out["type"] = st
    return out


def norm_port_tok(tok):
    if isinstance(tok, dict):
        tok = tok.get("item") or tok.get("tag") or json.dumps(tok)
    if isinstance(tok, str) and tok.startswith("#"):
        return tok
    return tok


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "diff"
    ref = json.load(open(REF))
    port = {}
    for f in sorted(glob.glob(os.path.join(RECIPE_DIR, "*", "*.json"))):
        station = os.path.relpath(f, RECIPE_DIR).split(os.sep)[0]
        d = json.load(open(f))
        res = d.get("result", {}).get("id")
        if res:
            port.setdefault(res, []).append((station, os.path.relpath(f, RECIPE_DIR), d))

    ref_by_result = {}
    for rid, entries in ref.items():
        for e in entries:
            res = e.get("result", "")
            if not res:
                continue
            pid = RESULT_OVERRIDE.get(rid)
            if not pid:
                s = snake(res)
                pid = "thaumcraft:" + s if "thaumcraft:" + s in port else "minecraft:" + s if "minecraft:" + s in port else None
            if pid:
                ref_by_result.setdefault(pid, []).append((rid, e))

    matched = 0
    for res, plist in sorted(port.items()):
        refs = ref_by_result.get(res, [])
        if not refs:
            if mode == "unmatched":
                print(f"NO 1.12 MATCH: {res}  ({plist[0][1]})")
            continue
        for station, rel, d in plist:
            e = refs[0][1]
            a = norm_112(e)
            b = norm_port(d)
            matched += 1
            issues = []
            if a["station"] != STATION_DIR.get(station, station):
                issues.append(f"station 1.12={a['station']} port={station}")
            if a["research"] and b["research"] and a["research"] != b["research"]:
                issues.append(f"research 1.12={a['research']} port={b['research']}")
            if a["research"] and not b["research"]:
                issues.append(f"port has no research gate (1.12 {a['research']})")
            if b.get("bad_aspects"):
                issues.append(f"UNREGISTERED aspect key(s) {b['bad_aspects']}")
            if a["aspects"] and b["aspects"] and a["aspects"] != b["aspects"]:
                issues.append(f"aspects 1.12={a['aspects']} port={b['aspects']}")
            if a["ingredients"] != b["ingredients"]:
                issues.append("ingredients differ")
            if mode == "diff" and not issues:
                continue
            print(f"=== {res}  [{rel}]  (1.12 {refs[0][0]} L{e['line']})")
            for i in issues:
                print("   !", i)
            print("   1.12:", dict(a["ingredients"]), "center=", a.get("center"), "vis=", a["vis"], "inst=", a.get("instability"))
            print("   port:", dict(b["ingredients"]), "center=", b.get("center"), "vis=", b["vis"], "inst=", b.get("instability"), "crystals=", b.get("crystals"))
    if mode == "unmatched":
        print("\n--- 1.12 recipes with no port result ---")
        for rid, entries in sorted(ref.items()):
            for e in entries:
                res = e.get("result", "")
                if not res:
                    continue
                pid = RESULT_OVERRIDE.get(rid)
                if not pid:
                    s = snake(res)
                    pid = "thaumcraft:" + s if "thaumcraft:" + s in port else "minecraft:" + s if "minecraft:" + s in port else None
                if not pid:
                    print(f"  {rid:34s} {e['station']:9s} {res}")
    print(f"\nmatched pairs: {matched}")


main()
