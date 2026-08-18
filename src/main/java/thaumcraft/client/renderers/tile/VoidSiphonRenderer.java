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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.common.tiles.crafting.TileVoidSiphon;

/**
 * Block entity renderer for the Void Siphon.
 * Renders a void portal-like effect when the siphon is active.
 */
@OnlyIn(Dist.CLIENT)
public class VoidSiphonRenderer implements BlockEntityRenderer<TileVoidSiphon> {

    private static final Identifier END_PORTAL_TEXTURE = 
            Identifier.withDefaultNamespace("textures/entity/end_portal.png");

    public VoidSiphonRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileVoidSiphon tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        if (tile.getLevel() == null) return;
        
        // Only render the void effect when enabled (not powered by redstone)
        if (tile.getLevel().hasNeighborSignal(tile.getBlockPos())) return;
        
        // Render void orb at top
        renderVoidOrb(tile, poseStack, buffer, 0.875f, 0.25f);
        
        // Render void orb in center/bottom
        renderVoidOrb(tile, poseStack, buffer, 0.3125f, 0.5f);
    }
    
    /**
     * Render a void orb at the specified height.
     */
    private void renderVoidOrb(TileVoidSiphon tile, PoseStack poseStack, 
                                MultiBufferSource buffer, float height, float size) {
        poseStack.pushPose();
        poseStack.translate(0.5, height, 0.5);
        
        // Use end portal render type for the starfield effect
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.endPortal());
        
        // Render cube faces
        for (Direction face : Direction.values()) {
            poseStack.pushPose();
            
            // Rotate to face direction
            switch (face) {
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                case NORTH -> { /* Default */ }
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            }
            
            // Adjust size based on face type
            float faceSize = size;
            if (face.getAxis() == Direction.Axis.Z) {
                // Z faces are not square, they stretch vertically  
                poseStack.mulPose(Axis.ZN.rotationDegrees(90));
            }
            
            // Move to face position
            float offset = face == Direction.DOWN || face == Direction.UP ? 0.126f : 0.26f;
            poseStack.translate(0, 0, face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? -offset : offset);
            
            poseStack.scale(faceSize, faceSize, faceSize);
            
            Matrix4f matrix = poseStack.last().pose();
            float half = 0.5f;
            
            vertexConsumer.vertex(matrix, -half, -half, 0).endVertex();
            vertexConsumer.vertex(matrix, half, -half, 0).endVertex();
            vertexConsumer.vertex(matrix, half, half, 0).endVertex();
            vertexConsumer.vertex(matrix, -half, half, 0).endVertex();
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
    }
}
