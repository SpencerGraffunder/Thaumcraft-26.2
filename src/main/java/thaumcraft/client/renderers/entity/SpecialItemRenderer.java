package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.renderers.entity.state.SpecialItemRenderState;
import thaumcraft.common.entities.EntitySpecialItem;

import java.util.Random;

/**
 * Renderer for EntitySpecialItem - items with magical glowing effects.
 * Renders glowing tendrils around the item for a mystical appearance.
 */
@OnlyIn(Dist.CLIENT)
public class SpecialItemRenderer extends EntityRenderer<EntitySpecialItem, SpecialItemRenderState> {
    
    private final ItemModelResolver itemModelResolver;
    private final Random random = new Random(187L);
    
    public SpecialItemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }
    
    @Override
    public SpecialItemRenderState createRenderState() {
        return new SpecialItemRenderState();
    }
    
    @Override
    public void extractRenderState(EntitySpecialItem entity, SpecialItemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.GROUND, entity);
        state.age = entity.getAge() + partialTick;
        state.ageInt = entity.getAge();
        state.bobOffs = entity.bobOffs;
    }
    
    @Override
    public void submit(SpecialItemRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        
        // Bobbing motion
        float bob = Mth.sin(state.age / 10.0F + state.bobOffs) * 0.1F + 0.1F;
        
        poseStack.pushPose();
        poseStack.translate(0.0D, bob + 0.25D, 0.0D);
        
        // Render glowing tendrils effect
        renderGlowingTendrils(state, poseStack, collector);
        
        // Render the actual item
        poseStack.pushPose();
        
        // Spin the item
        poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 2.0F));
        
        // Scale up slightly
        poseStack.scale(0.5F, 0.5F, 0.5F);
        
        // Render item with full brightness for magical glow
        state.item.submit(poseStack, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, state.outlineColor);
        
        poseStack.popPose();
        poseStack.popPose();
        
        super.submit(state, poseStack, collector, camera);
    }
    
    private void renderGlowingTendrils(SpecialItemRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        random.setSeed(187L);
        
        int count = Minecraft.getInstance().options.graphicsPreset().get() != GraphicsPreset.FAST ? 10 : 5;
        float ageFrac = state.ageInt / 500.0F;
        float scaleFrac = Math.min(state.ageInt, 10) / 10.0F;
        
        for (int i = 0; i < count; i++) {
            poseStack.pushPose();
            
            // Random rotation for each tendril
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F + ageFrac * 360.0F));
            
            // Scale based on entity age
            float length = (random.nextFloat() * 20.0F + 5.0F) / 30.0F * scaleFrac;
            float width = (random.nextFloat() * 2.0F + 1.0F) / 30.0F * scaleFrac;
            
            collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
                // Draw tendril as a triangle fan
                // Center vertex (white, opaque)
                buffer.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, 255);
                
                // Outer vertices (purple, transparent)
                buffer.addVertex(pose, (float)(-0.866 * width), length, (float)(-0.5 * width)).setColor(255, 0, 255, 0);
                buffer.addVertex(pose, (float)(0.866 * width), length, (float)(-0.5 * width)).setColor(255, 0, 255, 0);
                buffer.addVertex(pose, 0.0F, length, width).setColor(255, 0, 255, 0);
                buffer.addVertex(pose, (float)(-0.866 * width), length, (float)(-0.5 * width)).setColor(255, 0, 255, 0);
            });
            
            poseStack.popPose();
        }
    }
}
