package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EldritchOrbRenderState extends EntityRenderState {
    public float age;       // tickCount + partialTick
    public float scale;     // min(tickCount, 10) / 10
    public int frame;       // tickCount % 13
}
