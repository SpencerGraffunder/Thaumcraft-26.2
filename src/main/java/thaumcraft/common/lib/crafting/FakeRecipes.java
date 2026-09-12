package thaumcraft.common.lib.crafting;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.FakeRecipe;
import thaumcraft.api.crafting.Part;
import thaumcraft.init.ModItems;

/**
 * Registers display-only recipes for the Thaumonomicon: multiblock structures
 * that have no recipe backing and are not covered by
 * {@link thaumcraft.common.config.ConfigRecipes} (which owns the infusion
 * enchantment + runic shielding fakes).
 *
 * <p>The Infusion Altar variants are {@link ThaumcraftApi.BluePrint} multiblocks
 * (8 stone corners + a pedestal + the runic matrix), matching the 1.12
 * {@code ThaumcraftApi.BluePrint} that the old book rendered in 3D. The
 * remaining entries are simple {@link FakeRecipe} display recipes.
 */
public class FakeRecipes {

    public static void register() {
        // Infusion Altar multiblocks (display blueprints for the book)
        ThaumcraftApi.addMultiblockRecipeToCatalog(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "infusion_altar"),
                infusionAltar("INFUSION",
                        thaumcraft.init.ModBlocks.ARCANE_STONE.get(),
                        thaumcraft.init.ModBlocks.PEDESTAL_ARCANE.get()));
        ThaumcraftApi.addMultiblockRecipeToCatalog(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "infusion_altar_ancient"),
                infusionAltar("INFUSIONANCIENT",
                        thaumcraft.init.ModBlocks.ANCIENT_STONE.get(),
                        thaumcraft.init.ModBlocks.PEDESTAL_ANCIENT.get()));
        ThaumcraftApi.addMultiblockRecipeToCatalog(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "infusion_altar_eldritch"),
                infusionAltar("INFUSIONELDRITCH",
                        thaumcraft.init.ModBlocks.ELDRITCH_STONE_TILE.get(),
                        thaumcraft.init.ModBlocks.PEDESTAL_ELDRITCH.get()));

        // Simple display-only recipes
        ThaumcraftApi.addFakeCraftingRecipe(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "thaumatorium"),
                new FakeRecipe("ALCHEMY", "Thaumatorium",
                        new ItemStack(thaumcraft.init.ModBlocks.THAUMATORIUM.get())));
        ThaumcraftApi.addFakeCraftingRecipe(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "golem_press"),
                new FakeRecipe("GOLEMANCY", "Golem Press",
                        new ItemStack(ModItems.GOLEM_PLACER.get())));

        Thaumcraft.LOGGER.info("Registered 5 multiblock/display book recipes");
    }

    /**
     * Build the Infusion Altar blueprint (3×3×3): runic matrix on top-center,
     * 8 stone blocks at the four corners of the bottom two layers, and the
     * pedestal at the bottom-center. Mirrors the 1.12
     * {@code infusionAltarNormalBlueprint}.
     */
    private static ThaumcraftApi.BluePrint infusionAltar(String research, net.minecraft.world.level.block.Block stone,
            net.minecraft.world.level.block.Block pedestal) {
        Part matrix = new Part(thaumcraft.init.ModBlocks.INFUSION_MATRIX.get(), null);
        Part st = new Part(stone, null);
        Part ped = new Part(pedestal, null);

        // blueprint[y][x][z]; y=0 bottom, y=2 top
        Part[][][] parts = {
                { { st, null, st }, { null, ped, null }, { st, null, st } }, // bottom
                { { st, null, st }, { null, null, null }, { st, null, st } }, // middle
                { { null, null, null }, { null, matrix, null }, { null, null, null } } // top
        };

        ItemStack display = new ItemStack(thaumcraft.init.ModBlocks.INFUSION_MATRIX.get());
        ItemStack[] ingredients = {
                new ItemStack(stone, 8),
                new ItemStack(pedestal),
                new ItemStack(thaumcraft.init.ModBlocks.INFUSION_MATRIX.get())
        };
        return new ThaumcraftApi.BluePrint(research, display, parts, ingredients);
    }
}
