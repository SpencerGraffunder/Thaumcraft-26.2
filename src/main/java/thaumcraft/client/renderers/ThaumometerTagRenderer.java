package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.client.ThaumometerHUD;

/**
 * Renders the aspect tags above a thaumometer's scan target, matching 1.12's
 * {@code RenderEventHandler.drawTagsOnContainer}.
 *
 * In 1.12 this is an immediate-mode GL render (aspect icons as quads, rotated to face the
 * player, laid out in rows of 5, with the amount drawn beneath each icon). The 26.3 port
 * submits the same quads through a {@link SubmitNodeCollector} (the 26.3 render-state model,
 * same pattern as {@link SealRenderer}). The amount text is omitted: 1.12 drew it with the
 * font renderer in immediate mode, which has no direct 26.3 equivalent in this geometry pass.
 *
 * Called from {@code ClientRenderEvents.onSubmitCustomGeometry} when the local player holds a
 * thaumometer and is looking at a target with aspects.
 */
@OnlyIn(Dist.CLIENT)
public final class ThaumometerTagRenderer {

    // 1.12 uses a 5-column layout; each icon is ~0.5 blocks wide at scale 1.
    private static final int ROW_SIZE = 5;
    private static final float ICON_SPACING = 0.5f;
    private static final float ICON_SCALE = 0.5f;

    private ThaumometerTagRenderer() {}

    /**
     * Submit the aspect tags for the current thaumometer target.
     */
    public static void renderTags(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Player player) {
        Entity target = ThaumometerHUD.target;
        if (target == null || target.isRemoved()) return;

        AspectList tags = AspectHelper.getEntityAspects(target);
        if (tags == null || tags.size() == 0) return;

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.mainCamera().position();

        // Target position (current; 1.12 interpolates via iPX/iPY/iPZ, but the target moves
        // slowly enough that the current position is visually equivalent here).
        double tx = target.getX();
        double ty = target.getY();
        double tz = target.getZ();
        double drawY = ty + target.getBbHeight() + 0.2;

        // Rotate the row to face the player (1.12: rotYaw = atan2(xd, zd) * 180/pi + 180).
        float xd = (float) (cameraPos.x - tx);
        float zd = (float) (cameraPos.z - tz);
        float rotYaw = (float) (Math.atan2(xd, zd) * 180.0 / Math.PI) + 180.0f;

        poseStack.pushPose();
        poseStack.translate(tx - cameraPos.x, drawY - cameraPos.y, tz - cameraPos.z);
        poseStack.rotate(Axis.YP.rotationDegrees(rotYaw));

        int count = tags.size();
        int current = 0;
        float shifty = 0.0f;

        for (Aspect tag : tags.getAspects()) {
            if (current >= ROW_SIZE) {
                current = 0;
                shifty -= ICON_SPACING;
            }
            // Center the current row (1.12: shift = (current - div/2 + 0.5) * tagscale * 4).
            int div = Math.min(count - current, ROW_SIZE);
            float shift = (current - div / 2.0f + 0.5f) * ICON_SPACING;

            int color = tag.getColor();
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            poseStack.pushPose();
            poseStack.translate(shift, shifty, 0);
            poseStack.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
            renderIconQuad(poseStack, submitNodeCollector, tag.getImage(), r, g, b);
            poseStack.popPose();

            current++;
        }

        poseStack.popPose();
    }

    /**
     * Submit a centered, camera-facing icon quad (same vertex layout as SealRenderer).
     */
    private static void renderIconQuad(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                       Identifier texture, float r, float g, float b) {
        if (texture == null) return;

        float size = 0.5f;
        submitNodeCollector.submitCustomGeometry(
            poseStack, RenderTypes.entityTranslucent(texture),
            (pose, buffer) -> {
                buffer.addVertex(pose, -size, -size, 0.0F)
                    .setColor(r, g, b, 1.0F)
                    .setUv(0.0F, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, 0.0F, 0.0F, 1.0F);
                buffer.addVertex(pose, size, -size, 0.0F)
                    .setColor(r, g, b, 1.0F)
                    .setUv(1.0F, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, 0.0F, 0.0F, 1.0F);
                buffer.addVertex(pose, size, size, 0.0F)
                    .setColor(r, g, b, 1.0F)
                    .setUv(1.0F, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, 0.0F, 0.0F, 1.0F);
                buffer.addVertex(pose, -size, size, 0.0F)
                    .setColor(r, g, b, 1.0F)
                    .setUv(0.0F, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, 0.0F, 0.0F, 1.0F);
            }
        );
    }
}
