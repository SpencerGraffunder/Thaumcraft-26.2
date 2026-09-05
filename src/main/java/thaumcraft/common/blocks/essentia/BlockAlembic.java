package thaumcraft.common.blocks.essentia;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.Aspect;
import net.minecraft.world.item.ItemStack;

import net.minecraft.core.Direction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import thaumcraft.init.BlockRegistration;
import thaumcraft.common.tiles.essentia.TileAlembic;
import thaumcraft.init.ModBlockEntities;

/**
 * Alembic for distilling essentia from items in a crucible.
 * Placed on top of or adjacent to a crucible to collect essentia.
 * Can be labeled to filter specific aspects.
 */
public class BlockAlembic extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    private static final int MAX_ESSENTIA = 64;

    public BlockAlembic() {
        super(BlockRegistration.id(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(1.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                  BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Phial interaction: right-click with an empty essentia container to extract 1 essence
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof IEssentiaContainerItem container) {
            AspectList heldAspects = container.getAspects(held);
            if (heldAspects == null || heldAspects.size() == 0) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof thaumcraft.common.tiles.essentia.TileAlembic tile) {
                    Direction side = hit.getDirection();
                    Aspect aspect = tile.getEssentiaType(side);
                    int amt = tile.getEssentiaAmount(side);
                    if (aspect != null && amt > 0) {
                        if (tile.takeFromContainer(aspect, 1)) {
                            AspectList cur = container.getAspects(held);
                            if (cur == null) cur = new AspectList();
                            cur.add(aspect, 1);
                            container.setAspects(held, cur);
                                                        return InteractionResult.CONSUME;
                        }
                    }
                }
            }
        }


        BlockEntity blockEntity = level.getBlockEntity(pos);
        // TODO: Implement alembic interaction when TileAlembic is implemented
        // - Right-click with phial to extract essentia
        // - Shift-right-click to void contents
        // - Apply label to filter specific aspect
        // - Shift-right-click with empty hand to remove label

        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            // TODO: Release essentia as flux when TileAlembic is implemented
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        // TODO: Return fill level when TileAlembic is implemented
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileAlembic(ModBlockEntities.ALEMBIC.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // TODO: Return ticker when TileAlembic is implemented
        return null;
    }
}
