package thaumcraft.common.items.curios;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import thaumcraft.common.items.ItemTC;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Thaumonomicon - the player's guide to all things Thaumcraft.
 * Opens the research GUI when used.
 */
public class ItemThaumonomicon extends ItemTC {

    public ItemThaumonomicon() {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // The research GUI opens client-side via ClientEvents#onRightClickItem
        // (PlayerInteractEvent.RightClickItem). In modern Minecraft Item.use() is
        // only invoked on the server, so the client open cannot live here.
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.thaumcraft.thaumonomicon.desc"));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.thaumcraft.thaumonomicon");
    }
}
