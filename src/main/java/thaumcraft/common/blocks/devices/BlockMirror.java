package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import thaumcraft.init.BlockRegistration;
import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.common.tiles.devices.TileMirrorEssentia;
import thaumcraft.init.ModBlockEntities;

/**
 * Magic mirror for item teleportation.
 * Links to another mirror to teleport items between locations.
 * Can also teleport players when configured.
 */
public class BlockMirror extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    private static final VoxelShape SHAPE_NORTH = Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 4.0);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2.0, 2.0, 12.0, 14.0, 14.0, 16.0);
    private static final VoxelShape SHAPE_EAST = Block.box(12.0, 2.0, 2.0, 16.0, 14.0, 14.0);
    private static final VoxelShape SHAPE_WEST = Block.box(0.0, 2.0, 2.0, 4.0, 14.0, 14.0);

    public enum MirrorType {
        ITEM,       // Teleports items only
        PLAYER,     // Teleports players (essentia mirror)
        HAND        // Hand mirror - portable
    }

    private final MirrorType mirrorType;

    public BlockMirror(MirrorType type) {
        super(BlockRegistration.id(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .lightLevel(state -> state.getValue(LINKED) ? 7 : 0)));
        this.mirrorType = type;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LINKED, false));
    }

    public MirrorType getMirrorType() {
        return mirrorType;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LINKED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LINKED, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                  BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        // Mirrors are linked via the Hand Mirror item (ItemHandMirror.useOn).

        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (mirrorType) {
            case ITEM -> new TileMirror(ModBlockEntities.MIRROR_ITEM.get(), pos, state);
            case PLAYER -> new TileMirrorEssentia(ModBlockEntities.MIRROR_ESSENTIA.get(), pos, state);
            case HAND -> null;
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, st, be) -> { if (be instanceof TileMirror t) TileMirror.serverTick(lvl, pos, st, t); };
        }
        return null;
    }

    /**
     * Create an item mirror (teleports items).
     */
    public static BlockMirror createItem() {
        return new BlockMirror(MirrorType.ITEM);
    }

    /**
     * Create a player mirror (essentia mirror - teleports players).
     */
    public static BlockMirror createPlayer() {
        return new BlockMirror(MirrorType.PLAYER);
    }
}
