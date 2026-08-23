package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.tile.state.JarRenderState;
import thaumcraft.common.tiles.essentia.TileJar;

import java.awt.Color;

/**
 * Block entity renderer for Warded Jars.
 * Renders the essentia liquid level and aspect filter label.
 */
@OnlyIn(Dist.CLIENT)
public class JarRenderer implements BlockEntityRenderer<TileJar, JarRenderState> {

    private static final Identifier LIQUID_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/animatedglow.png");
    private static final Identifier LABEL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/label.png");

    public JarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public JarRenderState createRenderState() {
        return new JarRenderState();
    }

    @Override
    public void extractRenderState(TileJar tile, JarRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.amount = tile.getAmount();
        state.aspect = tile.getAspect();
        state.aspectFilter = tile.getAspectFilter();
        state.facing = tile.getFacing();
    }

    @Override
    public void submit(JarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        // Render essentia liquid if jar has contents
        if (state.amount > 0 && state.aspect != null) {
            renderLiquid(state, poseStack, submitNodeCollector);
        }

        // Render aspect filter label if set
        Aspect filter = state.aspectFilter;
        if (filter != null) {
            renderLabel(state, filter, poseStack, submitNodeCollector);
        }
    }

    /**
     * Render the essentia liquid inside the jar.
     */
    private void renderLiquid(JarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        Aspect aspect = state.aspect;

        int amount = state.amount;
        float fillLevel = amount / 250.0f; // 0 to 1
        float liquidHeight = 0.0625f + fillLevel * 0.625f; // Height in block units

        // Get aspect color
        Color color = new Color(aspect.getColor());
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = 0.8f; // Slightly transparent

        // Render a colored cube for the liquid
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Liquid bounds: 0.25 to 0.75 horizontally, 0.0625 to liquidHeight vertically
            float minX = 0.251f;
            float maxX = 0.749f;
            float minY = 0.0625f;
            float maxY = liquidHeight;
            float minZ = 0.251f;
            float maxZ = 0.749f;

            int light = 0x00F000F0; // Full brightness for glowing essentia

            // Bottom face (Y-)
            addQuad(buffer, matrix,
                    minX, minY, minZ, maxX, minY, maxZ,
                    0, -1, 0, r, g, b, a, light);

            // Top face (Y+)
            addQuad(buffer, matrix,
                    minX, maxY, maxZ, maxX, maxY, minZ,
                    0, 1, 0, r, g, b, a, light);

            // North face (Z-)
            addQuad(buffer, matrix,
                    maxX, minY, minZ, minX, maxY, minZ,
                    0, 0, -1, r, g, b, a, light);

            // South face (Z+)
            addQuad(buffer, matrix,
                    minX, minY, maxZ, maxX, maxY, maxZ,
                    0, 0, 1, r, g, b, a, light);

            // West face (X-)
            addQuad(buffer, matrix,
                    minX, minY, minZ, minX, maxY, maxZ,
                    -1, 0, 0, r, g, b, a, light);

            // East face (X+)
            addQuad(buffer, matrix,
                    maxX, minY, maxZ, maxX, maxY, minZ,
                    1, 0, 0, r, g, b, a, light);
        });
    }

    /**
     * Add a quad to the vertex consumer.
     */
    private void addQuad(VertexConsumer consumer, Matrix4f matrix,
                         float x1, float y1, float z1, float x2, float y2, float z2,
                         float nx, float ny, float nz, float r, float g, float b, float a, int light) {

        // Calculate the 4 corners based on the face direction
        float minU = 0, maxU = 1, minV = 0, maxV = 1;

        if (ny != 0) {
            // Horizontal face (top/bottom)
            consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(minU, minV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(minU, maxV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(maxU, maxV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(maxU, minV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
        } else if (nz != 0) {
            // Z-facing face (north/south)
            consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(minU, maxV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(maxU, maxV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(maxU, minV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(minU, minV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
        } else {
            // X-facing face (east/west)
            consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(minU, maxV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(maxU, maxV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(maxU, minV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(minU, minV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
        }
    }

    /**
     * Render the label with aspect icon on the front of the jar.
     */
    private void renderLabel(JarRenderState state, Aspect aspect, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        poseStack.pushPose();

        // Move to center of jar
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate based on facing direction
        int facing = state.facing;
        switch (facing) {
            case 2 -> poseStack.mulPose(Axis.YP.rotationDegrees(0));    // North
            case 3 -> poseStack.mulPose(Axis.YP.rotationDegrees(180));  // South
            case 4 -> poseStack.mulPose(Axis.YP.rotationDegrees(90));   // West
            case 5 -> poseStack.mulPose(Axis.YP.rotationDegrees(270));  // East
        }

        // Move to front of jar
        poseStack.translate(0, -0.1, 0.315);

        // Scale down for the label
        poseStack.scale(0.5f, 0.5f, 0.5f);

        // Render a label background quad
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(LABEL_TEXTURE), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float size = 0.5f;
            buffer.addVertex(matrix, -size, -size, 0).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, -size, 0).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, size, 0).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
            buffer.addVertex(matrix, -size, size, 0).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
        });

        // Render the aspect icon on the label
        poseStack.translate(0, 0, 0.01); // Slightly in front of label
        poseStack.scale(0.03f, 0.03f, 0.03f); // Scale for aspect icon

        // The aspect icon rendering would go here using AspectRenderer
        // For now we'll just render a colored square as placeholder
        Color color = new Color(aspect.getColor());
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(aspect.getImage()), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float iconSize = 8;
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;

            buffer.addVertex(matrix, -iconSize, -iconSize, 0).setColor(r, g, b, 1f).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
            buffer.addVertex(matrix, iconSize, -iconSize, 0).setColor(r, g, b, 1f).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
            buffer.addVertex(matrix, iconSize, iconSize, 0).setColor(r, g, b, 1f).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
            buffer.addVertex(matrix, -iconSize, iconSize, 0).setColor(r, g, b, 1f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
        });

        poseStack.popPose();
    }
}
