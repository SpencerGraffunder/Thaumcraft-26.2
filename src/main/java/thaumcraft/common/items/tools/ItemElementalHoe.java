package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Elemental Hoe - Enhanced thaumium hoe with elemental power.
 * Has increased durability and tilling area.
 */
public class ItemElementalHoe extends Item {

    public ItemElementalHoe() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties().rarity(Rarity.RARE).hoe(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, -2, -1.0f)));
    }

}
