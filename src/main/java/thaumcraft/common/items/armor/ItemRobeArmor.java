package thaumcraft.common.items.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.DyeableLeatherItem;
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
public class ItemRobeArmor extends Item implements IVisDiscountGear, DyeableLeatherItem {
    
    // Default robe color (brown-ish purple)
    private static final int DEFAULT_COLOR = 0x6A4C00;
    
    public ItemRobeArmor(ArmorType type) {
        super(new Item.Properties().humanoidArmor(ThaumcraftMaterials.ARMORMAT_SPECIAL, type));
    
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
        return this.getType() == ArmorType.BOOTS ? 2 : 3;
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.ENCHANTED_FABRIC.get()) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // Use layer 1 for chest/boots, layer 2 for legs
        if (slot == EquipmentSlot.LEGS) {
            return type == null ? "thaumcraft:textures/entity/armor/robes_2.png" 
                               : "thaumcraft:textures/entity/armor/robes_2_overlay.png";
        }
        return type == null ? "thaumcraft:textures/entity/armor/robes_1.png" 
                           : "thaumcraft:textures/entity/armor/robes_1_overlay.png";
    }
    
    // ==================== Dyeable Implementation ====================
    
    @Override
    public int getColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("display");
        return tag != null && tag.contains("color") ? tag.getIntOr("color", 0) : DEFAULT_COLOR;
    }
    
    @Override
    public boolean hasCustomColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("display");
        return tag != null && tag.contains("color");
    }
    
    @Override
    public void clearColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("display");
        if (tag != null && tag.contains("color")) {
            tag.remove("color");
        }
    }
    
    @Override
    public void setColor(ItemStack stack, int color) {
        stack.getOrCreateTagElement("display").putInt("color", color);
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
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        
        return super.useOn(context);
    }
}
