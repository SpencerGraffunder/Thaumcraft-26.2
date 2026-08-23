package thaumcraft.common.items.curios;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            // Client: Open the Thaumonomicon GUI
            openThaumonomiconGui();
            return InteractionResult.SUCCESS;
        }

        // Server: Could sync research data here if needed
        return InteractionResult.CONSUME;
    }

    /**
     * Open the Thaumonomicon GUI on the client.
     * Must be called only on the client side.
     */
    @OnlyIn(Dist.CLIENT)
    private void openThaumonomiconGui() {
        // Use reflection so this common item class carries no direct client-class
        // references (keeps the class loadable on the dedicated server in dev).
        try {
            Class<?> screenClass = Class.forName("thaumcraft.client.gui.screens.ResearchBrowserScreen");
            Object screen = screenClass.getConstructor().newInstance();
            Minecraft.getInstance().gui.setScreen((net.minecraft.client.gui.screens.Screen) screen);
        } catch (Exception e) {
            thaumcraft.Thaumcraft.LOGGER.error("Failed to open Thaumonomicon GUI", e);
        }
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
