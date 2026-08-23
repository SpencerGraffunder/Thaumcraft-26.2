package thaumcraft.init;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.crafting.*;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;
import thaumcraft.common.tiles.crafting.TileThaumatorium;
import thaumcraft.common.tiles.crafting.TileVoidSiphon;
import thaumcraft.common.tiles.devices.*;
import thaumcraft.common.tiles.devices.TileDioptra;
import thaumcraft.common.tiles.devices.TileLevitator;
import thaumcraft.common.tiles.devices.TilePotionSprayer;
import thaumcraft.common.tiles.devices.TileRechargePedestal;
import thaumcraft.common.tiles.devices.TileFluxScrubber;
import thaumcraft.common.tiles.devices.TileInfernalFurnace;
import thaumcraft.common.tiles.devices.TileSpa;
import thaumcraft.common.tiles.devices.TileVisRelay;
import thaumcraft.common.tiles.devices.TileWaterJug;
import thaumcraft.common.tiles.essentia.*;
import thaumcraft.common.tiles.essentia.TileCentrifuge;
import thaumcraft.common.tiles.essentia.TileEssentiaReservoir;
import thaumcraft.common.tiles.essentia.TileJarBrain;
import thaumcraft.common.tiles.essentia.TileJarVoid;
import thaumcraft.common.tiles.misc.TileBanner;
import thaumcraft.common.tiles.misc.TileBarrierStone;
import thaumcraft.common.tiles.misc.TileHole;
import net.minecraft.core.registries.Registries;

/**
 * Registry for all Thaumcraft block entities (tile entities).
 * Uses DeferredRegister for 1.20.1 Forge.
 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Thaumcraft.MODID);

    // ==================== Essentia ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileJar>> JAR =
            BLOCK_ENTITIES.register("jar",
                    () -> new BlockEntityType<>(TileJar::new,
                            ModBlocks.JAR_NORMAL.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileJarVoid>> JAR_VOID =
            BLOCK_ENTITIES.register("jar_void",
                    () -> new BlockEntityType<>(TileJarVoid::new,
                            ModBlocks.JAR_VOID.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileJarBrain>> JAR_BRAIN =
            BLOCK_ENTITIES.register("jar_brain",
                    () -> new BlockEntityType<>(TileJarBrain::new,
                            ModBlocks.JAR_BRAIN.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAlembic>> ALEMBIC =
            BLOCK_ENTITIES.register("alembic",
                    () -> new BlockEntityType<>(TileAlembic::new,
                            ModBlocks.ALEMBIC.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSmelter>> SMELTER =
            BLOCK_ENTITIES.register("smelter",
                    () -> new BlockEntityType<>(TileSmelter::new,
                            ModBlocks.SMELTER.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTube>> TUBE =
            BLOCK_ENTITIES.register("tube",
                    () -> new BlockEntityType<>(TileTube::new,
                            ModBlocks.TUBE_NORMAL.get(),
                            ModBlocks.TUBE_RESTRICTED.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeFilter>> TUBE_FILTER =
            BLOCK_ENTITIES.register("tube_filter",
                    () -> new BlockEntityType<>(TileTubeFilter::new,
                            ModBlocks.TUBE_FILTER.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeValve>> TUBE_VALVE =
            BLOCK_ENTITIES.register("tube_valve",
                    () -> new BlockEntityType<>(TileTubeValve::new,
                            ModBlocks.TUBE_VALVE.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeBuffer>> TUBE_BUFFER =
            BLOCK_ENTITIES.register("tube_buffer",
                    () -> new BlockEntityType<>(TileTubeBuffer::new,
                            ModBlocks.TUBE_BUFFER.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeOneway>> TUBE_ONEWAY =
            BLOCK_ENTITIES.register("tube_oneway",
                    () -> new BlockEntityType<>(TileTubeOneway::new,
                            ModBlocks.TUBE_ONEWAY.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeRestrict>> TUBE_RESTRICT =
            BLOCK_ENTITIES.register("tube_restrict",
                    () -> new BlockEntityType<>(TileTubeRestrict::new,
                            ModBlocks.TUBE_RESTRICTED.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCentrifuge>> CENTRIFUGE =
            BLOCK_ENTITIES.register("centrifuge",
                    () -> new BlockEntityType<>(TileCentrifuge::new,
                            ModBlocks.CENTRIFUGE.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEssentiaReservoir>> ESSENTIA_RESERVOIR =
            BLOCK_ENTITIES.register("essentia_reservoir",
                    () -> new BlockEntityType<>(TileEssentiaReservoir::new,
                            ModBlocks.ESSENTIA_RESERVOIR.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEssentiaInput>> ESSENTIA_INPUT =
            BLOCK_ENTITIES.register("essentia_input",
                    () -> new BlockEntityType<>(TileEssentiaInput::new,
                            ModBlocks.ESSENTIA_INPUT.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEssentiaOutput>> ESSENTIA_OUTPUT =
            BLOCK_ENTITIES.register("essentia_output",
                    () -> new BlockEntityType<>(TileEssentiaOutput::new,
                            ModBlocks.ESSENTIA_OUTPUT.get()
                    ));

    // ==================== Crafting ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TilePedestal>> PEDESTAL =
            BLOCK_ENTITIES.register("pedestal",
                    () -> new BlockEntityType<>(TilePedestal::new,
                            ModBlocks.PEDESTAL_ARCANE.get(),
                            ModBlocks.PEDESTAL_ANCIENT.get(),
                            ModBlocks.PEDESTAL_ELDRITCH.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCrucible>> CRUCIBLE =
            BLOCK_ENTITIES.register("crucible",
                    () -> new BlockEntityType<>(TileCrucible::new,
                            ModBlocks.CRUCIBLE.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneWorkbench>> ARCANE_WORKBENCH =
            BLOCK_ENTITIES.register("arcane_workbench",
                    () -> new BlockEntityType<>(TileArcaneWorkbench::new,
                            ModBlocks.ARCANE_WORKBENCH.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileResearchTable>> RESEARCH_TABLE =
            BLOCK_ENTITIES.register("research_table",
                    () -> new BlockEntityType<>(TileResearchTable::new,
                            ModBlocks.RESEARCH_TABLE.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInfusionMatrix>> INFUSION_MATRIX =
            BLOCK_ENTITIES.register("infusion_matrix",
                    () -> new BlockEntityType<>(TileInfusionMatrix::new,
                            ModBlocks.INFUSION_MATRIX.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileFocalManipulator>> FOCAL_MANIPULATOR =
            BLOCK_ENTITIES.register("focal_manipulator",
                    () -> new BlockEntityType<>(TileFocalManipulator::new,
                            ModBlocks.FOCAL_MANIPULATOR.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileThaumatorium>> THAUMATORIUM =
            BLOCK_ENTITIES.register("thaumatorium",
                    () -> new BlockEntityType<>(TileThaumatorium::new,
                            ModBlocks.THAUMATORIUM.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileThaumatoriumTop>> THAUMATORIUM_TOP =
            BLOCK_ENTITIES.register("thaumatorium_top",
                    () -> new BlockEntityType<>(TileThaumatoriumTop::new,
                            ModBlocks.THAUMATORIUM_TOP.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TilePatternCrafter>> PATTERN_CRAFTER =
            BLOCK_ENTITIES.register("pattern_crafter",
                    () -> new BlockEntityType<>(TilePatternCrafter::new,
                            ModBlocks.PATTERN_CRAFTER.get()
                    ));

    // ==================== Devices ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileBellows>> BELLOWS =
            BLOCK_ENTITIES.register("bellows",
                    () -> new BlockEntityType<>(TileBellows::new,
                            ModBlocks.BELLOWS.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileHungryChest>> HUNGRY_CHEST =
            BLOCK_ENTITIES.register("hungry_chest",
                    () -> new BlockEntityType<>(TileHungryChest::new,
                            ModBlocks.HUNGRY_CHEST.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLampArcane>> LAMP_ARCANE =
            BLOCK_ENTITIES.register("lamp_arcane",
                    () -> new BlockEntityType<>(TileLampArcane::new,
                            ModBlocks.LAMP_ARCANE.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLampGrowth>> LAMP_GROWTH =
            BLOCK_ENTITIES.register("lamp_growth",
                    () -> new BlockEntityType<>(TileLampGrowth::new,
                            ModBlocks.LAMP_GROWTH.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLampFertility>> LAMP_FERTILITY =
            BLOCK_ENTITIES.register("lamp_fertility",
                    () -> new BlockEntityType<>(TileLampFertility::new,
                            ModBlocks.LAMP_FERTILITY.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileMirror>> MIRROR_ITEM =
            BLOCK_ENTITIES.register("mirror_item",
                    () -> new BlockEntityType<>(TileMirror::new,
                            ModBlocks.MIRROR_ITEM.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileStabilizer>> STABILIZER =
            BLOCK_ENTITIES.register("stabilizer",
                    () -> new BlockEntityType<>(TileStabilizer::new,
                            ModBlocks.STABILIZER.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileMirrorEssentia>> MIRROR_ESSENTIA =
            BLOCK_ENTITIES.register("mirror_essentia",
                    () -> new BlockEntityType<>(TileMirrorEssentia::new,
                            ModBlocks.MIRROR_ESSENTIA.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileVisGenerator>> VIS_GENERATOR =
            BLOCK_ENTITIES.register("vis_generator",
                    () -> new BlockEntityType<>(TileVisGenerator::new,
                            ModBlocks.VIS_GENERATOR.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCondenser>> CONDENSER =
            BLOCK_ENTITIES.register("condenser",
                    () -> new BlockEntityType<>(TileCondenser::new,
                            ModBlocks.CONDENSER.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneEar>> ARCANE_EAR =
            BLOCK_ENTITIES.register("arcane_ear",
                    () -> new BlockEntityType<>(TileArcaneEar::new,
                            ModBlocks.ARCANE_EAR.get(),
                            ModBlocks.ARCANE_EAR_TOGGLE.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileRedstoneRelay>> REDSTONE_RELAY =
            BLOCK_ENTITIES.register("redstone_relay",
                    () -> new BlockEntityType<>(TileRedstoneRelay::new,
                            ModBlocks.REDSTONE_RELAY.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInfernalFurnace>> INFERNAL_FURNACE =
            BLOCK_ENTITIES.register("infernal_furnace",
                    () -> new BlockEntityType<>(TileInfernalFurnace::new,
                            ModBlocks.INFERNAL_FURNACE.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSpa>> SPA =
            BLOCK_ENTITIES.register("spa",
                    () -> new BlockEntityType<>(TileSpa::new,
                            ModBlocks.SPA.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileFluxScrubber>> FLUX_SCRUBBER =
            BLOCK_ENTITIES.register("flux_scrubber",
                    () -> new BlockEntityType<>(TileFluxScrubber::new,
                            ModBlocks.FLUX_SCRUBBER.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileVisRelay>> VIS_RELAY =
            BLOCK_ENTITIES.register("vis_relay",
                    () -> new BlockEntityType<>(TileVisRelay::new,
                            ModBlocks.VIS_RELAY.get()
                    ));

    // ==================== Golem Crafting ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileGolemBuilder>> GOLEM_BUILDER =
            BLOCK_ENTITIES.register("golem_builder",
                    () -> new BlockEntityType<>(TileGolemBuilder::new,
                            ModBlocks.GOLEM_BUILDER.get()
                    ));

    // ==================== Misc ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileHole>> HOLE =
            BLOCK_ENTITIES.register("hole",
                    () -> new BlockEntityType<>(TileHole::new,
                            ModBlocks.HOLE.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileBarrierStone>> BARRIER_STONE =
            BLOCK_ENTITIES.register("barrier_stone",
                    () -> new BlockEntityType<>(TileBarrierStone::new,
                            ModBlocks.PAVING_STONE_BARRIER.get()
                    ));

    // ==================== New Devices ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLevitator>> LEVITATOR =
            BLOCK_ENTITIES.register("levitator",
                    () -> new BlockEntityType<>(TileLevitator::new,
                            ModBlocks.LEVITATOR.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileRechargePedestal>> RECHARGE_PEDESTAL =
            BLOCK_ENTITIES.register("recharge_pedestal",
                    () -> new BlockEntityType<>(TileRechargePedestal::new,
                            ModBlocks.RECHARGE_PEDESTAL.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDioptra>> DIOPTRA =
            BLOCK_ENTITIES.register("dioptra",
                    () -> new BlockEntityType<>(TileDioptra::new,
                            ModBlocks.DIOPTRA.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileVoidSiphon>> VOID_SIPHON =
            BLOCK_ENTITIES.register("void_siphon",
                    () -> new BlockEntityType<>(TileVoidSiphon::new,
                            ModBlocks.VOID_SIPHON.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TilePotionSprayer>> POTION_SPRAYER =
            BLOCK_ENTITIES.register("potion_sprayer",
                    () -> new BlockEntityType<>(TilePotionSprayer::new,
                            ModBlocks.POTION_SPRAYER.get()
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWaterJug>> WATER_JUG =
            BLOCK_ENTITIES.register("water_jug",
                    () -> new BlockEntityType<>(TileWaterJug::new,
                            ModBlocks.EVERFULL_URN.get()
                    ));

    // ==================== Banners ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileBanner>> BANNER =
            BLOCK_ENTITIES.register("banner",
                    () -> new BlockEntityType<>(TileBanner::new,
                            ModBlocks.BANNER_WHITE.get(),
                            ModBlocks.BANNER_ORANGE.get(),
                            ModBlocks.BANNER_MAGENTA.get(),
                            ModBlocks.BANNER_LIGHT_BLUE.get(),
                            ModBlocks.BANNER_YELLOW.get(),
                            ModBlocks.BANNER_LIME.get(),
                            ModBlocks.BANNER_PINK.get(),
                            ModBlocks.BANNER_GRAY.get(),
                            ModBlocks.BANNER_LIGHT_GRAY.get(),
                            ModBlocks.BANNER_CYAN.get(),
                            ModBlocks.BANNER_PURPLE.get(),
                            ModBlocks.BANNER_BLUE.get(),
                            ModBlocks.BANNER_BROWN.get(),
                            ModBlocks.BANNER_GREEN.get(),
                            ModBlocks.BANNER_RED.get(),
                            ModBlocks.BANNER_BLACK.get(),
                            ModBlocks.BANNER_CRIMSON_CULT.get()
                    ));
}
