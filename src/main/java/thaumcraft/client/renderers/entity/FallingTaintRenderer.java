package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.entities.EntityFallingTaint;

/**
 * Renderer for Falling Taint blocks.
 * Renders the falling block state using the block model renderer.
 */
@OnlyIn(Dist.CLIENT)
public class FallingTaintRenderer extends EntityRenderer<EntityFallingTaint, FallingBlockRenderState> {
    
    public FallingTaintRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }
    
    @Override
    public FallingBlockRenderState createRenderState() {
        return new FallingBlockRenderState();
    }
    
    @Override
    public void extractRenderState(EntityFallingTaint entity, FallingBlockRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        state.movingBlockRenderState.randomSeedPos = entity.blockPosition();
        state.movingBlockRenderState.blockPos = pos;
        state.movingBlockRenderState.blockState = entity.getBlockState();
        if (entity.level() instanceof ClientLevel clientLevel) {
            state.movingBlockRenderState.biome = clientLevel.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }
    }
    
    @Override
    public void submit(FallingBlockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        BlockState blockState = state.movingBlockRenderState.blockState;
        if (blockState != null && blockState.getRenderShape() == RenderShape.MODEL) {
            poseStack.pushPose();
            
            // Center the block
            poseStack.translate(-0.5, 0.0, -0.5);
            
            submitNodeCollector.submitMovingBlock(poseStack, state.movingBlockRenderState, state.outlineColor);
            
            poseStack.popPose();
            
            super.submit(state, poseStack, submitNodeCollector, camera);
        }
    }
}
