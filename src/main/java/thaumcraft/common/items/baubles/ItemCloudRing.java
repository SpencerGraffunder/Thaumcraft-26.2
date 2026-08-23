package thaumcraft.common.items.baubles;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/**
 * Cloud Ring - A ring that allows double-jumping while in the air.
 * When the player presses jump while airborne, they get a boost upward.
 * 
 * TODO: Add Curios integration for ring slot support.
 */
public class ItemCloudRing extends Item {
    
    // Track which players have used their double jump
    private static final HashMap<UUID, Boolean> jumpUsed = new HashMap<>();
    
    public ItemCloudRing() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }
    
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        
        if (!(entity instanceof Player player)) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Reset jump when on ground
        if (player.onGround()) {
            jumpUsed.remove(playerId);
            return;
        }
        
        // The actual double-jump logic would need client-side input handling
        // For now, we provide a passive slow-fall effect when falling
        if (!level.isClientSide() && !player.onGround() && !player.isInWater()) {
            Vec3 motion = player.getDeltaMovement();
            
            // If falling, reduce fall speed
            if (motion.y < -0.1) {
                // Apply slow fall effect
                player.setDeltaMovement(motion.x, Math.max(motion.y, -0.4), motion.z);
                player.fallDistance = Math.max(0, player.fallDistance - 0.5f);
            }
        }
    }
    
    /**
     * Called when player attempts to double-jump (from key input handler).
     * This would be triggered by client-side input detection.
     */
    public static boolean tryDoubleJump(Player player) {
        UUID playerId = player.getUUID();
        
        if (!player.onGround() && !player.isInWater() && !jumpUsed.getOrDefault(playerId, false)) {
            jumpUsed.put(playerId, true);
            
            // Apply upward boost
            double boost = 0.75;
            if (player.hasEffect(MobEffects.JUMP_BOOST)) {
                boost += (player.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1;
            }
            
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, boost, motion.z);
            
            // Forward boost if sprinting
            if (player.isSprinting()) {
                float yaw = player.getYRot() * 0.017453292f;
                player.setDeltaMovement(
                        player.getDeltaMovement().add(
                                -Math.sin(yaw) * 0.2,
                                0,
                                Math.cos(yaw) * 0.2
                        )
                );
            }
            
            player.fallDistance = 0.0f;
            
            return true;
        }
        return false;
    }
}
