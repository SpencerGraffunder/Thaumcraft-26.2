package thaumcraft.common.blocks.world.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.init.BlockRegistration;

/**
 * Leaves blocks for greatwood and silverwood trees.
 */
public class BlockLeavesTC extends LeavesBlock {

    private final boolean glows;

    public BlockLeavesTC(Properties properties, boolean glows) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), BlockRegistration.id(properties));
        this.glows = glows;
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
                .isViewBlocking((state, level, pos, nearPlaneBox) -> false),
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
                .isViewBlocking((state, level, pos, nearPlaneBox) -> false),
                true);
    }
}
