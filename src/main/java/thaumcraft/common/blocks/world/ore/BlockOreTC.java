package thaumcraft.common.blocks.world.ore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.init.BlockRegistration;

/**
 * Ore blocks for Thaumcraft (amber, cinnabar, quartz).
 * These use loot tables for drops in 1.20.1.
 */
public class BlockOreTC extends Block {

    /** 1.12 getExpDrop: amber and quartz ores drop 1-4 XP (non-silk-touch). */
    private final boolean dropsXp;

    public BlockOreTC(Properties properties, boolean dropsXp) {
        super(BlockRegistration.id(properties));
        this.dropsXp = dropsXp;
    }

    /**
     * 1.12 getExpDrop: 1-4 XP when mined without silk touch (26.3 has no
     * experience loot entry type; playerWillDestroy is the modern XP hook).
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (!dropsXp || level.isClientSide()) return result;
        ItemStack tool = player.getMainHandItem();
        boolean silkTouch = tool.getEnchantments().getLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH)) > 0;
        if (!silkTouch) {
            popExperience((ServerLevel) level, pos, 1 + level.getRandom().nextInt(4));
        }
        return result;
    }

    /**
     * Creates amber ore - drops amber items.
     */
    public static BlockOreTC createAmberOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(1.5f, 5.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops(), true);
    }

    /**
     * Creates cinnabar ore - drops cinnabar and quicksilver.
     */
    public static BlockOreTC createCinnabarOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 5.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops(), false);
    }

    /**
     * Creates quartz ore (overworld variant) - drops quartz.
     */
    public static BlockOreTC createQuartzOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f, 5.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops(), true);
    }

    // ==================== Deepslate Variants ====================

    /**
     * Creates deepslate amber ore - drops amber items.
     */
    public static BlockOreTC createDeepslateAmberOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .strength(3.0f, 6.0f)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops(), true);
    }

    /**
     * Creates deepslate cinnabar ore - drops cinnabar and quicksilver.
     */
    public static BlockOreTC createDeepslateCinnabarOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .strength(3.5f, 6.0f)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops(), false);
    }

    /**
     * Creates deepslate quartz ore - drops quartz.
     */
    public static BlockOreTC createDeepslateQuartzOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .strength(4.5f, 6.0f)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops(), true);
    }
}
