package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Render state for the Eldritch Golem boss.
 * Carries the animation data the EldritchGolemModel needs during setupAnim
 * without touching the entity during submit.
 */
@OnlyIn(Dist.CLIENT)
public class EldritchGolemRenderState extends LivingEntityRenderState {
    /** Spawn timer (head bowed while spawning). */
    public int spawnTimer;
    /** Whether the golem is headless (head stump visible instead of head). */
    public boolean headless;
    /** Attack timer for the arm swing animation. */
    public int attackTimer;
}
