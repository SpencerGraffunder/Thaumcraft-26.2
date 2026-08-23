package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.block.TubeValveModel;
import thaumcraft.client.renderers.tile.state.TubeValveRenderState;
import thaumcraft.common.tiles.essentia.TileTubeValve;

/**
 * Block entity renderer for the Essentia Valve tube.
 * Renders a spinning valve mechanism that opens/closes.
 */
@OnlyIn(Dist.CLIENT)
public class TubeValveRenderer implements BlockEntityRenderer<TileTubeValve, TubeValveRenderState> {

    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/valve.png");

    private final TubeValveModel model;

    public TubeValveRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new TubeValveModel(context.bakeLayer(TubeValveModel.LAYER_LOCATION));
    }

    @Override
    public TubeValveRenderState createRenderState() {
        return new TubeValveRenderState();
    }

    @Override
    public void extractRenderState(TileTubeValve tile, TubeValveRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.facing = tile.facing;
        state.rotation = tile.rotation;
    }

    @Override
    public void submit(TubeValveRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate based on facing direction
        applyFacingRotation(poseStack, state.facing);

        // Animate valve rotation when opening/closing
        float rotation = state.rotation;
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotation * 1.5f));

        // Move valve position based on rotation state (down when closed)
        float offset = -0.03f - (rotation / 360.0f) * 0.09f;
        poseStack.translate(0, offset, 0);

        // Scale for model size (model uses 1/16 scale internally)
        poseStack.scale(0.0625f, 0.0625f, 0.0625f);

        RenderType renderType = this.model.renderType(TEXTURE);

        // Render ring
        submitNodeCollector.submitModelPart(this.model.getRing(), poseStack, renderType,
                state.lightCoords, OverlayTexture.NO_OVERLAY, null, -1, state.breakProgress);

        // Scale down rod slightly
        poseStack.scale(0.75f, 1.0f, 0.75f);
        submitNodeCollector.submitModelPart(this.model.getRod(), poseStack, renderType,
                state.lightCoords, OverlayTexture.NO_OVERLAY, null, -1, state.breakProgress);

        poseStack.popPose();
    }

    /**
     * Apply rotation based on the facing direction of the valve.
     */
    private void applyFacingRotation(PoseStack poseStack, Direction facing) {
        if (facing.getAxis() != Direction.Axis.Y) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
        } else {
            poseStack.mulPose(Axis.XN.rotationDegrees(90));
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0f * facing.getStepY()));
        }
        
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f * facing.getStepX()));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f * facing.getStepY()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f * facing.getStepZ()));
    }
}
