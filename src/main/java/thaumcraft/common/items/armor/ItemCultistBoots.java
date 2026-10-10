package thaumcraft.common.items.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;

/**
 * Crimson Cult Boots - Standard iron-quality boots worn by cultists.
 * Shared between robe and plate variants.
 * 1.12 parity: +1 vis discount and +1 warp per tick cycle while worn.
 */
import thaumcraft.init.ItemRegistration;
public class ItemCultistBoots extends Item implements IVisDiscountGear, IWarpingGear {
    
    public ItemCultistBoots() {
        super(ItemRegistration.id((new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.UNCOMMON)).humanoidArmor(ArmorMaterials.IRON, ArmorType.BOOTS)));
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 1;
    }

    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 1;
    }
}
