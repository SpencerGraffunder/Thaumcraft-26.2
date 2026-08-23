package thaumcraft.client.renderers.entity.state;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;

public class GolemRenderState extends ArmedEntityRenderState {
    public Identifier texture;
    public boolean holdingItem;
    public boolean swinging;
    public float attackAnim;
}
