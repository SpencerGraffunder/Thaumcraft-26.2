package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Renderer for projectiles that should be invisible.
 * The visual effects come from particles spawned by the entity itself.
 * Used for: EntityFocusProjectile, EntityAlumentum, EntityCausalityCollapser
 */
@OnlyIn(Dist.CLIENT)
public class NoProjectileRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState> {
    
    public NoProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
