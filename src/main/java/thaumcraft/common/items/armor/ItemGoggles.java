package thaumcraft.common.items.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;

/**
 * Goggles of Revealing - Allows the player to see aura information, 
 * essentia contents of blocks, and provides a small vis discount.
 * 
 * Can be worn as helmet armor or potentially as a Curios bauble.
 */
public class ItemGoggles extends Item implements IVisDiscountGear, IRevealer, IGoggles {
    
    public ItemGoggles() {
        super((new Item.Properties()
                        .stacksTo(1)
                        .durability(350)
                        .rarity(Rarity.RARE)).humanoidArmor(ThaumcraftMaterials.ARMORMAT_SPECIAL, ArmorType.HELMET));
    }
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 5;
    }
    
    @Override
    public boolean showNodes(ItemStack itemstack, LivingEntity player) {
        return true;
    }
    
    @Override
    public boolean showIngamePopups(ItemStack itemstack, LivingEntity player) {
        return true;
    }
    
    // TODO: Add Curios integration for wearing as bauble
    // This would require adding the Curios API as a dependency and implementing ICurioItem
}
