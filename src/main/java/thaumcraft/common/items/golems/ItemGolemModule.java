package thaumcraft.common.items.golems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;

/**
 * Golem module item - modifies golem behavior when right-clicked on a golem.
 * AGGRESSION module: Makes the golem more aggressive
 * VISION module: Makes the golem able to see through walls
 */
public class ItemGolemModule extends Item {

    private final EnumGolemTrait trait;

    public ItemGolemModule(Properties props, EnumGolemTrait trait) {
        super(props);
        this.trait = trait;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Check if target is a golem
        BlockPos clicked = context.getClickedPos();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(clicked).inflate(2.0))) {
            if (entity instanceof thaumcraft.common.golems.EntityThaumcraftGolem golem) {
                // Add the trait to the golem's properties
                var props = golem.getProperties();
                if (!props.hasTrait(trait)) {
                    props.getTraits().add(trait);
                    player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    // Consume the item
                    if (!player.getAbilities().instabuild) {
                        context.getItemInHand().shrink(1);
                    }
                    return InteractionResult.CONSUME;
                }
            }
        }
        return InteractionResult.PASS;
    }

    public EnumGolemTrait getTrait() {
        return trait;
    }

    public static Item create(EnumGolemTrait trait) {
        return new ItemGolemModule(new Item.Properties(), trait);
    }
}
