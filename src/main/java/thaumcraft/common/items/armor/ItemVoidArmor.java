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
import net.minecraft.server.level.ServerLevel;

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
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        // Self-repair: repair 1 durability every second (20 ticks) while worn
        if (stack.isDamaged() && entity != null && entity.tickCount % 20 == 0 && entity instanceof LivingEntity living) {
            // Only repair if actually worn
            for (net.minecraft.world.entity.EquipmentSlot s : new net.minecraft.world.entity.EquipmentSlot[]{
                    net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.HEAD}) {
                if (living.getItemBySlot(s) == stack) {
                    stack.setDamageValue(stack.getDamageValue() - 1);
                    break;
                }
            }
        }
    }
    
    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 1;
    }
}
