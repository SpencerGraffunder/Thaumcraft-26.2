package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.ItemHandlerProvider;
import thaumcraft.common.tiles.devices.TileVisGenerator;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Vis Generator block - converts aura vis into Forge Energy (RF/FE).
 * Automatically faces toward adjacent energy receivers when placed.
 */
public class BlockVisGenerator extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0);

    public BlockVisGenerator() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(1.0f)
                .sound(SoundType.WOOD)
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
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        
        // Try to find an adjacent energy receiver
        for (Direction face : Direction.values()) {
            BlockEntity te = level.getBlockEntity(pos.relative(face));
            if (te != null) {
                te.getCapability(ForgeCapabilities.ENERGY, face.getOpposite()).ifPresent(handler -> {
                    // Found a receiver - we'll set facing toward it
                });
                if (te.getCapability(ForgeCapabilities.ENERGY, face.getOpposite()).isPresent()) {
                    return this.defaultBlockState()
                            .setValue(FACING, face)
                            .setValue(ENABLED, true);
                }
            }
        }
        
        // Default to player facing direction
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ENABLED, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ENABLED)) return;
        
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof TileVisGenerator generator) {
            if (generator.getEnergyStored() > 0) {
                Direction face = state.getValue(FACING);
                double x = pos.getX() + 0.5 + (face.getStepX() == 0 ? random.nextGaussian() * 0.1 : face.getStepX() * 0.1);
                double y = pos.getY() + 0.5 + (face.getStepY() == 0 ? random.nextGaussian() * 0.1 : face.getStepY() * 0.1);
                double z = pos.getZ() + 0.5 + (face.getStepZ() == 0 ? random.nextGaussian() * 0.1 : face.getStepZ() * 0.1);
                // TODO: Spawn spark particles when FXDispatcher is implemented
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileVisGenerator(ModBlockEntities.VIS_GENERATOR.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.VIS_GENERATOR.get() 
                ? (lvl, pos, st, te) -> TileVisGenerator.serverTick(lvl, pos, st, (TileVisGenerator) te)
                : null;
    }
}
