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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.entity.state.SpellBatRenderState;
import thaumcraft.common.entities.monster.EntitySpellBat;

/**
 * Renderer for SpellBats - magical summoned bats with colored transparency.
 * Renders as a simple billboard sprite since the vanilla bat model doesn't
 * work well with our custom SpellBat entity.
 */
@OnlyIn(Dist.CLIENT)
public class SpellBatRenderer extends EntityRenderer<EntitySpellBat, SpellBatRenderState> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/spellbat.png");
    
    public SpellBatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
    }
    
    @Override
    public SpellBatRenderState createRenderState() {
        return new SpellBatRenderState();
    }
    
    @Override
    public void extractRenderState(EntitySpellBat entity, SpellBatRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Get color from entity
        state.color = entity.color;
        state.animAge = entity.tickCount + partialTick;
    }
    
    @Override
    public void submit(SpellBatRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        
        int color = state.color;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float alpha = 0.6F;
        
        // Scale down
        poseStack.scale(0.35F, 0.35F, 0.35F);
        
        // Billboard - face the camera
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        float size = 1.0F;
        int light = state.lightCoords;
        
        // Simple quad for the bat
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TEXTURE), (pose, buffer) -> {
            vertex(buffer, pose, -size, -size, 0, 0, 0, r, g, b, alpha, light);
            vertex(buffer, pose, -size, size, 0, 0, 1, r, g, b, alpha, light);
            vertex(buffer, pose, size, size, 0, 1, 1, r, g, b, alpha, light);
            vertex(buffer, pose, size, -size, 0, 1, 0, r, g, b, alpha, light);
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
