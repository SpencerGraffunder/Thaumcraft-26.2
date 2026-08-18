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
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;

import java.util.Random;

/**
 * Block entity renderer for the Infusion Matrix.
 * Renders 8 floating cubes that pulse and wobble during crafting.
 * Also renders energy halo effect during active crafting.
 */
@OnlyIn(Dist.CLIENT)
public class InfusionMatrixRenderer implements BlockEntityRenderer<TileInfusionMatrix> {

    private static final Identifier TEXTURE_NORMAL = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/infuser_normal.png");
    private static final Identifier TEXTURE_ANCIENT = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/infuser_ancient.png");
    private static final Identifier TEXTURE_ELDRITCH = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/infuser_eldritch.png");

    public InfusionMatrixRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileInfusionMatrix tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        float ticks = Minecraft.getInstance().player != null ? 
                Minecraft.getInstance().player.tickCount + partialTicks : 0;
        
        poseStack.pushPose();
        
        // Move to block center
        poseStack.translate(0.5, 0.5, 0.5);
        
        // Apply startup rotation
        if (tile.startUp > 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees(ticks % 360.0f * tile.startUp));
            poseStack.mulPose(Axis.XP.rotationDegrees(35.0f * tile.startUp));
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0f * tile.startUp));
        }
        
        // Calculate instability wobble
        float instability = Math.min(6.0f, 1.0f + (tile.stability < 0 ? -tile.stability * 0.66f : 1.0f) * 
                (Math.min(tile.craftCount, 50) / 50.0f));
        
        // Choose texture based on pillar type (simplified - always use normal for now)
        Identifier texture = TEXTURE_NORMAL;
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        
        // Render 8 cubes at corners
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < 2; b++) {
                for (int c = 0; c < 2; c++) {
                    // Calculate wobble offset during crafting
                    float wobbleX = 0, wobbleY = 0, wobbleZ = 0;
                    if (tile.active) {
                        wobbleX = Mth.sin((ticks + a * 10) / 15.0f) * 0.01f * tile.startUp * instability;
                        wobbleY = Mth.sin((ticks + b * 10) / 14.0f) * 0.01f * tile.startUp * instability;
                        wobbleZ = Mth.sin((ticks + c * 10) / 13.0f) * 0.01f * tile.startUp * instability;
                    }
                    
                    int signA = (a == 0) ? -1 : 1;
                    int signB = (b == 0) ? -1 : 1;
                    int signC = (c == 0) ? -1 : 1;
                    
                    poseStack.pushPose();
                    poseStack.translate(wobbleX + signA * 0.25f, wobbleY + signB * 0.25f, wobbleZ + signC * 0.25f);
                    
                    // Rotation for visual variety
                    if (a > 0) poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                    if (b > 0) poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
                    if (c > 0) poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
                    
                    poseStack.scale(0.45f, 0.45f, 0.45f);
                    renderCube(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);
                    poseStack.popPose();
                }
            }
        }
        
        // Render glow overlay when active
        if (tile.active) {
            VertexConsumer glowConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
            
            for (int a = 0; a < 2; a++) {
                for (int b = 0; b < 2; b++) {
                    for (int c = 0; c < 2; c++) {
                        float wobbleX = Mth.sin((ticks + a * 10) / 15.0f) * 0.01f * tile.startUp * instability;
                        float wobbleY = Mth.sin((ticks + b * 10) / 14.0f) * 0.01f * tile.startUp * instability;
                        float wobbleZ = Mth.sin((ticks + c * 10) / 13.0f) * 0.01f * tile.startUp * instability;
                        
                        int signA = (a == 0) ? -1 : 1;
                        int signB = (b == 0) ? -1 : 1;
                        int signC = (c == 0) ? -1 : 1;
                        
                        poseStack.pushPose();
                        poseStack.translate(wobbleX + signA * 0.25f, wobbleY + signB * 0.25f, wobbleZ + signC * 0.25f);
                        
                        if (a > 0) poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                        if (b > 0) poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
                        if (c > 0) poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
                        
                        poseStack.scale(0.45f, 0.45f, 0.45f);
                        
                        // Pulsing purple glow
                        float pulse = (Mth.sin((ticks + a * 2 + b * 3 + c * 4) / 4.0f) * 0.1f + 0.2f) * tile.startUp;
                        renderCube(poseStack, glowConsumer, 0x00F000F0, packedOverlay, 0.8f, 0.1f, 1.0f, pulse);
                        
                        poseStack.popPose();
                    }
                }
            }
        }
        
        poseStack.popPose();
        
        // Render halo effect during crafting
        if (tile.crafting && tile.craftCount > 0) {
            renderHalo(tile, poseStack, buffer, partialTicks, tile.craftCount);
        }
    }

    /**
     * Render a simple cube.
     */
    private void renderCube(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                            float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();
        float size = 0.125f; // Half size
        
        // UV coordinates for cube texture
        float u0 = 0, u1 = 0.5f, v0 = 0, v1 = 0.5f;
        
        // Top face (Y+)
        addQuad(consumer, matrix, -size, size, -size, size, size, size, 0, 1, 0, r, g, b, a, packedLight, u0, v0, u1, v1);
        // Bottom face (Y-)
        addQuad(consumer, matrix, -size, -size, size, size, -size, -size, 0, -1, 0, r, g, b, a, packedLight, u0, v0, u1, v1);
        // North face (Z-)
        addQuad(consumer, matrix, size, -size, -size, -size, size, -size, 0, 0, -1, r, g, b, a, packedLight, u0, v0, u1, v1);
        // South face (Z+)
        addQuad(consumer, matrix, -size, -size, size, size, size, size, 0, 0, 1, r, g, b, a, packedLight, u0, v0, u1, v1);
        // West face (X-)
        addQuad(consumer, matrix, -size, -size, -size, -size, size, size, -1, 0, 0, r, g, b, a, packedLight, u0, v0, u1, v1);
        // East face (X+)
        addQuad(consumer, matrix, size, -size, size, size, size, -size, 1, 0, 0, r, g, b, a, packedLight, u0, v0, u1, v1);
    }

    private void addQuad(VertexConsumer consumer, Matrix4f matrix,
                         float x1, float y1, float z1, float x2, float y2, float z2,
                         float nx, float ny, float nz, float r, float g, float b, float a,
                         int light, float u0, float v0, float u1, float v1) {
        if (ny != 0) {
            // Horizontal face
            consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x1, y1, z2).color(r, g, b, a).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x2, y1, z2).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x2, y1, z1).color(r, g, b, a).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        } else if (nz != 0) {
            // Z-facing face
            consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x2, y1, z1).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x2, y2, z1).color(r, g, b, a).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x1, y2, z1).color(r, g, b, a).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        } else {
            // X-facing face
            consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x1, y1, z2).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x1, y2, z2).color(r, g, b, a).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
            consumer.vertex(matrix, x1, y2, z1).color(r, g, b, a).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
        }
    }

    /**
     * Render the energy halo effect during crafting.
     */
    private void renderHalo(TileInfusionMatrix tile, PoseStack poseStack, MultiBufferSource buffer,
                            float partialTicks, int craftCount) {
        
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        
        Random random = new Random(245L);
        int numSpikes = Minecraft.getInstance().options.graphicsMode().get().getId() >= 1 ? 20 : 10;
        
        float intensity = craftCount / 500.0f;
        
        // Use a simple line-based rendering for the halo spikes
        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());
        
        for (int i = 0; i < numSpikes; i++) {
            poseStack.pushPose();
            
            // Random rotations for each spike
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0f + intensity * 360.0f));
            
            float length = (random.nextFloat() * 20.0f + 5.0f) / 20.0f * (Math.min(craftCount, 50) / 50.0f);
            float width = (random.nextFloat() * 2.0f + 1.0f) / 20.0f * (Math.min(craftCount, 50) / 50.0f);
            
            Matrix4f matrix = poseStack.last().pose();
            
            // Draw spike as colored lines (simplified from triangle fan)
            float alpha = 1.0f - intensity;
            lineConsumer.vertex(matrix, 0, 0, 0).color(1.0f, 1.0f, 1.0f, alpha).normal(0, 1, 0).endVertex();
            lineConsumer.vertex(matrix, 0, length, 0).color(1.0f, 0, 1.0f, 0).normal(0, 1, 0).endVertex();
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(TileInfusionMatrix tile) {
        return tile.crafting;
    }
}
