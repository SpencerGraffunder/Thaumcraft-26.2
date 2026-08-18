package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.essentia.TileAlembic;

import java.awt.Color;

/**
 * Block entity renderer for the Alembic.
 * Renders the aspect filter label and connection nozzles to adjacent tubes.
 */
@OnlyIn(Dist.CLIENT)
public class AlembicRenderer implements BlockEntityRenderer<TileAlembic> {

    private static final Identifier LABEL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/label.png");

    public AlembicRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileAlembic tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        // Render aspect filter label if set
        if (tile.getAspectFilter() != null) {
            renderLabel(tile, tile.getAspectFilter(), poseStack, buffer, packedLight);
        }

        // Render connection nozzles to adjacent essentia transport blocks
        if (tile.getLevel() != null) {
            renderNozzles(tile, poseStack, buffer, packedLight);
        }
    }

    /**
     * Render the aspect filter label on the front of the alembic.
     */
    private void renderLabel(TileAlembic tile, Aspect aspect, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight) {
        
        poseStack.pushPose();

        // Move to block center
        poseStack.translate(0.5, 0.0, 0.5);

        // Rotate based on alembic facing
        int facing = tile.getFacing();
        switch (facing) {
            case 2 -> poseStack.mulPose(Axis.YP.rotationDegrees(180)); // North - face south
            case 3 -> poseStack.mulPose(Axis.YP.rotationDegrees(0));   // South - face north
            case 4 -> poseStack.mulPose(Axis.YP.rotationDegrees(90));  // West - face east
            case 5 -> poseStack.mulPose(Axis.YP.rotationDegrees(270)); // East - face west
        }

        // Move to label position on front face
        poseStack.translate(0, 0.5, -0.376);

        // Render label background
        VertexConsumer labelConsumer = buffer.getBuffer(RenderType.entityCutout(LABEL_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();

        float size = 0.22f;
        labelConsumer.vertex(matrix, -size, -size, 0).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        labelConsumer.vertex(matrix, size, -size, 0).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        labelConsumer.vertex(matrix, size, size, 0).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        labelConsumer.vertex(matrix, -size, size, 0).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();

        // Render aspect icon on label
        poseStack.translate(0, 0, -0.001); // Slightly in front
        
        // Get aspect color and render colored quad
        Color color = new Color(aspect.getColor());
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        // Render aspect icon using the aspect's image
        VertexConsumer iconConsumer = buffer.getBuffer(RenderType.entityCutout(aspect.getImage()));
        Matrix4f iconMatrix = poseStack.last().pose();

        float iconSize = 0.15f;
        iconConsumer.vertex(iconMatrix, -iconSize, -iconSize, 0).color(r, g, b, 1f)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
        iconConsumer.vertex(iconMatrix, iconSize, -iconSize, 0).color(r, g, b, 1f)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
        iconConsumer.vertex(iconMatrix, iconSize, iconSize, 0).color(r, g, b, 1f)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();
        iconConsumer.vertex(iconMatrix, -iconSize, iconSize, 0).color(r, g, b, 1f)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 0, 1).endVertex();

        poseStack.popPose();
    }

    /**
     * Render nozzle connections to adjacent essentia transport blocks.
     */
    private void renderNozzles(TileAlembic tile, PoseStack poseStack,
                               MultiBufferSource buffer, int packedLight) {
        
        // Check each horizontal direction for connected tubes
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!tile.canOutputTo(dir)) continue;

            BlockEntity te = tile.getLevel().getBlockEntity(tile.getBlockPos().relative(dir));
            if (te instanceof IEssentiaTransport transport) {
                if (transport.canInputFrom(dir.getOpposite())) {
                    renderNozzle(dir, poseStack, buffer, packedLight);
                }
            }
        }
    }

    /**
     * Render a single nozzle in the specified direction.
     */
    private void renderNozzle(Direction dir, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight) {
        
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
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());
        Matrix4f matrix = poseStack.last().pose();

        float size = 0.125f;
        float depth = 0.0625f;
        
        // Nozzle is a small box extruding from the block
        // Front face
        vertexConsumer.vertex(matrix, -size, -size, depth).color(100, 80, 60, 255)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, size, -size, depth).color(100, 80, 60, 255)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, size, size, depth).color(100, 80, 60, 255)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, -size, size, depth).color(100, 80, 60, 255)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();

        poseStack.popPose();
    }
}
