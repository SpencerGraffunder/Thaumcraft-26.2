package thaumcraft.common.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Shovel - Magic-infused iron shovel with better stats.
 */
public class ItemThaumiumShovel extends ShovelItem {
    
    public ItemThaumiumShovel() {
        super(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 1.5F, -3.0F, new Item.Properties());
    }
}
