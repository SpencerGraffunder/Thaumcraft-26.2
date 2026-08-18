package thaumcraft.common.items.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;

/**
 * Thaumium Armor - Balanced magical armor, better than iron.
 */
public class ItemThaumiumArmor extends Item {
    
    public ItemThaumiumArmor(ArmorType type) {
        super(new Item.Properties().humanoidArmor(ThaumcraftMaterials.ARMORMAT_THAUMIUM, type));
    
    }
    
    // Factory methods for different armor pieces
    public static ItemThaumiumArmor createHelmet() {
        return new ItemThaumiumArmor(ArmorType.HELMET);
    }
    
    public static ItemThaumiumArmor createChestplate() {
        return new ItemThaumiumArmor(ArmorType.CHESTPLATE);
    }
    
    public static ItemThaumiumArmor createLeggings() {
        return new ItemThaumiumArmor(ArmorType.LEGGINGS);
    }
    
    public static ItemThaumiumArmor createBoots() {
        return new ItemThaumiumArmor(ArmorType.BOOTS);
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // Use layer 1 for helmet/chest/boots, layer 2 for legs
        if (slot == EquipmentSlot.LEGS) {
            return "thaumcraft:textures/entity/armor/thaumium_2.png";
        }
        return "thaumcraft:textures/entity/armor/thaumium_1.png";
    }
}
