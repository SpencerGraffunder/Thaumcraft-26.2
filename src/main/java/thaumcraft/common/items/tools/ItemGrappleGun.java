package thaumcraft.common.items.tools;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Grapple Gun - A vis-powered grappling hook launcher.
 * Fires a grapple that pulls the player towards where it hits.
 * Requires vis to fire.
 */
public class ItemGrappleGun extends Item implements IRechargable {

    public ItemGrappleGun() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public int getMaxCharge(ItemStack stack, LivingEntity entity) {
        return 100;
    }

    @Override
    public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity entity) {
        return EnumChargeDisplay.NORMAL;
    }

    /**
     * Check if grapple is currently deployed.
     */
    public boolean isLoaded(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag != null) {
            return tag.getByteOr("loaded", (byte)0) == 1;
        }
        return false;
    }

    /**
     * Set the loaded state.
     */
    public void setLoaded(ItemStack stack, boolean loaded) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putByte("loaded", (byte) (loaded ? 1 : 0));
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        // Reset loaded state if grapple entity is gone
        // TODO: Check grapple entity tracking
        // For now, just clear after some time
        if (isLoaded(stack) && !level.isClientSide()) {
            // The grapple system would need entity tracking to work properly
            // This is a placeholder that clears the loaded state
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Play sound
        player.playSound(SoundEvents.GLASS_BREAK, 3.0f, 0.8f + level.getRandom().nextFloat() * 0.1f);

        if (!level.isClientSide() && RechargeHelper.getCharge(stack) > 0) {
            // TODO: Spawn EntityGrapple projectile
            // EntityGrapple grapple = new EntityGrapple(level, player, hand);
            // grapple.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0f, 1.5f, 0.0f);
            // Adjust position based on hand
            // level.addFreshEntity(grapple);
            
            // Consume charge and mark as loaded
            RechargeHelper.consumeCharge(stack, player, 1);
            setLoaded(stack, true);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (oldStack.getItem() == this && newStack.getItem() == this) {
            boolean wasLoaded = isLoaded(oldStack);
            boolean isLoaded = isLoaded(newStack);
            return wasLoaded != isLoaded;
        }
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.thaumcraft.grapple_gun.desc").withStyle(style -> style.withColor(0x808080)));
        if (isLoaded(stack)) {
            builder.accept(Component.translatable("item.thaumcraft.grapple_gun.loaded").withStyle(style -> style.withColor(0x00FF00)));
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
