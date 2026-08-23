package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.block.BrainModel;
import thaumcraft.client.renderers.tile.state.JarBrainRenderState;
import thaumcraft.common.tiles.essentia.TileJarBrain;

/**
 * Block entity renderer for Brain in a Jar.
 * Renders a floating, rotating brain inside the jar.
 */
@OnlyIn(Dist.CLIENT)
public class JarBrainRenderer implements BlockEntityRenderer<TileJarBrain, JarBrainRenderState> {

    private static final Identifier BRAIN_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/brain.png");

    private final BrainModel brainModel;

    public JarBrainRenderer(BlockEntityRendererProvider.Context context) {
        this.brainModel = new BrainModel(context.bakeLayer(BrainModel.LAYER_LOCATION));
    }

    @Override
    public JarBrainRenderState createRenderState() {
        return new JarBrainRenderState();
    }

    @Override
    public void extractRenderState(TileJarBrain tile, JarBrainRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        // Interpolate animation values for smooth motion
        state.rotation = Mth.lerp(partialTicks, tile.brainRotationPrev, tile.brainRotation);
        state.yOffset = Mth.lerp(partialTicks, tile.brainYPrev, tile.brainY);
    }

    @Override
    public void submit(JarBrainRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        // Position brain in center of jar, slightly raised
        // The jar is roughly 1 block tall, brain should float in the middle-upper portion
        poseStack.translate(0.5, 0.35 + state.yOffset, 0.5);

        // Apply rotation around Y axis (slow spin)
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotation));

        // Scale down the brain to fit inside the jar
        // Original model is designed at 1/16 scale, we need it smaller for the jar
        float scale = 0.03125f; // 1/32 scale
        poseStack.scale(scale, scale, scale);

        // Flip the model (models are often upside down)
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));

        // Render the brain model with a slightly pink/flesh color tint
        submitNodeCollector.submitModel(this.brainModel, Unit.INSTANCE, poseStack, this.brainModel.renderType(BRAIN_TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFD9D9D9, null, 0, state.breakProgress);

        poseStack.popPose();
    }
}
