package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.EntityInhabitedZombie;

/**
 * Renderer for Inhabited Zombies - zombies containing eldritch crabs.
 * Uses vanilla zombie model with custom texture.
 */
@OnlyIn(Dist.CLIENT)
public class InhabitedZombieRenderer extends ZombieRenderer {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/czombie.png");
    
    public InhabitedZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public Identifier getTextureLocation(net.minecraft.world.entity.monster.Zombie entity) {
        return TEXTURE;
    }
}
