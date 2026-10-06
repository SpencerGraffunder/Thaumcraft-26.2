package thaumcraft.api.research;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A scan thing that matches anything carrying one of the given item tags.
 * <p>
 * This is the 26.3 replacement for 1.12's {@code ScanOreDictionary}: ore dictionary
 * entries became item tags, so {@code "ingotIron"} becomes {@code #c:ingots/iron}.
 */
public class ScanTag implements IScanThing {

    private final String research;
    private final TagKey<Item>[] tags;

    @SafeVarargs
    public ScanTag(String research, TagKey<Item>... tags) {
        this.research = research;
        this.tags = tags;
    }

    /** Convenience factory so call sites can name tags by path. */
    public static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.parse(namespace + ":" + path));
    }

    @Override
    public boolean checkThing(Player player, Object obj) {
        ItemStack stack = ItemStack.EMPTY;

        if (obj instanceof ItemStack is) {
            stack = is;
        } else if (obj instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty()) {
            stack = itemEntity.getItem();
        } else if (obj instanceof BlockPos pos) {
            BlockState state = player.level().getBlockState(pos);
            stack = new ItemStack(state.getBlock());
        }

        if (stack.isEmpty()) return false;

        for (TagKey<Item> t : tags) {
            if (stack.is(t)) return true;
        }
        return false;
    }

    @Override
    public String getResearchKey(Player player, Object object) {
        return research;
    }
}
