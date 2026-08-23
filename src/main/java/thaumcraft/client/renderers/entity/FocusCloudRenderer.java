package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.entities.projectile.EntityFocusCloud;

/**
 * Renderer for Focus Cloud entities.
 * The cloud effect is rendered purely through particles, so this renderer does nothing.
 */
@OnlyIn(Dist.CLIENT)
public class FocusCloudRenderer extends EntityRenderer<EntityFocusCloud, EntityRenderState> {
    
    public FocusCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
