package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.TaintacleModel;
import thaumcraft.common.entities.monster.tainted.EntityTaintacle;

/**
 * Renderer for Taintacles - tentacle-like taint creatures that emerge from the ground.
 */
@OnlyIn(Dist.CLIENT)
public class TaintacleRenderer extends MobRenderer<EntityTaintacle, TaintacleModel<EntityTaintacle>> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/taintacle.png");
    
    public TaintacleRenderer(EntityRendererProvider.Context context) {
        super(context, new TaintacleModel<>(context.bakeLayer(TaintacleModel.LAYER_LOCATION)), 0.3F);
    }
    
    @Override
    public Identifier getTextureLocation(EntityTaintacle entity) {
        return TEXTURE;
    }
    
    @Override
    protected void scale(EntityTaintacle entity, PoseStack poseStack, float partialTicks) {
        // Scale based on taintacle length/size
        float scale = 0.8F + entity.getLength() * 0.1F;
        poseStack.scale(scale, scale, scale);
    }
}
