package thaumcraft.init;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Item;

/**
 * Carries the pending registration id for an item from the DeferredRegister
 * registration lambda into the item's constructor, where it is applied to the
 * {@link Item.Properties} via {@code setId(...)} before the Item
 * super-constructor runs. MC 26.2 requires {@code Properties.id} to be set
 * before {@code Item.<init>} runs (the Item constructor calls
 * {@code itemIdOrThrow()} which throws "Item id not set" when the id is null).
 *
 * <p>Items constructed outside of registration (e.g. data-driven paths) find
 * no pending id: {@link #take()} returns null and
 * {@link #id(Item.Properties)} becomes a no-op (setId(null) merely assigns
 * null, matching the pre-26.2 behaviour).
 */
public class ItemRegistration {

    private static final ThreadLocal<ResourceKey<Item>> PENDING = new ThreadLocal<>();

    private ItemRegistration() {
    }

    /**
     * Stores the id that the next item constructor in this thread should use.
     */
    public static void set(ResourceKey<Item> key) {
        PENDING.set(key);
    }

    /**
     * Returns and clears the pending id (null when not registering an item).
     */
    public static ResourceKey<Item> take() {
        ResourceKey<Item> key = PENDING.get();
        PENDING.remove();
        return key;
    }

    /**
     * Clears any pending id without returning it. Safety net for suppliers that
     * construct an item whose constructor does not consume the pending id.
     */
    public static void clear() {
        PENDING.remove();
    }

    /**
     * Applies the pending registration id to the given properties, if any.
     */
    public static Item.Properties id(Item.Properties properties) {
        return properties.setId(take());
    }
}
