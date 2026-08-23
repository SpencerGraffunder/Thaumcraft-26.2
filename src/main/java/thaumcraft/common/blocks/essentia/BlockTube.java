package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.IEssentiaTransport;

import javax.annotation.Nullable;

/**
 * Essentia tubes for transporting essentia between containers.
 * Variants:
 * - Normal: Basic transport
 * - Restricted: One-way flow
 * - Filter: Only allows specific aspect
 * - Valve: Can be toggled on/off
 * - Buffer: Stores small amount of essentia
 */
public class BlockTube extends Block implements EntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    // Center tube shape
    private static final VoxelShape CENTER = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    
    // Connection shapes for each direction
    private static final VoxelShape NORTH_AABB = Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape SOUTH_AABB = Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape EAST_AABB = Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    private static final VoxelShape WEST_AABB = Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape UP_AABB = Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape DOWN_AABB = Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);

    public enum TubeType {
        NORMAL,
        RESTRICTED,
        FILTER,
        VALVE,
        BUFFER,
        ONEWAY
    }

    private final TubeType tubeType;

    public BlockTube(TubeType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.5f, 5.0f)
                .sound(SoundType.METAL)
                .noOcclusion());
        this.tubeType = type;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    public TubeType getTubeType() {
        return tubeType;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_AABB);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_AABB);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_AABB);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_AABB);
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_AABB);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_AABB);
        
        return shape;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean connected = canConnectTo(level, neighborPos, direction.getOpposite());
        return state.setValue(getPropertyForDirection(direction), connected);
    }

    private BooleanProperty getPropertyForDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos pos, Direction side) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof IEssentiaTransport transport) {
            return transport.isConnectable(side);
        }
        // Also connect to other tubes
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BlockTube;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Special interactions for valve and filter tubes
        if (tubeType == TubeType.VALVE) {
            // TODO: Toggle valve when TileTubeValve is implemented
            return InteractionResult.CONSUME;
        } else if (tubeType == TubeType.FILTER) {
            // TODO: Set filter aspect when TileTubeFilter is implemented
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return tubeType == TubeType.BUFFER;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        // TODO: Return buffer fill level when TileTubeBuffer is implemented
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // TODO: Return appropriate tile entity based on tube type
        return null;
    }

    /**
     * Create a normal essentia tube.
     */
    public static BlockTube createNormal() {
        return new BlockTube(TubeType.NORMAL);
    }

    /**
     * Create a restricted tube (one-way flow).
     */
    public static BlockTube createRestricted() {
        return new BlockTube(TubeType.RESTRICTED);
    }

    /**
     * Create a filter tube (aspect filtering).
     */
    public static BlockTube createFilter() {
        return new BlockTube(TubeType.FILTER);
    }

    /**
     * Create a valve tube (toggleable).
     */
    public static BlockTube createValve() {
        return new BlockTube(TubeType.VALVE);
    }

    /**
     * Create a buffer tube (small storage).
     */
    public static BlockTube createBuffer() {
        return new BlockTube(TubeType.BUFFER);
    }

    /**
     * Create a one-way tube (directional flow only).
     */
    public static BlockTube createOneway() {
        return new BlockTube(TubeType.ONEWAY);
    }
}
