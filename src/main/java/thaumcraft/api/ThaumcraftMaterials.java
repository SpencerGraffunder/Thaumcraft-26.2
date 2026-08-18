package thaumcraft.api;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ToolMaterial;
import thaumcraft.Thaumcraft;
import thaumcraft.init.ModItems;

import java.util.function.Supplier;

/**
 * Custom materials for Thaumcraft armor and tools.
 * Updated for 1.20.1 using the new ToolMaterial and ArmorMaterial systems.
 */
public class ThaumcraftMaterials {

    // ==================== Tool Tiers ====================
    
    /**
     * Thaumium - Better than iron, magic-infused metal
     * Harvest Level: 3 (diamond), Durability: 500, Speed: 7, Damage: 2.5, Enchantability: 22
     */
    public static final ToolMaterial TOOLMAT_THAUMIUM = new ToolMaterial() {
        @Override public int getUses() { return 500; }
        @Override public float getSpeed() { return 7.0F; }
        @Override public float getAttackDamageBonus() { return 2.5F; }
        @Override public int getLevel() { return 3; }
        @Override public int getEnchantmentValue() { return 22; }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.THAUMIUM_INGOT.get()); 
        }
    };
    
    /**
     * Void Metal - Brittle but extremely powerful
     * Harvest Level: 4 (netherite), Durability: 150, Speed: 8, Damage: 3, Enchantability: 10
     */
    public static final ToolMaterial TOOLMAT_VOID = new ToolMaterial() {
        @Override public int getUses() { return 150; }
        @Override public float getSpeed() { return 8.0F; }
        @Override public float getAttackDamageBonus() { return 3.0F; }
        @Override public int getLevel() { return 4; }
        @Override public int getEnchantmentValue() { return 10; }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.VOID_METAL_INGOT.get()); 
        }
    };
    
    /**
     * Elemental Thaumium - Enhanced thaumium with elemental power
     * Harvest Level: 3, Durability: 1500, Speed: 9, Damage: 3, Enchantability: 18
     */
    public static final ToolMaterial TOOLMAT_ELEMENTAL = new ToolMaterial() {
        @Override public int getUses() { return 1500; }
        @Override public float getSpeed() { return 9.0F; }
        @Override public float getAttackDamageBonus() { return 3.0F; }
        @Override public int getLevel() { return 3; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.THAUMIUM_INGOT.get()); 
        }
    };

    // ==================== Armor Materials ====================
    
    /**
     * Thaumium Armor - Balanced magical armor
     * Protection: 2/5/6/2, Durability: 25x, Enchantability: 25, Toughness: 1.0
     */
    public static final ArmorMaterial ARMORMAT_THAUMIUM = new ArmorMaterial() {
        private static final int[] DURABILITY = {13, 15, 16, 11};
        private static final int[] PROTECTION = {2, 5, 6, 2};
        
        @Override public int getDurabilityForType(ArmorItem.Type type) { 
            return DURABILITY[type.ordinal()] * 25; 
        }
        @Override public int getDefenseForType(ArmorItem.Type type) { 
            return PROTECTION[type.ordinal()]; 
        }
        @Override public int getEnchantmentValue() { return 25; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { 
            return SoundEvents.ARMOR_EQUIP_IRON; 
        }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.THAUMIUM_INGOT.get()); 
        }
        @Override public String getName() { return Thaumcraft.MODID + ":thaumium"; }
        @Override public float getToughness() { return 1.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    };
    
    /**
     * Special/Cloth Armor (Robes, Goggles) - Light magical protection
     * Protection: 1/2/3/1, Durability: 25x, Enchantability: 25, Toughness: 0
     */
    public static final ArmorMaterial ARMORMAT_SPECIAL = new ArmorMaterial() {
        private static final int[] DURABILITY = {13, 15, 16, 11};
        private static final int[] PROTECTION = {1, 2, 3, 1};
        
        @Override public int getDurabilityForType(ArmorItem.Type type) { 
            return DURABILITY[type.ordinal()] * 25; 
        }
        @Override public int getDefenseForType(ArmorItem.Type type) { 
            return PROTECTION[type.ordinal()]; 
        }
        @Override public int getEnchantmentValue() { return 25; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { 
            return SoundEvents.ARMOR_EQUIP_LEATHER; 
        }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.ENCHANTED_FABRIC.get()); 
        }
        @Override public String getName() { return Thaumcraft.MODID + ":special"; }
        @Override public float getToughness() { return 0.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    };
    
    /**
     * Void Metal Armor - High protection, brittle
     * Protection: 3/6/8/3, Durability: 10x, Enchantability: 10, Toughness: 1.0
     */
    public static final ArmorMaterial ARMORMAT_VOID = new ArmorMaterial() {
        private static final int[] DURABILITY = {13, 15, 16, 11};
        private static final int[] PROTECTION = {3, 6, 8, 3};
        
        @Override public int getDurabilityForType(ArmorItem.Type type) { 
            return DURABILITY[type.ordinal()] * 10; 
        }
        @Override public int getDefenseForType(ArmorItem.Type type) { 
            return PROTECTION[type.ordinal()]; 
        }
        @Override public int getEnchantmentValue() { return 10; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { 
            return SoundEvents.ARMOR_EQUIP_CHAIN; 
        }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.VOID_METAL_INGOT.get()); 
        }
        @Override public String getName() { return Thaumcraft.MODID + ":void"; }
        @Override public float getToughness() { return 1.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    };
    
    /**
     * Void Robe Armor - Powerful mage armor
     * Protection: 4/7/9/4, Durability: 18x, Enchantability: 10, Toughness: 2.0
     */
    public static final ArmorMaterial ARMORMAT_VOIDROBE = new ArmorMaterial() {
        private static final int[] DURABILITY = {13, 15, 16, 11};
        private static final int[] PROTECTION = {4, 7, 9, 4};
        
        @Override public int getDurabilityForType(ArmorItem.Type type) { 
            return DURABILITY[type.ordinal()] * 18; 
        }
        @Override public int getDefenseForType(ArmorItem.Type type) { 
            return PROTECTION[type.ordinal()]; 
        }
        @Override public int getEnchantmentValue() { return 10; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { 
            return SoundEvents.ARMOR_EQUIP_LEATHER; 
        }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.VOID_METAL_INGOT.get()); 
        }
        @Override public String getName() { return Thaumcraft.MODID + ":void_robe"; }
        @Override public float getToughness() { return 2.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    };
    
    /**
     * Fortress Armor - Heavy battle mage armor
     * Protection: 3/6/7/3, Durability: 40x, Enchantability: 25, Toughness: 3.0
     */
    public static final ArmorMaterial ARMORMAT_FORTRESS = new ArmorMaterial() {
        private static final int[] DURABILITY = {13, 15, 16, 11};
        private static final int[] PROTECTION = {3, 6, 7, 3};
        
        @Override public int getDurabilityForType(ArmorItem.Type type) { 
            return DURABILITY[type.ordinal()] * 40; 
        }
        @Override public int getDefenseForType(ArmorItem.Type type) { 
            return PROTECTION[type.ordinal()]; 
        }
        @Override public int getEnchantmentValue() { return 25; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { 
            return SoundEvents.ARMOR_EQUIP_IRON; 
        }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.of(ModItems.THAUMIUM_INGOT.get()); 
        }
        @Override public String getName() { return Thaumcraft.MODID + ":fortress"; }
        @Override public float getToughness() { return 3.0F; }
        @Override public float getKnockbackResistance() { return 0.1F; }
    };
    
    /**
     * Cultist Plate Armor - Crimson cult heavy armor
     * Protection: 2/5/6/2, Durability: 18x, Enchantability: 13, Toughness: 0
     */
    public static final ArmorMaterial ARMORMAT_CULTIST_PLATE = new ArmorMaterial() {
        private static final int[] DURABILITY = {13, 15, 16, 11};
        private static final int[] PROTECTION = {2, 5, 6, 2};
        
        @Override public int getDurabilityForType(ArmorItem.Type type) { 
            return DURABILITY[type.ordinal()] * 18; 
        }
        @Override public int getDefenseForType(ArmorItem.Type type) { 
            return PROTECTION[type.ordinal()]; 
        }
        @Override public int getEnchantmentValue() { return 13; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { 
            return SoundEvents.ARMOR_EQUIP_IRON; 
        }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.EMPTY; 
        }
        @Override public String getName() { return Thaumcraft.MODID + ":cultist_plate"; }
        @Override public float getToughness() { return 0.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    };
    
    /**
     * Cultist Robe Armor - Crimson cult light armor
     * Protection: 2/4/5/2, Durability: 17x, Enchantability: 13, Toughness: 0
     */
    public static final ArmorMaterial ARMORMAT_CULTIST_ROBE = new ArmorMaterial() {
        private static final int[] DURABILITY = {13, 15, 16, 11};
        private static final int[] PROTECTION = {2, 4, 5, 2};
        
        @Override public int getDurabilityForType(ArmorItem.Type type) { 
            return DURABILITY[type.ordinal()] * 17; 
        }
        @Override public int getDefenseForType(ArmorItem.Type type) { 
            return PROTECTION[type.ordinal()]; 
        }
        @Override public int getEnchantmentValue() { return 13; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { 
            return SoundEvents.ARMOR_EQUIP_CHAIN; 
        }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.EMPTY; 
        }
        @Override public String getName() { return Thaumcraft.MODID + ":cultist_robe"; }
        @Override public float getToughness() { return 0.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    };
    
    /**
     * Cultist Leader Armor - Elite crimson cult armor
     * Protection: 3/6/7/3, Durability: 30x, Enchantability: 20, Toughness: 1.0
     */
    public static final ArmorMaterial ARMORMAT_CULTIST_LEADER = new ArmorMaterial() {
        private static final int[] DURABILITY = {13, 15, 16, 11};
        private static final int[] PROTECTION = {3, 6, 7, 3};
        
        @Override public int getDurabilityForType(ArmorItem.Type type) { 
            return DURABILITY[type.ordinal()] * 30; 
        }
        @Override public int getDefenseForType(ArmorItem.Type type) { 
            return PROTECTION[type.ordinal()]; 
        }
        @Override public int getEnchantmentValue() { return 20; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { 
            return SoundEvents.ARMOR_EQUIP_IRON; 
        }
        @Override public Ingredient getRepairIngredient() { 
            return Ingredient.EMPTY; 
        }
        @Override public String getName() { return Thaumcraft.MODID + ":cultist_leader"; }
        @Override public float getToughness() { return 1.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    };
}
