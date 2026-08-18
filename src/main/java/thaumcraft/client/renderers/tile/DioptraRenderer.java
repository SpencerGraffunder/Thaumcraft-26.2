package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.devices.TileDioptra;

import java.awt.Color;

/**
 * Block entity renderer for the Dioptra (vis/flux detector).
 * Renders a 3D heightmap grid showing vis or flux levels in surrounding chunks.
 */
@OnlyIn(Dist.CLIENT)
public class DioptraRenderer implements BlockEntityRenderer<TileDioptra> {

    private static final Identifier GRID_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/gridblock.png");
    private static final Identifier SIDE_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/dioptra_side.png");

    public DioptraRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileDioptra tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        if (tile.getLevel() == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        float ticks = (mc.player != null ? mc.player.tickCount : 0) + partialTicks;
        
        // Determine color based on whether showing vis or flux
        boolean showingVis = tile.isDisplayingVis();
        float rc, gc, bc;
        
        if (showingVis) {
            // Vis: cyan/blue pulsing
            rc = Mth.sin(ticks / 12.0f) * 0.05f + 0.85f;
            gc = Mth.sin(ticks / 11.0f) * 0.05f + 0.9f;
            bc = Mth.sin(ticks / 10.0f) * 0.05f + 0.95f;
        } else {
            // Flux: purple/magenta pulsing
            rc = Mth.sin(ticks / 12.0f) * 0.05f + 0.85f;
            gc = Mth.sin(ticks / 11.0f) * 0.05f + 0.45f;
            bc = Mth.sin(ticks / 10.0f) * 0.05f + 0.95f;
        }
        
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        
        // Render the 3D grid
        renderGrid(tile, poseStack, buffer, rc, gc, bc, ticks, packedLight);
        
        // Render the side panels
        renderSidePanels(poseStack, buffer, rc, gc, bc, packedLight);
        
        poseStack.popPose();
    }
    
    private void renderGrid(TileDioptra tile, PoseStack poseStack, MultiBufferSource buffer,
                            float r, float g, float b, float ticks, int packedLight) {
        
        poseStack.pushPose();
        poseStack.translate(-0.495, 0.501, -0.495);
        poseStack.scale(0.99f, 1.0f, 0.99f);
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(GRID_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        
        int fullLight = 0x00F000F0; // Full brightness for emissive effect
        
        // Render 12x12 grid quads
        for (int a = 0; a < 12; a++) {
            for (int bb = 0; bb < 12; bb++) {
                // Calculate heights at each corner
                float h00 = tile.getGridValue(a, bb) / 96.0f;
                float h10 = tile.getGridValue(a + 1, bb) / 96.0f;
                float h11 = tile.getGridValue(a + 1, bb + 1) / 96.0f;
                float h01 = tile.getGridValue(a, bb + 1) / 96.0f;
                
                // Position coordinates (0 to 1)
                float x0 = a / 12.0f;
                float x1 = (a + 1) / 12.0f;
                float z0 = bb / 12.0f;
                float z1 = (bb + 1) / 12.0f;
                
                // Add wave animation
                double d3 = a - 6;
                double d4 = bb - 6;
                double dis = Math.sqrt(d3 * d3 + d4 * d4);
                float wave = Mth.sin((float)((tile.counter - dis * 10.0) / 8.0));
                float brightness = 200.0f + wave * 15.0f;
                int light = (int)brightness << 4 | (int)brightness << 20;
                
                // Top face quad
                Color c = new Color(r * 0.8f, g, b);
                float cr = c.getRed() / 255.0f;
                float cg = c.getGreen() / 255.0f;
                float cb = c.getBlue() / 255.0f;
                float alpha = 0.9f;
                
                // Render quad (counter-clockwise for correct facing)
                vertexConsumer.vertex(matrix, x0, h00, z0).color(cr, cg, cb, alpha)
                        .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 1, 0).endVertex();
                vertexConsumer.vertex(matrix, x0, h01, z1).color(cr, cg, cb, alpha)
                        .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 1, 0).endVertex();
                vertexConsumer.vertex(matrix, x1, h11, z1).color(cr, cg, cb, alpha)
                        .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 1, 0).endVertex();
                vertexConsumer.vertex(matrix, x1, h10, z0).color(cr, cg, cb, alpha)
                        .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 1, 0).endVertex();
                
                // Render edge walls for outer edges
                if (a == 0) {
                    // West edge
                    vertexConsumer.vertex(matrix, 0, 0, z0).color(cr, cg, cb, 0f)
                            .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(-1, 0, 0).endVertex();
                    vertexConsumer.vertex(matrix, 0, h00, z0).color(cr, cg, cb, alpha)
                            .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(-1, 0, 0).endVertex();
                    vertexConsumer.vertex(matrix, 0, h01, z1).color(cr, cg, cb, alpha)
                            .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(-1, 0, 0).endVertex();
                    vertexConsumer.vertex(matrix, 0, 0, z1).color(cr, cg, cb, 0f)
                            .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(-1, 0, 0).endVertex();
                }
                if (a == 11) {
                    // East edge
                    vertexConsumer.vertex(matrix, 1, 0, z0).color(cr, cg, cb, 0f)
                            .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(1, 0, 0).endVertex();
                    vertexConsumer.vertex(matrix, 1, 0, z1).color(cr, cg, cb, 0f)
                            .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(1, 0, 0).endVertex();
                    vertexConsumer.vertex(matrix, 1, h11, z1).color(cr, cg, cb, alpha)
                            .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(1, 0, 0).endVertex();
                    vertexConsumer.vertex(matrix, 1, h10, z0).color(cr, cg, cb, alpha)
                            .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(1, 0, 0).endVertex();
                }
                if (bb == 0) {
                    // North edge
                    vertexConsumer.vertex(matrix, x0, 0, 0).color(cr, cg, cb, 0f)
                            .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 0, -1).endVertex();
                    vertexConsumer.vertex(matrix, x1, 0, 0).color(cr, cg, cb, 0f)
                            .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 0, -1).endVertex();
                    vertexConsumer.vertex(matrix, x1, h10, 0).color(cr, cg, cb, alpha)
                            .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 0, -1).endVertex();
                    vertexConsumer.vertex(matrix, x0, h00, 0).color(cr, cg, cb, alpha)
                            .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 0, -1).endVertex();
                }
                if (bb == 11) {
                    // South edge
                    vertexConsumer.vertex(matrix, x0, 0, 1).color(cr, cg, cb, 0f)
                            .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 0, 1).endVertex();
                    vertexConsumer.vertex(matrix, x0, h01, 1).color(cr, cg, cb, alpha)
                            .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 0, 1).endVertex();
                    vertexConsumer.vertex(matrix, x1, h11, 1).color(cr, cg, cb, alpha)
                            .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 0, 1).endVertex();
                    vertexConsumer.vertex(matrix, x1, 0, 1).color(cr, cg, cb, 0f)
                            .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(0, 0, 1).endVertex();
                }
            }
        }
        
        poseStack.popPose();
    }
    
    private void renderSidePanels(PoseStack poseStack, MultiBufferSource buffer, 
                                   float r, float g, float b, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0, 1.0, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(270));
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(SIDE_TEXTURE));
        int fullLight = 0x00F000F0;
        
        // Render 4 side panels
        for (int q = 0; q < 4; q++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f * q));
            poseStack.translate(0, 0, -0.5);
            
            renderCenteredQuad(poseStack, vertexConsumer, 1.0f, r, g, b, 0.8f, fullLight);
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
    }
    
    private void renderCenteredQuad(PoseStack poseStack, VertexConsumer consumer, 
                                     float size, float r, float g, float b, float a, int light) {
        Matrix4f matrix = poseStack.last().pose();
        float half = size / 2.0f;
        
        consumer.vertex(matrix, -half, -half, 0).color(r, g, b, a)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix, half, -half, 0).color(r, g, b, a)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix, half, half, 0).color(r, g, b, a)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix, -half, half, 0).color(r, g, b, a)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
    }
    
    @Override
    public boolean shouldRenderOffScreen(TileDioptra tile) {
        return true;
    }
}
