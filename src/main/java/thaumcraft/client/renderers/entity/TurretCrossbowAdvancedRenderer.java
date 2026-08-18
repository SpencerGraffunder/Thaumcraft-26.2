package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.CrossbowAdvancedModel;
import thaumcraft.common.entities.construct.EntityTurretCrossbowAdvanced;

/**
 * Renderer for the advanced crossbow turret.
 * 
 * Features:
 * - Rotating mechanism that aims at targets
 * - Animated loader mechanism during reload
 * - Animated bow arms when firing
 * - Shield, box, and brain modules
 */
@OnlyIn(Dist.CLIENT)
public class TurretCrossbowAdvancedRenderer extends MobRenderer<EntityTurretCrossbowAdvanced, CrossbowAdvancedModel> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/crossbow_advanced.png");
    
    public TurretCrossbowAdvancedRenderer(EntityRendererProvider.Context context) {
        super(context, new CrossbowAdvancedModel(context.bakeLayer(CrossbowAdvancedModel.LAYER_LOCATION)), 0.5F);
    }
    
    @Override
    public Identifier getTextureLocation(EntityTurretCrossbowAdvanced entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityTurretCrossbowAdvanced entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Update load progress for animation
        entity.loadProgressForRender = entity.getLoadProgress(partialTicks);
        
        // Reset yaw offset (turret rotates mech, not body)
        entity.yBodyRot = 0.0F;
        entity.yBodyRotO = 0.0F;
        
        // Apply hurt color tint
        if (entity.hurtTime > 0) {
            // The super.render will handle the hurt overlay
        }
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    @Override
    protected void scale(EntityTurretCrossbowAdvanced entity, PoseStack poseStack, float partialTicks) {
        // Advanced turret is slightly larger
        poseStack.scale(1.0F, 1.0F, 1.0F);
        
        // Hurt jiggle effect
        if (entity.hurtTime > 0) {
            float jiggle = entity.hurtTime / 500.0F;
            poseStack.translate(
                entity.getRandom().nextGaussian() * jiggle, 
                entity.getRandom().nextGaussian() * jiggle, 
                entity.getRandom().nextGaussian() * jiggle
            );
        }
        
        super.scale(entity, poseStack, partialTicks);
    }
    
    @Override
    protected boolean shouldShowName(EntityTurretCrossbowAdvanced entity) {
        return false; // Turrets don't show names
    }
}
