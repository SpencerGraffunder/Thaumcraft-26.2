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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.tile.state.GolemBuilderRenderState;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;

/**
 * Block entity renderer for the Golem Builder.
 * Renders the press mechanism and lava pool.
 */
@OnlyIn(Dist.CLIENT)
public class GolemBuilderRenderer implements BlockEntityRenderer<TileGolemBuilder, GolemBuilderRenderState> {

    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/golembuilder.png");

    private final SpriteGetter sprites;

    public GolemBuilderRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public GolemBuilderRenderState createRenderState() {
        return new GolemBuilderRenderState();
    }

    @Override
    public void extractRenderState(TileGolemBuilder tile, GolemBuilderRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        // Get facing direction
        Direction facing = Direction.NORTH;
        if (tile.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        state.facing = facing;
        state.pressAnimation = tile.pressAnimation;
    }

    @Override
    public void submit(GolemBuilderRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);

        // Rotate based on facing
        float rotation = switch (state.facing) {
            case EAST -> 270.0f;
            case WEST -> 90.0f;
            case SOUTH -> 180.0f;
            case NORTH -> 0.0f;
            default -> 0.0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Render the press mechanism
        renderPress(state, poseStack, submitNodeCollector);

        // Render the lava pool
        renderLavaPool(poseStack, submitNodeCollector);

        poseStack.popPose();
    }

    /**
     * Render the press mechanism.
     */
    private void renderPress(GolemBuilderRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        poseStack.pushPose();

        // Calculate press offset based on animation
        float pressHeight = state.pressAnimation;
        double offset = Math.sin(Math.toRadians(pressHeight)) * 0.625;
        poseStack.translate(0, -offset, 0);

        // Render press as a simple box (placeholder - original uses OBJ model)
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Press is a flat plate that moves down
            float width = 0.375f;
            float height = 0.125f;
            float depth = 0.375f;
            float yPos = 0.875f;

            // Top face
            buffer.addVertex(matrix, -width, yPos, -depth).setColor(150, 150, 150, 255)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 1, 0);
            buffer.addVertex(matrix, -width, yPos, depth).setColor(150, 150, 150, 255)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 1, 0);
            buffer.addVertex(matrix, width, yPos, depth).setColor(150, 150, 150, 255)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 1, 0);
            buffer.addVertex(matrix, width, yPos, -depth).setColor(150, 150, 150, 255)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, 1, 0);

            // Bottom face
            buffer.addVertex(matrix, -width, yPos - height, depth).setColor(100, 100, 100, 255)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, -1, 0);
            buffer.addVertex(matrix, -width, yPos - height, -depth).setColor(100, 100, 100, 255)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, -1, 0);
            buffer.addVertex(matrix, width, yPos - height, -depth).setColor(100, 100, 100, 255)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, -1, 0);
            buffer.addVertex(matrix, width, yPos - height, depth).setColor(100, 100, 100, 255)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(0, -1, 0);
        });

        poseStack.popPose();
    }

    /**
     * Render the lava pool at the bottom.
     */
    private void renderLavaPool(PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        poseStack.pushPose();
        poseStack.translate(-0.3125, 0.625, 0.3125 + 1.0);
        poseStack.mulPose(Axis.XN.rotationDegrees(90));

        // Get lava texture from the fluid model
        net.minecraft.client.renderer.block.FluidModel fluidModel = net.minecraft.client.Minecraft.getInstance()
                .getModelManager().getFluidStateModelSet().get(Fluids.LAVA.defaultFluidState());
        TextureAtlasSprite lavaSprite = fluidModel.stillMaterial().sprite();

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float size = 0.625f;
            int fullBright = 0x00F000F0; // Full brightness for lava

            float u0 = lavaSprite.getU0();
            float u1 = lavaSprite.getU1();
            float v0 = lavaSprite.getV0();
            float v1 = lavaSprite.getV1();

            buffer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255)
                    .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullBright).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, 0, 0).setColor(255, 255, 255, 255)
                    .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullBright).setNormal(0, 0, 1);
            buffer.addVertex(matrix, size, size, 0).setColor(255, 255, 255, 255)
                    .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullBright).setNormal(0, 0, 1);
            buffer.addVertex(matrix, 0, size, 0).setColor(255, 255, 255, 255)
                    .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullBright).setNormal(0, 0, 1);
        });

        poseStack.popPose();
    }
}
