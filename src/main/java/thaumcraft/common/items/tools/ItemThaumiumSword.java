package thaumcraft.common.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Sword - Magic-infused iron sword with better stats.
 */
import thaumcraft.init.ItemRegistration;
public class ItemThaumiumSword extends Item {
    
    public ItemThaumiumSword() {
        super(ItemRegistration.id((new Item.Properties()).sword(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 3, -2.4F)));
    }
}
