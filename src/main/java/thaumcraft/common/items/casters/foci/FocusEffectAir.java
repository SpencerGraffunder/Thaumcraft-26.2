package thaumcraft.common.items.casters.foci;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.client.multiplayer.ClientLevel;
import thaumcraft.client.fx.FXDispatcher;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;

/**
 * Air Focus Effect - Deals damage and applies knockback to targets.
 */
public class FocusEffectAir extends FocusEffect {

    @Override
    public String getResearch() {
        return "FOCUSELEMENTAL";
    }

    @Override
    public String getKey() {
        return "thaumcraft.AIR";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.AIR;
    }

    @Override
    public int getComplexity() {
        return getSettingValue("power") * 2;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        return (1 + getSettingValue("power")) * finalPower;
    }

    @Override
    public boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num) {
        if (getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        Level world = getPackage().world;
        Vec3 hitPos = target.getLocation();
        
        // Impact particles are rendered client-side: 1.12 sent PacketFXFocusPartImpact here,
        // which re-ran renderParticleFX 15 times at the impact point on each client.
        if (!world.isClientSide()) {
            Vec3 impact = target.getLocation();
            PacketHandler.sendToAllAround(
                new PacketFXFocusPartImpact(impact.x, impact.y, impact.z, new String[] { getKey() }),
                (ServerLevel) world, BlockPos.containing(impact), 64.0);
        }
        
        // Play wind sound at impact
        world.playSound(null, hitPos.x, hitPos.y, hitPos.z, 
            SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 0.66f);
        
        if (target.getType() == HitResult.Type.ENTITY && target instanceof EntityHitResult entityHit) {
            Entity hitEntity = entityHit.getEntity();
            
            if (hitEntity == null) {
                return false;
            }
            
            float damage = getDamageForDisplay(finalPower);
            
            // Create damage source
            Entity caster = getCaster();
            DamageSource damageSource;
            if (caster != null) {
                damageSource = world.damageSources().thrown(hitEntity, caster);
            } else {
                damageSource = world.damageSources().magic();
            }
            
            hitEntity.hurt(damageSource, damage);
            
            // Apply knockback to living entities
            if (hitEntity instanceof LivingEntity living) {
                float knockbackStrength = damage * 0.25f;
                
                if (trajectory != null) {
                    // Knockback in the direction the spell was traveling
                    living.knockback(knockbackStrength, 
                        -trajectory.direction.x, 
                        -trajectory.direction.z,
                        damageSource, damage);
                } else {
                    // Fallback: knockback based on entity rotation
                    float yawRad = hitEntity.getYRot() * ((float) Math.PI / 180F);
                    living.knockback(knockbackStrength, 
                        -Mth.sin(yawRad), 
                        Mth.cos(yawRad),
                        damageSource, damage);
                }
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
            new NodeSetting("power", "focus.common.power", 
                new NodeSetting.NodeSettingIntRange(1, 5))
        };
    }

    @Override
    public void renderParticleFX(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        if (!(level instanceof ClientLevel)) {
            return;
        }
        net.minecraft.util.RandomSource random = level.getRandom();
        // 1.12-faithful: FXDispatcher.GenPart wind mote (grid 32, sprites 337..341)
        FXDispatcher.GenPart pp = new FXDispatcher.GenPart();
        pp.grav = -0.1F;
        pp.age = 20 + random.nextInt(10);
        pp.alpha = new float[] { 0.5F, 0.0F };
        pp.grid = 32;
        pp.partStart = 337;
        pp.partInc = 1;
        pp.partNum = 5;
        pp.slowDown = 0.75;
        pp.rot = (float) random.nextGaussian() / 2.0F;
        float s = (float) (2.0 + random.nextGaussian() * 0.5);
        pp.scale = new float[] { s, s * 2.0F };
        FXDispatcher.INSTANCE.drawGenericParticles(posX, posY, posZ, motionX, motionY, motionZ, pp);
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            caster.level().playSound(null, caster.blockPosition().above(), 
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 
                0.125f, 2.0f);
        }
    }
    
    /**
     * Gets the caster entity from the focus package.
     */
    private Entity getCaster() {
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
}
