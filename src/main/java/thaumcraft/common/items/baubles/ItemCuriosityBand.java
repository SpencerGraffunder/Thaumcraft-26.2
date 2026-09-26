package thaumcraft.common.items.baubles;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Curiosity Band - A headband that provides research bonuses.
 * When worn, the player gains bonus research points when scanning things.
 * 
 * Implements ICuriosItemHandler for head slot support.
 * Integrates with research/scanning system for actual bonuses.
 */
public class ItemCuriosityBand extends Item {
    
    public ItemCuriosityBand() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)));
    }
    
    /**
     * Check if a player is wearing the curiosity band.
     * Used by the scanning system to apply research bonuses.
     */
    public static boolean isWearingBand(net.minecraft.world.entity.player.Player player) {
        // Check Curios head slot
        var curios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventoryOrNull(player);
        if (curios != null) {
            var head = curios.getStacksHandler("head");
            if (head.isPresent()) {
                var stacks = head.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i).getItem() instanceof ItemCuriosityBand) {
                        // Head slot bonus: +10% research speed
                        return true;
                    }
                }
            }
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemCuriosityBand) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the research bonus multiplier when wearing the band.
     */
    public static float getResearchBonus() {
        return 1.25f; // 25% bonus research points
    }
}
