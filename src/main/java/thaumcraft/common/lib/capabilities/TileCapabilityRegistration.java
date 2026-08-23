package thaumcraft.common.lib.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.crafting.TileCrucible;
import thaumcraft.common.tiles.devices.TileVisGenerator;
import thaumcraft.common.tiles.devices.TileWaterJug;
import thaumcraft.init.ModBlockEntities;

/**
 * Registers NeoForge capability providers for Thaumcraft block entities.
 * Replaces the old Forge getCapability overrides (NeoForge 26.2).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class TileCapabilityRegistration {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerInventoryTile(event, ModBlockEntities.FOCAL_MANIPULATOR.get());
        registerInventoryTile(event, ModBlockEntities.GOLEM_BUILDER.get());
        registerInventoryTile(event, ModBlockEntities.PEDESTAL.get());
        registerInventoryTile(event, ModBlockEntities.RESEARCH_TABLE.get());
        registerInventoryTile(event, ModBlockEntities.THAUMATORIUM.get());
        registerInventoryTile(event, ModBlockEntities.VOID_SIPHON.get());
        registerInventoryTile(event, ModBlockEntities.INFERNAL_FURNACE.get());
        registerInventoryTile(event, ModBlockEntities.POTION_SPRAYER.get());
        registerInventoryTile(event, ModBlockEntities.RECHARGE_PEDESTAL.get());
        registerInventoryTile(event, ModBlockEntities.SMELTER.get());

        // Fluid handlers
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.CRUCIBLE.get(),
                (be, side) -> ((TileCrucible) be).getTankHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.WATER_JUG.get(),
                (be, side) -> side == Direction.UP ? ((TileWaterJug) be).getDrainHandler() : null);

        // Energy handlers
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.VIS_GENERATOR.get(),
                (be, side) -> side == ((TileVisGenerator) be).getFacing() ? (TileVisGenerator) be : null);
    }

    /**
     * Register an item handler capability for a Container-based tile entity.
     */
    private static void registerInventoryTile(RegisterCapabilitiesEvent event,
            net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, type,
                (be, side) -> be instanceof WorldlyContainer worldly
                        ? new WorldlyContainerWrapper(worldly, side)
                        : VanillaContainerWrapper.of((Container) be));
    }
}
