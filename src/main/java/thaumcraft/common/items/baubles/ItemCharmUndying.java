package thaumcraft.common.items.baubles;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Charm of Undying - A charm that prevents death once, similar to Totem of Undying.
 * When the player would die, this charm is consumed and the player is healed instead.
 * 
 * TODO: Add Curios integration for charm slot support.
 * TODO: Add death prevention event handler.
 */
public class ItemCharmUndying extends Item {
    
    public ItemCharmUndying() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }
    
    /**
     * Check if a player has the charm and can be saved from death.
     * Called from death event handler.
     * 
     * @param player The player who is dying
     * @return The charm ItemStack if found, null otherwise
     */
    public static ItemStack findCharm(net.minecraft.world.entity.player.Player player) {
        // Check main inventory
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemCharmUndying) {
                return stack;
            }
        }
        // Check offhand
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() instanceof ItemCharmUndying) {
            return offhand;
        }
        // TODO: Check Curios charm slot when integrated
        return null;
    }
    
    /**
     * Apply the undying effect and consume the charm.
     */
    public static void applyUndyingEffect(net.minecraft.world.entity.player.Player player, ItemStack charm) {
        // Heal the player
        player.setHealth(4.0f); // 2 hearts
        
        // Clear negative effects
        player.removeAllEffects();
        
        // Apply regeneration and absorption
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 900, 1));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 800, 0));
        
        // Consume the charm
        charm.shrink(1);
    }
}
