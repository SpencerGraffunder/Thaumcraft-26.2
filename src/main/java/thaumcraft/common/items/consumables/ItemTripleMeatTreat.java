package thaumcraft.common.items.consumables;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Triple Meat Treat - Special food item made from three types of meat.
 * Provides good nutrition and has a chance to grant regeneration.
 * Can be eaten even when not hungry.
 * 
 * Ported to 1.20.1
 */
public class ItemTripleMeatTreat extends Item {
    
    public ItemTripleMeatTreat() {
        super(thaumcraft.init.ItemRegistration.id(new Properties()
                .food(new FoodProperties.Builder()
                        .nutrition(6)
                        .saturationModifier(0.8f)
                        .alwaysEdible() // Can eat even when full
                        .build())));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // 1.12: 66% chance of Regeneration(100,0) — 1.20.1 FoodProperties has no effects field
        if (!level.isClientSide() && level.getRandom().nextFloat() < 0.66f) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
