package thaumcraft.common.lib.crafting;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.FakeRecipe;
import thaumcraft.init.ModItems;

/**
 * Registers display-only "fake" recipes for the Thaumonomicon: multiblock
 * structures that have no recipe backing and are not covered by
 * {@link thaumcraft.common.config.ConfigRecipes} (which owns the infusion
 * enchantment + runic shielding fakes).
 */
public class FakeRecipes {

    public static void register() {
        // Multiblock structures (display recipes for the book)
        ThaumcraftApi.addFakeCraftingRecipe(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "infusion_altar"),
                new FakeRecipe("INFUSION", "Infusion Altar",
                        new ItemStack(thaumcraft.init.ModBlocks.INFUSION_MATRIX.get())));
        ThaumcraftApi.addFakeCraftingRecipe(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "infusion_altar_ancient"),
                new FakeRecipe("INFUSION", "Infusion Altar (Ancient)",
                        new ItemStack(thaumcraft.init.ModBlocks.INFUSION_MATRIX.get())));
        ThaumcraftApi.addFakeCraftingRecipe(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "infusion_altar_eldritch"),
                new FakeRecipe("INFUSION", "Infusion Altar (Eldritch)",
                        new ItemStack(thaumcraft.init.ModBlocks.INFUSION_MATRIX.get())));
        ThaumcraftApi.addFakeCraftingRecipe(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "thaumatorium"),
                new FakeRecipe("ALCHEMY", "Thaumatorium",
                        new ItemStack(thaumcraft.init.ModBlocks.THAUMATORIUM.get())));
        ThaumcraftApi.addFakeCraftingRecipe(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "golem_press"),
                new FakeRecipe("GOLEMANCY", "Golem Press",
                        new ItemStack(ModItems.GOLEM_PLACER.get())));

        Thaumcraft.LOGGER.info("Registered {} fake book recipes (multiblocks)", 5);
    }
}
