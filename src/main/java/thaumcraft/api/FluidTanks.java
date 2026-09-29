package thaumcraft.api;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Adapter utilities for the 26.3 transfer-based fluid API (ResourceHandler&lt;FluidResource&gt;),
 * exposing the familiar FluidTank-style operations used throughout the 1.12 port.
 *
 * <p>All mutating operations run inside a transaction: if a transaction is already open on this
 * thread, a nested transaction is used (so the outer caller retains commit control); otherwise a
 * fresh root transaction is opened. With {@code simulate = true} the transaction is rolled back.
 */
public final class FluidTanks {

    @FunctionalInterface
    private interface TxOp<R> {
        R run(Transaction tx);
    }

    private FluidTanks() {
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

    /** Fluid amount in slot 0. */
    public static int getAmount(ResourceHandler<FluidResource> tank) {
        return (int) tank.getAmountAsLong(0);
    }

    /** The current fluid stack in slot 0 (empty stack if none). */
    public static FluidStack getStack(ResourceHandler<FluidResource> tank) {
        if (tank.getAmountAsLong(0) <= 0) {
            return FluidStack.EMPTY;
        }
        return tank.getResource(0).toStack((int) tank.getAmountAsLong(0));
    }

    /** The current fluid in slot 0 (Fluids.EMPTY if none). */
    public static Fluid getFluid(ResourceHandler<FluidResource> tank) {
        if (tank.getAmountAsLong(0) <= 0) {
            return Fluids.EMPTY;
        }
        return tank.getResource(0).value();
    }

    /**
     * Fills the tank with the given fluid stack.
     * @return the amount actually filled
     */
    public static int fill(ResourceHandler<FluidResource> tank, FluidStack toFill, boolean simulate) {
        int amount = toFill.getAmount();
        if (amount <= 0) {
            return 0;
        }
        return inTransaction(simulate, tx -> tank.insert(0, FluidResource.of(toFill), amount, tx));
    }

    /**
     * Drains up to the given amount of the tank's current fluid (any type).
     * @return the drained stack (empty if none)
     */
    public static FluidStack drain(ResourceHandler<FluidResource> tank, int amount, boolean simulate) {
        if (amount <= 0 || tank.getAmountAsLong(0) <= 0) {
            return FluidStack.EMPTY;
        }
        FluidResource current = tank.getResource(0);
        return inTransaction(simulate, tx -> {
            int drained = tank.extract(0, current, amount, tx);
            return drained <= 0 ? FluidStack.EMPTY : current.toStack(drained);
        });
    }

    /**
     * Drains the given fluid (must match the tank's contents).
     * @return the drained stack (empty if none)
     */
    public static FluidStack drain(ResourceHandler<FluidResource> tank, FluidStack toDrain, boolean simulate) {
        int amount = toDrain.getAmount();
        if (amount <= 0 || tank.getAmountAsLong(0) <= 0) {
            return FluidStack.EMPTY;
        }
        FluidResource current = tank.getResource(0);
        if (!current.getFluid().isSame(toDrain.getFluid())) {
            return FluidStack.EMPTY;
        }
        return inTransaction(simulate, tx -> {
            int drained = tank.extract(0, current, amount, tx);
            return drained <= 0 ? FluidStack.EMPTY : toDrain.copyWithAmount(drained);
        });
    }

    /** True if the tank accepts the given fluid type. */
    public static boolean isFluidValid(ResourceHandler<FluidResource> tank, FluidStack stack) {
        return tank.isValid(0, FluidResource.of(stack));
    }
}
