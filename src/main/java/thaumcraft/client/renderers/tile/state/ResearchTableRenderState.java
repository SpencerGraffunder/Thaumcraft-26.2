package thaumcraft.client.renderers.tile.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class ResearchTableRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean hasResearch;
    public boolean hasScribeTools;
}
