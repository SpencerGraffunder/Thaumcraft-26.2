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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.init.ModRecipeTypes;

import java.util.HashMap;
import java.util.Map;

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
public class CrucibleRecipeType implements Recipe<RecipeInput>, IThaumcraftRecipe {
    
    private final String group;
    private final Ingredient catalyst;
    private final AspectList aspects;
    private final ItemStack result;
    private final String research;
    
    public CrucibleRecipeType(String group, Ingredient catalyst,
                              AspectList aspects, ItemStack result, String research) {
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
    public boolean matches(RecipeInput input, Level level) {
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
    public ItemStack assemble(RecipeInput input) {
        return result.copy();
    }
    
    public ItemStack getResultItem() {
        return result.copy();
    }
    
    @Override
    public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return SERIALIZER;
    }
    
    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return ModRecipeTypes.CRUCIBLE.get();
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
        return NonNullList.of(Ingredient.of(net.minecraft.core.HolderSet.empty()), catalyst);
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
    
    public static final MapCodec<CrucibleRecipeType> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
            Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
            Ingredient.CODEC.optionalFieldOf("catalyst", Ingredient.of(net.minecraft.core.HolderSet.empty())).forGetter(r -> r.catalyst),
            Ingredient.CODEC.optionalFieldOf("ingredient", Ingredient.of(net.minecraft.core.HolderSet.empty())).forGetter(r -> r.catalyst),
            ASPECTS_CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(r -> r.aspects),
            ItemStack.OPTIONAL_CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(i, CrucibleRecipeType::create));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipeType> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CrucibleRecipeType decode(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            String research = buffer.readUtf();
            
            Ingredient catalyst = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            
            AspectList aspects = readAspects(buffer);
            
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            
            return new CrucibleRecipeType(group, catalyst, aspects, result, research);
        }
        
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CrucibleRecipeType recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeUtf(recipe.research);
            
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.catalyst);
            
            writeAspects(buffer, recipe.aspects);
            
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }
    };
    
    public static final RecipeSerializer<CrucibleRecipeType> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    
    private static CrucibleRecipeType create(String group, String research,
                                             Ingredient catalyst, Ingredient ingredient,
                                             AspectList aspects, ItemStack result) {
        return new CrucibleRecipeType(group, !catalyst.isEmpty() ? catalyst : ingredient, aspects, result, research);
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
