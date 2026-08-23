package thaumcraft.common.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

/**
 * Base class for items that can contain essentia (phials, bottles, etc.).
 * Stores aspect data in NBT.
 */
public class ItemEssentiaContainer extends ItemTC implements IEssentiaContainerItem {

    protected final int capacity;

    public ItemEssentiaContainer(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public ItemEssentiaContainer(int capacity) {
        super(new Properties().stacksTo(64));
        this.capacity = capacity;
    }

    /**
     * Get the capacity of this container.
     */
    public int getCapacity() {
        return capacity;
    }

    @Override
    public AspectList getAspects(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            AspectList aspects = new AspectList();
            aspects.readFromNBT(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
            return aspects.size() > 0 ? aspects : null;
        }
        return null;
    }

    @Override
    public void setAspects(ItemStack stack, AspectList aspects) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        aspects.writeToNBT(tag);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    @Override
    public boolean ignoreContainedAspects() {
        return false;
    }

    /**
     * Check if this container is empty.
     */
    public boolean isEmpty(ItemStack stack) {
        AspectList aspects = getAspects(stack);
        return aspects == null || aspects.size() == 0;
    }

    /**
     * Get the aspect stored in this container (if single aspect).
     */
    public Aspect getAspect(ItemStack stack) {
        AspectList aspects = getAspects(stack);
        if (aspects != null && aspects.size() > 0) {
            return aspects.getAspects()[0];
        }
        return null;
    }

    /**
     * Get the amount of the stored aspect.
     */
    public int getAmount(ItemStack stack) {
        AspectList aspects = getAspects(stack);
        if (aspects != null && aspects.size() > 0) {
            Aspect aspect = aspects.getAspects()[0];
            return aspects.getAmount(aspect);
        }
        return 0;
    }

    /**
     * Create a filled container with the given aspect.
     */
    public ItemStack createFilled(Aspect aspect, int amount) {
        ItemStack stack = new ItemStack(this);
        setAspects(stack, new AspectList().add(aspect, amount));
        return stack;
    }

    /**
     * Create a container filled to capacity with the given aspect.
     */
    public ItemStack createFull(Aspect aspect) {
        return createFilled(aspect, capacity);
    }
}
