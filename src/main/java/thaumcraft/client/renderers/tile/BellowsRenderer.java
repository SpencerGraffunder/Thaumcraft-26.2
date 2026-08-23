package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.block.BellowsModel;
import thaumcraft.client.renderers.tile.state.BellowsRenderState;
import thaumcraft.common.tiles.devices.TileBellows;

/**
 * Block entity renderer for the Bellows.
 * Renders an animated bellows model that inflates and deflates.
 */
@OnlyIn(Dist.CLIENT)
public class BellowsRenderer implements BlockEntityRenderer<TileBellows, BellowsRenderState> {

    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/bellows.png");

    private final BellowsModel model;

    public BellowsRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new BellowsModel(context.bakeLayer(BellowsModel.LAYER_LOCATION));
    }

    @Override
    public BellowsRenderState createRenderState() {
        return new BellowsRenderState();
    }

    @Override
    public void extractRenderState(TileBellows tile, BellowsRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.facing = TileBellows.getFacing(tile.getBlockState());
        state.inflation = tile.inflation;
    }

    @Override
    public void submit(BellowsRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        // Move to block center and apply transformations
        poseStack.translate(0.5, 0.0, 0.5);

        // Rotate based on facing direction
        applyRotation(poseStack, state.facing);

        // Scale to fit within block (model uses 1/16 units)
        poseStack.scale(0.0625f, 0.0625f, 0.0625f);

        // Set inflation state for animation (applied in model.setupAnim from the state)
        submitNodeCollector.submitModel(this.model, state, poseStack, this.model.renderType(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, 0, state.breakProgress);

        poseStack.popPose();
    }

    /**
     * Apply rotation based on the bellows facing direction.
     */
    private void applyRotation(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0, 1, -1);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
            }
            case UP -> {
                poseStack.translate(0, 1, 1);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            }
            case NORTH -> {
                // Default orientation, no rotation needed
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }
            case EAST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            }
        }
    }
}
