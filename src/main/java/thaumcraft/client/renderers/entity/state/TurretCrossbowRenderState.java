package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Render state shared by both crossbow turrets (basic and advanced).
 * Carries the animation data the crossbow models need during setupAnim
 * without touching the entity during submit.
 */
@OnlyIn(Dist.CLIENT)
public class TurretCrossbowRenderState extends LivingEntityRenderState {
    /** Interpolated load progress (0..1) for the loading mechanism. */
    public float loadProgress;
    /** Interpolated attack/swing animation value for the bow arms. */
    public float attackAnim;
    /** Whether the turret is riding a minecart (leg pose adjustment). */
    public boolean passenger;
    /** Hurt jiggle offset, computed once per frame in extract. */
    public float jiggleX;
    public float jiggleY;
    public float jiggleZ;
}
