package thaumcraft.common.entities.monster.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.init.ModEntities;

import javax.annotation.Nullable;

/**
 * EntityCultist - Base class for Crimson Cult members.
 * These hostile humans worship the eldritch entities and attack players.
 */
public class EntityCultist extends Monster {
    
    // Home position for restricting movement
    private BlockPos homePos;
    private int homeDistance;
    
    public EntityCultist(EntityType<? extends EntityCultist> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
        
        // Set equipment drop chances
        setDropChance(EquipmentSlot.HEAD, 0.05f);
        setDropChance(EquipmentSlot.CHEST, 0.05f);
        setDropChance(EquipmentSlot.LEGS, 0.05f);
        setDropChance(EquipmentSlot.FEET, 0.05f);
    }
    
    public EntityCultist(Level level) {
        this(ModEntities.CULTIST.get(), level);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MAX_HEALTH, 20.0);
    }
    
    @Override
    protected void registerGoals() {
        // Subclasses will register their own goals
    }
    
    @Override
    public boolean canPickUpLoot() {
        return false;
    }
    
    /**
     * Set equipment based on difficulty. Subclasses override this.
     */
    protected void setLoot(DifficultyInstance difficulty) {
        // Override in subclasses
    }
    
    /**
     * Add enchantments based on difficulty. Subclasses override this.
     */
    protected void setEnchantmentBasedOnDifficulty(DifficultyInstance difficulty) {
        // Override in subclasses
    }
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason spawnType, @Nullable SpawnGroupData spawnData) {
        
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        setLoot(difficulty);
        setEnchantmentBasedOnDifficulty(difficulty);
        return spawnData;
    }
    
    // ==================== Home Position ====================
    
    public void setHomePos(BlockPos pos, int distance) {
        this.homePos = pos;
        this.homeDistance = distance;
        setHomeTo(pos, distance);
    }
    
    public BlockPos getHomePos() {
        return homePos;
    }
    
    public int getHomeDistance() {
        return homeDistance;
    }
    
    public boolean hasHome() {
        return homePos != null && homeDistance > 0;
    }
    
    // ==================== Team Logic ====================
    
    @Override
    protected boolean considersEntityAsAlly(Entity entity) {
        // Allied with other cultists
        if (entity instanceof EntityCultist) {
            return true;
        }
        // TODO: Also allied with EntityCultistLeader when implemented
        return super.considersEntityAsAlly(entity);
    }
    
    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        // Don't attack other cultists
        if (target instanceof EntityCultist) {
            return false;
        }
        return super.canAttack(target);
    }
    
    // ==================== Spawn Particles ====================
    
    public void spawnExplosionParticle() {
        if (level().isClientSide()) {
            // TODO: FXDispatcher.INSTANCE.cultistSpawn particles
            for (int i = 0; i < 20; ++i) {
                double dx = random.nextGaussian() * 0.05;
                double dy = random.nextGaussian() * 0.05;
                double dz = random.nextGaussian() * 0.05;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        getX() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth() + dx * 2.0,
                        getY() + random.nextFloat() * getBbHeight() + dy * 2.0,
                        getZ() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth() + dz * 2.0,
                        dx, dy, dz);
            }
        } else {
            level().broadcastEntityEvent(this, (byte) 20);
        }
    }
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 20) {
            spawnExplosionParticle();
        } else {
            super.handleEntityEvent(id);
        }
    }
    
    // ==================== NBT ====================
    
    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (homePos != null && homeDistance > 0) {
            output.putInt("HomeD", homeDistance);
            output.putInt("HomeX", homePos.getX());
            output.putInt("HomeY", homePos.getY());
            output.putInt("HomeZ", homePos.getZ());
        }
    }
    
    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.keySet().contains("HomeD")) {
            setHomePos(new BlockPos(
                    input.getIntOr("HomeX", 0),
                    input.getIntOr("HomeY", 0),
                    input.getIntOr("HomeZ", 0)),
                    input.getIntOr("HomeD", 0));
        }
    }
}
