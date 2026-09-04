package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import thaumcraft.init.BlockRegistration;
import thaumcraft.common.tiles.devices.TileStabilizer;
import thaumcraft.init.ModBlockEntities;

/**
 * Runic matrix stabilizer for infusion crafting.
 * Provides significant stabilization bonus to nearby infusion altar.
 * More effective than candles but requires essentia to operate.
 */
public class BlockStabilizer extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

    private static final float STABILIZATION_BONUS = 0.25f;

    public BlockStabilizer() {
        super(BlockRegistration.id(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f)
                .sound(SoundType.STONE)
                .noOcclusion()
                .lightLevel(state -> 4)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Returns the stabilization bonus for infusion crafting.
     */
    public float getStabilizationBonus() {
        return STABILIZATION_BONUS;
    }

    /**
     * Check if this stabilizer can currently stabilize infusion.
     * Requires essentia to function.
     */
    public boolean canStabilizeInfusion(Level level, BlockPos pos) {
        // TODO: Check if has essentia when TileStabilizer is implemented
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileStabilizer(ModBlockEntities.STABILIZER.get(), pos, state);
    }
}
