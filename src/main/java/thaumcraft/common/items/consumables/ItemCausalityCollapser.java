package thaumcraft.common.items.consumables;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Causality Collapser - A powerful throwable that creates a void implosion.
 * Extremely dangerous - destroys everything in the blast radius.
 */
public class ItemCausalityCollapser extends Item {

    public ItemCausalityCollapser() {
        super(new Item.Properties()
                .stacksTo(16)
                .rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Play throw sound
        player.playSound(SoundEvents.EGG_THROW, 0.3f,
                0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide()) {
            // TODO: Spawn EntityCausalityCollapser projectile
            // This creates a devastating void implosion where it lands
            // EntityCausalityCollapser proj = new EntityCausalityCollapser(level, player);
            // proj.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0f, 0.8f, 2.0f);
            // level.addFreshEntity(proj);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.causality_collapser.desc")
                .withStyle(style -> style.withColor(0x8B0000)));
        tooltip.add(Component.translatable("item.thaumcraft.causality_collapser.warning")
                .withStyle(style -> style.withColor(0xFF0000)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
