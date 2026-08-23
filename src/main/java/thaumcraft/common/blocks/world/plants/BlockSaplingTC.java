package thaumcraft.common.blocks.world.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import javax.annotation.Nullable;
import thaumcraft.init.BlockRegistration;

/**
 * Sapling blocks for greatwood and silverwood trees.
 */
public class BlockSaplingTC extends SaplingBlock {

    public BlockSaplingTC(TreeGrower treeGrower, Properties properties) {
        super(treeGrower, BlockRegistration.id(properties));
    }

    /**
     * Creates greatwood sapling.
     */
    public static BlockSaplingTC createGreatwood() {
        return new BlockSaplingTC(
                new TreeGrower("thaumcraft_greatwood", java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PLANT)
                        .noCollision()
                        .randomTicks()
                        .instabreak()
                        .sound(SoundType.GRASS));
    }

    /**
     * Creates silverwood sapling.
     */
    public static BlockSaplingTC createSilverwood() {
        return new BlockSaplingTC(
                new TreeGrower("thaumcraft_silverwood", java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.QUARTZ)
                        .noCollision()
                        .randomTicks()
                        .instabreak()
                        .sound(SoundType.GRASS)
                        .lightLevel(state -> 5));
    }
}
