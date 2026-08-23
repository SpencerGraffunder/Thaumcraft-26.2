package thaumcraft.common.lib.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializer for InfusionEnchantmentRecipe.
 * Handles JSON parsing and network synchronization via codecs.
 */
public class InfusionEnchantmentRecipeSerializer {
    
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
    
    public static final MapCodec<InfusionEnchantmentRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.xmap(
                    s -> EnumInfusionEnchantment.valueOf(s.toUpperCase()),
                    e -> e.name()
            ).fieldOf("enchantment").forGetter(r -> r.enchantment),
            Ingredient.CODEC.listOf().optionalFieldOf("components", List.of()).forGetter(r -> r.getComponents()),
            ASPECTS_CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(r -> r.getAspects())
    ).apply(i, InfusionEnchantmentRecipeSerializer::create));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionEnchantmentRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public InfusionEnchantmentRecipe decode(RegistryFriendlyByteBuf buffer) {
            int enchantmentOrdinal = buffer.readVarInt();
            EnumInfusionEnchantment enchantment = EnumInfusionEnchantment.values()[enchantmentOrdinal];
            
            int componentCount = buffer.readVarInt();
            NonNullList<Ingredient> components = NonNullList.create();
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
            
            return new InfusionEnchantmentRecipe(enchantment, aspects, components);
        }
        
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, InfusionEnchantmentRecipe recipe) {
            buffer.writeVarInt(recipe.enchantment.ordinal());
            
            buffer.writeVarInt(recipe.getComponents().size());
            for (Ingredient component : recipe.getComponents()) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, component);
            }
            
            Aspect[] aspectArray = recipe.getAspects().getAspects();
            buffer.writeVarInt(aspectArray.length);
            for (Aspect aspect : aspectArray) {
                buffer.writeUtf(aspect.getTag());
                buffer.writeVarInt(recipe.getAspects().getAmount(aspect));
            }
        }
    };
    
    public static final RecipeSerializer<InfusionEnchantmentRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    
    private static InfusionEnchantmentRecipe create(EnumInfusionEnchantment enchantment,
                                                    List<Ingredient> components, AspectList aspects) {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(components);
        return new InfusionEnchantmentRecipe(enchantment, aspects, list);
    }
}
