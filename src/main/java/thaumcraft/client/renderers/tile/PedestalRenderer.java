package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.renderers.tile.state.PedestalRenderState;
import thaumcraft.common.tiles.crafting.TilePedestal;

/**
 * Block entity renderer for Pedestals.
 * Renders the item placed on the pedestal with a floating/spinning animation.
 */
@OnlyIn(Dist.CLIENT)
public class PedestalRenderer implements BlockEntityRenderer<TilePedestal, PedestalRenderState> {

    private final ItemModelResolver itemModelResolver;

    public PedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public PedestalRenderState createRenderState() {
        return new PedestalRenderState();
    }

    @Override
    public void extractRenderState(TilePedestal tile, PedestalRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        state.item.clear();
        ItemStack stack = tile.getItem(0);
        if (!stack.isEmpty()) {
            this.itemModelResolver.updateForTopItem(state.item, stack, ItemDisplayContext.FIXED, tile.getLevel(), null, 0);
        }

        // Bobbing and rotation animation time
        state.time = (tile.getLevel() != null ? tile.getLevel().getGameTime() : 0) + partialTicks;
    }

    @Override
    public void submit(PedestalRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Move to center of pedestal, above the base
        poseStack.translate(0.5, 1.1, 0.5);

        // Bobbing animation
        float bob = (float) Math.sin(state.time * 0.1) * 0.05F;
        poseStack.translate(0, bob, 0);

        // Rotation animation
        float rotation = state.time * 2.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Scale the item
        poseStack.scale(0.5F, 0.5F, 0.5F);

        // Render the item
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}
