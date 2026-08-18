package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.CrossbowModel;
import thaumcraft.common.entities.construct.EntityTurretCrossbow;

/**
 * Renderer for the basic crossbow turret.
 * 
 * Features:
 * - Rotating head that aims at targets
 * - Animated loading mechanism
 * - Animated bow arms when firing
 */
@OnlyIn(Dist.CLIENT)
public class TurretCrossbowRenderer extends MobRenderer<EntityTurretCrossbow, CrossbowModel> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/crossbow.png");
    
    public TurretCrossbowRenderer(EntityRendererProvider.Context context) {
        super(context, new CrossbowModel(context.bakeLayer(CrossbowModel.LAYER_LOCATION)), 0.5F);
    }
    
    @Override
    public Identifier getTextureLocation(EntityTurretCrossbow entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityTurretCrossbow entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Update load progress for animation
        entity.loadProgressForRender = entity.getLoadProgress(partialTicks);
        
        // Reset yaw offset (turret rotates head, not body)
        entity.yBodyRot = 0.0F;
        entity.yBodyRotO = 0.0F;
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    @Override
    protected void scale(EntityTurretCrossbow entity, PoseStack poseStack, float partialTicks) {
        // Turrets are a fixed size, no scaling needed
        super.scale(entity, poseStack, partialTicks);
    }
    
    @Override
    protected boolean shouldShowName(EntityTurretCrossbow entity) {
        return false; // Turrets don't show names
    }
}
