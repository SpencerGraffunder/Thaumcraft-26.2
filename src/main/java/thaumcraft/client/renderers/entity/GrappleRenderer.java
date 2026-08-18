package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.GrapplerModel;
import thaumcraft.common.entities.projectile.EntityGrapple;

/**
 * Renderer for Grapple hook entities.
 * Renders the hook model and a rope connecting back to the player.
 */
@OnlyIn(Dist.CLIENT)
public class GrappleRenderer extends EntityRenderer<EntityGrapple> {
    
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
    public Identifier getTextureLocation(EntityGrapple entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityGrapple entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Render the grapple hook model
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks));
        
        RenderType renderType = RenderType.entityCutout(TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, 
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        poseStack.popPose();
        
        // Render the rope to the player
        Entity owner = entity.getOwner();
        if (owner instanceof LivingEntity thrower) {
            renderRope(entity, thrower, partialTicks, poseStack, buffer);
        }
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    private void renderRope(EntityGrapple grapple, LivingEntity thrower, float partialTicks,
                            PoseStack poseStack, MultiBufferSource buffer) {
        // Calculate thrower position
        double tx = Mth.lerp(partialTicks, thrower.xo, thrower.getX());
        double ty = Mth.lerp(partialTicks, thrower.yo, thrower.getY()) + thrower.getEyeHeight() * 0.5;
        double tz = Mth.lerp(partialTicks, thrower.zo, thrower.getZ());
        
        // Calculate grapple position
        double gx = Mth.lerp(partialTicks, grapple.xo, grapple.getX());
        double gy = Mth.lerp(partialTicks, grapple.yo, grapple.getY());
        double gz = Mth.lerp(partialTicks, grapple.zo, grapple.getZ());
        
        // Offset for first person view
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            float yaw = thrower.getYRot() * Mth.DEG_TO_RAD;
            float px = -Mth.cos(yaw) * 0.1f * (grapple.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? 1 : -1);
            float pz = -Mth.sin(yaw) * 0.1f * (grapple.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? 1 : -1);
            Vec3 look = thrower.getLookAngle();
            tx += px + look.x / 5.0;
            ty += thrower.getEyeHeight() / 2.6 + look.y / 5.0;
            tz += pz + look.z / 5.0;
        }
        
        // Direction vector from grapple to thrower
        double dx = tx - gx;
        double dy = ty - gy;
        double dz = tz - gz;
        
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.1f) return;
        
        poseStack.pushPose();
        
        // Render rope as a series of line segments
        RenderType lineType = RenderType.entityCutout(ROPE_TEXTURE);
        VertexConsumer ropeBuffer = buffer.getBuffer(lineType);
        
        int segments = Math.max(2, (int)(distance * 4));
        float width = 0.025f;
        
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;
            
            // Add some sway to the rope
            float sway1 = Mth.sin(t1 * (float) Math.PI) * grapple.ampl * (1.0f - t1);
            float sway2 = Mth.sin(t2 * (float) Math.PI) * grapple.ampl * (1.0f - t2);
            
            float x1 = (float) (dx * t1);
            float y1 = (float) (dy * t1) + sway1;
            float z1 = (float) (dz * t1);
            
            float x2 = (float) (dx * t2);
            float y2 = (float) (dy * t2) + sway2;
            float z2 = (float) (dz * t2);
            
            // Simple line rendering (quad strip would be better but this is simpler)
            float u = t1 * distance;
            ropeBuffer.vertex(matrix, x1 - width, y1, z1)
                    .color(0.6f, 0.4f, 0.2f, 1.0f)
                    .uv(u, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xF000F0).normal(normal, 0, 1, 0).endVertex();
            ropeBuffer.vertex(matrix, x1 + width, y1, z1)
                    .color(0.6f, 0.4f, 0.2f, 1.0f)
                    .uv(u, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xF000F0).normal(normal, 0, 1, 0).endVertex();
        }
        
        poseStack.popPose();
    }
}
