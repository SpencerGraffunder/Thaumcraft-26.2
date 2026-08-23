package thaumcraft.common.items.baubles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Voidseer Charm - A charm that converts accumulated warp into vis discount.
 * The more permanent warp the player has, the higher the discount (up to 25%).
 * However, wearing it also adds warp based on the discount.
 * 
 * This is a double-edged item for players who have embraced corruption.
 * 
 * TODO: Add Curios integration for charm slot support.
 */
public class ItemVoidseerCharm extends Item implements IVisDiscountGear, IWarpingGear {
    
    public ItemVoidseerCharm() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        // Calculate discount based on permanent warp
        // Max 25% discount at 100+ permanent warp
        // TODO: Get actual warp from player capability when implemented
        int permanentWarp = getPlayerPermanentWarp(player);
        permanentWarp = Math.min(100, permanentWarp);
        return (int) (permanentWarp / 100.0f * 25.0f);
    }
    
    @Override
    public int getWarp(ItemStack stack, Player player) {
        // Warp added is 1/5th of the discount gained
        return getVisDiscount(stack, player) / 5;
    }
    
    /**
     * Get the player's permanent warp level.
     */
    private int getPlayerPermanentWarp(Player player) {
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null) {
            return warp.get(IPlayerWarp.EnumWarpType.PERMANENT);
        }
        return 0;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.thaumcraft.voidseer_charm.text")
                .withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.ITALIC));
        builder.accept(Component.translatable("item.thaumcraft.voidseer_charm.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
