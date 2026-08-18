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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.crafting.TileResearchTable;

import java.awt.Color;

/**
 * Block entity renderer for the Research Table.
 * Renders scroll/paper items on the table surface when research is in progress.
 */
@OnlyIn(Dist.CLIENT)
public class ResearchTableRenderer implements BlockEntityRenderer<TileResearchTable> {

    private static final Identifier SCROLL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/scroll.png");
    private static final Identifier INKWELL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/inkwell.png");

    public ResearchTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileResearchTable tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        Direction facing = getFacing(tile.getBlockState());
        
        poseStack.pushPose();
        
        // Move to table surface
        poseStack.translate(0.5, 1.0, 0.5);
        
        // Rotate based on table facing
        applyRotation(poseStack, facing);
        
        // Render scroll if there's research data
        if (tile.hasResearchData()) {
            renderScroll(poseStack, buffer, packedLight);
        }
        
        // Render inkwell if scribe tools are present
        if (tile.hasScribeTools()) {
            renderInkwell(poseStack, buffer, packedLight);
        }
        
        poseStack.popPose();
    }

    /**
     * Render a scroll/paper on the table surface.
     */
    private void renderScroll(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Position scroll on table
        poseStack.translate(0, 0.02, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90)); // Lay flat
        poseStack.scale(0.5f, 0.5f, 0.5f);
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(SCROLL_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        
        // Render scroll as a flat quad
        float size = 0.5f;
        Color color = new Color(Aspect.ALCHEMY.getColor());
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        
        vertexConsumer.vertex(matrix, -size, -size, 0).color(r, g, b, 1.0f)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, size, -size, 0).color(r, g, b, 1.0f)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, size, size, 0).color(r, g, b, 1.0f)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, -size, size, 0).color(r, g, b, 1.0f)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        
        poseStack.popPose();
    }

    /**
     * Render an inkwell with quill on the table.
     */
    private void renderInkwell(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Position inkwell offset from center
        poseStack.translate(-0.3, 0.02, 0.2);
        poseStack.scale(0.2f, 0.2f, 0.2f);
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(INKWELL_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        
        // Simple cube for inkwell
        float size = 0.5f;
        
        // Top face
        vertexConsumer.vertex(matrix, -size, size, -size).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, -size, size, size).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, size, size, size).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, size, size, -size).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        
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
