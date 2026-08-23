package thaumcraft.common.entities.construct;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.init.ModItems;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * EntityTurretCrossbowAdvanced - An advanced crossbow turret with targeting options.
 * 
 * Features:
 * - All base turret features
 * - Configurable targeting: animals, mobs, players, friendlies
 * - Higher health and armor
 * - Faster firing rate
 */
public class EntityTurretCrossbowAdvanced extends EntityTurretCrossbow {
    
    private static final EntityDataAccessor<Byte> DATA_FLAGS = 
            SynchedEntityData.defineId(EntityTurretCrossbowAdvanced.class, EntityDataSerializers.BYTE);
    
    // Flag bits for targeting options
    private static final byte FLAG_TARGET_ANIMAL = 0x01;
    private static final byte FLAG_TARGET_MOB = 0x02;
    private static final byte FLAG_TARGET_PLAYER = 0x04;
    private static final byte FLAG_TARGET_FRIENDLY = 0x08;
    
    private int targetUnseenTicks = 0;
    
    public EntityTurretCrossbowAdvanced(EntityType<? extends EntityTurretCrossbowAdvanced> type, Level level) {
        super(type, level);
    }
    
    public EntityTurretCrossbowAdvanced(EntityType<? extends EntityTurretCrossbowAdvanced> type, Level level, BlockPos pos) {
        super(type, level, pos);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.removeAllGoals(g -> true);
        this.targetSelector.removeAllGoals(g -> true);
        
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0, 20, 40, 24.0f));
        this.goalSelector.addGoal(2, new AdvancedWatchTargetGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new AdvancedTargetGoal(this, LivingEntity.class, 5, true, false));
        
        // Default to targeting mobs
        setTargetMob(true);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS, (byte) 0);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return EntityTurretCrossbow.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 8.0);
    }
    
    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
        return 1.0f;
    }
    
    // ==================== Targeting Options ====================
    
    private byte getFlags() {
        return this.entityData.get(DATA_FLAGS);
    }
    
    private void setFlags(byte flags) {
        this.entityData.set(DATA_FLAGS, flags);
    }
    
    private boolean getFlag(byte flag) {
        return (getFlags() & flag) != 0;
    }
    
    private void setFlag(byte flag, boolean value) {
        byte flags = getFlags();
        if (value) {
            flags |= flag;
        } else {
            flags &= ~flag;
        }
        setFlags(flags);
        setTarget(null); // Clear target when changing targeting options
    }
    
    public boolean getTargetAnimal() {
        return getFlag(FLAG_TARGET_ANIMAL);
    }
    
    public void setTargetAnimal(boolean value) {
        setFlag(FLAG_TARGET_ANIMAL, value);
    }
    
    public boolean getTargetMob() {
        return getFlag(FLAG_TARGET_MOB);
    }
    
    public void setTargetMob(boolean value) {
        setFlag(FLAG_TARGET_MOB, value);
    }
    
    public boolean getTargetPlayer() {
        return getFlag(FLAG_TARGET_PLAYER);
    }
    
    public void setTargetPlayer(boolean value) {
        setFlag(FLAG_TARGET_PLAYER, value);
    }
    
    public boolean getTargetFriendly() {
        return getFlag(FLAG_TARGET_FRIENDLY);
    }
    
    public void setTargetFriendly(boolean value) {
        setFlag(FLAG_TARGET_FRIENDLY, value);
    }
    
    /**
     * Check if the turret can attack a specific entity class based on its targeting options
     */
    public boolean canAttackType(LivingEntity entity) {
        // Animals (non-hostile creatures)
        if (entity instanceof Animal && !(entity instanceof Enemy)) {
            return getTargetAnimal();
        }
        
        // Hostile mobs
        if (entity instanceof Enemy) {
            return getTargetMob();
        }
        
        // Players
        if (entity instanceof Player) {
            if (!getTargetPlayer()) {
                return false;
            }
            // Check if PvP is enabled
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.isPvpAllowed()) {
                setTargetPlayer(false);
                return false;
            }
            return true;
        }
        
        return false;
    }
    
    // ==================== Update ====================
    
    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide()) {
            // Check PvP and clear player targets if needed
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.isPvpAllowed() && getTarget() instanceof Player && getTarget() != getOwner()) {
                setTarget(null);
            }
        }
    }
    
    @Override
    public void move(MoverType type, Vec3 motion) {
        // Even more reduced horizontal movement than base turret
        super.move(type, new Vec3(motion.x / 15.0, motion.y, motion.z / 15.0));
    }
    
    // ==================== Interaction ====================
    
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && isOwner(player) && !isRemoved()) {
            if (player.isShiftKeyDown()) {
                // Pick up turret
                playSound(SoundEvents.ITEM_PICKUP, 1.0f, 1.0f);
                dropAmmo();
                // Drop advanced turret placer
                spawnAtLocation((ServerLevel) this.level(), new ItemStack(ModItems.TURRET_PLACER_ADVANCED.get()), 0.5f);
                discard();
                player.swing(hand);
                return InteractionResult.SUCCESS;
            } else {
                // Open GUI - show current targeting options
                // TODO: Implement proper GUI
                StringBuilder sb = new StringBuilder("Targeting: ");
                if (getTargetMob()) sb.append("[Mobs] ");
                if (getTargetAnimal()) sb.append("[Animals] ");
                if (getTargetPlayer()) sb.append("[Players] ");
                if (getTargetFriendly()) sb.append("[Friendly] ");
                player.displayClientMessage(Component.literal(sb.toString()), true);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
    
    // ==================== Death ====================
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        float bonus = 0 * 0.15f;
        
        // TODO: Drop Thaumcraft items when implemented
        // Advanced turret drops more/better items
        // if (random.nextFloat() < 0.2f + bonus) spawnAtLocation(new ItemStack(ItemsTC.mind, 1, 1));
        // if (random.nextFloat() < 0.5f + bonus) spawnAtLocation((ServerLevel) this.level(), ItemsTC.mechanismSimple);
        // if (random.nextFloat() < 0.5f + bonus) spawnAtLocation((ServerLevel) this.level(), BlocksTC.plankGreatwood);
        // if (random.nextFloat() < 0.5f + bonus) spawnAtLocation((ServerLevel) this.level(), BlocksTC.plankGreatwood);
        // if (random.nextFloat() < 0.3f + bonus) spawnAtLocation(new ItemStack(ItemsTC.plate, 1, 0));
        // if (random.nextFloat() < 0.4f + bonus) spawnAtLocation(new ItemStack(ItemsTC.plate, 1, 1));
        // if (random.nextFloat() < 0.4f + bonus) spawnAtLocation(new ItemStack(ItemsTC.plate, 1, 1));
    }
    
    // ==================== NBT ====================
    
    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("targets", getFlags());
    }
    
    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.contains("targets")) {
            setFlags(input.getByteOr("targets", (byte)0));
        }
    }
    
    // ==================== AI Goals ====================
    
    /**
     * Advanced watch target goal with unseen timeout
     */
    protected class AdvancedWatchTargetGoal extends Goal {
        protected final Mob watcher;
        protected Entity target;
        private int lookTime;
        
        public AdvancedWatchTargetGoal(Mob mob) {
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
     * Advanced target goal with team and friendly-fire handling
     */
    protected class AdvancedTargetGoal extends TargetGoal {
        protected final Class<? extends LivingEntity> targetClass;
        protected final int randomInterval;
        protected LivingEntity target;
        private int targetUnseenTicks;
        
        public AdvancedTargetGoal(Mob mob, Class<? extends LivingEntity> targetClass, 
                int interval, boolean mustSee, boolean mustReach) {
            super(mob, mustSee, mustReach);
            this.targetClass = targetClass;
            this.randomInterval = interval;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }
        
        @Override
        public boolean canUse() {
            if (randomInterval > 0 && mob.getRandom().nextInt(randomInterval) != 0) {
                return false;
            }
            
            double range = getFollowDistance();
            AABB searchBox = mob.getBoundingBox().inflate(range, 4.0, range);
            
            EntityTurretCrossbowAdvanced turret = (EntityTurretCrossbowAdvanced) mob;
            
            List<LivingEntity> targets = mob.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> {
                    if (!targetClass.isInstance(entity)) return false;
                    if (!entity.isAlive()) return false;
                    if (entity == mob) return false;
                    
                    // Check targeting options
                    if (!turret.canAttackType(entity)) return false;
                    
                    // Team checks
                    Team ourTeam = mob.getTeam();
                    Team theirTeam = entity.getTeam();
                    
                    if (ourTeam != null && theirTeam == ourTeam && !turret.getTargetFriendly()) {
                        return false;
                    }
                    if (ourTeam != null && theirTeam != ourTeam && turret.getTargetFriendly()) {
                        return false;
                    }
                    
                    // Owner checks
                    if (turret.isOwned()) {
                        UUID ownerUUID = turret.getOwnerUUID();
                        
                        // Don't attack owner (unless targeting friendly)
                        if (entity == turret.getOwner() && !turret.getTargetFriendly()) {
                            return false;
                        }
                        
                        // Check for other owned entities
                        if (entity instanceof OwnableEntity owned) {
                            UUID theirOwner = owned.getOwnerUUID();
                            if (ownerUUID != null && ownerUUID.equals(theirOwner) && !turret.getTargetFriendly()) {
                                return false;
                            }
                            if (ownerUUID != null && !ownerUUID.equals(theirOwner) && turret.getTargetFriendly()) {
                                return false;
                            }
                        } else if (!(entity instanceof Player) && turret.getTargetFriendly()) {
                            // Non-ownable, non-player entities when targeting friendly only
                            return false;
                        }
                    }
                    
                    // Player damage immunity check
                    if (entity instanceof Player player) {
                        if (player.getAbilities().invulnerable && !turret.getTargetFriendly()) {
                            return false;
                        }
                    }
                    
                    // Sight check
                    if (mustSee && !mob.getSensing().hasLineOfSight(entity)) {
                        return false;
                    }
                    
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
        public boolean canContinueToUse() {
            LivingEntity currentTarget = mob.getTarget();
            if (currentTarget == null || !currentTarget.isAlive()) {
                return false;
            }
            
            EntityTurretCrossbowAdvanced turret = (EntityTurretCrossbowAdvanced) mob;
            
            // Team checks
            Team ourTeam = mob.getTeam();
            Team theirTeam = currentTarget.getTeam();
            
            if (ourTeam != null && theirTeam == ourTeam && !turret.getTargetFriendly()) {
                return false;
            }
            if (ourTeam != null && theirTeam != ourTeam && turret.getTargetFriendly()) {
                return false;
            }
            
            // Range check
            double range = getFollowDistance();
            if (mob.distanceToSqr(currentTarget) > range * range) {
                return false;
            }
            
            // Sight check with timeout
            if (mustSee) {
                if (mob.getSensing().hasLineOfSight(currentTarget)) {
                    targetUnseenTicks = 0;
                } else if (++targetUnseenTicks > 60) {
                    return false;
                }
            }
            
            return true;
        }
        
        @Override
        public void start() {
            mob.setTarget(target);
            targetUnseenTicks = 0;
            super.start();
        }
        
        @Override
        public void stop() {
            target = null;
            super.stop();
        }
    }
}
