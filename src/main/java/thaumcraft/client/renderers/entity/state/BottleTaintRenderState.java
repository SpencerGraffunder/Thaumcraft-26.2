package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BottleTaintRenderState extends ThrownItemRenderState {
    public float yRot;
    public float spinAge;
}
