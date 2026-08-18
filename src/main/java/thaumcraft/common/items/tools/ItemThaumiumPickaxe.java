package thaumcraft.common.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Pickaxe - Magic-infused iron pickaxe with better stats.
 */
public class ItemThaumiumPickaxe extends Item {
    
    public ItemThaumiumPickaxe() {
        super((new Item.Properties()).pickaxe(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 1, -2.8F));
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
}
