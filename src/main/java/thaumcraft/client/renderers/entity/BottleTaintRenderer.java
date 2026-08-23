package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.renderers.entity.state.BottleTaintRenderState;
import thaumcraft.common.entities.projectile.EntityBottleTaint;
import thaumcraft.init.ModItems;

/**
 * Renderer for thrown taint bottles - renders as a spinning item.
 */
@OnlyIn(Dist.CLIENT)
public class BottleTaintRenderer extends EntityRenderer<EntityBottleTaint, BottleTaintRenderState> {
    
    private final ItemModelResolver itemModelResolver;
    private ItemStack cachedItem;
    
    public BottleTaintRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }
    
    private ItemStack getItemStack() {
        if (cachedItem == null) {
            cachedItem = new ItemStack(ModItems.BOTTLE_TAINT.get());
        }
        return cachedItem;
    }
    
    @Override
    public BottleTaintRenderState createRenderState() {
        return new BottleTaintRenderState();
    }
    
    @Override
    public void extractRenderState(EntityBottleTaint entity, BottleTaintRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.itemModelResolver.updateForNonLiving(state.item, getItemStack(), ItemDisplayContext.GROUND, entity);
        state.yRot = entity.getYRot(partialTick);
        state.spinAge = entity.tickCount + partialTick;
    }
    
    @Override
    public void submit(BottleTaintRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        
        poseStack.pushPose();
        
        // Spinning motion
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F - state.yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.spinAge * 20.0F));
        
        // Scale down slightly
        poseStack.scale(0.5F, 0.5F, 0.5F);
        
        // Render the bottle taint item
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        
        poseStack.popPose();
        
        super.submit(state, poseStack, collector, camera);
    }
}
