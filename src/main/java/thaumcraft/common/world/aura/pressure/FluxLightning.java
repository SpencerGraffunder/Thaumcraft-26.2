package thaumcraft.common.world.aura.pressure;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import thaumcraft.init.ModEffects;

/**
 * A flux lightning bolt: an initial flash at the strike point (with scattered
 * goo) plus a few re-flashes on a short delay. Each flash plays thunder,
 * spark particles, pools goo and hits nearby living entities for magic damage
 * + flux taint (Thaumaturge FluxLightning equivalent).
 */
public final class FluxLightning {

    private static final int MIN_DELAY = 4;
    private static final int DELAY_SPREAD = 5;
    private static final float THUNDER_VOLUME = 4.0F;
    private static final float THUNDER_PITCH = 0.9F;
    private static final float IMPACT_VOLUME = 2.0F;
    private static final float IMPACT_PITCH = 0.5F;
    private static final float PITCH_SPREAD = 0.2F;
    private static final int SPARK_COUNT = 24;
    private static final double SPARK_Y_OFFSET = 0.8;
    private static final double SPARK_SPREAD_XZ = 0.5;
    private static final double SPARK_SPREAD_Y = 1.0;
    private static final double SPARK_SPEED = 0.12;
    private static final int SCATTER_ATTEMPTS = 4;
    private static final int SCATTER_RANGE = 5;
    private static final double HIT_RADIUS = 3.0;
    private static final float HIT_DAMAGE = 3.0F;
    private static final int FLUX_TAINT_TICKS = 1200;

    private final BlockPos strike;
    private int remainingFlashes;
    private int delay;

    public FluxLightning(BlockPos strike, int remainingFlashes, RandomSource random) {
        this.strike = strike.immutable();
        this.remainingFlashes = remainingFlashes;
        this.delay = nextDelay(random);
    }

    public boolean tick(ServerLevel level) {
        if (--this.delay > 0) {
            return true;
        }
        flash(level, this.strike, false);
        if (--this.remainingFlashes <= 0) {
            return false;
        }
        this.delay = nextDelay(level.getRandom());
        return true;
    }

    private static int nextDelay(RandomSource random) {
        return MIN_DELAY + random.nextInt(DELAY_SPREAD);
    }

    public static void flash(ServerLevel level, BlockPos strike, boolean scatter) {
        RandomSource random = level.getRandom();
        level.playSound(null, strike, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                THUNDER_VOLUME, THUNDER_PITCH + random.nextFloat() * PITCH_SPREAD);
        level.playSound(null, strike, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER,
                IMPACT_VOLUME, IMPACT_PITCH + random.nextFloat() * PITCH_SPREAD);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                strike.getX() + 0.5, strike.getY() + SPARK_Y_OFFSET, strike.getZ() + 0.5,
                SPARK_COUNT, SPARK_SPREAD_XZ, SPARK_SPREAD_Y, SPARK_SPREAD_XZ, SPARK_SPEED);
        placeGoo(level, strike);
        if (scatter) {
            for (int attempt = 0; attempt < SCATTER_ATTEMPTS; attempt++) {
                int ox = random.nextInt(SCATTER_RANGE) - SCATTER_RANGE / 2;
                int oy = random.nextInt(SCATTER_RANGE) - SCATTER_RANGE / 2;
                int oz = random.nextInt(SCATTER_RANGE) - SCATTER_RANGE / 2;
                placeGoo(level, strike.offset(ox, oy, oz));
            }
        }
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(strike).inflate(HIT_RADIUS));
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().magic(), HIT_DAMAGE);
            target.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, FLUX_TAINT_TICKS, 0, false, true, false));
        }
    }

    private static void placeGoo(ServerLevel level, BlockPos target) {
        if (!level.hasChunkAt(target)) {
            return;
        }
        if (!FluxGooUtil.placeGoo(level, target)) {
            FluxGooUtil.placeGoo(level, target.above());
        }
    }
}
