package thaumcraft.common.items.consumables;

import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.projectile.EntityAlumentum;

/**
 * Alumentum - Throwable explosive item.
 * When thrown, creates an explosion on impact.
 * Also dispensable (1.12 BehaviorDispenseAlumetum).
 *
 * Ported to 1.20.1
 */
public class ItemAlumentum extends Item {

    public ItemAlumentum() {
        super(thaumcraft.init.ItemRegistration.id(new Properties().stacksTo(64)));
        DispenserBlock.registerBehavior(this, new DispenseAlumentumBehavior());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Consume item if not creative
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Play throw sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EGG_THROW, SoundSource.PLAYERS,
                0.3f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        // Spawn projectile on server
        if (!level.isClientSide()) {
            EntityAlumentum alumentum = new EntityAlumentum(level, player);
            alumentum.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0f, 0.4f, 2.0f);
            level.addFreshEntity(alumentum);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Dispenser behavior: fires an alumentum projectile from the dispenser
     * (equivalent of 1.12 thaumcraft.common.lib.BehaviorDispenseAlumetum).
     */
    private static class DispenseAlumentumBehavior implements DispenseItemBehavior {
        @Override
        public ItemStack dispense(BlockSource source, ItemStack stack) {
            Vec3 center = source.center();
            EntityAlumentum alumentum = new EntityAlumentum(source.level(), center.x, center.y, center.z);
            alumentum.shoot(center.x, center.y + 0.1f, center.z, 0.1f, 0.5f);
            source.level().addFreshEntity(alumentum);
            stack.shrink(1);
            return stack;
        }
    }
}
