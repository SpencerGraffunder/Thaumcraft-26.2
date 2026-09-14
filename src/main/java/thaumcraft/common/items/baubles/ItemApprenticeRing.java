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
 * Apprentice Ring - Vis discount ring providing 5% discount.
 * 
 * This is the only item from the 1.12 mundane/fancy set that provided
 * a vis discount (5%). Ported from ItemBaubles.java (damage 3).
 */
public class ItemApprenticeRing extends Item implements IVisDiscountGear {
    
    public static final int VIS_DISCOUNT = 5;
    
    public ItemApprenticeRing() {
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
        builder.accept(Component.translatable("item.thaumcraft.apprentice_ring.text")
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.thaumcraft.apprentice_ring.discount", VIS_DISCOUNT)
                .withStyle(ChatFormatting.AQUA));
    }
}
