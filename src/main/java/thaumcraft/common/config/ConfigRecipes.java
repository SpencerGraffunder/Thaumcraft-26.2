package thaumcraft.common.config;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.crafting.InfusionRecipeType;
import thaumcraft.init.ModItems;

/**
 * Display-only ("fake") recipe catalog for the thaumonomicon.
 * Ported from the 1.20.1 fork's ConfigRecipes: these entries are not craftable,
 * they only back book pages that show dynamic recipe types (infusion enchantment).
 *
 * Call convention: addFake(id, baseItem, extraItem, [extraItem, ...], instability, aspect, amount, ...).
 */
public class ConfigRecipes {

    public static void init() {
        // IE infusion enchantment fakes (base tool + enchanted book + key item)
        addFake("ie_burrowing_fake", Items.WOODEN_PICKAXE, Items.ENCHANTED_BOOK, Items.RABBIT_FOOT, 8,
                Aspect.SENSES, 80, Aspect.EARTH, 150);
        addFake("ie_collector_fake", Items.STONE_AXE, Items.ENCHANTED_BOOK, Items.LEAD, 10,
                Aspect.DESIRE, 80, Aspect.WATER, 100);
        addFake("ie_destructive_fake", Items.STONE_PICKAXE, Items.ENCHANTED_BOOK, Items.TNT, 2,
                Aspect.AVERSION, 200, Aspect.ENTROPY, 250);
        addFake("ie_refining_fake", Items.IRON_PICKAXE, Items.ENCHANTED_BOOK, ModItems.SALIS_MUNDUS.get(), 8,
                Aspect.ORDER, 80, Aspect.EXCHANGE, 60);
        addFake("ie_sounding_fake", Items.GOLDEN_PICKAXE, Items.ENCHANTED_BOOK, Items.MAP, 3,
                Aspect.SENSES, 40, Aspect.FIRE, 60);
        addFake("ie_arcing_fake", Items.WOODEN_SWORD, Items.ENCHANTED_BOOK, Items.REDSTONE_BLOCK, 3,
                Aspect.ENERGY, 40, Aspect.AIR, 60);
        addFake("ie_essence_fake", Items.STONE_SWORD, Items.ENCHANTED_BOOK, ModItems.CRYSTAL_ESSENCE.get(), 3,
                Aspect.BEAST, 40, Aspect.DEATH, 60);
        addFake("ie_lamplight_fake", Items.GOLDEN_PICKAXE, Items.ENCHANTED_BOOK, ModItems.NITOR.get(), 3,
                Aspect.LIGHT, 80, Aspect.AIR, 20);

        // Runic shielding fakes: enchanting armor pieces, each application costs more
        addFake("runic_armor_fake_0", Items.IRON_HELMET, ModItems.CRYSTAL_ESSENCE.get(), 0,
                Aspect.PROTECT, 15);
        addFake("runic_armor_fake_1", Items.IRON_CHESTPLATE, ModItems.CRYSTAL_ESSENCE.get(), 0,
                Aspect.PROTECT, 30);
        addFake("runic_armor_fake_2", Items.IRON_LEGGINGS, ModItems.CRYSTAL_ESSENCE.get(), 0,
                Aspect.PROTECT, 45);
    }
    @SafeVarargs
    private static void addFake(String recipeID, Item baseItem, Object... rest) {
        // rest = extra items..., instability, (aspect, amount)*
        int i = 0;
        java.util.List<Item> items = new java.util.ArrayList<>();
        items.add(baseItem);
        while (i < rest.length && rest[i] instanceof Item) {
            items.add((Item) rest[i]);
            i++;
        }
        int instability = (Integer) rest[i++];
        AspectList aspects = new AspectList();
        for (; i < rest.length - 1; i += 2) {
            aspects.add((Aspect) rest[i], (Integer) rest[i + 1]);
        }
        register(recipeID, items, instability, aspects);
    }

    private static void register(String recipeID, java.util.List<Item> items, int instability, AspectList aspects) {
        ItemStack centralStack = new ItemStack(items.get(0));
        java.util.List<Ingredient> components = new java.util.ArrayList<>();
        for (int i = 1; i < items.size(); i++) components.add(Ingredient.of(items.get(i)));

        InfusionRecipeType recipe = new InfusionRecipeType(
                "fake", Ingredient.of(items.get(0)), components, aspects,
                ItemStackTemplate.fromStack(centralStack), "", instability);

        ThaumcraftApi.addFakeCraftingRecipe(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, recipeID), recipe);
    }
}
