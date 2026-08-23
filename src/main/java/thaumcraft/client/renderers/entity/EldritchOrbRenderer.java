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
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.entity.state.EldritchOrbRenderState;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;

/**
 * Renderer for the Eldritch Orb projectile.
 * Renders as a dark, chaotic sphere with radiating tendrils.
 */
@OnlyIn(Dist.CLIENT)
public class EldritchOrbRenderer extends EntityRenderer<EntityEldritchOrb, EldritchOrbRenderState> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");
    
    private final RandomSource random = RandomSource.create();
    
    public EldritchOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public EldritchOrbRenderState createRenderState() {
        return new EldritchOrbRenderState();
    }
    
    @Override
    public void extractRenderState(EntityEldritchOrb entity, EldritchOrbRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.scale = Math.min(entity.tickCount, 10) / 10.0f;
        state.frame = entity.tickCount % 13;
    }
    
    @Override
    public void submit(EldritchOrbRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        random.setSeed(187L);
        
        poseStack.pushPose();
        
        float age = state.age;
        float scale = state.scale;
        
        // Render dark energy tendrils
        renderTendrils(poseStack, submitNodeCollector, age, scale);
        
        // Render central orb sprite
        renderOrbSprite(state, poseStack, submitNodeCollector, camera);
        
        poseStack.popPose();
        
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
    
    private void renderTendrils(PoseStack poseStack, 
                                SubmitNodeCollector submitNodeCollector, float age, float scale) {
        for (int i = 0; i < 12; i++) {
            poseStack.pushPose();
            
            // Random rotation for each tendril
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0f + age * 4.5f));
            
            // Tendril dimensions
            float length = (random.nextFloat() * 20.0f + 5.0f) / 30.0f * scale;
            float width = (random.nextFloat() * 2.0f + 1.0f) / 30.0f * scale;
            
            // Draw tendril as a triangle fan
            float x1 = (float)(-0.866 * width);
            float z1 = -0.5f * width;
            float x2 = (float)(0.866 * width);
            float z2 = -0.5f * width;
            float z3 = 1.0f * width;
            
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
                // Center vertex (white/bright)
                buffer.addVertex(pose, 0, 0, 0).setColor(255, 255, 255, 255);
                
                // Outer vertices (dark purple/black)
                buffer.addVertex(pose, x1, length, z1).setColor(64, 0, 64, 0);
                buffer.addVertex(pose, x2, length, z2).setColor(64, 0, 64, 0);
                buffer.addVertex(pose, 0, length, z3).setColor(64, 0, 64, 0);
                buffer.addVertex(pose, x1, length, z1).setColor(64, 0, 64, 0);
            });
            
            poseStack.popPose();
        }
    }
    
    private void renderOrbSprite(EldritchOrbRenderState state, PoseStack poseStack, 
                                 SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        
        // Billboard rotation
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.75f, 0.75f, 0.75f);
        
        // Animate through particle texture frames
        int frame = state.frame;
        float u0 = frame / 64.0f;
        float u1 = u0 + 1.0f / 64.0f;
        float v0 = 3.0f / 64.0f;  // Row 3 in particle texture
        float v1 = v0 + 1.0f / 64.0f;
        
        float size = 0.5f;
        
        // Render quad
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE), (pose, buffer) -> {
            buffer.addVertex(pose, -size, -size, 0)
                .setColor(255, 255, 255, 255)
                .setUv(u0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0, 1, 0);
            
            buffer.addVertex(pose, size, -size, 0)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0, 1, 0);
            
            buffer.addVertex(pose, size, size, 0)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0, 1, 0);
            
            buffer.addVertex(pose, -size, size, 0)
                .setColor(255, 255, 255, 255)
                .setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0, 1, 0);
        });
        
        poseStack.popPose();
    }
}
