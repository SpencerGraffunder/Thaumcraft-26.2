package thaumcraft.common.entities.monster;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;

/**
 * EntityBrainyZombie - A zombie variant that drops zombie brains.
 * Has higher health and attack damage than regular zombies.
 * Does not spawn reinforcements.
 */
public class EntityBrainyZombie extends Zombie {
    
    public EntityBrainyZombie(EntityType<? extends EntityBrainyZombie> type, Level level) {
        super(type, level);
    }
    
    public EntityBrainyZombie(Level level) {
        super(ModEntities.BRAINY_ZOMBIE.get(), level);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        super.dropCustomDeathLoot(level, source, wasRecentlyHit);
        
        // Drop zombie brain with chance affected by 0
        if (random.nextInt(10) - 0 <= 4) {
            this.spawnAtLocation(level, new ItemStack(ModItems.ZOMBIE_BRAIN.get()), 1.5f);
        }
    }
    
    @Override
    public int getArmorValue() {
        return super.getArmorValue() + 1;
    }
}
