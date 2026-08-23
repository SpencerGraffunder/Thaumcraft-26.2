package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.ArcaneBoreModel;
import thaumcraft.client.renderers.entity.state.ArcaneBoreRenderState;
import thaumcraft.common.entities.construct.EntityArcaneBore;

/**
 * Renderer for the Arcane Bore mining construct.
 * 
 * Features:
 * - Rotating head that aims at mining targets
 * - Mining beam effect when actively digging
 * - Glow effect on the front crystal
 */
@OnlyIn(Dist.CLIENT)
public class ArcaneBoreRenderer extends MobRenderer<EntityArcaneBore, ArcaneBoreRenderState, ArcaneBoreModel> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/arcanebore.png");
    
    private static final Identifier BEAM_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/beam1.png");
    
    public ArcaneBoreRenderer(EntityRendererProvider.Context context) {
        super(context, new ArcaneBoreModel(context.bakeLayer(ArcaneBoreModel.LAYER_LOCATION)), 0.5F);
    }
    
    @Override
    public Identifier getTextureLocation(ArcaneBoreRenderState state) {
        return TEXTURE;
    }
    
    @Override
    public ArcaneBoreRenderState createRenderState() {
        return new ArcaneBoreRenderState();
    }
    
    @Override
    public void extractRenderState(EntityArcaneBore entity, ArcaneBoreRenderState state, float partialTick) {
        // Reset yaw offset (bore rotates head, not body)
        entity.yBodyRot = 0.0F;
        entity.yBodyRotO = 0.0F;
        
        super.extractRenderState(entity, state, partialTick);
        
        state.digging = entity.isClientDigging() && entity.isActive() && entity.hasValidInventory();
        state.yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        state.eyeHeight = entity.getEyeHeight();
        state.gameTime = entity.level().getGameTime();
    }
    
    @Override
    public void submit(ArcaneBoreRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        
        // Render mining beam if actively digging
        if (state.digging) {
            renderMiningBeam(state, poseStack, submitNodeCollector);
        }
    }
    
    /**
     * Renders the mining beam effect when the bore is digging.
     */
    private void renderMiningBeam(ArcaneBoreRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        poseStack.pushPose();
        
        // Offset from center to tip
        Vec3 offset = new Vec3(0.5, 0.075, 0.0);
        offset = rotateAroundZ(offset, state.pitch * Mth.DEG_TO_RAD);
        offset = rotateAroundY(offset, -((state.yaw + 90.0F) * Mth.DEG_TO_RAD));
        
        poseStack.translate(offset.x, state.eyeHeight + offset.y, offset.z);
        
        // Render the beam
        renderBeam(state, poseStack, submitNodeCollector);
        
        poseStack.popPose();
    }
    
    /**
     * Renders the mining beam itself.
     */
    private void renderBeam(ArcaneBoreRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        // Beam parameters
        float beamLength = 5.0F;
        float beamWidth = 0.15F;
        float opacity = 0.4F;
        
        // Animation
        float rotation = (state.gameTime % 72L) * 5.0F + 5.0F * state.partialTick;
        float scroll = -state.ageInTicks * 0.2F;
        scroll = scroll - Mth.floor(scroll);
        
        poseStack.pushPose();
        
        // Rotate to face correct direction
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZN.rotationDegrees(180.0F + state.yaw));
        poseStack.mulPose(Axis.XN.rotationDegrees(state.pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        
        // Draw 3 beam quads rotated 60 degrees apart
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(60.0F * (i + 1)));
            
            final int quad = i;
            
            // Green tint (0, 1, 0.4)
            int r = (int)(0.0F * 255);
            int g = (int)(1.0F * 255);
            int b = (int)(0.4F * 255);
            int a = (int)(opacity * 255);
            
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(BEAM_TEXTURE), (pose, buffer) -> {
                float u0 = 0.0F;
                float u1 = 1.0F;
                float v0 = scroll + quad / 3.0F;
                float v1 = beamLength + v0;
                
                // Draw quad
                buffer.addVertex(pose, 0.0F, beamLength, 0.0F)
                    .setColor(r, g, b, a)
                    .setUv(u1, v1)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
                
                buffer.addVertex(pose, -beamWidth, 0.0F, 0.0F)
                    .setColor(r, g, b, a)
                    .setUv(u1, v0)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
                
                buffer.addVertex(pose, beamWidth, 0.0F, 0.0F)
                    .setColor(r, g, b, a)
                    .setUv(u0, v0)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
                
                buffer.addVertex(pose, 0.0F, beamLength, 0.0F)
                    .setColor(r, g, b, a)
                    .setUv(u0, v1)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(state.lightCoords)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
            });
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
    }
    
    /**
     * Rotate a vector around the Y axis.
     */
    private static Vec3 rotateAroundY(Vec3 vec, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vec.x * cos + vec.z * sin;
        double z = -vec.x * sin + vec.z * cos;
        return new Vec3(x, vec.y, z);
    }
    
    /**
     * Rotate a vector around the Z axis.
     */
    private static Vec3 rotateAroundZ(Vec3 vec, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vec.x * cos - vec.y * sin;
        double y = vec.x * sin + vec.y * cos;
        return new Vec3(x, y, vec.z);
    }
    
    @Override
    protected boolean shouldShowName(EntityArcaneBore entity, double distanceToCameraSq) {
        return false; // Bores don't show names
    }
}
