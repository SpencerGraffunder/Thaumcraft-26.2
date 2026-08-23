package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.essentia.TileJar;
import thaumcraft.common.tiles.essentia.TileJarBrain;
import thaumcraft.common.tiles.essentia.TileJarVoid;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Warded jars for storing essentia.
 * Different variants:
 * - Normal jar: Standard essentia storage (250 capacity)
 * - Void jar: Destroys excess essentia when full
 * - Brain jar: Stores XP orbs
 */
public class BlockJar extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);

    public enum JarType {
        NORMAL(250),
        VOID(250),
        BRAIN(0); // Brain jar stores XP, not essentia

        private final int capacity;

        JarType(int capacity) {
            this.capacity = capacity;
        }

        public int getCapacity() {
            return capacity;
        }
    }

    private final JarType jarType;

    public BlockJar(JarType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(0.3f)
                .sound(SoundType.GLASS)
                .noOcclusion());
        this.jarType = type;
    }

    public JarType getJarType() {
        return jarType;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        // TODO: Implement jar interaction when TileJar is implemented
        // - Right-click with phial to fill/drain
        // - Shift-right-click to void contents
        // - Apply label for filtering
        // - Brain jar: dispense XP on right-click

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            // TODO: Drop jar contents or spawn essentia pollution when TileJar is implemented
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        // TODO: Return fill level when TileJar is implemented
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (jarType) {
            case BRAIN -> new TileJarBrain(pos, state);
            case VOID -> new TileJarVoid(pos, state);
            default -> new TileJar(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() && jarType != JarType.BRAIN) {
            return null;
        }
        
        return switch (jarType) {
            case BRAIN -> {
                if (type == ModBlockEntities.JAR_BRAIN.get()) {
                    if (level.isClientSide()) {
                        yield (lvl, pos, st, te) -> TileJarBrain.clientTick(lvl, pos, st, (TileJarBrain) te);
                    } else {
                        yield (lvl, pos, st, te) -> TileJarBrain.serverTick(lvl, pos, st, (TileJarBrain) te);
                    }
                }
                yield null;
            }
            case VOID -> type == ModBlockEntities.JAR_VOID.get() ?
                    (lvl, pos, st, te) -> TileJarVoid.serverTick(lvl, pos, st, (TileJarVoid) te) : null;
            default -> type == ModBlockEntities.JAR.get() ? 
                    (lvl, pos, st, te) -> TileJar.serverTick(lvl, pos, st, (TileJar) te) : null;
        };
    }

    /**
     * Create a normal warded jar.
     */
    public static BlockJar createNormal() {
        return new BlockJar(JarType.NORMAL);
    }

    /**
     * Create a void jar that destroys excess essentia.
     */
    public static BlockJar createVoid() {
        return new BlockJar(JarType.VOID);
    }

    /**
     * Create a brain-in-a-jar that stores XP.
     */
    public static BlockJar createBrain() {
        return new BlockJar(JarType.BRAIN);
    }
}
