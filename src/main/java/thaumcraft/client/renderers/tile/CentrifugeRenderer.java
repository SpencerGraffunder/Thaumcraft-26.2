package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.block.CentrifugeModel;
import thaumcraft.client.renderers.tile.state.CentrifugeRenderState;
import thaumcraft.common.tiles.essentia.TileCentrifuge;

/**
 * Block entity renderer for the Centrifuge.
 * Renders a custom model with a spinning inner mechanism.
 */
@OnlyIn(Dist.CLIENT)
public class CentrifugeRenderer implements BlockEntityRenderer<TileCentrifuge, CentrifugeRenderState> {

    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/centrifuge.png");

    private final CentrifugeModel model;

    public CentrifugeRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new CentrifugeModel(context.bakeLayer(CentrifugeModel.LAYER_LOCATION));
    }

    @Override
    public CentrifugeRenderState createRenderState() {
        return new CentrifugeRenderState();
    }

    @Override
    public void extractRenderState(TileCentrifuge tile, CentrifugeRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        // Calculate interpolated rotation for smooth animation
        state.rotation = (float) Math.toRadians(Mth.lerp(partialTicks, tile.spinLast, tile.spin));
    }

    @Override
    public void submit(CentrifugeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        // Move to block center
        poseStack.translate(0.5, 0.5, 0.5);

        // Scale down to fit within block (model is in model space at 1/16 scale factor)
        poseStack.scale(0.0625f, 0.0625f, 0.0625f);

        // Render the whole model (static parts + spinning mechanism; rotation applied in model.setupAnim)
        submitNodeCollector.submitModel(this.model, state, poseStack, this.model.renderType(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, 0, state.breakProgress);

        poseStack.popPose();
    }
}
