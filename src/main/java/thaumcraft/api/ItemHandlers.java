package thaumcraft.api;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 26.3: NeoForge replaced the old {@code net.neoforged.neoforge.items.IItemHandler}
 * API with the transaction-based {@code ResourceHandler<T>} framework
 * ({@code net.neoforged.neoforge.transfer}). This class re-exposes the old-style
 * slot methods that most of Thaumcraft's code used (getSlots / getStackInSlot /
 * insertItem / extractItem / setStackInSlot / insertItemStacked) on top of
 * {@code ResourceHandler<ItemResource>} so tiles, seals and helpers can keep
 * their existing logic. The "simulate" flag is emulated with a
 * {@link Transaction} that is rolled back when not committed.
 *
 * <p>Transactions are opened nesting-safely: if a transaction is already open on
 * this thread (e.g. the caller is inside a larger interaction transaction), a
 * nested transaction is used and only that layer is committed/rolled back;
 * otherwise a fresh root transaction is opened.
 */
public final class ItemHandlers {

    @FunctionalInterface
    private interface TxOp<R> {
        R run(Transaction tx);
    }

    private ItemHandlers() {
    }

    private static <R> R inTransaction(boolean simulate, TxOp<R> op) {
        TransactionContext current = Transaction.getCurrentOpenedTransaction();
        try (Transaction tx = Transaction.open(current)) {
            R result = op.run(tx);
            if (!simulate) {
                tx.commit();
            }
            return result;
        }
    }

    /** Equivalent of IItemHandler.getSlots(). */
    public static int getSlots(ResourceHandler<ItemResource> handler) {
        return handler == null ? 0 : handler.size();
    }

    /** Equivalent of IItemHandler.getStackInSlot(slot). */
    public static ItemStack getStackInSlot(ResourceHandler<ItemResource> handler, int slot) {
        if (handler == null) {
            return ItemStack.EMPTY;
        }
        long amount = handler.getAmountAsLong(slot);
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemResource resource = handler.getResource(slot);
        if (resource == null || resource.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack((int) Math.min(amount, Integer.MAX_VALUE));
    }

    /** Equivalent of IItemHandlerModifiable.setStackInSlot(slot, stack). */
    public static void setStackInSlot(ResourceHandler<ItemResource> handler, int slot, ItemStack stack) {
        if (handler == null) {
            return;
        }
        if (handler instanceof StacksResourceHandler<?, ItemResource> stacks) {
            stacks.set(slot, ItemResource.of(stack), stack.getCount());
            return;
        }
        // Fallback for non-stack handlers: clear the slot, then insert.
        inTransaction(false, tx -> {
            long current = handler.getAmountAsLong(slot);
            if (current > 0) {
                ItemResource resource = handler.getResource(slot);
                if (resource != null && !resource.isEmpty()) {
                    handler.extract(slot, resource, (int) Math.min(current, Integer.MAX_VALUE), tx);
                }
            }
            if (!stack.isEmpty()) {
                handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
            }
            return null;
        });
    }

    /** Equivalent of IItemHandler.insertItem(slot, stack, simulate); returns items inserted. */
    public static int insertItem(ResourceHandler<ItemResource> handler, int slot, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return 0;
        }
        return inTransaction(simulate, tx -> handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx));
    }

    /** Equivalent of IItemHandler.extractItem(slot, amount, simulate). */
    public static ItemStack extractItem(ResourceHandler<ItemResource> handler, int slot, int amount, boolean simulate) {
        if (handler == null || amount <= 0) {
            return ItemStack.EMPTY;
        }
        return inTransaction(simulate, tx -> {
            long current = handler.getAmountAsLong(slot);
            if (current <= 0) {
                return ItemStack.EMPTY;
            }
            ItemResource resource = handler.getResource(slot);
            if (resource == null || resource.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int extracted = handler.extract(slot, resource, amount, tx);
            return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
        });
    }

    /** Equivalent of IItemHandler.insertItem(slot, stack, simulate); returns the uninserted remainder. */
    public static ItemStack insertItemSlot(ResourceHandler<ItemResource> handler, int slot, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        int inserted = insertItem(handler, slot, stack, simulate);
        int remaining = stack.getCount() - inserted;
        return remaining <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }

    /**
     * Equivalent of ItemHandlerHelper.insertItemStacked(handler, stack, simulate):
     * inserts into every slot that can accept the stack (stacking onto matching
     * stacks and filling empties), returning the uninserted remainder.
     */
    public static ItemStack insertItemStacked(ResourceHandler<ItemResource> handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        return inTransaction(simulate, tx -> {
            int remaining = stack.getCount();
            ItemResource resource = ItemResource.of(stack);
            int slots = handler.size();
            for (int i = 0; i < slots && remaining > 0; i++) {
                remaining -= handler.insert(i, resource, remaining, tx);
            }
            return remaining <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
        });
    }

    /**
     * Equivalent of ItemHandlerHelper.insertItemStacked(handler, stack, simulate, startSlot)
     * with a slot filter: only slots where slotTest passes are considered.
     * Returns the uninserted remainder.
     */
    public static ItemStack insertItemStacked(ResourceHandler<ItemResource> handler, ItemStack stack, boolean simulate, int startSlot, int endSlot) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        return inTransaction(simulate, tx -> {
            int remaining = stack.getCount();
            ItemResource resource = ItemResource.of(stack);
            int slots = handler.size();
            int end = Math.min(endSlot, slots);
            for (int i = Math.max(0, startSlot); i < end && remaining > 0; i++) {
                remaining -= handler.insert(i, resource, remaining, tx);
            }
            return remaining <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
        });
    }
}
