package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.models.block.BannerModel;
import thaumcraft.client.renderers.tile.state.BannerRenderState;
import thaumcraft.common.tiles.misc.TileBanner;

import java.awt.Color;

/**
 * Block entity renderer for Thaumcraft banners.
 * Renders the banner with cloth animation and optional aspect decoration.
 * 
 * Ported from 1.12.2 TileBannerRenderer.
 */
@OnlyIn(Dist.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<TileBanner, BannerRenderState> {
    
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
    public BannerRenderState createRenderState() {
        return new BannerRenderState();
    }

    @Override
    public void extractRenderState(TileBanner banner, BannerRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(banner, state, partialTicks, cameraPosition, breakProgress);

        // Choose texture based on banner type
        Aspect aspect = banner.getAspect();
        int color = banner.getColor();
        state.aspect = aspect;
        state.color = color;
        state.cultTexture = (aspect == null && color == -1);
        state.wall = banner.getWall();
        state.bannerFacing = banner.getBannerFacing() * 360.0f / 16.0f;

        // Calculate wind animation
        if (banner.getLevel() != null) {
            Minecraft mc = Minecraft.getInstance();
            float time = banner.getBlockPos().getX() * 7 +
                        banner.getBlockPos().getY() * 9 +
                        banner.getBlockPos().getZ() * 13 +
                        (mc.player != null ? mc.player.tickCount : 0) + partialTicks;
            state.wind = 0.02f - Mth.sin(time / 11.0f) * 0.02f;
        }
    }

    @Override
    public void submit(BannerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        // Choose texture based on banner type
        Identifier texture = state.cultTexture ? TEX_CULT : TEX_BLANK;

        poseStack.pushPose();

        // Position and orient the banner
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.bannerFacing));

        // Render pole (only for standing banners)
        if (state.wall) {
            poseStack.translate(0.0, 1.0, -0.4125);
        }

        // Determine banner color
        int tintedColor = -1;
        int color = state.color;
        if (color != -1) {
            Color c = new Color(color);
            tintedColor = 0xFF000000 | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        }

        // Submit the whole banner model (pole, beam, tabs and cloth with wind animation)
        submitNodeCollector.submitModel(this.model, state, poseStack, this.model.renderType(texture),
                state.lightCoords, OverlayTexture.NO_OVERLAY, tintedColor, null, 0, state.breakProgress);

        // Render aspect decoration if present
        Aspect aspect = state.aspect;
        if (aspect != null) {
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, 0.05001);
            poseStack.scale(0.0375f, 0.0375f, 0.0375f);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            // Rotate with banner animation
            poseStack.mulPose(Axis.XP.rotationDegrees(-state.wind * 57.295776f * 2.0f));

            // Draw aspect icon in world space
            renderAspectIcon(poseStack, submitNodeCollector, aspect, -8, 0, state.lightCoords, 0.75f);

            poseStack.popPose();
        }

        poseStack.popPose();
    }
    
    /**
     * Render an aspect icon in world space.
     */
    private void renderAspectIcon(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Aspect aspect,
                                  int x, int y, int packedLight, float alpha) {
        if (aspect == null) return;
        
        Identifier texture = aspect.getImage();
        
        // Get aspect color
        int color = aspect.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Draw a quad for the aspect icon
            float x1 = x;
            float y1 = y;
            float x2 = x + ICON_SIZE;
            float y2 = y + ICON_SIZE;

            buffer.addVertex(matrix, x1, y2, 0).setColor(r, g, b, alpha).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, alpha).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, x2, y1, 0).setColor(r, g, b, alpha).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, alpha).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        });
    }
}
