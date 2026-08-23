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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.tile.state.PatternCrafterRenderState;
import thaumcraft.common.tiles.crafting.TilePatternCrafter;

/**
 * Block entity renderer for the Pattern Crafter.
 * Renders the mode display and rotating gears.
 */
@OnlyIn(Dist.CLIENT)
public class PatternCrafterRenderer implements BlockEntityRenderer<TilePatternCrafter, PatternCrafterRenderState> {

    private static final Identifier MODES_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/pattern_crafter_modes.png");
    private static final Identifier GEAR_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/gear_brass.png");

    public PatternCrafterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PatternCrafterRenderState createRenderState() {
        return new PatternCrafterRenderState();
    }

    @Override
    public void extractRenderState(TilePatternCrafter tile, PatternCrafterRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        // Get facing direction
        Direction facing = Direction.NORTH;
        if (tile.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        state.facing = facing;
        state.type = tile.type;
        state.rot = tile.rot;
    }

    @Override
    public void submit(PatternCrafterRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.75, 0.5);

        // Rotate based on facing
        float rotation = switch (state.facing) {
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

        renderModeDisplay(state, poseStack, submitNodeCollector);

        poseStack.popPose();

        // Render left gear
        poseStack.pushPose();
        poseStack.translate(-0.2, -0.40625, 0.05);
        float gearRot = -state.rot % 360.0f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(gearRot));
        poseStack.scale(0.5f, 0.5f, 1.0f);
        poseStack.translate(-0.5, -0.5, 0);

        renderGear(poseStack, submitNodeCollector, state.lightCoords);

        poseStack.popPose();

        // Render right gear
        poseStack.pushPose();
        poseStack.translate(0.2, -0.40625, 0.05);
        gearRot = state.rot % 360.0f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(gearRot));
        poseStack.scale(0.5f, 0.5f, 1.0f);
        poseStack.translate(-0.5, -0.5, 0);

        renderGear(poseStack, submitNodeCollector, state.lightCoords);

        poseStack.popPose();

        poseStack.popPose();
    }

    /**
     * Render the mode indicator display.
     */
    private void renderModeDisplay(PatternCrafterRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(MODES_TEXTURE), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Calculate UV for the specific mode (10 modes in a row)
            float uMin = state.type / 10.0f;
            float uMax = (state.type + 1) / 10.0f;
            float vMin = 0;
            float vMax = 1;

            float size = 0.5f;

            buffer.addVertex(matrix, -size, -size, 0).setColor(255, 255, 255, 255)
                    .setUv(uMin, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, -size, 0).setColor(255, 255, 255, 255)
                    .setUv(uMax, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, size, 0).setColor(255, 255, 255, 255)
                    .setUv(uMax, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
            buffer.addVertex(matrix, -size, size, 0).setColor(255, 255, 255, 255)
                    .setUv(uMin, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 0, 1);
        });
    }

    /**
     * Render a brass gear.
     */
    private void renderGear(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(GEAR_TEXTURE), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float size = 1.0f;

            buffer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, 0, 0).setColor(255, 255, 255, 255)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, size, 0).setColor(255, 255, 255, 255)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
            buffer.addVertex(matrix, 0, size, 0).setColor(255, 255, 255, 255)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        });
    }
}
