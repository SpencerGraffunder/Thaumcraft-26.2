package thaumcraft.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.armor.ItemGoggles;
import thaumcraft.common.items.armor.ItemRobeArmor;
import thaumcraft.common.items.armor.ItemThaumiumArmor;
import thaumcraft.common.items.armor.ItemVoidArmor;
import thaumcraft.common.items.armor.ItemBootsTraveller;
import thaumcraft.common.items.armor.ItemFortressArmor;
import thaumcraft.common.items.armor.ItemVoidRobeArmor;
import thaumcraft.common.items.armor.ItemCultistRobeArmor;
import thaumcraft.common.items.armor.ItemCultistPlateArmor;
import thaumcraft.common.items.armor.ItemCultistBoots;
import thaumcraft.common.items.armor.ItemCultistLeaderArmor;
import thaumcraft.common.items.consumables.ItemPhial;
import thaumcraft.common.items.curios.ItemThaumonomicon;
import thaumcraft.common.items.resources.ItemCrystalEssence;
import thaumcraft.common.items.resources.ItemMaterial;
import thaumcraft.common.items.resources.ItemVisCrystal;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.resources.ItemMagicDust;
import thaumcraft.common.items.tools.ItemScribingTools;
import thaumcraft.common.items.tools.ItemThaumometer;
import thaumcraft.common.items.tools.ItemThaumiumSword;
import thaumcraft.common.items.tools.ItemThaumiumPickaxe;
import thaumcraft.common.items.tools.ItemThaumiumAxe;
import thaumcraft.common.items.tools.ItemThaumiumShovel;
import thaumcraft.common.items.tools.ItemThaumiumHoe;
import thaumcraft.common.items.tools.ItemVoidSword;
import thaumcraft.common.items.tools.ItemVoidPickaxe;
import thaumcraft.common.items.tools.ItemVoidAxe;
import thaumcraft.common.items.tools.ItemVoidShovel;
import thaumcraft.common.items.tools.ItemVoidHoe;
import thaumcraft.common.items.casters.ItemCaster;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.baubles.ItemCloudRing;
import thaumcraft.common.items.baubles.ItemCuriosityBand;
import thaumcraft.common.items.baubles.ItemCharmUndying;
import thaumcraft.common.items.baubles.ItemVerdantCharm;
import thaumcraft.common.items.baubles.ItemVoidseerCharm;
import thaumcraft.common.items.tools.ItemElementalPickaxe;
import thaumcraft.common.items.tools.ItemElementalAxe;
import thaumcraft.common.items.tools.ItemElementalSword;
import thaumcraft.common.items.tools.ItemElementalShovel;
import thaumcraft.common.items.tools.ItemPrimalCrusher;
import thaumcraft.common.items.tools.ItemCrimsonBlade;
import thaumcraft.common.items.tools.ItemElementalHoe;
import thaumcraft.common.items.tools.ItemResonator;
import thaumcraft.common.items.tools.ItemSanityChecker;
import thaumcraft.common.items.tools.ItemHandMirror;
import thaumcraft.common.items.tools.ItemGrappleGun;
import thaumcraft.common.items.consumables.ItemBathSalts;
import thaumcraft.common.items.consumables.ItemSanitySoap;
import thaumcraft.common.items.consumables.ItemBottleTaint;
import thaumcraft.common.items.consumables.ItemCausalityCollapser;
import thaumcraft.common.items.consumables.ItemLabel;
import thaumcraft.common.items.curios.ItemCelestialNotes;
import thaumcraft.common.items.curios.ItemLootBag;
import thaumcraft.common.items.curios.ItemPechWand;
import thaumcraft.common.items.curios.ItemPrimordialPearl;
import thaumcraft.common.items.casters.ItemFocusPouch;
import thaumcraft.common.golems.ItemGolemBell;
import thaumcraft.common.golems.ItemGolemPlacer;
import thaumcraft.common.golems.seals.ItemSealPlacer;
import thaumcraft.common.entities.construct.ItemTurretPlacer;
import thaumcraft.common.items.misc.ItemCreativeFluxSponge;
import thaumcraft.common.items.misc.ItemCreativePlacer;

/**
 * Registry for all Thaumcraft items.
 * Uses DeferredRegister for 1.20.1 Forge.
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(BuiltInRegistries.ITEM, Thaumcraft.MODID);

    // ==================== Tools ====================

    public static final DeferredHolder<Item, Item> THAUMONOMICON = ITEMS.register("thaumonomicon",
            ItemThaumonomicon::new);

    public static final DeferredHolder<Item, Item> CRIMSON_RITES = ITEMS.register("crimson_rites",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final DeferredHolder<Item, Item> THAUMOMETER = ITEMS.register("thaumometer",
            ItemThaumometer::new);

    public static final DeferredHolder<Item, Item> SCRIBING_TOOLS = ITEMS.register("scribing_tools",
            ItemScribingTools::new);

    // ==================== Phials ====================

    public static final DeferredHolder<Item, Item> PHIAL_EMPTY = ITEMS.register("phial_empty",
            ItemPhial::createEmpty);

    public static final DeferredHolder<Item, Item> PHIAL_FILLED = ITEMS.register("phial_filled",
            ItemPhial::createFilled);

    // ==================== Basic Materials ====================

    public static final DeferredHolder<Item, Item> QUICKSILVER = ITEMS.register("quicksilver",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> ALUMENTUM = ITEMS.register("alumentum",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> NITOR = ITEMS.register("nitor",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> SALIS_MUNDUS = ITEMS.register("salis_mundus",
            ItemMagicDust::new);

    public static final DeferredHolder<Item, Item> BALANCED_SHARD = ITEMS.register("balanced_shard",
            ItemMaterial::uncommon);

    // ==================== Metal Ingots & Nuggets ====================

    public static final DeferredHolder<Item, Item> THAUMIUM_INGOT = ITEMS.register("thaumium_ingot",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> THAUMIUM_NUGGET = ITEMS.register("thaumium_nugget",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> VOID_METAL_INGOT = ITEMS.register("void_metal_ingot",
            () -> new ItemMaterial(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<Item, Item> VOID_METAL_NUGGET = ITEMS.register("void_metal_nugget",
            () -> new ItemMaterial(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<Item, Item> BRASS_INGOT = ITEMS.register("brass_ingot",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> BRASS_NUGGET = ITEMS.register("brass_nugget",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> QUICKSILVER_NUGGET = ITEMS.register("quicksilver_nugget",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> QUARTZ_NUGGET = ITEMS.register("quartz_nugget",
            ItemMaterial::basic);

    // ==================== Primal Shards ====================

    public static final DeferredHolder<Item, Item> SHARD_AIR = ITEMS.register("shard_air",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SHARD_FIRE = ITEMS.register("shard_fire",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SHARD_WATER = ITEMS.register("shard_water",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SHARD_EARTH = ITEMS.register("shard_earth",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SHARD_ORDER = ITEMS.register("shard_order",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SHARD_ENTROPY = ITEMS.register("shard_entropy",
            ItemMaterial::basic);

    // ==================== Vis Crystals (6 primal types) ====================

    public static final DeferredHolder<Item, Item> VIS_CRYSTAL_AIR = ITEMS.register("vis_crystal_air",
            () -> new ItemVisCrystal(Aspect.AIR));

    public static final DeferredHolder<Item, Item> VIS_CRYSTAL_FIRE = ITEMS.register("vis_crystal_fire",
            () -> new ItemVisCrystal(Aspect.FIRE));

    public static final DeferredHolder<Item, Item> VIS_CRYSTAL_WATER = ITEMS.register("vis_crystal_water",
            () -> new ItemVisCrystal(Aspect.WATER));

    public static final DeferredHolder<Item, Item> VIS_CRYSTAL_EARTH = ITEMS.register("vis_crystal_earth",
            () -> new ItemVisCrystal(Aspect.EARTH));

    public static final DeferredHolder<Item, Item> VIS_CRYSTAL_ORDER = ITEMS.register("vis_crystal_order",
            () -> new ItemVisCrystal(Aspect.ORDER));

    public static final DeferredHolder<Item, Item> VIS_CRYSTAL_ENTROPY = ITEMS.register("vis_crystal_entropy",
            () -> new ItemVisCrystal(Aspect.ENTROPY));

    // ==================== Crafting Components ====================

    public static final DeferredHolder<Item, Item> AMBER = ITEMS.register("amber",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> AMBER_BEAD = ITEMS.register("amber_bead",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> ENCHANTED_FABRIC = ITEMS.register("enchanted_fabric",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> PRIMAL_CHARM = ITEMS.register("primal_charm",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> SALISITE = ITEMS.register("salisite",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> BLANK_SEAL = ITEMS.register("blank_seal",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> MIRRORED_GLASS = ITEMS.register("mirrored_glass",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> FILTER = ITEMS.register("filter",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> MORPHIC_RESONATOR = ITEMS.register("morphic_resonator",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> VIS_RESONATOR = ITEMS.register("vis_resonator",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> TALLOW = ITEMS.register("tallow",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> JAR_BRACE = ITEMS.register("jar_brace",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> MIND = ITEMS.register("mind",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> MECHANISM_SIMPLE = ITEMS.register("mechanism_simple",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> MECHANISM_COMPLEX = ITEMS.register("mechanism_complex",
            ItemMaterial::uncommon);

    // ==================== Plates ====================

    public static final DeferredHolder<Item, Item> PLATE_IRON = ITEMS.register("plate_iron",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> PLATE_BRASS = ITEMS.register("plate_brass",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> PLATE_THAUMIUM = ITEMS.register("plate_thaumium",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> PLATE_VOID = ITEMS.register("plate_void",
            ItemMaterial::rare);

    // ==================== Clusters (Raw Ores) ====================

    public static final DeferredHolder<Item, Item> CLUSTER_IRON = ITEMS.register("cluster_iron",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> CLUSTER_GOLD = ITEMS.register("cluster_gold",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> CLUSTER_COPPER = ITEMS.register("cluster_copper",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> CLUSTER_CINNABAR = ITEMS.register("cluster_cinnabar",
            ItemMaterial::basic);

    // ==================== Golem Materials ====================

    public static final DeferredHolder<Item, Item> BRAIN_NORMAL = ITEMS.register("brain_normal",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> BRAIN_CLOCKWORK = ITEMS.register("brain_clockwork",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> BRAIN_CURIOUS = ITEMS.register("brain_curious",
            ItemMaterial::uncommon);

    // ==================== Research Notes ====================

    public static final DeferredHolder<Item, Item> RESEARCH_NOTES = ITEMS.register("research_notes",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> COMPLETE_NOTES = ITEMS.register("complete_notes",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // ==================== Curiosities ====================

    public static final DeferredHolder<Item, Item> CURIOSITY = ITEMS.register("curiosity",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> PRIMORDIAL_PEARL = ITEMS.register("primordial_pearl",
            ItemPrimordialPearl::new);

    public static final DeferredHolder<Item, Item> TAINT_SLIME = ITEMS.register("taint_slime",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> TAINT_TENDRIL = ITEMS.register("taint_tendril",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> ZOMBIE_BRAIN = ITEMS.register("zombie_brain",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> FLUX_CRYSTAL = ITEMS.register("flux_crystal",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> VOID_SEED = ITEMS.register("void_seed",
            () -> new ItemMaterial(new Item.Properties().rarity(Rarity.RARE)));

    // ==================== Food ====================

    // Note: Foods need FoodProperties - simplified for now
    public static final DeferredHolder<Item, Item> TRIPLE_MEAT_TREAT = ITEMS.register("triple_meat_treat",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(8)
                            .saturationMod(1.0f)
                            .meat()
                            .build())));

    public static final DeferredHolder<Item, Item> CHUNKS_BEEF = ITEMS.register("chunks_beef",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(4)
                            .saturationMod(0.5f)
                            .meat()
                            .build())));

    public static final DeferredHolder<Item, Item> CHUNKS_CHICKEN = ITEMS.register("chunks_chicken",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(4)
                            .saturationMod(0.5f)
                            .meat()
                            .build())));

    public static final DeferredHolder<Item, Item> CHUNKS_PORK = ITEMS.register("chunks_pork",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(4)
                            .saturationMod(0.5f)
                            .meat()
                            .build())));

    public static final DeferredHolder<Item, Item> CHUNKS_FISH = ITEMS.register("chunks_fish",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(4)
                            .saturationMod(0.4f)
                            .build())));

    // ==================== Armor - Goggles ====================

    public static final DeferredHolder<Item, Item> GOGGLES = ITEMS.register("goggles",
            ItemGoggles::new);

    // ==================== Armor - Thaumaturge Robes ====================

    public static final DeferredHolder<Item, Item> CLOTH_CHEST = ITEMS.register("cloth_chest",
            ItemRobeArmor::createChest);

    public static final DeferredHolder<Item, Item> CLOTH_LEGS = ITEMS.register("cloth_legs",
            ItemRobeArmor::createLegs);

    public static final DeferredHolder<Item, Item> CLOTH_BOOTS = ITEMS.register("cloth_boots",
            ItemRobeArmor::createBoots);

    // ==================== Tools - Thaumium ====================

    public static final DeferredHolder<Item, Item> THAUMIUM_SWORD = ITEMS.register("thaumium_sword",
            ItemThaumiumSword::new);

    public static final DeferredHolder<Item, Item> THAUMIUM_PICK = ITEMS.register("thaumium_pick",
            ItemThaumiumPickaxe::new);

    public static final DeferredHolder<Item, Item> THAUMIUM_AXE = ITEMS.register("thaumium_axe",
            ItemThaumiumAxe::new);

    public static final DeferredHolder<Item, Item> THAUMIUM_SHOVEL = ITEMS.register("thaumium_shovel",
            ItemThaumiumShovel::new);

    public static final DeferredHolder<Item, Item> THAUMIUM_HOE = ITEMS.register("thaumium_hoe",
            ItemThaumiumHoe::new);

    // ==================== Tools - Void Metal ====================

    public static final DeferredHolder<Item, Item> VOID_SWORD = ITEMS.register("void_sword",
            ItemVoidSword::new);

    public static final DeferredHolder<Item, Item> VOID_PICK = ITEMS.register("void_pick",
            ItemVoidPickaxe::new);

    public static final DeferredHolder<Item, Item> VOID_AXE = ITEMS.register("void_axe",
            ItemVoidAxe::new);

    public static final DeferredHolder<Item, Item> VOID_SHOVEL = ITEMS.register("void_shovel",
            ItemVoidShovel::new);

    public static final DeferredHolder<Item, Item> VOID_HOE = ITEMS.register("void_hoe",
            ItemVoidHoe::new);

    // ==================== Armor - Thaumium ====================

    public static final DeferredHolder<Item, Item> THAUMIUM_HELM = ITEMS.register("thaumium_helm",
            ItemThaumiumArmor::createHelmet);

    public static final DeferredHolder<Item, Item> THAUMIUM_CHEST = ITEMS.register("thaumium_chest",
            ItemThaumiumArmor::createChestplate);

    public static final DeferredHolder<Item, Item> THAUMIUM_LEGS = ITEMS.register("thaumium_legs",
            ItemThaumiumArmor::createLeggings);

    public static final DeferredHolder<Item, Item> THAUMIUM_BOOTS = ITEMS.register("thaumium_boots",
            ItemThaumiumArmor::createBoots);

    // ==================== Armor - Void Metal ====================

    public static final DeferredHolder<Item, Item> VOID_HELM = ITEMS.register("void_helm",
            ItemVoidArmor::createHelmet);

    public static final DeferredHolder<Item, Item> VOID_CHEST = ITEMS.register("void_chest",
            ItemVoidArmor::createChestplate);

    public static final DeferredHolder<Item, Item> VOID_LEGS = ITEMS.register("void_legs",
            ItemVoidArmor::createLeggings);

    public static final DeferredHolder<Item, Item> VOID_BOOTS = ITEMS.register("void_boots",
            ItemVoidArmor::createBoots);

    // ==================== Caster Gauntlets ====================

    public static final DeferredHolder<Item, Item> CASTER_BASIC = ITEMS.register("caster_basic",
            ItemCaster::createBasic);

    public static final DeferredHolder<Item, Item> CASTER_ADVANCED = ITEMS.register("caster_advanced",
            ItemCaster::createAdvanced);

    public static final DeferredHolder<Item, Item> CASTER_MASTER = ITEMS.register("caster_master",
            ItemCaster::createMaster);

    // ==================== Focus Items ====================

    public static final DeferredHolder<Item, Item> FOCUS_BLANK = ITEMS.register("focus_blank",
            ItemFocus::createBlank);

    public static final DeferredHolder<Item, Item> FOCUS_ADVANCED = ITEMS.register("focus_advanced",
            ItemFocus::createAdvanced);

    // ==================== Special Armor ====================

    public static final DeferredHolder<Item, Item> TRAVELLER_BOOTS = ITEMS.register("traveller_boots",
            ItemBootsTraveller::new);

    // ==================== Fortress Armor ====================

    public static final DeferredHolder<Item, Item> FORTRESS_HELM = ITEMS.register("fortress_helm",
            ItemFortressArmor::createHelmet);

    public static final DeferredHolder<Item, Item> FORTRESS_CHEST = ITEMS.register("fortress_chest",
            ItemFortressArmor::createChestplate);

    public static final DeferredHolder<Item, Item> FORTRESS_LEGS = ITEMS.register("fortress_legs",
            ItemFortressArmor::createLeggings);

    // ==================== Void Robe Armor ====================

    public static final DeferredHolder<Item, Item> VOID_ROBE_HELM = ITEMS.register("void_robe_helm",
            ItemVoidRobeArmor::createHelmet);

    public static final DeferredHolder<Item, Item> VOID_ROBE_CHEST = ITEMS.register("void_robe_chest",
            ItemVoidRobeArmor::createChestplate);

    public static final DeferredHolder<Item, Item> VOID_ROBE_LEGS = ITEMS.register("void_robe_legs",
            ItemVoidRobeArmor::createLeggings);

    // ==================== Crimson Cult Robe Armor ====================

    public static final DeferredHolder<Item, Item> CRIMSON_ROBE_HELM = ITEMS.register("crimson_robe_helm",
            ItemCultistRobeArmor::createHelmet);

    public static final DeferredHolder<Item, Item> CRIMSON_ROBE_CHEST = ITEMS.register("crimson_robe_chest",
            ItemCultistRobeArmor::createChestplate);

    public static final DeferredHolder<Item, Item> CRIMSON_ROBE_LEGS = ITEMS.register("crimson_robe_legs",
            ItemCultistRobeArmor::createLeggings);

    // ==================== Crimson Cult Plate Armor ====================

    public static final DeferredHolder<Item, Item> CRIMSON_PLATE_HELM = ITEMS.register("crimson_plate_helm",
            ItemCultistPlateArmor::createHelmet);

    public static final DeferredHolder<Item, Item> CRIMSON_PLATE_CHEST = ITEMS.register("crimson_plate_chest",
            ItemCultistPlateArmor::createChestplate);

    public static final DeferredHolder<Item, Item> CRIMSON_PLATE_LEGS = ITEMS.register("crimson_plate_legs",
            ItemCultistPlateArmor::createLeggings);

    // ==================== Crimson Cult Boots (shared) ====================

    public static final DeferredHolder<Item, Item> CRIMSON_BOOTS = ITEMS.register("crimson_boots",
            ItemCultistBoots::new);

    // ==================== Crimson Praetor Armor (Cultist Leader) ====================

    public static final DeferredHolder<Item, Item> CRIMSON_PRAETOR_HELM = ITEMS.register("crimson_praetor_helm",
            ItemCultistLeaderArmor::createHelmet);

    public static final DeferredHolder<Item, Item> CRIMSON_PRAETOR_CHEST = ITEMS.register("crimson_praetor_chest",
            ItemCultistLeaderArmor::createChestplate);

    public static final DeferredHolder<Item, Item> CRIMSON_PRAETOR_LEGS = ITEMS.register("crimson_praetor_legs",
            ItemCultistLeaderArmor::createLeggings);

    // ==================== Baubles / Curios ====================

    public static final DeferredHolder<Item, Item> AMULET_VIS_FOUND = ITEMS.register("amulet_vis_found",
            ItemAmuletVis::createFound);

    public static final DeferredHolder<Item, Item> AMULET_VIS_CRAFTED = ITEMS.register("amulet_vis_crafted",
            ItemAmuletVis::createCrafted);

    public static final DeferredHolder<Item, Item> CLOUD_RING = ITEMS.register("cloud_ring",
            ItemCloudRing::new);

    public static final DeferredHolder<Item, Item> CURIOSITY_BAND = ITEMS.register("curiosity_band",
            ItemCuriosityBand::new);

    public static final DeferredHolder<Item, Item> CHARM_UNDYING = ITEMS.register("charm_undying",
            ItemCharmUndying::new);

    // Verdant Charms (3 variants)
    public static final DeferredHolder<Item, Item> VERDANT_CHARM = ITEMS.register("verdant_charm",
            ItemVerdantCharm::createBasic);

    public static final DeferredHolder<Item, Item> VERDANT_CHARM_LIFE = ITEMS.register("verdant_charm_life",
            ItemVerdantCharm::createLife);

    public static final DeferredHolder<Item, Item> VERDANT_CHARM_SUSTAIN = ITEMS.register("verdant_charm_sustain",
            ItemVerdantCharm::createSustain);

    public static final DeferredHolder<Item, Item> VOIDSEER_CHARM = ITEMS.register("voidseer_charm",
            ItemVoidseerCharm::new);

    // ==================== Elemental Tools ====================

    public static final DeferredHolder<Item, Item> ELEMENTAL_PICK = ITEMS.register("elemental_pick",
            ItemElementalPickaxe::new);

    public static final DeferredHolder<Item, Item> ELEMENTAL_AXE = ITEMS.register("elemental_axe",
            ItemElementalAxe::new);

    public static final DeferredHolder<Item, Item> ELEMENTAL_SWORD = ITEMS.register("elemental_sword",
            ItemElementalSword::new);

    public static final DeferredHolder<Item, Item> ELEMENTAL_SHOVEL = ITEMS.register("elemental_shovel",
            ItemElementalShovel::new);

    // ==================== Special Tools ====================

    public static final DeferredHolder<Item, Item> PRIMAL_CRUSHER = ITEMS.register("primal_crusher",
            ItemPrimalCrusher::new);

    public static final DeferredHolder<Item, Item> CRIMSON_BLADE = ITEMS.register("crimson_blade",
            ItemCrimsonBlade::new);

    public static final DeferredHolder<Item, Item> ELEMENTAL_HOE = ITEMS.register("elemental_hoe",
            ItemElementalHoe::new);

    // ==================== Utility Tools ====================

    public static final DeferredHolder<Item, Item> RESONATOR = ITEMS.register("resonator",
            ItemResonator::new);

    public static final DeferredHolder<Item, Item> SANITY_CHECKER = ITEMS.register("sanity_checker",
            ItemSanityChecker::new);

    public static final DeferredHolder<Item, Item> HAND_MIRROR = ITEMS.register("hand_mirror",
            ItemHandMirror::new);

    public static final DeferredHolder<Item, Item> GRAPPLE_GUN = ITEMS.register("grapple_gun",
            ItemGrappleGun::new);

    // ==================== Consumables ====================

    public static final DeferredHolder<Item, Item> BATH_SALTS = ITEMS.register("bath_salts",
            ItemBathSalts::new);

    public static final DeferredHolder<Item, Item> SANITY_SOAP = ITEMS.register("sanity_soap",
            ItemSanitySoap::new);

    public static final DeferredHolder<Item, Item> BOTTLE_TAINT = ITEMS.register("bottle_taint",
            ItemBottleTaint::new);

    public static final DeferredHolder<Item, Item> CAUSALITY_COLLAPSER = ITEMS.register("causality_collapser",
            ItemCausalityCollapser::new);

    public static final DeferredHolder<Item, Item> LABEL_BLANK = ITEMS.register("label_blank",
            ItemLabel::createBlank);

    public static final DeferredHolder<Item, Item> LABEL_FILLED = ITEMS.register("label_filled",
            ItemLabel::createFilled);

    // ==================== Loot Bags ====================

    public static final DeferredHolder<Item, Item> LOOT_BAG_COMMON = ITEMS.register("loot_bag_common",
            ItemLootBag::createCommon);

    public static final DeferredHolder<Item, Item> LOOT_BAG_UNCOMMON = ITEMS.register("loot_bag_uncommon",
            ItemLootBag::createUncommon);

    public static final DeferredHolder<Item, Item> LOOT_BAG_RARE = ITEMS.register("loot_bag_rare",
            ItemLootBag::createRare);

    // ==================== Curios ====================

    public static final DeferredHolder<Item, Item> PECH_WAND = ITEMS.register("pech_wand",
            ItemPechWand::new);

    // ==================== Celestial Notes ====================

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_SUN = ITEMS.register("celestial_notes_sun",
            ItemCelestialNotes::createSun);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_STARS_1 = ITEMS.register("celestial_notes_stars_1",
            ItemCelestialNotes::createStars1);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_STARS_2 = ITEMS.register("celestial_notes_stars_2",
            ItemCelestialNotes::createStars2);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_STARS_3 = ITEMS.register("celestial_notes_stars_3",
            ItemCelestialNotes::createStars3);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_STARS_4 = ITEMS.register("celestial_notes_stars_4",
            ItemCelestialNotes::createStars4);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_1 = ITEMS.register("celestial_notes_moon_1",
            ItemCelestialNotes::createMoon1);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_2 = ITEMS.register("celestial_notes_moon_2",
            ItemCelestialNotes::createMoon2);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_3 = ITEMS.register("celestial_notes_moon_3",
            ItemCelestialNotes::createMoon3);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_4 = ITEMS.register("celestial_notes_moon_4",
            ItemCelestialNotes::createMoon4);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_5 = ITEMS.register("celestial_notes_moon_5",
            ItemCelestialNotes::createMoon5);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_6 = ITEMS.register("celestial_notes_moon_6",
            ItemCelestialNotes::createMoon6);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_7 = ITEMS.register("celestial_notes_moon_7",
            ItemCelestialNotes::createMoon7);

    public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_8 = ITEMS.register("celestial_notes_moon_8",
            ItemCelestialNotes::createMoon8);

    // ==================== Focus Accessories ====================

    public static final DeferredHolder<Item, Item> FOCUS_POUCH = ITEMS.register("focus_pouch",
            ItemFocusPouch::new);

    // ==================== Golem Items ====================

    public static final DeferredHolder<Item, Item> GOLEM_PLACER = ITEMS.register("golem_placer",
            ItemGolemPlacer::new);

    public static final DeferredHolder<Item, Item> GOLEM_BELL = ITEMS.register("golem_bell",
            ItemGolemBell::new);

    // ==================== Golem Seals ====================

    public static final DeferredHolder<Item, Item> SEAL_BLANK = ITEMS.register("seal_blank",
            ItemSealPlacer::createBlank);

    // Seal types - one item per seal
    public static final DeferredHolder<Item, Item> SEAL_PICKUP = ITEMS.register("seal_pickup",
            () -> ItemSealPlacer.create("thaumcraft:pickup"));

    public static final DeferredHolder<Item, Item> SEAL_EMPTY = ITEMS.register("seal_empty",
            () -> ItemSealPlacer.create("thaumcraft:empty"));

    public static final DeferredHolder<Item, Item> SEAL_FILL = ITEMS.register("seal_fill",
            () -> ItemSealPlacer.create("thaumcraft:fill"));

    public static final DeferredHolder<Item, Item> SEAL_GUARD = ITEMS.register("seal_guard",
            () -> ItemSealPlacer.create("thaumcraft:guard"));

    public static final DeferredHolder<Item, Item> SEAL_BUTCHER = ITEMS.register("seal_butcher",
            () -> ItemSealPlacer.create("thaumcraft:butcher"));

    public static final DeferredHolder<Item, Item> SEAL_HARVEST = ITEMS.register("seal_harvest",
            () -> ItemSealPlacer.create("thaumcraft:harvest"));

    public static final DeferredHolder<Item, Item> SEAL_LUMBER = ITEMS.register("seal_lumber",
            () -> ItemSealPlacer.create("thaumcraft:lumber"));

    public static final DeferredHolder<Item, Item> SEAL_BREAKER = ITEMS.register("seal_breaker",
            () -> ItemSealPlacer.create("thaumcraft:breaker"));

    public static final DeferredHolder<Item, Item> SEAL_PROVIDE = ITEMS.register("seal_provide",
            () -> ItemSealPlacer.create("thaumcraft:provide"));

    public static final DeferredHolder<Item, Item> SEAL_STOCK = ITEMS.register("seal_stock",
            () -> ItemSealPlacer.create("thaumcraft:stock"));

    public static final DeferredHolder<Item, Item> SEAL_USE = ITEMS.register("seal_use",
            () -> ItemSealPlacer.create("thaumcraft:use"));

    public static final DeferredHolder<Item, Item> SEAL_BREAKER_ADVANCED = ITEMS.register("seal_breaker_advanced",
            () -> ItemSealPlacer.create("thaumcraft:breaker_advanced"));

    public static final DeferredHolder<Item, Item> SEAL_PICKUP_ADVANCED = ITEMS.register("seal_pickup_advanced",
            () -> ItemSealPlacer.create("thaumcraft:pickup_advanced"));

    // ==================== Crystal Essence ====================

    public static final DeferredHolder<Item, Item> CRYSTAL_ESSENCE = ITEMS.register("crystal_essence",
            ItemCrystalEssence::new);

    // ==================== Turret Placers ====================

    public static final DeferredHolder<Item, Item> TURRET_PLACER_BASIC = ITEMS.register("turret_placer_basic",
            ItemTurretPlacer::createBasic);

    public static final DeferredHolder<Item, Item> TURRET_PLACER_ADVANCED = ITEMS.register("turret_placer_advanced",
            ItemTurretPlacer::createAdvanced);

    public static final DeferredHolder<Item, Item> TURRET_PLACER_BORE = ITEMS.register("turret_placer_bore",
            ItemTurretPlacer::createBore);

    // ==================== Grapple Gun Components ====================

    public static final DeferredHolder<Item, Item> GRAPPLE_GUN_SPOOL = ITEMS.register("grapple_gun_spool",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> GRAPPLE_GUN_TIP = ITEMS.register("grapple_gun_tip",
            ItemMaterial::uncommon);

    // ==================== Golem Modules ====================

    public static final DeferredHolder<Item, Item> GOLEM_MODULE_AGGRESSION = ITEMS.register("golem_module_aggression",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> GOLEM_MODULE_VISION = ITEMS.register("golem_module_vision",
            ItemMaterial::uncommon);

    // ==================== Creative-Only Items ====================

    public static final DeferredHolder<Item, Item> CREATIVE_FLUX_SPONGE = ITEMS.register("creative_flux_sponge",
            () -> new ItemCreativeFluxSponge(new Item.Properties()));

    public static final DeferredHolder<Item, Item> CREATIVE_PLACER = ITEMS.register("creative_placer",
            () -> new ItemCreativePlacer(new Item.Properties()));
}
