package thaumcraft.api.internal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi.EntityTags;
import thaumcraft.api.ThaumcraftApi.SmeltBonus;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IThaumcraftRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal variables and methods used by Thaumcraft and the API.
 * These should normally not be accessed directly by addon mods.
 * 
 * @author Azanor
 */
public class CommonInternals {
    
    /** Research JSON file locations registered by addons */
    public static Map<String, Identifier> jsonLocs = new HashMap<>();
    
    /** Entity aspect tags for scanning */
    public static List<EntityTags> scanEntities = new ArrayList<>();
    
    /** Thaumcraft crafting recipe catalog (infusion, crucible, etc.) */
    public static Map<Identifier, IThaumcraftRecipe> craftingRecipeCatalog = new HashMap<>();
    
    /** Fake recipes for thaumonomicon display only */
    public static Map<Identifier, Object> craftingRecipeCatalogFake = new HashMap<>();
    
    /** Infernal furnace smelting bonuses */
    public static List<SmeltBonus> smeltingBonus = new ArrayList<>();
    
    /** Object -> Aspect mappings (item/block aspect tags) */
    public static ConcurrentHashMap<String, AspectList> objectTags = new ConcurrentHashMap<>();
    
    /** Warp amounts for crafted items */
    public static Map<Object, Integer> warpMap = new HashMap<>();
    
    /** Block -> Seed item mappings */
    public static Map<String, ItemStack> seedList = new HashMap<>();
    
    /**
     * Get a recipe from the crafting catalog.
     */
    public static IThaumcraftRecipe getCatalogRecipe(Identifier key) {
        return craftingRecipeCatalog.get(key);
    }
    
    /**
     * Get a fake recipe from the catalog.
     */
    public static Object getCatalogRecipeFake(Identifier key) {
        return craftingRecipeCatalogFake.get(key);
    }
    
    /**
     * Generate a unique ID for an itemstack based on its NBT serialization.
     * Obviously not truly unique, but unique enough for this purpose.
     */
    public static int generateUniqueItemstackId(ItemStack stack) {
        ItemStack sc = stack.copy();
        sc.setCount(1);
        CompoundTag nbt = new CompoundTag();
        sc.save(nbt);
        return nbt.toString().hashCode();
    }
    
    /**
     * Generate a unique ID for an itemstack, stripping all NBT data first.
     */
    public static int generateUniqueItemstackIdStripped(ItemStack stack) {
        ItemStack sc = stack.copy();
        sc.setCount(1);
        sc.setTag(null);
        CompoundTag nbt = new CompoundTag();
        sc.save(nbt);
        return nbt.toString().hashCode();
    }
}
