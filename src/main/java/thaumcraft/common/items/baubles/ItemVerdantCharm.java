package thaumcraft.common.items.baubles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.init.ModEffects;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Verdant Charm - A charm that provides various life-sustaining benefits.
 * Comes in three variants:
 * - Basic: Removes poison and wither effects
 * - Life: Also slowly regenerates health
 * - Sustain: Also provides air and food
 * 
 * Uses vis charge to power its effects.
 * 
 * TODO: Add Curios integration for charm slot support.
 */
public class ItemVerdantCharm extends Item implements IRechargable {
    
    public enum CharmType {
        BASIC(0),
        LIFE(1),
        SUSTAIN(2);
        
        public final int id;
        CharmType(int id) { this.id = id; }
        
        public static CharmType fromId(int id) {
            return switch (id) {
                case 1 -> LIFE;
                case 2 -> SUSTAIN;
                default -> BASIC;
            };
        }
    }
    
    private final CharmType type;
    
    public ItemVerdantCharm(CharmType type) {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
        this.type = type;
    }
    
    public static ItemVerdantCharm createBasic() {
        return new ItemVerdantCharm(CharmType.BASIC);
    }
    
    public static ItemVerdantCharm createLife() {
        return new ItemVerdantCharm(CharmType.LIFE);
    }
    
    public static ItemVerdantCharm createSustain() {
        return new ItemVerdantCharm(CharmType.SUSTAIN);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        
        if (player.tickCount % 20 != 0) {
            return;
        }
        
        // Remove wither effect (costs 20 charge)
        if (player.hasEffect(MobEffects.WITHER) && RechargeHelper.consumeCharge(stack, player, 20)) {
            player.removeEffect(MobEffects.WITHER);
            return;
        }
        
        // Remove poison effect (costs 10 charge)
        if (player.hasEffect(MobEffects.POISON) && RechargeHelper.consumeCharge(stack, player, 10)) {
            player.removeEffect(MobEffects.POISON);
            return;
        }
        
        // Remove flux taint effect (costs 5 charge)
        if (player.hasEffect(ModEffects.FLUX_TAINT) && RechargeHelper.consumeCharge(stack, player, 5)) {
            player.removeEffect(ModEffects.FLUX_TAINT);
            return;
        }
        
        // Life variant: regenerate health
        if (type == CharmType.LIFE) {
            if (player.getHealth() < player.getMaxHealth() && RechargeHelper.consumeCharge(stack, player, 5)) {
                player.heal(1.0f);
                return;
            }
        }
        
        // Sustain variant: provide air and food
        if (type == CharmType.SUSTAIN) {
            // Restore air
            if (player.getAirSupply() < 100 && RechargeHelper.consumeCharge(stack, player, 1)) {
                player.setAirSupply(300);
                return;
            }
            // Restore food
            if (player.canEat(false) && RechargeHelper.consumeCharge(stack, player, 1)) {
                player.getFoodData().eat(1, 0.3f);
            }
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        switch (type) {
            case LIFE -> tooltip.add(Component.translatable("item.thaumcraft.verdant_charm.life.text")
                    .withStyle(ChatFormatting.GOLD));
            case SUSTAIN -> tooltip.add(Component.translatable("item.thaumcraft.verdant_charm.sustain.text")
                    .withStyle(ChatFormatting.GOLD));
            default -> tooltip.add(Component.translatable("item.thaumcraft.verdant_charm.text")
                    .withStyle(ChatFormatting.GREEN));
        }
    }
    
    @Override
    public int getMaxCharge(ItemStack stack, LivingEntity entity) {
        return 200;
    }
    
    @Override
    public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity entity) {
        return EnumChargeDisplay.NORMAL;
    }
}
