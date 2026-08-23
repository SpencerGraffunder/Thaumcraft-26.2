package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
import thaumcraft.client.renderers.tile.state.RechargePedestalRenderState;
import thaumcraft.common.tiles.devices.TileRechargePedestal;

/**
 * Block entity renderer for the Recharge Pedestal.
 * Renders a floating, spinning item on top of the pedestal, larger than regular pedestals.
 */
@OnlyIn(Dist.CLIENT)
public class RechargePedestalRenderer implements BlockEntityRenderer<TileRechargePedestal, RechargePedestalRenderState> {

    private final ItemModelResolver itemModelResolver;

    public RechargePedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public RechargePedestalRenderState createRenderState() {
        return new RechargePedestalRenderState();
    }

    @Override
    public void extractRenderState(TileRechargePedestal tile, RechargePedestalRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        state.item.clear();
        ItemStack stack = tile.getItem(0);
        if (!stack.isEmpty()) {
            ItemStack renderStack = stack.copy();
            renderStack.setCount(1);
            this.itemModelResolver.updateForTopItem(state.item, renderStack, ItemDisplayContext.GROUND, tile.getLevel(), null, 0);
        }

        state.ticks = Minecraft.getInstance().player != null ?
                Minecraft.getInstance().player.tickCount + partialTicks : 0;
    }

    @Override
    public void submit(RechargePedestalRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;

        poseStack.pushPose();

        // Position above pedestal
        poseStack.translate(0.5, 0.85, 0.5);

        // Larger scale for recharge pedestal
        poseStack.scale(1.5f, 1.5f, 1.5f);

        // Spin the item
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ticks % 360.0f));

        // Slight bob
        float bob = (float) Math.sin(state.ticks * 0.1f) * 0.05f;
        poseStack.translate(0, bob, 0);

        // Render the item
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}
