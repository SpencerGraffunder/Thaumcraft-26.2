package thaumcraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.ItemHandlerProvider;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InventoryWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInventoryWrapper;

import javax.annotation.Nullable;

/**
 * Inventory helper utilities for Thaumcraft.
 * Provides methods for item stack comparison, inventory searching, and item insertion/extraction.
 * 
 * Ported to 1.20.1 from 1.12.2.
 * Key changes:
 * - OreDictionary replaced with Tags
 * - EnumFacing -> Direction
 * - NBTTagCompound -> CompoundTag
 */
public class ThaumcraftInvHelper {
    
    /**
     * Filter options for item stack comparison.
     */
    public static class InvFilter {
        public boolean igDmg;      // Ignore damage values
        public boolean igNBT;      // Ignore NBT tags
        public boolean useOre;     // Use tag-based (ore dict) matching
        public boolean useMod;     // Match by mod namespace only
        public boolean relaxedNBT = false; // Use relaxed NBT comparison
        
        public InvFilter(boolean ignoreDamage, boolean ignoreNBT, boolean useOre, boolean useMod) {
            this.igDmg = ignoreDamage;
            this.igNBT = ignoreNBT;
            this.useOre = useOre;
            this.useMod = useMod;
        }
        
        public InvFilter setRelaxedNBT() {
            this.relaxedNBT = true;
            return this;
        }
        
        /** Strict comparison - item and NBT must match exactly */
        public static final InvFilter STRICT = new InvFilter(false, false, false, false);
        
        /** Base ore comparison - uses tag matching */
        public static final InvFilter BASEORE = new InvFilter(false, false, true, false);
    }
    
    /**
     * Get an IItemHandler at a position from a specific side.
     */
    @Nullable
    public static IItemHandler getItemHandlerAt(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            // Try capability first
            var capability = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
            if (capability.isPresent()) {
                return capability.orElse(null);
            }
            
            // Fall back to Container wrapping
            if (blockEntity instanceof Container container) {
                return wrapInventory(container, side);
            }
        }
        return null;
    }
    
    /**
     * Wrap a Container as an IItemHandler.
     */
    public static IItemHandler wrapInventory(Container inventory, Direction side) {
        if (inventory instanceof WorldlyContainer worldly) {
            return new SidedInvWrapper(worldly, side);
        }
        return new InvWrapper(inventory);
    }
    
    /**
     * Relaxed NBT comparison - checks if all tags in prime exist and match in other.
     * Extra tags in other are ignored. This handles mods that add extra NBT data.
     */
    public static boolean areItemStackTagsEqualRelaxed(ItemStack prime, ItemStack other) {
        if (prime.isEmpty() && other.isEmpty()) {
            return true;
        }
        if (!prime.isEmpty() && !other.isEmpty()) {
            return prime.getTag() == null || compareTagsRelaxed(prime.getTag(), other.getTag());
        }
        return false;
    }
    
    /**
     * Compare two CompoundTags in a relaxed manner.
     * Returns true if all keys in prime exist in other with equal values.
     */
    public static boolean compareTagsRelaxed(CompoundTag prime, CompoundTag other) {
        if (prime == null) return true;
        if (other == null) return false;
        
        for (String key : prime.getAllKeys()) {
            if (!other.contains(key)) return false;
            if (!prime.get(key).equals(other.get(key))) return false;
        }
        return true;
    }
    
    /**
     * Check if two item stacks match for crafting purposes.
     * Handles tag-based matching and relaxed NBT comparison.
     */
    public static boolean areItemStacksEqualForCrafting(ItemStack stack0, Object input) {
        if (stack0 == null && input != null) return false;
        if (stack0 != null && input == null) return false;
        if (stack0 == null) return true;
        
        if (input instanceof Object[]) return true;
        
        if (input instanceof ItemStack other) {
            // NBT comparison
            boolean nbtMatch = !stack0.hasTag() || areItemStackTagsEqualForCrafting(stack0, other);
            if (!nbtMatch) return false;
            
            // Item comparison (ignoring damage for damageable items)
            return ItemStack.isSameItem(stack0, other);
        }
        
        return false;
    }
    
    /**
     * Check if stacks share any item tags.
     */
    public static boolean containsTagMatch(ItemStack input, ItemStack target) {
        for (var tag : input.getTags().toList()) {
            if (target.is(tag)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if two items are equal (ignoring damage for damageable items).
     */
    public static boolean areItemsEqual(ItemStack s1, ItemStack s2) {
        if (s1.isDamageableItem() && s2.isDamageableItem()) {
            return s1.is(s2.getItem());
        }
        return ItemStack.isSameItemSameTags(s1, s2);
    }
    
    /**
     * NBT comparison for crafting - recipe NBT must be subset of slot NBT.
     */
    public static boolean areItemStackTagsEqualForCrafting(ItemStack slotItem, ItemStack recipeItem) {
        if (recipeItem == null || slotItem == null) return false;
        if (recipeItem.hasTag() && !slotItem.hasTag()) return false;
        if (!recipeItem.hasTag()) return true;
        
        CompoundTag recipeTag = recipeItem.getTag();
        CompoundTag slotTag = slotItem.getTag();
        
        for (String key : recipeTag.getAllKeys()) {
            if (!slotTag.contains(key)) return false;
            if (!slotTag.get(key).toString().equals(recipeTag.get(key).toString())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Insert a stack into an inventory at a position.
     */
    public static ItemStack insertStackAt(Level level, BlockPos pos, Direction side, ItemStack stack, boolean simulate) {
        IItemHandler inventory = getItemHandlerAt(level, pos, side);
        if (inventory != null) {
            return ItemHandlerHelper.insertItemStacked(inventory, stack, simulate);
        }
        return stack;
    }
    
    /**
     * Check how much room is available for a stack.
     */
    public static ItemStack hasRoomFor(Level level, BlockPos pos, Direction side, ItemStack stack) {
        ItemStack testStack = insertStackAt(level, pos, side, stack.copy(), true);
        if (testStack.isEmpty()) {
            return stack.copy();
        }
        ItemStack result = stack.copy();
        result.setCount(stack.getCount() - testStack.getCount());
        return result;
    }
    
    /**
     * Check if there's room for at least some of the stack.
     */
    public static boolean hasRoomForSome(Level level, BlockPos pos, Direction side, ItemStack stack) {
        ItemStack testStack = insertStackAt(level, pos, side, stack.copy(), true);
        return stack.isEmpty() || testStack.getCount() != stack.getCount();
    }
    
    /**
     * Check if there's room for the entire stack.
     */
    public static boolean hasRoomForAll(Level level, BlockPos pos, Direction side, ItemStack stack) {
        return insertStackAt(level, pos, side, stack.copy(), true).isEmpty();
    }
    
    /**
     * Count total matching items in an inventory.
     */
    public static int countTotalItemsIn(IItemHandler inventory, ItemStack stack, InvFilter filter) {
        int count = 0;
        if (inventory != null) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack slotStack = inventory.getStackInSlot(slot);
                if (areItemStacksEqualWithFilter(stack, slotStack, filter)) {
                    count += slotStack.getCount();
                }
            }
        }
        return count;
    }
    
    /**
     * Count total matching items in an inventory at a position.
     */
    public static int countTotalItemsIn(Level level, BlockPos pos, Direction side, ItemStack stack, InvFilter filter) {
        return countTotalItemsIn(getItemHandlerAt(level, pos, side), stack, filter);
    }
    
    /**
     * Compare two item stacks with filter options.
     * This is the core comparison method used throughout the inventory system.
     */
    public static boolean areItemStacksEqualWithFilter(ItemStack stack0, ItemStack stack1, InvFilter filter) {
        // Handle null/empty cases
        if (stack0 == null && stack1 != null) return false;
        if (stack0 != null && stack1 == null) return false;
        if (stack0 == null) return true;
        if (stack0.isEmpty() && !stack1.isEmpty()) return false;
        if (!stack0.isEmpty() && stack1.isEmpty()) return false;
        if (stack0.isEmpty()) return true;
        
        // Mod-based matching
        if (filter.useMod) {
            String mod0 = stack0.getItem().builtInRegistryHolder().key().location().getNamespace();
            String mod1 = stack1.getItem().builtInRegistryHolder().key().location().getNamespace();
            return mod0.equals(mod1);
        }
        
        // Tag-based matching (replacement for ore dictionary)
        if (filter.useOre) {
            if (containsTagMatch(stack0, stack1)) {
                return true;
            }
        }
        
        // NBT comparison
        boolean nbtMatch = true;
        if (!filter.igNBT) {
            if (filter.relaxedNBT) {
                nbtMatch = areItemStackTagsEqualRelaxed(stack0, stack1);
            } else {
                nbtMatch = ItemStack.isSameItemSameTags(stack0, stack1) ||
                           (!stack0.hasTag() && !stack1.hasTag());
            }
        }
        
        // Damage comparison
        boolean damageMatch = filter.igDmg ||
                stack0.getDamageValue() == stack1.getDamageValue() ||
                !stack0.isDamageableItem();
        
        return stack0.is(stack1.getItem()) && damageMatch && nbtMatch;
    }
}
