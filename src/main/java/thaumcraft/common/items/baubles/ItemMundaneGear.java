package thaumcraft.common.items.baubles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.items.IVisDiscountGear;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Mundane Vis Gear - Basic vis discount gear (amulet, ring, girdle variants).
 * 
 * These provide a small vis discount (2% each) to help with casting costs.
 * Part of the mundane gear set from 1.12.
 */
public class ItemMundaneGear extends Item implements IVisDiscountGear {
    
    public static final int VIS_DISCOUNT = 2;
    
    public ItemMundaneGear() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.COMMON)));
    }
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return VIS_DISCOUNT;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.thaumcraft.mundane_gear.text")
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.thaumcraft.mundane_gear.discount", VIS_DISCOUNT)
                .withStyle(ChatFormatting.AQUA));
    }
}
