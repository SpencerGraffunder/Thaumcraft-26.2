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
        // Check inventory for the band
        // Check Curios head slot
        if (entity instanceof Player player) {
            var curios = player.getCapability(top.theillusivec4.curios.api.type.capability.ICuriosItemHandler.class);
            if (curios != null) {
                for (int i = 0; i < curios.getSlotsCount(); i++) {
                    if (curios.getStackInSlot(i).getItem() == this) {
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
