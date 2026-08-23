package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.renderers.tile.state.VoidSiphonRenderState;
import thaumcraft.common.tiles.crafting.TileVoidSiphon;

/**
 * Block entity renderer for the Void Siphon.
 * Renders a void portal-like effect when the siphon is active.
 */
@OnlyIn(Dist.CLIENT)
public class VoidSiphonRenderer implements BlockEntityRenderer<TileVoidSiphon, VoidSiphonRenderState> {

    public VoidSiphonRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public VoidSiphonRenderState createRenderState() {
        return new VoidSiphonRenderState();
    }

    @Override
    public void extractRenderState(TileVoidSiphon tile, VoidSiphonRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        // Only render the void effect when enabled (not powered by redstone)
        state.enabled = tile.getLevel() == null || !tile.getLevel().hasNeighborSignal(tile.getBlockPos());
    }

    @Override
    public void submit(VoidSiphonRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.enabled) return;

        // Render void orb at top
        renderVoidOrb(poseStack, submitNodeCollector, 0.875f, 0.25f);

        // Render void orb in center/bottom
        renderVoidOrb(poseStack, submitNodeCollector, 0.3125f, 0.5f);
    }

    /**
     * Render a void orb at the specified height.
     */
    private void renderVoidOrb(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float height, float size) {
        poseStack.pushPose();
        poseStack.translate(0.5, height, 0.5);

        // Use end portal render type for the starfield effect
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.endPortal(), (pose, buffer) -> {
            // Render cube faces
            for (Direction face : Direction.values()) {
                Matrix4f matrix = new Matrix4f(pose.pose());

                // Rotate to face direction
                switch (face) {
                    case DOWN -> matrix.rotate(Axis.XP.rotationDegrees(90));
                    case UP -> matrix.rotate(Axis.XP.rotationDegrees(-90));
                    case NORTH -> { /* Default */ }
                    case SOUTH -> matrix.rotate(Axis.YP.rotationDegrees(180));
                    case WEST -> matrix.rotate(Axis.YP.rotationDegrees(90));
                    case EAST -> matrix.rotate(Axis.YP.rotationDegrees(-90));
                }

                // Adjust size based on face type
                float faceSize = size;
                if (face.getAxis() == Direction.Axis.Z) {
                    // Z faces are not square, they stretch vertically
                    matrix.rotate(Axis.ZN.rotationDegrees(90));
                }

                // Move to face position
                float offset = face == Direction.DOWN || face == Direction.UP ? 0.126f : 0.26f;
                matrix.translate(0, 0, face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? -offset : offset);

                matrix.scale(faceSize, faceSize, faceSize);

                float half = 0.5f;

                buffer.addVertex(matrix, -half, -half, 0);
                buffer.addVertex(matrix, half, -half, 0);
                buffer.addVertex(matrix, half, half, 0);
                buffer.addVertex(matrix, -half, half, 0);
            }
        });

        poseStack.popPose();
    }
}
