package thaumcraft.init;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.common.lib.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.lib.crafting.InfusionEnchantmentRecipeSerializer;
import thaumcraft.common.lib.crafting.InfusionRecipeType;
import thaumcraft.common.lib.crafting.ShapedArcaneRecipe;
import thaumcraft.common.lib.crafting.ShapelessArcaneRecipe;
import net.minecraft.core.registries.Registries;

/**
 * Registry for Thaumcraft recipe serializers.
 * 
 * Each recipe type needs a serializer to handle JSON parsing and network sync.
 */
public class ModRecipeSerializers {
    
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = 
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Thaumcraft.MODID);
    
    /**
     * Serializer for shaped arcane workbench recipes.
     */
    public static final DeferredHolder<RecipeSerializer<ShapedArcaneRecipe>, RecipeSerializer<ShapedArcaneRecipe>> ARCANE_WORKBENCH_SHAPED = 
            RECIPE_SERIALIZERS.register("arcane_workbench_shaped", () -> ShapedArcaneRecipe.Serializer.INSTANCE);
    
    /**
     * Serializer for shapeless arcane workbench recipes.
     */
    public static final DeferredHolder<RecipeSerializer<ShapelessArcaneRecipe>, RecipeSerializer<ShapelessArcaneRecipe>> ARCANE_WORKBENCH_SHAPELESS = 
            RECIPE_SERIALIZERS.register("arcane_workbench_shapeless", () -> ShapelessArcaneRecipe.Serializer.INSTANCE);
    
    /**
     * Serializer for crucible alchemy recipes.
     */
    public static final DeferredHolder<RecipeSerializer<CrucibleRecipeType>, RecipeSerializer<CrucibleRecipeType>> CRUCIBLE = 
            RECIPE_SERIALIZERS.register("crucible", () -> CrucibleRecipeType.Serializer.INSTANCE);
    
    /**
     * Serializer for infusion altar recipes.
     */
    public static final DeferredHolder<RecipeSerializer<InfusionRecipeType>, RecipeSerializer<InfusionRecipeType>> INFUSION = 
            RECIPE_SERIALIZERS.register("infusion", () -> InfusionRecipeType.Serializer.INSTANCE);
            
    /**
     * Serializer for infusion enchantment recipes.
     */
    public static final DeferredHolder<RecipeSerializer<InfusionEnchantmentRecipe>, RecipeSerializer<InfusionEnchantmentRecipe>> INFUSION_ENCHANTMENT = 
            RECIPE_SERIALIZERS.register("infusion_enchantment", () -> InfusionEnchantmentRecipeSerializer.INSTANCE);
}
