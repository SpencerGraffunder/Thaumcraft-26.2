package thaumcraft.common.blocks.devices;
import net.minecraft.world.level.redstone.Orientation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TilePotionSprayer;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;
import thaumcraft.init.BlockRegistration;

/**
 * BlockPotionSprayer - Applies potion effects to nearby entities using essentia.
 * 
 * The potion sprayer loads potions which determine the effect to apply.
 * It draws essentia matching the potion's aspects to power the effect.
 * Activated by redstone signal.
 */
public class BlockPotionSprayer extends Block implements EntityBlock {
    
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);
    
    public BlockPotionSprayer() {
        super(BlockRegistration.id(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f)
                .sound(SoundType.METAL)
                .noOcclusion()));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(ENABLED, true));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(ENABLED, true);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                  BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        // TODO: Open GUI when menu system is implemented
        // For now, just toggle enabled state
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TilePotionSprayer sprayer) {
            // Could open menu here
            // player.openMenu(sprayer);
        }
        
        return InteractionResult.CONSUME;
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                 Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);
        
        // Update enabled state based on redstone
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != !state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, !powered), 2);
        }
    }
    
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TilePotionSprayer sprayer) {
                for (int i = 0; i < sprayer.getContainerSize(); i++) {
                    ItemStack stack = sprayer.getItem(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TilePotionSprayer(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (type == ModBlockEntities.POTION_SPRAYER.get()) {
            if (level.isClientSide()) {
                return (lvl, pos, st, be) -> TilePotionSprayer.clientTick(lvl, pos, st, (TilePotionSprayer) be);
            } else {
                return (lvl, pos, st, be) -> TilePotionSprayer.serverTick(lvl, pos, st, (TilePotionSprayer) be);
            }
        }
        return null;
    }
}
