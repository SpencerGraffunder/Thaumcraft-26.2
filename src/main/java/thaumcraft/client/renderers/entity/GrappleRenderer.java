package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.GrapplerModel;
import thaumcraft.client.renderers.entity.state.GrappleRenderState;
import thaumcraft.common.entities.projectile.EntityGrapple;

/**
 * Renderer for Grapple hook entities.
 * Renders the hook model and a rope connecting back to the player.
 */
@OnlyIn(Dist.CLIENT)
public class GrappleRenderer extends EntityRenderer<EntityGrapple, GrappleRenderState> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/grappler.png");
    private static final Identifier ROPE_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/rope.png");
    
    private final GrapplerModel model;
    
    public GrappleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new GrapplerModel(context.bakeLayer(GrapplerModel.LAYER_LOCATION));
    }
    
    @Override
    public GrappleRenderState createRenderState() {
        return new GrappleRenderState();
    }
    
    @Override
    public void extractRenderState(EntityGrapple entity, GrappleRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        
        state.yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        state.xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        state.ampl = entity.ampl;
        state.ropeStart = null;
        state.ropeEnd = null;
        
        Entity owner = entity.getOwner();
        if (owner instanceof LivingEntity thrower) {
            // Calculate thrower position
            double tx = Mth.lerp(partialTick, thrower.xo, thrower.getX());
            double ty = Mth.lerp(partialTick, thrower.yo, thrower.getY()) + thrower.getEyeHeight() * 0.5;
            double tz = Mth.lerp(partialTick, thrower.zo, thrower.getZ());
            
            // Calculate grapple position
            double gx = Mth.lerp(partialTick, entity.xo, entity.getX());
            double gy = Mth.lerp(partialTick, entity.yo, entity.getY());
            double gz = Mth.lerp(partialTick, entity.zo, entity.getZ());
            
            // Offset for first person view
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                float yaw = thrower.getYRot() * Mth.DEG_TO_RAD;
                float px = -Mth.cos(yaw) * 0.1f * (entity.getHand() == InteractionHand.MAIN_HAND ? 1 : -1);
                float pz = -Mth.sin(yaw) * 0.1f * (entity.getHand() == InteractionHand.MAIN_HAND ? 1 : -1);
                Vec3 look = thrower.getLookAngle();
                tx += px + look.x / 5.0;
                ty += thrower.getEyeHeight() / 2.6 + look.y / 5.0;
                tz += pz + look.z / 5.0;
            }
            
            state.ropeStart = new Vec3(tx, ty, tz);
            state.ropeEnd = new Vec3(gx, gy, gz);
        }
    }
    
    @Override
    public void submit(GrappleRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        
        // Render the grapple hook model
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
        
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) -> {
            PoseStack ps = new PoseStack();
            ps.last().set(pose);
            this.model.renderToBuffer(ps, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        });
        
        poseStack.popPose();
        
        // Render the rope to the player
        if (state.ropeStart != null) {
            renderRope(state, poseStack, submitNodeCollector);
        }
        
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
    
    private void renderRope(GrappleRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        // Direction vector from grapple to thrower
        double dx = state.ropeStart.x - state.ropeEnd.x;
        double dy = state.ropeStart.y - state.ropeEnd.y;
        double dz = state.ropeStart.z - state.ropeEnd.z;
        
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.1f) return;
        
        // Render rope as a series of line segments
        int segments = Math.max(2, (int)(distance * 4));
        float width = 0.025f;
        
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(ROPE_TEXTURE), (pose, buffer) -> {
            for (int i = 0; i < segments; i++) {
                float t1 = (float) i / segments;
                float t2 = (float) (i + 1) / segments;
                
                // Add some sway to the rope
                float sway1 = Mth.sin(t1 * (float) Math.PI) * state.ampl * (1.0f - t1);
                float sway2 = Mth.sin(t2 * (float) Math.PI) * state.ampl * (1.0f - t2);
                
                float x1 = (float) (dx * t1);
                float y1 = (float) (dy * t1) + sway1;
                float z1 = (float) (dz * t1);
                
                float x2 = (float) (dx * t2);
                float y2 = (float) (dy * t2) + sway2;
                float z2 = (float) (dz * t2);
                
                // Simple line rendering (quad strip would be better but this is simpler)
                float u = t1 * distance;
                buffer.addVertex(pose, x1 - width, y1, z1)
                        .setColor(0.6f, 0.4f, 0.2f, 1.0f)
                        .setUv(u, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(0xF000F0).setNormal(pose, 0, 1, 0);
                buffer.addVertex(pose, x1 + width, y1, z1)
                        .setColor(0.6f, 0.4f, 0.2f, 1.0f)
                        .setUv(u, 1).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(0xF000F0).setNormal(pose, 0, 1, 0);
            }
        });
    }
}
