package thaumcraft.common.items.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;

import javax.annotation.Nullable;

/**
 * Crimson Cult Robe Armor - Worn by members of the Crimson Cult.
 * Provides small vis discount but causes warp.
 */
public class ItemCultistRobeArmor extends Item implements IVisDiscountGear, IWarpingGear {
    
    public ItemCultistRobeArmor(ArmorType type) {
        super(new Item.Properties().humanoidArmor(ThaumcraftMaterials.ARMORMAT_CULTIST_ROBE, type));
    
    }
    
    // Factory methods
    public static ItemCultistRobeArmor createHelmet() {
        return new ItemCultistRobeArmor(ArmorType.HELMET);
    }
    
    public static ItemCultistRobeArmor createChestplate() {
        return new ItemCultistRobeArmor(ArmorType.CHESTPLATE);
    }
    
    public static ItemCultistRobeArmor createLeggings() {
        return new ItemCultistRobeArmor(ArmorType.LEGGINGS);
    }
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 1; // Small discount
    }
    
    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 1; // Causes warp
    }
}
