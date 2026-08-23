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
import thaumcraft.client.models.entity.CrossbowModel;
import thaumcraft.client.renderers.entity.state.TurretCrossbowRenderState;
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
public class TurretCrossbowRenderer extends MobRenderer<EntityTurretCrossbow, TurretCrossbowRenderState, CrossbowModel> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/crossbow.png");
    
    public TurretCrossbowRenderer(EntityRendererProvider.Context context) {
        super(context, new CrossbowModel(context.bakeLayer(CrossbowModel.LAYER_LOCATION)), 0.5F);
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
    public void extractRenderState(EntityTurretCrossbow entity, TurretCrossbowRenderState state, float partialTick) {
        // Update load progress for animation
        entity.loadProgressForRender = entity.getLoadProgress(partialTick);
        
        // Reset yaw offset (turret rotates head, not body)
        entity.yBodyRot = 0.0F;
        entity.yBodyRotO = 0.0F;
        
        super.extractRenderState(entity, state, partialTick);
        
        state.loadProgress = entity.getLoadProgress(partialTick);
        state.attackAnim = entity.getAttackAnim(partialTick);
        state.passenger = entity.isPassenger();
    }
    
    @Override
    public void submit(TurretCrossbowRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        // Turrets have no custom render body - the model, shadow and name are handled by the base
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
    
    @Override
    protected boolean shouldShowName(EntityTurretCrossbow entity, double distanceToCameraSq) {
        return false; // Turrets don't show names
    }
}
