package thaumcraft.common.lib.crafting;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Multimap;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.nbt.ByteTag;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.items.IRechargable;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.init.ModRecipeSerializers;
import thaumcraft.common.lib.compat.CuriosCompat;
import net.neoforged.neoforge.common.util.RecipeMatcher;

public class InfusionEnchantmentRecipe extends InfusionRecipeType {
    
    public final EnumInfusionEnchantment enchantment;
    
    public InfusionEnchantmentRecipe(EnumInfusionEnchantment ench, AspectList as, NonNullList<Ingredient> components) {
        super("", Ingredient.of(), components, as, ItemStack.EMPTY, ench.research, 4);
        this.enchantment = ench;
    }
    
    @Override
    public boolean matchesInfusion(List<ItemStack> input, ItemStack central, Level level, Player player) {
        if (central == null || central.isEmpty() || !ThaumcraftCapabilities.isResearchKnown(player, getResearch())) {
            return false;
        }
        
        if (EnumInfusionEnchantment.getInfusionEnchantmentLevel(central, enchantment) >= enchantment.maxLevel) {
            return false;
        }
        
        if (!enchantment.toolClasses.contains("all")) {
            boolean cool = false;
            
            if (central.is(net.minecraft.tags.ItemTags.SWORDS) && enchantment.toolClasses.contains("weapon")) {
                cool = true;
            }
            
            if (!cool) {
                // Simplified check matching the previous implementation
                if (enchantment.toolClasses.contains("axe") && central.is(net.minecraft.tags.ItemTags.AXES)) cool = true;
                if (enchantment.toolClasses.contains("pickaxe") && central.is(net.minecraft.tags.ItemTags.PICKAXES)) cool = true;
                if (enchantment.toolClasses.contains("shovel") && central.is(net.minecraft.tags.ItemTags.SHOVELS)) cool = true;
                if (enchantment.toolClasses.contains("sword") && central.is(net.minecraft.tags.ItemTags.SWORDS)) cool = true;
                if (enchantment.toolClasses.contains("hoe") && central.is(net.minecraft.tags.ItemTags.HOES)) cool = true;
            }
            
            if (!cool && central.has(DataComponents.EQUIPPABLE)) {
                String at = "none";
                switch (central.get(DataComponents.EQUIPPABLE).slot()) {
                    case HEAD -> at = "helm";
                    case CHEST -> at = "chest";
                    case LEGS -> at = "legs";
                    case FEET -> at = "boots";
                }
                if (enchantment.toolClasses.contains("armor") || enchantment.toolClasses.contains(at)) {
                    cool = true;
                }
            }
            
            // Check if item is a Curios-compatible bauble
            if (!cool && enchantment.toolClasses.contains("bauble")) {
                // Items with ICurioItem interface or in curios slot identifiers are baubles
                // For simplicity, we check if it can go in any curios slot
                if (CuriosCompat.isCuriosLoaded()) {
                    // Check if this is a curio-capable item
                    try {
                        Class<?> curioInterface = Class.forName("top.theillusivec4.curios.api.type.capability.ICurioItem");
                        if (curioInterface.isAssignableFrom(central.getItem().getClass())) {
                            cool = true;
                        }
                    } catch (ClassNotFoundException ignored) {
                        // Curios not available
                    }
                }
            }
            
            if (!cool && central.getItem() instanceof IRechargable && enchantment.toolClasses.contains("chargable")) {
                cool = true;
            }
            
            if (!cool) {
                return false;
            }
        }
        
        return RecipeMatcher.findMatches(input, getComponents()) != null;
    }
    
    @Override
    public ItemStack getRecipeOutput(Player player, ItemStack input, List<ItemStack> comps) {
        if (input == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = input.copy();
        int cl = EnumInfusionEnchantment.getInfusionEnchantmentLevel(out, enchantment);
        if (cl >= enchantment.maxLevel) {
            return ItemStack.EMPTY;
        }
        
        List<EnumInfusionEnchantment> el = EnumInfusionEnchantment.getInfusionEnchantments(input);
        Random rand = new Random(System.nanoTime());
        if (rand.nextInt(10) < el.size()) {
            int base = 1;
            if (input.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                base += input.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getByteOr("TC.WARP", (byte)0);
            }
            final int warpBase = base;
            net.minecraft.world.item.component.CustomData.update(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA, out,
                    tag -> tag.putByte("TC.WARP", (byte)warpBase));
        }
        
        EnumInfusionEnchantment.addInfusionEnchantment(out, enchantment, cl + 1);
        return out;
    }
    
    @Override
    public AspectList getAspects(Player player, ItemStack input, List<ItemStack> comps) {
        AspectList out = new AspectList();
        if (input == null || input.isEmpty()) {
            return out;
        }
        
        int cl = EnumInfusionEnchantment.getInfusionEnchantmentLevel(input, enchantment) + 1;
        if (cl > enchantment.maxLevel) {
            return out;
        }
        
        List<EnumInfusionEnchantment> el = EnumInfusionEnchantment.getInfusionEnchantments(input);
        int otherEnchantments = el.size();
        if (el.contains(enchantment)) {
            --otherEnchantments;
        }
        
        float modifier = cl + otherEnchantments * 0.33f;
        for (Aspect a : super.getAspects().getAspects()) {
            out.add(a, (int)(super.getAspects().getAmount(a) * modifier));
        }
        
        return out;
    }
    
    @Override
    public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return ModRecipeSerializers.INFUSION_ENCHANTMENT.get();
    }
}
