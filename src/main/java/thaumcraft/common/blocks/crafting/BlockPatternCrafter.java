package thaumcraft.common.blocks.crafting;
import net.minecraft.world.level.redstone.Orientation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
import thaumcraft.common.tiles.crafting.TilePatternCrafter;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;

/**
 * BlockPatternCrafter - Automated crafting device.
 * 
 * Applies items from above inventory in a configurable pattern to a
 * crafting grid and outputs results below.
 * Right-click to cycle through 10 different pattern configurations.
 */
public class BlockPatternCrafter extends Block implements EntityBlock {
    
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    
    public BlockPatternCrafter() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f)
                .sound(SoundType.METAL)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ENABLED, true));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TilePatternCrafter crafter) {
            if (!level.isClientSide()) {
                crafter.cycle();
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        ModSounds.KEY.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
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
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TilePatternCrafter(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (type == ModBlockEntities.PATTERN_CRAFTER.get()) {
            if (level.isClientSide()) {
                return (lvl, pos, st, be) -> TilePatternCrafter.clientTick(lvl, pos, st, (TilePatternCrafter) be);
            } else {
                return (lvl, pos, st, be) -> TilePatternCrafter.serverTick(lvl, pos, st, (TilePatternCrafter) be);
            }
        }
        return null;
    }
}
