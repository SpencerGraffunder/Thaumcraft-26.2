package thaumcraft.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import thaumcraft.Thaumcraft;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.common.lib.crafting.InfusionRecipeType;
import thaumcraft.compat.jei.arcane.ArcaneWorkbenchCategory;
import thaumcraft.compat.jei.crucible.CrucibleCategory;
import thaumcraft.compat.jei.infusion.InfusionCategory;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModRecipeTypes;

import java.util.List;

/**
 * JEI Plugin for Thaumcraft.
 * Registers recipe categories for:
 * - Arcane Workbench (shaped and shapeless)
 * - Crucible
 * - Infusion Altar
 */
@JeiPlugin
public class ThaumcraftJEIPlugin implements IModPlugin {

    public static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "jei_plugin");

    // Recipe types for JEI
    public static final RecipeType<IArcaneRecipe> ARCANE_TYPE =
            RecipeType.create(Thaumcraft.MODID, "arcane_workbench", IArcaneRecipe.class);

    public static final RecipeType<CrucibleRecipeType> CRUCIBLE_TYPE =
            RecipeType.create(Thaumcraft.MODID, "crucible", CrucibleRecipeType.class);

    public static final RecipeType<InfusionRecipeType> INFUSION_TYPE =
            RecipeType.create(Thaumcraft.MODID, "infusion", InfusionRecipeType.class);

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers jeiHelpers = registration.getJeiHelpers();
        IGuiHelper guiHelper = jeiHelpers.getGuiHelper();

        // Register Arcane Workbench category (handles both shaped and shapeless)
        registration.addRecipeCategories(new ArcaneWorkbenchCategory(guiHelper));

        // Register Crucible category
        registration.addRecipeCategories(new CrucibleCategory(guiHelper));

        // Register Infusion Altar category
        registration.addRecipeCategories(new InfusionCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getServer() == null) {
            return;
        }
        RecipeManager recipeManager = mc.level.getServer().getRecipeManager();

        // Get all arcane recipes (both shaped and shapeless use same recipe type)
        List<IArcaneRecipe> arcaneRecipes = recipeManager.recipeMap().byType(ModRecipeTypes.ARCANE_WORKBENCH.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(r -> r instanceof IArcaneRecipe)
                .map(r -> (IArcaneRecipe) r)
                .toList();

        registration.addRecipes(ARCANE_TYPE, arcaneRecipes);

        // Get all crucible recipes
        List<CrucibleRecipeType> crucibleRecipes = recipeManager.recipeMap().byType(ModRecipeTypes.CRUCIBLE.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(r -> r instanceof CrucibleRecipeType)
                .map(r -> (CrucibleRecipeType) r)
                .toList();

        registration.addRecipes(CRUCIBLE_TYPE, crucibleRecipes);

        // Get all infusion recipes
        List<InfusionRecipeType> infusionRecipes = recipeManager.recipeMap().byType(ModRecipeTypes.INFUSION.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(r -> r instanceof InfusionRecipeType)
                .map(r -> (InfusionRecipeType) r)
                .toList();

        registration.addRecipes(INFUSION_TYPE, infusionRecipes);

        Thaumcraft.LOGGER.info("JEI: Registered {} arcane, {} crucible, {} infusion recipes",
                arcaneRecipes.size(), crucibleRecipes.size(), infusionRecipes.size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Arcane Workbench is the catalyst for arcane recipes
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ARCANE_WORKBENCH.get()), ARCANE_TYPE);

        // Crucible is the catalyst for crucible recipes
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUCIBLE.get()), CRUCIBLE_TYPE);

        // Infusion Matrix is the catalyst for infusion recipes
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INFUSION_MATRIX.get()), INFUSION_TYPE);
    }
}
