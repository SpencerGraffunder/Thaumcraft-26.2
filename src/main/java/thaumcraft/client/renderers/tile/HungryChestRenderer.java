package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.tile.state.HungryChestRenderState;
import thaumcraft.common.tiles.devices.TileHungryChest;

/**
 * Block entity renderer for the Hungry Chest.
 * Renders a chest model with animated lid opening/closing.
 */
@OnlyIn(Dist.CLIENT)
public class HungryChestRenderer implements BlockEntityRenderer<TileHungryChest, HungryChestRenderState> {

    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/chesthungry.png");

    private final ChestModel model;

    public HungryChestRenderer(BlockEntityRendererProvider.Context context) {
        // Use vanilla chest model
        this.model = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
    }

    @Override
    public HungryChestRenderState createRenderState() {
        return new HungryChestRenderState();
    }

    @Override
    public void extractRenderState(TileHungryChest tile, HungryChestRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        // Get facing direction
        Direction facing = Direction.NORTH;
        if (tile.getLevel() != null && tile.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        state.facing = facing;

        // Calculate lid angle with smooth interpolation
        float lidAngle = tile.prevLidAngle + (tile.lidAngle - tile.prevLidAngle) * partialTicks;
        lidAngle = 1.0f - lidAngle;
        lidAngle = 1.0f - lidAngle * lidAngle * lidAngle;
        state.lidOpen = lidAngle;
    }

    @Override
    public void submit(HungryChestRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        // Position at center of block
        poseStack.translate(0.5f, 0.5f, 0.5f);

        // Rotate based on facing
        float rotation = switch (state.facing) {
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case EAST -> -90.0f;
            default -> 0.0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Translate for chest model positioning
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        // Translate up by 1 block because chest model is positioned at y=0
        poseStack.translate(0.5, 1.0, 0.5);
        poseStack.scale(1.0f, -1.0f, -1.0f);

        // Submit the chest model; lid rotation is applied from state.lidOpen in ChestModel.setupAnim
        submitNodeCollector.submitModel(this.model, state.lidOpen, poseStack, this.model.renderType(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);

        poseStack.popPose();
    }
}
