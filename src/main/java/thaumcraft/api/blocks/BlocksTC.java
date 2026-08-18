package thaumcraft.api.blocks;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.init.ModBlocks;

/**
 * References to Thaumcraft blocks.
 * For 1.20.1, these point to the RegistryObjects in ModBlocks.
 * 
 * Note: Accessing .get() on RegistryObjects is only safe after the block registry event has fired.
 */
public class BlocksTC {

    // Stone Blocks
    public static final DeferredHolder<Block> stoneArcane = ModBlocks.ARCANE_STONE;
    public static final DeferredHolder<Block> stoneArcaneBrick = ModBlocks.ARCANE_STONE_BRICK;
    public static final DeferredHolder<Block> stoneAncient = ModBlocks.ANCIENT_STONE;
    public static final DeferredHolder<Block> stoneAncientTile = ModBlocks.ANCIENT_STONE_TILE;
    public static final DeferredHolder<Block> stoneAncientRock = ModBlocks.ANCIENT_STONE_ROCK;
    public static final DeferredHolder<Block> stoneAncientGlyphed = ModBlocks.ANCIENT_STONE_GLYPHED;
    public static final DeferredHolder<Block> stoneAncientDoorway = ModBlocks.ANCIENT_STONE_DOORWAY;
    public static final DeferredHolder<Block> stoneEldritchTile = ModBlocks.ELDRITCH_STONE_TILE;
    public static final DeferredHolder<Block> stonePorous = ModBlocks.POROUS_STONE;

    // Stairs
    public static final DeferredHolder<Block> stairsArcane = ModBlocks.ARCANE_STONE_STAIRS;
    public static final DeferredHolder<Block> stairsArcaneBrick = ModBlocks.ARCANE_STONE_BRICK_STAIRS;
    public static final DeferredHolder<Block> stairsAncient = ModBlocks.ANCIENT_STONE_STAIRS;
    
    // Slabs
    public static final DeferredHolder<Block> slabArcaneStone = ModBlocks.ARCANE_STONE_SLAB;
    public static final DeferredHolder<Block> slabArcaneBrick = ModBlocks.ARCANE_STONE_BRICK_SLAB;
    public static final DeferredHolder<Block> slabAncient = ModBlocks.ANCIENT_STONE_SLAB;

    // Pillars
    public static final DeferredHolder<Block> pillarArcane = ModBlocks.ARCANE_PILLAR;
    public static final DeferredHolder<Block> pillarAncient = ModBlocks.ANCIENT_PILLAR;
    public static final DeferredHolder<Block> pillarEldritch = ModBlocks.ELDRITCH_PILLAR;

    // Wood Blocks
    public static final DeferredHolder<Block> logGreatwood = ModBlocks.GREATWOOD_LOG;
    public static final DeferredHolder<Block> logSilverwood = ModBlocks.SILVERWOOD_LOG;
    public static final DeferredHolder<Block> plankGreatwood = ModBlocks.GREATWOOD_PLANKS;
    public static final DeferredHolder<Block> plankSilverwood = ModBlocks.SILVERWOOD_PLANKS;
    public static final DeferredHolder<Block> stairsGreatwood = ModBlocks.GREATWOOD_STAIRS;
    public static final DeferredHolder<Block> stairsSilverwood = ModBlocks.SILVERWOOD_STAIRS;
    public static final DeferredHolder<Block> slabGreatwood = ModBlocks.GREATWOOD_SLAB;
    public static final DeferredHolder<Block> slabSilverwood = ModBlocks.SILVERWOOD_SLAB;
    public static final DeferredHolder<Block> leafGreatwood = ModBlocks.GREATWOOD_LEAVES;
    public static final DeferredHolder<Block> leafSilverwood = ModBlocks.SILVERWOOD_LEAVES;
    public static final DeferredHolder<Block> saplingGreatwood = ModBlocks.GREATWOOD_SAPLING;
    public static final DeferredHolder<Block> saplingSilverwood = ModBlocks.SILVERWOOD_SAPLING;

    // Metal Blocks
    public static final DeferredHolder<Block> metalBlockBrass = ModBlocks.BRASS_BLOCK;
    public static final DeferredHolder<Block> metalBlockThaumium = ModBlocks.THAUMIUM_BLOCK;
    public static final DeferredHolder<Block> metalBlockVoid = ModBlocks.VOID_METAL_BLOCK;
    public static final DeferredHolder<Block> metalAlchemical = ModBlocks.ALCHEMICAL_BRASS_BLOCK;
    public static final DeferredHolder<Block> metalAlchemicalAdvanced = ModBlocks.ALCHEMICAL_BRASS_ADVANCED_BLOCK;

    // Amber
    public static final DeferredHolder<Block> amberBlock = ModBlocks.AMBER_BLOCK;
    public static final DeferredHolder<Block> amberBrick = ModBlocks.AMBER_BRICK;

    // Matrix
    public static final DeferredHolder<Block> matrixSpeed = ModBlocks.MATRIX_SPEED;
    public static final DeferredHolder<Block> matrixCost = ModBlocks.MATRIX_COST;

    // Crafting
    public static final DeferredHolder<Block> arcaneWorkbench = ModBlocks.ARCANE_WORKBENCH;
    public static final DeferredHolder<Block> crucible = ModBlocks.CRUCIBLE;
    public static final DeferredHolder<Block> researchTable = ModBlocks.RESEARCH_TABLE;
    public static final DeferredHolder<Block> infusionMatrix = ModBlocks.INFUSION_MATRIX;
    public static final DeferredHolder<Block> focalManipulator = ModBlocks.FOCAL_MANIPULATOR;
    public static final DeferredHolder<Block> thaumatorium = ModBlocks.THAUMATORIUM;
    public static final DeferredHolder<Block> patternCrafter = ModBlocks.PATTERN_CRAFTER;
    public static final DeferredHolder<Block> golemBuilder = ModBlocks.GOLEM_BUILDER;

    // Ores
    public static final DeferredHolder<Block> oreAmber = ModBlocks.AMBER_ORE;
    public static final DeferredHolder<Block> oreCinnabar = ModBlocks.CINNABAR_ORE;
    public static final DeferredHolder<Block> oreQuartz = ModBlocks.QUARTZ_ORE;

    // Deepslate Ores
    public static final DeferredHolder<Block> deepslateOreAmber = ModBlocks.DEEPSLATE_AMBER_ORE;
    public static final DeferredHolder<Block> deepslateOreCinnabar = ModBlocks.DEEPSLATE_CINNABAR_ORE;
    public static final DeferredHolder<Block> deepslateOreQuartz = ModBlocks.DEEPSLATE_QUARTZ_ORE;

    // Crystals
    public static final DeferredHolder<Block> crystalAir = ModBlocks.CRYSTAL_AIR;
    public static final DeferredHolder<Block> crystalFire = ModBlocks.CRYSTAL_FIRE;
    public static final DeferredHolder<Block> crystalWater = ModBlocks.CRYSTAL_WATER;
    public static final DeferredHolder<Block> crystalEarth = ModBlocks.CRYSTAL_EARTH;
    public static final DeferredHolder<Block> crystalOrder = ModBlocks.CRYSTAL_ORDER;
    public static final DeferredHolder<Block> crystalEntropy = ModBlocks.CRYSTAL_ENTROPY;
    public static final DeferredHolder<Block> crystalTaint = ModBlocks.CRYSTAL_FLUX;

    // Plants
    public static final DeferredHolder<Block> shimmerleaf = ModBlocks.SHIMMERLEAF;
    public static final DeferredHolder<Block> cinderpearl = ModBlocks.CINDERPEARL;
    public static final DeferredHolder<Block> vishroom = ModBlocks.VISHROOM;

    // Devices
    public static final DeferredHolder<Block> pedestalArcane = ModBlocks.PEDESTAL_ARCANE;
    public static final DeferredHolder<Block> pedestalAncient = ModBlocks.PEDESTAL_ANCIENT;
    public static final DeferredHolder<Block> pedestalEldritch = ModBlocks.PEDESTAL_ELDRITCH;
    public static final DeferredHolder<Block> tableWood = ModBlocks.TABLE_WOOD;
    public static final DeferredHolder<Block> tableStone = ModBlocks.TABLE_STONE;
    public static final DeferredHolder<Block> rechargePedestal = ModBlocks.RECHARGE_PEDESTAL;
    public static final DeferredHolder<Block> bellows = ModBlocks.BELLOWS;
    public static final DeferredHolder<Block> hungryChest = ModBlocks.HUNGRY_CHEST;
    public static final DeferredHolder<Block> mirror = ModBlocks.MIRROR_ITEM;
    public static final DeferredHolder<Block> mirrorEssentia = ModBlocks.MIRROR_ESSENTIA;
    public static final DeferredHolder<Block> stabilizer = ModBlocks.STABILIZER;
    public static final DeferredHolder<Block> visGenerator = ModBlocks.VIS_GENERATOR;
    public static final DeferredHolder<Block> condenser = ModBlocks.CONDENSER;
    public static final DeferredHolder<Block> arcaneEar = ModBlocks.ARCANE_EAR;
    public static final DeferredHolder<Block> arcaneEarToggle = ModBlocks.ARCANE_EAR_TOGGLE;
    public static final DeferredHolder<Block> redstoneRelay = ModBlocks.REDSTONE_RELAY;
    public static final DeferredHolder<Block> levitator = ModBlocks.LEVITATOR;
    public static final DeferredHolder<Block> dioptra = ModBlocks.DIOPTRA;
    public static final DeferredHolder<Block> voidSiphon = ModBlocks.VOID_SIPHON;
    public static final DeferredHolder<Block> potionSprayer = ModBlocks.POTION_SPRAYER;
    public static final DeferredHolder<Block> everfullUrn = ModBlocks.EVERFULL_URN;

    // Essentia
    public static final DeferredHolder<Block> jarNormal = ModBlocks.JAR_NORMAL;
    public static final DeferredHolder<Block> jarVoid = ModBlocks.JAR_VOID;
    public static final DeferredHolder<Block> jarBrain = ModBlocks.JAR_BRAIN;
    public static final DeferredHolder<Block> tube = ModBlocks.TUBE_NORMAL;
    public static final DeferredHolder<Block> tubeValve = ModBlocks.TUBE_VALVE;
    public static final DeferredHolder<Block> tubeRestrict = ModBlocks.TUBE_RESTRICTED;
    public static final DeferredHolder<Block> tubeOneway = ModBlocks.TUBE_ONEWAY;
    public static final DeferredHolder<Block> tubeFilter = ModBlocks.TUBE_FILTER;
    public static final DeferredHolder<Block> tubeBuffer = ModBlocks.TUBE_BUFFER;
    public static final DeferredHolder<Block> alembic = ModBlocks.ALEMBIC;
    public static final DeferredHolder<Block> smelterBasic = ModBlocks.SMELTER;
    public static final DeferredHolder<Block> centrifuge = ModBlocks.CENTRIFUGE;
    public static final DeferredHolder<Block> infernalFurnace = ModBlocks.INFERNAL_FURNACE;
    public static final DeferredHolder<Block> essentiaReservoir = ModBlocks.ESSENTIA_RESERVOIR;
    public static final DeferredHolder<Block> spa = ModBlocks.SPA;
    public static final DeferredHolder<Block> fluxScrubber = ModBlocks.FLUX_SCRUBBER;
    public static final DeferredHolder<Block> visRelay = ModBlocks.VIS_RELAY;

    // Lamps
    public static final DeferredHolder<Block> lampArcane = ModBlocks.LAMP_ARCANE;
    public static final DeferredHolder<Block> lampGrowth = ModBlocks.LAMP_GROWTH;
    public static final DeferredHolder<Block> lampFertility = ModBlocks.LAMP_FERTILITY;

    // Candles & Nitor
    public static final DeferredHolder<Block> candleWhite = ModBlocks.CANDLE_WHITE;
    public static final DeferredHolder<Block> nitorWhite = ModBlocks.NITOR_WHITE;
    // ... mapping for all colors would go here, omitting for brevity in this initial pass ...

    // Infusion Support
    public static final DeferredHolder<Block> inlay = ModBlocks.INLAY;

    // Effects & Misc
    public static final DeferredHolder<Block> effectSap = ModBlocks.EFFECT_SAP;
    public static final DeferredHolder<Block> effectShock = ModBlocks.EFFECT_SHOCK;
    public static final DeferredHolder<Block> effectGlimmer = ModBlocks.EFFECT_GLIMMER;
    public static final DeferredHolder<Block> hole = ModBlocks.HOLE;
    public static final DeferredHolder<Block> fluxGoo = ModBlocks.FLUX_GOO;
    public static final DeferredHolder<Block> taintFibre = ModBlocks.TAINT_FIBRE;

}
