package thaumcraft.client.renderers.entity;

import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.EntityBrainyZombie;

/**
 * Renderer for Brainy Zombies - zombies with exposed brains.
 * Uses the vanilla zombie model with a custom texture.
 */
@OnlyIn(Dist.CLIENT)
public class BrainyZombieRenderer extends HumanoidMobRenderer<EntityBrainyZombie, ZombieModel<EntityBrainyZombie>> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/brainy_zombie.png");
    
    public BrainyZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }
    
    @Override
    public Identifier getTextureLocation(EntityBrainyZombie entity) {
        return TEXTURE;
    }
}
