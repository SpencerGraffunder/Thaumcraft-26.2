package thaumcraft.api.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import thaumcraft.api.aspects.AspectList;

/**
 * Interface for arcane workbench recipes.
 * These recipes consume vis from the aura and require specific crystals.
 */
public interface IArcaneRecipe extends Recipe<IArcaneWorkbench>, IThaumcraftRecipe {

    /**
     * Gets the result of this recipe.
     * @return the resulting ItemStack
     */
    ItemStack getResultItem();

    /**
     * Gets the vis cost for this recipe.
     * @return the amount of vis required
     */
    int getVis();

    /**
     * Gets the research key required to unlock this recipe.
     * @return the research key
     */
    @Override
    String getResearch();

    /**
     * Gets the crystals required for this recipe.
     * The AspectList contains the primal aspects and their required amounts.
     * @return the crystal requirements
     */
    AspectList getCrystals();
}
