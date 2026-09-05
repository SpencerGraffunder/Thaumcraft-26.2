package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import thaumcraft.init.BlockRegistration;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;
import thaumcraft.init.ModBlockEntities;

/**
 * The infusion altar's central matrix block.
 * This is the heart of the infusion crafting system.
 * Renders invisibly - actual visuals are handled by the tile entity renderer.
 */
public class BlockInfusionMatrix extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);

    public BlockInfusionMatrix() {
        super(BlockRegistration.id(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(5.0f, 100.0f)
                .sound(SoundType.STONE)
                .noOcclusion()
                .noCollision()
                .lightLevel(state -> 5)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Matrix is rendered by tile entity special renderer
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileInfusionMatrix(ModBlockEntities.INFUSION_MATRIX.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.INFUSION_MATRIX.get()) {
            if (!level.isClientSide()) {
                return (lvl, pos, st, be) -> TileInfusionMatrix.serverTick(lvl, pos, st, (TileInfusionMatrix) be);
            } else {
                return (lvl, pos, st, be) -> TileInfusionMatrix.clientTick(lvl, pos, st, (TileInfusionMatrix) be);
            }
        }
        return null;
    }
}
