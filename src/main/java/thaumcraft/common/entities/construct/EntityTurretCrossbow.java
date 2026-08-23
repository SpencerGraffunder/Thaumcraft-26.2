package thaumcraft.common.entities.construct;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * EntityTurretCrossbow - A stationary crossbow turret construct.
 * 
 * Features:
 * - Fires arrows at hostile mobs
 * - Reloads from dispenser placed below it (facing up)
 * - Can be placed on Thaumcraft activator rails to disable
 * - Slow health regeneration
 * - Owner can pick up by shift-clicking
 */
public class EntityTurretCrossbow extends EntityOwnedConstruct implements RangedAttackMob {
    
    // Animation data
    protected int loadProgressInt = 0;
    protected boolean isLoadInProgress = false;
    protected float loadProgress = 0.0f;
    protected float prevLoadProgress = 0.0f;
    public float loadProgressForRender = 0.0f;
    protected boolean attackedLastTick = false;
    protected int attackCount = 0;
    
    public EntityTurretCrossbow(EntityType<? extends EntityTurretCrossbow> type, Level level) {
        super(type, level);
        if (getAttribute(Attributes.STEP_HEIGHT) != null) {
            getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.0);
        }
    }
    
    public EntityTurretCrossbow(EntityType<? extends EntityTurretCrossbow> type, Level level, BlockPos pos) {
        this(type, level);
        this.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0, 20, 60, 24.0f));
        this.goalSelector.addGoal(2, new WatchTargetGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestValidTargetGoal<>(this, Mob.class, 5, true, false, 
                entity -> entity instanceof Enemy));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return EntityOwnedConstruct.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ARMOR, 2.0);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }
    
    // ==================== Combat ====================
    
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack ammo = getMainHandItem();
        if (!ammo.isEmpty() && ammo.getCount() > 0) {
            Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), null);
            arrow.setBaseDamage(2.25 + distanceFactor * 2.0 + random.nextGaussian() * 0.25);
            
            Vec3 look = getLookAngle();
            if (!isPassenger()) {
                arrow.setPos(
                    arrow.getX() - look.x * 0.9,
                    arrow.getY() - look.y * 0.9,
                    arrow.getZ() - look.z * 0.9
                );
            } else {
                arrow.setPos(
                    arrow.getX() + look.x * 1.75,
                    arrow.getY() + look.y * 1.75,
                    arrow.getZ() + look.z * 1.75
                );
            }
            
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            
            double dx = target.getX() - getX();
            double dy = target.getBoundingBox().minY + target.getEyeHeight() + distanceFactor * distanceFactor * 3.0 - arrow.getY();
            double dz = target.getZ() - getZ();
            
            arrow.shoot(dx, dy, dz, 2.0f, 2.0f);
            level().addFreshEntity(arrow);
            
            level().broadcastEntityEvent(this, (byte) 16);
            playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
            
            ammo.shrink(1);
            if (ammo.isEmpty()) {
                setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }
    }
    
    // ==================== Animation ====================
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            if (!swinging) {
                swingTime = -1;
                swinging = true;
            }
        } else if (id == 17) {
            if (!isLoadInProgress) {
                loadProgressInt = -1;
                isLoadInProgress = true;
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    public float getLoadProgress(float partialTicks) {
        float diff = loadProgress - prevLoadProgress;
        if (diff < 0.0f) {
            diff += 1.0f;
        }
        return prevLoadProgress + diff * partialTicks;
    }
    
    protected void updateSwingProgress() {
        if (swinging) {
            swingTime++;
            if (swingTime >= 6) {
                swingTime = 0;
                swinging = false;
            }
        } else {
            swingTime = 0;
        }
        
        if (isLoadInProgress) {
            loadProgressInt++;
            if (loadProgressInt >= 10) {
                loadProgressInt = 0;
                isLoadInProgress = false;
            }
        } else {
            loadProgressInt = 0;
        }
        loadProgress = loadProgressInt / 10.0f;
    }
    
    // ==================== Update ====================
    
    @Override
    public void aiStep() {
        prevLoadProgress = loadProgress;
        
        // Reload from dispenser below
        if (!level().isClientSide() && (getMainHandItem().isEmpty() || getMainHandItem().getCount() <= 0)) {
            BlockPos below = blockPosition().below();
            BlockEntity blockEntity = level().getBlockEntity(below);
            
            if (blockEntity instanceof DispenserBlockEntity dispenser) {
                BlockState state = level().getBlockState(below);
                if (state.getBlock() instanceof DispenserBlock) {
                    // Check if dispenser is facing up
                    if (state.getValue(DirectionalBlock.FACING) == net.minecraft.core.Direction.UP) {
                        for (int i = 0; i < dispenser.getContainerSize(); i++) {
                            ItemStack stack = dispenser.getItem(i);
                            if (!stack.isEmpty() && stack.getItem() instanceof ArrowItem) {
                                setItemInHand(InteractionHand.MAIN_HAND, dispenser.removeItem(i, stack.getCount()));
                                playSound(ModSounds.TICKS.get(), 1.0f, 1.0f);
                                level().broadcastEntityEvent(this, (byte) 17);
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        super.aiStep();
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Clear target if same team or dead
        if (getTarget() != null && (getTarget().isDeadOrDying() || isAlliedTo(getTarget()))) {
            setTarget(null);
        }
        
        if (!level().isClientSide()) {
            setYRot(yHeadRot);
            
            // Slow health regen
            if (tickCount % 80 == 0) {
                heal(1.0f);
            }
            
            // Check for activator rail (disable AI when powered)
            // TODO: Check for Thaumcraft activator rail when implemented
            // For now, check vanilla powered rail
            BlockPos pos = blockPosition();
            BlockState state = level().getBlockState(pos);
            if (state.is(Blocks.ACTIVATOR_RAIL)) {
                boolean powered = state.getValue(net.minecraft.world.level.block.PoweredRailBlock.POWERED);
                setNoAi(powered);
            }
        } else {
            updateSwingProgress();
        }
    }
    
    // ==================== Properties ====================
    
    @Override
    public boolean isPushable() {
        return true;
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    @Override
    public int getMaxHeadXRot() {
        return 20;
    }
    
    // ==================== Damage ====================
    
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Random rotation when hit for visual effect
        setYRot(getYRot() + (float)(getRandom().nextGaussian() * 45.0));
        setXRot(getXRot() + (float)(getRandom().nextGaussian() * 20.0));
        return super.hurtServer(level, source, amount);
    }
    
    @Override
    public void knockback(double power, double xd, double zd, DamageSource source, float damage, boolean comesFromEffect) {
        super.knockback(power, xd / 10.0, zd / 10.0, source, damage, comesFromEffect);
        Vec3 delta = getDeltaMovement();
        if (delta.y > 0.1) {
            setDeltaMovement(delta.x, 0.1, delta.z);
        }
    }
    
    @Override
    public void move(MoverType type, Vec3 motion) {
        // Heavily reduced horizontal movement
        super.move(type, new Vec3(motion.x / 20.0, motion.y, motion.z / 20.0));
    }
    
    // ==================== Interaction ====================
    
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && isOwner(player) && !isRemoved()) {
            if (player.isShiftKeyDown()) {
                // Pick up turret
                playSound(ModSounds.ZAP.get(), 1.0f, 1.0f);
                dropAmmo();
                // Drop turret placer item
                spawnAtLocation((ServerLevel) this.level(), new ItemStack(ModItems.TURRET_PLACER_BASIC.get()), 0.5f);
                discard();
                player.swing(hand);
                return InteractionResult.SUCCESS;
            } else {
                // Open GUI
                // TODO: player.openMenu(...)
                player.sendSystemMessage(Component.literal("Turret GUI not yet implemented"));
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }
    
    // ==================== Death ====================
    
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide()) {
            dropAmmo();
        }
    }
    
    protected void dropAmmo() {
        ItemStack held = getMainHandItem();
        if (!held.isEmpty()) {
            spawnAtLocation((ServerLevel) this.level(), held, 0.5f);
            setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        float bonus = 0 * 0.15f;
        
        // TODO: Drop Thaumcraft items when implemented
        // if (random.nextFloat() < 0.2f + bonus) spawnAtLocation((ServerLevel) this.level(), ItemsTC.mind);
        // if (random.nextFloat() < 0.5f + bonus) spawnAtLocation((ServerLevel) this.level(), ItemsTC.mechanismSimple);
        // if (random.nextFloat() < 0.5f + bonus) spawnAtLocation((ServerLevel) this.level(), BlocksTC.plankGreatwood);
        // if (random.nextFloat() < 0.5f + bonus) spawnAtLocation((ServerLevel) this.level(), BlocksTC.plankGreatwood);
    }
    
    // ==================== NBT ====================
    
    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }
    
    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }
    
    // ==================== AI Goals ====================
    
    /**
     * Goal to watch the current attack target
     */
    protected class WatchTargetGoal extends Goal {
        protected final Mob watcher;
        protected Entity target;
        private int lookTime;
        
        public WatchTargetGoal(Mob mob) {
            this.watcher = mob;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            if (watcher.getTarget() != null) {
                target = watcher.getTarget();
            }
            return target != null;
        }
        
        @Override
        public boolean canContinueToUse() {
            if (target == null || !target.isAlive()) {
                return false;
            }
            double range = getFollowDistance();
            return watcher.distanceToSqr(target) <= range * range && lookTime > 0;
        }
        
        @Override
        public void start() {
            lookTime = 40 + watcher.getRandom().nextInt(40);
        }
        
        @Override
        public void stop() {
            target = null;
        }
        
        @Override
        public void tick() {
            watcher.getLookControl().setLookAt(
                target.getX(),
                target.getY() + target.getEyeHeight(),
                target.getZ(),
                10.0f,
                watcher.getMaxHeadXRot()
            );
            lookTime--;
        }
        
        protected double getFollowDistance() {
            AttributeInstance attr = watcher.getAttribute(Attributes.FOLLOW_RANGE);
            return attr == null ? 16.0 : attr.getValue();
        }
    }
    
    /**
     * Target goal that properly handles team and owner checks
     */
    protected static class NearestValidTargetGoal<T extends LivingEntity> extends TargetGoal {
        protected final Class<T> targetClass;
        protected final int randomInterval;
        protected final Predicate<LivingEntity> targetPredicate;
        protected LivingEntity target;
        
        public NearestValidTargetGoal(Mob mob, Class<T> targetClass, int interval, 
                boolean mustSee, boolean mustReach, Predicate<LivingEntity> predicate) {
            super(mob, mustSee, mustReach);
            this.targetClass = targetClass;
            this.randomInterval = interval;
            this.targetPredicate = predicate;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }
        
        @Override
        public boolean canUse() {
            if (randomInterval > 0 && mob.getRandom().nextInt(randomInterval) != 0) {
                return false;
            }
            
            double range = getFollowDistance();
            AABB searchBox = mob.getBoundingBox().inflate(range, 4.0, range);
            
            List<LivingEntity> targets = mob.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> {
                    if (!targetClass.isInstance(entity)) return false;
                    if (targetPredicate != null && !targetPredicate.test(entity)) return false;
                    if (!entity.isAlive()) return false;
                    if (entity == mob) return false;
                    
                    // Check team/owner for constructs
                    if (mob instanceof EntityOwnedConstruct construct) {
                        if (construct.isAlliedTo(entity)) return false;
                    }
                    
                    // Check sight
                    if (mustSee && !mob.getSensing().hasLineOfSight(entity)) return false;
                    
                    return true;
                }
            );
            
            if (targets.isEmpty()) {
                return false;
            }
            
            // Sort by distance
            targets.sort(Comparator.comparingDouble(e -> mob.distanceToSqr(e)));
            target = targets.get(0);
            return true;
        }
        
        @Override
        public void start() {
            mob.setTarget(target);
            super.start();
        }
        
        @Override
        public void stop() {
            target = null;
            super.stop();
        }
    }
}
