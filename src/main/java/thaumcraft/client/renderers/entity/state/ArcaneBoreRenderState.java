package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Render state for the Arcane Bore mining construct.
 * Carries the data needed to draw the rotating head and mining beam
 * without touching the entity during submit.
 */
@OnlyIn(Dist.CLIENT)
public class ArcaneBoreRenderState extends LivingEntityRenderState {
    /** Whether the bore is actively digging (beam should render). */
    public boolean digging;
    /** Lerped raw yaw (degrees) used to aim the mining beam. */
    public float yaw;
    /** Lerped raw pitch (degrees) used to aim the mining beam. */
    public float pitch;
    /** World game time, used to animate the beam spin. */
    public long gameTime;
}
