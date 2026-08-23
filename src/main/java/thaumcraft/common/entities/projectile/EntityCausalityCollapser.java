package thaumcraft.common.entities.projectile;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.init.ModEntities;

/**
 * EntityCausalityCollapser - Special projectile that closes flux rifts.
 * 
 * Features:
 * - Creates explosion on impact
 * - Collapses any flux rifts within 3 block radius
 * - Orange/red particle trail
 */
public class EntityCausalityCollapser extends ThrowableProjectile {
    
    public EntityCausalityCollapser(EntityType<? extends EntityCausalityCollapser> type, Level level) {
        super(type, level);
    }
    
    public EntityCausalityCollapser(Level level, LivingEntity owner) {
        super(ModEntities.CAUSALITY_COLLAPSER.get(), level);
        this.setOwner(owner);
    }
    
    public EntityCausalityCollapser(Level level, double x, double y, double z) {
        super(ModEntities.CAUSALITY_COLLAPSER.get(), x, y, z, level);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No additional synced data
    }
    
    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        // Fixed velocity of 0.8
        super.shoot(x, y, z, 0.8f, inaccuracy);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Client-side particles - orange/red trail
        if (level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                double coeff = i / 3.0;
                double px = xOld + (getX() - xOld) * coeff;
                double py = yOld + (getY() - yOld) * coeff + getBbHeight() / 2.0f;
                double pz = zOld + (getZ() - zOld) * coeff;
                
                // Orange/red flame particles
                level().addParticle(ParticleTypes.FLAME,
                    px + (random.nextFloat() - 0.5) * 0.2,
                    py + (random.nextFloat() - 0.5) * 0.2,
                    pz + (random.nextFloat() - 0.5) * 0.2,
                    (random.nextFloat() - 0.5) * 0.02,
                    (random.nextFloat() - 0.5) * 0.02,
                    (random.nextFloat() - 0.5) * 0.02);
                
                // Bright spark particles
                level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    px + (random.nextFloat() - 0.5) * 0.2,
                    py + (random.nextFloat() - 0.5) * 0.2,
                    pz + (random.nextFloat() - 0.5) * 0.2,
                    0, 0, 0);
            }
        }
    }
    
    @Override
    protected void onHit(HitResult result) {
        if (!level().isClientSide()) {
            // Create medium explosion (larger than alumentum)
            level().explode(this, getX(), getY(), getZ(), 2.0f, Level.ExplosionInteraction.TNT);
            
            // TODO: Find and collapse nearby flux rifts when EntityFluxRift is implemented
            // List<EntityFluxRift> rifts = EntityUtils.getEntitiesInRange(level(), getX(), getY(), getZ(), this, EntityFluxRift.class, 3.0);
            // for (EntityFluxRift rift : rifts) {
            //     rift.setCollapse(true);
            // }
            
            discard();
        }
    }
    
    @Override
    protected double getDefaultGravity() {
        return 0.03f; // Standard throwable gravity
    }
}
