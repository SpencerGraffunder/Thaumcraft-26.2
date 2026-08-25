package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Fortress Armor - Heavy battle mage armor.
 * The helmet can have goggles attached and different mask variants.
 * Provides bonus armor when wearing multiple pieces.
 */
public class ItemFortressArmor extends Item implements IGoggles, IRevealer {
    
    public ItemFortressArmor(ArmorType type) {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties().humanoidArmor(ThaumcraftMaterials.ARMORMAT_FORTRESS, type)));
    
    }
    
    // Factory methods
    public static ItemFortressArmor createHelmet() {
        return new ItemFortressArmor(ArmorType.HELMET);
    }
    
    public static ItemFortressArmor createChestplate() {
        return new ItemFortressArmor(ArmorType.CHESTPLATE);
    }
    
    public static ItemFortressArmor createLeggings() {
        return new ItemFortressArmor(ArmorType.LEGGINGS);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        // Show attached goggles
        if (hasGoggles(stack)) {
            builder.accept(Component.translatable("item.thaumcraft.goggles.name")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        
        // Show mask variant
        if (hasMask(stack)) {
            int maskType = getMaskType(stack);
            builder.accept(Component.translatable("item.thaumcraft.fortress_helm.mask." + maskType)
                    .withStyle(ChatFormatting.GOLD));
        }
        
        super.appendHoverText(stack, context, display, builder, flag);
    }
    
    // ==================== Goggles Attachment ====================
    
    /**
     * Check if this helmet has goggles attached.
     */
    public static boolean hasGoggles(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag != null && tag.contains("goggles");
    }
    
    /**
     * Attach goggles to this helmet.
     */
    public static void attachGoggles(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("goggles", true);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
    
    /**
     * Remove goggles from this helmet.
     */
    public static void removeGoggles(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag != null) {
            tag.remove("goggles");
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        }
    }
    
    // ==================== Mask System ====================
    
    /**
     * Check if this helmet has a mask.
     */
    public static boolean hasMask(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag != null && tag.contains("mask");
    }
    
    /**
     * Get the mask type (0-3 for different variants).
     */
    public static int getMaskType(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag != null ? tag.getIntOr("mask", 0) : 0;
    }
    
    /**
     * Set the mask type.
     */
    public static void setMask(ItemStack stack, int maskType) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("mask", maskType);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
    
    // ==================== IGoggles/IRevealer Implementation ====================
    
    @Override
    public boolean showNodes(ItemStack itemstack, LivingEntity player) {
        return hasGoggles(itemstack) && getArmorType(itemstack) == ArmorType.HELMET;
    }
    
    @Override
    public boolean showIngamePopups(ItemStack itemstack, LivingEntity player) {
        return hasGoggles(itemstack) && getArmorType(itemstack) == ArmorType.HELMET;
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
    
    // ==================== Set Bonus Calculation ====================
    
    /**
     * Count how many fortress armor pieces the player is wearing.
     */
    public static int countFortressPieces(Player player) {
        int count = 0;
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.HEAD}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.getItem() instanceof ItemFortressArmor) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Calculate bonus armor from wearing multiple fortress pieces.
     * Wearing 2+ pieces grants bonus armor.
     */
    public static int getSetBonus(Player player) {
        int pieces = countFortressPieces(player);
        if (pieces >= 2) {
            return pieces - 1; // +1 armor per piece after the first
        }
        return 0;
    }
}
