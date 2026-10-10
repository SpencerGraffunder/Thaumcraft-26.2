package thaumcraft.common.world.aura.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import thaumcraft.init.ModEffects;

import java.util.List;

/**
 * Saturated flux exhausts nearby living entities with infectious vis
 * exhaustion (Thaumaturge ExhaustPressureEvent equivalent).
 */
public final class ExhaustPressureEvent extends AbstractFluxPressureEvent {

    private static final int WEIGHT = 5;
    private static final float COST = 15.0F;
    private static final boolean ALLOWED_NEAR_TAINT = true;
    private static final double RANGE = 16.0;
    private static final int DURATION = 3000;
    private static final int AMPLIFIER = 2;

    public ExhaustPressureEvent() {
        super("exhaust", WEIGHT, COST, ALLOWED_NEAR_TAINT);
    }

    @Override
    public boolean fire(ServerLevel level, BlockPos origin, FluxPressureState state) {
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(origin).inflate(RANGE));
        if (targets.isEmpty()) {
            return false;
        }
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.INFECTIOUS_VIS_EXHAUST, DURATION, AMPLIFIER, false, true, false));
        }
        return true;
    }
}
