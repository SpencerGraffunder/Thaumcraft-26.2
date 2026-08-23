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
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.renderers.tile.state.ThaumatoriumRenderState;
import thaumcraft.common.tiles.crafting.TileThaumatorium;

/**
 * Block entity renderer for the Thaumatorium.
 * Renders the floating result item on the display shelf.
 */
@OnlyIn(Dist.CLIENT)
public class ThaumatoriumRenderer implements BlockEntityRenderer<TileThaumatorium, ThaumatoriumRenderState> {

    private final ItemModelResolver itemModelResolver;

    public ThaumatoriumRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public ThaumatoriumRenderState createRenderState() {
        return new ThaumatoriumRenderState();
    }

    @Override
    public void extractRenderState(TileThaumatorium tile, ThaumatoriumRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        // Get facing direction
        Direction facing = Direction.NORTH;
        if (tile.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        state.facing = facing;

        // Get the result item to display
        state.item.clear();
        ItemStack displayItem = tile.getRecipeResult();
        if (displayItem == null || displayItem.isEmpty()) {
            // If no recipe in progress, show output slot item
            displayItem = tile.getItem(TileThaumatorium.OUTPUT_SLOT);
        }

        if (displayItem != null && !displayItem.isEmpty()) {
            this.itemModelResolver.updateForTopItem(state.item, displayItem, ItemDisplayContext.FIXED, tile.getLevel(), null, 0);
        }
    }

    @Override
    public void submit(ThaumatoriumRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;

        poseStack.pushPose();

        // Position at the front display shelf
        poseStack.translate(0.5, 1.125, 0.5);

        // Offset based on facing
        float offsetX = state.facing.getStepX() / 1.99f;
        float offsetZ = state.facing.getStepZ() / 1.99f;
        poseStack.translate(offsetX, 0, offsetZ);

        // Rotate to face outward
        float rotation = switch (state.facing) {
            case EAST -> 90.0f;
            case WEST -> 270.0f;
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            default -> 0.0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Scale down item
        poseStack.scale(0.75f, 0.75f, 0.75f);

        // Render the item
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}
