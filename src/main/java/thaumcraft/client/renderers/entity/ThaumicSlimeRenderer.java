package thaumcraft.client.renderers.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.EntityThaumicSlime;

/**
 * Renderer for Thaumic Slimes - magical slimes with aspect-colored innards.
 * Uses the vanilla slime model with a custom texture and optional tinting.
 */
@OnlyIn(Dist.CLIENT)
public class ThaumicSlimeRenderer extends AbstractCubeMobRenderer<EntityThaumicSlime, SlimeRenderState, SlimeModel> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/thaumic_slime.png");
    
    public ThaumicSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel(context.bakeLayer(ModelLayers.SLIME)));
        this.addLayer(new SlimeOuterLayer(this, context.getModelSet()));
    }
    
    @Override
    public Identifier getTextureLocation(SlimeRenderState state) {
        return TEXTURE;
    }
    
    @Override
    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }
}
