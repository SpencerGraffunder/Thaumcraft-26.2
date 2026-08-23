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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.tile.state.ResearchTableRenderState;
import thaumcraft.common.tiles.crafting.TileResearchTable;

import java.awt.Color;

/**
 * Block entity renderer for the Research Table.
 * Renders scroll/paper items on the table surface when research is in progress.
 */
@OnlyIn(Dist.CLIENT)
public class ResearchTableRenderer implements BlockEntityRenderer<TileResearchTable, ResearchTableRenderState> {

    private static final Identifier SCROLL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/scroll.png");
    private static final Identifier INKWELL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/inkwell.png");

    public ResearchTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public ResearchTableRenderState createRenderState() {
        return new ResearchTableRenderState();
    }

    @Override
    public void extractRenderState(TileResearchTable tile, ResearchTableRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.facing = getFacing(tile.getBlockState());
        state.hasResearch = tile.hasResearchData();
        state.hasScribeTools = tile.hasScribeTools();
    }

    @Override
    public void submit(ResearchTableRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        // Move to table surface
        poseStack.translate(0.5, 1.0, 0.5);

        // Rotate based on table facing
        applyRotation(poseStack, state.facing);

        // Render scroll if there's research data
        if (state.hasResearch) {
            renderScroll(poseStack, submitNodeCollector, state.lightCoords);
        }

        // Render inkwell if scribe tools are present
        if (state.hasScribeTools) {
            renderInkwell(poseStack, submitNodeCollector, state.lightCoords);
        }

        poseStack.popPose();
    }

    /**
     * Render a scroll/paper on the table surface.
     */
    private void renderScroll(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
        poseStack.pushPose();

        // Position scroll on table
        poseStack.translate(0, 0.02, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90)); // Lay flat
        poseStack.scale(0.5f, 0.5f, 0.5f);

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(SCROLL_TEXTURE), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Render scroll as a flat quad
            float size = 0.5f;
            Color color = new Color(Aspect.ALCHEMY.getColor());
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;

            buffer.addVertex(matrix, -size, -size, 0).setColor(r, g, b, 1.0f)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
            buffer.addVertex(matrix, size, -size, 0).setColor(r, g, b, 1.0f)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
            buffer.addVertex(matrix, size, size, 0).setColor(r, g, b, 1.0f)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
            buffer.addVertex(matrix, -size, size, 0).setColor(r, g, b, 1.0f)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
        });

        poseStack.popPose();
    }

    /**
     * Render an inkwell with quill on the table.
     */
    private void renderInkwell(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
        poseStack.pushPose();

        // Position inkwell offset from center
        poseStack.translate(-0.3, 0.02, 0.2);
        poseStack.scale(0.2f, 0.2f, 0.2f);

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(INKWELL_TEXTURE), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Simple cube for inkwell
            float size = 0.5f;

            // Top face
            buffer.addVertex(matrix, -size, size, -size).setColor(255, 255, 255, 255)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
            buffer.addVertex(matrix, -size, size, size).setColor(255, 255, 255, 255)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
            buffer.addVertex(matrix, size, size, size).setColor(255, 255, 255, 255)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
            buffer.addVertex(matrix, size, size, -size).setColor(255, 255, 255, 255)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
        });

        poseStack.popPose();
    }

    private void applyRotation(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            default -> { } // NORTH is default
        }
    }

    private Direction getFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }
}
