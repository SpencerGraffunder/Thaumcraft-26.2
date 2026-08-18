package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.HoeItem;
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

/**
 * Elemental Hoe - Enhanced thaumium hoe with elemental power.
 * Has increased durability and tilling area.
 */
public class ItemElementalHoe extends HoeItem {

    public ItemElementalHoe() {
        super(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, -2, -1.0f, new Item.Properties()
                        .rarity(Rarity.RARE));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.elemental_hoe.desc").withStyle(style -> style.withColor(0x228B22)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
