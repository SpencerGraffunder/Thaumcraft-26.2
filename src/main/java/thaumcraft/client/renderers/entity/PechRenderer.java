package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.PechModel;
import thaumcraft.client.renderers.entity.state.PechRenderState;
import thaumcraft.common.entities.monster.EntityPech;

/**
 * Renderer for the Pech mob.
 * 
 * Features:
 * - Different textures for different pech types (forager, mage, regular)
 * - Renders held items (bow, wand, or melee weapon)
 * - Animated jowls for mumbling
 */
@OnlyIn(Dist.CLIENT)
public class PechRenderer extends MobRenderer<EntityPech, PechRenderState, PechModel> {
    
    // Textures for different pech types
    private static final Identifier TEXTURE_FORAGER = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/pech_forage.png");
    private static final Identifier TEXTURE_MAGE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/pech_thaum.png");
    private static final Identifier TEXTURE_STALKER = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/pech_stalker.png");
    
    public PechRenderer(EntityRendererProvider.Context context) {
        super(context, new PechModel(context.bakeLayer(PechModel.LAYER_LOCATION)), 0.4F);
        
        // Add layer for held items
        this.addLayer(new ItemInHandLayer<>(this));
    }
    
    @Override
    public Identifier getTextureLocation(PechRenderState state) {
        return state.texture;
    }
    
    @Override
    public PechRenderState createRenderState() {
        return new PechRenderState();
    }
    
    @Override
    public void extractRenderState(EntityPech entity, PechRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTick);
        
        state.texture = switch (entity.getPechType()) {
            case EntityPech.TYPE_MAGE -> TEXTURE_MAGE;
            case EntityPech.TYPE_FORAGER -> TEXTURE_STALKER;
            default -> TEXTURE_FORAGER;
        };
        
        // Sneak offset and animation data used by the model
        state.sneakOffsetY = entity.isShiftKeyDown() ? -0.125F : 0.0F;
        state.mumble = entity.getMumble();
        state.sneaking = entity.isShiftKeyDown();
        state.riding = entity.isPassenger();
        state.attackTime = entity.getAttackAnim(partialTick);
    }
    
    @Override
    public void submit(PechRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // Adjust Y position when sneaking (pech has unique sneak animation)
        poseStack.pushPose();
        poseStack.translate(0.0, state.sneakOffsetY, 0.0);
        super.submit(state, poseStack, collector, camera);
        poseStack.popPose();
    }
    
    @Override
    protected void scale(PechRenderState state, PoseStack poseStack) {
        // Pechs are slightly smaller than normal humanoids
        poseStack.scale(0.9F, 0.9F, 0.9F);
    }
}
