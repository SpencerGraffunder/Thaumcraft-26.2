package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.client.renderers.tile.state.AlembicRenderState;
import thaumcraft.common.tiles.essentia.TileAlembic;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Block entity renderer for the Alembic.
 * Renders the aspect filter label and connection nozzles to adjacent tubes.
 */
@OnlyIn(Dist.CLIENT)
public class AlembicRenderer implements BlockEntityRenderer<TileAlembic, AlembicRenderState> {

    private static final Identifier LABEL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/label.png");

    public AlembicRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AlembicRenderState createRenderState() {
        return new AlembicRenderState();
    }

    @Override
    public void extractRenderState(TileAlembic tile, AlembicRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.aspectFilter = tile.getAspectFilter();
        state.facing = tile.getFacing();
        List<Direction> nozzles = new ArrayList<>();
        if (tile.getLevel() != null) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (tile.canOutputTo(dir)) {
                    BlockEntity te = tile.getLevel().getBlockEntity(tile.getBlockPos().relative(dir));
                    if (te instanceof IEssentiaTransport transport && transport.canInputFrom(dir.getOpposite())) {
                        nozzles.add(dir);
                    }
                }
            }
        }
        state.nozzles = nozzles.toArray(new Direction[0]);
    }

    @Override
    public void submit(AlembicRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        // Render aspect filter label if set
        if (state.aspectFilter != null) {
            renderLabel(state, poseStack, submitNodeCollector);
        }

        // Render connection nozzles to adjacent essentia transport blocks
        for (Direction dir : state.nozzles) {
            renderNozzle(dir, poseStack, submitNodeCollector, state.lightCoords);
        }
    }

    /**
     * Render the aspect filter label on the front of the alembic.
     */
    private void renderLabel(AlembicRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        Aspect aspect = state.aspectFilter;

        poseStack.pushPose();

        // Move to block center
        poseStack.translate(0.5, 0.0, 0.5);

        // Rotate based on alembic facing
        int facing = state.facing;
        switch (facing) {
            case 2 -> poseStack.mulPose(Axis.YP.rotationDegrees(180)); // North - face south
            case 3 -> poseStack.mulPose(Axis.YP.rotationDegrees(0));   // South - face north
            case 4 -> poseStack.mulPose(Axis.YP.rotationDegrees(90));  // West - face east
            case 5 -> poseStack.mulPose(Axis.YP.rotationDegrees(270)); // East - face west
        }

        // Move to label position on front face
        poseStack.translate(0, 0.5, -0.376);

        // Render label background
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(LABEL_TEXTURE), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float size = 0.22f;
            buffer.addVertex(matrix, -size, -size, 0).setColor(255, 255, 255, 255)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, -size, 0).setColor(255, 255, 255, 255)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, size, 0).setColor(255, 255, 255, 255)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
            buffer.addVertex(matrix, -size, size, 0).setColor(255, 255, 255, 255)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
        });

        // Render aspect icon on label
        poseStack.translate(0, 0, -0.001); // Slightly in front

        // Get aspect color and render colored quad
        Color color = new Color(aspect.getColor());
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        // Render aspect icon using the aspect's image
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(aspect.getImage()), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float iconSize = 0.15f;
            buffer.addVertex(matrix, -iconSize, -iconSize, 0).setColor(r, g, b, 1f)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
            buffer.addVertex(matrix, iconSize, -iconSize, 0).setColor(r, g, b, 1f)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
            buffer.addVertex(matrix, iconSize, iconSize, 0).setColor(r, g, b, 1f)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
            buffer.addVertex(matrix, -iconSize, iconSize, 0).setColor(r, g, b, 1f)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
        });

        poseStack.popPose();
    }

    /**
     * Render a single nozzle in the specified direction.
     */
    private void renderNozzle(Direction dir, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
        poseStack.pushPose();

        // Move to block center
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate based on direction
        switch (dir) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> { } // No rotation needed
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            default -> { }
        }

        // Move to edge of block
        poseStack.translate(0, 0, 0.5);

        // Render a simple nozzle quad (placeholder - could use a model)
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float size = 0.125f;
            float depth = 0.0625f;

            // Nozzle is a small box extruding from the block
            // Front face
            buffer.addVertex(matrix, -size, -size, depth).setColor(100, 80, 60, 255)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, -size, depth).setColor(100, 80, 60, 255)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, size, depth).setColor(100, 80, 60, 255)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, -size, size, depth).setColor(100, 80, 60, 255)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        });

        poseStack.popPose();
    }
}
