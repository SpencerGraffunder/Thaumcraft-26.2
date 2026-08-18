package thaumcraft.common.items.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;

/**
 * Void Metal Armor - High protection, self-repairing, but warping.
 */
public class ItemVoidArmor extends Item implements IWarpingGear {
    
    public ItemVoidArmor(ArmorType type) {
        super(new Item.Properties().humanoidArmor(ThaumcraftMaterials.ARMORMAT_VOID, type));
    
    }
    
    // Factory methods for different armor pieces
    public static ItemVoidArmor createHelmet() {
        return new ItemVoidArmor(ArmorType.HELMET);
    }
    
    public static ItemVoidArmor createChestplate() {
        return new ItemVoidArmor(ArmorType.CHESTPLATE);
    }
    
    public static ItemVoidArmor createLeggings() {
        return new ItemVoidArmor(ArmorType.LEGGINGS);
    }
    
    public static ItemVoidArmor createBoots() {
        return new ItemVoidArmor(ArmorType.BOOTS);
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.VOID_METAL_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // Self-repair: repair 1 durability every second (20 ticks) while worn
        if (stack.isDamaged() && entity != null && entity.tickCount % 20 == 0 && entity instanceof LivingEntity living) {
            // Only repair if actually worn
            for (ItemStack armorPiece : living.getArmorSlots()) {
                if (armorPiece == stack) {
                    stack.setDamageValue(stack.getDamageValue() - 1);
                    break;
                }
            }
        }
    }
    
    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // Use layer 1 for helmet/chest/boots, layer 2 for legs
        if (slot == EquipmentSlot.LEGS) {
            return "thaumcraft:textures/entity/armor/void_2.png";
        }
        return "thaumcraft:textures/entity/armor/void_1.png";
    }
    
    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 1;
    }
}
