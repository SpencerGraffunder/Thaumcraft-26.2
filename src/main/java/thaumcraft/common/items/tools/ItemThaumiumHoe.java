package thaumcraft.common.items.tools;

import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Hoe - Magic-infused iron hoe with better stats.
 */
public class ItemThaumiumHoe extends HoeItem {
    
    public ItemThaumiumHoe() {
        super(ThaumcraftMaterials.TOOLMAT_THAUMIUM, -2, -1.0F, thaumcraft.init.ItemRegistration.id(new Item.Properties()));
    }
}
