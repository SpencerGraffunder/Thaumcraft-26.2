package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireBatRenderState extends EntityRenderState {
    public float animAge;   // tickCount + partialTick
    public float size;      // billboard quad size
}
