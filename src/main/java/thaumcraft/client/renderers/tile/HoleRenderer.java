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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.renderers.tile.state.HoleRenderState;
import thaumcraft.common.tiles.misc.TileHole;
import thaumcraft.init.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Block entity renderer for the Portable Hole.
 * Renders an end portal-like void effect on faces adjacent to solid blocks.
 */
@OnlyIn(Dist.CLIENT)
public class HoleRenderer implements BlockEntityRenderer<TileHole, HoleRenderState> {

    public HoleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public HoleRenderState createRenderState() {
        return new HoleRenderState();
    }

    @Override
    public void extractRenderState(TileHole tile, HoleRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        List<Direction> faces = new ArrayList<>();
        if (tile.getLevel() != null) {
            // Render void face on each side adjacent to a solid block
            for (Direction face : Direction.values()) {
                BlockState adjacentState = tile.getLevel().getBlockState(tile.getBlockPos().relative(face));

                // Only render the void face if the adjacent block is opaque and not another hole
                if (adjacentState.isSolidRender()
                        && !adjacentState.is(ModBlocks.HOLE.get())) {
                    faces.add(face);
                }
            }
        }
        state.faces = faces.toArray(new Direction[0]);
    }

    @Override
    public void submit(HoleRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        for (Direction face : state.faces) {
            poseStack.pushPose();

            // Rotate to face the correct direction
            switch (face) {
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                case NORTH -> { /* Default facing */ }
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            }

            // Move to the face
            poseStack.translate(0, 0, 0.499);

            // Render a single void/portal-like face
            // Use end portal render type for the starfield effect
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.endPortal(), (pose, buffer) -> {
                Matrix4f matrix = pose.pose();

                float half = 0.5f;

                // Render quad facing +Z (towards the solid block)
                // End portal render type handles its own texturing
                buffer.addVertex(matrix, -half, -half, 0);
                buffer.addVertex(matrix, half, -half, 0);
                buffer.addVertex(matrix, half, half, 0);
                buffer.addVertex(matrix, -half, half, 0);
            });

            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
