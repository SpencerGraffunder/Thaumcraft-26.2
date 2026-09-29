package thaumcraft.common.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Axe - Magic-infused iron axe with better stats.
 */
public class ItemThaumiumAxe extends Item {
    
    public ItemThaumiumAxe() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()).axe(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 6.0F, -3.1F));
    }
}
