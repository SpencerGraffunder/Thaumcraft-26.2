package thaumcraft.client.renderers.tile.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class FocalManipulatorRenderState extends BlockEntityRenderState {
    public ItemStackRenderState focusItem = new ItemStackRenderState();
    public ItemStackRenderState[] crystals = new ItemStackRenderState[0];
    public int[] crystalColors = new int[0];
    public float ticks;
}
