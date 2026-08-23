package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TaintSwarmRenderState extends EntityRenderState {
    public float time;      // tickCount + partialTick
    public int seed;        // entity.getId(), seeds the per-particle Random
}
