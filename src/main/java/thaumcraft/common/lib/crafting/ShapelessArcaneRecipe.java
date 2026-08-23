package thaumcraft.common.lib.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IArcaneWorkbench;
import thaumcraft.init.ModRecipeTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.util.RecipeMatcher;

/**
 * ShapelessArcaneRecipe - A shapeless crafting recipe for the Arcane Workbench.
 * 
 * Requires:
 * - Items placed anywhere in the 3x3 grid (order doesn't matter)
 * - Vis drawn from the aura
 * - Primal crystals in the 6 crystal slots
 * - Research unlocked by the player
 */
public class ShapelessArcaneRecipe implements IArcaneRecipe {
    
    private final String group;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int visCost;
    private final AspectList crystals;
    private final String research;
    
    public ShapelessArcaneRecipe(String group,
                                 NonNullList<Ingredient> ingredients, ItemStack result,
                                 int visCost, AspectList crystals, String research) {
        this.group = group;
        this.ingredients = ingredients;
        this.result = result;
        this.visCost = visCost;
        this.crystals = crystals;
        this.research = research;
    }
    
    @Override
    public boolean matches(IArcaneWorkbench container, Level level) {
        List<ItemStack> inputs = new ArrayList<>();
        
        // Gather all non-empty items from the 3x3 grid (slots 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack item = container.getItem(i);
            if (!item.isEmpty()) {
                inputs.add(item);
            }
        }
        
        // Check if input count matches and all ingredients are satisfied
        return inputs.size() == ingredients.size() && RecipeMatcher.findMatches(inputs, ingredients) != null;
    }
    
    @Override
    public ItemStack assemble(IArcaneWorkbench container) {
        return result.copy();
    }
    
    @Override
    public ItemStack getResultItem() {
        return result.copy();
    }
    
    @Override
    public RecipeSerializer<? extends Recipe<IArcaneWorkbench>> getSerializer() {
        return SERIALIZER;
    }
    
    @Override
    public RecipeType<? extends Recipe<IArcaneWorkbench>> getType() {
        return ModRecipeTypes.ARCANE_WORKBENCH.get();
    }
    
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }
    
    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
    
    @Override
    public boolean showNotification() {
        return true;
    }
    
    @Override
    public String group() {
        return group;
    }
    
    public String getGroup() {
        return group;
    }
    
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }
    
    // IArcaneRecipe implementation
    
    @Override
    public int getVis() {
        return visCost;
    }
    
    @Override
    public String getResearch() {
        return research;
    }
    
    @Override
    public AspectList getCrystals() {
        return crystals;
    }
    
    /** Codec for an AspectList stored as { "aspectTag": amount, ... }. */
    private static final Codec<AspectList> ASPECTS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(map -> {
                AspectList list = new AspectList();
                map.forEach((name, amount) -> {
                    Aspect aspect = Aspect.getAspect(name);
                    if (aspect != null) list.add(aspect, amount);
                });
                return list;
            }, list -> {
                Map<String, Integer> map = new HashMap<>();
                for (Aspect aspect : list.getAspects()) {
                    map.put(aspect.getTag(), list.getAmount(aspect));
                }
                return map;
            });
    
    public static final MapCodec<ShapelessArcaneRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
            Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
            Codec.INT.optionalFieldOf("vis", 0).forGetter(r -> r.visCost),
            ASPECTS_CODEC.optionalFieldOf("crystals", new AspectList()).forGetter(r -> r.crystals),
            ASPECTS_CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(r -> r.crystals),
            Ingredient.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(r -> r.ingredients),
            ItemStack.OPTIONAL_CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(i, ShapelessArcaneRecipe::create));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessArcaneRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ShapelessArcaneRecipe decode(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            String research = buffer.readUtf();
            int visCost = buffer.readVarInt();
            
            AspectList crystals = readAspects(buffer);
            
            int ingredientCount = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (int i = 0; i < ingredientCount; i++) {
                ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            
            return new ShapelessArcaneRecipe(group, ingredients, result, visCost, crystals, research);
        }
        
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ShapelessArcaneRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeUtf(recipe.research);
            buffer.writeVarInt(recipe.visCost);
            
            writeAspects(buffer, recipe.crystals);
            
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
            
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }
    };
    
    public static final RecipeSerializer<ShapelessArcaneRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    
    private static ShapelessArcaneRecipe create(String group, String research, int visCost,
                                                AspectList crystals, AspectList aspects,
                                                List<Ingredient> ingredients, ItemStack result) {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(ingredients);
        return new ShapelessArcaneRecipe(group, list, result, visCost,
                aspects.size() > 0 ? aspects : crystals, research);
    }
    
    private static AspectList readAspects(RegistryFriendlyByteBuf buffer) {
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
        return aspects;
    }
    
    private static void writeAspects(RegistryFriendlyByteBuf buffer, AspectList aspects) {
        Aspect[] aspectArray = aspects.getAspects();
        buffer.writeVarInt(aspectArray.length);
        for (Aspect aspect : aspectArray) {
            buffer.writeUtf(aspect.getTag());
            buffer.writeVarInt(aspects.getAmount(aspect));
        }
    }
}
