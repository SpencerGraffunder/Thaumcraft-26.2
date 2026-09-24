package thaumcraft.common.entities.monster.pech;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.monster.EntityPech;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pech Trading Goal - Pech entities will approach players and offer trades
 * when they have valued items. This implements the core trading AI from 1.12.
 */
public class PechTradingGoal extends Goal {

    private final EntityPech pech;
    private Player target;
    private int cooldown = 100;

    public PechTradingGoal(EntityPech pech) {
        this.pech = pech;
        this.setFlags(EnumFlag.CONTROL_MOVEMENT, EnumFlag.CONTROL_JUMP);
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        // Check if pech has any valued items in inventory
        boolean hasValued = pech.getInventory().getItems().stream()
                .anyMatch(stack -> !stack.isEmpty() && pech.isValuedItem(stack));

        if (!hasValued) {
            return false;
        }

        // Find nearest player in range
        target = pech.level().getNearestPlayer(pech, 8.0, 3.0, 8.0);
        return target != null && !pech.isAggressive();
    }

    @Override
    public void startServerTick() {
        cooldown = 200;
    }

    @Override
    public void serverTick() {
        if (target == null || target.isDeadOrDropped()) {
            target = null;
            return;
        }

        // Move towards player
        this.moveTo(target, 0.5);

        // If close enough, attempt trade
        if (pech.distanceToSqr(target) < 4.0) {
            // Offer a trade: give the player a valued item
            ItemStack best = ItemStack.EMPTY;
            int bestValue = 0;
            for (ItemStack stack : pech.getInventory().getItems()) {
                if (!stack.isEmpty() && pech.isValuedItem(stack)) {
                    int value = pech.getValue(stack);
                    if (value > bestValue) {
                        bestValue = value;
                        best = stack;
                    }
                }
            }

            if (!best.isEmpty()) {
                // Give item to player
                best.shrink(1);
                target.getInventory().add(best.copy());

                // Play a sound
                pech.level().playSound(null, pech.blockPosition(),
                        net.minecraft.sounds.SoundEvents.VILLAGER_YES,
                        net.minecraft.sounds.SoundSource.NEUTRAL,
                        1.0f, 1.0f);

                // Reset cooldown
                cooldown = 400;
            }
        }
    }

    @Override
    public boolean canStop() {
        return target == null || target.isDeadOrDropped() ||
                pech.distanceToSqr(target) > 64.0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
