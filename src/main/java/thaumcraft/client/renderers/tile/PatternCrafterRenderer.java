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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.crafting.TilePatternCrafter;

/**
 * Block entity renderer for the Pattern Crafter.
 * Renders the mode display and rotating gears.
 */
@OnlyIn(Dist.CLIENT)
public class PatternCrafterRenderer implements BlockEntityRenderer<TilePatternCrafter> {

    private static final Identifier MODES_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/pattern_crafter_modes.png");
    private static final Identifier GEAR_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/gear_brass.png");

    public PatternCrafterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TilePatternCrafter tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        if (tile.getLevel() == null) return;
        
        // Get facing direction
        Direction facing = Direction.NORTH;
        if (tile.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        
        poseStack.pushPose();
        poseStack.translate(0.5, 0.75, 0.5);
        
        // Rotate based on facing
        float rotation = switch (facing) {
            case EAST -> 90.0f;
            case WEST -> 270.0f;
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            default -> 0.0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        
        // Render mode display
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
        poseStack.translate(0, 0, -0.5);
        
        renderModeDisplay(tile, poseStack, buffer, packedLight, tile.type);
        
        poseStack.popPose();
        
        // Render left gear
        poseStack.pushPose();
        poseStack.translate(-0.2, -0.40625, 0.05);
        float gearRot = -tile.rot % 360.0f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(gearRot));
        poseStack.scale(0.5f, 0.5f, 1.0f);
        poseStack.translate(-0.5, -0.5, 0);
        
        renderGear(poseStack, buffer, packedLight);
        
        poseStack.popPose();
        
        // Render right gear
        poseStack.pushPose();
        poseStack.translate(0.2, -0.40625, 0.05);
        gearRot = tile.rot % 360.0f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(gearRot));
        poseStack.scale(0.5f, 0.5f, 1.0f);
        poseStack.translate(-0.5, -0.5, 0);
        
        renderGear(poseStack, buffer, packedLight);
        
        poseStack.popPose();
        
        poseStack.popPose();
    }
    
    /**
     * Render the mode indicator display.
     */
    private void renderModeDisplay(TilePatternCrafter tile, PoseStack poseStack, 
                                    MultiBufferSource buffer, int packedLight, int type) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(MODES_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        
        // Calculate UV for the specific mode (10 modes in a row)
        float uMin = type / 10.0f;
        float uMax = (type + 1) / 10.0f;
        float vMin = 0;
        float vMax = 1;
        
        float size = 0.5f;
        
        vertexConsumer.vertex(matrix, -size, -size, 0).color(255, 255, 255, 255)
                .uv(uMin, vMax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, size, -size, 0).color(255, 255, 255, 255)
                .uv(uMax, vMax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, size, size, 0).color(255, 255, 255, 255)
                .uv(uMax, vMin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, -size, size, 0).color(255, 255, 255, 255)
                .uv(uMin, vMin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
    }
    
    /**
     * Render a brass gear.
     */
    private void renderGear(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(GEAR_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        
        float size = 1.0f;
        
        vertexConsumer.vertex(matrix, 0, 0, 0).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, size, 0, 0).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, size, size, 0).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, 0, size, 0).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
    }
}
