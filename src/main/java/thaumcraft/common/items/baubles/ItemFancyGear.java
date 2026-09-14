package thaumcraft.common.items.baubles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.items.IVisDiscountGear;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Fancy Vis Gear - Enhanced vis discount gear (amulet, ring, girdle variants).
 * 
 * These provide a larger vis discount (3% each) compared to mundane gear.
 * Part of the fancy gear set from 1.12.
 */
public class ItemFancyGear extends Item implements IVisDiscountGear {
    
    public static final int VIS_DISCOUNT = 3;
    
    public ItemFancyGear() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)));
    }
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return VIS_DISCOUNT;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.thaumcraft.fancy_gear.text")
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.thaumcraft.fancy_gear.discount", VIS_DISCOUNT)
                .withStyle(ChatFormatting.AQUA));
    }
}
