package thaumcraft;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModCreativeTabs;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModSounds;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModMenuTypes;
import thaumcraft.init.ModRecipeTypes;
import thaumcraft.init.ModRecipeSerializers;
import thaumcraft.init.ModFeatures;
import thaumcraft.init.ModStructures;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.items.casters.FocusInit;
import thaumcraft.common.golems.GolemProperties;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.lib.research.theorycraft.TheoryRegistry;
import thaumcraft.common.lib.InternalMethodHandler;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.config.ConfigAspects;
import thaumcraft.common.config.ConfigMultiblocks;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.client.lib.events.KeyHandler;

/**
 * Thaumcraft - A mod about discovering the arcane and harnessing the power of magic.
 * Originally created by Azanor.
 * Ported to 1.20.1 from 1.12.2 decompiled source.
 */
@Mod(Thaumcraft.MODID)
public class Thaumcraft {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "thaumcraft";
    public static final String MODNAME = "Thaumcraft";
    public static final String VERSION = "6.2.0";
    
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // Singleton instance
    private static Thaumcraft instance;
    
    public Thaumcraft(IEventBus modEventBus) {
        instance = this;

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register network payload handlers on the mod bus
        modEventBus.addListener(this::registerPayloadHandlers);

        // Register Deferred Registers to the mod event bus
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ITEMS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModStructures.STRUCTURE_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        // ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ThaumcraftConfig.SPEC);
    }
    
    public static Thaumcraft getInstance() {
        return instance;
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("Thaumcraft common setup");
        
        // Initialize the internal API handler - this enables all ThaumcraftApi methods
        ThaumcraftApi.internalMethods = new InternalMethodHandler();
        LOGGER.info("Initialized ThaumcraftApi internal methods");

        // Network payload handlers are registered via RegisterPayloadHandlersEvent
        // (see registerPayloadHandlers below).
        
                event.enqueueWork(() -> {
            // Initialize focus system
            FocusInit.registerFoci();
            LOGGER.info("Registered {} focus elements", FocusInit.getAllFocusKeys().length);
            LOGGER.info("Thaumcraft setup complete!");
        });
    }

    // Register every CustomPacketPayload (both directions) on the payload registry.
    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PacketHandler.register(event);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Golem parts reference vanilla ItemStacks, which are only safe to
        // construct after registry holders bind their components (commonSetup is
        // too early: 'Components not bound yet'). Register them on server start.
        GolemProperties.registerDefaultParts();
        LOGGER.info("Registered golem parts");
        SealHandler.registerDefaultSeals();
        LOGGER.info("Registered golem seals");

        // Research, aspect, and multiblock scan registries also construct vanilla
        // ItemStacks (ScanBlock/ScanObject), so they must run here as well.
        ConfigResearch.init();
        ConfigAspects.init();
        ConfigMultiblocks.init();
                ConfigResearch.postInit();

        // Register commands
        // CommandThaumcraft.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("Thaumcraft server starting");
    }

    // Entity attribute registration
    @EventBusSubscriber(modid = MODID)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
            // Monster entities
            event.put(ModEntities.WISP.get(), 
                    thaumcraft.common.entities.monster.EntityWisp.createAttributes().build());
            event.put(ModEntities.FIRE_BAT.get(), 
                    thaumcraft.common.entities.monster.EntityFireBat.createAttributes().build());
            event.put(ModEntities.BRAINY_ZOMBIE.get(), 
                    thaumcraft.common.entities.monster.EntityBrainyZombie.createAttributes().build());
            event.put(ModEntities.MIND_SPIDER.get(), 
                    thaumcraft.common.entities.monster.EntityMindSpider.createAttributes().build());
            event.put(ModEntities.THAUMIC_SLIME.get(), 
                    thaumcraft.common.entities.monster.EntityThaumicSlime.createAttributes().build());
            event.put(ModEntities.GIANT_BRAINY_ZOMBIE.get(), 
                    thaumcraft.common.entities.monster.EntityGiantBrainyZombie.createAttributes().build());
            event.put(ModEntities.INHABITED_ZOMBIE.get(), 
                    thaumcraft.common.entities.monster.EntityInhabitedZombie.createAttributes().build());
            
            event.put(ModEntities.ELDRITCH_CRAB.get(), 
                    thaumcraft.common.entities.monster.EntityEldritchCrab.createAttributes().build());
            event.put(ModEntities.SPELL_BAT.get(), 
                    thaumcraft.common.entities.monster.EntitySpellBat.createAttributes().build());
            
            // Tainted entities
            event.put(ModEntities.TAINT_CRAWLER.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintCrawler.createAttributes().build());
            event.put(ModEntities.TAINT_SWARM.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintSwarm.createAttributes().build());
            event.put(ModEntities.TAINTACLE.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintacle.createAttributes().build());
            event.put(ModEntities.TAINTACLE_SMALL.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintacleSmall.createAttributes().build());
            event.put(ModEntities.TAINT_SEED.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintSeed.createAttributes().build());
            event.put(ModEntities.TAINT_SEED_PRIME.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintSeedPrime.createAttributes().build());
            
            // Eldritch entities
            event.put(ModEntities.ELDRITCH_GUARDIAN.get(), 
                    thaumcraft.common.entities.monster.EntityEldritchGuardian.createAttributes().build());
            
            // Cult entities
            event.put(ModEntities.CULTIST.get(), 
                    thaumcraft.common.entities.monster.cult.EntityCultist.createAttributes().build());
            event.put(ModEntities.CULTIST_KNIGHT.get(), 
                    thaumcraft.common.entities.monster.cult.EntityCultistKnight.createAttributes().build());
            event.put(ModEntities.CULTIST_CLERIC.get(), 
                    thaumcraft.common.entities.monster.cult.EntityCultistCleric.createAttributes().build());
            event.put(ModEntities.CULTIST_PORTAL_LESSER.get(), 
                    thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser.createAttributes().build());
            
            // Boss entities
            event.put(ModEntities.CULTIST_LEADER.get(), 
                    thaumcraft.common.entities.monster.boss.EntityCultistLeader.createAttributes().build());
            event.put(ModEntities.TAINTACLE_GIANT.get(), 
                    thaumcraft.common.entities.monster.boss.EntityTaintacleGiant.createAttributes().build());
            event.put(ModEntities.CULTIST_PORTAL_GREATER.get(), 
                    thaumcraft.common.entities.monster.boss.EntityCultistPortalGreater.createAttributes().build());
            event.put(ModEntities.ELDRITCH_GOLEM.get(), 
                    thaumcraft.common.entities.monster.boss.EntityEldritchGolem.createAttributes().build());
            event.put(ModEntities.ELDRITCH_WARDEN.get(), 
                    thaumcraft.common.entities.monster.boss.EntityEldritchWarden.createAttributes().build());
            
            // Pech
            event.put(ModEntities.PECH.get(), 
                    thaumcraft.common.entities.monster.EntityPech.createAttributes().build());
            
            // Construct entities
            event.put(ModEntities.TURRET_CROSSBOW.get(), 
                    thaumcraft.common.entities.construct.EntityTurretCrossbow.createAttributes().build());
            event.put(ModEntities.TURRET_CROSSBOW_ADVANCED.get(), 
                    thaumcraft.common.entities.construct.EntityTurretCrossbowAdvanced.createAttributes().build());
            
            // Golem
            event.put(ModEntities.THAUMCRAFT_GOLEM.get(), 
                    thaumcraft.common.golems.EntityThaumcraftGolem.createAttributes().build());
            
            LOGGER.info("Registered Thaumcraft entity attributes");
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("Thaumcraft client setup");
            LOGGER.info("MINECRAFT NAME >> {}", net.minecraft.client.Minecraft.getInstance().getUser().getName());
            
            // Register entity renderers and menu screens
            event.enqueueWork(() -> {
                // Entity renderers
                
                
                
                
                
                
                
                
                
                
                
                
                // Cultists
                
                
                
                // Tainted entities
                
                
                
                
                
                
                
                // Eldritch entities
                
                
                
                // SpellBat
                
                // InhabitedZombie
                
                // Boss entities
                
                // Cultist portals
                
                
                // Projectiles - using generic renderer
                
                
                
                
                // Focus/projectile renderers
                
                
                
                
                // Falling taint
                
                // Special/Following items - glowing magical item entities
                
                
                // Invisible projectiles (effects come from particles)
                
                
                
                // Bottle Taint - uses item rendering (thrown item texture)
                
                // Eldritch Warden - boss version of Eldritch Guardian
                
                LOGGER.info("Registered Thaumcraft entity renderers");
                
                // Menu screens
                
                
                
                
                
                
                // New menu screens
                
                
                
                
                
                
                
                
                
                
                
                
                
                LOGGER.info("Registered Thaumcraft menu screens");
            });
            
            // Register block entity renderers
            // ProxyTESR.setupTESR();
            
            // Register key bindings
            // KeyHandler.registerKeyBindings();
            
            // Register color handlers
            // ColorHandler.registerColourHandlers();
        }
        
        @SubscribeEvent
        public static void onRegisterLayerDefinitions(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            // Register entity model layers
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.GolemModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.GolemModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.CrossbowModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.CrossbowModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.CrossbowAdvancedModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.CrossbowAdvancedModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.ArcaneBoreModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.ArcaneBoreModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.PechModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.PechModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.TaintacleModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.TaintacleModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.TaintSeedModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.TaintSeedModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.EldritchGolemModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.EldritchGolemModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.GrapplerModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.GrapplerModel::createBodyLayer
            );
            
            // Register block entity model layers
            event.registerLayerDefinition(
                thaumcraft.client.models.block.CentrifugeModel.LAYER_LOCATION,
                thaumcraft.client.models.block.CentrifugeModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.block.BellowsModel.LAYER_LOCATION,
                thaumcraft.client.models.block.BellowsModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.block.TubeValveModel.LAYER_LOCATION,
                thaumcraft.client.models.block.TubeValveModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.block.BrainModel.LAYER_LOCATION,
                thaumcraft.client.models.block.BrainModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.block.BannerModel.LAYER_LOCATION,
                thaumcraft.client.models.block.BannerModel::createBodyLayer
            );
            LOGGER.info("Registered Thaumcraft model layers");
        }
        
        @SubscribeEvent
        public static void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {

            event.registerEntityRenderer(
                ModEntities.THAUMCRAFT_GOLEM.get(),
                thaumcraft.client.renderers.entity.GolemRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.TURRET_CROSSBOW.get(),
                thaumcraft.client.renderers.entity.TurretCrossbowRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.TURRET_CROSSBOW_ADVANCED.get(),
                thaumcraft.client.renderers.entity.TurretCrossbowAdvancedRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.ARCANE_BORE.get(),
                thaumcraft.client.renderers.entity.ArcaneBoreRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.PECH.get(),
                thaumcraft.client.renderers.entity.PechRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.WISP.get(),
                thaumcraft.client.renderers.entity.WispRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.FLUX_RIFT.get(),
                thaumcraft.client.renderers.entity.FluxRiftRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.BRAINY_ZOMBIE.get(),
                thaumcraft.client.renderers.entity.BrainyZombieRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.GIANT_BRAINY_ZOMBIE.get(),
                thaumcraft.client.renderers.entity.BrainyZombieRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.THAUMIC_SLIME.get(),
                thaumcraft.client.renderers.entity.ThaumicSlimeRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.MIND_SPIDER.get(),
                thaumcraft.client.renderers.entity.MindSpiderRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.FIRE_BAT.get(),
                thaumcraft.client.renderers.entity.FireBatRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.CULTIST.get(),
                thaumcraft.client.renderers.entity.CultistRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.CULTIST_KNIGHT.get(),
                thaumcraft.client.renderers.entity.CultistRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.CULTIST_CLERIC.get(),
                thaumcraft.client.renderers.entity.CultistRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.TAINT_CRAWLER.get(),
                thaumcraft.client.renderers.entity.TaintCrawlerRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.TAINT_SWARM.get(),
                thaumcraft.client.renderers.entity.TaintSwarmRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.TAINTACLE.get(),
                thaumcraft.client.renderers.entity.TaintacleRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.TAINTACLE_SMALL.get(),
                thaumcraft.client.renderers.entity.TaintacleRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.TAINTACLE_GIANT.get(),
                thaumcraft.client.renderers.entity.TaintacleRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.TAINT_SEED.get(),
                thaumcraft.client.renderers.entity.TaintSeedRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.TAINT_SEED_PRIME.get(),
                thaumcraft.client.renderers.entity.TaintSeedRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.ELDRITCH_CRAB.get(),
                thaumcraft.client.renderers.entity.EldritchCrabRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.ELDRITCH_GUARDIAN.get(),
                thaumcraft.client.renderers.entity.EldritchGuardianRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.ELDRITCH_GOLEM.get(),
                thaumcraft.client.renderers.entity.EldritchGolemRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.SPELL_BAT.get(),
                thaumcraft.client.renderers.entity.SpellBatRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.INHABITED_ZOMBIE.get(),
                thaumcraft.client.renderers.entity.InhabitedZombieRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.CULTIST_LEADER.get(),
                thaumcraft.client.renderers.entity.CultistLeaderRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.CULTIST_PORTAL_LESSER.get(),
                thaumcraft.client.renderers.entity.CultistPortalRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.CULTIST_PORTAL_GREATER.get(),
                thaumcraft.client.renderers.entity.CultistPortalGreaterRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.GOLEM_ORB.get(),
                ctx -> thaumcraft.client.renderers.entity.ThaumcraftProjectileRenderer.Factory.orb(ctx, 0x8844FF)
            );
            event.registerEntityRenderer(
                ModEntities.GOLEM_DART.get(),
                ctx -> thaumcraft.client.renderers.entity.ThaumcraftProjectileRenderer.Factory.dart(ctx)
            );
            event.registerEntityRenderer(
                ModEntities.ELDRITCH_ORB.get(),
                thaumcraft.client.renderers.entity.EldritchOrbRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.HOMING_SHARD.get(),
                ctx -> thaumcraft.client.renderers.entity.ThaumcraftProjectileRenderer.Factory.magic(ctx, 0x66FFFF)
            );
            event.registerEntityRenderer(
                ModEntities.FOCUS_MINE.get(),
                thaumcraft.client.renderers.entity.FocusMineRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.FOCUS_CLOUD.get(),
                thaumcraft.client.renderers.entity.FocusCloudRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.GRAPPLE.get(),
                thaumcraft.client.renderers.entity.GrappleRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.RIFT_BLAST.get(),
                thaumcraft.client.renderers.entity.RiftBlastRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.FALLING_TAINT.get(),
                thaumcraft.client.renderers.entity.FallingTaintRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.SPECIAL_ITEM.get(),
                thaumcraft.client.renderers.entity.SpecialItemRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.FOLLOWING_ITEM.get(),
                thaumcraft.client.renderers.entity.SpecialItemRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.FOCUS_PROJECTILE.get(),
                thaumcraft.client.renderers.entity.NoProjectileRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.ALUMENTUM.get(),
                thaumcraft.client.renderers.entity.NoProjectileRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.CAUSALITY_COLLAPSER.get(),
                thaumcraft.client.renderers.entity.NoProjectileRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.BOTTLE_TAINT.get(),
                thaumcraft.client.renderers.entity.BottleTaintRenderer::new
            );
            event.registerEntityRenderer(
                ModEntities.ELDRITCH_WARDEN.get(),
                thaumcraft.client.renderers.entity.EldritchWardenRenderer::new
            );

            LOGGER.info("Registered Thaumcraft block entity renderers");
        }

        @SubscribeEvent
        public static void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            event.register(
                ModMenuTypes.GOLEM_BUILDER.get(),
                thaumcraft.client.gui.screens.GolemBuilderScreen::new
            );
            event.register(
                ModMenuTypes.ARCANE_WORKBENCH.get(),
                thaumcraft.client.gui.screens.ArcaneWorkbenchScreen::new
            );
            event.register(
                ModMenuTypes.THAUMATORIUM.get(),
                thaumcraft.client.gui.screens.ThaumatoriumScreen::new
            );
            event.register(
                ModMenuTypes.SMELTER.get(),
                thaumcraft.client.gui.screens.SmelterScreen::new
            );
            event.register(
                ModMenuTypes.RESEARCH_TABLE.get(),
                thaumcraft.client.gui.screens.ResearchTableScreen::new
            );
            event.register(
                ModMenuTypes.FOCAL_MANIPULATOR.get(),
                thaumcraft.client.gui.screens.FocalManipulatorScreen::new
            );
            event.register(
                ModMenuTypes.FOCUS_POUCH.get(),
                thaumcraft.client.gui.screens.FocusPouchScreen::new
            );
            event.register(
                ModMenuTypes.HAND_MIRROR.get(),
                thaumcraft.client.gui.screens.HandMirrorScreen::new
            );
            event.register(
                ModMenuTypes.POTION_SPRAYER.get(),
                thaumcraft.client.gui.screens.PotionSprayerScreen::new
            );
            event.register(
                ModMenuTypes.SPA.get(),
                thaumcraft.client.gui.screens.SpaScreen::new
            );
            event.register(
                ModMenuTypes.VOID_SIPHON.get(),
                thaumcraft.client.gui.screens.VoidSiphonScreen::new
            );
            event.register(
                ModMenuTypes.TURRET_BASIC.get(),
                thaumcraft.client.gui.screens.TurretScreen::new
            );
            event.register(
                ModMenuTypes.TURRET_ADVANCED.get(),
                thaumcraft.client.gui.screens.TurretScreen::new
            );
            event.register(
                ModMenuTypes.ARCANE_BORE.get(),
                thaumcraft.client.gui.screens.ArcaneBoreScreen::new
            );
            event.register(
                ModMenuTypes.PECH_TRADING.get(),
                thaumcraft.client.gui.screens.PechScreen::new
            );
            event.register(
                ModMenuTypes.HUNGRY_CHEST.get(),
                thaumcraft.client.gui.screens.HungryChestScreen::new
            );
            event.register(
                ModMenuTypes.LOGISTICS.get(),
                thaumcraft.client.gui.screens.LogisticsScreen::new
            );
            event.register(
                ModMenuTypes.SEAL.get(),
                thaumcraft.client.gui.screens.SealScreen::new
            );
            LOGGER.info("Registered Thaumcraft menu screens");
        }
        
        @SubscribeEvent
        public static void onRegisterKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
            // Register Thaumcraft key bindings
            KeyHandler.registerKeyMappings(event);
        }
        
        @SubscribeEvent
        public static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.ItemTintSources event) {
            // TODO(26.2): vis crystal tinting now uses the codec-based ItemTintSource system;
            // register a custom ItemTintSource map codec here when the item models are updated.
            LOGGER.info("Item color handlers: skipped (codec-based ItemTintSource system)");
        }
    }
    
    /**
     * Get the client world (client side only) - moved to client code to keep
     * this class dedicated-server safe (no client class references).
     */
}
