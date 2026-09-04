package thaumcraft.common.world.aura;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the per-dimension aura world lifecycle.
 *
 * The aura simulation itself runs on the server thread via {@link AuraScheduler}
 * (every 20 ticks per dimension). MC 26.2 enforces thread affinity on level and
 * chunk data, so the 1.12-era background "aura thread" design is no longer usable.
 */
public class AuraManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuraManager.class);

    /**
     * Called when a level is loaded.
     * Ensures the aura world exists for the dimension.
     */
    public static void onLevelLoad(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        AuraHandler.addAuraWorld(dimension);
        LOGGER.debug("Aura world ready for dimension {}", dimension.identifier());
    }

    /**
     * Called when a level is unloaded.
     * Removes the aura world and clears per-dimension simulation state.
     */
    public static void onLevelUnload(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        AuraHandler.removeAuraWorld(dimension);
        AuraScheduler.resetDimension(dimension);
        LOGGER.debug("Aura world removed for dimension {}", dimension.identifier());
    }

    /**
     * Called on server shutdown. Clears all aura simulation state.
     */
    public static void stopAll() {
        AuraScheduler.resetAll();
        LOGGER.debug("Aura simulation state cleared");
    }
}
