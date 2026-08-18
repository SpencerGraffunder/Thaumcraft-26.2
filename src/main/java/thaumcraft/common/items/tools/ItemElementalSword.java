package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Sword of the Zephyr - Air elemental sword.
 * When used, allows the player to float and pushes nearby entities away.
 * Has built-in Arcing infusion enchantment.
 */
public class ItemElementalSword extends Item {

    public ItemElementalSword() {
        super((// attack speed
                new Item.Properties()
                        .rarity(Rarity.RARE)).sword(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 1, -2.8F));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity player, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, player, stack, remainingUseDuration);
        
        int ticksUsed = getUseDuration(stack) - remainingUseDuration;

        // Float effect - reduce falling and add upward momentum
        Vec3 motion = player.getDeltaMovement();
        double newY = motion.y;
        
        if (motion.y < 0) {
            newY = motion.y / 1.2;
            player.fallDistance /= 1.2f;
        }
        
        newY += 0.08;
        if (newY > 0.5) {
            newY = 0.2;
        }
        
        player.setDeltaMovement(motion.x, newY, motion.z);

        // Reset float counter for server player to prevent kick
        if (player instanceof ServerPlayer serverPlayer) {
            // Reset the fly status to prevent kick for "flying" while using the sword
            serverPlayer.resetFallDistance();
        }

        // Push nearby entities away
        AABB pushBox = player.getBoundingBox().inflate(2.5);
        List<Entity> nearbyEntities = level.getEntities(player, pushBox);
        
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player) continue;  // Don't affect players
            if (!(entity instanceof LivingEntity)) continue;
            if (entity.isRemoved()) continue;
            if (player.getVehicle() == entity) continue;

            Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
            Vec3 entityPos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            double distance = playerPos.distanceTo(entityPos) + 0.1;

            // Calculate push direction (away from player)
            Vec3 pushDir = entityPos.subtract(playerPos);
            
            Vec3 entityMotion = entity.getDeltaMovement();
            entity.setDeltaMovement(
                    entityMotion.x + pushDir.x / 2.5 / distance,
                    entityMotion.y + pushDir.y / 2.5 / distance,
                    entityMotion.z + pushDir.z / 2.5 / distance
            );
        }

        // Damage the item periodically
        if (ticksUsed % 20 == 0) {
            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }

        // TODO: Add wind spiral particle effects on client
        // TODO: Add wind sound effects
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enchantment.thaumcraft.arcing").withStyle(style -> style.withColor(0x87CEEB)));
        tooltip.add(Component.translatable("item.thaumcraft.elemental_sword.desc").withStyle(style -> style.withColor(0x808080)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
