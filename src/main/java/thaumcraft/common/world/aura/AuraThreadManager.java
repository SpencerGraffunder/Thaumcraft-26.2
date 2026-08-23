package thaumcraft.common.world.aura;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages AuraThread instances for all loaded dimensions.
 * 
 * This class handles:
 * - Starting aura threads when dimensions are loaded
 * - Stopping aura threads when dimensions are unloaded
 * - Stopping all threads on server shutdown
 */
public class AuraThreadManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuraThreadManager.class);

    /** Map of dimension to aura thread */
    private static final Map<ResourceKey<Level>, AuraThread> auraThreads = new ConcurrentHashMap<>();
    
    /** Map of dimension to actual Thread objects */
    private static final Map<ResourceKey<Level>, Thread> threads = new ConcurrentHashMap<>();

    /**
     * Start an aura thread for a dimension if one doesn't exist.
     * Called when a dimension is loaded and has aura data.
     */
    public static void startThread(ResourceKey<Level> dimension) {
        if (auraThreads.containsKey(dimension)) {
            // Thread already exists
            AuraThread existing = auraThreads.get(dimension);
            if (!existing.isStopped()) {
                return;
            }
            // Old thread is stopped, remove it
            removeThread(dimension);
        }
        
        // Only start if there's aura data for this dimension
        if (AuraHandler.getAuraWorld(dimension) == null) {
            LOGGER.debug("Not starting aura thread for {} - no aura world exists", dimension.identifier());
            return;
        }
        
        LOGGER.info("Starting aura thread for dimension {}", dimension.identifier());
        
        AuraThread auraThread = new AuraThread(dimension);
        Thread thread = new Thread(auraThread, "Thaumcraft-Aura-" + dimension.identifier().toString().replace(':', '-'));
        thread.setDaemon(true);
        thread.start();
        
        auraThreads.put(dimension, auraThread);
        threads.put(dimension, thread);
    }

    /**
     * Stop the aura thread for a dimension.
     * Called when a dimension is unloaded.
     */
    public static void stopThread(ResourceKey<Level> dimension) {
        AuraThread auraThread = auraThreads.get(dimension);
        if (auraThread != null) {
            LOGGER.info("Stopping aura thread for dimension {}", dimension.identifier());
            auraThread.stop();
        }
        
        Thread thread = threads.get(dimension);
        if (thread != null) {
            try {
                thread.interrupt();
                thread.join(2000); // Wait up to 2 seconds for thread to finish
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for aura thread to stop");
            }
        }
        
        removeThread(dimension);
    }

    /**
     * Remove a thread from the registry without stopping it.
     * Called by the thread itself when it stops.
     */
    public static void removeThread(ResourceKey<Level> dimension) {
        auraThreads.remove(dimension);
        threads.remove(dimension);
    }

    /**
     * Stop all aura threads.
     * Called on server shutdown.
     */
    public static void stopAllThreads() {
        LOGGER.info("Stopping all aura threads ({} active)", auraThreads.size());
        
        // Stop all threads
        for (AuraThread auraThread : auraThreads.values()) {
            auraThread.stop();
        }
        
        // Interrupt and wait for all threads
        for (Thread thread : threads.values()) {
            try {
                thread.interrupt();
            } catch (Exception e) {
                // Ignore
            }
        }
        
        for (Map.Entry<ResourceKey<Level>, Thread> entry : threads.entrySet()) {
            try {
                entry.getValue().join(1000);
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for aura thread {} to stop", entry.getKey().identifier());
            }
        }
        
        auraThreads.clear();
        threads.clear();
    }

    /**
     * Check if a dimension has an active aura thread.
     */
    public static boolean hasThread(ResourceKey<Level> dimension) {
        AuraThread thread = auraThreads.get(dimension);
        return thread != null && !thread.isStopped();
    }

    /**
     * Get the aura thread for a dimension.
     */
    public static AuraThread getThread(ResourceKey<Level> dimension) {
        return auraThreads.get(dimension);
    }

    /**
     * Called when a level is loaded.
     * Starts aura thread if aura data exists.
     */
    public static void onLevelLoad(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        
        // Ensure aura world exists
        AuraHandler.addAuraWorld(dimension);
        
        // Start aura thread
        startThread(dimension);
    }

    /**
     * Called when a level is unloaded.
     * Stops the aura thread for that dimension.
     */
    public static void onLevelUnload(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        stopThread(dimension);
        AuraHandler.removeAuraWorld(dimension);
    }
}
