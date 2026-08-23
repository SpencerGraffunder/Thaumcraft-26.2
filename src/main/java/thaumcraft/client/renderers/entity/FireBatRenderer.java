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
import thaumcraft.client.renderers.entity.state.FireBatRenderState;
import thaumcraft.common.entities.monster.EntityFireBat;

/**
 * Renderer for Fire Bats - fire elemental bats.
 * 
 * Fire bats are rendered as simple glowing sprites since the vanilla
 * BatModel doesn't support custom entity types. This creates a flame-like
 * animated appearance that fits the entity.
 */
@OnlyIn(Dist.CLIENT)
public class FireBatRenderer extends EntityRenderer<EntityFireBat, FireBatRenderState> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/firebat.png");
    
    public FireBatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
    }
    
    @Override
    public FireBatRenderState createRenderState() {
        return new FireBatRenderState();
    }
    
    @Override
    public void extractRenderState(EntityFireBat entity, FireBatRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.animAge = entity.tickCount + partialTick;
        // Wing flap animation
        state.size = 0.5F + Mth.sin(state.animAge * 0.75F) * 0.2F * 0.1F;
    }
    
    @Override
    public void submit(FireBatRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        
        // Billboard rotation
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        float size = state.size;
        
        // Full bright for fire
        int light = 0xF000F0;
        
        // Render as billboard quad
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE), (pose, buffer) -> {
            // Simple quad
            buffer.addVertex(pose, -size, -size, 0.0F)
                .setColor(255, 200, 100, 255)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
            
            buffer.addVertex(pose, size, -size, 0.0F)
                .setColor(255, 200, 100, 255)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
            
            buffer.addVertex(pose, size, size, 0.0F)
                .setColor(255, 200, 100, 255)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
            
            buffer.addVertex(pose, -size, size, 0.0F)
                .setColor(255, 200, 100, 255)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        });
        
        poseStack.popPose();
        
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
