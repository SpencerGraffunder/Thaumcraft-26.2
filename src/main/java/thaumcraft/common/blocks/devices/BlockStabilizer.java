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
import thaumcraft.api.crafting.IInfusionStabiliserExt;
import thaumcraft.init.BlockRegistration;
import thaumcraft.common.tiles.devices.TileStabilizer;
import thaumcraft.init.ModBlockEntities;

/**
 * Runic matrix stabilizer for infusion crafting.
 * Provides significant stabilization bonus to nearby infusion altar.
 * More effective than candles but requires essentia to operate.
 */
public class BlockStabilizer extends Block implements EntityBlock, IInfusionStabiliserExt {

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

    // ==================== IInfusionStabiliserExt (1.12: BlockStabilizer) ====================

    @Override
    public boolean canStabaliseInfusion(Level level, BlockPos pos) {
        return true;
    }

    @Override
    public float getStabilizationAmount(Level level, BlockPos pos) {
        return STABILIZATION_BONUS; // 0.25f, 1.12 BlockStabilizer
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileStabilizer(ModBlockEntities.STABILIZER.get(), pos, state);
    }
}
