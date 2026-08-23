package thaumcraft.common.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ScanBlock;
import thaumcraft.api.research.ScanEntity;
import thaumcraft.api.research.ScanItem;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;
import thaumcraft.common.entities.monster.*;
import thaumcraft.common.entities.monster.boss.EntityThaumcraftBoss;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.monster.tainted.*;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ScanGeneric;
import thaumcraft.common.lib.research.ScanSky;
import thaumcraft.common.lib.research.theorycraft.AidBookshelf;
import thaumcraft.common.lib.research.theorycraft.CardAnalyze;
import thaumcraft.common.lib.research.theorycraft.CardBalance;
import thaumcraft.common.lib.research.theorycraft.CardExperimentation;
import thaumcraft.common.lib.research.theorycraft.CardInspired;
import thaumcraft.common.lib.research.theorycraft.CardNotation;
import thaumcraft.common.lib.research.theorycraft.CardPonder;
import thaumcraft.common.lib.research.theorycraft.CardReject;
import thaumcraft.common.lib.research.theorycraft.CardRethink;
import thaumcraft.common.lib.research.theorycraft.CardStudy;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

/**
 * ConfigResearch - Initializes research categories, scannables, and theorycraft.
 * Ported from 1.12.2 to 1.20.1
 */
public class ConfigResearch {
    
    public static final String[] TC_CATEGORIES = {
        "BASICS", "ALCHEMY", "AUROMANCY", "ARTIFICE", "INFUSION", "GOLEMANCY", "ELDRITCH"
    };
    
    private static final Identifier BACK_OVER = Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_research_back_over.png");
    
    /**
     * Initialize all research-related systems.
     * Called during mod common setup.
     */
    public static void init() {
        Thaumcraft.LOGGER.info("Initializing Thaumcraft research system...");
        
        initCategories();
        initScannables();
        initTheorycraft();
        initWarp();
        
        // Register research JSON locations
        for (String cat : TC_CATEGORIES) {
            ThaumcraftApi.registerResearchLocation(Identifier.fromNamespaceAndPath("thaumcraft", "research/" + cat.toLowerCase()));
        }
        ThaumcraftApi.registerResearchLocation(Identifier.fromNamespaceAndPath("thaumcraft", "research/scans"));
        
        Thaumcraft.LOGGER.info("Research categories and JSON locations registered");
    }
    
    /**
     * Post-initialization - parse all research JSON files.
     * Called after all mods have registered their research locations.
     */
    public static void postInit() {
        Thaumcraft.LOGGER.info("Parsing research JSON files...");
        ResearchManager.parseAllResearch();
        Thaumcraft.LOGGER.info("Research system initialized with {} categories", 
                ResearchCategories.researchCategories.size());
    }
    
    /**
     * Initialize all research categories.
     */
    private static void initCategories() {
        // BASICS - always visible, no prerequisite research
        ResearchCategories.registerCategory("BASICS", null,
                new AspectList()
                        .add(Aspect.PLANT, 5).add(Aspect.ORDER, 5).add(Aspect.ENTROPY, 5)
                        .add(Aspect.AIR, 5).add(Aspect.FIRE, 5).add(Aspect.EARTH, 3).add(Aspect.WATER, 5),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/items/thaumonomicon_cheat.png"),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_research_back_1.jpg"),
                BACK_OVER);
        
        // AUROMANCY - requires UNLOCKAUROMANCY
        ResearchCategories.registerCategory("AUROMANCY", "UNLOCKAUROMANCY",
                new AspectList()
                        .add(Aspect.AURA, 20).add(Aspect.MAGIC, 20).add(Aspect.FLUX, 15)
                        .add(Aspect.CRYSTAL, 5).add(Aspect.COLD, 5).add(Aspect.AIR, 5),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/research/cat_auromancy.png"),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_research_back_2.jpg"),
                BACK_OVER);
        
        // ALCHEMY - requires UNLOCKALCHEMY
        ResearchCategories.registerCategory("ALCHEMY", "UNLOCKALCHEMY",
                new AspectList()
                        .add(Aspect.ALCHEMY, 30).add(Aspect.FLUX, 10).add(Aspect.MAGIC, 10)
                        .add(Aspect.LIFE, 5).add(Aspect.AVERSION, 5).add(Aspect.DESIRE, 5).add(Aspect.WATER, 5),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/research/cat_alchemy.png"),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_research_back_3.jpg"),
                BACK_OVER);
        
        // ARTIFICE - requires UNLOCKARTIFICE
        ResearchCategories.registerCategory("ARTIFICE", "UNLOCKARTIFICE",
                new AspectList()
                        .add(Aspect.MECHANISM, 10).add(Aspect.CRAFT, 10).add(Aspect.METAL, 10)
                        .add(Aspect.TOOL, 10).add(Aspect.ENERGY, 10).add(Aspect.LIGHT, 5)
                        .add(Aspect.FLIGHT, 5).add(Aspect.TRAP, 5).add(Aspect.FIRE, 5),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/research/cat_artifice.png"),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_research_back_4.jpg"),
                BACK_OVER);
        
        // INFUSION - requires UNLOCKINFUSION
        ResearchCategories.registerCategory("INFUSION", "UNLOCKINFUSION",
                new AspectList()
                        .add(Aspect.MAGIC, 30).add(Aspect.PROTECT, 10).add(Aspect.TOOL, 10)
                        .add(Aspect.FLUX, 5).add(Aspect.CRAFT, 5).add(Aspect.SOUL, 5).add(Aspect.EARTH, 3),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/research/cat_infusion.png"),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_research_back_7.jpg"),
                BACK_OVER);
        
        // GOLEMANCY - requires UNLOCKGOLEMANCY
        ResearchCategories.registerCategory("GOLEMANCY", "UNLOCKGOLEMANCY",
                new AspectList()
                        .add(Aspect.MAN, 20).add(Aspect.MOTION, 10).add(Aspect.MIND, 10)
                        .add(Aspect.MECHANISM, 10).add(Aspect.EXCHANGE, 5).add(Aspect.SENSES, 5)
                        .add(Aspect.BEAST, 5).add(Aspect.ORDER, 5),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/research/cat_golemancy.png"),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_research_back_5.jpg"),
                BACK_OVER);
        
        // ELDRITCH - requires UNLOCKELDRITCH
        ResearchCategories.registerCategory("ELDRITCH", "UNLOCKELDRITCH",
                new AspectList()
                        .add(Aspect.ELDRITCH, 20).add(Aspect.DARKNESS, 10).add(Aspect.MAGIC, 5)
                        .add(Aspect.MIND, 5).add(Aspect.VOID, 5).add(Aspect.DEATH, 5)
                        .add(Aspect.UNDEAD, 5).add(Aspect.ENTROPY, 5),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/research/cat_eldritch.png"),
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_research_back_6.jpg"),
                BACK_OVER);
        
        Thaumcraft.LOGGER.info("Registered {} research categories", TC_CATEGORIES.length);
    }
    
    /**
     * Initialize all scannable objects for the Thaumometer.
     */
    private static void initScannables() {
        // Generic scanner for basic items/blocks
        ScanningManager.addScannableThing(new ScanGeneric());
        
        // TODO: Port ScanEnchantment and ScanPotion when enchantment/effect registries are stable
        // For now, skip dynamic enchantment/potion scanning
        
        // Thaumcraft entities
        ScanningManager.addScannableThing(new ScanEntity("!Wisp", EntityWisp.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!ThaumSlime", EntityThaumicSlime.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!Firebat", EntityFireBat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!Pech", EntityPech.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!BrainyZombie", EntityBrainyZombie.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!EldritchCrab", EntityEldritchCrab.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!EldritchCrab", EntityInhabitedZombie.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!CrimsonCultist", EntityCultist.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!EldritchGuardian", EntityEldritchGuardian.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!TaintCrawler", EntityTaintCrawler.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!Taintacle", EntityTaintacle.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!TaintSeed", EntityTaintSeed.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!TaintSwarm", EntityTaintSwarm.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_toomuchflux", EntityFluxRift.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!FluxRift", EntityFluxRift.class, true));
        
        // Vanilla entity types for research triggers
        ScanningManager.addScannableThing(new ScanEntity("f_golem", EntityOwnedConstruct.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_SPIDER", Spider.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_BAT", Bat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_BAT", EntityFireBat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Bat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Parrot.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", EntityFireBat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", EntityTaintSwarm.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", EntityWisp.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Ghast.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Blaze.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!ORMOB", IEldritchMob.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!ORBOSS", EntityThaumcraftBoss.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_TELEPORT", EnderMan.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_BRAIN", EntityBrainyZombie.class, true));
        
        // Thaumcraft blocks - use block registry objects
        ScanningManager.addScannableThing(new ScanBlock("!ORBLOCK1", 
                ModBlocks.ANCIENT_STONE.get(), ModBlocks.ANCIENT_STONE_TILE.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORBLOCK2", 
                ModBlocks.ELDRITCH_STONE_TILE.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORBLOCK3", 
                ModBlocks.ANCIENT_STONE_GLYPHED.get()));
        ScanningManager.addScannableThing(new ScanBlock("ORE", 
                ModBlocks.AMBER_ORE.get(), ModBlocks.CINNABAR_ORE.get(),
                ModBlocks.CRYSTAL_AIR.get(), ModBlocks.CRYSTAL_FIRE.get(),
                ModBlocks.CRYSTAL_WATER.get(), ModBlocks.CRYSTAL_EARTH.get(),
                ModBlocks.CRYSTAL_ORDER.get(), ModBlocks.CRYSTAL_ENTROPY.get(),
                ModBlocks.CRYSTAL_FLUX.get()));
        ScanningManager.addScannableThing(new ScanBlock("!OREAMBER", ModBlocks.AMBER_ORE.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORECINNABAR", ModBlocks.CINNABAR_ORE.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORECRYSTAL",
                ModBlocks.CRYSTAL_AIR.get(), ModBlocks.CRYSTAL_FIRE.get(),
                ModBlocks.CRYSTAL_WATER.get(), ModBlocks.CRYSTAL_EARTH.get(),
                ModBlocks.CRYSTAL_ORDER.get(), ModBlocks.CRYSTAL_ENTROPY.get(),
                ModBlocks.CRYSTAL_FLUX.get()));
        
        // Plants
        ScanningManager.addScannableThing(new ScanBlock("PLANTS",
                ModBlocks.GREATWOOD_LOG.get(), ModBlocks.SILVERWOOD_LOG.get(),
                ModBlocks.GREATWOOD_SAPLING.get(), ModBlocks.SILVERWOOD_SAPLING.get(),
                ModBlocks.CINDERPEARL.get(), ModBlocks.SHIMMERLEAF.get(), ModBlocks.VISHROOM.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", ModBlocks.GREATWOOD_LOG.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", ModBlocks.SILVERWOOD_LOG.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", ModBlocks.GREATWOOD_SAPLING.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", ModBlocks.SILVERWOOD_SAPLING.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTCINDERPEARL", ModBlocks.CINDERPEARL.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTSHIMMERLEAF", ModBlocks.SHIMMERLEAF.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTVISHROOM", ModBlocks.VISHROOM.get()));
        
        // Special items
        ScanningManager.addScannableThing(new ScanItem("PRIMPEARL", new ItemStack(ModItems.PRIMORDIAL_PEARL.get())));
        ScanningManager.addScannableThing(new ScanItem("!DRAGONBREATH", new ItemStack(Items.DRAGON_BREATH)));
        ScanningManager.addScannableThing(new ScanItem("!TOTEMUNDYING", new ItemStack(Items.TOTEM_OF_UNDYING)));
        ScanningManager.addScannableThing(new ScanItem("f_TELEPORT", new ItemStack(Items.ENDER_PEARL)));
        ScanningManager.addScannableThing(new ScanItem("f_BRAIN", new ItemStack(ModItems.BRAIN_NORMAL.get())));
        ScanningManager.addScannableThing(new ScanItem("f_arrow", new ItemStack(Items.ARROW)));
        ScanningManager.addScannableThing(new ScanItem("f_VOIDSEED", new ItemStack(ModItems.VOID_SEED.get())));
        ScanningManager.addScannableThing(new ScanItem("f_MATCLAY", new ItemStack(Items.CLAY_BALL)));
        
        // Portals
        ScanningManager.addScannableThing(new ScanBlock("f_TELEPORT", 
                Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME));
        ScanningManager.addScannableThing(new ScanBlock("f_DISPENSER", Blocks.DISPENSER));
        ScanningManager.addScannableThing(new ScanItem("f_DISPENSER", new ItemStack(Blocks.DISPENSER)));
        
        // Sky scanning
        ScanningManager.addScannableThing(new ScanSky());
        
        Thaumcraft.LOGGER.info("Registered {} scannable objects", ScanningManager.getScannableCount());
    }
    
    /**
     * Initialize theorycraft cards and aids.
     * Note: Additional aids and cards can be added later as they are ported.
     */
    private static void initTheorycraft() {
        // Register aids - only AidBookshelf is currently ported
        TheorycraftManager.registerAid(new AidBookshelf());
        // TODO: Port remaining aids:
        // - AidBrainInAJar, AidGlyphedStone, AidPortal, AidBasicAlchemy, etc.
        
        // Basic cards (available in normal draw rotation) - only those already ported
        TheorycraftManager.registerCard(CardStudy.class);
        TheorycraftManager.registerCard(CardAnalyze.class);
        TheorycraftManager.registerCard(CardBalance.class);
        TheorycraftManager.registerCard(CardNotation.class);
        TheorycraftManager.registerCard(CardPonder.class);
        TheorycraftManager.registerCard(CardRethink.class);
        TheorycraftManager.registerCard(CardReject.class);
        TheorycraftManager.registerCard(CardExperimentation.class);
        TheorycraftManager.registerCard(CardInspired.class);
        
        // TODO: Port remaining cards:
        // - CardCurio, CardEnchantment, CardBeacon, CardCelestial, etc.
        
        Thaumcraft.LOGGER.info("Registered {} theorycraft cards and {} aids",
                TheorycraftManager.cards.size(), TheorycraftManager.aids.size());
    }
    
    /**
     * Initialize warp values for items.
     */
    private static void initWarp() {
        // Brain in a Jar gives warp
        ThaumcraftApi.addWarpToItem(new ItemStack(ModBlocks.JAR_BRAIN.get()), 1);
    }
    
    /**
     * Check periodic research triggers for a player.
     * Called from PlayerEvents tick handler.
     */
    public static void checkPeriodicResearch(Player player) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) return;
        
        // Check for dimension-based research
        Identifier dimKey = player.level().dimension().identifier();
        
        // Nether discovery
        if (!knowledge.isResearchKnown("m_hellandback") && dimKey.getPath().contains("nether")) {
            knowledge.addResearch("m_hellandback");
            knowledge.sync(serverPlayer);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("\u00A75" + 
                            net.minecraft.network.chat.Component.translatable("got.hellandback").getString()), 
                    true);
        }
        
        // End discovery
        if (!knowledge.isResearchKnown("m_endoftheworld") && dimKey.getPath().contains("end")) {
            knowledge.addResearch("m_endoftheworld");
            knowledge.sync(serverPlayer);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("\u00A75" + 
                            net.minecraft.network.chat.Component.translatable("got.endoftheworld").getString()), 
                    true);
        }
        
        // Height-based discoveries (only if auromancy is partially unlocked)
        if (knowledge.isResearchKnown("UNLOCKAUROMANCY@1") && !knowledge.isResearchKnown("UNLOCKAUROMANCY@2")) {
            // Deep underground
            if (player.getY() < 10 && !knowledge.isResearchKnown("m_deepdown")) {
                knowledge.addResearch("m_deepdown");
                knowledge.sync(serverPlayer);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("\u00A75" + 
                                net.minecraft.network.chat.Component.translatable("got.deepdown").getString()), 
                        true);
            }
            
            // High up
            int worldHeight = player.level().getMaxBuildHeight();
            if (player.getY() > worldHeight * 0.4 && !knowledge.isResearchKnown("m_uphigh")) {
                knowledge.addResearch("m_uphigh");
                knowledge.sync(serverPlayer);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("\u00A75" + 
                                net.minecraft.network.chat.Component.translatable("got.uphigh").getString()), 
                        true);
            }
        }
        
        // TODO: Add stat-based discoveries (walking, running, jumping, swimming) when stat tracking is implemented
    }
}
