package thaumcraft.common.blocks.world.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.IShearable;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.init.BlockRegistration;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * Leaves blocks for greatwood and silverwood trees.
 *
 * 1.12 parity (BlockLeavesTC):
 * - flammability 60 / fire spread 30
 * - natural (not player-placed) SILVERWOOD leaves slowly regenerate vis in the
 *   local aura chunk (up to the aura base): +0.01 per random tick
 * - broken leaves drop a sapling at 1/75 per leaf; silverwood leaves additionally
 *   drop a quicksilver nugget at 1/(75*0.75)
 * - shearing drops the leaves block
 *
 * The modern LeavesBlock PERSISTENT property is the 1.12 "decayable" flag with
 * inverted polarity: player-placed leaves are PERSISTENT, tree-grown leaves are not.
 */
public class BlockLeavesTC extends LeavesBlock {

    /** 1.12 getSaplingChance = 75 -> each leaf has a 1/75 chance to drop its sapling. */
    public static final int SAPLING_CHANCE = 75;

    private final boolean silverwood;

    public BlockLeavesTC(BlockBehaviour.Properties properties, boolean silverwood) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), BlockRegistration.id(properties));
        this.silverwood = silverwood;
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
     * Creates silverwood leaves - they glow slightly and regenerate vis.
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

    // ==================== Fire (1.12: flammability 60, spread 30) ====================

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction side) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction side) {
        return 30;
    }

    // ==================== 1.12 behavior ====================

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        // 1.12: natural (non-persistent) silverwood leaves add 0.01 vis per random tick
        // while the chunk's vis is below the aura base.
        if (silverwood && !state.getValue(PERSISTENT)
                && AuraHandler.getVis(level, pos) < AuraHandler.getAuraBase(level, pos)) {
            AuraHandler.addVis(level, pos, 0.01f);
        }
        super.randomTick(state, level, pos, rand);
    }

    // Drops: 1.12 dropBlock (1/75 sapling, silverwood +1/56 quicksilver nugget) is
    // implemented in the block loot tables (26.3 removed the onRemove hook).

    // ==================== Shear (1.12 onSheared: drops the leaves block) ====================

    @Override
    public boolean isShearable(Player player, ItemStack tool, Level level, BlockPos pos) {
        return true;
    }

    @Override
    public List<ItemStack> onSheared(Player player, ItemStack tool, Level level, BlockPos pos) {
        return List.of(new ItemStack(this));
    }
}
