package thaumcraft.common.world.aura.pressure;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Per-level bookkeeping for in-flight flux pressure events (Thaumaturge
 * FluxPressureState equivalent): the single pending chunk trigger plus the
 * live rain pools and lightning bolts still ticking down.
 */
public final class FluxPressureState {

    private final List<FluxRain> rains = new ArrayList<>();
    private final List<FluxLightning> bolts = new ArrayList<>();
    @Nullable
    private BlockPos pending;

    public void queue(BlockPos chunkOrigin) {
        this.pending = chunkOrigin.immutable();
    }

    @Nullable
    public BlockPos pollPending() {
        BlockPos queued = this.pending;
        this.pending = null;
        return queued;
    }

    public void addRain(FluxRain rain) {
        this.rains.add(rain);
    }

    public void addLightning(FluxLightning bolt) {
        this.bolts.add(bolt);
    }

    public boolean hasRainNear(BlockPos pos, double rangeSq) {
        for (FluxRain rain : this.rains) {
            if (rain.center().distSqr(pos) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    public void tick(ServerLevel level) {
        if (!this.rains.isEmpty()) {
            this.rains.removeIf(rain -> !rain.tick(level));
        }
        if (!this.bolts.isEmpty()) {
            this.bolts.removeIf(bolt -> !bolt.tick(level));
        }
    }
}
