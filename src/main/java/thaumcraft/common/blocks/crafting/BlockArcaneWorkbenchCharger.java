package thaumcraft.common.blocks.crafting;
import net.minecraft.world.level.redstone.Orientation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.MenuProvider;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;
import thaumcraft.init.ModBlocks;

/**
 * Arcane Workbench Charger - An accessory block placed on top of the Arcane Workbench.
 * 
 * This block can only be placed on top of an Arcane Workbench.
 * When the workbench below is removed, this block drops as an item.
 * 
 * Right-clicking opens the workbench GUI below.
 * The charger enhances the workbench's ability to draw vis from the aura.
 */
public class BlockArcaneWorkbenchCharger extends Block {
    
    // Charger has a smaller, decorative shape
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 6, 14);
    
    public BlockArcaneWorkbenchCharger() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(1.25f, 10.0f)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return isValidBase(below);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos below = context.getClickedPos().below();
        BlockState belowState = context.getLevel().getBlockState(below);
        
        if (!isValidBase(belowState)) {
            return null; // Can't place
        }
        
        return defaultBlockState();
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        
        // Notify the workbench below that a charger has been added
        BlockEntity be = level.getBlockEntity(pos.below());
        if (be instanceof TileArcaneWorkbench workbench) {
            workbench.syncTile(true);
        }
        // TileFocalManipulator also supports syncTile via its parent class
    }
    
    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                   BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        // Check if block below is still a valid base
        if (direction == Direction.DOWN && !isValidBase(neighborState)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, currentPos, direction, neighborPos, neighborState, random);
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean isMoving) {
        if (!level.isClientSide()) {
            if (!canSurvive(state, level, pos)) {
                level.destroyBlock(pos, true);
            }
        }
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                  BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        // Open the workbench GUI below
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        if (belowState.is(ModBlocks.ARCANE_WORKBENCH.get())) {
            // Open arcane workbench GUI
            BlockEntity be = level.getBlockEntity(belowPos);
            if (be instanceof TileArcaneWorkbench workbench) {
                player.openMenu(workbench);
                return InteractionResult.CONSUME;
            }
        } else if (belowState.is(ModBlocks.FOCAL_MANIPULATOR.get())) {
            // Open focal manipulator GUI
            BlockEntity be = level.getBlockEntity(belowPos);
            if (be instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider);
                return InteractionResult.CONSUME;
            }
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Checks if a block state is a valid base for the charger.
     */
    private boolean isValidBase(BlockState state) {
        return state.is(ModBlocks.ARCANE_WORKBENCH.get()) || state.is(ModBlocks.FOCAL_MANIPULATOR.get());
    }
    
    // Factory method
    public static BlockArcaneWorkbenchCharger create() {
        return new BlockArcaneWorkbenchCharger();
    }
}
