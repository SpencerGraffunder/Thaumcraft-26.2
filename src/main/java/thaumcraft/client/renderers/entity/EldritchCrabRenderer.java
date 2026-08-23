package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.EntityEldritchCrab;

/**
 * Renderer for Eldritch Crabs - creepy void crabs.
 * Uses a modified spider model with custom texture.
 */
@OnlyIn(Dist.CLIENT)
public class EldritchCrabRenderer extends MobRenderer<EntityEldritchCrab, LivingEntityRenderState, SpiderModel> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/eldritch_crab.png");
    
    public EldritchCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel(context.bakeLayer(ModelLayers.SPIDER)), 0.4F);
        this.addLayer(new SpiderEyesLayer<>(this));
    }
    
    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
    
    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
    
    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        // Crabs are wider and shorter than spiders
        poseStack.scale(0.8F, 0.6F, 0.8F);
    }
}
