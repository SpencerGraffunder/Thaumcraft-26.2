package thaumcraft.common.items.tools;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.init.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;

/**
 * Void Metal Axe - Powerful but warping axe that applies weakness and self-repairs.
 */
public class ItemVoidAxe extends AxeItem implements IWarpingGear {
    
    public ItemVoidAxe() {
        super(ThaumcraftMaterials.TOOLMAT_VOID, 5.0F, -3.0F, thaumcraft.init.ItemRegistration.id(new Item.Properties().rarity(Rarity.RARE)));
    }
    
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        // Self-repair: repair 1 durability every second (20 ticks)
        if (stack.isDamaged() && entity != null && entity.tickCount % 20 == 0 && entity instanceof LivingEntity) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }
    
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply weakness effect on hit
        if (!attacker.level().isClientSide()) {
            if (!(target instanceof Player) || isPvPEnabled(attacker.level())) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            }
        }
        super.hurtEnemy(stack, target, attacker);
    }
    
    private boolean isPvPEnabled(Level level) {
        if (level.getServer() != null) {
            return true;
        }
        return true;
    }
    
    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 1;
    }
}
