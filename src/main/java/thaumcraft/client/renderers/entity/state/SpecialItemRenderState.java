package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpecialItemRenderState extends ThrownItemRenderState {
    public float age;
    public int ageInt;
    public float bobOffs;
}
