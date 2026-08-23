package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TileDioptra;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * BlockDioptra - Vis/Flux visualization device.
 * 
 * Displays vis and flux levels in a 13x13 chunk grid around it.
 * Right-click to toggle between vis and flux display modes.
 * Outputs comparator signal based on vis level in its chunk.
 */
public class BlockDioptra extends Block implements EntityBlock {
    
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    
    // Slightly smaller shape - like a viewing device
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);
    
    public BlockDioptra() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f)
                .sound(SoundType.STONE)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ENABLED, true)); // true = show vis, false = show flux
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENABLED);
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
        // Toggle between vis and flux display
        boolean currentMode = state.getValue(ENABLED);
        level.setBlock(pos, state.setValue(ENABLED, !currentMode), 3);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
    
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileDioptra dioptra) {
            // Center of grid is index 84 (6*13 + 6 for a 13x13 grid)
            float r = dioptra.getVisAtCenter() / 64.0f;
            return Mth.floor(r * 14.0f) + (r > 0.0f ? 1 : 0);
        }
        return 0;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileDioptra(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (type == ModBlockEntities.DIOPTRA.get()) {
            if (level.isClientSide()) {
                return (lvl, pos, st, be) -> TileDioptra.clientTick(lvl, pos, st, (TileDioptra) be);
            } else {
                return (lvl, pos, st, be) -> TileDioptra.serverTick(lvl, pos, st, (TileDioptra) be);
            }
        }
        return null;
    }
}
