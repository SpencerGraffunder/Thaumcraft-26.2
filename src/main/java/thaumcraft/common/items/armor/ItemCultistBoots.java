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

/**
 * Crimson Cult Boots - Standard iron-quality boots worn by cultists.
 * Shared between robe and plate variants.
 */
public class ItemCultistBoots extends Item {
    
    public ItemCultistBoots() {
        super((new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.UNCOMMON)).humanoidArmor(ArmorMaterials.IRON, ArmorType.BOOTS));
    }
}
