package thaumcraft.common.world.aura.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * A flux pressure event — something the aura does when a chunk's flux is
 * saturated (Thaumaturge parity, content/aura/pressure).
 */
public interface FluxPressureEvent {
    String name();

    int weight();

    float cost();

    /** Whether the event may fire near taint seeds (rain/lightning refuse). */
    boolean allowedNearTaint();

    /**
     * Fire the event. Return false to abort (and refund nothing — the caller
     * checks the flux drain simulation first).
     */
    boolean fire(ServerLevel level, BlockPos origin, FluxPressureState state);
}
