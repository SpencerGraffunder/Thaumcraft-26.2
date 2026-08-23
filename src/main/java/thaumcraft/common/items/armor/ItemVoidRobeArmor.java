package thaumcraft.common.items.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;

/**
 * Void Robe Armor - The ultimate mage armor combining void metal protection
 * with robe aesthetics. Self-repairs, provides vis discount, and goggles functionality
 * on the helmet. Causes warp when worn.
 */
public class ItemVoidRobeArmor extends Item 
        implements IVisDiscountGear, IGoggles, IRevealer, IWarpingGear, DyeableLeatherItem {
    
    // Default color (dark purple)
    private static final int DEFAULT_COLOR = 0x6A4C00;
    
    public ItemVoidRobeArmor(ArmorType type) {
        super(new Item.Properties().humanoidArmor(ThaumcraftMaterials.ARMORMAT_VOIDROBE, type));
    
    }
    
    // Factory methods
    public static ItemVoidRobeArmor createHelmet() {
        return new ItemVoidRobeArmor(ArmorType.HELMET);
    }
    
    public static ItemVoidRobeArmor createChestplate() {
        return new ItemVoidRobeArmor(ArmorType.CHESTPLATE);
    }
    
    public static ItemVoidRobeArmor createLeggings() {
        return new ItemVoidRobeArmor(ArmorType.LEGGINGS);
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.VOID_METAL_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // Has overlay for dyeable layer
        return type == null ? "thaumcraft:textures/entity/armor/void_robe_armor_overlay.png" 
                           : "thaumcraft:textures/entity/armor/void_robe_armor.png";
    }
    
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        
        // Self-repair while worn
        if (!level.isClientSide() && stack.isDamaged() && entity.tickCount % 20 == 0 
                && entity instanceof LivingEntity living) {
            // Check if actually worn
            for (ItemStack armorPiece : living.getArmorSlots()) {
                if (armorPiece == stack) {
                    stack.setDamageValue(stack.getDamageValue() - 1);
                    break;
                }
            }
        }
    }
    
    // ==================== IVisDiscountGear ====================
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 5; // 5% discount per piece
    }
    
    // ==================== IGoggles/IRevealer ====================
    
    @Override
    public boolean showNodes(ItemStack itemstack, LivingEntity player) {
        return this.getType() == ArmorType.HELMET;
    }
    
    @Override
    public boolean showIngamePopups(ItemStack itemstack, LivingEntity player) {
        return this.getType() == ArmorType.HELMET;
    }
    
    // ==================== IWarpingGear ====================
    
    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 3; // High warp for powerful armor
    }
    
    // ==================== DyeableLeatherItem ====================
    
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
     * Allow using cauldron to wash dye off robes.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        
        if (state.is(Blocks.WATER_CAULDRON)) {
            ItemStack stack = context.getItemInHand();
            if (hasCustomColor(stack)) {
                if (!level.isClientSide()) {
                    clearColor(stack);
                    LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        
        return super.useOn(context);
    }
}
