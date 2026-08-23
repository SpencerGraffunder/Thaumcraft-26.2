package thaumcraft.common.blocks.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.init.BlockRegistration;

/**
 * Candle blocks that provide light and infusion stabilization.
 * 16 color variants available.
 */
public class BlockCandle extends Block {

    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 8.0, 10.0);
    
    private final DyeColor color;
    private final float stabilizationBonus;

    public BlockCandle(DyeColor color) {
        super(BlockRegistration.id(BlockBehaviour.Properties.of()
                .mapColor(getMapColorForDye(color))
                .strength(0.1f)
                .sound(SoundType.WOOL)
                .lightLevel(state -> 14)
                .noOcclusion()
                .noCollision()));
        this.color = color;
        this.stabilizationBonus = 0.1f;
    }

    private static MapColor getMapColorForDye(DyeColor dye) {
        return switch (dye) {
            case WHITE -> MapColor.SNOW;
            case ORANGE -> MapColor.COLOR_ORANGE;
            case MAGENTA -> MapColor.COLOR_MAGENTA;
            case LIGHT_BLUE -> MapColor.COLOR_LIGHT_BLUE;
            case YELLOW -> MapColor.COLOR_YELLOW;
            case LIME -> MapColor.COLOR_LIGHT_GREEN;
            case PINK -> MapColor.COLOR_PINK;
            case GRAY -> MapColor.COLOR_GRAY;
            case LIGHT_GRAY -> MapColor.COLOR_LIGHT_GRAY;
            case CYAN -> MapColor.COLOR_CYAN;
            case PURPLE -> MapColor.COLOR_PURPLE;
            case BLUE -> MapColor.COLOR_BLUE;
            case BROWN -> MapColor.COLOR_BROWN;
            case GREEN -> MapColor.COLOR_GREEN;
            case RED -> MapColor.COLOR_RED;
            case BLACK -> MapColor.COLOR_BLACK;
        };
    }

    public DyeColor getColor() {
        return color;
    }

    /**
     * Returns the stabilization bonus for infusion crafting.
     */
    public float getStabilizationBonus() {
        return stabilizationBonus;
    }

    /**
     * Candles can stabilize infusion without symmetry penalty.
     */
    public boolean canStabilizeInfusion(Level level, BlockPos pos) {
        return true;
    }

    public boolean hasSymmetryPenalty() {
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                   BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.7;
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }

    /**
     * Create a candle of a specific color.
     */
    public static BlockCandle create(DyeColor color) {
        return new BlockCandle(color);
    }
}
