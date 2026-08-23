package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SealRenderer - Renders seals in the world when player holds ISealDisplayer items.
 * 
 * Renders:
 * - Seal icons on block faces
 * - Seal working area (for seals with ISealConfigArea)
 * - Inactive state indicator (when stopped by redstone)
 * 
 * Ported to the 26.2 render-state model: geometry is submitted through a
 * SubmitNodeCollector (call this from SubmitCustomGeometryEvent with the event's
 * collector and pose stack) instead of immediate-mode rendering.
 */
@OnlyIn(Dist.CLIENT)
public class SealRenderer {
    
    // Default seal texture for seals without custom icons
    private static final Identifier DEFAULT_SEAL_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/items/seals/seal_blank.png");
    
    // Maximum render distance squared (16 blocks)
    private static final double MAX_RENDER_DIST_SQ = 256.0;
    
    /**
     * Submit all seals visible to the player.
     * Called from SubmitCustomGeometryEvent with its pose stack and collector.
     * 
     * @param poseStack The pose stack for transformations (camera-relative)
     * @param submitNodeCollector The collector to submit geometry to
     * @param player The local player
     */
    public static void renderSeals(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Player player) {
        if (player == null || player.level() == null) return;
        
        // Get seals in player's dimension
        String dimKey = player.level().dimension().identifier().toString();
        ConcurrentHashMap<SealPos, SealEntity> seals = SealHandler.sealEntities.get(dimKey);
        
        if (seals == null || seals.isEmpty()) return;
        
        // Get camera position for distance calculations
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.mainCamera().position();
        
        poseStack.pushPose();
        
        // Render each seal
        for (ISealEntity seal : seals.values()) {
            if (seal.getSeal() == null || seal.getSealPos() == null) continue;
            
            BlockPos pos = seal.getSealPos().pos;
            double distSq = cameraPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            
            if (distSq <= MAX_RENDER_DIST_SQ) {
                float alpha = 1.0f - (float)(distSq / MAX_RENDER_DIST_SQ);
                boolean inactive = seal.isStoppedByRedstone(player.level());
                
                renderSeal(poseStack, submitNodeCollector, seal, cameraPos, alpha, inactive);
            }
        }
        
        poseStack.popPose();
    }
    
    /**
     * Render a single seal.
     */
    private static void renderSeal(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                   ISealEntity seal, Vec3 cameraPos, 
                                   float alpha, boolean inactive) {
        SealPos sealPos = seal.getSealPos();
        BlockPos pos = sealPos.pos;
        Direction face = sealPos.face;
        
        poseStack.pushPose();
        
        // Translate to seal position (relative to camera)
        poseStack.translate(
            pos.getX() + 0.5 - cameraPos.x,
            pos.getY() + 0.5 - cameraPos.y,
            pos.getZ() + 0.5 - cameraPos.z
        );
        
        // Rotate based on face direction
        applyFaceRotation(poseStack, face);
        
        // Move slightly off the surface to prevent z-fighting
        poseStack.translate(0, 0, 0.51);
        
        // Scale down
        poseStack.scale(0.5f, 0.5f, 0.5f);
        
        // Render the seal quad
        float brightness = inactive ? 0.5f : 1.0f;
        renderSealQuad(poseStack, submitNodeCollector, seal.getSeal().getSealIcon(), brightness, brightness, brightness, alpha);
        
        poseStack.popPose();
    }
    
    /**
     * Apply rotation to face the correct direction.
     */
    private static void applyFaceRotation(PoseStack poseStack, Direction face) {
        switch (face) {
            case UP -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
            case DOWN -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
            case NORTH -> { } // Default facing
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
        }
    }
    
    /**
     * Submit a textured quad for the seal icon.
     */
    private static void renderSealQuad(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                       Identifier texture, 
                                       float r, float g, float b, float a) {
        Identifier actualTexture;
        if (texture == null) {
            actualTexture = DEFAULT_SEAL_TEXTURE;
        } else {
            // Convert from "items/seals/seal_xxx" format to full texture path
            String path = texture.getPath();
            if (!path.startsWith("textures/")) {
                path = "textures/" + path;
            }
            if (!path.endsWith(".png")) {
                path = path + ".png";
            }
            actualTexture = Identifier.fromNamespaceAndPath(texture.getNamespace(), path);
        }
        
        // Render a centered quad
        float size = 0.5f;
        
        submitNodeCollector.submitCustomGeometry(
            poseStack, RenderTypes.entityTranslucent(actualTexture),
            (pose, buffer) -> {
                buffer.addVertex(pose, -size, -size, 0.0F)
                    .setColor(r, g, b, a)
                    .setUv(0.0F, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, 0.0F, 0.0F, 1.0F);
                buffer.addVertex(pose, size, -size, 0.0F)
                    .setColor(r, g, b, a)
                    .setUv(1.0F, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, 0.0F, 0.0F, 1.0F);
                buffer.addVertex(pose, size, size, 0.0F)
                    .setColor(r, g, b, a)
                    .setUv(1.0F, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, 0.0F, 0.0F, 1.0F);
                buffer.addVertex(pose, -size, size, 0.0F)
                    .setColor(r, g, b, a)
                    .setUv(0.0F, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, 0.0F, 0.0F, 1.0F);
            }
        );
    }
}
