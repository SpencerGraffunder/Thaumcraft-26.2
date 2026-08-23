package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.boss.EntityCultistLeader;

/**
 * Renderer for Cultist Leader - boss version of cultists.
 * Larger scale and uses standard cultist texture.
 */
@OnlyIn(Dist.CLIENT)
public class CultistLeaderRenderer extends HumanoidMobRenderer<EntityCultistLeader, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/cultist.png");
    
    public CultistLeaderRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()));
    }
    
    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
    
    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }
    
    @Override
    protected void scale(HumanoidRenderState state, PoseStack poseStack) {
        // Cultist leader is 15% larger than normal
        poseStack.scale(1.15F, 1.15F, 1.15F);
    }
}
