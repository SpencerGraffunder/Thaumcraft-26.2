package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Sanity Checker - Shows the player their current warp levels.
 * Right-click to check your sanity status.
 */
public class ItemSanityChecker extends Item {

    public ItemSanityChecker() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
            if (warp != null) {
                int permanent = warp.get(IPlayerWarp.EnumWarpType.PERMANENT);
                int normal = warp.get(IPlayerWarp.EnumWarpType.NORMAL);
                int temporary = warp.get(IPlayerWarp.EnumWarpType.TEMPORARY);
                int total = permanent + normal + temporary;

                player.sendSystemMessage(Component.translatable("tc.sanity.header"));
                player.sendSystemMessage(Component.translatable("tc.sanity.permanent", permanent));
                player.sendSystemMessage(Component.translatable("tc.sanity.normal", normal));
                player.sendSystemMessage(Component.translatable("tc.sanity.temporary", temporary));
                player.sendSystemMessage(Component.translatable("tc.sanity.total", total));

                // Give a hint about sanity level
                if (total == 0) {
                    player.sendSystemMessage(Component.translatable("tc.sanity.pure"));
                } else if (total < 25) {
                    player.sendSystemMessage(Component.translatable("tc.sanity.low"));
                } else if (total < 50) {
                    player.sendSystemMessage(Component.translatable("tc.sanity.medium"));
                } else if (total < 100) {
                    player.sendSystemMessage(Component.translatable("tc.sanity.high"));
                } else {
                    player.sendSystemMessage(Component.translatable("tc.sanity.extreme"));
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.sanity_checker.desc").withStyle(style -> style.withColor(0x808080)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
