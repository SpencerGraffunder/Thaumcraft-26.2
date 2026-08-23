package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.EldritchGolemModel;
import thaumcraft.client.renderers.entity.state.EldritchGolemRenderState;
import thaumcraft.common.entities.monster.boss.EntityEldritchGolem;

/**
 * Renderer for Eldritch Golem - a large armored boss entity.
 * Renders with transparency/blending for eldritch effect.
 */
@OnlyIn(Dist.CLIENT)
public class EldritchGolemRenderer extends MobRenderer<EntityEldritchGolem, EldritchGolemRenderState, EldritchGolemModel> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/eldritch_golem.png");
    
    public EldritchGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new EldritchGolemModel(context.bakeLayer(EldritchGolemModel.LAYER_LOCATION)), 0.7F);
    }
    
    @Override
    public Identifier getTextureLocation(EldritchGolemRenderState state) {
        return TEXTURE;
    }
    
    @Override
    public EldritchGolemRenderState createRenderState() {
        return new EldritchGolemRenderState();
    }
    
    @Override
    public void extractRenderState(EntityEldritchGolem entity, EldritchGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.spawnTimer = entity.getSpawnTimer();
        state.headless = entity.isHeadless();
        state.attackTimer = entity.getAttackTimer();
    }
    
    @Override
    protected RenderType getRenderType(EldritchGolemRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        // Render with translucent blending for the eldritch effect
        return RenderTypes.entityTranslucent(this.getTextureLocation(state));
    }
    
    @Override
    protected int getModelTint(EldritchGolemRenderState state) {
        // Slight transparency (alpha ~0.9) for the eldritch look
        return ARGB.color((int)(0.9F * 255.0F), 255, 255, 255);
    }
    
    @Override
    protected void scale(EldritchGolemRenderState state, PoseStack poseStack) {
        // Eldritch golem is 70% larger than normal
        poseStack.scale(1.7F, 1.7F, 1.7F);
    }
}
