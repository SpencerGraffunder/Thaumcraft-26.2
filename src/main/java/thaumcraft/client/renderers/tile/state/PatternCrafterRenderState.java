package thaumcraft.client.renderers.tile.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class PatternCrafterRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public byte type;
    public float rot;
}
