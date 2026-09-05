package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.init.BlockRegistration;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

/**
 * Flux Condenser Lattice block. Forms the wireframe structure above a
 * {@code BlockCondenser}. Two distinct instances are registered:
 * {@code condenser_lattice} (clean) and {@code condenser_lattice_dirty}
 * (clogged). A dirty lattice is cleaned by right-clicking it with a filter,
 * which consumes the filter, drops a flux crystal, and restores the clean
 * block.
 *
 * <p>Ported from the 1.12.2 {@code BlockCondenserLattice} to the 26.2 block
 * API (six {@link BooleanProperty} connection states instead of metadata,
 * dynamic voxel shape instead of an AABB).
 */
public class BlockCondenserLattice extends Block {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    // Center cube (the "core" model is a 6x6x6 cube centred in the block)
    private static final VoxelShape CENTER = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape NORTH_AABB = Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape SOUTH_AABB = Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape EAST_AABB = Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    private static final VoxelShape WEST_AABB = Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape UP_AABB = Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape DOWN_AABB = Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);

    private final boolean dirty;

    private BlockCondenserLattice(boolean dirty) {
        super(BlockRegistration.id(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.5f, 5.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noCollision()
                .lightLevel(s -> dirty ? 0 : 10)));
        this.dirty = dirty;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    /** Clean lattice instance (registered as {@code condenser_lattice}). */
    public static BlockCondenserLattice clean() {
        return new BlockCondenserLattice(false);
    }

    /** Dirty / clogged lattice instance (registered as {@code condenser_lattice_dirty}). */
    public static BlockCondenserLattice dirty() {
        return new BlockCondenserLattice(true);
    }

    public boolean isDirty() {
        return dirty;
    }

    public static boolean isLattice(BlockState state) {
        return state.getBlock() instanceof BlockCondenserLattice;
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

    /**
     * A lattice connects to another lattice, or (when the neighbour is below)
     * to a condenser.
     */
    private boolean canConnectTo(LevelReader level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlockCondenserLattice) {
            return true;
        }
        return side == Direction.DOWN && state.is(ModBlocks.CONDENSER.get());
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level,
                                  ScheduledTickAccess ticks,
                                  BlockPos pos, Direction direction, BlockPos neighborPos,
                                  BlockState neighborState, RandomSource random) {
        boolean connected = canConnectTo(level, neighborPos, direction.getOpposite());
        return state.setValue(getPropertyForDirection(direction), connected);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        recheckConnections(level, pos);
    }

    /**
     * Recompute the six connection properties against current neighbours and
     * notify the condenser below (if any) so it re-scans its lattice.
     */
    private void recheckConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean[] cons = { false, false, false, false, false, false };
        int a = 0;
        for (Direction face : Direction.values()) {
            BlockState ns = level.getBlockState(pos.relative(face));
            if (ns.getBlock() instanceof BlockCondenserLattice
                    || (face == Direction.DOWN && ns.is(ModBlocks.CONDENSER.get()))) {
                cons[a] = true;
            }
            a++;
        }
        BlockState updated = state
                .setValue(DOWN, cons[0])
                .setValue(UP, cons[1])
                .setValue(NORTH, cons[2])
                .setValue(SOUTH, cons[3])
                .setValue(WEST, cons[4])
                .setValue(EAST, cons[5]);
        if (!state.equals(updated)) {
            level.setBlock(pos, updated, 3);
        }
        // setBlock above already fires neighbour updates; explicitly refresh
        // adjacent lattices so their own connection property toward this block
        // is recomputed (mirrors the 1.12.2 makeConnections propagation).
        for (Direction face : Direction.values()) {
            BlockPos p2 = pos.relative(face);
            BlockState nstate = level.getBlockState(p2);
            if (nstate.getBlock() instanceof BlockCondenserLattice) {
                boolean conn = canConnectTo(level, pos, face.getOpposite());
                level.setBlock(p2, nstate.setValue(getPropertyForDirection(face), conn), 3);
            }
        }
        // Notify the condenser directly below to re-run its lattice check
        BlockState below = level.getBlockState(pos.below());
        if (below.is(ModBlocks.CONDENSER.get())) {
            BlockEntity te = level.getBlockEntity(pos.below());
            if (te instanceof thaumcraft.common.tiles.devices.TileCondenser condenser) {
                condenser.triggerCheck();
            }
        }
    }

    /**
     * Right-click a dirty lattice with a filter to clean it (1.12.2 behaviour).
     */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                             BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (this.dirty && player.getMainHandItem().is(ModItems.FILTER.get())) {
            player.getMainHandItem().shrink(1);
            if (level.getRandom().nextBoolean()) {
                popResource(level, pos, new ItemStack(ModItems.FLUX_CRYSTAL.get()));
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f,
                    (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7f + 1.0f);
            level.setBlock(pos, ModBlocks.CONDENSER_LATTICE.get().defaultBlockState(), 3);
            recheckConnections(level, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
