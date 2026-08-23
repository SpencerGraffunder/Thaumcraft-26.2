package thaumcraft.common.blocks.basic;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.init.BlockRegistration;

/**
 * Pillar blocks that can be oriented on different axes.
 * Used for arcane pillar, ancient pillar, eldritch pillar, etc.
 */
public class BlockPillarTC extends RotatedPillarBlock {

    public BlockPillarTC(Properties properties) {
        super(BlockRegistration.id(properties));
        this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.Y));
    }

    /**
     * Creates a standard stone pillar.
     */
    public static BlockPillarTC create() {
        return new BlockPillarTC(
            Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 10.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
