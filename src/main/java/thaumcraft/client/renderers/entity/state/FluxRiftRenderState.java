package thaumcraft.client.renderers.entity.state;

import java.util.List;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FluxRiftRenderState extends EntityRenderState {
    public List<Vec3> points = List.of();
    public List<Float> widths = List.of();
    public float stability;
    public float time;      // tickCount + partialTick
}
