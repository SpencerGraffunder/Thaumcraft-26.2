package thaumcraft.common.lib.crafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IArcaneWorkbench;
import thaumcraft.common.lib.capabilities.ThaumcraftCapabilities;
import thaumcraft.init.ModRecipeTypes;

import java.util.List;
import java.util.Optional;

/**
 * ThaumcraftCraftingManager - Handles recipe lookup for Thaumcraft crafting.
 * 
 * Provides methods to find matching recipes for:
 * - Arcane Workbench (arcane recipes)
 * - Crucible (alchemy recipes)
 * - Infusion Altar (infusion recipes)
 */
public class ThaumcraftCraftingManager {
    
    // Aspect cap for generated tags
    public static final int ASPECT_CAP = 500;
    
    // ==================== Arcane Workbench Recipes ====================
    
    /**
     * Find a matching arcane recipe for the given crafting matrix.
     * 
     * @param matrix The crafting container (3x3 grid + 6 crystal slots)
     * @param player The player crafting
     * @return The matching arcane recipe, or null if none found
     */
    public static IArcaneRecipe findMatchingArcaneRecipe(CraftingContainer matrix, Player player) {
        if (player == null || matrix == null) {
            return null;
        }
        
        // The matrix must implement IArcaneWorkbench for arcane recipes
        if (!(matrix instanceof IArcaneWorkbench workbench)) {
            return null;
        }
        
        Level level = player.level();
        
        // Search through all arcane workbench recipes
        for (RecipeHolder<?> recipe : level.getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.ARCANE_WORKBENCH.get())) {
            if (recipe.value() instanceof IArcaneRecipe arcaneRecipe) {
                // Check if the recipe matches
                if (arcaneRecipe.matches(workbench, level)) {
                    // Check research requirement
                    String research = arcaneRecipe.getResearch();
                    if (research != null && !research.isEmpty()) {
                        if (!ThaumcraftCapabilities.isResearchKnown(player, research)) {
                            continue;
                        }
                    }
                    return arcaneRecipe;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find the result of a matching arcane recipe.
     * 
     * @param matrix The crafting container
     * @param player The player crafting
     * @return The result ItemStack, or empty if no recipe matches
     */
    public static ItemStack findMatchingArcaneRecipeResult(CraftingContainer matrix, Player player) {
        IArcaneRecipe recipe = findMatchingArcaneRecipe(matrix, player);
        if (recipe != null && matrix instanceof IArcaneWorkbench workbench) {
            return recipe.assemble(workbench);
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Get the crystal requirements for a matching arcane recipe.
     * 
     * @param matrix The crafting container
     * @param player The player crafting
     * @return The AspectList of required crystals, or null if no recipe matches
     */
    public static AspectList findMatchingArcaneRecipeCrystals(CraftingContainer matrix, Player player) {
        IArcaneRecipe recipe = findMatchingArcaneRecipe(matrix, player);
        return recipe != null ? recipe.getCrystals() : null;
    }
    
    /**
     * Get the vis cost for a matching arcane recipe.
     * 
     * @param matrix The crafting container
     * @param player The player crafting
     * @return The vis cost, or 0 if no recipe matches
     */
    public static int findMatchingArcaneRecipeVis(CraftingContainer matrix, Player player) {
        IArcaneRecipe recipe = findMatchingArcaneRecipe(matrix, player);
        return recipe != null ? recipe.getVis() : 0;
    }
    
    // ==================== Crucible Recipes ====================
    
    /**
     * Find a matching crucible recipe for the given catalyst and aspects.
     * 
     * @param crucibleAspects The aspects currently in the crucible
     * @param catalyst The item being thrown in
     * @param player The player crafting
     * @param level The world
     * @return The matching crucible recipe, or null if none found
     */
    public static CrucibleRecipeType findMatchingCrucibleRecipe(AspectList crucibleAspects, 
                                                                  ItemStack catalyst, 
                                                                  Player player, 
                                                                  Level level) {
        if (catalyst.isEmpty() || level == null) {
            return null;
        }
        
        for (RecipeHolder<?> recipe : level.getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.CRUCIBLE.get())) {
            if (recipe.value() instanceof CrucibleRecipeType crucibleRecipe) {
                // Check if the recipe matches
                if (crucibleRecipe.matchesCrucible(crucibleAspects, catalyst)) {
                    // Check research requirement
                    String research = crucibleRecipe.getResearch();
                    if (research != null && !research.isEmpty() && player != null) {
                        if (!ThaumcraftCapabilities.isResearchKnown(player, research)) {
                            continue;
                        }
                    }
                    return crucibleRecipe;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find all crucible recipes that use the given catalyst.
     * Useful for displaying possible recipes to the player.
     * 
     * @param catalyst The catalyst item
     * @param level The world
     * @return List of matching recipes
     */
    public static List<CrucibleRecipeType> findCrucibleRecipesForCatalyst(ItemStack catalyst, Level level) {
        return level.getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.CRUCIBLE.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe instanceof CrucibleRecipeType)
                .map(recipe -> (CrucibleRecipeType) recipe)
                .filter(recipe -> recipe.catalystMatches(catalyst))
                .toList();
    }
    
    // ==================== Infusion Recipes ====================
    
    /**
     * Find a matching infusion recipe for the given altar state.
     * 
     * @param pedestalItems Items on the pedestals
     * @param centralItem The item on the runic matrix
     * @param player The player crafting
     * @param level The world
     * @return The matching infusion recipe, or null if none found
     */
    public static InfusionRecipeType findMatchingInfusionRecipe(List<ItemStack> pedestalItems,
                                                                  ItemStack centralItem,
                                                                  Player player,
                                                                  Level level) {
        if (centralItem.isEmpty() || level == null || player == null) {
            return null;
        }
        
        for (RecipeHolder<?> recipe : level.getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.INFUSION.get())) {
            if (recipe.value() instanceof InfusionRecipeType infusionRecipe) {
                if (infusionRecipe.matchesInfusion(pedestalItems, centralItem, level, player)) {
                    return infusionRecipe;
                }
            }
        }
        
        return null;
    }
    
    // ==================== Object Tag Generation ====================
    
    /**
     * Get the aspects of an item.
     * This checks multiple sources in order:
     * 1. Exact match with NBT
     * 2. Match ignoring NBT
     * 3. Wildcard damage values
     * 4. Generated from recipes
     * 
     * @param stack The item stack
     * @return The AspectList for this item, never null
     */
    public static AspectList getObjectTags(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new AspectList();
        }
        
        // First, try direct lookup via AspectHelper
        AspectList aspects = AspectHelper.getObjectAspects(stack);
        
        // If not found, check if it's an essentia container
        if (aspects == null && stack.getItem() instanceof IEssentiaContainerItem container) {
            if (!container.ignoreContainedAspects()) {
                aspects = container.getAspects(stack);
            }
        }
        
        // If still not found, generate from recipes
        if (aspects == null) {
            aspects = generateTags(stack);
        }
        
        // Add bonus aspects from enchantments, tools, armor, etc.
        aspects = getBonusTags(stack, aspects);
        
        // Cap aspects to prevent ridiculous values
        return capAspects(aspects, ASPECT_CAP);
    }
    
    /**
     * Register aspects for an item.
     * 
     * @param stack The item stack pattern
     * @param aspects The aspects to register
     */
    public static void registerObjectAspects(ItemStack stack, AspectList aspects) {
        if (stack != null && !stack.isEmpty() && aspects != null) {
            AspectHelper.registerObjectTag(stack, aspects);
        }
    }
    
    /**
     * Generate aspects for an item by analyzing its crafting recipes.
     * This is called for items that don't have manually registered aspects.
     * 
     * @param stack The item stack to generate tags for
     * @return The generated AspectList
     */
    public static AspectList generateTags(ItemStack stack) {
        // TODO: Implement full aspect generation from crafting recipes
        // This would analyze all recipes that produce this item
        // and sum up the aspects of the ingredients, divided by output count
        
        // For now, return a minimal aspect list based on item properties
        AspectList aspects = new AspectList();
        
        if (stack == null || stack.isEmpty()) {
            return aspects;
        }
        
        // Basic aspect for all items
        aspects.add(Aspect.ENTROPY, 1);
        
        return aspects;
    }
    
    /**
     * Add bonus aspects based on item properties (enchantments, tool type, etc.)
     */
    private static AspectList getBonusTags(ItemStack stack, AspectList sourceTags) {
        AspectList result = new AspectList();
        
        if (stack == null || stack.isEmpty()) {
            return result;
        }
        
        // Copy source tags if present
        if (sourceTags != null) {
            for (Aspect aspect : sourceTags.getAspects()) {
                if (aspect != null) {
                    result.add(aspect, sourceTags.getAmount(aspect));
                }
            }
        }
        
        // Add aspects based on item type
        net.minecraft.world.item.Item item = stack.getItem();
        
        // Armor
        if (stack.has(net.minecraft.core.component.DataComponents.EQUIPPABLE)) {
            result.merge(Aspect.PROTECT, 8);
        }
        
        // Swords
        if (stack.is(net.minecraft.tags.ItemTags.SWORDS)) {
            result.merge(Aspect.AVERSION, 8);
        }
        
        // Bows
        if (stack.is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE)) {
            result.merge(Aspect.AVERSION, 10);
            result.merge(Aspect.FLIGHT, 5);
        }
        
        // Tools
        if (stack.is(net.minecraft.tags.ItemTags.AXES) || stack.is(net.minecraft.tags.ItemTags.PICKAXES)
                || stack.is(net.minecraft.tags.ItemTags.SHOVELS) || stack.is(net.minecraft.tags.ItemTags.HOES)) {
            result.merge(Aspect.TOOL, 8);
        }
        
        // Enchantments add aspects
        var enchantments = stack.getEnchantments();
        if (enchantments != null && !enchantments.isEmpty()) {
            // Each enchantment adds some magic aspect
            result.merge(Aspect.MAGIC, enchantments.size() * 3);
        }
        
        // Food
        var food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
        if (food != null) {
            result.merge(Aspect.LIFE, food.nutrition());
            if (food.saturation() > 0.5f) {
                result.merge(Aspect.DESIRE, 2);
            }
        }
        
        return result;
    }
    
    /**
     * Cap aspect amounts to prevent extreme values.
     */
    private static AspectList capAspects(AspectList source, int maxAmount) {
        if (source == null) {
            return new AspectList();
        }
        
        AspectList result = new AspectList();
        for (Aspect aspect : source.getAspects()) {
            if (aspect != null) {
                int amount = Math.min(maxAmount, source.getAmount(aspect));
                result.add(aspect, amount);
            }
        }
        return result;
    }
}
