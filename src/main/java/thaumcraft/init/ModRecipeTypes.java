package thaumcraft.init;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.common.lib.crafting.InfusionRecipeType;

/**
 * Registry for Thaumcraft custom recipe types.
 * 
 * Recipe types:
 * - ARCANE_WORKBENCH: Recipes that require vis and crystals
 * - CRUCIBLE: Alchemy recipes that require aspects in a crucible
 * - INFUSION: Infusion altar recipes with pedestal items and essentia
 */
public class ModRecipeTypes {
    
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = 
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Thaumcraft.MODID);
    
    /**
     * Arcane Workbench recipe type.
     * Used for crafting items that require vis from the aura and primal crystals.
     */
    public static final DeferredHolder<RecipeType<IArcaneRecipe>, RecipeType<IArcaneRecipe>> ARCANE_WORKBENCH = 
            RECIPE_TYPES.register("arcane_workbench", () -> new RecipeType<IArcaneRecipe>() {
                @Override
                public String toString() {
                    return Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "arcane_workbench").toString();
                }
            });
    
    /**
     * Crucible recipe type.
     * Used for alchemy transmutation by throwing items into a crucible with aspects.
     */
    public static final DeferredHolder<RecipeType<CrucibleRecipeType>, RecipeType<CrucibleRecipeType>> CRUCIBLE = 
            RECIPE_TYPES.register("crucible", () -> new RecipeType<CrucibleRecipeType>() {
                @Override
                public String toString() {
                    return Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "crucible").toString();
                }
            });
    
    /**
     * Infusion altar recipe type.
     * Used for creating powerful items via the infusion altar with pedestals and essentia.
     */
    public static final DeferredHolder<RecipeType<InfusionRecipeType>, RecipeType<InfusionRecipeType>> INFUSION = 
            RECIPE_TYPES.register("infusion", () -> new RecipeType<InfusionRecipeType>() {
                @Override
                public String toString() {
                    return Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "infusion").toString();
                }
            });
}
