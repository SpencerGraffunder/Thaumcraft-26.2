package thaumcraft.common.lib.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStackHelper;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.common.lib.capabilities.ThaumcraftCapabilities;
import thaumcraft.init.ModRecipeTypes;

import javax.annotation.Nullable;
import java.util.List;

/**
 * InfusionRecipeType - A Recipe implementation for infusion altar crafting.
 * 
 * Infusion recipes require:
 * - A central item on the runic matrix
 * - Component items placed on pedestals around the altar
 * - Essentia from nearby jars
 * - Research unlocked by the player
 * 
 * The infusion process consumes the central item and components,
 * drains essentia, and produces the output item.
 */
public class InfusionRecipeType implements Recipe<Container>, IThaumcraftRecipe {
    
    private final Identifier id;
    private final String group;
    private final Ingredient centralItem;
    private final NonNullList<Ingredient> components;
    private final AspectList aspects;
    private final ItemStack result;
    private final String research;
    private final int instability;
    
    public InfusionRecipeType(Identifier id, String group, Ingredient centralItem,
                              NonNullList<Ingredient> components, AspectList aspects,
                              ItemStack result, String research, int instability) {
        this.id = id;
        this.group = group;
        this.centralItem = centralItem;
        this.components = components;
        this.aspects = aspects;
        this.result = result;
        this.research = research;
        this.instability = instability;
    }
    
    /**
     * Standard Recipe.matches() - not typically used for infusion recipes
     * since they don't use a standard crafting grid.
     */
    @Override
    public boolean matches(Container container, Level level) {
        // Infusion matching is handled differently - via matchesInfusion()
        return false;
    }
    
    /**
     * Check if this recipe matches the current infusion altar state.
     * 
     * @param pedestalItems Items on the pedestals around the altar
     * @param centralItemStack The item on the runic matrix
     * @param level The world
     * @param player The player crafting
     * @return true if the recipe can be crafted
     */
    public boolean matchesInfusion(List<ItemStack> pedestalItems, ItemStack centralItemStack,
                                    Level level, Player player) {
        // Check research requirement
        if (research != null && !research.isEmpty()) {
            if (!ThaumcraftCapabilities.isResearchKnown(player, research)) {
                return false;
            }
        }
        
        // Check central item (empty ingredient means any item is valid)
        if (centralItem != Ingredient.EMPTY && !centralItem.test(centralItemStack)) {
            return false;
        }
        
        // Check pedestal components using RecipeMatcher for shapeless matching
        return RecipeMatcher.findMatches(pedestalItems, components) != null;
    }
    
    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return result.copy();
    }
    
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // Infusion doesn't use dimensions
    }
    
    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result;
    }
    
    public ItemStack getResultItem() {
        return result.copy();
    }
    
    @Override
    public Identifier getId() {
        return id;
    }
    
    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }
    
    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.INFUSION.get();
    }
    
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> allIngredients = NonNullList.create();
        allIngredients.add(centralItem);
        allIngredients.addAll(components);
        return allIngredients;
    }
    
    @Override
    public String getGroup() {
        return group;
    }
    
    @Override
    public String getResearch() {
        return research;
    }
    
    public Ingredient getCentralItem() {
        return centralItem;
    }
    
    public NonNullList<Ingredient> getComponents() {
        return components;
    }
    
    public AspectList getAspects() {
        return aspects;
    }
    
    public int getInstability() {
        return instability;
    }
    
    /**
     * Gets the recipe output, potentially modified based on input.
     * Override this in subclasses for recipes that modify the output based on input properties.
     */
    public ItemStack getRecipeOutput(Player player, ItemStack input, List<ItemStack> pedestalItems) {
        return result.copy();
    }
    
    /**
     * Gets the aspects required, potentially modified based on input.
     * Override this in subclasses for recipes with variable essentia costs.
     */
    public AspectList getAspects(Player player, ItemStack input, List<ItemStack> pedestalItems) {
        return aspects;
    }
    
    /**
     * Gets the instability level, potentially modified based on input.
     * Override this in subclasses for recipes with variable instability.
     */
    public int getInstability(Player player, ItemStack input, List<ItemStack> pedestalItems) {
        return instability;
    }
    
    /**
     * Serializer for InfusionRecipeType.
     */
    public static class Serializer implements RecipeSerializer<InfusionRecipeType> {
        
        public static final Serializer INSTANCE = new Serializer();
        public static final Identifier ID = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "infusion");
        
        @Override
        public InfusionRecipeType fromJson(Identifier recipeId, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            String research = GsonHelper.getAsString(json, "research", "");
            int instability = GsonHelper.getAsInt(json, "instability", 0);
            
            // Parse central item
            Ingredient centralItem = json.has("central") 
                ? Ingredient.fromJson(json.get("central"), false)
                : Ingredient.EMPTY;
            
            // Parse pedestal components
            NonNullList<Ingredient> components = NonNullList.create();
            if (json.has("components")) {
                JsonArray componentsArray = GsonHelper.getAsJsonArray(json, "components");
                for (int i = 0; i < componentsArray.size(); i++) {
                    Ingredient ingredient = Ingredient.fromJson(componentsArray.get(i), false);
                    if (!ingredient.isEmpty()) {
                        components.add(ingredient);
                    }
                }
            }
            
            // Parse aspects
            AspectList aspects = new AspectList();
            if (json.has("aspects")) {
                JsonObject aspectsJson = GsonHelper.getAsJsonObject(json, "aspects");
                for (String aspectName : aspectsJson.keySet()) {
                    Aspect aspect = Aspect.getAspect(aspectName);
                    if (aspect != null) {
                        aspects.add(aspect, aspectsJson.get(aspectName).getAsInt());
                    }
                }
            }
            
            // Parse result
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            
            return new InfusionRecipeType(recipeId, group, centralItem, components, aspects, result, research, instability);
        }
        
        @Override
        public @Nullable InfusionRecipeType fromNetwork(Identifier recipeId, FriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            String research = buffer.readUtf();
            int instability = buffer.readVarInt();
            
            Ingredient centralItem = Ingredient.fromNetwork(buffer);
            
            // Read components
            int componentCount = buffer.readVarInt();
            NonNullList<Ingredient> components = NonNullList.withSize(componentCount, Ingredient.EMPTY);
            for (int i = 0; i < componentCount; i++) {
                components.set(i, Ingredient.fromNetwork(buffer));
            }
            
            // Read aspects
            AspectList aspects = new AspectList();
            int aspectCount = buffer.readVarInt();
            for (int i = 0; i < aspectCount; i++) {
                String aspectName = buffer.readUtf();
                int amount = buffer.readVarInt();
                Aspect aspect = Aspect.getAspect(aspectName);
                if (aspect != null) {
                    aspects.add(aspect, amount);
                }
            }
            
            ItemStack result = buffer.readItem();
            
            return new InfusionRecipeType(recipeId, group, centralItem, components, aspects, result, research, instability);
        }
        
        @Override
        public void toNetwork(FriendlyByteBuf buffer, InfusionRecipeType recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeUtf(recipe.research);
            buffer.writeVarInt(recipe.instability);
            
            recipe.centralItem.toNetwork(buffer);
            
            // Write components
            buffer.writeVarInt(recipe.components.size());
            for (Ingredient component : recipe.components) {
                component.toNetwork(buffer);
            }
            
            // Write aspects
            Aspect[] aspectArray = recipe.aspects.getAspects();
            buffer.writeVarInt(aspectArray.length);
            for (Aspect aspect : aspectArray) {
                buffer.writeUtf(aspect.getTag());
                buffer.writeVarInt(recipe.aspects.getAmount(aspect));
            }
            
            buffer.writeItem(recipe.result);
        }
    }
}
