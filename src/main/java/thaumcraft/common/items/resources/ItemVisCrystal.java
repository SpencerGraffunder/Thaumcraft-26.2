package thaumcraft.common.items.resources;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

/**
 * Vis Crystal - Crystallized vis that contains a single primal aspect.
 * Used as crafting ingredients and can be broken down for essentia.
 * 
 * Unlike the old ItemCrystalEssence, each aspect has its own item type,
 * so no NBT storage is needed. This prevents freezing issues and simplifies the code.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class ItemVisCrystal extends Item implements IEssentiaContainerItem {
    
    private final Aspect aspect;
    
    public ItemVisCrystal(Aspect aspect) {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties().stacksTo(64)));
        this.aspect = aspect;
    }
    
    /**
     * Get the aspect this crystal contains.
     */
    public Aspect getAspect() {
        return aspect;
    }
    
    /**
     * Get the color for tinting this crystal item.
     */
    public int getColor() {
        return aspect != null ? aspect.getColor() : 0xFFFFFF;
    }
    
    // ==================== IEssentiaContainerItem ====================
    
    @Override
    public AspectList getAspects(ItemStack stack) {
        if (aspect != null) {
            return new AspectList().add(aspect, 1);
        }
        return null;
    }
    
    @Override
    public void setAspects(ItemStack stack, AspectList aspects) {
        // No-op - aspect is fixed by item type
    }
    
    @Override
    public boolean ignoreContainedAspects() {
        return false;
    }
    
    // ==================== Display ====================
    
    @Override
    public Component getName(ItemStack stack) {
        if (aspect != null) {
            return Component.translatable("item.thaumcraft.vis_crystal", aspect.getName());
        }
        return super.getName(stack);
    }
    
    // ==================== Static Helper Methods ====================
    
    /**
     * Get the aspect from a vis crystal item stack.
     */
    public static Aspect getAspectFromStack(ItemStack stack) {
        if (stack.getItem() instanceof ItemVisCrystal crystal) {
            return crystal.getAspect();
        }
        return null;
    }
    
    /**
     * Check if two crystal stacks contain the same aspect.
     */
    public static boolean isSameAspect(ItemStack stack1, ItemStack stack2) {
        Aspect a1 = getAspectFromStack(stack1);
        Aspect a2 = getAspectFromStack(stack2);
        if (a1 == null || a2 == null) return false;
        return a1.equals(a2);
    }
}
