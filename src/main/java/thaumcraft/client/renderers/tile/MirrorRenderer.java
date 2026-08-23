package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.tile.state.MirrorRenderState;
import thaumcraft.common.tiles.devices.TileMirror;

/**
 * Block entity renderer for Magic Mirrors.
 * Renders a portal-like effect when the mirror is linked.
 * Uses an end portal-style layered effect for the active portal.
 */
@OnlyIn(Dist.CLIENT)
public class MirrorRenderer implements BlockEntityRenderer<TileMirror, MirrorRenderState> {

    private static final Identifier PORTAL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/tunnel.png");
    private static final Identifier INACTIVE_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/mirrorpane.png");
    private static final Identifier ACTIVE_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/mirrorpanetrans.png");

    public MirrorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public MirrorRenderState createRenderState() {
        return new MirrorRenderState();
    }

    @Override
    public void extractRenderState(TileMirror tile, MirrorRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.facing = getFacing(tile.getBlockState());
        state.active = tile.linked && cameraPosition != null
                && cameraPosition.distanceToSqr(tile.getBlockPos().getX() + 0.5, tile.getBlockPos().getY() + 0.5, tile.getBlockPos().getZ() + 0.5) < 1024.0;
    }

    @Override
    public void submit(MirrorRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        // Move to block center and rotate based on facing
        poseStack.translate(0.5, 0.5, 0.5);
        applyRotation(poseStack, state.facing);

        // Render the portal effect (or inactive pane)
        if (state.active) {
            renderActivePortal(poseStack, submitNodeCollector, state.lightCoords);
        } else {
            renderInactivePane(poseStack, submitNodeCollector, state.lightCoords);
        }

        poseStack.popPose();
    }

    /**
     * Render the active portal with swirling effect.
     */
    private void renderActivePortal(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
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
            float r = 0.3f + 0.2f * (float) Math.sin(time + layer);
            float g = 0.1f + 0.1f * (float) Math.sin(time * 1.3f + layer);
            float b = 0.5f + 0.3f * (float) Math.sin(time * 0.7f + layer);

            submitNodeCollector.submitCustomGeometry(poseStack,
                    layer == 0 ? RenderTypes.entityTranslucent(PORTAL_TEXTURE) : RenderTypes.entityTranslucentEmissive(PORTAL_TEXTURE),
                    (pose, buffer) -> {
                Matrix4f matrix = pose.pose();

                buffer.addVertex(matrix, -size, -size, 0).setColor(r, g, b, alpha)
                        .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
                buffer.addVertex(matrix, size, -size, 0).setColor(r, g, b, alpha)
                        .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
                buffer.addVertex(matrix, size, size, 0).setColor(r, g, b, alpha)
                        .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
                buffer.addVertex(matrix, -size, size, 0).setColor(r, g, b, alpha)
                        .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
            });

            poseStack.popPose();
        }

        // Render frame overlay
        renderOverlay(poseStack, submitNodeCollector, ACTIVE_TEXTURE, packedLight);
    }

    /**
     * Render the inactive mirror pane.
     */
    private void renderInactivePane(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
        renderOverlay(poseStack, submitNodeCollector, INACTIVE_TEXTURE, packedLight);
    }

    /**
     * Render a texture overlay on the mirror surface.
     */
    private void renderOverlay(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                               Identifier texture, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0, 0, -0.43);

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float size = 0.35f;
            buffer.addVertex(matrix, -size, -size, 0).setColor(255, 255, 255, 255)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, -size, 0).setColor(255, 255, 255, 255)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, size, 0).setColor(255, 255, 255, 255)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, -size, size, 0).setColor(255, 255, 255, 255)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        });

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
