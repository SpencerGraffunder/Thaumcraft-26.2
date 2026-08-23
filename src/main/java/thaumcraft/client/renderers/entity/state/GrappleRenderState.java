package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GrappleRenderState extends EntityRenderState {
    public float yRot;
    public float xRot;
    public float ampl;
    public Vec3 ropeStart;
    public Vec3 ropeEnd;
}
