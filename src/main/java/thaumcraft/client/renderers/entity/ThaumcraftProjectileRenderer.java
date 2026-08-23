package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;

/**
 * Generic renderer for Thaumcraft projectiles.
 * Renders as a glowing billboard sprite.
 * Can be configured with different textures and colors.
 */
@OnlyIn(Dist.CLIENT)
public class ThaumcraftProjectileRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState> {
    
    private final Identifier texture;
    private final float size;
    private final int color;
    private final boolean emissive;
    
    public ThaumcraftProjectileRenderer(EntityRendererProvider.Context context, 
                                        Identifier texture, float size, int color, boolean emissive) {
        super(context);
        this.texture = texture;
        this.size = size;
        this.color = color;
        this.emissive = emissive;
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
    
    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        
        // Billboard rotation
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        // Spin animation
        float spin = state.ageInTicks * 10.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        
        // Get render type
        RenderType renderType = emissive ? 
                RenderTypes.entityTranslucentEmissive(texture) : 
                RenderTypes.entityTranslucent(texture);
        
        // Extract color components
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 255;
        
        int light = emissive ? 0xF000F0 : state.lightCoords;
        
        // Render quad
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            buffer.addVertex(pose, -size, -size, 0.0F)
                .setColor(r, g, b, a)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
            
            buffer.addVertex(pose, size, -size, 0.0F)
                .setColor(r, g, b, a)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
            
            buffer.addVertex(pose, size, size, 0.0F)
                .setColor(r, g, b, a)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
            
            buffer.addVertex(pose, -size, size, 0.0F)
                .setColor(r, g, b, a)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        });
        
        poseStack.popPose();
        
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
    
    /**
     * Factory for creating common projectile renderers.
     */
    public static class Factory {
        private static final Identifier ORB_TEXTURE = 
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/orb.png");
        private static final Identifier DART_TEXTURE = 
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/dart.png");
        
        public static <T extends Entity> ThaumcraftProjectileRenderer<T> orb(
                EntityRendererProvider.Context context, int color) {
            return new ThaumcraftProjectileRenderer<>(context, ORB_TEXTURE, 0.25F, color, true);
        }
        
        public static <T extends Entity> ThaumcraftProjectileRenderer<T> dart(
                EntityRendererProvider.Context context) {
            return new ThaumcraftProjectileRenderer<>(context, DART_TEXTURE, 0.15F, 0xFFFFFF, false);
        }
        
        public static <T extends Entity> ThaumcraftProjectileRenderer<T> magic(
                EntityRendererProvider.Context context, int color) {
            return new ThaumcraftProjectileRenderer<>(context, ORB_TEXTURE, 0.2F, color, true);
        }
    }
}
