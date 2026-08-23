package thaumcraft.api;

import com.google.common.collect.Maps;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import thaumcraft.Thaumcraft;

import java.util.Map;
import net.minecraft.core.registries.Registries;

/**
 * Custom materials for Thaumcraft armor and tools.
 * Ported to MC 26.2: ToolMaterial and ArmorMaterial are now records.
 */
public class ThaumcraftMaterials {

    // ==================== Tool Tiers ====================

    /**
     * Thaumium - Better than iron, magic-infused metal
     * Harvest Level: 3 (diamond), Durability: 500, Speed: 7, Damage: 2.5, Enchantability: 22
     */
    public static final ToolMaterial TOOLMAT_THAUMIUM = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 500, 7.0F, 2.5F, 22, thaumcraftItemTag("thaumium_ingot")
    );

    /**
     * Void Metal - Brittle but extremely powerful
     * Harvest Level: 4 (netherite), Durability: 150, Speed: 8, Damage: 3, Enchantability: 10
     */
    public static final ToolMaterial TOOLMAT_VOID = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 150, 8.0F, 3.0F, 10, thaumcraftItemTag("void_metal_ingot")
    );

    /**
     * Elemental Thaumium - Enhanced thaumium with elemental power
     * Durability: 1500, Speed: 9, Damage: 3, Enchantability: 18
     */
    public static final ToolMaterial TOOLMAT_ELEMENTAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1500, 9.0F, 3.0F, 18, thaumcraftItemTag("thaumium_ingot")
    );

    // ==================== Armor Materials ====================

    /**
     * Thaumium Armor - Balanced magical armor
     * Protection: 2/5/6/2, Enchantability: 25, Toughness: 1.0
     */
    public static final ArmorMaterial ARMORMAT_THAUMIUM = armorMaterial(
            25, 2, 5, 6, 2, 25, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, "thaumium_ingot", "thaumium"
    );

    /**
     * Special/Cloth Armor (Robes, Goggles) - Light magical protection
     * Protection: 1/2/3/1, Enchantability: 25
     */
    public static final ArmorMaterial ARMORMAT_SPECIAL = armorMaterial(
            25, 1, 2, 3, 1, 25, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, "enchanted_fabric", "special"
    );

    /**
     * Void Metal Armor - High protection, brittle
     * Protection: 3/6/8/3, Enchantability: 10, Toughness: 1.0
     */
    public static final ArmorMaterial ARMORMAT_VOID = armorMaterial(
            10, 3, 6, 8, 3, 10, SoundEvents.ARMOR_EQUIP_CHAIN, 1.0F, 0.0F, "void_metal_ingot", "void"
    );

    /**
     * Void Robe Armor - Powerful mage armor
     * Protection: 4/7/9/4, Enchantability: 10, Toughness: 2.0
     */
    public static final ArmorMaterial ARMORMAT_VOIDROBE = armorMaterial(
            18, 4, 7, 9, 4, 10, SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, 0.0F, "void_metal_ingot", "void_robe"
    );

    /**
     * Fortress Armor - Heavy battle mage armor
     * Protection: 3/6/7/3, Enchantability: 25, Toughness: 3.0
     */
    public static final ArmorMaterial ARMORMAT_FORTRESS = armorMaterial(
            40, 3, 6, 7, 3, 25, SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.1F, "thaumium_ingot", "fortress"
    );

    /**
     * Cultist Plate Armor - Crimson cult heavy armor
     * Protection: 2/5/6/2, Enchantability: 13
     */
    public static final ArmorMaterial ARMORMAT_CULTIST_PLATE = armorMaterial(
            18, 2, 5, 6, 2, 13, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, "crimson_cloth", "cultist_plate"
    );

    /**
     * Cultist Robe Armor - Crimson cult light armor
     * Protection: 2/4/5/2, Enchantability: 13
     */
    public static final ArmorMaterial ARMORMAT_CULTIST_ROBE = armorMaterial(
            17, 2, 4, 5, 2, 13, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, 0.0F, "crimson_cloth", "cultist_robe"
    );

    /**
     * Cultist Leader Armor - Elite crimson cult armor
     * Protection: 3/6/7/3, Enchantability: 20, Toughness: 1.0
     */
    public static final ArmorMaterial ARMORMAT_CULTIST_LEADER = armorMaterial(
            30, 3, 6, 7, 3, 20, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, "crimson_cloth", "cultist_leader"
    );

    // ==================== Helpers ====================

    private static TagKey<Item> thaumcraftItemTag(String name) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, name));
    }

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID,
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, name));
    }

    private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helm, int body) {
        return Maps.newEnumMap(Map.of(
                ArmorType.BOOTS, boots, ArmorType.LEGGINGS, legs,
                ArmorType.CHESTPLATE, chest, ArmorType.HELMET, helm, ArmorType.BODY, body));
    }

    private static ArmorMaterial armorMaterial(int durability, int boots, int legs, int chest, int helm,
            int enchantability, Holder<SoundEvent> equipSound, float toughness, float knockback,
            String repairItem, String assetName) {
        return new ArmorMaterial(durability, defense(boots, legs, chest, helm, 3),
                enchantability, equipSound, toughness, knockback,
                thaumcraftItemTag(repairItem), asset(assetName));
    }
}
