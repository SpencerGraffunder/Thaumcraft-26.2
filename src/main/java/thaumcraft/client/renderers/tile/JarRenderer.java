package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.AspectRenderer;
import thaumcraft.common.tiles.essentia.TileJar;

import java.awt.Color;

/**
 * Block entity renderer for Warded Jars.
 * Renders the essentia liquid level and aspect filter label.
 */
@OnlyIn(Dist.CLIENT)
public class JarRenderer implements BlockEntityRenderer<TileJar> {

    private static final Identifier LIQUID_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/animatedglow.png");
    private static final Identifier LABEL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/label.png");

    public JarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileJar tile, float partialTicks, PoseStack poseStack, 
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        // Render essentia liquid if jar has contents
        if (tile.getAmount() > 0) {
            renderLiquid(tile, partialTicks, poseStack, buffer, packedLight);
        }
        
        // Render aspect filter label if set
        Aspect filter = tile.getAspectFilter();
        if (filter != null) {
            renderLabel(tile, filter, partialTicks, poseStack, buffer, packedLight);
        }
    }

    /**
     * Render the essentia liquid inside the jar.
     */
    private void renderLiquid(TileJar tile, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight) {
        
        Aspect aspect = tile.getAspect();
        if (aspect == null) return;
        
        int amount = tile.getAmount();
        float fillLevel = amount / 250.0f; // 0 to 1
        float liquidHeight = 0.0625f + fillLevel * 0.625f; // Height in block units
        
        // Get aspect color
        Color color = new Color(aspect.getColor());
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = 0.8f; // Slightly transparent
        
        poseStack.pushPose();
        
        // Render a colored cube for the liquid
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();
        
        // Liquid bounds: 0.25 to 0.75 horizontally, 0.0625 to liquidHeight vertically
        float minX = 0.251f;
        float maxX = 0.749f;
        float minY = 0.0625f;
        float maxY = liquidHeight;
        float minZ = 0.251f;
        float maxZ = 0.749f;
        
        int light = 0x00F000F0; // Full brightness for glowing essentia
        
        // Bottom face (Y-)
        addQuad(vertexConsumer, matrix, 
                minX, minY, minZ, maxX, minY, maxZ, 
                0, -1, 0, r, g, b, a, light);
        
        // Top face (Y+)
        addQuad(vertexConsumer, matrix, 
                minX, maxY, maxZ, maxX, maxY, minZ, 
                0, 1, 0, r, g, b, a, light);
        
        // North face (Z-)
        addQuad(vertexConsumer, matrix, 
                maxX, minY, minZ, minX, maxY, minZ, 
                0, 0, -1, r, g, b, a, light);
        
        // South face (Z+)
        addQuad(vertexConsumer, matrix, 
                minX, minY, maxZ, maxX, maxY, maxZ, 
                0, 0, 1, r, g, b, a, light);
        
        // West face (X-)
        addQuad(vertexConsumer, matrix, 
                minX, minY, minZ, minX, maxY, maxZ, 
                -1, 0, 0, r, g, b, a, light);
        
        // East face (X+)
        addQuad(vertexConsumer, matrix, 
                maxX, minY, maxZ, maxX, maxY, minZ, 
                1, 0, 0, r, g, b, a, light);
        
        poseStack.popPose();
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
            consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).uv(minU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x1, y1, z2).color(r, g, b, a).uv(minU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x2, y1, z2).color(r, g, b, a).uv(maxU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x2, y1, z1).color(r, g, b, a).uv(maxU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        } else if (nz != 0) {
            // Z-facing face (north/south)
            consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).uv(minU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x2, y1, z1).color(r, g, b, a).uv(maxU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x2, y2, z1).color(r, g, b, a).uv(maxU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x1, y2, z1).color(r, g, b, a).uv(minU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        } else {
            // X-facing face (east/west)
            consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).uv(minU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x1, y1, z2).color(r, g, b, a).uv(maxU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x1, y2, z2).color(r, g, b, a).uv(maxU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x1, y2, z1).color(r, g, b, a).uv(minU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        }
    }

    /**
     * Render the label with aspect icon on the front of the jar.
     */
    private void renderLabel(TileJar tile, Aspect aspect, float partialTicks, 
                             PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        
        poseStack.pushPose();
        
        // Move to center of jar
        poseStack.translate(0.5, 0.5, 0.5);
        
        // Rotate based on facing direction
        int facing = tile.getFacing();
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
        VertexConsumer labelConsumer = buffer.getBuffer(RenderType.entityCutout(LABEL_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        
        float size = 0.5f;
        labelConsumer.vertex(matrix, -size, -size, 0).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        labelConsumer.vertex(matrix, size, -size, 0).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        labelConsumer.vertex(matrix, size, size, 0).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        labelConsumer.vertex(matrix, -size, size, 0).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        
        // Render the aspect icon on the label
        poseStack.translate(0, 0, 0.01); // Slightly in front of label
        poseStack.scale(0.03f, 0.03f, 0.03f); // Scale for aspect icon
        
        // The aspect icon rendering would go here using AspectRenderer
        // For now we'll just render a colored square as placeholder
        Color color = new Color(aspect.getColor());
        VertexConsumer iconConsumer = buffer.getBuffer(RenderType.entitySolid(aspect.getImage()));
        Matrix4f iconMatrix = poseStack.last().pose();
        
        float iconSize = 8;
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        
        iconConsumer.vertex(iconMatrix, -iconSize, -iconSize, 0).color(r, g, b, 1f).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
        iconConsumer.vertex(iconMatrix, iconSize, -iconSize, 0).color(r, g, b, 1f).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
        iconConsumer.vertex(iconMatrix, iconSize, iconSize, 0).color(r, g, b, 1f).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
        iconConsumer.vertex(iconMatrix, -iconSize, iconSize, 0).color(r, g, b, 1f).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
        
        poseStack.popPose();
    }
}
