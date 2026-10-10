package thaumcraft.common.world.aura.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Saturated flux summons a flux rain pool high above the surface, which then
 * drifts and pools goo over 30-40 seconds (Thaumaturge RainPressureEvent
 * equivalent).
 */
public final class RainPressureEvent extends AbstractFluxPressureEvent {

    private static final int WEIGHT = 1;
    private static final float COST = 40.0F;
    private static final boolean ALLOWED_NEAR_TAINT = false;
    private static final int HEIGHT_ABOVE_SURFACE = 20;
    private static final int MIN_SECONDS = 30;
    private static final int SECONDS_SPREAD = 10;
    private static final int TICKS_PER_SECOND = 20;

    public RainPressureEvent() {
        super("rain", WEIGHT, COST, ALLOWED_NEAR_TAINT);
    }

    @Override
    public boolean fire(ServerLevel level, BlockPos origin, FluxPressureState state) {
        BlockPos center = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, origin)
                .above(HEIGHT_ABOVE_SURFACE);
        if (center.getY() > level.getMaxY()) {
            return false;
        }
        int lifespan = (MIN_SECONDS + level.getRandom().nextInt(SECONDS_SPREAD)) * TICKS_PER_SECOND;
        state.addRain(new FluxRain(center, lifespan));
        return true;
    }
}
