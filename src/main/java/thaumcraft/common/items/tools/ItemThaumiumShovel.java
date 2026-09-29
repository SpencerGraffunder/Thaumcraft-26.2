package thaumcraft.common.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Shovel - Magic-infused iron shovel with better stats.
 */
public class ItemThaumiumShovel extends Item {
    
    public ItemThaumiumShovel() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()).shovel(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 1.5F, -3.0F));
    }
}
