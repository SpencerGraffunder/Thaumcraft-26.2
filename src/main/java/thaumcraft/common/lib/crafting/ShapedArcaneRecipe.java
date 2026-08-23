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
import net.minecraft.world.item.crafting.ShapedRecipePattern;
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
import java.util.Optional;

/**
 * ShapedArcaneRecipe - A shaped crafting recipe for the Arcane Workbench.
 * 
 * Requires:
 * - Items arranged in a specific pattern in the 3x3 grid
 * - Vis drawn from the aura
 * - Primal crystals in the 6 crystal slots
 * - Research unlocked by the player
 */
public class ShapedArcaneRecipe implements IArcaneRecipe {
    
    private final String group;
    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int visCost;
    private final AspectList crystals;
    private final String research;
    
    public ShapedArcaneRecipe(String group, int width, int height,
                              NonNullList<Ingredient> ingredients, ItemStack result,
                              int visCost, AspectList crystals, String research) {
        this.group = group;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
        this.visCost = visCost;
        this.crystals = crystals;
        this.research = research;
    }
    
    @Override
    public boolean matches(IArcaneWorkbench container, Level level) {
        // Try all possible positions in the grid
        for (int x = 0; x <= 3 - width; x++) {
            for (int y = 0; y <= 3 - height; y++) {
                if (matches(container, x, y, true) || matches(container, x, y, false)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if the recipe matches at a specific position with optional mirroring.
     */
    private boolean matches(IArcaneWorkbench container, int offsetX, int offsetY, boolean mirrored) {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                int checkX = x - offsetX;
                int checkY = y - offsetY;
                
                Ingredient ingredient = null;
                if (checkX >= 0 && checkY >= 0 && checkX < width && checkY < height) {
                    if (mirrored) {
                        ingredient = ingredients.get(width - checkX - 1 + checkY * width);
                    } else {
                        ingredient = ingredients.get(checkX + checkY * width);
                    }
                }
                
                // Slots 0-8 are the crafting grid
                if (ingredient != null && !ingredient.test(container.getItem(x + y * 3))) {
                    return false;
                }
            }
        }
        return true;
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
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
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
    
    public static final MapCodec<ShapedArcaneRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
            Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
            Codec.INT.optionalFieldOf("vis", 0).forGetter(r -> r.visCost),
            ASPECTS_CODEC.optionalFieldOf("crystals", new AspectList()).forGetter(r -> r.crystals),
            ASPECTS_CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(r -> r.crystals),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> new ShapedRecipePattern(r.width, r.height, toOptionalIngredients(r.ingredients), Optional.empty())),
            ItemStack.OPTIONAL_CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(i, ShapedArcaneRecipe::create));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ShapedArcaneRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ShapedArcaneRecipe decode(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            String research = buffer.readUtf();
            int visCost = buffer.readVarInt();
            
            AspectList crystals = readAspects(buffer);
            
            int width = buffer.readVarInt();
            int height = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (int i = 0; i < width * height; i++) {
                ingredients.add(Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buffer).orElse(null));
            }
            
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            
            return new ShapedArcaneRecipe(group, width, height, ingredients, result, visCost, crystals, research);
        }
        
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ShapedArcaneRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeUtf(recipe.research);
            buffer.writeVarInt(recipe.visCost);
            
            writeAspects(buffer, recipe.crystals);
            
            buffer.writeVarInt(recipe.width);
            buffer.writeVarInt(recipe.height);
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buffer, ingredient.isEmpty() ? Optional.empty() : Optional.of(ingredient));
            }
            
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }
    };
    
    public static final RecipeSerializer<ShapedArcaneRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    
    private static ShapedArcaneRecipe create(String group, String research, int visCost,
                                             AspectList crystals, AspectList aspects,
                                             ShapedRecipePattern pattern, ItemStack result) {
        return new ShapedArcaneRecipe(group, pattern.width(), pattern.height(),
                toIngredients(pattern.ingredients()), result, visCost,
                aspects.size() > 0 ? aspects : crystals, research);
    }
    
    private static NonNullList<Ingredient> toIngredients(List<Optional<Ingredient>> list) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (Optional<Ingredient> opt : list) {
            ingredients.add(opt.orElse(null));
        }
        return ingredients;
    }
    
    private static List<Optional<Ingredient>> toOptionalIngredients(NonNullList<Ingredient> ingredients) {
        List<Optional<Ingredient>> list = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            list.add(ingredient.isEmpty() ? Optional.empty() : Optional.of(ingredient));
        }
        return list;
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
