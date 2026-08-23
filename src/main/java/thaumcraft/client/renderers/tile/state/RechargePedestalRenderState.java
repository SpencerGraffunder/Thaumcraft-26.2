package thaumcraft.client.renderers.tile.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class RechargePedestalRenderState extends BlockEntityRenderState {
    public ItemStackRenderState item = new ItemStackRenderState();
    public float ticks;
}
