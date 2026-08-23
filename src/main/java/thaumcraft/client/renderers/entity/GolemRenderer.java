package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.client.models.entity.GolemModel;
import thaumcraft.client.renderers.entity.state.GolemRenderState;
import thaumcraft.common.golems.EntityThaumcraftGolem;

/**
 * GolemRenderer - Renders Thaumcraft golems in the world.
 * 
 * Features:
 * - Uses material-based textures
 * - Renders held items
 * - Scales based on golem size
 * 
 * This is a simplified version. The original used OBJ models
 * with swappable parts for heads, arms, legs, and addons.
 */
@OnlyIn(Dist.CLIENT)
public class GolemRenderer extends MobRenderer<EntityThaumcraftGolem, GolemRenderState, GolemModel> {
    
    // Default texture for golems without material
    private static final Identifier DEFAULT_TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/golems/mat_wood.png");
    
    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(GolemModel.LAYER_LOCATION)), 0.3F);
        
        // Add layer for held items
        this.addLayer(new ItemInHandLayer<>(this));
    }
    
    @Override
    public Identifier getTextureLocation(GolemRenderState state) {
        return state.texture;
    }
    
    @Override
    public GolemRenderState createRenderState() {
        return new GolemRenderState();
    }
    
    @Override
    public void extractRenderState(EntityThaumcraftGolem entity, GolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTick);
        
        // Get texture based on material
        if (entity.getProperties() != null && entity.getProperties().getMaterial() != null) {
            GolemMaterial material = entity.getProperties().getMaterial();
            if (material.texture != null) {
                state.texture = material.texture;
            } else {
                state.texture = DEFAULT_TEXTURE;
            }
        } else {
            state.texture = DEFAULT_TEXTURE;
        }
        
        // Animation data used by the model
        state.holdingItem = !entity.getMainHandItem().isEmpty();
        state.swinging = entity.swinging;
        state.attackAnim = entity.getAttackAnim(partialTick);
    }
    
    @Override
    protected void scale(GolemRenderState state, PoseStack poseStack) {
        // Base scale (golems are small)
        poseStack.scale(0.6F, 0.6F, 0.6F);
    }
    
    @Override
    protected boolean shouldShowName(EntityThaumcraftGolem entity, double distanceToCameraSq) {
        // Show name if golem is looked at while sneaking
        return super.shouldShowName(entity, distanceToCameraSq);
    }
}
