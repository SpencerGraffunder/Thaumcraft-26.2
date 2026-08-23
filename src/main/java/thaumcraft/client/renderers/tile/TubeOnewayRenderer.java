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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.client.models.block.TubeValveModel;
import thaumcraft.client.renderers.tile.state.TubeOnewayRenderState;
import thaumcraft.common.tiles.essentia.TileTubeOneway;

/**
 * Block entity renderer for the One-way Essentia Tube.
 * Renders a directional indicator showing flow direction.
 */
@OnlyIn(Dist.CLIENT)
public class TubeOnewayRenderer implements BlockEntityRenderer<TileTubeOneway, TubeOnewayRenderState> {

    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/valve.png");

    private final TubeValveModel model;

    public TubeOnewayRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new TubeValveModel(context.bakeLayer(TubeValveModel.LAYER_LOCATION));
    }

    @Override
    public TubeOnewayRenderState createRenderState() {
        return new TubeOnewayRenderState();
    }

    @Override
    public void extractRenderState(TileTubeOneway tile, TubeOnewayRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.facing = tile.facing;

        // Check if there's a connectable tile in the opposite direction
        BlockEntity connectedTile = tile.getLevel() != null
                ? ThaumcraftApiHelper.getConnectableTile(tile.getLevel(), tile.getBlockPos(), tile.facing.getOpposite())
                : null;
        state.hasConnection = connectedTile != null;
    }

    @Override
    public void submit(TubeOnewayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.hasConnection) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate based on facing direction
        applyFacingRotation(poseStack, state.facing);

        // Color: blue/cyan for flow indicator
        int tintedColor = 0xFF7380FF;

        // Scale and position
        poseStack.scale(0.0625f, 0.0625f, 0.0625f);
        poseStack.scale(2.0f, 2.0f, 2.0f);
        poseStack.translate(0, -0.32 / 0.0625 / 2, 0);  // Adjust position

        submitNodeCollector.submitModelPart(this.model.getRod(), poseStack, this.model.renderType(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, null, tintedColor, state.breakProgress);

        poseStack.popPose();
    }

    /**
     * Apply rotation based on the facing direction.
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
