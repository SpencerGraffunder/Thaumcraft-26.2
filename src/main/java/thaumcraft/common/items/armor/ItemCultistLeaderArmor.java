package thaumcraft.common.items.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IWarpingGear;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Crimson Praetor Armor - Elite armor worn by the Crimson Praetor (Cultist Leader).
 * Has special custom model rendering with extended shoulder pauldrons and cape details.
 * Provides excellent protection with toughness bonus.
 * Also inflicts warp on the wearer due to its eldritch nature.
 */
public class ItemCultistLeaderArmor extends Item implements IWarpingGear {
    
    public ItemCultistLeaderArmor(ArmorType type) {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties().humanoidArmor(ThaumcraftMaterials.ARMORMAT_CULTIST_LEADER, type)));
    
    }
    
    // Factory methods for each armor piece
    public static ItemCultistLeaderArmor createHelmet() {
        return new ItemCultistLeaderArmor(ArmorType.HELMET);
    }
    
    public static ItemCultistLeaderArmor createChestplate() {
        return new ItemCultistLeaderArmor(ArmorType.CHESTPLATE);
    }
    
    public static ItemCultistLeaderArmor createLeggings() {
        return new ItemCultistLeaderArmor(ArmorType.LEGGINGS);
    }
    
    /**
     * Praetor armor inflicts warp on the wearer.
     * Full set gives 2 warp total.
     */
    @Override
    public int getWarp(ItemStack stack, Player player) {
        return switch (getArmorType(stack)) {
            case HELMET -> 1;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 0;
            default -> 0;
        };
    }
    
    private static ArmorType getArmorType(ItemStack stack) {
        net.minecraft.world.item.equipment.Equippable equippable = stack.getOrDefault(net.minecraft.core.component.DataComponents.EQUIPPABLE, null);
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
}
