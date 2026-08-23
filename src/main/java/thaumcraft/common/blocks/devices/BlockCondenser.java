package thaumcraft.common.blocks.devices;
import net.minecraft.world.level.redstone.Orientation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TileCondenser;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;
import thaumcraft.init.BlockRegistration;

/**
 * Flux Condenser block - converts flux from the aura into Vitium essentia.
 * Requires a lattice structure above it to function efficiently.
 */
public class BlockCondenser extends Block implements EntityBlock {

    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public BlockCondenser() {
        super(BlockRegistration.id(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .requiresCorrectToolForDrops()));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ENABLED, true));
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
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean isMoving) {
        if (!level.isClientSide()) {
            // Redstone can disable the condenser
            boolean powered = level.hasNeighborSignal(pos);
            boolean enabled = state.getValue(ENABLED);
            
            if (powered && enabled) {
                level.setBlock(pos, state.setValue(ENABLED, false), 3);
            } else if (!powered && !enabled) {
                level.setBlock(pos, state.setValue(ENABLED, true), 3);
            }
            
            // Notify tile entity to recheck lattice structure
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof TileCondenser condenser) {
                condenser.triggerCheck();
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof TileCondenser condenser) {
                condenser.triggerCheck();
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileCondenser(ModBlockEntities.CONDENSER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.CONDENSER.get()) {
            return null;
        }
        
        if (level.isClientSide()) {
            return (lvl, pos, st, te) -> TileCondenser.clientTick(lvl, pos, st, (TileCondenser) te);
        }
        return (lvl, pos, st, te) -> TileCondenser.serverTick(lvl, pos, st, (TileCondenser) te);
    }
}
