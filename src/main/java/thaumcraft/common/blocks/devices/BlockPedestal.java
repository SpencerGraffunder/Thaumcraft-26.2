package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.BlockTC;
import thaumcraft.common.tiles.crafting.TilePedestal;

import javax.annotation.Nullable;

/**
 * Pedestal block for displaying items and use in infusion crafting.
 * Different variants (arcane, ancient, eldritch) provide different stability bonuses.
 */
public class BlockPedestal extends BlockTC implements EntityBlock {

    public enum PedestalType {
        ARCANE(0.0f),
        ANCIENT(0.0f),
        ELDRITCH(0.1f); // Eldritch provides stability bonus

        private final float stabilityBonus;

        PedestalType(float stabilityBonus) {
            this.stabilityBonus = stabilityBonus;
        }

        public float getStabilityBonus() {
            return stabilityBonus;
        }
    }

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),   // Base
            Block.box(3.0, 4.0, 3.0, 13.0, 10.0, 13.0),  // Middle
            Block.box(1.0, 10.0, 1.0, 15.0, 14.0, 15.0)  // Top
    );

    private final PedestalType pedestalType;

    public BlockPedestal(PedestalType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 10.0f)
                .sound(SoundType.STONE)
                .noOcclusion());
        this.pedestalType = type;
    }

    public PedestalType getPedestalType() {
        return pedestalType;
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

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TilePedestal pedestal) {
            if (!pedestal.getDisplayedItem().isEmpty()) {
                // Take item from pedestal
                ItemStack taken = pedestal.tryTakeItem(player);
                if (!taken.isEmpty()) {
                    if (!player.getInventory().add(taken)) {
                        player.drop(taken, false);
                    }
                }
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TilePedestal pedestal) {
            if (pedestal.getDisplayedItem().isEmpty() && !heldItem.isEmpty()) {
                // Place item on pedestal
                pedestal.tryInsertItem(player, heldItem);
            } else if (!pedestal.getDisplayedItem().isEmpty()) {
                // Take item from pedestal
                ItemStack taken = pedestal.tryTakeItem(player);
                if (!taken.isEmpty()) {
                    if (!player.getInventory().add(taken)) {
                        player.drop(taken, false);
                    }
                }
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TilePedestal pedestal) {
                pedestal.dropContents();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TilePedestal(pos, state);
    }

    /**
     * Creates an arcane pedestal.
     */
    public static BlockPedestal createArcane() {
        return new BlockPedestal(PedestalType.ARCANE);
    }

    /**
     * Creates an ancient pedestal.
     */
    public static BlockPedestal createAncient() {
        return new BlockPedestal(PedestalType.ANCIENT);
    }

    /**
     * Creates an eldritch pedestal with stability bonus.
     */
    public static BlockPedestal createEldritch() {
        return new BlockPedestal(PedestalType.ELDRITCH);
    }
}
