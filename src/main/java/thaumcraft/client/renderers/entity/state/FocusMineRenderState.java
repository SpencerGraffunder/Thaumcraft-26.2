package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FocusMineRenderState extends EntityRenderState {
    public float animAge;
    public boolean armed;
    public float yRot;
    public float xRot;
}
