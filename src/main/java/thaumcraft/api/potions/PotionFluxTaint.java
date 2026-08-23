package thaumcraft.api.potions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.api.entities.ITaintedMob;

/**
 * Flux Taint potion effect.
 * Damages non-tainted entities while healing tainted ones.
 * 
 * Ported from 1.12.2
 * API changes:
 * - Potion -> MobEffect
 * - Constructor takes MobEffectCategory instead of boolean
 * - performEffect -> applyEffectTick
 * - isReady -> isDurationEffectTick
 * - target.isEntityUndead() -> target.isInvertedHealAndHarm()
 */
public class PotionFluxTaint extends MobEffect {
    
    public PotionFluxTaint() {
        super(MobEffectCategory.HARMFUL, 0x6600AA); // Purple color
    }
    
    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity target, int amplifier) {
        if (serverLevel.isClientSide()) return false;
        
        // Check if entity is tainted (heals them)
        if (target instanceof ITaintedMob) {
            target.heal(1);
            return true;
        }
        
        // TODO: Check for Champion mod attribute when implemented
        // IAttributeInstance cai = target.getAttribute(ThaumcraftApiHelper.CHAMPION_MOD);
        // if (cai != null && (int) cai.getValue() == 13) {
        //     target.heal(1);
        //     return;
        // }
        
        // Damage non-undead entities
        if (!target.isInvertedHealAndHarm()) {
            // Players always take damage, other entities need more than 1 max health
            if (target instanceof Player || target.getMaxHealth() > 1) {
                target.hurtServer(serverLevel, DamageSourceThaumcraft.createTaint(serverLevel), 1);
            }
        }
        return true;
    }
    
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int k = 40 >> amplifier;
        return k > 0 ? duration % k == 0 : true;
    }
}
