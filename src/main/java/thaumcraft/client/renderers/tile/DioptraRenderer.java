package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.tile.state.DioptraRenderState;
import thaumcraft.common.tiles.devices.TileDioptra;

import java.awt.Color;

/**
 * Block entity renderer for the Dioptra (vis/flux detector).
 * Renders a 3D heightmap grid showing vis or flux levels in surrounding chunks.
 */
@OnlyIn(Dist.CLIENT)
public class DioptraRenderer implements BlockEntityRenderer<TileDioptra, DioptraRenderState> {

    private static final Identifier GRID_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/gridblock.png");
    private static final Identifier SIDE_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/dioptra_side.png");

    public DioptraRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public DioptraRenderState createRenderState() {
        return new DioptraRenderState();
    }

    @Override
    public void extractRenderState(TileDioptra tile, DioptraRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        System.arraycopy(tile.grid_amt, 0, state.grid, 0, Math.min(tile.grid_amt.length, state.grid.length));
        state.counter = tile.counter;

        Minecraft mc = Minecraft.getInstance();
        float ticks = (mc.player != null ? mc.player.tickCount : 0) + partialTicks;

        // Determine color based on whether showing vis or flux
        boolean showingVis = tile.isDisplayingVis();
        if (showingVis) {
            // Vis: cyan/blue pulsing
            state.r = Mth.sin(ticks / 12.0f) * 0.05f + 0.85f;
            state.g = Mth.sin(ticks / 11.0f) * 0.05f + 0.9f;
            state.b = Mth.sin(ticks / 10.0f) * 0.05f + 0.95f;
        } else {
            // Flux: purple/magenta pulsing
            state.r = Mth.sin(ticks / 12.0f) * 0.05f + 0.85f;
            state.g = Mth.sin(ticks / 11.0f) * 0.05f + 0.45f;
            state.b = Mth.sin(ticks / 10.0f) * 0.05f + 0.95f;
        }
    }

    @Override
    public void submit(DioptraRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Render the 3D grid
        renderGrid(state, poseStack, submitNodeCollector);

        // Render the side panels
        renderSidePanels(state, poseStack, submitNodeCollector);

        poseStack.popPose();
    }

    private void renderGrid(DioptraRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        poseStack.pushPose();
        poseStack.translate(-0.495, 0.501, -0.495);
        poseStack.scale(0.99f, 1.0f, 0.99f);

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(GRID_TEXTURE), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            int fullLight = 0x00F000F0; // Full brightness for emissive effect

            // Render 12x12 grid quads
            for (int a = 0; a < 12; a++) {
                for (int bb = 0; bb < 12; bb++) {
                    // Calculate heights at each corner
                    float h00 = (state.grid[a + bb * 13] & 0xFF) / 96.0f;
                    float h10 = (state.grid[a + 1 + bb * 13] & 0xFF) / 96.0f;
                    float h11 = (state.grid[a + 1 + (bb + 1) * 13] & 0xFF) / 96.0f;
                    float h01 = (state.grid[a + (bb + 1) * 13] & 0xFF) / 96.0f;

                    // Position coordinates (0 to 1)
                    float x0 = a / 12.0f;
                    float x1 = (a + 1) / 12.0f;
                    float z0 = bb / 12.0f;
                    float z1 = (bb + 1) / 12.0f;

                    // Add wave animation
                    double d3 = a - 6;
                    double d4 = bb - 6;
                    double dis = Math.sqrt(d3 * d3 + d4 * d4);
                    float wave = Mth.sin((float) ((state.counter - dis * 10.0) / 8.0));
                    float brightness = 200.0f + wave * 15.0f;
                    int light = (int) brightness << 4 | (int) brightness << 20;

                    // Top face quad
                    Color c = new Color(state.r * 0.8f, state.g, state.b);
                    float cr = c.getRed() / 255.0f;
                    float cg = c.getGreen() / 255.0f;
                    float cb = c.getBlue() / 255.0f;
                    float alpha = 0.9f;

                    // Render quad (counter-clockwise for correct facing)
                    buffer.addVertex(matrix, x0, h00, z0).setColor(cr, cg, cb, alpha)
                            .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 1, 0);
                    buffer.addVertex(matrix, x0, h01, z1).setColor(cr, cg, cb, alpha)
                            .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 1, 0);
                    buffer.addVertex(matrix, x1, h11, z1).setColor(cr, cg, cb, alpha)
                            .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 1, 0);
                    buffer.addVertex(matrix, x1, h10, z0).setColor(cr, cg, cb, alpha)
                            .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 1, 0);

                    // Render edge walls for outer edges
                    if (a == 0) {
                        // West edge
                        buffer.addVertex(matrix, 0, 0, z0).setColor(cr, cg, cb, 0f)
                                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(-1, 0, 0);
                        buffer.addVertex(matrix, 0, h00, z0).setColor(cr, cg, cb, alpha)
                                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(-1, 0, 0);
                        buffer.addVertex(matrix, 0, h01, z1).setColor(cr, cg, cb, alpha)
                                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(-1, 0, 0);
                        buffer.addVertex(matrix, 0, 0, z1).setColor(cr, cg, cb, 0f)
                                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(-1, 0, 0);
                    }
                    if (a == 11) {
                        // East edge
                        buffer.addVertex(matrix, 1, 0, z0).setColor(cr, cg, cb, 0f)
                                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(1, 0, 0);
                        buffer.addVertex(matrix, 1, 0, z1).setColor(cr, cg, cb, 0f)
                                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(1, 0, 0);
                        buffer.addVertex(matrix, 1, h11, z1).setColor(cr, cg, cb, alpha)
                                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(1, 0, 0);
                        buffer.addVertex(matrix, 1, h10, z0).setColor(cr, cg, cb, alpha)
                                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(1, 0, 0);
                    }
                    if (bb == 0) {
                        // North edge
                        buffer.addVertex(matrix, x0, 0, 0).setColor(cr, cg, cb, 0f)
                                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, -1);
                        buffer.addVertex(matrix, x1, 0, 0).setColor(cr, cg, cb, 0f)
                                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, -1);
                        buffer.addVertex(matrix, x1, h10, 0).setColor(cr, cg, cb, alpha)
                                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, -1);
                        buffer.addVertex(matrix, x0, h00, 0).setColor(cr, cg, cb, alpha)
                                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, -1);
                    }
                    if (bb == 11) {
                        // South edge
                        buffer.addVertex(matrix, x0, 0, 1).setColor(cr, cg, cb, 0f)
                                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, 1);
                        buffer.addVertex(matrix, x0, h01, 1).setColor(cr, cg, cb, alpha)
                                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, 1);
                        buffer.addVertex(matrix, x1, h11, 1).setColor(cr, cg, cb, alpha)
                                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, 1);
                        buffer.addVertex(matrix, x1, 0, 1).setColor(cr, cg, cb, 0f)
                                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, 1);
                    }
                }
            }
        });

        poseStack.popPose();
    }

    private void renderSidePanels(DioptraRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        poseStack.pushPose();
        poseStack.translate(0, 1.0, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(270));

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(SIDE_TEXTURE), (pose, buffer) -> {
            int fullLight = 0x00F000F0;

            // Render 4 side panels
            for (int q = 0; q < 4; q++) {
                Matrix4f matrix = new Matrix4f(pose.pose());
                matrix.rotate(Axis.XP.rotationDegrees(90.0f * q));
                matrix.translate(0, 0, -0.5f);

                renderCenteredQuad(buffer, matrix, 1.0f, state.r, state.g, state.b, 0.8f, fullLight);
            }
        });

        poseStack.popPose();
    }

    private void renderCenteredQuad(VertexConsumer consumer, Matrix4f matrix,
                                    float size, float r, float g, float b, float a, int light) {
        float half = size / 2.0f;

        consumer.addVertex(matrix, -half, -half, 0).setColor(r, g, b, a)
                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, half, -half, 0).setColor(r, g, b, a)
                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, half, half, 0).setColor(r, g, b, a)
                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, -half, half, 0).setColor(r, g, b, a)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
