package thaumcraft.api.potions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Vis Exhaustion potion effect.
 * Increases vis costs and slows vis regeneration.
 * The actual effect is handled by checking for this effect in vis-using code.
 * 
 * Ported from 1.12.2
 */
public class PotionVisExhaust extends MobEffect {
    
    public PotionVisExhaust() {
        super(MobEffectCategory.HARMFUL, 0x555577); // Gray-blue color
    }
    
    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity target, int amplifier) {
        // Effect is passive - checked by vis manipulation code
        return false;
    }
    
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false; // No periodic effect
    }
}
