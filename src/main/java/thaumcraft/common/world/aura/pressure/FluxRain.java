package thaumcraft.common.world.aura.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * A flux rain pool: for a bounded lifetime it drifts witch particles and,
 * every {@link #POOL_INTERVAL} ticks, tries to pool a goo blob on the surface
 * within a {@link #RADIUS} square, draining flux to do so (Thaumaturge
 * FluxRain equivalent).
 */
public final class FluxRain {

    private static final int RADIUS = 16;
    private static final int PARTICLE_INTERVAL = 5;
    private static final int PARTICLE_COUNT = 6;
    private static final double PARTICLE_Y_OFFSET = 0.25;
    private static final double PARTICLE_SPREAD_XZ = 12.0;
    private static final double PARTICLE_SPREAD_Y = 2.0;
    private static final double PARTICLE_SPEED = 0.02;
    private static final int POOL_INTERVAL = 20;
    private static final float POOL_FLUX_COST = 1.0F;
    private static final float POOL_FLUX_EPSILON = 0.001F;
    private static final int STARVED_TICK_PENALTY = 20;

    private final BlockPos center;
    private int remainingTicks;

    public FluxRain(BlockPos center, int lifespan) {
        this.center = center.immutable();
        this.remainingTicks = lifespan;
    }

    public BlockPos center() {
        return this.center;
    }

    public boolean tick(ServerLevel level) {
        if (--this.remainingTicks <= 0) {
            return false;
        }
        if (this.remainingTicks % PARTICLE_INTERVAL == 0) {
            level.sendParticles(ParticleTypes.WITCH,
                    this.center.getX() + 0.5, this.center.getY() + PARTICLE_Y_OFFSET, this.center.getZ() + 0.5,
                    PARTICLE_COUNT, PARTICLE_SPREAD_XZ, PARTICLE_SPREAD_Y, PARTICLE_SPREAD_XZ, PARTICLE_SPEED);
        }
        if (this.remainingTicks % POOL_INTERVAL == 0) {
            pool(level, level.getRandom());
        }
        return true;
    }

    private void pool(ServerLevel level, RandomSource random) {
        int x = this.center.getX() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
        int z = this.center.getZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
        BlockPos cursor = new BlockPos(x, this.center.getY(), z);
        if (!level.hasChunkAt(cursor)) {
            return;
        }
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, cursor);
        BlockPos target = level.getBlockState(surface).canBeReplaced() ? surface : surface.above();
        if (TaintHelper.isNearTaintSeed(level, target)) {
            return;
        }
        if (AuraHandler.drainFlux(level, target, POOL_FLUX_COST, true) + POOL_FLUX_EPSILON < POOL_FLUX_COST) {
            // Starved of flux: shorten the rain's life.
            this.remainingTicks = Math.max(0, this.remainingTicks - STARVED_TICK_PENALTY);
            return;
        }
        if (FluxGooUtil.placeGoo(level, target)) {
            AuraHandler.drainFlux(level, target, POOL_FLUX_COST, false);
        }
    }
}
