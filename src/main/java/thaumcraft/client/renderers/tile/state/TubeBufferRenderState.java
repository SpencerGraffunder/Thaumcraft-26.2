package thaumcraft.client.renderers.tile.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class TubeBufferRenderState extends BlockEntityRenderState {
    public byte[] chokedSides = new byte[6];
    public boolean[] showIndicator = new boolean[6];
}
