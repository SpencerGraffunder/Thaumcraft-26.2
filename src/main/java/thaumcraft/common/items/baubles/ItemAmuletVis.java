package thaumcraft.common.items.baubles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Amulet of Vis - A bauble that slowly recharges rechargeable items in the player's inventory.
 * Comes in two variants: found (slower) and crafted (faster).
 * 
 * TODO: Add Curios integration for proper bauble slot support.
 */
public class ItemAmuletVis extends Item {
    
    private final boolean isCrafted;
    
    public ItemAmuletVis(boolean crafted) {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(crafted ? Rarity.RARE : Rarity.UNCOMMON));
        this.isCrafted = crafted;
    }
    
    /**
     * Create the found variant (slower recharge).
     */
    public static ItemAmuletVis createFound() {
        return new ItemAmuletVis(false);
    }
    
    /**
     * Create the crafted variant (faster recharge).
     */
    public static ItemAmuletVis createCrafted() {
        return new ItemAmuletVis(true);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        
        // Only process server-side and for players
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        
        // Recharge interval: crafted = every 5 ticks, found = every 40 ticks
        int interval = isCrafted ? 5 : 40;
        if (player.tickCount % interval != 0) {
            return;
        }
        
        // TODO: Scan inventory and recharge IRechargable items
        // This will integrate with RechargeHelper once fully implemented
        // For now, the framework is in place
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.thaumcraft.amulet_vis.text")
                .withStyle(ChatFormatting.AQUA));
        
        if (isCrafted) {
            builder.accept(Component.translatable("item.thaumcraft.amulet_vis.crafted")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
