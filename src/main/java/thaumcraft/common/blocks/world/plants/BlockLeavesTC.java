package thaumcraft.common.blocks.world.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Leaves blocks for greatwood and silverwood trees.
 */
public class BlockLeavesTC extends LeavesBlock {

    private final boolean glows;

    public BlockLeavesTC(Properties properties, boolean glows) {
        super(0.25f, properties);
        this.glows = glows;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // No falling leaf particles for Thaumcraft trees
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return simpleCodec(properties -> new BlockLeavesTC(properties, this.glows));
    }

    /**
     * Creates greatwood leaves.
     */
    public static BlockLeavesTC createGreatwood() {
        return new BlockLeavesTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .strength(0.2f)
                .randomTicks()
                .sound(SoundType.GRASS)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false),
                false);
    }

    /**
     * Creates silverwood leaves - they glow slightly.
     */
    public static BlockLeavesTC createSilverwood() {
        return new BlockLeavesTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(0.2f)
                .randomTicks()
                .sound(SoundType.GRASS)
                .noOcclusion()
                .lightLevel(state -> 4)
                .isValidSpawn((state, level, pos, type) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false),
                true);
    }
}
