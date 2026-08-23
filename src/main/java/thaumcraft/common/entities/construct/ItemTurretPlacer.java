package thaumcraft.common.entities.construct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

import thaumcraft.init.ModEntities;

/**
 * Turret Placer - Places turret constructs in the world.
 * Different variants for basic crossbow, advanced crossbow, and arcane bore.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class ItemTurretPlacer extends Item {
    
    public enum TurretType {
        BASIC_CROSSBOW,
        ADVANCED_CROSSBOW,
        ARCANE_BORE
    }
    
    private final TurretType turretType;
    
    public ItemTurretPlacer(TurretType type) {
        super(new Item.Properties().stacksTo(16));
        this.turretType = type;
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction side = context.getClickedFace();
        ItemStack stack = context.getItemInHand();
        
        // Can't place on bottom face
        if (side == Direction.DOWN) {
            return InteractionResult.PASS;
        }
        
        // Determine placement position
        boolean replaceable = level.getBlockState(clickedPos).canBeReplaced();
        BlockPos placePos = replaceable ? clickedPos : clickedPos.relative(side);
        
        // Check if player can edit
        if (context.getPlayer() != null && !context.getPlayer().mayUseItemAt(placePos, side, stack)) {
            return InteractionResult.PASS;
        }
        
        // Check if both blocks (turret is 2 high) can be placed
        BlockPos upperPos = placePos.above();
        boolean canPlace = (level.isEmptyBlock(placePos) || level.getBlockState(placePos).canBeReplaced()) &&
                          (level.isEmptyBlock(upperPos) || level.getBlockState(upperPos).canBeReplaced());
        
        if (!canPlace) {
            return InteractionResult.PASS;
        }
        
        // Check for entities in the way
        double x = placePos.getX();
        double y = placePos.getY();
        double z = placePos.getZ();
        List<Entity> entities = level.getEntities(null, new AABB(x, y, z, x + 1, y + 2, z + 1));
        if (!entities.isEmpty()) {
            return InteractionResult.PASS;
        }
        
        if (!level.isClientSide()) {
            // Clear the blocks
            level.removeBlock(placePos, false);
            level.removeBlock(upperPos, false);
            
            // Create the turret
            EntityOwnedConstruct turret = createTurret(level, placePos, context);
            
            if (turret != null) {
                level.addFreshEntity(turret);
                turret.setOwned(true);
                turret.setValidSpawn();
                
                if (context.getPlayer() != null) {
                    turret.setOwnerUUID(context.getPlayer().getUUID());
                }
                
                level.playSound(null, turret.getX(), turret.getY(), turret.getZ(),
                        SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75f, 0.8f);
                
                // Consume item
                stack.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
    
    /**
     * Create the appropriate turret entity based on type.
     */
    private EntityOwnedConstruct createTurret(Level level, BlockPos pos, UseOnContext context) {
        return switch (turretType) {
            case BASIC_CROSSBOW -> new EntityTurretCrossbow(
                    ModEntities.TURRET_CROSSBOW.get(), level, pos);
            case ADVANCED_CROSSBOW -> new EntityTurretCrossbowAdvanced(
                    ModEntities.TURRET_CROSSBOW_ADVANCED.get(), level, pos);
            case ARCANE_BORE -> {
                // Arcane bore needs facing direction
                Direction facing = context.getPlayer() != null ? 
                        context.getPlayer().getDirection() : Direction.NORTH;
                // TODO: Create EntityArcaneBore when implemented
                // yield new EntityArcaneBore(level, pos, facing);
                yield null;
            }
        };
    }
    
    // ==================== Factory Methods ====================
    
    public static ItemTurretPlacer createBasic() {
        return new ItemTurretPlacer(TurretType.BASIC_CROSSBOW);
    }
    
    public static ItemTurretPlacer createAdvanced() {
        return new ItemTurretPlacer(TurretType.ADVANCED_CROSSBOW);
    }
    
    public static ItemTurretPlacer createBore() {
        return new ItemTurretPlacer(TurretType.ARCANE_BORE);
    }
}
