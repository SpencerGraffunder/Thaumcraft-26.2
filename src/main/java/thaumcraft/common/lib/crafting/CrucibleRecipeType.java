package thaumcraft.common.lib.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.init.ModRecipeTypes;

import javax.annotation.Nullable;

/**
 * CrucibleRecipeType - A Recipe implementation for crucible alchemy.
 * 
 * Crucible recipes require:
 * - A catalyst item thrown into the crucible
 * - Required aspects (essentia) already dissolved in the crucible
 * - Research unlocked by the player
 * 
 * The catalyst is consumed and the aspects are removed from the crucible
 * to produce the output item.
 */
public class CrucibleRecipeType implements Recipe<Container>, IThaumcraftRecipe {
    
    private final Identifier id;
    private final String group;
    private final Ingredient catalyst;
    private final AspectList aspects;
    private final ItemStack result;
    private final String research;
    
    public CrucibleRecipeType(Identifier id, String group, Ingredient catalyst,
                              AspectList aspects, ItemStack result, String research) {
        this.id = id;
        this.group = group;
        this.catalyst = catalyst;
        this.aspects = aspects;
        this.result = result;
        this.research = research;
    }
    
    /**
     * Standard Recipe.matches() - not typically used for crucible recipes
     * since they don't use a standard crafting grid.
     */
    @Override
    public boolean matches(Container container, Level level) {
        // Crucible matching is handled differently - via matchesCrucible()
        return false;
    }
    
    /**
     * Check if this recipe matches the given crucible state.
     * 
     * @param crucibleAspects The aspects currently in the crucible
     * @param catalystStack The item being thrown in
     * @return true if the recipe can be crafted
     */
    public boolean matchesCrucible(AspectList crucibleAspects, ItemStack catalystStack) {
        if (!catalyst.test(catalystStack)) return false;
        if (crucibleAspects == null) return false;
        
        // Check all required aspects are present in sufficient amounts
        for (Aspect aspect : aspects.getAspects()) {
            if (crucibleAspects.getAmount(aspect) < aspects.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if the given item can act as a catalyst for this recipe.
     */
    public boolean catalystMatches(ItemStack catalystStack) {
        return catalyst.test(catalystStack);
    }
    
    /**
     * Creates a new AspectList with the required aspects removed.
     */
    public AspectList removeMatchingAspects(AspectList crucibleAspects) {
        AspectList result = crucibleAspects.copy();
        for (Aspect aspect : aspects.getAspects()) {
            result.remove(aspect, aspects.getAmount(aspect));
        }
        return result;
    }
    
    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return result.copy();
    }
    
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // Crucible doesn't use dimensions
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
        return ModRecipeTypes.CRUCIBLE.get();
    }
    
    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, catalyst);
    }
    
    @Override
    public String getGroup() {
        return group;
    }
    
    @Override
    public String getResearch() {
        return research;
    }
    
    public Ingredient getCatalyst() {
        return catalyst;
    }
    
    public AspectList getAspects() {
        return aspects;
    }
    
    /**
     * Serializer for CrucibleRecipeType.
     */
    public static class Serializer implements RecipeSerializer<CrucibleRecipeType> {
        
        public static final Serializer INSTANCE = new Serializer();
        public static final Identifier ID = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "crucible");
        
        @Override
        public CrucibleRecipeType fromJson(Identifier recipeId, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            String research = GsonHelper.getAsString(json, "research", "");
            
            // Parse catalyst (accept both "catalyst" and "ingredient" for compatibility)
            JsonElement catalystJson = json.has("catalyst") ? json.get("catalyst") : json.get("ingredient");
            Ingredient catalyst = catalystJson != null ? Ingredient.fromJson(catalystJson, false) : Ingredient.EMPTY;
            
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
            
            return new CrucibleRecipeType(recipeId, group, catalyst, aspects, result, research);
        }
        
        @Override
        public @Nullable CrucibleRecipeType fromNetwork(Identifier recipeId, FriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            String research = buffer.readUtf();
            
            Ingredient catalyst = Ingredient.fromNetwork(buffer);
            
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
            
            return new CrucibleRecipeType(recipeId, group, catalyst, aspects, result, research);
        }
        
        @Override
        public void toNetwork(FriendlyByteBuf buffer, CrucibleRecipeType recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeUtf(recipe.research);
            
            recipe.catalyst.toNetwork(buffer);
            
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
