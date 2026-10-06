package thaumcraft.common.lib.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IDustTrigger;
import thaumcraft.common.lib.events.ServerEvents;

import javax.annotation.Nullable;

/**
 * Dust trigger that matches any block carrying a block tag - the 26.3 equivalent of
 * 1.12's {@code DustTriggerOre}, which matched OreDictionary entries ("bookshelf",
 * "workbench") so that modded bookshelves and crafting tables converted as well.
 *
 * <p>1.12 reference: {@code thaumcraft/common/lib/crafting/DustTriggerOre.java},
 * registered in {@code ConfigRecipes.initializeCompoundRecipes()} alongside
 * {@link DustTriggerSimple} for the same research gates.
 */
public class DustTriggerTag implements IDustTrigger {

    private final TagKey<Block> target;
    private final ItemStack result;
    private final String research;

    /**
     * @param research required research key (null for no requirement)
     * @param target   block tag every matching block accepts, e.g. {@code c:bookshelves}
     * @param result   block/item to place in the target's place
     */
    public DustTriggerTag(@Nullable String research, TagKey<Block> target, ItemStack result) {
        this.target = target;
        this.result = result;
        this.research = research;
    }

    @Override
    public Placement getValidFace(Level level, Player player, BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);

        // 1.12: OreDictionary.getOreIDs(block) contains the target oredict name
        if (!state.is(target)) {
            return null;
        }

        if (research != null && !ThaumcraftCapabilities.knowsResearch(player, research)) {
            return null;
        }

        return new Placement(0, 0, 0, null);
    }

    @Override
    public void execute(Level level, Player player, BlockPos pos, Placement placement, Direction side) {
        if (level.isClientSide()) {
            return;
        }

        // 1.12: FMLCommonHandler.firePlayerCraftingEvent(player, result, InventoryFake)
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, result.copy(), null));

        BlockState state = level.getBlockState(pos);

        // 1.12: ServerEvents.addRunnableServer(world, swapperRunnable, 50)
        ServerEvents.addRunnableServer(level, () -> {
            ServerEvents.addSwapper(level, pos, state, result.copy(), false, 0, player,
                    true, true, -9999, false, false, 0, ServerEvents.DEFAULT_PREDICATE, 0.0f);
        }, 50);
    }

    /** Block tag helper for the {@code c:} (NeoForge common) namespace. */
    public static TagKey<Block> commonTag(String path) {
        return TagKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", path));
    }
}
