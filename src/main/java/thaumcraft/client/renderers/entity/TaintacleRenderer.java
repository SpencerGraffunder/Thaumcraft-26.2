package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.TaintacleModel;
import thaumcraft.client.renderers.entity.state.TaintacleRenderState;
import thaumcraft.common.entities.monster.tainted.EntityTaintacle;

/**
 * Renderer for Taintacles - tentacle-like taint creatures that emerge from the ground.
 */
@OnlyIn(Dist.CLIENT)
public class TaintacleRenderer extends MobRenderer<EntityTaintacle, TaintacleRenderState, TaintacleModel> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/taintacle.png");
    
    public TaintacleRenderer(EntityRendererProvider.Context context) {
        super(context, new TaintacleModel(context.bakeLayer(TaintacleModel.LAYER_LOCATION)), 0.3F);
    }
    
    @Override
    public Identifier getTextureLocation(TaintacleRenderState state) {
        return TEXTURE;
    }
    
    @Override
    public TaintacleRenderState createRenderState() {
        return new TaintacleRenderState();
    }
    
    @Override
    public void extractRenderState(EntityTaintacle entity, TaintacleRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.length = entity.getLength();
        state.flailIntensity = entity.flailIntensity;
        state.hurtTime = entity.hurtTime;
    }
    
    @Override
    protected void scale(TaintacleRenderState state, PoseStack poseStack) {
        // Scale based on taintacle length/size
        float scale = 0.8F + state.length * 0.1F;
        poseStack.scale(scale, scale, scale);
    }
}
