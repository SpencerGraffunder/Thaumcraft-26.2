package thaumcraft.api.golems.parts;

import net.minecraft.world.entity.Entity;

/**
 * Base interface for golem part functions.
 * Extended by specific part function interfaces (IHeadFunction, IArmFunction, etc.).
 */
public interface IGenericFunction {
    /**
     * Called every tick for the golem entity that owns this part.
     * Implementations should be lightweight; heavy work should be deferred.
     */
    default void onUpdateTick(Entity entity) {}
}
