package thaumcraft.common.items.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;

/**
 * Thaumaturge's Robe Armor - Cloth armor that provides vis discounts.
 * Can be dyed like leather armor.
 */
public class ItemRobeArmor extends Item implements IVisDiscountGear {
    
    // Default robe color (brown-ish purple)
    private static final int DEFAULT_COLOR = 0x6A4C00;
    
    public ItemRobeArmor(ArmorType type) {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties().humanoidArmor(ThaumcraftMaterials.ARMORMAT_SPECIAL, type)));
    
    }
    
    // Factory methods for different armor pieces
    public static ItemRobeArmor createChest() {
        return new ItemRobeArmor(ArmorType.CHESTPLATE);
    }
    
    public static ItemRobeArmor createLegs() {
        return new ItemRobeArmor(ArmorType.LEGGINGS);
    }
    
    public static ItemRobeArmor createBoots() {
        return new ItemRobeArmor(ArmorType.BOOTS);
    }
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        // Boots give 2%, other pieces give 3%
        return getArmorType(stack) == ArmorType.BOOTS ? 2 : 3;
    }
    
    private static ArmorType getArmorType(ItemStack stack) {
        net.minecraft.world.item.equipment.Equippable equippable = stack.getOrDefault(DataComponents.EQUIPPABLE, null);
        if (equippable == null) {
            return null;
        }
        return switch (equippable.slot()) {
            case HEAD -> ArmorType.HELMET;
            case CHEST -> ArmorType.CHESTPLATE;
            case LEGS -> ArmorType.LEGGINGS;
            case FEET -> ArmorType.BOOTS;
            default -> null;
        };
    }
    
    // ==================== Dyeable Implementation ====================
    
    public int getColor(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, DEFAULT_COLOR);
    }
    
    public boolean hasCustomColor(ItemStack stack) {
        return stack.has(DataComponents.DYED_COLOR);
    }
    
    public void clearColor(ItemStack stack) {
        stack.remove(DataComponents.DYED_COLOR);
    }
    
    public void setColor(ItemStack stack, int color) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
    }
    
    /**
     * Allow using cauldron to wash dye off robes (vanilla behavior for leather)
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        
        // Check if clicking on a water cauldron
        if (state.is(Blocks.WATER_CAULDRON)) {
            ItemStack stack = context.getItemInHand();
            if (hasCustomColor(stack)) {
                if (!level.isClientSide()) {
                    clearColor(stack);
                    // Decrease water level
                    LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                }
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }
        }
        
        return super.useOn(context);
    }
}
