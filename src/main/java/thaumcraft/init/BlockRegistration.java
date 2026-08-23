package thaumcraft.init;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Carries the pending registration id for a block from the DeferredRegister
 * registration lambda into the block's constructor, where it is applied to the
 * {@link BlockBehaviour.Properties} via {@code setId(...)} before the Block
 * super-constructor runs. MC 26.2 requires {@code Properties.id} to be set
 * before {@code BlockBehaviour.<init>} (it calls {@code effectiveDrops()}
 * which throws "Block id not set" when the id is null).
 *
 * <p>Blocks constructed outside of registration (e.g. worldgen or block codec
 * paths) find no pending id: {@link #take()} returns null and
 * {@link #id(BlockBehaviour.Properties)} becomes a no-op (setId(null) merely
 * assigns null, matching the pre-26.2 behaviour).
 */
public class BlockRegistration {

    private static final ThreadLocal<ResourceKey<Block>> PENDING = new ThreadLocal<>();

    private BlockRegistration() {
    }

    /**
     * Stores the id that the next block constructor in this thread should use.
     */
    public static void set(ResourceKey<Block> key) {
        PENDING.set(key);
    }

    /**
     * Returns and clears the pending id (null when not registering a block).
     */
    public static ResourceKey<Block> take() {
        ResourceKey<Block> key = PENDING.get();
        PENDING.remove();
        return key;
    }

    /**
     * Clears any pending id without returning it. Safety net for suppliers that
     * construct a block whose constructor does not consume the pending id.
     */
    public static void clear() {
        PENDING.remove();
    }

    /**
     * Applies the pending registration id to the given properties, if any.
     */
    public static BlockBehaviour.Properties id(BlockBehaviour.Properties properties) {
        return properties.setId(take());
    }
}
