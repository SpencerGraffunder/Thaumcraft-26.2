package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Fiery Champion Modifier - Champion mobs that set targets on fire.
 * 
 * Effects:
 * - Sets targets on fire for 5 seconds on hit
 * - Shows flame particles
 * 
 * Type: 1 (Offensive)
 */
public class ChampionModFire implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Set target on fire
        if (target != null && !target.fireImmune()) {
            target.igniteForSeconds(5.0F);
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().getRandom().nextInt(2) != 0) {
            return;
        }
        
        // Flame particles
        double w = champion.level().getRandom().nextFloat() * champion.getBbWidth();
        double d = champion.level().getRandom().nextFloat() * champion.getBbWidth();
        double h = champion.level().getRandom().nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ParticleTypes.FLAME,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0.02, 0);
    }
}
