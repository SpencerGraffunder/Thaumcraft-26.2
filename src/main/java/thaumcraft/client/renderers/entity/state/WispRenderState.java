package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WispRenderState extends EntityRenderState {
    public int color = 0xFFFFFF;
    public boolean dead;
    public float animAge;   // tickCount + partialTick
    public int frame;       // (tickCount + (int)partialTick) % 16
}
