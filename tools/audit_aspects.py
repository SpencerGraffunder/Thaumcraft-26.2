#!/usr/bin/env python3
"""Audit 1: 1.12 ConfigAspects vs port ConfigAspects — missing/divergent aspect values.

Compares every registerObjectTag in the 1.12 reference against the port,
normalizing references:
  - 1.12 oredict names  -> port c: tag names
  - 1.12 SRG fields     -> modern vanilla ids
  - BlocksTC/ItemsTC    -> port thaumcraft ids
Meta 32767 ("all metas") is matched against ANY meta of the target.
"""
import re, sys, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
# 1.12 reference: stable in-repo copy (.reference/, gitignored, see tools/run_all_audits.sh)
_REF_CANDIDATES = (
    os.environ.get('TC112_REF', ''),
    os.path.join(ROOT, '.reference', 'thaumcraft-1.12', 'src'),
    '/private/tmp/tc112src',
)
_REF = next((c for c in _REF_CANDIDATES if c and os.path.isfile(c + '/thaumcraft/common/config/ConfigAspects.java')), '')
if not _REF:
    print('ERROR: 1.12 reference not found (run tools/run_all_audits.sh bootstrap or set TC112_REF)')
    sys.exit(2)
REF = _REF + '/thaumcraft/common/config/ConfigAspects.java'
PORT = os.path.join(ROOT, 'src/main/java/thaumcraft/common/config/ConfigAspects.py'.replace('.py', '.java'))

# ---------------------------------------------------------------- mappings
# 1.12 oredict name -> port tag (or list of tags that collectively cover it)
ORE112 = {
    "blockCactus": ["c:plantable"],  # checked manually; placeholder
    "blockGlass": ["c:glass"],
    "bone": ["van:minecraft:bone"],
    "clusterCinnabar": ["tc:thaumcraft:cluster_cinnabar"],
    "clusterCopper": ["tc:thaumcraft:cluster_copper"],
    "clusterGold": ["tc:thaumcraft:cluster_gold"],
    "clusterIron": ["tc:thaumcraft:cluster_iron"],
    "clusterLead": ["tc:thaumcraft:cluster_lead"],
    "clusterQuartz": ["NOTMODELED:clusterQuartz"],  # port has no quartz cluster item
    "clusterSilver": ["tc:thaumcraft:cluster_silver"],
    "clusterTin": ["tc:thaumcraft:cluster_tin"],
    "cobblestone": ["van:minecraft:cobblestone"],
    "cropCarrot": ["van:minecraft:carrot"],
    "cropNetherWart": ["van:minecraft:nether_wart"],
    "cropPotato": ["van:minecraft:potato"],
    "cropWheat": ["van:minecraft:wheat"],
    "dirt": ["van:minecraft:dirt"],
    "dustBrass": ["c:dusts/brass"],
    "dustBronze": ["c:dusts/bronze"],
    "dustCopper": ["c:dusts/copper"],
    "dustGlowstone": ["c:dusts/glowstone"],
    "dustGold": ["c:dusts/gold"],
    "dustIron": ["c:dusts/iron"],
    "dustLead": ["c:dusts/lead"],
    "dustRedstone": ["c:dusts/redstone"],
    "dustSilver": ["c:dusts/silver"],
    "dustTin": ["c:dusts/tin"],
    "egg": ["van:minecraft:egg"],
    "enderpearl": ["van:minecraft:ender_pearl"],
    "endstone": ["van:minecraft:end_stone"],
    "feather": ["van:minecraft:feather"],
    "gemAmber": ["tc:thaumcraft:amber"],  # tc item
    "gemDiamond": ["c:gems/diamond"],
    "gemEmerald": ["c:gems/emerald"],
    "gemGreenSapphire": ["c:gems/green_sapphire"],
    "gemQuartz": ["c:gems/quartz"],
    "gemRuby": ["c:gems/ruby"],
    "gemSapphire": ["c:gems/sapphire"],
    "glowstone": ["SPECIAL:glowstone-oredict-derives-from-block-item"],  # 1.12 oredict copies the (unregistered) glowstone block item = effectively empty
    "grass": ["van:minecraft:grass_block"],
    "gravel": ["van:minecraft:gravel"],
    "gunpowder": ["van:minecraft:gunpowder"],
    "ingotBrass": ["c:ingots/brass"],
    "ingotBrickNether": ["minecraft:nether_bricks"],
    "ingotBronze": ["c:ingots/bronze"],
    "ingotCopper": ["c:ingots/copper"],
    "ingotGold": ["c:ingots/gold"],
    "ingotIron": ["c:ingots/iron"],
    "ingotLead": ["c:ingots/lead"],
    "ingotSilver": ["c:ingots/silver"],
    "ingotSteel": ["c:ingots/steel"],
    "ingotTin": ["c:ingots/tin"],
    "ingotUranium": ["c:ingots/uranium"],
    "itemDropUranium": ["c:raw/uranium"],
    "itemRubber": ["c:rubber"],
    "leather": ["van:minecraft:leather"],
    "logWood": ["NOTMODELED:logWood"],  # 1.12 oredict only populated by other mods; no 26.3 standard tag
    "netherStar": ["van:minecraft:nether_star"],
    "netherrack": ["van:minecraft:netherrack"],
    "obsidian": ["van:minecraft:obsidian"],
    "oreAmber": ["thaumcraft:ores/amber"],
    "oreCinnabar": ["thaumcraft:ores/cinnabar"],
    "oreCopper": ["c:ores/copper"],
    "oreDiamond": ["c:ores/diamond"],
    "oreEmerald": ["c:ores/emerald"],
    "oreGold": ["c:ores/gold"],
    "oreIron": ["c:ores/iron"],
    "oreLapis": ["c:ores/lapis"],
    "oreLead": ["thaumcraft:ores/lead"],
    "oreQuartz": ["c:ores/quartz"],
    "oreRedstone": ["c:ores/redstone"],
    "oreSilver": ["thaumcraft:ores/silver"],
    "oreTin": ["thaumcraft:ores/tin"],
    "oreUranium": ["c:ores/uranium"],
    "quicksilver": ["tc:thaumcraft:quicksilver"],  # tc item
    "sand": ["c:sands"],  # port registers the 26.3 plural tag
    "slimeball": ["van:minecraft:slime_ball"],
    "stone": ["c:stones"],
    "stoneAndesite": ["van:minecraft:andesite"],
    "stoneDiorite": ["van:minecraft:diorite"],
    "stoneGranite": ["van:minecraft:granite"],
    "string": ["van:minecraft:string"],
    "sugarcane": ["van:minecraft:sugar_cane"],
    "treeLeaves": ["NOTMODELED:treeLeaves"],  # 1.12 oredict only populated by other mods
    "treeSapling": ["van:minecraft:oak_sapling", "van:minecraft:spruce_sapling", "van:minecraft:birch_sapling", "van:minecraft:jungle_sapling", "van:minecraft:acacia_sapling", "van:minecraft:dark_oak_sapling", "van:minecraft:cherry_sapling", "van:minecraft:pale_oak_sapling"],  # 26.3: no c:saplings; port registers each vanilla sapling directly
    "vine": ["van:minecraft:vine"],  # port key: Blocks.VINE -> van:minecraft:vine
}

# 1.12.2 SRG -> modern id (blocks + items). Uncommon ones verified against 1.12.2 mappings.
SRG_MCP = {  # 1.12.2 SRG -> MCP name, from MCP stable 39-1.12 fields.csv (authoritative)
    "field_150318_D": "GOLDEN_RAIL",
    "field_150319_E": "DETECTOR_RAIL",
    "field_150320_F": "STICKY_PISTON",
    "field_150321_G": "WEB",
    "field_150322_A": "SANDSTONE",
    "field_150323_B": "NOTEBLOCK",
    "field_150325_L": "WOOL",
    "field_150327_N": "YELLOW_FLOWER",
    "field_150328_O": "RED_FLOWER",
    "field_150329_H": "TALLGRASS",
    "field_150330_I": "DEADBUSH",
    "field_150331_J": "PISTON",
    "field_150337_Q": "RED_MUSHROOM",
    "field_150338_P": "BROWN_MUSHROOM",
    "field_150341_Y": "MOSSY_COBBLESTONE",
    "field_150342_X": "BOOKSHELF",
    "field_150346_d": "DIRT",
    "field_150349_c": "GRASS",
    "field_150357_h": "BEDROCK",
    "field_150360_v": "SPONGE",
    "field_150365_q": "COAL_ORE",
    "field_150367_z": "DISPENSER",
    "field_150378_br": "END_PORTAL_FRAME",
    "field_150380_bt": "DRAGON_EGG",
    "field_150381_bn": "ENCHANTING_TABLE",
    "field_150384_bq": "END_PORTAL",
    "field_150391_bh": "MYCELIUM",
    "field_150392_bi": "WATERLILY",
    "field_150398_cm": "DOUBLE_PLANT",
    "field_150403_cj": "PACKED_ICE",
    "field_150405_ch": "HARDENED_CLAY",
    "field_150406_ce": "STAINED_HARDENED_CLAY",
    "field_150408_cc": "ACTIVATOR_RAIL",
    "field_150409_cd": "DROPPER",
    "field_150415_aT": "TRAPDOOR",
    "field_150417_aV": "STONEBRICK",
    "field_150419_aX": "RED_MUSHROOM_BLOCK",
    "field_150420_aW": "BROWN_MUSHROOM_BLOCK",
    "field_150421_aI": "JUKEBOX",
    "field_150423_aK": "PUMPKIN",
    "field_150424_aL": "NETHERRACK",
    "field_150425_aM": "SOUL_SAND",
    "field_150426_aN": "GLOWSTONE",
    "field_150427_aO": "PORTAL",
    "field_150430_aB": "STONE_BUTTON",
    "field_150432_aD": "ICE",
    "field_150434_aF": "CACTUS",
    "field_150435_aG": "CLAY",
    "field_150438_bZ": "HOPPER",
    "field_150439_ay": "LIT_REDSTONE_ORE",
    "field_150440_ba": "MELON_BLOCK",
    "field_150442_at": "LEVER",
    "field_150443_bT": "HEAVY_WEIGHTED_PRESSURE_PLATE",
    "field_150445_bS": "LIGHT_WEIGHTED_PRESSURE_PLATE",
    "field_150447_bR": "TRAPPED_CHEST",
    "field_150448_aq": "RAIL",
    "field_150452_aw": "WOODEN_PRESSURE_PLATE",
    "field_150453_bW": "DAYLIGHT_DETECTOR",
    "field_150456_au": "STONE_PRESSURE_PLATE",
    "field_150458_ak": "FARMLAND",
    "field_150460_al": "FURNACE",
    "field_150461_bJ": "BEACON",
    "field_150462_ai": "CRAFTING_TABLE",
    "field_150471_bO": "WOODEN_BUTTON",
    "field_150473_bD": "TRIPWIRE",
    "field_150474_ac": "MOB_SPAWNER",
    "field_150477_bB": "ENDER_CHEST",
    "field_150478_aa": "TORCH",
    "field_150479_bC": "TRIPWIRE_HOOK",
    "field_150480_ab": "FIRE",
    "field_150486_ae": "CHEST",
    "field_151009_A": "MUSHROOM_STEW",
    "field_151014_N": "WHEAT_SEEDS",
    "field_151020_U": "CHAINMAIL_HELMET",
    "field_151022_W": "CHAINMAIL_LEGGINGS",
    "field_151023_V": "CHAINMAIL_CHESTPLATE",
    "field_151024_Q": "LEATHER_HELMET",
    "field_151026_S": "LEATHER_LEGGINGS",
    "field_151027_R": "LEATHER_CHESTPLATE",
    "field_151028_Y": "IRON_HELMET",
    "field_151029_X": "CHAINMAIL_BOOTS",
    "field_151030_Z": "IRON_CHESTPLATE",
    "field_151032_g": "ARROW",
    "field_151033_d": "FLINT_AND_STEEL",
    "field_151034_e": "APPLE",
    "field_151040_l": "IRON_SWORD",
    "field_151044_h": "COAL",
    "field_151054_z": "BOWL",
    "field_151057_cb": "NAME_TAG",
    "field_151060_bw": "SPECKLED_MELON",
    "field_151061_bv": "ENDER_EYE",
    "field_151062_by": "EXPERIENCE_BOTTLE",
    "field_151064_bs": "MAGMA_CREAM",
    "field_151065_br": "BLAZE_POWDER",
    "field_151066_bu": "CAULDRON",
    "field_151067_bt": "BREWING_STAND",
    "field_151068_bn": "POTIONITEM",
    "field_151069_bo": "GLASS_BOTTLE",
    "field_151070_bp": "SPIDER_EYE",
    "field_151071_bq": "FERMENTED_SPIDER_EYE",
    "field_151072_bj": "BLAZE_ROD",
    "field_151073_bk": "GHAST_TEAR",
    "field_151076_bf": "CHICKEN",
    "field_151077_bg": "COOKED_CHICKEN",
    "field_151078_bh": "ROTTEN_FLESH",
    "field_151080_bb": "PUMPKIN_SEEDS",
    "field_151081_bc": "MELON_SEEDS",
    "field_151082_bd": "BEEF",
    "field_151083_be": "COOKED_BEEF",
    "field_151084_co": "RECORD_WAIT",
    "field_151085_cm": "RECORD_WARD",
    "field_151086_cn": "RECORD_11",
    "field_151087_ck": "RECORD_STAL",
    "field_151088_cl": "RECORD_STRAD",
    "field_151089_ci": "RECORD_MALL",
    "field_151090_cj": "RECORD_MELLOHI",
    "field_151091_cg": "RECORD_CHIRP",
    "field_151092_ch": "RECORD_FAR",
    "field_151093_ce": "RECORD_CAT",
    "field_151094_cf": "RECORD_BLOCKS",
    "field_151096_cd": "RECORD_13",
    "field_151100_aR": "DYE",
    "field_151102_aT": "SUGAR",
    "field_151105_aU": "CAKE",
    "field_151106_aX": "COOKIE",
    "field_151107_aW": "REPEATER",
    "field_151112_aM": "FISHING_ROD",
    "field_151113_aN": "CLOCK",
    "field_151115_aP": "FISH",
    "field_151117_aB": "MILK_BUCKET",
    "field_151118_aC": "BRICK",
    "field_151119_aD": "CLAY_BALL",
    "field_151121_aF": "PAPER",
    "field_151122_aG": "BOOK",
    "field_151124_az": "BOAT",
    "field_151125_bZ": "DIAMOND_HORSE_ARMOR",
    "field_151126_ay": "SNOWBALL",
    "field_151127_ba": "MELON",
    "field_151129_at": "LAVA_BUCKET",
    "field_151131_as": "WATER_BUCKET",
    "field_151132_bS": "COMPARATOR",
    "field_151133_ar": "BUCKET",
    "field_151134_bR": "ENCHANTED_BOOK",
    "field_151136_bY": "GOLDEN_HORSE_ARMOR",
    "field_151138_bX": "IRON_HORSE_ARMOR",
    "field_151139_aw": "IRON_DOOR",
    "field_151141_av": "SADDLE",
    "field_151143_au": "MINECART",
    "field_151144_bL": "SKULL",
    "field_151145_ak": "FLINT",
    "field_151146_bM": "CARROT_ON_A_STICK",
    "field_151147_al": "PORKCHOP",
    "field_151150_bK": "GOLDEN_CARROT",
    "field_151153_ao": "GOLDEN_APPLE",
    "field_151157_am": "COOKED_PORKCHOP",
    "field_151158_bO": "PUMPKIN_PIE",
    "field_151162_bE": "FLOWER_POT",
    "field_151165_aa": "IRON_LEGGINGS",
    "field_151167_ab": "IRON_BOOTS",
    "field_151168_bH": "BAKED_POTATO",
    "field_151170_bI": "POISONOUS_POTATO",
    "field_179555_bs": "RABBIT_HIDE",
    "field_179556_br": "RABBIT_FOOT",
    "field_179557_bn": "COOKED_MUTTON",
    "field_179558_bo": "RABBIT",
    "field_179559_bp": "COOKED_RABBIT",
    "field_179561_bm": "MUTTON",
    "field_179562_cC": "PRISMARINE_SHARD",
    "field_179563_cD": "PRISMARINE_CRYSTALS",
    "field_179566_aV": "COOKED_FISH",
    "field_179567_at": "JUNGLE_DOOR",
    "field_179568_as": "BIRCH_DOOR",
    "field_179569_ar": "SPRUCE_DOOR",
    "field_179570_aq": "OAK_DOOR",
    "field_179571_av": "DARK_OAK_DOOR",
    "field_179572_au": "ACACIA_DOOR",
    "field_180385_bs": "DARK_OAK_FENCE_GATE",
    "field_180386_br": "JUNGLE_FENCE_GATE",
    "field_180387_bt": "ACACIA_FENCE_GATE",
    "field_180391_bp": "SPRUCE_FENCE_GATE",
    "field_180392_bq": "BIRCH_FENCE_GATE",
    "field_185150_aH": "SPRUCE_BOAT",
    "field_185151_aI": "BIRCH_BOAT",
    "field_185152_aJ": "JUNGLE_BOAT",
    "field_185153_aK": "ACACIA_BOAT",
    "field_185154_aL": "DARK_OAK_BOAT",
    "field_185155_bH": "SPLASH_POTION",
    "field_185156_bI": "LINGERING_POTION",
    "field_185157_bK": "DRAGON_BREATH",
    "field_185159_cQ": "SHIELD",
    "field_185160_cR": "ELYTRA",
    "field_185161_cS": "CHORUS_FRUIT",
    "field_185162_cT": "CHORUS_FRUIT_POPPED",
    "field_185163_cU": "BEETROOT_SEEDS",
    "field_185164_cV": "BEETROOT",
    "field_185166_h": "SPECTRAL_ARROW",
    "field_185167_i": "TIPPED_ARROW",
    "field_185764_cQ": "END_ROD",
    "field_185765_cR": "CHORUS_PLANT",
    "field_185766_cS": "CHORUS_FLOWER",
    "field_185774_da": "GRASS_PATH",
    "field_189877_df": "MAGMA",
    "field_190929_cY": "TOTEM_OF_UNDYING",
    "field_190930_cZ": "SHULKER_SHELL",
    "field_190975_dA": "BLACK_SHULKER_BOX",
    "field_190977_dl": "WHITE_SHULKER_BOX",
    "field_190978_dm": "ORANGE_SHULKER_BOX",
    "field_190979_dn": "MAGENTA_SHULKER_BOX",
    "field_190980_do": "LIGHT_BLUE_SHULKER_BOX",
    "field_190981_dp": "YELLOW_SHULKER_BOX",
    "field_190982_dq": "LIME_SHULKER_BOX",
    "field_190983_dr": "PINK_SHULKER_BOX",
    "field_190984_ds": "GRAY_SHULKER_BOX",
    "field_190985_dt": "SILVER_SHULKER_BOX",
    "field_190986_du": "CYAN_SHULKER_BOX",
    "field_190987_dv": "PURPLE_SHULKER_BOX",
    "field_190988_dw": "BLUE_SHULKER_BOX",
    "field_190989_dx": "BROWN_SHULKER_BOX",
    "field_190990_dy": "GREEN_SHULKER_BOX",
    "field_190991_dz": "RED_SHULKER_BOX",
    "field_192427_dB": "WHITE_GLAZED_TERRACOTTA",
    "field_192428_dC": "ORANGE_GLAZED_TERRACOTTA",
    "field_192429_dD": "MAGENTA_GLAZED_TERRACOTTA",
    "field_192430_dE": "LIGHT_BLUE_GLAZED_TERRACOTTA",
    "field_192431_dF": "YELLOW_GLAZED_TERRACOTTA",
    "field_192432_dG": "LIME_GLAZED_TERRACOTTA",
    "field_192433_dH": "PINK_GLAZED_TERRACOTTA",
    "field_192434_dI": "GRAY_GLAZED_TERRACOTTA",
    "field_192435_dJ": "SILVER_GLAZED_TERRACOTTA",
    "field_192436_dK": "CYAN_GLAZED_TERRACOTTA",
    "field_192437_dL": "PURPLE_GLAZED_TERRACOTTA",
    "field_192438_dM": "BLUE_GLAZED_TERRACOTTA",
    "field_192439_dN": "BROWN_GLAZED_TERRACOTTA",
    "field_192440_dO": "GREEN_GLAZED_TERRACOTTA",
    "field_192441_dP": "RED_GLAZED_TERRACOTTA",
    "field_192442_dQ": "BLACK_GLAZED_TERRACOTTA",
    "field_192443_dR": "CONCRETE",
    "field_192444_dS": "CONCRETE_POWDER",
}

# 1.12 BlocksTC name -> port id (block). None => not present in port (flag it).
TCB = {
    "arcaneWorkbench": "thaumcraft:arcane_workbench",
    "cinderpearl": "thaumcraft:cinderpearl",
    "crucible": "thaumcraft:crucible",
    "crystalAir": "thaumcraft:crystal_air",
    "crystalEarth": "thaumcraft:crystal_earth",
    "crystalEntropy": "thaumcraft:crystal_entropy",
    "crystalFire": "thaumcraft:crystal_fire",
    "crystalOrder": "thaumcraft:crystal_order",
    "crystalTaint": "thaumcraft:crystal_flux",  # port models crystalTaint as CRYSTAL_FLUX
    "crystalWater": "thaumcraft:crystal_water",
    "eldritch": "thaumcraft:eldritch",          # 8 metas -> multiple port blocks
    "grassAmbient": "thaumcraft:grass_ambient",
    "leafGreatwood": "thaumcraft:greatwood_leaves",
    "leafSilverwood": "thaumcraft:silverwood_leaves",
    "logGreatwood": "thaumcraft:greatwood_log",
    "logSilverwood": "thaumcraft:silverwood_log",
    "lootCrateCommon": "thaumcraft:loot_crate_common",
    "lootCrateRare": "thaumcraft:loot_crate_rare",
    "lootCrateUncommon": "thaumcraft:loot_crate_uncommon",
    "lootUrnCommon": "thaumcraft:loot_urn_common",
    "lootUrnRare": "thaumcraft:loot_urn_rare",
    "lootUrnUncommon": "thaumcraft:loot_urn_uncommon",
    "researchTable": "thaumcraft:research_table",
    "saplingGreatwood": "thaumcraft:greatwood_sapling",
    "saplingSilverwood": "thaumcraft:silverwood_sapling",
    "shimmerleaf": "thaumcraft:shimmerleaf",
    "stoneAncient": "thaumcraft:ancient_stone",
    "stoneAncientDoorway": "thaumcraft:ancient_pedestal_doorway",
    "stoneAncientGlyphed": "thaumcraft:ancient_stone_glyphed",
    "stoneAncientRock": "thaumcraft:ancient_rock",
    "stoneAncientTile": "thaumcraft:ancient_stone_tile",
    "stoneEldritchTile": "thaumcraft:eldritch_stone_tile",
    "stonePorous": "thaumcraft:porous_stone",
    "taintCrust": "thaumcraft:taint_crust",
    "taintFeature": "thaumcraft:taint_feature",
    "taintFibre": "thaumcraft:taint_fibre",
    "taintGeyser": "thaumcraft:taint_geyser",
    "taintLog": "thaumcraft:taint_log",
    "taintRock": "thaumcraft:taint_rock",
    "taintSoil": "thaumcraft:taint_soil",
    "vishroom": "thaumcraft:vishroom",
}

# 1.12 ItemsTC name -> port id (item). Meta-aware where noted.
TCI = {
    "amuletVis": {"*": ["thaumcraft:amulet_vis_crafted"]},  # port models only the crafted variant
    "baubles": {"*": []},                                  # baubles not modeled (documented deviation)
    "brain": {"*": ["thaumcraft:zombie_brain"]},
    "celestialNotes": {"*": ["thaumcraft:celestial_notes_%s" % t for t in ["sun", "stars_1", "stars_2", "stars_3", "stars_4", "moon_1", "moon_2", "moon_3", "moon_4", "moon_5", "moon_6", "moon_7", "moon_8"]]},
    "chunks": {"0": ["thaumcraft:chunks_beef"], "1": ["thaumcraft:chunks_chicken"], "2": ["thaumcraft:chunks_pork"], "3": ["thaumcraft:chunks_fish"], "4": ["thaumcraft:chunks_rabbit"], "5": ["thaumcraft:chunks_mutton"]},
    "crimsonBlade": {"*": ["thaumcraft:crimson_blade"]},
    "crimsonBoots": {"*": ["thaumcraft:crimson_boots"]},
    "crimsonPlateChest": {"*": ["thaumcraft:crimson_plate_chest"]},
    "crimsonPlateHelm": {"*": ["thaumcraft:crimson_plate_helm"]},
    "crimsonPlateLegs": {"*": ["thaumcraft:crimson_plate_legs"]},
    "crimsonPraetorChest": {"*": ["thaumcraft:crimson_praetor_chest"]},
    "crimsonPraetorHelm": {"*": ["thaumcraft:crimson_praetor_helm"]},
    "crimsonPraetorLegs": {"*": ["thaumcraft:crimson_praetor_legs"]},
    "crimsonRobeChest": {"*": ["thaumcraft:crimson_robe_chest"]},
    "crimsonRobeHelm": {"*": ["thaumcraft:crimson_robe_helm"]},
    "crimsonRobeLegs": {"*": ["thaumcraft:crimson_robe_legs"]},
    "curio": {"*": ["thaumcraft:curio"]},
    "eldritchEye": {"*": []},
    "lootBag": {"0": ["thaumcraft:loot_bag_common"], "1": ["thaumcraft:loot_bag_uncommon"], "2": ["thaumcraft:loot_bag_rare"]},
    "nuggets": {"10": ["thaumcraft:nugget_rareearth"]},   # 1.12 nuggets meta 10 = rare earth (check)
    "phial": {"0": ["thaumcraft:phial_empty"], "1": ["thaumcraft:phial_filled"], "*": ["thaumcraft:phial_empty", "thaumcraft:phial_filled"]},
    "primordialPearl": {"*": ["thaumcraft:primordial_pearl"]},
    "runedTablet": {"*": []},
    "salisMundus": {"*": ["thaumcraft:salis_mundus"]},
    "thaumonomicon": {"*": ["thaumcraft:thaumonomicon"]},
    "tripleMeatTreat": {"*": ["thaumcraft:triple_meat_treat"]},
}

# ---------------------------------------------------------------- port field->id maps (auto-extracted)
def port_id_maps():
    maps = {'ModBlocks': {}, 'ModItems': {}}
    for f, field in [('src/main/java/thaumcraft/init/ModBlocks.java', 'ModBlocks'),
                     ('src/main/java/thaumcraft/init/ModItems.java', 'ModItems')]:
        s = open(os.path.join(ROOT, f)).read()
        for k, v in re.findall(r'(\w+)\s*=\s*register(?:Block|Item)(?:NoItem)?\("([^"]+)"', s):
            maps[field][k] = 'thaumcraft:' + v
    return maps

PORT_IDS = port_id_maps()

# ---------------------------------------------------------------- parsing
# MCP name -> modern id (26.3). Default: lowercase. Exceptions below.
MCP2MOD = {
    'WEB': 'cobweb', 'WOOL': 'wool', 'TALLGRASS': 'tall_grass', 'DEADBUSH': 'dead_bush',
    'SANDSTONE': 'sandstone', 'STONEBRICK': 'stone_bricks', 'DOUBLE_PLANT': 'double_plant',
    'STAINED_HARDENED_CLAY': 'terracotta', 'LIT_REDSTONE_ORE': 'redstone_ore',
    'HARDENED_CLAY': 'clay', 'POTIONITEM': 'potion', 'SKULL': 'skull',
    'CHORUS_FRUIT_POPPED': 'popped_chorus_fruit', 'GRASS_PATH': 'path',
    'MOB_SPAWNER': 'mob_spawner', 'ENCHANTING_TABLE': 'enchanting_table',
    'END_PORTAL_FRAME': 'end_portal_frame', 'DAYLIGHT_DETECTOR': 'daylight_detector',
    'TRAPDOOR': 'oak_trapdoor', 'WOODEN_BUTTON': 'oak_button',
    'WOODEN_PRESSURE_PLATE': 'oak_pressure_plate', 'MUSHROOM_STEW': 'mushroom_stew',
    'NAME_TAG': 'name_tag', 'CLAY_BALL': 'clay_ball', 'MILK_BUCKET': 'milk_bucket',
    'LAVA_BUCKET': 'lava_bucket', 'WATER_BUCKET': 'water_bucket', 'MELON_BLOCK': 'melon',
    'MELON': 'melon_slice', 'BAKED_POTATO': 'baked_potato',
    'POISONOUS_POTATO': 'poisonous_potato', 'CARROT_ON_A_STICK': 'carrot_on_a_stick',
    'GOLDEN_CARROT': 'golden_carrot', 'GOLDEN_APPLE': 'golden_apple',
    'COOKED_PORKCHOP': 'cooked_porkchop', 'PUMPKIN_PIE': 'pumpkin_pie',
    'FLOWER_POT': 'flower_pot', 'SPECKLED_MELON': 'speckled_melon',
    'ENDER_EYE': 'ender_eye', 'EXPERIENCE_BOTTLE': 'experience_bottle',
    'MAGMA_CREAM': 'magma_cream', 'BLAZE_POWDER': 'blaze_powder',
    'BREWING_STAND': 'brewing_stand', 'GLASS_BOTTLE': 'glass_bottle',
    'FERMENTED_SPIDER_EYE': 'fermented_spider_eye', 'BLAZE_ROD': 'blaze_rod',
    'GHAST_TEAR': 'ghast_tear', 'CHICKEN': 'chicken', 'COOKED_CHICKEN': 'cooked_chicken',
    'ROTTEN_FLESH': 'rotten_flesh', 'BEEF': 'beef', 'COOKED_BEEF': 'cooked_beef',
    'WHEAT_SEEDS': 'wheat_seeds', 'MELON_SEEDS': 'melon_seeds', 'PUMPKIN_SEEDS': 'pumpkin_seeds',
    'RABBIT_HIDE': 'rabbit_hide', 'RABBIT_FOOT': 'rabbit_foot', 'COOKED_MUTTON': 'cooked_mutton',
    'RABBIT': 'rabbit', 'COOKED_RABBIT': 'cooked_rabbit', 'MUTTON': 'mutton',
    'PRISMARINE_SHARD': 'prismarine_shard', 'PRISMARINE_CRYSTALS': 'prismarine_crystals',
    'COOKED_FISH': 'cooked_cod', 'DRAGON_BREATH': 'dragon_breath', 'SHIELD': 'shield',
    'ELYTRA': 'elytra', 'CHORUS_FRUIT': 'chorus_fruit', 'BEETROOT_SEEDS': 'beetroot_seeds',
    'BEETROOT': 'beetroot', 'SPECTRAL_ARROW': 'spectral_arrow', 'TIPPED_ARROW': 'tipped_arrow',
    'END_ROD': 'end_rod', 'CHORUS_PLANT': 'chorus_plant', 'CHORUS_FLOWER': 'chorus_flower',
    'MAGMA': 'magma_block', 'TOTEM_OF_UNDYING': 'totem_of_undying', 'SHULKER_SHELL': 'shulker_shell',
    'YELLOW_FLOWER': 'dandelion', 'RED_FLOWER': 'poppy', 'RED_MUSHROOM': 'red_mushroom',
    'BROWN_MUSHROOM': 'brown_mushroom', 'MOSSY_COBBLESTONE': 'mossy_cobblestone',
    'BOOKSHELF': 'bookshelf', 'DIRT': 'dirt', 'GRASS': 'grass', 'BEDROCK': 'bedrock',
    'SPONGE': 'sponge', 'COAL_ORE': 'coal_ore', 'DISPENSER': 'dispenser',
    'DRAGON_EGG': 'dragon_egg', 'END_PORTAL': 'end_portal', 'MYCELIUM': 'mycelium',
    'WATERLILY': 'lily_pad', 'PACKED_ICE': 'packed_ice', 'ACTIVATOR_RAIL': 'activator_rail',
    'DROPPER': 'dropper', 'RED_MUSHROOM_BLOCK': 'red_mushroom_block',
    'BROWN_MUSHROOM_BLOCK': 'brown_mushroom_block', 'JUKEBOX': 'jukebox',
    'PUMPKIN': 'pumpkin', 'NETHERRACK': 'netherrack', 'SOUL_SAND': 'soulsand',
    'GLOWSTONE': 'glowstone', 'PORTAL': 'nether_portal', 'ICE': 'ice', 'CACTUS': 'cactus',
    'CLAY': 'clay_block', 'HOPPER': 'hopper', 'LEVER': 'lever',
    'HEAVY_WEIGHTED_PRESSURE_PLATE': 'heavy_weighted_pressure_plate',
    'LIGHT_WEIGHTED_PRESSURE_PLATE': 'light_weighted_pressure_plate',
    'TRAPPED_CHEST': 'trapped_chest', 'RAIL': 'rail', 'GOLDEN_RAIL': 'golden_rail',
    'DETECTOR_RAIL': 'detector_rail', 'STONE_PRESSURE_PLATE': 'stone_pressure_plate',
    'FARMLAND': 'farmland', 'FURNACE': 'furnace', 'BEACON': 'beacon',
    'CRAFTING_TABLE': 'crafting_table', 'TRIPWIRE': 'tripwire', 'ENDER_CHEST': 'ender_chest',
    'TORCH': 'torch', 'TRIPWIRE_HOOK': 'tripwire_hook', 'FIRE': 'fire', 'CHEST': 'chest',
    'STICKY_PISTON': 'sticky_piston', 'PISTON': 'piston', 'NOTEBLOCK': 'note_block',
    'FISH': 'cod', 'BOAT': 'oak_boat', 'SADDLE': 'saddle', 'MINECART': 'minecart',
    'FLINT': 'flint', 'PORKCHOP': 'porkchop', 'ARROW': 'arrow',
    'FLINT_AND_STEEL': 'flint_and_steel', 'IRON_SWORD': 'iron_sword', 'COAL': 'coal',
    'BOWL': 'bowl', 'DYE': 'dye', 'SUGAR': 'sugar', 'CAKE': 'cake', 'COOKIE': 'cookie',
    'REPEATER': 'repeater', 'FISHING_ROD': 'fishing_rod', 'CLOCK': 'clock',
    'BRICK': 'brick', 'PAPER': 'paper', 'BOOK': 'book',
    'ENCHANTED_BOOK': 'enchanted_book', 'SNOWBALL': 'snowball', 'APPLE': 'apple',
}

# meta-specific splits (1.12 meta -> modern id)
MCP_METAS = {
    'SKULL': {0: 'skeleton_skull', 1: 'wither_skeleton_skull', 2: 'zombie_head',
              3: 'player_head', 4: 'piglin_head', 5: 'dragon_head'},
    'DYE': {0: 'white_dye', 2: 'light_blue_dye', 3: 'yellow_dye', 4: 'light_gray_dye',
            15: 'orange_dye'},
    'STONEBRICK': {1: 'mossy_stone_bricks', 2: 'cracked_stone_bricks', 3: 'chiseled_stone_bricks'},
    'SANDSTONE': {1: 'chiseled_sandstone', 2: 'smooth_sandstone'},
    'SPONGE': {1: 'wet_sponge'},
    'DOUBLE_PLANT': {0: 'sunflower', 1: 'rose_bush', 2: 'peony', 3: 'NOTMODELED_bee_flower',
                     4: 'golden_dandelion', 5: 'wither_rose'},
}

def mcp_key(mcp, meta):
    meta_i = int(meta)
    if mcp in MCP_METAS and meta_i != 32767 and meta_i in MCP_METAS[mcp]:
        return f"van:minecraft:{MCP_METAS[mcp][meta_i]}:m{meta_i}"
    mod = MCP2MOD.get(mcp, mcp.lower())
    # per-color families: prefix pattern (record/shulker/glazed/door/boat/fence gate)
    if mcp.startswith('RECORD_'):
        mod = 'music_disc_' + mcp[7:].lower()
    return f"van:minecraft:{mod}:m{meta_i}"

def parse(path, is_ref):
    """Return list of (refkey, aspects) where refkey is a canonical string,
    aspects is a frozenset of (aspect, value) tuples."""
    src = open(path, encoding='utf-8').read()
    out = []
    # grab every complete registerObjectTag(...) call (may span multiple lines)
    for m in re.finditer(r'registerObjectTag\(', src):
        i = m.end(); depth = 1
        while i < len(src) and depth > 0:
            if src[i] == '(': depth += 1
            elif src[i] == ')': depth -= 1
            i += 1
        line = src[m.end():i-1]
        if 'AspectList' not in line and 'getPotionAspects' not in line:
            continue
        # split ref and aspectlist
        # find the aspectlist part
        am = re.search(r'(?:new AspectList\(\)(?:\s*\.add\([^)]*\))*)|getPotionAspects\(.*?\)', line)
        if not am:
            continue
        aspects = re.findall(r'Aspect\.(\w+)\s*,\s*(\d+)', am.group(0))
        aspects = frozenset((a, int(v)) for a, v in aspects)
        head = line[:am.start()].strip().rstrip(',').strip()
        refkey = normalize_ref(head, is_ref)
        if refkey:
            out.append((refkey, aspects))
    return out

# 1.12 meta variants that are separate items/blocks in 26.3
KEY_ALIASES = {
    "van:minecraft:dirt:m2": "van:minecraft:podzol",
    "van:minecraft:soulsand": "van:minecraft:soul_sand",
    "van:minecraft:soulsand:m32767": "van:minecraft:soul_sand",
    "van:minecraft:path": "van:minecraft:dirt_path",
    "van:minecraft:mob_spawner": "van:minecraft:spawner",
    "van:minecraft:dandelion:m4": "van:minecraft:golden_dandelion",
}

def _normalize_ref_inner(head, is_ref):
    head = head.strip()
    if not head:
        return None
    # oredict / tag string
    m = re.match(r'^"([^"]+)"$', head)
    if m:
        name = m.group(1)
        if is_ref:
            if name in ORE112:
                return "tag:" + "/;".join(ORE112[name])
            return "tag?:" + name
        return "tag:" + name
    # stack
    m = re.match(r'^new ItemStack\(([^,]+)(?:\s*,\s*(\d+)(?:\s*,\s*(\d+))?)?\)$', head)
    if m:
        inner, cnt, meta = m.group(1), m.group(2), m.group(3)
        meta = meta if meta else "0"
        if is_ref:
            mt = re.match(r'^(BlocksTC|ItemsTC)\.(\w+)$', inner.strip())
            if mt:
                kind, name = mt.groups()
                tbl = TCB if kind == 'BlocksTC' else TCI
                if name in tbl:
                    if kind == 'BlocksTC':
                        return f"tc:{tbl[name]}:m{meta}"
                    metas = tbl[name]
                    cands = metas.get(meta, None) or metas.get("*")
                    if not cands:
                        return f"tc-missing:{name}:m{meta}"
                    return "tc:" + "/;".join(cands) + f":m{meta}"
                return f"tc-unmapped:{kind}.{name}:m{meta}"
            mt = re.match(r'^(Blocks|Items)\.(field_\w+)$', inner.strip())
            if mt:
                mcp = SRG_MCP.get(mt.group(2))
                if not mcp:
                    return f"van-unmapped:{inner.strip()}:m{meta}"
                return mcp_key(mcp, meta)
            if 'FluidUtil' in head or 'universalBucket' in head or 'getPotionAspects' in head:
                return "special:" + re.sub(r'\s+', '', head)[:40]
            return "unmapped:" + head[:60]
        else:
            mt = re.match(r'^(ModBlocks|ModItems)\.(\w+)\.get\(\)$', inner.strip())
            if mt:
                fid = PORT_IDS[mt.group(1)].get(mt.group(2))
                return f"tc:{fid}:m{meta}" if fid else f"tc-portunmapped:{mt.group(2)}:m{meta}"
            inner2 = re.sub(r'\.pick\(DyeColor\.\w+\)', '', inner.strip())
            mt = re.match(r'^(Blocks|Items)\.(\w+)$', inner2)
            if mt:
                mod = mt.group(2)
                return f"van:{'minecraft:' + mod.lower()}:m{meta}"
            if 'FluidUtil' in head or 'universalBucket' in head:
                return "special:" + re.sub(r'\s+', '', head)[:40]
            return "unmapped:" + head[:60]
    key = "unmapped:" + head[:60]
    return KEY_ALIASES.get(key, key)


def normalize_ref(head, is_ref):
    key = _normalize_ref_inner(head, is_ref)
    if key is None:
        return None
    if key in KEY_ALIASES:
        return KEY_ALIASES[key]
    base = re.sub(r':m\d+$', '', key)  # meta-tolerant alias (1.12 meta stacks -> 26.3 separate items)
    return KEY_ALIASES.get(base, key)

def main():
    ref = parse(REF, True)
    port = parse(PORT, False)
    print(f"1.12 entries: {len(ref)}   port entries: {len(port)}")

    # index port by base id (strip meta); tag keys match on the part before ':'
    port_base = {}
    for k, a in port:
        base = re.sub(r':m\d+$', '', k)
        port_base.setdefault(base, []).append((k, a))

    def find_cands(key):
        """Find port candidates for a (possibly multi-candidate) ref key."""
        cands = []
        for part in key.split('/;'):
            base = re.sub(r':m\d+$', '', part)
            if base.startswith('tag:'):
                # tag: may list several tags; match if any port entry has that tag
                # (or the name resolves to a registered item id)
                for t in base[4:].split('/;'):
                    if t.startswith(('van:', 'tc:')):
                        # oredict mapped directly to a registered item id
                        for pk, pa in port_base.get(t, []):
                            cands.append((pk, pa))
                        continue
                    hit = port_base.get('tag:' + t, [])
                    if not hit:
                        hit = port_base.get('tc:thaumcraft:' + t, []) or port_base.get('van:minecraft:' + t, [])
                    for pk, pa in hit:
                        cands.append((pk, pa))
            else:
                for pk, pa in port_base.get(base, []):
                    cands.append((pk, pa))
        # dedupe
        seen = set(); out = []
        for c in cands:
            if c not in seen:
                seen.add(c); out.append(c)
        return out

    ok = 0
    divergent, missing, unmapped, special, notmodeled = [], [], [], [], []
    for k, a in ref:
        if re.match(r'^tc:thaumcraft:eldritch:m\d+$', k):
            notmodeled.append((k, sorted(a)))  # 1.12's 8-meta eldritch block family has no port equivalent
            continue
        if k.startswith(('unmapped:', 'van-unmapped:', 'tc-unmapped:', 'tc-portunmapped:', 'tag?:', 'van-port:')):
            # 1.12 potion loop (registerObjectTag(stackN, getPotionAspects(stackN)+add)): NBT potion data,
            # represented by data components in 26.3 - not statically modelable
            if k.startswith('unmapped:stack') or k.startswith('special:'):
                special.append((k, sorted(a)))
                continue
            unmapped.append((k, sorted(a)))
            continue
        if k.startswith('SPECIAL:'):
            special.append((k[8:], sorted(a)))
            continue
        if k.startswith('NOTMODELED:') or ':NOTMODELED' in k:
            notmodeled.append((k, sorted(a)))
            continue
        if not a:
            special.append((k + "  (1.12 registered an EMPTY aspect list - 1.12 artifact, e.g. phial meta 1)", sorted(a)))
            continue
        if re.match(r'^van:minecraft:(white|yellow|light_gray|orange)_dye:m\d+$', k):
            notmodeled.append((k, sorted(a)))  # 26.3: single component-based dye item; per-color static registration impossible
            continue
        if k == 'van:minecraft:NOTMODELED_bee_flower:m3':
            notmodeled.append((k, sorted(a)))  # bee flower removed in 26.3
            continue
            unmapped.append((k, sorted(a)))
            continue
        if k.startswith('special:'):
            special.append((k, sorted(a)))
            continue
        if k.startswith('tc-missing:'):
            notmodeled.append((k, sorted(a)))
            continue
        cands = find_cands(k)
        if not cands:
            missing.append((k, sorted(a)))
            continue
        if any(a == pa for _, pa in cands):
            ok += 1
        else:
            divergent.append((k, sorted(a), cands))

    print(f"\n=== OK (aspect list matches): {ok}")
    print(f"\n=== DIVERGENT ({len(divergent)}):")
    for k, a, cands in divergent:
        print(f"  {k}\n    1.12: {a}")
        for ck, pa in cands[:4]:
            print(f"    port {ck}: {sorted(pa)}")
    print(f"\n=== MISSING from port ({len(missing)}):")
    for k, a in missing:
        print(f"  {k}: {a}")
    print(f"\n=== TC NOT MODELED IN PORT ({len(notmodeled)}):")
    for k, a in notmodeled:
        print(f"  {k}: {a}")
    print(f"\n=== SPECIAL (fluids/buckets/potions — check manually) ({len(special)}):")
    for k, a in special:
        print(f"  {k}: {a}")
    print(f"\n=== UNMAPPED refs (need mapping work) ({len(unmapped)}):")
    for k, a in unmapped:
        print(f"  {k}: {a}")

if __name__ == '__main__':
    main()
