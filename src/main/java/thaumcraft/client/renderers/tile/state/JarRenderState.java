package thaumcraft.client.renderers.tile.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import thaumcraft.api.aspects.Aspect;

public class JarRenderState extends BlockEntityRenderState {
    public int amount;
    public Aspect aspect;
    public Aspect aspectFilter;
    public int facing = 3;
}
