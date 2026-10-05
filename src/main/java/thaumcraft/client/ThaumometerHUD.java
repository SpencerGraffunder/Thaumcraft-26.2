package thaumcraft.client;

import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-side holder for the thaumometer's current scan target.
 *
 * Matches 1.12's {@code RenderEventHandler.thaumTarget}: it is set every 5 ticks by
 * {@code ItemThaumometer.highlightScannables} (while the thaumometer is held) and read by
 * {@code ThaumometerTagRenderer} to draw the target's aspect tags above it in the world.
 *
 * @OnlyIn(Dist.CLIENT) because it is only ever touched on the logical client.
 */
@OnlyIn(Dist.CLIENT)
public final class ThaumometerHUD {

    /** The entity the local player is looking at while holding a thaumometer, or null. */
    public static Entity target;

    private ThaumometerHUD() {}
}
