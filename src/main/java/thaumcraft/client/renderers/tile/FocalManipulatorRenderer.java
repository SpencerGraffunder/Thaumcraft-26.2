package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.tile.state.FocalManipulatorRenderState;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

import java.awt.Color;

/**
 * Block entity renderer for the Focal Manipulator.
 * Renders floating focus item and orbiting crystal elements.
 */
@OnlyIn(Dist.CLIENT)
public class FocalManipulatorRenderer implements BlockEntityRenderer<TileFocalManipulator, FocalManipulatorRenderState> {

    private static final Identifier PARTICLE_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");

    private final ItemModelResolver itemModelResolver;

    public FocalManipulatorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public FocalManipulatorRenderState createRenderState() {
        return new FocalManipulatorRenderState();
    }

    @Override
    public void extractRenderState(TileFocalManipulator tile, FocalManipulatorRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        Minecraft mc = Minecraft.getInstance();
        state.ticks = (mc.player != null ? mc.player.tickCount : 0) + partialTicks;

        // Render floating focus item
        state.focusItem.clear();
        ItemStack focusStack = tile.getItem(0);
        if (!focusStack.isEmpty()) {
            this.itemModelResolver.updateForTopItem(state.focusItem, focusStack, ItemDisplayContext.FIXED, tile.getLevel(), null, 0);
        }

        // Render orbiting crystals during crafting
        Aspect[] aspects = tile.crystalsSync.getAspects();
        state.crystals = new net.minecraft.client.renderer.item.ItemStackRenderState[aspects != null ? aspects.length : 0];
        state.crystalColors = new int[state.crystals.length];
        for (int a = 0; a < state.crystals.length; a++) {
            state.crystals[a] = new net.minecraft.client.renderer.item.ItemStackRenderState();
            ItemStack crystalStack = ThaumcraftApiHelper.makeCrystal(aspects[a]);
            this.itemModelResolver.updateForTopItem(state.crystals[a], crystalStack, ItemDisplayContext.FIXED, tile.getLevel(), null, 0);
            state.crystalColors[a] = aspects[a].getColor();
        }
    }

    @Override
    public void submit(FocalManipulatorRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        float ticks = state.ticks;

        // Render floating focus item
        if (!state.focusItem.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.8, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(ticks % 360.0f));

            // Bobbing animation
            float bob = Mth.sin(ticks / 14.0f) * 0.2f + 0.2f;
            poseStack.translate(0, bob * 0.1, 0);

            poseStack.scale(0.5f, 0.5f, 0.5f);
            state.focusItem.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

            poseStack.popPose();
        }

        // Render orbiting crystals during crafting
        int count = state.crystals.length;
        if (count > 0) {
            float angleStep = 360.0f / count;

            for (int a = 0; a < count; a++) {
                float angle = (ticks % 720.0f / 2.0f) + angleStep * a;
                float bob = Mth.sin((ticks + a * 10) / 12.0f) * 0.02f + 0.02f;

                Color c = new Color(state.crystalColors[a]);
                float r = c.getRed() / 255.0f;
                float g = c.getGreen() / 255.0f;
                float b = c.getBlue() / 255.0f;

                // Render glowing particle
                poseStack.pushPose();
                poseStack.translate(0.5, 1.3, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(angle));
                poseStack.translate(0, bob, 0.4);
                poseStack.mulPose(Axis.YP.rotationDegrees(-angle));

                // Face camera
                poseStack.mulPose(camera.orientation);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

                renderGlowingOrb(poseStack, submitNodeCollector, 0.175f, r, g, b, 0.66f);

                poseStack.popPose();

                // Render crystal item
                poseStack.pushPose();
                poseStack.translate(0.5, 1.05, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(angle));
                poseStack.translate(0, bob, 0.4);
                poseStack.scale(0.5f, 0.5f, 0.5f);

                // Render ray effect
                renderRay(poseStack, submitNodeCollector, angle, a, bob, r, g, b, ticks);

                // Render crystal
                state.crystals[a].submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

                poseStack.popPose();
            }
        }
    }

    /**
     * Render a glowing orb particle.
     */
    private void renderGlowingOrb(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                  float size, float r, float g, float b, float a) {
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(PARTICLE_TEXTURE), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            int fullLight = 0x00F000F0;
            float half = size / 2.0f;

            // UV coordinates for glow particle (somewhere in particle atlas)
            float u0 = 0.0f;
            float u1 = 0.0625f;
            float v0 = 0.0f;
            float v1 = 0.0625f;

            buffer.addVertex(matrix, -half, -half, 0).setColor(r, g, b, a)
                    .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, half, -half, 0).setColor(r, g, b, a)
                    .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, half, half, 0).setColor(r, g, b, a)
                    .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, -half, half, 0).setColor(r, g, b, a)
                    .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullLight).setNormal(0, 0, 1);
        });
    }

    /**
     * Render a ray effect from crystal to focus.
     */
    private void renderRay(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                           float angle, int num, float lift, float r, float g, float b, float ticks) {
        poseStack.pushPose();

        float pan = Mth.sin((ticks + num * 10) / 15.0f) * 15.0f;
        float aperture = Mth.sin((ticks + num * 10) / 14.0f) * 2.0f;

        poseStack.translate(0, 0.475f + lift, 0);
        poseStack.mulPose(Axis.XN.rotationDegrees(90));
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.mulPose(Axis.XP.rotationDegrees(pan));

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Randomize ray shape
            java.util.Random random = new java.util.Random(187L + (long) num * num);
            float fa = random.nextFloat() * 20.0f + 10.0f;
            float f4 = random.nextFloat() * 4.0f + 6.0f + aperture;
            fa /= 30.0f / (Math.min(ticks, 10.0f) / 10.0f);
            f4 /= 30.0f / (Math.min(ticks, 10.0f) / 10.0f);

            // Scale down the ray
            fa *= 0.02f;
            f4 *= 0.02f;

            // Render cone/ray using triangle fan pattern
            buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, 0.66f);
            buffer.addVertex(matrix, -0.8f * f4, fa, -0.5f * f4).setColor(r, g, b, 0.0f);
            buffer.addVertex(matrix, 0.8f * f4, fa, -0.5f * f4).setColor(r, g, b, 0.0f);
            buffer.addVertex(matrix, 0, fa, 1.0f * f4).setColor(r, g, b, 0.0f);
        });

        poseStack.popPose();
    }
}
