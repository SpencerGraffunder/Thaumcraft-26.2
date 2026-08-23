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
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.entity.state.TaintSwarmRenderState;
import thaumcraft.common.entities.monster.tainted.EntityTaintSwarm;

import java.util.Random;

/**
 * Renderer for Taint Swarms - clouds of tainted insects.
 * Renders as a cluster of animated particles/sprites.
 */
@OnlyIn(Dist.CLIENT)
public class TaintSwarmRenderer extends EntityRenderer<EntityTaintSwarm, TaintSwarmRenderState> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/taint_swarm.png");
    
    public TaintSwarmRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }
    
    @Override
    public TaintSwarmRenderState createRenderState() {
        return new TaintSwarmRenderState();
    }
    
    @Override
    public void extractRenderState(EntityTaintSwarm entity, TaintSwarmRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.time = entity.tickCount + partialTick;
        state.seed = entity.getId();
    }
    
    @Override
    public void submit(TaintSwarmRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        
        // Billboard rotation
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        Random random = new Random(state.seed);
        float time = state.time;
        int light = state.lightCoords;
        
        // Render multiple swirling particles
        for (int i = 0; i < 12; i++) {
            poseStack.pushPose();
            
            // Random offset for each particle
            float angle = (i / 12.0F) * Mth.TWO_PI + time * 0.1F;
            float radius = 0.3F + random.nextFloat() * 0.4F;
            float bobY = Mth.sin(time * 0.15F + i) * 0.2F;
            
            float offsetX = Mth.cos(angle) * radius;
            float offsetY = bobY + (random.nextFloat() - 0.5F) * 0.5F;
            float offsetZ = Mth.sin(angle) * radius;
            
            poseStack.translate(offsetX, offsetY, offsetZ);
            
            // Individual particle rotation
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360 + time * 5));
            
            float size = 0.15F + random.nextFloat() * 0.1F;
            
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TEXTURE), (pose, buffer) -> {
                renderParticle(pose, buffer, size, light);
            });
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
        
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
    
    private static void renderParticle(PoseStack.Pose pose, VertexConsumer vertexConsumer, 
                                       float size, int light) {
        // Purple taint color
        int r = 128;
        int g = 64;
        int b = 160;
        int a = 200;
        
        vertexConsumer.addVertex(pose, -size, -size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(0.0F, 1.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
        
        vertexConsumer.addVertex(pose, size, -size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(1.0F, 1.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
        
        vertexConsumer.addVertex(pose, size, size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(1.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
        
        vertexConsumer.addVertex(pose, -size, size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(0.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
