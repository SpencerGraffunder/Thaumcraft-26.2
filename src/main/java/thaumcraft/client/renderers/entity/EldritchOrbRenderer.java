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
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;

/**
 * Renderer for the Eldritch Orb projectile.
 * Renders as a dark, chaotic sphere with radiating tendrils.
 */
@OnlyIn(Dist.CLIENT)
public class EldritchOrbRenderer extends EntityRenderer<EntityEldritchOrb> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");
    
    private final RandomSource random = RandomSource.create();
    
    public EldritchOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public Identifier getTextureLocation(EntityEldritchOrb entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityEldritchOrb entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        random.setSeed(187L);
        
        poseStack.pushPose();
        
        float age = entity.tickCount + partialTicks;
        float scale = Math.min(entity.tickCount, 10) / 10.0f;
        
        // Render dark energy tendrils
        renderTendrils(poseStack, buffer, age, scale);
        
        // Render central orb sprite
        renderOrbSprite(entity, poseStack, buffer, age);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    private void renderTendrils(PoseStack poseStack, MultiBufferSource buffer, 
                                float age, float scale) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lightning());
        
        for (int i = 0; i < 12; i++) {
            poseStack.pushPose();
            
            // Random rotation for each tendril
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0f + age * 4.5f));
            
            // Tendril dimensions
            float length = (random.nextFloat() * 20.0f + 5.0f) / 30.0f * scale;
            float width = (random.nextFloat() * 2.0f + 1.0f) / 30.0f * scale;
            
            Matrix4f matrix = poseStack.last().pose();
            
            // Draw tendril as a triangle fan
            // Center vertex (white/bright)
            vertexConsumer.vertex(matrix, 0, 0, 0).color(255, 255, 255, 255).endVertex();
            
            // Outer vertices (dark purple/black)
            float x1 = (float)(-0.866 * width);
            float z1 = -0.5f * width;
            float x2 = (float)(0.866 * width);
            float z2 = -0.5f * width;
            float z3 = 1.0f * width;
            
            vertexConsumer.vertex(matrix, x1, length, z1).color(64, 0, 64, 0).endVertex();
            vertexConsumer.vertex(matrix, x2, length, z2).color(64, 0, 64, 0).endVertex();
            vertexConsumer.vertex(matrix, 0, length, z3).color(64, 0, 64, 0).endVertex();
            vertexConsumer.vertex(matrix, x1, length, z1).color(64, 0, 64, 0).endVertex();
            
            poseStack.popPose();
        }
    }
    
    private void renderOrbSprite(EntityEldritchOrb entity, PoseStack poseStack, 
                                 MultiBufferSource buffer, float age) {
        poseStack.pushPose();
        
        // Billboard rotation
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.75f, 0.75f, 0.75f);
        
        VertexConsumer vertexConsumer = buffer.getBuffer(
                RenderType.entityTranslucentEmissive(TEXTURE));
        
        // Animate through particle texture frames
        int frame = entity.tickCount % 13;
        float u0 = frame / 64.0f;
        float u1 = u0 + 1.0f / 64.0f;
        float v0 = 3.0f / 64.0f;  // Row 3 in particle texture
        float v1 = v0 + 1.0f / 64.0f;
        
        float size = 0.5f;
        
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        
        // Render quad
        vertexConsumer.vertex(matrix, -size, -size, 0)
            .color(255, 255, 255, 255)
            .uv(u0, v1)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(0xF000F0)
            .normal(normal, 0, 1, 0)
            .endVertex();
        
        vertexConsumer.vertex(matrix, size, -size, 0)
            .color(255, 255, 255, 255)
            .uv(u1, v1)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(0xF000F0)
            .normal(normal, 0, 1, 0)
            .endVertex();
        
        vertexConsumer.vertex(matrix, size, size, 0)
            .color(255, 255, 255, 255)
            .uv(u1, v0)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(0xF000F0)
            .normal(normal, 0, 1, 0)
            .endVertex();
        
        vertexConsumer.vertex(matrix, -size, size, 0)
            .color(255, 255, 255, 255)
            .uv(u0, v0)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(0xF000F0)
            .normal(normal, 0, 1, 0)
            .endVertex();
        
        poseStack.popPose();
    }
}
