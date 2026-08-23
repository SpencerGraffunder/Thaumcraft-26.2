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
import thaumcraft.client.renderers.entity.state.CultistPortalRenderState;
import thaumcraft.common.entities.monster.boss.EntityCultistPortalGreater;

/**
 * Renderer for Greater Cultist Portals.
 * Similar to lesser portal but larger and always active.
 */
@OnlyIn(Dist.CLIENT)
public class CultistPortalGreaterRenderer extends EntityRenderer<EntityCultistPortalGreater, CultistPortalRenderState> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/cultist_portal.png");
    
    public CultistPortalGreaterRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public CultistPortalRenderState createRenderState() {
        return new CultistPortalRenderState();
    }
    
    @Override
    public void extractRenderState(EntityCultistPortalGreater entity, CultistPortalRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // The greater portal is always active and has no hurt time
        state.active = true;
        state.activeCounter = entity.tickCount;
        state.hurtTime = 0;
        state.pulse = entity.pulse;
        state.healthPercent = entity.getMaxHealth() <= 0 ? 0.0F : entity.getHealth() / entity.getMaxHealth();
        state.height = entity.getBbHeight();
    }
    
    @Override
    public void submit(CultistPortalRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        
        // Calculate animation values
        long time = System.nanoTime() / 50000000L;
        float scaley = 2.0F; // Larger than lesser portal
        int activeFrames = (int) Math.min(50.0F, state.activeCounter);
        
        // Pulse effect
        if (state.pulse > 0) {
            double pulseWobble = Math.sin(state.pulse * 36 * Math.PI / 180.0);
            scaley += (float)(pulseWobble / 4.0);
            activeFrames += (int)(12.0 * pulseWobble);
        }
        
        float scale = Math.min(1.0F, activeFrames / 50.0F) * 1.75F; // Larger scale
        
        // Health-based wobble
        float healthPercent = 1.0F - state.healthPercent;
        float m = healthPercent / 3.0F;
        float bob = Mth.sin(state.activeCounter / (5.0F - 12.0F * m)) * m + m;
        float bob2 = Mth.sin(state.activeCounter / (6.0F - 15.0F * m)) * m + m;
        float alpha = 1.0F - bob;
        scaley -= bob / 4.0F;
        scale -= bob2 / 3.0F;
        
        // Position at center of entity
        poseStack.translate(0.0, state.height / 2.0F, 0.0);
        
        // Billboard - face the camera
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        // Get texture frame (16 frame animation)
        int frame = 15 - (int)(time % 16);
        float minU = frame / 16.0F;
        float maxU = minU + 0.0625F;
        float minV = 0.0F;
        float maxV = 1.0F;
        
        int light = 0xF000F0; // Full bright
        
        // Render the quad
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TEXTURE), (pose, buffer) -> {
            vertex(buffer, pose, -scale, -scaley, 0, maxU, minV, alpha, light);
            vertex(buffer, pose, -scale, scaley, 0, maxU, maxV, alpha, light);
            vertex(buffer, pose, scale, scaley, 0, minU, maxV, alpha, light);
            vertex(buffer, pose, scale, -scaley, 0, minU, minV, alpha, light);
        });
        
        poseStack.popPose();
        
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
    
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                        float x, float y, float z, float u, float v, float alpha, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
