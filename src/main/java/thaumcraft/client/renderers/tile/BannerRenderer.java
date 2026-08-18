package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.models.block.BannerModel;
import thaumcraft.common.tiles.misc.TileBanner;

import java.awt.Color;

/**
 * Block entity renderer for Thaumcraft banners.
 * Renders the banner with cloth animation and optional aspect decoration.
 * 
 * Ported from 1.12.2 TileBannerRenderer.
 */
@OnlyIn(Dist.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<TileBanner> {
    
    private static final Identifier TEX_CULT = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/banner_cultist.png");
    private static final Identifier TEX_BLANK = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/banner_blank.png");
    
    private final BannerModel model;
    
    public BannerRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new BannerModel(context.bakeLayer(BannerModel.LAYER_LOCATION));
    }
    
    private static final int ICON_SIZE = 16;
    
    @Override
    public void render(TileBanner banner, float partialTicks, PoseStack poseStack, 
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        // Choose texture based on banner type
        Identifier texture;
        if (banner.getAspect() == null && banner.getColor() == -1) {
            texture = TEX_CULT;
        } else {
            texture = TEX_BLANK;
        }
        
        poseStack.pushPose();
        
        // Position and orient the banner
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
        
        if (banner.getLevel() != null) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            float facingAngle = banner.getBannerFacing() * 360.0f / 16.0f;
            poseStack.mulPose(Axis.YP.rotationDegrees(facingAngle));
        }
        
        // Get render type
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        
        // Render pole (only for standing banners)
        if (!banner.getWall()) {
            model.renderPole(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            poseStack.translate(0.0, 1.0, -0.4125);
        }
        
        // Render beam
        model.renderBeam(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);
        
        // Determine banner color
        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        int color = banner.getColor();
        if (color != -1) {
            Color c = new Color(color);
            red = c.getRed() / 255.0f;
            green = c.getGreen() / 255.0f;
            blue = c.getBlue() / 255.0f;
        }
        
        // Render tabs with color
        model.renderTabs(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, 1.0f);
        
        // Calculate wind animation
        Player player = Minecraft.getInstance().player;
        if (player != null && banner.getLevel() != null) {
            float time = banner.getBlockPos().getX() * 7 + 
                        banner.getBlockPos().getY() * 9 + 
                        banner.getBlockPos().getZ() * 13 + 
                        player.tickCount + partialTicks;
            float rx = 0.02f - Mth.sin(time / 11.0f) * 0.02f;
            model.setBannerRotation(rx);
            
            // Render banner with animation
            model.renderBanner(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, 1.0f);
            
            // Render aspect decoration if present
            Aspect aspect = banner.getAspect();
            if (aspect != null) {
                poseStack.pushPose();
                poseStack.translate(0.0, 0.0, 0.05001);
                poseStack.scale(0.0375f, 0.0375f, 0.0375f);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
                // Rotate with banner animation
                poseStack.mulPose(Axis.XP.rotationDegrees(-rx * 57.295776f * 2.0f));
                
                // Draw aspect icon in world space
                renderAspectIcon(poseStack, buffer, aspect, -8, 0, packedLight, packedOverlay, 0.75f);
                
                poseStack.popPose();
            }
        } else {
            // Static rendering when player not available
            model.renderBanner(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, 1.0f);
        }
        
        poseStack.popPose();
    }
    
    /**
     * Render an aspect icon in world space.
     */
    private void renderAspectIcon(PoseStack poseStack, MultiBufferSource buffer, Aspect aspect,
                                  int x, int y, int packedLight, int packedOverlay, float alpha) {
        if (aspect == null) return;
        
        Identifier texture = aspect.getImage();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        
        // Get aspect color
        int color = aspect.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        Matrix4f matrix = poseStack.last().pose();
        
        // Draw a quad for the aspect icon
        float x1 = x;
        float y1 = y;
        float x2 = x + ICON_SIZE;
        float y2 = y + ICON_SIZE;
        
        vertexConsumer.vertex(matrix, x1, y2, 0).color(r, g, b, alpha).uv(0, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, x2, y2, 0).color(r, g, b, alpha).uv(1, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, x2, y1, 0).color(r, g, b, alpha).uv(1, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        vertexConsumer.vertex(matrix, x1, y1, 0).color(r, g, b, alpha).uv(0, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
    }
}
