package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
 * Pickaxe of the Core - Fire elemental pickaxe.
 * Sets entities on fire when hit.
 * Has built-in Refining and Sounding infusion enchantments.
 */
public class ItemElementalPickaxe extends Item {

    public ItemElementalPickaxe() {
        super((// attack speed
                new Item.Properties()
                        .rarity(Rarity.RARE)).pickaxe(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 1, -2.8F));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    /**
     * Sets entities on fire when hit with the pickaxe.
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide()) {
            // Check if PvP is enabled for player targets
            if (!(entity instanceof Player) || isPvPEnabled(player)) {
                entity.setSecondsOnFire(2);
            }
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    /**
     * Check if PvP is enabled on the server.
     */
    private boolean isPvPEnabled(Player player) {
        if (player.level().getServer() != null) {
            return player.level().getServer().isPvpAllowed();
        }
        return true; // Default to true if we can't check
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("enchantment.thaumcraft.refining").withStyle(style -> style.withColor(0xFFD700)));
        builder.accept(Component.translatable("enchantment.thaumcraft.sounding").withStyle(style -> style.withColor(0xFFD700)));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
