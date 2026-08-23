package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.renderers.tile.state.CrucibleRenderState;
import thaumcraft.common.tiles.crafting.TileCrucible;

/**
 * Block entity renderer for the Crucible.
 * Renders the water/essentia fluid inside the crucible.
 * The fluid color shifts from blue (water) to purple as more aspects are added.
 */
@OnlyIn(Dist.CLIENT)
public class CrucibleRenderer implements BlockEntityRenderer<TileCrucible, CrucibleRenderState> {

    private final SpriteGetter sprites;

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public CrucibleRenderState createRenderState() {
        return new CrucibleRenderState();
    }

    @Override
    public void extractRenderState(TileCrucible tile, CrucibleRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        // Only render if there's fluid in the crucible
        state.hasFluid = tile.getTank().getFluidAmount() > 0;
        if (!state.hasFluid) return;

        state.fluidHeight = tile.getFluidHeight();

        // Calculate color shift based on aspect content
        // More aspects = more purple/magic colored
        float aspectRatio = (float) tile.getAspects().visSize() / TileCrucible.MAX_ASPECTS;
        aspectRatio = Math.min(1.0f, aspectRatio);

        // Base water color to purple magic color
        if (tile.isHeated()) {
            // When heated, shift from blue toward purple based on aspects
            state.r = 0.3f + aspectRatio * 0.5f;  // More red when more aspects
            state.g = 0.3f - aspectRatio * 0.2f;  // Less green when more aspects
            state.b = 0.9f;                        // Keep blue high
            state.light = 0x00F000A0;
        } else {
            // When cold, just regular water color
            state.r = 0.2f;
            state.g = 0.4f;
            state.b = 0.8f;
            state.light = state.lightCoords;
        }
        state.a = 0.85f;
    }

    @Override
    public void submit(CrucibleRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.hasFluid) return;

        // Get water texture from the fluid model
        net.minecraft.client.renderer.block.FluidModel fluidModel = net.minecraft.client.Minecraft.getInstance()
                .getModelManager().getFluidStateModelSet().get(Fluids.WATER.defaultFluidState());
        TextureAtlasSprite sprite = fluidModel.stillMaterial().sprite();

        // Render the fluid surface
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Fluid surface bounds (inside the crucible walls)
            float minX = 0.125f;  // 2/16
            float maxX = 0.875f;  // 14/16
            float minZ = 0.125f;
            float maxZ = 0.875f;
            float y = state.fluidHeight;

            // UV coordinates from the sprite
            float u0 = sprite.getU0();
            float u1 = sprite.getU1();
            float v0 = sprite.getV0();
            float v1 = sprite.getV1();

            // Use combined light from block (make it glow slightly when heated)
            int light = state.light;

            // Top surface (Y+)
            buffer.addVertex(matrix, minX, y, minZ).setColor(state.r, state.g, state.b, state.a)
                    .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(0, 1, 0);
            buffer.addVertex(matrix, minX, y, maxZ).setColor(state.r, state.g, state.b, state.a)
                    .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(0, 1, 0);
            buffer.addVertex(matrix, maxX, y, maxZ).setColor(state.r, state.g, state.b, state.a)
                    .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(0, 1, 0);
            buffer.addVertex(matrix, maxX, y, minZ).setColor(state.r, state.g, state.b, state.a)
                    .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(0, 1, 0);
        });
    }
}
