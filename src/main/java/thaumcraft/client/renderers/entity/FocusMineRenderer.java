package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.GrapplerModel;
import thaumcraft.common.entities.projectile.EntityFocusMine;

/**
 * Renderer for Focus Mine entities.
 * Renders using the grappler model with pulsing red color when armed.
 */
@OnlyIn(Dist.CLIENT)
public class FocusMineRenderer extends EntityRenderer<EntityFocusMine> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/mine.png");
    
    private final GrapplerModel model;
    
    public FocusMineRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new GrapplerModel(context.bakeLayer(GrapplerModel.LAYER_LOCATION));
    }
    
    @Override
    public Identifier getTextureLocation(EntityFocusMine entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityFocusMine entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Pulsing effect - gets redder when armed
        float pulse = (entity.tickCount + partialTicks) % 8.0F / 8.0F;
        float red = 1.0F;
        float green = entity.isArmed() ? (1.0F - pulse) : 1.0F;
        float blue = entity.isArmed() ? (1.0F - pulse) : 1.0F;
        
        // Rotation based on entity direction
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks));
        
        // Render the model
        RenderType renderType = RenderType.entityCutout(TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        
        // Use full bright light when armed
        int light = entity.isArmed() ? 0xF000F0 : packedLight;
        
        this.model.renderToBuffer(poseStack, vertexConsumer, light, 
                OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
