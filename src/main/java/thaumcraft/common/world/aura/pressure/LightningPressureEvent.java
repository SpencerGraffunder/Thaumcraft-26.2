package thaumcraft.common.world.aura.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Saturated flux strikes lightning: it picks a living entity that can see the
 * sky (or the bare surface) near the origin and flashes a bolt there, then
 * registers the multi-flash bolt with the level state (Thaumaturge
 * LightningPressureEvent equivalent).
 */
public final class LightningPressureEvent extends AbstractFluxPressureEvent {

    private static final int WEIGHT = 5;
    private static final float COST = 25.0F;
    private static final boolean ALLOWED_NEAR_TAINT = false;
    private static final double TARGET_RANGE_XZ = 4.0;
    private static final double TARGET_RANGE_Y = 16.0;
    private static final int MIN_REFLASHES = 1;
    private static final int REFLASH_SPREAD = 3;

    public LightningPressureEvent() {
        super("lightning", WEIGHT, COST, ALLOWED_NEAR_TAINT);
    }

    @Override
    public boolean fire(ServerLevel level, BlockPos origin, FluxPressureState state) {
        BlockPos strike = findTarget(level, origin);
        if (!level.hasChunkAt(strike) || !level.canSeeSky(strike)) {
            return false;
        }
        RandomSource random = level.getRandom();
        FluxLightning.flash(level, strike, true);
        state.addLightning(new FluxLightning(strike, MIN_REFLASHES + random.nextInt(REFLASH_SPREAD), random));
        return true;
    }

    private static BlockPos findTarget(ServerLevel level, BlockPos origin) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, origin);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(surface).inflate(TARGET_RANGE_XZ, TARGET_RANGE_Y, TARGET_RANGE_XZ),
                entity -> entity.isAlive() && level.canSeeSky(entity.blockPosition()));
        if (entities.isEmpty()) {
            return surface;
        }
        return entities.get(level.getRandom().nextInt(entities.size())).blockPosition();
    }
}
