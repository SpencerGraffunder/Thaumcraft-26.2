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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.devices.TileMirror;

/**
 * Block entity renderer for Magic Mirrors.
 * Renders a portal-like effect when the mirror is linked.
 * Uses an end portal-style layered effect for the active portal.
 */
@OnlyIn(Dist.CLIENT)
public class MirrorRenderer implements BlockEntityRenderer<TileMirror> {

    private static final Identifier PORTAL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/tunnel.png");
    private static final Identifier INACTIVE_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/mirrorpane.png");
    private static final Identifier ACTIVE_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/mirrorpanetrans.png");

    public MirrorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileMirror tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        Direction facing = getFacing(tile.getBlockState());
        
        poseStack.pushPose();
        
        // Move to block center and rotate based on facing
        poseStack.translate(0.5, 0.5, 0.5);
        applyRotation(poseStack, facing);
        
        // Render the portal effect (or inactive pane)
        if (tile.linked && isPlayerNearby(tile)) {
            renderActivePortal(poseStack, buffer, partialTicks, packedLight);
        } else {
            renderInactivePane(poseStack, buffer, packedLight);
        }
        
        poseStack.popPose();
    }

    private boolean isPlayerNearby(TileMirror tile) {
        if (tile.getLevel() == null) return false;
        return Minecraft.getInstance().player != null &&
               Minecraft.getInstance().player.distanceToSqr(
                       tile.getBlockPos().getX() + 0.5,
                       tile.getBlockPos().getY() + 0.5,
                       tile.getBlockPos().getZ() + 0.5) < 1024.0;
    }

    /**
     * Render the active portal with swirling effect.
     */
    private void renderActivePortal(PoseStack poseStack, MultiBufferSource buffer,
                                    float partialTicks, int packedLight) {
        
        // Render multiple layers for a portal-like effect
        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
        
        for (int layer = 0; layer < 4; layer++) {
            poseStack.pushPose();
            
            float offset = 0.001f * layer;
            poseStack.translate(0, 0, -0.44 + offset);
            
            // Each layer has different rotation
            float rotation = time * (20 + layer * 10);
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
            
            float alpha = 1.0f - (layer * 0.2f);
            float size = 0.3f + layer * 0.02f;
            
            // Color shifts between layers
            float r = 0.3f + 0.2f * (float)Math.sin(time + layer);
            float g = 0.1f + 0.1f * (float)Math.sin(time * 1.3f + layer);
            float b = 0.5f + 0.3f * (float)Math.sin(time * 0.7f + layer);
            
            VertexConsumer vertexConsumer = buffer.getBuffer(
                    layer == 0 ? RenderType.entityTranslucent(PORTAL_TEXTURE) 
                              : RenderType.entityTranslucentEmissive(PORTAL_TEXTURE));
            Matrix4f matrix = poseStack.last().pose();
            
            vertexConsumer.vertex(matrix, -size, -size, 0).color(r, g, b, alpha)
                    .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
            vertexConsumer.vertex(matrix, size, -size, 0).color(r, g, b, alpha)
                    .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
            vertexConsumer.vertex(matrix, size, size, 0).color(r, g, b, alpha)
                    .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
            vertexConsumer.vertex(matrix, -size, size, 0).color(r, g, b, alpha)
                    .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
            
            poseStack.popPose();
        }
        
        // Render frame overlay
        renderOverlay(poseStack, buffer, ACTIVE_TEXTURE, packedLight);
    }

    /**
     * Render the inactive mirror pane.
     */
    private void renderInactivePane(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        renderOverlay(poseStack, buffer, INACTIVE_TEXTURE, packedLight);
    }

    /**
     * Render a texture overlay on the mirror surface.
     */
    private void renderOverlay(PoseStack poseStack, MultiBufferSource buffer,
                               Identifier texture, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0, 0, -0.43);
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        Matrix4f matrix = poseStack.last().pose();
        
        float size = 0.35f;
        vertexConsumer.vertex(matrix, -size, -size, 0).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, size, -size, 0).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, size, size, 0).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, -size, size, 0).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        
        poseStack.popPose();
    }

    private void applyRotation(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case NORTH -> { } // Default facing
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
        }
    }

    private Direction getFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }
}
