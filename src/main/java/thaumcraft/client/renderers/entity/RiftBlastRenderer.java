package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.renderers.entity.state.RiftBlastRenderState;
import thaumcraft.common.entities.projectile.EntityRiftBlast;

/**
 * Renderer for Rift Blast projectiles.
 * Renders as a glowing orb with a wispy trail effect.
 * The original used an end portal shader, this version uses a simpler approach.
 */
@OnlyIn(Dist.CLIENT)
public class RiftBlastRenderer extends EntityRenderer<EntityRiftBlast, RiftBlastRenderState> {
    
    private static final Identifier TEXTURE = 
            Identifier.withDefaultNamespace("textures/entity/end_portal.png");
    
    public RiftBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public RiftBlastRenderState createRenderState() {
        return new RiftBlastRenderState();
    }
    
    @Override
    public void extractRenderState(EntityRiftBlast entity, RiftBlastRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.time = entity.tickCount + partialTick;
        state.red = entity.isRed();
    }
    
    @Override
    public void submit(RiftBlastRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        
        // Billboard rotation
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        // Pulsing scale
        float pulse = 1.0F + Mth.sin(state.time * 0.5F) * 0.1F;
        float size = 0.5F * pulse;
        
        // Color based on red variant
        float r = state.red ? 1.0F : 0.3F;
        float g = state.red ? 0.2F : 0.1F;
        float b = state.red ? 0.3F : 0.4F;
        float alpha = 0.8F;
        
        int light = 0xF000F0; // Full bright
        
        // Render the orb
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TEXTURE), (pose, buffer) -> {
            // Core quad
            vertex(buffer, pose, -size, -size, 0, 0, 0, r, g, b, alpha, light);
            vertex(buffer, pose, -size, size, 0, 0, 1, r, g, b, alpha, light);
            vertex(buffer, pose, size, size, 0, 1, 1, r, g, b, alpha, light);
            vertex(buffer, pose, size, -size, 0, 1, 0, r, g, b, alpha, light);
            
            // Outer glow (larger, more transparent)
            float glowSize = size * 1.5F;
            float glowAlpha = 0.3F;
            vertex(buffer, pose, -glowSize, -glowSize, 0.01F, 0, 0, r, g, b, glowAlpha, light);
            vertex(buffer, pose, -glowSize, glowSize, 0.01F, 0, 1, r, g, b, glowAlpha, light);
            vertex(buffer, pose, glowSize, glowSize, 0.01F, 1, 1, r, g, b, glowAlpha, light);
            vertex(buffer, pose, glowSize, -glowSize, 0.01F, 1, 0, r, g, b, glowAlpha, light);
        });
        
        poseStack.popPose();
        
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
    
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                        float x, float y, float z, float u, float v,
                        float r, float g, float b, float alpha, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
