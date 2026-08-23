package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.entity.state.CultistRenderState;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.monster.cult.EntityCultistCleric;

import java.util.Random;

/**
 * Renderer for Cultists - hooded followers of the Crimson Cult.
 * Uses the humanoid biped model with custom textures.
 * Ritualist clerics float and have a ritual tether line.
 */
@OnlyIn(Dist.CLIENT)
public class CultistRenderer extends HumanoidMobRenderer<EntityCultist, CultistRenderState, HumanoidModel<CultistRenderState>> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/cultist.png");
    
    public CultistRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()));
    }
    
    @Override
    public Identifier getTextureLocation(CultistRenderState state) {
        return TEXTURE;
    }
    
    @Override
    public CultistRenderState createRenderState() {
        return new CultistRenderState();
    }
    
    @Override
    public void extractRenderState(EntityCultist entity, CultistRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Floating animation for ritualist clerics
        if (entity instanceof EntityCultistCleric cleric && cleric.isRitualist()) {
            int seed = new Random(entity.getId()).nextInt(1000);
            float time = entity.tickCount + partialTick + seed;
            state.floatBob = Mth.sin(time / 9.0F) * 0.1F + 0.21F;
        } else {
            state.floatBob = 0.0F;
        }
    }
    
    @Override
    public void submit(CultistRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.floatBob != 0.0F) {
            poseStack.pushPose();
            poseStack.translate(0.0, state.floatBob, 0.0);
            super.submit(state, poseStack, collector, camera);
            poseStack.popPose();
        } else {
            super.submit(state, poseStack, collector, camera);
        }
    }
}
