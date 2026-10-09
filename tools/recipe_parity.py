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
    "MaskSippingFiend": "thaumcraft:mask_sipping_fiend", "Mirror": "thaumcraft:mirror_item",
    "MirrorEssentia": "thaumcraft:mirror_essentia",
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
    # no common c:ores/{tin,silver,lead} tags exist in NeoForge 26.3 -> TC-namespace
    # equivalents (mirrors 1.12 OreDict, which mods self-register into)
    "oreTin": "#thaumcraft:ores/tin", "oreLead": "#thaumcraft:ores/lead", "oreSilver": "#thaumcraft:ores/silver",
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
    "stone": "#c:stones", "cobblestone": "minecraft:cobblestone",
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
    "blockPaneGlass": "#c:glass_panes", "paneGlass": "#c:glass_panes",
    "trapdoorWood": "#minecraft:wooden_trapdoors", "nitor": "#thaumcraft:nitor",
    "gemDiamond": "#c:gems/diamond",
    "blockHardenedClay": "minecraft:hardened_clay", "blockStickyMob": "minecraft:slime_block",
    "blockHopper": "minecraft:hopper", "blockDispenser": "minecraft:dispenser",
    "blockDropper": "minecraft:dropper", "blockFurnace": "minecraft:furnace",
    "blockEnchantTable": "minecraft:enchanting_table", "blockBeacon": "minecraft:beacon",
    "blockNoteblock": "minecraft:note_block", "blockJukebox": "minecraft:jukebox",
    "blockBedrock": "minecraft:bedrock", "blockMobSpawner": "minecraft:spawner",
    "blockBookshelf": "minecraft:bookshelf", "workbench": "minecraft:crafting_table", "blockTorch": "minecraft:torch",
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

# 1.12 OREDICT plate names map to the port's direct items (the port provides the
# c:plates/* tag files itself, and most recipes use the items directly)
OREDICT.update({"plateIron": "thaumcraft:plate_iron", "plateBrass": "thaumcraft:plate_brass",
                "ingotBrass": "thaumcraft:brass_ingot", "ingotThaumium": "thaumcraft:thaumium_ingot",
                "ingotVoid": "thaumcraft:void_metal_ingot"})

# equivalent token groups (item vs tag, 1.12 name vs port name); first member is canonical
EQUIV_GROUPS = [
    {"minecraft:iron_ingot", "#c:ingots/iron"},
    {"minecraft:gold_ingot", "#c:ingots/gold"},
    {"minecraft:copper_ingot", "#c:ingots/copper"},
    {"minecraft:leather", "#c:leathers"},
    {"minecraft:iron_nugget", "#c:nuggets/iron"},
    {"minecraft:gold_nugget", "#c:nuggets/gold"},
    {"thaumcraft:plate_iron", "#c:plates/iron", "#thaumcraft:plates/iron"},
    {"thaumcraft:plate_brass", "#c:plates/brass", "#thaumcraft:plates/brass"},
    {"thaumcraft:plate_thaumium", "#thaumcraft:plates/thaumium"},
    {"thaumcraft:plate_void", "#thaumcraft:plates/void"},
    {"thaumcraft:brass_nugget", "#c:nuggets/brass", "#thaumcraft:nuggets/brass"},
    {"thaumcraft:tube", "thaumcraft:tube_normal"},
    {"thaumcraft:plank_greatwood", "thaumcraft:greatwood_planks"},
    {"thaumcraft:plank_silverwood", "thaumcraft:silverwood_planks"},
    {"thaumcraft:stone_arcane", "thaumcraft:arcane_stone"},
    {"thaumcraft:slab_arcane_stone", "thaumcraft:arcane_stone_slab"},
    {"thaumcraft:metal_block_void", "thaumcraft:void_metal_block"},
    {"thaumcraft:metal_block_brass", "thaumcraft:brass_block"},
    {"thaumcraft:metal_block_thaumium", "thaumcraft:thaumium_block"},
    {"thaumcraft:stone_eldritch", "thaumcraft:eldritch_stone"},
    {"#minecraft:glass_blocks", "#c:glass_blocks"},
    {"#minecraft:glass", "#c:glass"},
    {"minecraft:emerald", "#c:gems/emerald"},
    {"minecraft:string", "#c:strings"},
    {"thaumcraft:metal_alchemical", "thaumcraft:alchemical_brass_block"},
    {"thaumcraft:mirror", "thaumcraft:mirror_item"},
    {"thaumcraft:nitor", "#thaumcraft:nitor"},
    {"thaumcraft:stone_arcane_brick", "thaumcraft:arcane_stone_brick"},
    {"thaumcraft:slab_ancient", "thaumcraft:ancient_stone_slab"},
    {"thaumcraft:stone_ancient", "thaumcraft:ancient_stone"},
    {"thaumcraft:slab_eldritch", "thaumcraft:eldritch_slab"},
    {"thaumcraft:stone_eldritch_tile", "thaumcraft:eldritch_stone_tile"},
    {"thaumcraft:seals", "thaumcraft:seal_blank"},
    {"minecraft:cobblestone", "#c:cobblestones"},
    {"thaumcraft:smelter_basic", "thaumcraft:smelter"},
    {"thaumcraft:metal_alchemical_advanced", "thaumcraft:alchemical_brass_advanced_block"},
    {"thaumcraft:phial", "thaumcraft:phial_empty"},
    {"thaumcraft:turret_placer", "thaumcraft:turret_placer_basic"},
    {"thaumcraft:tube_restrict", "thaumcraft:tube_restricted"},
]

# port recipes manually verified 1:1 against 1.12 ConfigRecipes (vanilla-style
# conversion recipes the extractor doesn't parse): path -> 1.12 recipe id
KNOWN_112_MATCHES = {
    "crafting/brass_ingots_from_block.json": "brassblocktoingots L2663",
    "crafting/brass_nuggets_to_ingot.json": "nuggetstobrass L2649",
    "crafting/thaumium_ingots_from_block.json": "thaumiumblocktoingots L2654",
    "crafting/thaumium_nuggets_to_ingot.json": "nuggetstothaumium L2645",
    "crafting/void_ingots_from_block.json": "voidblocktoingots L2659",
    "crafting/void_nuggets_to_ingot.json": "nuggetstovoid L2647",
}
_EQUIV = {}
for _g in EQUIV_GROUPS:
    _g = set(_g)
    _c = sorted(_g)[0]
    for _t in _g:
        _EQUIV[_t] = _c

def canon(tok):
    return _EQUIV.get(tok, tok)

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
    "ItemsTC.brain, 1, 0": "thaumcraft:zombie_brain",
    "ItemsTC.brain, 1, 1": "thaumcraft:brain_curious",
    "Items.field_151100_aR, 1, 0": "minecraft:white_dye",
    "Items.field_151100_aR, 1, 1": "minecraft:orange_dye",
    "Items.field_151100_aR, 1, 15": "minecraft:black_dye",
    "ItemsTC.nuggets, 1, 10": "thaumcraft:nugget_rareearth",
    "ItemsTC.brain": "thaumcraft:zombie_brain",
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


VANILLA_SRG = {  # 1.12.2 SRG field names -> modern names. Derived from 1.12 ConfigAspects
    # aspect entries + scan names + the port's in-game-verified ingredient choices.
    "Blocks.field_150408_cc": "minecraft:activator_rail",
    "Blocks.field_150448_aq": "minecraft:rail",
    "Blocks.field_150318_D": "minecraft:golden_rail",
    "Blocks.field_150319_E": "minecraft:detector_rail",
    "Blocks.field_150442_at": "minecraft:lever",
    "Blocks.field_150367_z": "minecraft:dispenser",
    "Blocks.field_150438_b_z": "minecraft:hopper",
    "Blocks.field_150460_al": "minecraft:furnace",
    "Blocks.field_150331_j": "minecraft:piston",
    "Blocks.field_150333_u": "minecraft:stone_slab",
    "Blocks.field_150429_a_a": "minecraft:redstone_torch",
    "Blocks.field_150410_a_z": "minecraft:glass_pane", "Blocks.field_150410_aZ": "minecraft:glass_pane",
    "Blocks.field_150411_a_y": "minecraft:iron_bars", "Blocks.field_150411_aY": "minecraft:iron_bars",
    "Blocks.field_150371_ca": "minecraft:quartz_block",
    "Blocks.field_150484_ah": "minecraft:diamond_block",
    "Blocks.field_150479_b_c": "minecraft:tripwire_hook",
    "Blocks.field_150325_L": "#minecraft:wool",
    "Items.field_151078_bh": "minecraft:rotten_flesh",
    "Items.field_151044_h": "minecraft:coal",
    "Items.field_151133_ar": "minecraft:bucket",
    "Items.field_151137_ax": "minecraft:redstone",
    "Items.field_151132_b_s": "minecraft:comparator",
    "Items.field_151119_aD": "minecraft:clay_ball",
    "Items.field_151031_f": "minecraft:bow",
    "Items.field_151065_br": "minecraft:blaze_powder",
    "Items.field_151069_bo": "minecraft:glass_bottle",
    "Items.field_151071_bq": "minecraft:fermented_spider_eye",
    "Items.field_151067_bt": "minecraft:brewing_stand",
    "Items.field_151162_b_e": "minecraft:flower_pot",
    # verified against 1.12 ConfigRecipes smelting + IF-bonus context:
    # cluster@0/1 base smelt -> 2 nuggets (field_151042_j/field_151043_k),
    # IF bonuses for those clusters are the INGOTS (field_191525_da/field_151074_bl)
    "Items.field_151014_N": "minecraft:wheat_seeds",
    "Items.field_150410_a_z": "minecraft:glass_pane",
    "Items.field_150331_j": "minecraft:piston", "Blocks.field_150331_J": "minecraft:piston",
    "Items.field_151153_ao": "minecraft:golden_apple",
    "Items.field_151156_bN": "minecraft:nether_star", "Items.field_151156_b_n": "minecraft:nether_star",
    "Items.field_151042_j": "minecraft:iron_nugget",
    "Items.field_151043_k": "minecraft:gold_nugget",
    "Items.field_151074_bl": "minecraft:gold_ingot",
    "Items.field_191525_da": "minecraft:iron_ingot",
    "Items.field_151128_bU": "minecraft:quartz",
    "Items.field_151079_bi": "minecraft:ender_pearl",
    "Items.field_151146_bM": "minecraft:minecart",
    "Items.field_151008_g": "minecraft:feather", "Items.field_151008_G": "minecraft:feather",
    "Items.field_151162_bE": "minecraft:flower_pot",
    "Items.field_151015_O": "minecraft:wheat", "Items.field_151172_bF": "minecraft:carrot",
    "Items.field_151100_aR": "minecraft:green_dye",
    "Items.field_151070_bp": "minecraft:spider_eye",
    "Items.field_151131_as": "minecraft:water_bucket",
    "Items.field_151099_b_a": "minecraft:writable_book", "Items.field_151099_bA": "minecraft:writable_book",
    "Items.field_151170_bI": "minecraft:poisonous_potato",
    "Items.field_151144_bL": "minecraft:skeleton_skull",
    "Items.field_151073_bk": "minecraft:ghast_tear",
    "Items.field_151117_aB": "minecraft:milk_bucket",
    "Items.field_151005_D": "minecraft:golden_axe", "Items.field_151006_E": "minecraft:golden_pickaxe",
    "Items.field_151011_C": "minecraft:golden_shovel",
    "Items.field_179555_bs": "minecraft:rabbit_hide", "Items.field_151147_al": "minecraft:porkchop",
    "Items.field_179561_bm": "minecraft:mutton", "Items.field_151082_bd": "minecraft:beef",
    "Blocks.field_150438_bZ": "minecraft:hopper",
    "Blocks.field_150429_aA": "minecraft:redstone_torch", "Blocks.field_150333_U": "minecraft:stone_slab",
    "Items.field_151132_bS": "minecraft:comparator",
    "Items.field_151080_bb": "minecraft:pumpkin_seeds", "Items.field_151081_bc": "minecraft:melon_seeds",
    "Items.field_185163_cU": "minecraft:beetroot_seeds", "Items.field_151120_aE": "minecraft:sugar_cane",
    "Blocks.field_150434_aF": "minecraft:cactus",
    "Items.field_151115_aP": "#c:foods/raw_fish",  # 1.12 fish@32767 = any fish "Items.field_151021_T": "minecraft:leather_boots",
    "Items.field_151046_w": "minecraft:diamond_pickaxe", "Items.field_151047_v": "minecraft:diamond_shovel",
    "Items.field_151111_a_l": "minecraft:compass",
    "Items.field_151148_b_j": "minecraft:map",
    "Items.field_151166_bC": "minecraft:emerald", "Items.field_151166_b_c": "minecraft:emerald",
    "Items.field_150451_b_x": "minecraft:redstone_block", "Blocks.field_150451_bX": "minecraft:redstone_block",
    "Items.field_150479_b_c": "minecraft:tripwire_hook", "Blocks.field_150479_bC": "minecraft:tripwire_hook",
    "Items.field_150335_w": "minecraft:tnt", "Blocks.field_150335_W": "minecraft:tnt",
    "Items.field_151111_aL": "minecraft:compass", "Items.field_151148_bJ": "minecraft:map",
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
        if tok.startswith(k) and (len(tok) == len(k) or not tok[len(k)].isalnum()):
            return canon(v)
    for k, v in VANILLA_SRG.items():
        if tok.startswith(k) and (len(tok) == len(k) or not tok[len(k)].isalnum()):
            return canon(v)
    if tok in VANILLA_SRG:
        return VANILLA_SRG[tok]
    m = re.match(r"^(ItemsTC|BlocksTC)\.([A-Za-z0-9_]+)", tok)
    if m:
        return canon("thaumcraft:" + snake(m.group(2)))
    m = re.match(r'^(Items|Blocks)\.([A-Za-z0-9_]+)', tok)
    if m:
        return canon("minecraft:" + snake(m.group(2)))
    if tok.startswith('"') and tok.endswith('"'):
        name = tok.strip('"')
        if name in OREDICT:
            return canon(OREDICT[name])
        return "OREDICT:" + name
    if tok.startswith("Ingredient.fromTag"):
        return "TAG?" + tok
    if "getSealStack" in tok:
        m = re.search(r'thaumcraft:([a-z_]+)', tok)
        return "thaumcraft:seal_" + m.group(1) if m else tok
    m = re.search(r"makeCrystal\(Aspect\.(\w+)\)", tok)
    if m:
        return "thaumcraft:vis_crystal_" + m.group(1).lower()
    if "makeCrystal" in tok:
        return "thaumcraft:crystal_essence"
    m = re.search(r"ConfigItems\.([A-Z]+)_CRYSTAL", tok)
    if m:
        # ConfigItems.<ELEMENT>_CRYSTAL -> the vis crystal ITEM for that element
        return "thaumcraft:vis_crystal_" + m.group(1).lower()
    if "Ingredient.func_193367_a" in tok:
        m = re.search(r"Ingredient\.func_193367_a\(([^)]*)\)", tok)
        if m:
            return map_token(m.group(1).strip())
    if "Ingredient.func_193369_a(nitorStacks)" in tok:
        return "#thaumcraft:nitor"
    if "makeFilledPhial" in tok:
        m = re.search(r"makeFilledPhial\(Aspect\.(\w+)\)", tok)
        return "POTION_COMPONENT:thaumcraft:phial_filled:" + (ASPECT_MAP.get(m.group(1), "") if m else "")
    if tok in ("pis1", "pis2"):
        # 1.12 local vars in ConfigRecipes: pis1 = strong healing, pis2 = strong regeneration
        return {"pis1": "POTION_COMPONENT:minecraft:potion:strong_healing",
                "pis2": "POTION_COMPONENT:minecraft:potion:strong_regeneration"}[tok]
    return "RAW:" + tok


def add_ing(ings, tok, cnt=1):
    """Add a 1.12 ingredient token; new Object[]{...} = multi-ingredient (all required)."""
    m = re.search(r"new Object\[\](?:\[\])?\{.*", tok.strip(), re.S)
    if m:
        for im in re.finditer(r"new ItemStack\(([^)]*)\)|\"([A-Za-z][A-Za-z0-9_]*)\"", m.group(0)):
            t = im.group(1) or ('"%s"' % im.group(2))
            ings[map_token(t)] += cnt
        return
    ings[map_token(tok)] += cnt


def norm_112(e):
    """Return (station, research, vis, aspects, ingredients multiset, center) for a 1.12 entry."""
    st = e["station"]
    res = {"station": st, "research": e.get("research", ""), "vis": e.get("vis", "")}
    if e.get("raw") and not (e.get("ingredients") or e.get("key") or e.get("ingredient")):
        # vanilla-style 1.12 conversion recipes captured as raw text only
        res["unparsed"] = True
        return res
    asp = {}
    for a, n in e.get("aspects", []):
        asp[ASPECT_MAP.get(a.upper(), a.lower())] = int(n)
    res["aspects"] = asp
    ings = collections.Counter()
    if st == "arcane":
        if "key" in e:
            for letter, tok in e.get("key", {}).items():
                cnt = "".join(e.get("pattern", [])).count(letter)
                add_ing(ings, tok, cnt or 1)
        else:
            for tok in e.get("ingredients", []):
                add_ing(ings, tok)
    elif st in ("crucible",):
        if e.get("ingredient"):
            add_ing(ings, e["ingredient"])
    elif st == "infusion":
        if e.get("center") or e.get("ingredients"):
            # structured parse from the extractor (1.12 ctor:
            # research, result, instability, aspects, center, *surrounding)
            if e.get("center"):
                # center is tracked separately (port recipes count it in "input", not ingredients)
                res["center"] = map_token(e["center"])
            for tok in e.get("ingredients", []):
                add_ing(ings, tok)
            if e.get("instability"):
                res["instability"] = e["instability"]
            res["ingredients"] = ings
            return res
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
    center_tok = d.get("center") or d.get("input")
    if center_tok:
        out["center"] = norm_port_tok(center_tok)
    if d.get("crystals"):
        out["crystals"] = d["crystals"]
    if d.get("instability"):
        out["instability"] = d["instability"]
    out["type"] = st
    return out


def norm_port_tok(tok):
    if isinstance(tok, dict):
        tok = tok.get("item") or tok.get("tag") or json.dumps(tok)
    if isinstance(tok, str) and tok.startswith("{"):
        # neoforge:components ingredient -> canonical pseudo-token for comparison
        try:
            d = json.loads(tok)
            item = d.get("items", "?")
            comp = d.get("components", {})
            if "minecraft:potion_contents" in comp:
                pot = comp["minecraft:potion_contents"].get("potion", "")
                if pot.startswith("minecraft:"):
                    pot = pot[len("minecraft:"):]
                return "POTION_COMPONENT:" + item + ":" + pot
            cd = comp.get("minecraft:custom_data", {})
            asp = (cd.get("Aspects") or [{}])[0]
            return "POTION_COMPONENT:" + item + ":" + asp.get("key", "")
        except Exception:
            pass
    if isinstance(tok, str) and tok.startswith("#"):
        return canon(tok)
    return canon(tok)


def norm_gate(g):
    """1.12 bare research key = stage 1, same as key@1."""
    if g.endswith("@1"):
        return g[:-2]
    return g

# ---------------------------------------------------------------- 1:1 aliases
# Per-recipe, per-ingredient aliases for documented 1:1 mappings where the 1.12
# dump loses information (item metas) or 26.3 has no equivalent oredict.
# Keyed by (1.12 recipe id, 1.12 ingredient ref) -> canonical ref used on both sides.
ING_ALIAS_112 = {
    # 1.12 AdvancedCrossbow uses ItemsTC.mind meta 1 (Clockwork Mind) = port brain_curious
    ("AdvancedCrossbow", "thaumcraft:mind"): "thaumcraft:brain_curious",
    # 1.12 FocusPouch uses ItemsTC.baubles meta 2 = girdle_mundane (ItemBaubles meta map 0/1/2=amulet/ring/girdle mundane)
    ("FocusPouch", "thaumcraft:baubles"): "thaumcraft:girdle_mundane",
    # 1.12 cinnabar purification uses oreDict "oreCinnabar"; 26.3 has no c: tag for it,
    # the port uses the dedicated cinnabar_ore block item (same single member)
    ("metal_purification_cinnabar", "#c:ores/cinnabar"): "thaumcraft:cinnabar_ore",
}


def aliased_112(ing, rid):
    return {ING_ALIAS_112.get((rid, k), k): v for k, v in ing.items()}


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
    diff_pairs = 0
    for res, plist in sorted(port.items()):
        refs = ref_by_result.get(res, [])
        if not refs:
            if mode == "unmatched":
                print(f"NO 1.12 MATCH: {res}  ({plist[0][1]})")
            continue
        for station, rel, d in plist:
            # prefer the 1.12 reference whose station matches this port recipe's dir
            cands = [r for r in refs if r[1]["station"] == STATION_DIR.get(station, station)] or refs
            rid, e = cands[0]
            a = norm_112(e)
            b = norm_port(d)
            a["ingredients"] = aliased_112(a["ingredients"], rid)
            matched += 1
            issues = []
            if rel in KNOWN_112_MATCHES:
                if mode == "diff":
                    continue
                issues.append(f"1.12 counterpart verified manually: {KNOWN_112_MATCHES[rel]}")
            if a.get("unparsed"):
                issues.append("1.12 reference is raw-only (vanilla conversion recipe, not structurally compared)")
                if mode == "diff":
                    continue
            if a["station"] != STATION_DIR.get(station, station):
                issues.append(f"station 1.12={a['station']} port={station}")
            ga, gb = norm_gate(a["research"]), norm_gate(b["research"])
            if ga and gb and ga != gb:
                issues.append(f"research 1.12={a['research']} port={b['research']}")
            if ga and not gb:
                issues.append(f"port has no research gate (1.12 {a['research']})")
            if b.get("bad_aspects"):
                issues.append(f"UNREGISTERED aspect key(s) {b['bad_aspects']}")
            if a["aspects"] and b["aspects"] and a["aspects"] != b["aspects"]:
                issues.append(f"aspects 1.12={a['aspects']} port={b['aspects']}")
            if a["ingredients"] != b["ingredients"]:
                issues.append("ingredients differ")
            if mode == "diff" and not issues:
                continue
            if mode == "diff":
                diff_pairs += 1
            print(f"=== {res}  [{rel}]  (1.12 {rid} L{e['line']})")
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
    if mode == "diff":
        print(f"divergent pairs: {diff_pairs}")
        return 1 if diff_pairs else 0
    return 0


sys.exit(main())
