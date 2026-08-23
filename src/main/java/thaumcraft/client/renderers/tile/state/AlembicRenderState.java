package thaumcraft.client.renderers.tile.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import thaumcraft.api.aspects.Aspect;

public class AlembicRenderState extends BlockEntityRenderState {
    public Aspect aspectFilter;
    public int facing = 3;
    public Direction[] nozzles = new Direction[0];
}
