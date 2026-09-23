package thaumcraft.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeedPrime;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * EntityFluxRift - A tear in reality caused by excess flux in the aura.
 * Can grow by absorbing more flux, destroys nearby blocks and entities,
 * and can trigger various dangerous events when unstable.
 */
public class EntityFluxRift extends Entity {
    
    private static final EntityDataAccessor<Integer> DATA_SEED = 
            SynchedEntityData.defineId(EntityFluxRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SIZE = 
            SynchedEntityData.defineId(EntityFluxRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_STABILITY = 
            SynchedEntityData.defineId(EntityFluxRift.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_COLLAPSE = 
            SynchedEntityData.defineId(EntityFluxRift.class, EntityDataSerializers.BOOLEAN);
    
    private int maxSize = 0;
    private int lastSize = -1;
    
    // Rift shape points for rendering and collision
    public ArrayList<Vec3> points = new ArrayList<>();
    public ArrayList<Float> pointsWidth = new ArrayList<>();
    
    /**
     * Get the list of points defining the rift shape.
     */
    public List<Vec3> getPoints() {
        return points;
    }
    
    /**
     * Get the list of widths at each point.
     */
    public List<Float> getPointWidths() {
        return pointsWidth;
    }
    
    public EntityFluxRift(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }
    
    public EntityFluxRift(Level level) {
        this(ModEntities.FLUX_RIFT.get(), level);
    }
    
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SEED, 0);
        builder.define(DATA_SIZE, 5);
        builder.define(DATA_STABILITY, 0.0f);
        builder.define(DATA_COLLAPSE, false);
    }
    
    // ==================== Data Accessors ====================
    
    public int getRiftSeed() {
        return this.entityData.get(DATA_SEED);
    }
    
    public void setRiftSeed(int seed) {
        this.entityData.set(DATA_SEED, seed);
    }
    
    public int getRiftSize() {
        return this.entityData.get(DATA_SIZE);
    }
    
    public void setRiftSize(int size) {
        this.entityData.set(DATA_SIZE, Math.max(1, size));
        recalculateShape();
    }
    
    public float getRiftStability() {
        return this.entityData.get(DATA_STABILITY);
    }
    
    public void setRiftStability(float stability) {
        this.entityData.set(DATA_STABILITY, Mth.clamp(stability, -100.0f, 100.0f));
    }
    
    public boolean isCollapsing() {
        return this.entityData.get(DATA_COLLAPSE);
    }
    
    public void setCollapsing(boolean collapse) {
        if (collapse) {
            this.maxSize = getRiftSize();
        }
        this.entityData.set(DATA_COLLAPSE, collapse);
    }
    
    public void addStability(float amount) {
        setRiftStability(getRiftStability() + amount);
    }
    
    public EnumStability getStability() {
        float stability = getRiftStability();
        if (stability > 50.0f) return EnumStability.VERY_STABLE;
        if (stability >= 0.0f) return EnumStability.STABLE;
        if (stability > -25.0f) return EnumStability.UNSTABLE;
        return EnumStability.VERY_UNSTABLE;
    }
    
    // ==================== Shape Calculation ====================
    
    private void recalculateShape() {
        calcSteps(points, pointsWidth, new Random(getRiftSeed()));
        lastSize = getRiftSize();
        
        // Calculate bounding box from points
        if (points.isEmpty()) {
            setBoundingBox(new AABB(getX() - 1, getY() - 1, getZ() - 1, 
                    getX() + 1, getY() + 1, getZ() + 1));
            return;
        }
        
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        
        for (Vec3 v : points) {
            minX = Math.min(minX, v.x);
            maxX = Math.max(maxX, v.x);
            minY = Math.min(minY, v.y);
            maxY = Math.max(maxY, v.y);
            minZ = Math.min(minZ, v.z);
            maxZ = Math.max(maxZ, v.z);
        }
        
        setBoundingBox(new AABB(
                getX() + minX, getY() + minY, getZ() + minZ,
                getX() + maxX, getY() + maxY, getZ() + maxZ));
    }
    
    private void calcSteps(ArrayList<Vec3> pp, ArrayList<Float> ww, Random rr) {
        pp.clear();
        ww.clear();
        
        Vec3 right = new Vec3(rr.nextGaussian(), rr.nextGaussian(), rr.nextGaussian()).normalize();
        Vec3 left = right.scale(-1.0);
        Vec3 lr = Vec3.ZERO;
        Vec3 ll = Vec3.ZERO;
        
        int steps = Mth.ceil(getRiftSize() / 3.0f);
        float girth = getRiftSize() / 300.0f;
        double angle = 0.33;
        float dec = girth / steps;
        
        for (int a = 0; a < steps; ++a) {
            girth -= dec;
            
            // Rotate right branch
            right = rotateVec(right, (float)(rr.nextGaussian() * angle), (float)(rr.nextGaussian() * angle));
            lr = lr.add(right.scale(0.2));
            pp.add(new Vec3(lr.x, lr.y, lr.z));
            ww.add(girth);
            
            // Rotate left branch
            left = rotateVec(left, (float)(rr.nextGaussian() * angle), (float)(rr.nextGaussian() * angle));
            ll = ll.add(left.scale(0.2));
            pp.add(0, new Vec3(ll.x, ll.y, ll.z));
            ww.add(0, girth);
        }
        
        // Add endpoints
        lr = lr.add(right.scale(0.1));
        pp.add(new Vec3(lr.x, lr.y, lr.z));
        ww.add(0.0f);
        
        ll = ll.add(left.scale(0.1));
        pp.add(0, new Vec3(ll.x, ll.y, ll.z));
        ww.add(0, 0.0f);
    }
    
    private Vec3 rotateVec(Vec3 vec, float pitch, float yaw) {
        // Simple rotation approximation
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        
        double x = vec.x * cosYaw - vec.z * sinYaw;
        double z = vec.x * sinYaw + vec.z * cosYaw;
        double y = vec.y * cosPitch - z * sinPitch;
        z = vec.y * sinPitch + z * cosPitch;
        
        return new Vec3(x, y, z);
    }
    
    // ==================== Update Logic ====================
    
    @Override
    public void tick() {
        super.tick();
        
        if (lastSize != getRiftSize()) {
            recalculateShape();
        }
        
        if (!level().isClientSide()) {
            serverTick();
        } else {
            clientTick();
        }
    }
    
    private void serverTick() {
        // Initialize seed if needed
        if (getRiftSeed() == 0) {
            setRiftSeed(random.nextInt());
        }
        
        // Damage blocks and entities along rift lines
        if (!points.isEmpty() && points.size() > 1) {
            int pi = random.nextInt(points.size() - 1);
            Vec3 v1 = points.get(pi).add(getX(), getY(), getZ());
            Vec3 v2 = points.get(pi + 1).add(getX(), getY(), getZ());
            
            // Destroy blocks
            BlockHitResult hit = level().clip(new ClipContext(v1, v2, 
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hit.getBlockPos();
                BlockState state = level().getBlockState(pos);
                if (!state.isAir() && state.getDestroySpeed(level(), pos) >= 0.0f) {
                    level().destroyBlock(pos, false);
                }
            }
            
            // Damage nearby entities
            List<Entity> entities = level().getEntities(this, 
                    new AABB(v1.x - 0.5, v1.y - 0.5, v1.z - 0.5, 
                            v1.x + 0.5, v1.y + 0.5, v1.z + 0.5));
            for (Entity e : entities) {
                if (e.isAlive()) {
                    if (e instanceof Player player && player.isCreative()) {
                        continue;
                    }
                    e.hurtServer((ServerLevel) level(), damageSources().fellOutOfWorld(), 2.0f);
                    if (e instanceof ItemEntity) {
                        e.discard();
                    }
                }
            }
        }
        
        // Force collapse if too small
        if (points.size() < 3 && !isCollapsing()) {
            setCollapsing(true);
        }
        
        // Handle collapse
        if (isCollapsing()) {
            setRiftSize(getRiftSize() - 1);
            
            // Release vis/flux during collapse
            if (random.nextBoolean()) {
                AuraHelper.addVis(level(), blockPosition(), 1.0f);
            } else {
                AuraHelper.polluteAura(level(), blockPosition(), 1.0f, false);
            }
            
            // Random explosions
            if (random.nextInt(10) == 0) {
                level().explode(this, 
                        getX() + random.nextGaussian() * 2.0,
                        getY() + random.nextGaussian() * 2.0,
                        getZ() + random.nextGaussian() * 2.0,
                        random.nextFloat() / 2.0f, Level.ExplosionInteraction.NONE);
            }
            
            if (getRiftSize() <= 1) {
                completeCollapse();
                return;
            }
        }
        
        // Periodic stability decay
        if (tickCount % 120 == 0) {
            setRiftStability(getRiftStability() - 0.2f);
        }
        
        // Periodic growth/events check
        if (tickCount % 600 == getId() % 600) {
            // Try to grow from flux
            float flux = AuraHelper.getFlux(level(), blockPosition());
            double sizeThreshold = Math.sqrt(getRiftSize() * 2);
            if (flux >= sizeThreshold && getRiftSize() < 100 && getStability() != EnumStability.VERY_STABLE) {
                AuraHelper.drainFlux(level(), blockPosition(), (float)sizeThreshold, false);
                setRiftSize(getRiftSize() + 1);
            }
            
            // Trigger random events when charged (1.12: random.nextInt(2000) < min(20, stability))
            if (random.nextInt(2000) < Math.min(20, Math.abs(getRiftStability()))) {
                executeRiftEvent();
            }
        }
        
        // Ambient sound
        if (tickCount % 300 == 0) {
            playSound(ModSounds.EVIL_PORTAL.get(), 0.5f, 0.8f + random.nextFloat() * 0.4f);
        }
    }
    
    private void clientTick() {
        // Spawn particles when unstable
        if (!points.isEmpty() && points.size() > 2) {
            if (!isCollapsing() && getRiftStability() < 0.0f && random.nextInt(150) < Math.abs(getRiftStability())) {
                int pi = 1 + random.nextInt(points.size() - 2);
                Vec3 v = points.get(pi).add(getX(), getY(), getZ());
                level().addParticle(ParticleTypes.PORTAL, v.x, v.y, v.z, 
                        random.nextGaussian() * 0.1, random.nextGaussian() * 0.1, random.nextGaussian() * 0.1);
            }
            
            // More particles when collapsing
            if (isCollapsing()) {
                int pi = 1 + random.nextInt(points.size() - 2);
                Vec3 v = points.get(pi).add(getX(), getY(), getZ());
                level().addParticle(ParticleTypes.SMOKE, v.x, v.y, v.z, 
                        random.nextGaussian() * 0.1, random.nextGaussian() * 0.1, random.nextGaussian() * 0.1);
            }
        }
    }
    
    // ==================== Rift Events (1.12 weighted table) ====================

    /** 1.12 flux event entry: event id, weight, stability cost, whether it's allowed near taint seeds. */
    private record FluxEventEntry(int event, int weight, int cost, boolean nearTaintAllowed) {}

    // 1.12 event table: (wisp, taint seed, vis exhaustion, flux focus, collapse)
    private static final List<FluxEventEntry> EVENTS = List.of(
            new FluxEventEntry(0, 50, 5, true),
            new FluxEventEntry(1, 10, 0, false),
            new FluxEventEntry(2, 20, 10, true),
            new FluxEventEntry(3, 20, 10, true),
            new FluxEventEntry(4, 1, 0, true));

    private FluxEventEntry chooseOnWeight() {
        int total = 0;
        for (FluxEventEntry e : EVENTS) total += e.weight;
        int roll = random.nextInt(total);
        for (FluxEventEntry e : EVENTS) {
            roll -= e.weight;
            if (roll < 0) return e;
        }
        return EVENTS.get(0);
    }

    private void executeRiftEvent() {
        FluxEventEntry ei = chooseOnWeight();
        if (ei == null) return;
        // Taint-seed adjacency check (1.12: skip non-taint-allowed events near taint seeds)
        if (!ei.nearTaintAllowed && TaintHelper.isNearTaintSeed(level(), blockPosition())) {
            return;
        }
        boolean didIt = false;
        switch (ei.event) {
            case 0: {
                // Wisp (FLUX type 20% chance)
                EntityWisp wisp = new EntityWisp(level());
                wisp.setPos(getX() + random.nextGaussian() * 5.0, getY() + random.nextGaussian() * 5.0, getZ() + random.nextGaussian() * 5.0);
                if (random.nextInt(5) == 0) {
                    wisp.setWispType("flux");
                }
                if (level().addFreshEntity(wisp)) {
                    didIt = true;
                }
                break;
            }
            case 1: {
                // Taint seed prime (pollute aura + setDead)
                EntityTaintSeedPrime seed = new EntityTaintSeedPrime(level());
                seed.setPos(getX() + random.nextGaussian() * 5.0, getY() + random.nextGaussian() * 5.0, getZ() + random.nextGaussian() * 5.0);
                if (level().addFreshEntity(seed)) {
                    didIt = true;
                    AuraHelper.polluteAura(level(), blockPosition(), getRiftSize() / 2f, true);
                    seed.discard();
                }
                break;
            }
            case 2: {
                // Infectious vis exhaustion on nearby entities
                for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16.0))) {
                    didIt = true;
                    target.addEffect(new MobEffectInstance(ModEffects.INFECTIOUS_VIS_EXHAUST, 600, 0));
                }
                break;
            }
            case 3: {
                // Flux focus: warp the closest player (simplified 1.12 focus cast)
                Player closest = null;
                double minDist = Double.MAX_VALUE;
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(32.0))) {
                    double d = p.distanceToSqr(this);
                    if (d < minDist) { minDist = d; closest = p; }
                }
                if (closest != null) {
                    didIt = true;
                    ThaumcraftApi.internalMethods.addWarpToPlayer(closest, 5 + getRiftSize() / 2, IPlayerWarp.EnumWarpType.NORMAL);
                }
                break;
            }
            case 4: {
                // Begin collapse
                didIt = true;
                setCollapsing(true);
                break;
            }
        }
        if (didIt) {
            addStability(-ei.cost);
        }
    }
    
    private void completeCollapse() {
        int dropAmount = (int) Math.sqrt(maxSize);
        
        // Chance to drop primordial pearl
        if (random.nextInt(100) < dropAmount) {
            spawnAtLocation((ServerLevel) this.level(), new ItemStack(ModItems.PRIMORDIAL_PEARL.get()));
        }
        
        // Drop void seeds
        for (int i = 0; i < dropAmount; i++) {
            spawnAtLocation((ServerLevel) this.level(), new ItemStack(ModItems.VOID_SEED.get()));
        }
        
        // Apply effects based on stability
        List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class, 
                getBoundingBox().inflate(32.0));
        
        switch (getStability()) {
            case VERY_UNSTABLE:
                // Apply flux taint effect
                for (LivingEntity e : nearby) {
                    double dist = e.distanceToSqr(this);
                    int duration = (int)((1.0 - dist / 1024.0) * 120.0);
                    if (duration > 0) {
                        e.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, duration * 20, 0));
                    }
                }
                // Fall through
            case UNSTABLE:
                for (LivingEntity e : nearby) {
                    double dist = e.distanceToSqr(this);
                    int duration = (int)((1.0 - dist / 1024.0) * 300.0);
                    if (duration > 0) {
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration * 20, 0));
                    }
                }
                // Fall through
            case STABLE:
                // Add warp to players
                for (LivingEntity e : nearby) {
                    if (e instanceof Player player) {
                        double dist = e.distanceToSqr(this);
                        int warp = (int)((1.0 - dist / 1024.0) * 25.0);
                        if (warp > 0) {
                            ThaumcraftApi.internalMethods.addWarpToPlayer(player, warp, IPlayerWarp.EnumWarpType.NORMAL);
                        }
                    }
                }
                break;
            case VERY_STABLE:
                // No negative effects
                break;
        }
        
        // Explosion effect
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, 
                    getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        }
        
        discard();
    }
    
    // ==================== Static Creation ====================
    
    /**
     * Attempt to create a new flux rift at or near the given position.
     */
    public static void createRift(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        
        // Randomize position slightly
        pos = pos.offset(level.getRandom().nextInt(16), 0, level.getRandom().nextInt(16));
        BlockPos spawnPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos);
        
        if (spawnPos.getY() >= level.getMaxY() - 4) return;
        
        // Check for existing rifts nearby
        List<EntityFluxRift> nearbyRifts = level.getEntitiesOfClass(EntityFluxRift.class, 
                new AABB(spawnPos).inflate(32.0));
        if (!nearbyRifts.isEmpty()) return;
        
        // Calculate initial size from flux
        float flux = AuraHelper.getFlux(level, spawnPos);
        int size = (int) Math.sqrt(flux * 3.0f);
        
        if (size > 5) {
            EntityFluxRift rift = new EntityFluxRift(level);
            rift.setRiftSeed(level.getRandom().nextInt());
            rift.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5);
        rift.setYRot(level.getRandom().nextFloat() * 360.0f);
        rift.setXRot(0.0f);
            rift.setRiftSize(size);
            
            if (level.addFreshEntity(rift)) {
                AuraHelper.drainFlux(level, spawnPos, size, false);
            }
        }
    }
    
    // ==================== Movement Override ====================
    
    @Override
    public void move(MoverType type, Vec3 movement) {
        // Rifts don't move
    }
    
    @Override
    public void push(double x, double y, double z) {
        // Rifts can't be pushed
    }
    
    // ==================== Fire Immunity ====================
    
    @Override
    public void setRemainingFireTicks(int remainingTicks) {
        // Rifts can't burn
    }
    
    @Override
    public boolean isOnFire() {
        return false;
    }
    
    @Override
    public boolean displayFireAnimation() {
        return false;
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        maxSize = input.getIntOr("MaxSize", 0);
        setRiftSize(input.getIntOr("RiftSize", 0));
        setRiftSeed(input.getIntOr("RiftSeed", 0));
        setRiftStability(input.getFloatOr("Stability", 0.0F));
        setCollapsing(input.getBooleanOr("Collapse", false));
    }
    
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("MaxSize", maxSize);
        output.putInt("RiftSize", getRiftSize());
        output.putInt("RiftSeed", getRiftSeed());
        output.putFloat("Stability", getRiftStability());
        output.putBoolean("Collapse", isCollapsing());
    }
    
    // ==================== Enums ====================
    
    public enum EnumStability {
        VERY_STABLE,
        STABLE,
        UNSTABLE,
        VERY_UNSTABLE
    }
}
