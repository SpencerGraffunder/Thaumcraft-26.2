package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CultistPortalRenderState extends EntityRenderState {
    public boolean active;      // isActive() (lesser portals may be inactive)
    public int activeCounter;
    public int hurtTime;
    public int pulse;
    public float healthPercent; // health / maxHealth (0 if maxHealth <= 0)
    public float height;        // getBbHeight()
}
