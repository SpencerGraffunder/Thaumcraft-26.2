package thaumcraft.common.items.armor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;

import javax.annotation.Nullable;

/**
 * Boots of the Traveller - Magical boots that grant speed and step height.
 * Consumes vis charge while providing the movement bonus.
 */
public class ItemBootsTraveller extends Item implements IRechargable {
    
    public ItemBootsTraveller() {
        super((new Item.Properties()
                        .stacksTo(1)
                        .durability(350)
                        .rarity(Rarity.RARE)).humanoidArmor(ThaumcraftMaterials.ARMORMAT_SPECIAL, ArmorType.BOOTS));
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.LEATHER) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "thaumcraft:textures/entity/armor/bootstraveler.png";
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // Only process if worn on feet and entity is a player
        if (!(entity instanceof Player player)) return;
        
        // Check if boots are actually equipped
        ItemStack feetStack = player.getItemBySlot(EquipmentSlot.FEET);
        if (feetStack != stack) return;
        
        boolean hasCharge = RechargeHelper.getCharge(stack) > 0;
        
        // Energy management - consume charge periodically
        if (!level.isClientSide() && player.tickCount % 20 == 0) {
            CompoundTag tag = stack.getOrCreateTag();
            int energy = tag.getInt("energy");
            
            if (energy > 0) {
                energy--;
            } else if (energy <= 0 && RechargeHelper.consumeCharge(stack, player, 1)) {
                energy = 60; // Recharge internal energy buffer
            }
            
            tag.putInt("energy", energy);
        }
        
        // Apply movement bonuses if we have charge and player is moving forward
        if (hasCharge && !player.getAbilities().flying && player.zza > 0.0f) {
            // Increase step height when not sneaking
            if (level.isClientSide() && !player.isShiftKeyDown()) {
                // Step height is handled via attribute modifiers in 1.20.1
                // For now, we'll handle the movement boost
            }
            
            if (player.onGround()) {
                // Ground speed boost
                float bonus = 0.05f;
                if (player.isInWater()) {
                    bonus /= 4.0f;
                }
                
                // Apply forward movement boost
                Vec3 lookAngle = player.getLookAngle();
                Vec3 horizontalLook = new Vec3(lookAngle.x, 0, lookAngle.z).normalize();
                player.setDeltaMovement(player.getDeltaMovement().add(
                        horizontalLook.x * bonus,
                        0,
                        horizontalLook.z * bonus
                ));
            } else {
                // Air control boost
                if (player.isInWater()) {
                    Vec3 lookAngle = player.getLookAngle();
                    Vec3 horizontalLook = new Vec3(lookAngle.x, 0, lookAngle.z).normalize();
                    player.setDeltaMovement(player.getDeltaMovement().add(
                            horizontalLook.x * 0.025f,
                            0,
                            horizontalLook.z * 0.025f
                    ));
                }
                // Improved air control - add small forward boost while in air
                Vec3 lookAngle = player.getLookAngle();
                Vec3 horizontalLook = new Vec3(lookAngle.x, 0, lookAngle.z).normalize();
                player.setDeltaMovement(player.getDeltaMovement().add(
                        horizontalLook.x * 0.01f,
                        0,
                        horizontalLook.z * 0.01f
                ));
            }
        }
    }
    
    // ==================== IRechargable Implementation ====================
    
    @Override
    public int getMaxCharge(ItemStack stack, LivingEntity entity) {
        return 240;
    }
    
    @Override
    public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity entity) {
        return EnumChargeDisplay.PERIODIC;
    }
}
