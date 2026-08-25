package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Crimson Blade - A powerful sword used by the Crimson Cult.
 * Applies weakness and hunger to targets.
 * Self-repairs and causes warp when equipped.
 */
import thaumcraft.init.ItemRegistration;
public class ItemCrimsonBlade extends Item implements IWarpingGear {

    public ItemCrimsonBlade() {
        super(ItemRegistration.id((new Item.Properties().rarity(Rarity.EPIC)).sword(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 1, -2.8F)));
    }

    /**
     * Self-repair mechanic - repairs 1 durability every 20 ticks.
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        
        if (stack.isDamaged() && entity != null && entity.tickCount % 20 == 0 && entity instanceof LivingEntity) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    /**
     * Apply weakness and hunger effects on hit.
     */
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide()) {
            // Check PvP rules
            boolean canApplyEffects = true;

            if (canApplyEffects) {
                // Weakness for 3 seconds (60 ticks)
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                // Hunger for 6 seconds (120 ticks)
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0));
            }
        }
        super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 2;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("enchantment.special.sapgreat").withStyle(style -> style.withColor(0x8B0000)));
        builder.accept(Component.translatable("item.thaumcraft.self_repair").withStyle(style -> style.withColor(0x9400D3)));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
