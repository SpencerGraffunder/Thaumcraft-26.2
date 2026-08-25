package thaumcraft.common.items.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
        implements IVisDiscountGear, IGoggles, IRevealer, IWarpingGear {
    
    // Default color (dark purple)
    private static final int DEFAULT_COLOR = 0x6A4C00;
    
    public ItemVoidRobeArmor(ArmorType type) {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties().humanoidArmor(ThaumcraftMaterials.ARMORMAT_VOIDROBE, type)));
    
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
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        
        // Self-repair while worn
        if (!level.isClientSide() && stack.isDamaged() && entity.tickCount % 20 == 0 
                && entity instanceof LivingEntity living) {
            // Check if actually worn
            for (net.minecraft.world.entity.EquipmentSlot s : new net.minecraft.world.entity.EquipmentSlot[]{
                    net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.HEAD}) {
                if (living.getItemBySlot(s) == stack) {
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
        return getArmorType(itemstack) == ArmorType.HELMET;
    }
    
    @Override
    public boolean showIngamePopups(ItemStack itemstack, LivingEntity player) {
        return getArmorType(itemstack) == ArmorType.HELMET;
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
    
    // ==================== IWarpingGear ====================
    
    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 3; // High warp for powerful armor
    }
    
    // ==================== DyeableLeatherItem ====================
    
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
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }
        }
        
        return super.useOn(context);
    }
}
