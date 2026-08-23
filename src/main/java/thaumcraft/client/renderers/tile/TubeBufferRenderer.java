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
import thaumcraft.client.renderers.tile.state.TubeBufferRenderState;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;

/**
 * Block entity renderer for the Essentia Buffer tube.
 * Renders colored valve indicators on sides based on choke state.
 */
@OnlyIn(Dist.CLIENT)
public class TubeBufferRenderer implements BlockEntityRenderer<TileTubeBuffer, TubeBufferRenderState> {

    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/valve.png");

    private final TubeValveModel model;

    public TubeBufferRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new TubeValveModel(context.bakeLayer(TubeValveModel.LAYER_LOCATION));
    }

    @Override
    public TubeBufferRenderState createRenderState() {
        return new TubeBufferRenderState();
    }

    @Override
    public void extractRenderState(TileTubeBuffer tile, TubeBufferRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        System.arraycopy(tile.chokedSides, 0, state.chokedSides, 0, Math.min(tile.chokedSides.length, state.chokedSides.length));

        // Render choke indicators on each side
        for (Direction dir : Direction.values()) {
            // Only render if side is choked and open to a connectable tile
            if (state.chokedSides[dir.ordinal()] != 0 && tile.openSides[dir.ordinal()]) {
                BlockEntity connectedTile = tile.getLevel() != null
                        ? ThaumcraftApiHelper.getConnectableTile(tile.getLevel(), tile.getBlockPos(), dir)
                        : null;
                state.showIndicator[dir.ordinal()] = connectedTile != null;
            }
        }
    }

    @Override
    public void submit(TubeBufferRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        RenderType renderType = this.model.renderType(TEXTURE);

        for (Direction dir : Direction.values()) {
            if (!state.showIndicator[dir.ordinal()]) continue;

            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);

            // Rotate to face the direction
            applyDirectionRotation(poseStack, dir.getOpposite());

            // Determine color based on choke state
            int tintedColor;
            if (state.chokedSides[dir.ordinal()] == 2) {
                // No suction - red
                tintedColor = 0xFFFF4D4D;
            } else {
                // Weak suction - blue
                tintedColor = 0xFF4D4DFF;
            }

            // Scale and position
            poseStack.scale(0.0625f, 0.0625f, 0.0625f);
            poseStack.scale(2.0f, 1.0f, 2.0f);
            poseStack.translate(0, -0.5 / 0.0625, 0);  // Move down

            submitNodeCollector.submitModelPart(this.model.getRod(), poseStack, renderType,
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null, tintedColor, state.breakProgress);

            poseStack.popPose();
        }
    }

    /**
     * Apply rotation to face a direction.
     */
    private void applyDirectionRotation(PoseStack poseStack, Direction dir) {
        if (dir.getAxis() != Direction.Axis.Y) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
        } else {
            poseStack.mulPose(Axis.XN.rotationDegrees(90));
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0f * dir.getStepY()));
        }
        
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f * dir.getStepX()));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f * dir.getStepY()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f * dir.getStepZ()));
    }
}
