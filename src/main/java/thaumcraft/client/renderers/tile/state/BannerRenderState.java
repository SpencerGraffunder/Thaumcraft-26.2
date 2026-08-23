package thaumcraft.client.renderers.tile.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import thaumcraft.api.aspects.Aspect;

public class BannerRenderState extends BlockEntityRenderState {
    public boolean cultTexture;
    public boolean wall;
    public float bannerFacing;
    public int color = -1;
    public Aspect aspect;
    public float wind;
}
