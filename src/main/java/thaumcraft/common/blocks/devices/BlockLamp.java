package thaumcraft.common.blocks.devices;
import net.minecraft.world.level.redstone.Orientation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Arcane lamp that provides magical light.
 * Different variants:
 * - Arcane: Creates invisible light blocks in dark areas
 * - Growth: Speeds up plant growth
 * - Fertility: Speeds up animal breeding
 */
public class BlockLamp extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    private static final VoxelShape SHAPE = Block.box(4.0, 2.0, 4.0, 12.0, 14.0, 12.0);

    public enum LampType {
        ARCANE,
        GROWTH,
        FERTILITY
    }

    private final LampType lampType;

    public BlockLamp(LampType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(state -> state.getValue(ENABLED) ? 15 : 0));
        this.lampType = type;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(ENABLED, false));
    }

    public LampType getLampType() {
        return lampType;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getClickedFace().getOpposite())
                .setValue(ENABLED, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos attachedPos = pos.relative(facing);
        return level.getBlockState(attachedPos).isFaceSturdy(level, attachedPos, facing.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                   BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Direction facing = state.getValue(FACING);
        if (direction == facing && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean isMoving) {
        if (!level.isClientSide()) {
            boolean powered = level.hasNeighborSignal(pos);
            boolean enabled = state.getValue(ENABLED);
            
            // Toggle based on redstone (inverted - redstone disables lamp)
            if (powered && enabled) {
                level.setBlock(pos, state.setValue(ENABLED, false), 3);
            } else if (!powered && !enabled) {
                level.setBlock(pos, state.setValue(ENABLED, true), 3);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // TODO: Return appropriate TileLamp variant when implemented
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // TODO: Return ticker when TileLamp is implemented
        return null;
    }

    /**
     * Create an arcane lamp (creates invisible light blocks).
     */
    public static BlockLamp createArcane() {
        return new BlockLamp(LampType.ARCANE);
    }

    /**
     * Create a growth lamp (speeds up plant growth).
     */
    public static BlockLamp createGrowth() {
        return new BlockLamp(LampType.GROWTH);
    }

    /**
     * Create a fertility lamp (speeds up animal breeding).
     */
    public static BlockLamp createFertility() {
        return new BlockLamp(LampType.FERTILITY);
    }
}
