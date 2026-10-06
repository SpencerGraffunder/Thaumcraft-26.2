package thaumcraft.common.items.casters.foci;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.client.multiplayer.ClientLevel;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.fx.particles.FXGeneric;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;

import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;

/**
 * Heal Focus Effect - Heals living targets, damages undead.
 */
public class FocusEffectHeal extends FocusEffect {

    @Override
    public String getResearch() {
        return "FOCUSHEAL";
    }

    @Override
    public String getKey() {
        return "thaumcraft.HEAL";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.LIFE;
    }

    @Override
    public int getComplexity() {
        return getSettingValue("power") * 4;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        // Negative indicates healing
        return -getSettingValue("power") * finalPower;
    }

    @Override
    public boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num) {
        if (getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        Level world = getPackage().world;
        
        // Impact particles are rendered client-side: 1.12 sent PacketFXFocusPartImpact here,
        // which re-ran renderParticleFX 15 times at the impact point on each client.
        if (!world.isClientSide()) {
            Vec3 impact = target.getLocation();
            PacketHandler.sendToAllAround(
                new PacketFXFocusPartImpact(impact.x, impact.y, impact.z, new String[] { getKey() }),
                (ServerLevel) world, BlockPos.containing(impact), 64.0);
        }
        
        if (target.getType() == HitResult.Type.ENTITY && target instanceof EntityHitResult entityHit) {
            Entity hitEntity = entityHit.getEntity();
            
            if (hitEntity == null || !(hitEntity instanceof LivingEntity living)) {
                return false;
            }
            
            // Undead take damage instead of healing
            if (living.isInvertedHealAndHarm()) {
                float damage = getSettingValue("power") * finalPower * 1.5f;
                
                Entity caster = getCaster();
                DamageSource damageSource;
                if (caster != null) {
                    damageSource = world.damageSources().indirectMagic(living, caster);
                } else {
                    damageSource = world.damageSources().magic();
                }
                
                living.hurt(damageSource, damage);
            } else {
                // Heal living entities
                float healAmount = getSettingValue("power") * finalPower;
                living.heal(healAmount);
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
            new NodeSetting("power", "focus.heal.power", 
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
        // 1.12-faithful: FXGeneric heal mote (white, alpha 0->0.7->0.7->0)
        FXGeneric fb = new FXGeneric((ClientLevel) level, posX, posY, posZ,
                motionX + random.nextGaussian() * 0.01,
                motionY + random.nextGaussian() * 0.01,
                motionZ + random.nextGaussian() * 0.01);
        fb.setMaxAge((int) (10.0F + 10.0F * random.nextFloat()));
        fb.setColor(1.0F, 1.0F, 1.0F);
        fb.setAlphaKeyframes(0.0F, 0.7F, 0.7F, 0.0F);
        fb.setGridSize(64);
        fb.setParticles(0, 1, 1);
        fb.setScaleKeyframes(random.nextFloat() * 2.0F * 0.1F, random.nextFloat() * 0.1F);
        fb.setSlowDown(0.8);
        fb.setGravity((float) (random.nextGaussian() * 0.1F));
        fb.setRandomMovementScale(0.0125F, 0.0125F, 0.0125F);
        fb.setRotationSpeed((float) random.nextGaussian());
        FXDispatcher.INSTANCE.addEffectWithDelay(fb, random.nextInt(4));
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            caster.level().playSound(null, caster.blockPosition().above(), 
                SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 
                2.0f, 2.0f + (float)(caster.level().getRandom().nextGaussian() * 0.1));
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
