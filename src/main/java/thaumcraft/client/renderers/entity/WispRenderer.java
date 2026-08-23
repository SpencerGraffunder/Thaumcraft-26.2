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
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.entity.state.WispRenderState;
import thaumcraft.common.entities.monster.EntityWisp;

/**
 * Renderer for Wisps - glowing orbs of magical energy.
 * 
 * Wisps don't use a traditional model. Instead, they render as
 * animated billboard quads facing the camera, with different
 * layers for the core glow, inner orb, and outer aura.
 * 
 * The color is determined by the Wisp's aspect type.
 */
@OnlyIn(Dist.CLIENT)
public class WispRenderer extends EntityRenderer<EntityWisp, WispRenderState> {
    
    // Texture atlas with wisp particles
    private static final Identifier WISP_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/wisp.png");
    
    public WispRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }
    
    @Override
    public WispRenderState createRenderState() {
        return new WispRenderState();
    }
    
    @Override
    public void extractRenderState(EntityWisp entity, WispRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.dead = entity.isDeadOrDying();
        state.color = 0xFFFFFF;
        Aspect aspect = entity.getAspect();
        if (aspect != null) {
            state.color = aspect.getColor();
        }
        state.animAge = entity.tickCount + partialTick;
        state.frame = (entity.tickCount + (int) partialTick) % 16;
    }
    
    @Override
    public void submit(WispRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.dead) {
            return;
        }
        
        // Get color from aspect type
        int color = state.color;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        
        poseStack.pushPose();
        
        // Billboard rotation - always face camera
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        // Animation based on tick count
        int animFrame = state.frame;
        float pulse = 0.8F + 0.2F * Mth.sin(state.animAge * 0.2F);
        
        // Render glow layers
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(WISP_TEXTURE), (pose, buffer) -> {
            // Outer glow (larger, more transparent)
            renderQuad(pose, buffer, 0.75F * pulse, red, green, blue, 0.25F, animFrame);
            
            // Middle glow
            renderQuad(pose, buffer, 0.5F * pulse, red, green, blue, 0.5F, animFrame);
            
            // Core (white-ish, bright)
            renderQuad(pose, buffer, 0.3F * pulse, 1.0F, 1.0F, 1.0F, 0.8F, animFrame);
        });
        
        poseStack.popPose();
        
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
    
    /**
     * Renders a billboard quad for one layer of the wisp.
     */
    private static void renderQuad(PoseStack.Pose pose, VertexConsumer vertexConsumer, 
                                   float size, float red, float green, float blue, float alpha,
                                   int frame) {
        // Calculate UV coordinates for animation frame (4x4 grid)
        int frameX = frame % 4;
        int frameY = frame / 4;
        float u0 = frameX / 4.0F;
        float u1 = (frameX + 1) / 4.0F;
        float v0 = frameY / 4.0F;
        float v1 = (frameY + 1) / 4.0F;
        
        // Full brightness for emissive rendering
        int light = 0xF000F0;
        
        // Quad vertices
        vertexConsumer.addVertex(pose, -size, -size, 0.0F)
            .setColor(red, green, blue, alpha)
            .setUv(u0, v1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
        
        vertexConsumer.addVertex(pose, size, -size, 0.0F)
            .setColor(red, green, blue, alpha)
            .setUv(u1, v1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
        
        vertexConsumer.addVertex(pose, size, size, 0.0F)
            .setColor(red, green, blue, alpha)
            .setUv(u1, v0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
        
        vertexConsumer.addVertex(pose, -size, size, 0.0F)
            .setColor(red, green, blue, alpha)
            .setUv(u0, v0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
