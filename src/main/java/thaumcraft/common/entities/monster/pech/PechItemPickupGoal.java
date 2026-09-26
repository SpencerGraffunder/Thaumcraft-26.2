package thaumcraft.common.entities.monster.pech;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.monster.EntityPech;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Pech Item Pickup Goal - Pech entities will pick up valued items
 * from the ground to use for trading.
 */
public class PechItemPickupGoal extends Goal {

    private final EntityPech pech;
    private ItemEntity target;

    public PechItemPickupGoal(EntityPech pech) {
        this.pech = pech;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (pech.loot.stream().anyMatch(s -> !s.isEmpty())) {
            return false; // Already have items
        }

        // Find nearest valued item on the ground
        List<ItemEntity> items = pech.level().getEntitiesOfClass(ItemEntity.class,
                pech.getBoundingBox().inflate(6.0, 4.0, 6.0),
                item -> item.getItem().getCount() > 0 &&
                        pech.isValued(item.getItem()));

        if (items.isEmpty()) {
            return false;
        }

        target = items.stream()
                .min(Comparator.comparingDouble(item -> pech.distanceToSqr(item)))
                .orElse(null);

        return target != null;
    }

    @Override
    public void tick() {
        if (target == null || target.isRemoved()) {
            target = null;
            return;
        }

        // Move towards the item
        pech.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 0.6);

        // Pick up the item if close enough
        if (pech.distanceToSqr(target) < 2.0) {
            ItemStack stack = target.getItem().copy();
            stack.shrink(1);
            target.getItem().shrink(1);

            if (pech.isValued(stack)) {
                for (int i = 0; i < pech.loot.size(); i++) {
                    if (pech.loot.get(i).isEmpty()) {
                        pech.loot.set(i, stack);
                        break;
                    }
                }
            } else {
                // Not a valued item, drop it
                pech.drop(stack, false, false);
            }
        }
    }

    // 26.2: Goal.canStop() was removed from the base class; the goal lifecycle is
    // driven by canUse()/canContinueToUse() only.
}
