package thaumcraft.common.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Hoe - Magic-infused iron hoe with better stats.
 */
public class ItemThaumiumHoe extends Item {
    
    public ItemThaumiumHoe() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()).hoe(ThaumcraftMaterials.TOOLMAT_THAUMIUM, -2, -1.0F));
    }
}
