package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.CrossbowAdvancedModel;
import thaumcraft.client.renderers.entity.state.TurretCrossbowRenderState;
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
public class TurretCrossbowAdvancedRenderer extends MobRenderer<EntityTurretCrossbowAdvanced, TurretCrossbowRenderState, CrossbowAdvancedModel> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/crossbow_advanced.png");
    
    public TurretCrossbowAdvancedRenderer(EntityRendererProvider.Context context) {
        super(context, new CrossbowAdvancedModel(context.bakeLayer(CrossbowAdvancedModel.LAYER_LOCATION)), 0.5F);
    }
    
    @Override
    public Identifier getTextureLocation(TurretCrossbowRenderState state) {
        return TEXTURE;
    }
    
    @Override
    public TurretCrossbowRenderState createRenderState() {
        return new TurretCrossbowRenderState();
    }
    
    @Override
    public void extractRenderState(EntityTurretCrossbowAdvanced entity, TurretCrossbowRenderState state, float partialTick) {
        // Update load progress for animation
        entity.loadProgressForRender = entity.getLoadProgress(partialTick);
        
        // Reset yaw offset (turret rotates mech, not body)
        entity.yBodyRot = 0.0F;
        entity.yBodyRotO = 0.0F;
        
        super.extractRenderState(entity, state, partialTick);
        
        state.loadProgress = entity.getLoadProgress(partialTick);
        state.attackAnim = entity.getAttackAnim(partialTick);
        state.passenger = entity.isPassenger();
        
        // Hurt jiggle effect - computed once per frame in extract so submit stays deterministic
        if (entity.hurtTime > 0) {
            float jiggle = entity.hurtTime / 500.0F;
            state.jiggleX = (float) entity.getRandom().nextGaussian() * jiggle;
            state.jiggleY = (float) entity.getRandom().nextGaussian() * jiggle;
            state.jiggleZ = (float) entity.getRandom().nextGaussian() * jiggle;
        } else {
            state.jiggleX = 0.0F;
            state.jiggleY = 0.0F;
            state.jiggleZ = 0.0F;
        }
    }
    
    @Override
    public void submit(TurretCrossbowRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        // Turrets have no custom render body - the model, shadow and name are handled by the base
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
    
    @Override
    protected void scale(TurretCrossbowRenderState state, PoseStack poseStack) {
        // Hurt jiggle effect
        poseStack.translate(state.jiggleX, state.jiggleY, state.jiggleZ);
    }
    
    @Override
    protected boolean shouldShowName(EntityTurretCrossbowAdvanced entity, double distanceToCameraSq) {
        return false; // Turrets don't show names
    }
}
