package thaumcraft.common.entities.monster.pech;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.monster.EntityPech;

import java.util.Comparator;
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
        this.setFlags(EnumFlag.CONTROL_MOVEMENT);
    }

    @Override
    public boolean canUse() {
        if (pech.getInventory().hasItem()) {
            return false; // Already have items
        }

        // Find nearest valued item on the ground
        List<ItemEntity> items = pech.level().getEntitiesOfClass(ItemEntity.class,
                pech.getBoundingBox().inflate(6.0, 4.0, 6.0),
                item -> item.getItem().getCount() > 0 &&
                        pech.isValuedItem(item.getItem()));

        if (items.isEmpty()) {
            return false;
        }

        target = items.stream()
                .min(Comparator.comparingDouble(item -> pech.distanceToSqr(item)))
                .orElse(null);

        return target != null;
    }

    @Override
    public void serverTick() {
        if (target == null || target.isRemoved()) {
            target = null;
            return;
        }

        // Move towards the item
        this.moveTo(target, 0.6);

        // Pick up the item if close enough
        if (pech.distanceToSqr(target) < 2.0) {
            ItemStack stack = target.getItem().copy();
            stack.shrink(1);
            target.getItem().shrink(1);

            if (pech.isValuedItem(stack)) {
                pech.getInventory().add(stack);
            } else {
                // Not a valued item, drop it
                pech.drop(stack);
            }
        }
    }

    @Override
    public boolean canStop() {
        return target == null || target.isRemoved();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
