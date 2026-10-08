package thaumcraft.common.blocks.misc;

import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The Arcane Activator Rail - the Thaumcraft rail (1.12 BlocksTC.activatorRail, a
 * vanilla-powered-rail behaviour block). Turrets and the Arcane Bore riding on a
 * minecart toggle their active state when they pass over this rail: an inactive
 * rail activates them, an active (redstone-powered) rail deactivates them.
 */
public class BlockArcaneActivatorRail extends PoweredRailBlock {

    public BlockArcaneActivatorRail(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
