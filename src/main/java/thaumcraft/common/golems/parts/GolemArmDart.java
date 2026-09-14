package thaumcraft.common.golems.parts;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.common.entities.projectile.EntityGolemDart;

/**
 * DARTS arm function - ranged attack with golem darts.
 */
public class GolemArmDart implements GolemArm.IArmFunction {

    @Override
    public void onMeleeAttack(IGolemAPI golem, Entity target) {
        // Darts don't do melee attacks
    }

    @Override
    public void onRangedAttack(IGolemAPI golem, LivingEntity target, float distanceFactor) {
        if (golem.getGolemWorld() == null || golem.getGolemEntity() == null) return;

        Level world = golem.getGolemWorld();
        LivingEntity golemEntity = golem.getGolemEntity();

        EntityGolemDart dart = new EntityGolemDart(world, golemEntity);
        double d0 = target.getX() - golemEntity.getX();
        double d1 = target.getY() + target.getEyeHeight() * 0.9 - golemEntity.getEyeY();
        double d2 = target.getZ() - golemEntity.getZ();

        dart.shoot(d0, d1, d2, 1.6f, 3.0f);
        world.addFreshEntity(dart);

        golemEntity.playSound(SoundEvents.ARROW_SHOOT, 1.0f,
                1.0f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
    }

    @Override
    public <T extends Mob & RangedAttackMob> RangedAttackGoal getRangedAttackAI(T mob) {
        // Use vanilla RangedAttackGoal with appropriate parameters
        return new RangedAttackGoal(mob, 1.0, 20, 25, 16.0f);
    }

    // onUpdateTick is a default method in IGenericFunction, no override needed
}
