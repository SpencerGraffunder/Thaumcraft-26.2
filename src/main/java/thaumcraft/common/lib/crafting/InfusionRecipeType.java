package thaumcraft.common.lib.crafting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.entity.player.Player;
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
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.common.lib.capabilities.ThaumcraftCapabilities;
import thaumcraft.init.ModRecipeTypes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.StreamCodec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.neoforged.neoforge.common.util.RecipeMatcher;

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
public class InfusionRecipeType implements Recipe<RecipeInput>, IThaumcraftRecipe {
    
    private final String group;
    private final Ingredient centralItem;
    private final List<Ingredient> components;
    private final AspectList aspects;
    private final ItemStack result;
    private final String research;
    private final int instability;
    
    public InfusionRecipeType(String group, Ingredient centralItem,
                              List<Ingredient> components, AspectList aspects,
                              ItemStack result, String research, int instability) {
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
    public boolean matches(RecipeInput input, Level level) {
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
        if (!centralItem.isEmpty() && !centralItem.test(centralItemStack)) {
            return false;
        }
        
        // Check pedestal components using RecipeMatcher for shapeless matching
        return RecipeMatcher.findMatches(pedestalItems, components) != null;
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
        return ModRecipeTypes.INFUSION.get();
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
    
    public List<Ingredient> getIngredients() {
        List<Ingredient> allIngredients = new java.util.ArrayList<>();
        allIngredients.add(centralItem);
        allIngredients.addAll(components);
        return allIngredients;
    }
    
    @Override
    public String getResearch() {
        return research;
    }
    
    public Ingredient getCentralItem() {
        return centralItem;
    }
    
    public List<Ingredient> getComponents() {
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

    public static final MapCodec<InfusionRecipeType> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
            Ingredient.CODEC.optionalFieldOf("center").forGetter(r -> java.util.Optional.ofNullable(r.centralItem)),
            Ingredient.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(r -> r.components),
            ASPECTS_CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(r -> r.aspects),
            ItemStack.OPTIONAL_CODEC.fieldOf("result").forGetter(r -> r.result),
            Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
            Codec.INT.optionalFieldOf("instability", 0).forGetter(r -> r.instability)
    ).apply(i, (group, center, ingredients, aspects, result, research, instability) ->
            new InfusionRecipeType(group, center.orElse(null), ingredients, aspects, result, research, instability)));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionRecipeType> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public InfusionRecipeType decode(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            String research = buffer.readUtf();
            int instability = buffer.readVarInt();

            Ingredient centralItem = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);

            int componentCount = buffer.readVarInt();
            List<Ingredient> components = new java.util.ArrayList<>();
            for (int i = 0; i < componentCount; i++) {
                components.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }

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

            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);

            return new InfusionRecipeType(group, centralItem, components, aspects, result, research, instability);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, InfusionRecipeType recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeUtf(recipe.research);
            buffer.writeVarInt(recipe.instability);

            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.centralItem);

            buffer.writeVarInt(recipe.components.size());
            for (Ingredient component : recipe.components) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, component);
            }

            Aspect[] aspectArray = recipe.aspects.getAspects();
            buffer.writeVarInt(aspectArray.length);
            for (Aspect aspect : aspectArray) {
                buffer.writeUtf(aspect.getTag());
                buffer.writeVarInt(recipe.aspects.getAmount(aspect));
            }

            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }
    };

    public static final RecipeSerializer<InfusionRecipeType> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
