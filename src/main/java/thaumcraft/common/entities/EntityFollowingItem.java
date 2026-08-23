package thaumcraft.common.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.AdditionalSpawnData;
import thaumcraft.init.ModEntities;

/**
 * EntityFollowingItem - A special item entity that follows a target position or entity.
 * Used for items being drawn toward players, into devices, etc.
 */
public class EntityFollowingItem extends EntitySpecialItem implements IEntityAdditionalSpawnData {
    
    private static final EntityDataAccessor<Integer> DATA_TYPE = 
            SynchedEntityData.defineId(EntityFollowingItem.class, EntityDataSerializers.INT);
    
    private double targetX;
    private double targetY;
    private double targetZ;
    private Entity target;
    private int age = 20;
    public double gravity = 0.04;
    
    public EntityFollowingItem(EntityType<? extends EntityFollowingItem> type, Level level) {
        super(type, level);
    }
    
    public EntityFollowingItem(Level level) {
        this(ModEntities.FOLLOWING_ITEM.get(), level);
    }
    
    public EntityFollowingItem(Level level, double x, double y, double z, ItemStack stack) {
        this(ModEntities.FOLLOWING_ITEM.get(), level);
        this.setPos(x, y, z);
        this.setItem(stack);
        this.setYRot((float)(Math.random() * 360.0));
    }
    
    /**
     * Create a following item that tracks an entity.
     */
    public EntityFollowingItem(Level level, double x, double y, double z, ItemStack stack, Entity target, int type) {
        this(level, x, y, z, stack);
        this.target = target;
        this.targetX = target.getX();
        this.targetY = target.getBoundingBox().minY + target.getBbHeight() / 2.0f;
        this.targetZ = target.getZ();
        setFollowType(type);
        this.noPhysics = true;
    }
    
    /**
     * Create a following item that moves toward fixed coordinates.
     */
    public EntityFollowingItem(Level level, double x, double y, double z, ItemStack stack, double tx, double ty, double tz) {
        this(level, x, y, z, stack);
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, 3);
    }
    
    public int getFollowType() {
        return this.entityData.get(DATA_TYPE);
    }
    
    public void setFollowType(int type) {
        this.entityData.set(DATA_TYPE, type);
    }
    
    @Override
    public void tick() {
        // Update target position if tracking an entity
        if (target != null) {
            targetX = target.getX();
            targetY = target.getBoundingBox().minY + target.getBbHeight() / 2.0f;
            targetZ = target.getZ();
        }
        
        // Move toward target if we have one
        if (targetX != 0.0 || targetY != 0.0 || targetZ != 0.0) {
            float xd = (float)(targetX - getX());
            float yd = (float)(targetY - getY());
            float zd = (float)(targetZ - getZ());
            
            if (age > 1) {
                --age;
            }
            
            double distance = Mth.sqrt(xd * xd + yd * yd + zd * zd);
            if (distance > 0.5) {
                // Move toward target, accelerating over time
                double scaledDist = distance * age;
                setDeltaMovement(xd / scaledDist, yd / scaledDist, zd / scaledDist);
            } else {
                // Arrived at target
                setDeltaMovement(getDeltaMovement().scale(0.1));
                targetX = 0.0;
                targetY = 0.0;
                targetZ = 0.0;
                target = null;
                this.noPhysics = false;
            }
            
            // Spawn particles on client
            if (level().isClientSide()) {
                spawnFollowingParticles();
            }
        } else {
            // No target - apply gravity
            setDeltaMovement(getDeltaMovement().add(0, -gravity, 0));
        }
        
        super.tick();
    }
    
    private void spawnFollowingParticles() {
        // TODO: Spawn nitor or crucible bubble particles based on type
        // int type = getFollowType();
        // float h = (float)((getBoundingBox().maxY - getBoundingBox().minY) / 2.0)
        //         + Mth.sin(tickCount / 10.0f + hoverStart) * 0.1f + 0.1f;
        // if (type != 10) {
        //     FXDispatcher.INSTANCE.drawNitorCore(...);
        // } else {
        //     FXDispatcher.INSTANCE.crucibleBubble(...);
        // }
    }
    
    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putShort("followType", (short) getFollowType());
        output.putDouble("targetX", targetX);
        output.putDouble("targetY", targetY);
        output.putDouble("targetZ", targetZ);
    }
    
    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setFollowType(input.getShortOr("followType", (short)0));
        targetX = input.getDoubleOr("targetX", 0.0D);
        targetY = input.getDoubleOr("targetY", 0.0D);
        targetZ = input.getDoubleOr("targetZ", 0.0D);
    }
    
    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeInt(target != null ? target.getId() : -1);
        buffer.writeDouble(targetX);
        buffer.writeDouble(targetY);
        buffer.writeDouble(targetZ);
        buffer.writeByte(getFollowType());
    }
    
    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        try {
            int entityId = buffer.readInt();
            if (entityId > -1) {
                target = level().getEntity(entityId);
            }
            targetX = buffer.readDouble();
            targetY = buffer.readDouble();
            targetZ = buffer.readDouble();
            setFollowType(buffer.readByte());
        } catch (Exception ignored) {}
    }
}
