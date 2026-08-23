package thaumcraft.common.entities.monster.tainted;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;

/**
 * EntityTaintCrawler - A small tainted bug that spreads taint fibers.
 * Implements ITaintedMob behavior.
 * Attacks players and can inflict flux taint effect.
 */
public class EntityTaintCrawler extends Monster {
    
    private BlockPos lastPos = BlockPos.ZERO;
    
    public EntityTaintCrawler(EntityType<? extends EntityTaintCrawler> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }
    
    public EntityTaintCrawler(Level level) {
        super(ModEntities.TAINT_CRAWLER.get(), level);
        this.xpReward = 3;
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.275)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }
    
    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
        return 0.1f;
    }
    
    @Override
    public float getVoicePitch() {
        return 0.7f;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }
    
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, 0.15f, 1.0f);
    }
    
    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }
    
    /**
     * Check if this entity is on the same team as tainted mobs.
     */
    public boolean isTaintedMob(Entity entity) {
        // TODO: Check for ITaintedMob interface when implemented
        return entity instanceof EntityTaintCrawler || 
               entity instanceof EntityTaintSwarm ||
               entity instanceof EntityTaintacle;
    }
    
    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        // Don't attack other tainted mobs
        if (isTaintedMob(target)) {
            return false;
        }
        return super.canAttack(target);
    }
    
    @Override
    public boolean isAlliedTo(Entity other) {
        if (isTaintedMob(other)) {
            return true;
        }
        return super.isAlliedTo(other);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Spread taint fibers while moving (server-side)
        if (!level().isClientSide() && isAlive() && tickCount % 40 == 0 && !lastPos.equals(blockPosition())) {
            lastPos = blockPosition();
            
            BlockPos pos = blockPosition();
            BlockState stateAt = level().getBlockState(pos);
            BlockState stateBelow = level().getBlockState(pos.below());
            
            // Place taint fiber if there's air and solid ground below
            if (stateAt.isAir() && stateBelow.isSolidRender(level(), pos.below())) {
                // Place taint fiber - connections are calculated automatically
                level().setBlockAndUpdate(pos, ModBlocks.TAINT_FIBRE.get().defaultBlockState());
            }
        }
    }
    
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target)) {
            // Apply flux taint effect based on difficulty
            if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                int duration = 0;
                Difficulty difficulty = level().getDifficulty();
                
                if (difficulty == Difficulty.NORMAL) {
                    duration = 3;
                } else if (difficulty == Difficulty.HARD) {
                    duration = 6;
                }
                
                if (duration > 0 && random.nextInt(duration + 1) > 2) {
                    // Apply flux taint effect
                    living.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, duration * 20, 0));
                }
            }
            return true;
        }
        return false;
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        super.dropCustomDeathLoot(level, source, wasRecentlyHit);
        
        // 1/8 chance to drop flux crystal
        if (random.nextInt(8) == 0) {
            this.spawnAtLocation((ServerLevel) this.level(), new net.minecraft.world.item.ItemStack(ModItems.FLUX_CRYSTAL.get()), getBbHeight() / 2.0f);
        }
    }
}
