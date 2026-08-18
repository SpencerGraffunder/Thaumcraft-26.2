package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.PechModel;
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
public class PechRenderer extends MobRenderer<EntityPech, PechModel> {
    
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
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }
    
    @Override
    public Identifier getTextureLocation(EntityPech entity) {
        return switch (entity.getPechType()) {
            case EntityPech.TYPE_MAGE -> TEXTURE_MAGE;
            case EntityPech.TYPE_FORAGER -> TEXTURE_STALKER;
            default -> TEXTURE_FORAGER;
        };
    }
    
    @Override
    public void render(EntityPech entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Adjust Y position when sneaking (pech has unique sneak animation)
        double yOffset = 0.0;
        if (entity.isShiftKeyDown()) {
            yOffset = -0.125;
        }
        
        poseStack.pushPose();
        poseStack.translate(0.0, yOffset, 0.0);
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        
        poseStack.popPose();
    }
    
    @Override
    protected void scale(EntityPech entity, PoseStack poseStack, float partialTicks) {
        // Pechs are slightly smaller than normal humanoids
        float scale = 0.9F;
        poseStack.scale(scale, scale, scale);
    }
}
