package thaumcraft.common.items.casters.foci;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.Trajectory;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Bolt Medium - Extended range instant hit delivery.
 * Similar to Touch but with longer range and a lightning/zap visual effect.
 * Extends FocusMediumTouch for shared raytrace logic.
 */
public class FocusMediumBolt extends FocusMediumTouch {
    
    /** Default range for bolt */
    private static final float BOLT_RANGE = 16.0f;

    @Override
    public String getResearch() {
        return "FOCUSBOLT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.BOLT";
    }

    @Override
    public int getComplexity() {
        return 5;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ENERGY;
    }

    @Override
    public boolean execute(Trajectory trajectory) {
        if (getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        Vec3 start = trajectory.source;
        Vec3 direction = trajectory.direction.normalize();
        Vec3 end = start.add(direction.scale(BOLT_RANGE));
        
        // Try to hit an entity first
        HitResult hit = rayTraceEntitiesFromTrajectory(start, end);
        
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            // Fall back to block raycast
            hit = getPackage().world.clip(new ClipContext(
                    start, end,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    (Entity) null));
            
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                end = hit.getLocation();
            }
        } else if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() != null) {
            // Adjust end point to entity position
            end = start.add(direction.scale(start.distanceTo(entityHit.getEntity().position())));
        }
        
        // Send visual zap effect (particles along the bolt path)
        if (!getPackage().world.isClientSide()) {
            int steps = 10;
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double x = start.x + (end.x - start.x) * t;
                double y = start.y + (end.y - start.y) * t;
                double z = start.z + (end.z - start.z) * t;
                for (int i2 = 0; i2 < 2; i2++) {
                    getPackage().world.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.1, 0.1, 0.1);
                }
                getPackage().world.addParticle(ParticleTypes.ENCHANT, x, y, z, 0.05, 0.05, 0.05);
            }
        }
        
        return true;
    }
    
    /**
     * Extended range version of supplyTargets.
     */
    @Override
    public HitResult[] supplyTargets() {
        if (getPackage() == null || getPackage().world == null) {
            return new HitResult[0];
        }

        Entity caster = getCasterEntity();
        if (caster == null) {
            return new HitResult[0];
        }

        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(BOLT_RANGE));

        // Try to hit an entity first
        HitResult hit = rayTraceEntitiesFromTrajectory(eyePos, endPos);
        
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            // Fall back to block raycast
            hit = getPackage().world.clip(new ClipContext(
                    eyePos, endPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    caster));
        }

        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return new HitResult[] { hit };
        }
        
        return new HitResult[0];
    }
    
    /**
     * Extended range version of supplyTrajectories.
     */
    @Override
    public Trajectory[] supplyTrajectories() {
        if (getPackage() == null || getPackage().world == null) {
            return new Trajectory[0];
        }

        Entity caster = getCasterEntity();
        if (caster == null) {
            return new Trajectory[0];
        }

        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(BOLT_RANGE));

        // Try to hit an entity first
        HitResult hit = rayTraceEntitiesFromTrajectory(eyePos, endPos);
        
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            // Fall back to block raycast
            hit = getPackage().world.clip(new ClipContext(
                    eyePos, endPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    caster));
        }

        Vec3 hitPos = (hit != null) ? hit.getLocation() : endPos;
        
        return new Trajectory[] { new Trajectory(hitPos, lookVec.normalize()) };
    }
    
    /**
     * Raytrace entities along a trajectory path.
     */
    private HitResult rayTraceEntitiesFromTrajectory(Vec3 start, Vec3 end) {
        if (getPackage() == null || getPackage().world == null) {
            return null;
        }
        
        AABB searchBox = new AABB(start, end).inflate(1.0);
        
        Entity casterEntity = getCasterEntity();
        Predicate<Entity> filter = e -> !e.isSpectator() && e.isPickable() && e != casterEntity;
        
        double closestDist = BOLT_RANGE;
        Entity closestEntity = null;
        Vec3 closestHit = null;

        for (Entity entity : getPackage().world.getEntities(casterEntity, searchBox, filter)) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hitOpt = entityBox.clip(start, end);
            
            if (hitOpt.isPresent()) {
                double dist = start.distanceTo(hitOpt.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestEntity = entity;
                    closestHit = hitOpt.get();
                }
            }
        }

        if (closestEntity != null) {
            return new EntityHitResult(closestEntity, closestHit);
        }
        
        return null;
    }
    
    /**
     * Gets the caster entity from the focus package.
     */
    private Entity getCasterEntity() {
        if (getPackage() == null || getPackage().getCasterUUID() == null) {
            return null;
        }
        if (getPackage().world != null) {
            for (Player player : getPackage().world.players()) {
                if (player.getUUID().equals(getPackage().getCasterUUID())) {
                    return player;
                }
            }
        }
        return null;
    }
    
    /**
     * Calculate a combined color from all focus effects for the zap visual.
     * Uses FocusEngine for casting
     */
    private int calculateEffectColor() {
        // Default to blue energy color
        return 0x5599FF;
    }
}
