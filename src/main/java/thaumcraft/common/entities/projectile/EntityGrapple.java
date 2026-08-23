package thaumcraft.common.entities.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.init.ModEntities;

import java.util.HashMap;
import java.util.Map;

/**
 * EntityGrapple - Grappling hook projectile.
 * When it hits a surface, it pulls the thrower toward that point.
 * Player can release by sneaking.
 */
public class EntityGrapple extends ThrowableProjectile {
    
    private static final EntityDataAccessor<Boolean> DATA_PULLING = 
            SynchedEntityData.defineId(EntityGrapple.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID = 
            SynchedEntityData.defineId(EntityGrapple.class, EntityDataSerializers.INT);
    
    // Track active grapples per player (player entity ID -> grapple entity ID)
    public static final Map<Integer, Integer> grapples = new HashMap<>();
    
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private boolean boost = false;
    private int prevDist = 0;
    private int count = 0;
    private boolean added = false;
    public float ampl = 0.0f;
    
    public EntityGrapple(EntityType<? extends EntityGrapple> type, Level level) {
        super(type, level);
    }
    
    public EntityGrapple(Level level, LivingEntity owner, InteractionHand hand) {
        super(ModEntities.GRAPPLE.get(), owner, level);
        this.hand = hand;
    }
    
    public EntityGrapple(Level level, double x, double y, double z) {
        super(ModEntities.GRAPPLE.get(), x, y, z, level);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PULLING, false);
        builder.define(DATA_OWNER_ID, -1);
    }
    
    public void setPulling(boolean pulling) {
        this.entityData.set(DATA_PULLING, pulling);
    }
    
    public boolean isPulling() {
        return this.entityData.get(DATA_PULLING);
    }
    
    public InteractionHand getHand() {
        return hand;
    }
    
    @Override
    protected float getGravity() {
        return isPulling() ? 0.0f : 0.03f;
    }
    
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }
    
    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        // Override to have zero inaccuracy for precise aiming
        super.shoot(x, y, z, velocity, 0.0f);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        Entity owner = getOwner();
        
        // Auto-discard if no longer valid
        if (!isPulling() && !isRemoved() && (tickCount > 30 || owner == null)) {
            if (owner != null) {
                grapples.remove(owner.getId());
            }
            discard();
            return;
        }
        
        if (owner != null) {
            // Server-side: manage grapple tracking
            if (!level().isClientSide() && !isRemoved() && !added) {
                // Remove any existing grapple for this player
                if (grapples.containsKey(owner.getId())) {
                    int existingId = grapples.get(owner.getId());
                    if (existingId != getId()) {
                        Entity existing = level().getEntity(existingId);
                        if (existing != null) {
                            existing.discard();
                        }
                    }
                }
                grapples.put(owner.getId(), getId());
                added = true;
            }
            
            // Check if another grapple has replaced us
            try {
                if (grapples.containsKey(owner.getId()) && grapples.get(owner.getId()) != getId()) {
                    discard();
                    return;
                }
            } catch (Exception ignored) {}
            
            double distance = owner.distanceTo(this);
            
            // Pulling logic
            if (isPulling() && !isRemoved() && owner instanceof LivingEntity livingOwner) {
                // Release if sneaking
                if (livingOwner.isShiftKeyDown()) {
                    grapples.remove(owner.getId());
                    discard();
                    return;
                }
                
                // Reset flying kick detection for players
                // Note: In 1.20.1, aboveGroundTickCount is private, so we just reset fall distance
                // The grapple naturally prevents kick by keeping player moving
                
                // Reset fall distance
                livingOwner.fallDistance = 0.0f;
                
                // Calculate pull vector
                double mx = getX() - livingOwner.getX();
                double my = getY() - livingOwner.getY();
                double mz = getZ() - livingOwner.getZ();
                
                // Adjust pull strength based on distance
                double dd = distance;
                if (distance < 8.0) {
                    dd = distance * (8.0 - distance);
                }
                dd = Math.max(1.0E-9, dd);
                
                mx /= dd * 5.0;
                my /= dd * 5.0;
                mz /= dd * 5.0;
                
                // Limit max pull speed
                Vec3 pullVec = new Vec3(mx, my, mz);
                if (pullVec.length() > 0.25) {
                    pullVec = pullVec.normalize();
                    mx = pullVec.x / 4.0;
                    my = pullVec.y / 4.0;
                    mz = pullVec.z / 4.0;
                }
                
                // Apply velocity to owner
                Vec3 currentMotion = livingOwner.getDeltaMovement();
                livingOwner.setDeltaMovement(
                    currentMotion.x + mx,
                    currentMotion.y + my + 0.033, // Small upward boost to counteract gravity
                    currentMotion.z + mz
                );
                
                // Initial boost when first latched
                if (!boost) {
                    livingOwner.setDeltaMovement(livingOwner.getDeltaMovement().add(0, 0.4, 0));
                    boost = true;
                }
                
                // Track distance for stuck detection
                int d = (int)(distance / 2.0);
                if (d == prevDist) {
                    count++;
                } else {
                    count = 0;
                }
                prevDist = d;
                
                // Discard if we've reached destination or stuck
                if (distance < 1.5 || count > 60) {
                    grapples.remove(owner.getId());
                    discard();
                }
            }
            
            // Client-side animation amplitude
            if (level().isClientSide()) {
                if (!isPulling()) {
                    ampl += 0.02f;
                } else {
                    ampl *= 0.66f;
                }
            }
        }
    }
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 6) {
            setPulling(true);
            setDeltaMovement(Vec3.ZERO);
        }
        super.handleEntityEvent(id);
    }
    
    @Override
    protected void onHit(HitResult result) {
        if (!level().isClientSide()) {
            setPulling(true);
            setDeltaMovement(Vec3.ZERO);
            
            // Move to exact hit point
            Vec3 hitVec = result.getLocation();
            setPos(hitVec.x, hitVec.y, hitVec.z);
            
            // Notify clients
            level().broadcastEntityEvent(this, (byte)6);
        }
    }
    
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Pulling", isPulling());
        output.putBoolean("Boost", boost);
        output.putByte("Hand", (byte)(hand == InteractionHand.MAIN_HAND ? 0 : 1));
    }
    
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setPulling(input.getBooleanOr("Pulling", false));
        boost = input.getBooleanOr("Boost", false);
        hand = input.getByteOr("Hand", (byte)0) == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
