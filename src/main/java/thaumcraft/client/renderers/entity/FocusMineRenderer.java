package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
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
import thaumcraft.client.models.entity.GrapplerModel;
import thaumcraft.client.renderers.entity.state.FocusMineRenderState;
import thaumcraft.common.entities.projectile.EntityFocusMine;

/**
 * Renderer for Focus Mine entities.
 * Renders using the grappler model with pulsing red color when armed.
 */
@OnlyIn(Dist.CLIENT)
public class FocusMineRenderer extends EntityRenderer<EntityFocusMine, FocusMineRenderState> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/mine.png");
    
    private final GrapplerModel model;
    
    public FocusMineRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new GrapplerModel(context.bakeLayer(GrapplerModel.LAYER_LOCATION));
    }
    
    @Override
    public FocusMineRenderState createRenderState() {
        return new FocusMineRenderState();
    }
    
    @Override
    public void extractRenderState(EntityFocusMine entity, FocusMineRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.animAge = entity.tickCount + partialTick;
        state.armed = entity.isArmed();
        state.yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        state.xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
    }
    
    @Override
    public void submit(FocusMineRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        
        // Pulsing effect - gets redder when armed
        float pulse = state.animAge % 8.0F / 8.0F;
        float red = 1.0F;
        float green = state.armed ? (1.0F - pulse) : 1.0F;
        float blue = state.armed ? (1.0F - pulse) : 1.0F;
        
        // Rotation based on entity direction
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
        
        // Use full bright light when armed
        int light = state.armed ? 0xF000F0 : state.lightCoords;
        
        // Render the model
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) -> {
            PoseStack ps = new PoseStack();
            ps.last().set(pose);
            this.model.renderToBuffer(ps, buffer, light, OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);
        });
        
        poseStack.popPose();
        
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
