package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Vis Generator tile entity - converts aura vis into Forge Energy (RF/FE).
 * Drains vis from the local aura and outputs energy to adjacent machines.
 */
public class TileVisGenerator extends TileThaumcraft implements EnergyHandler {

    private static final int CAPACITY = 1000;
    private static final int MAX_EXTRACT = 20;
    private static final float VIS_PER_RECHARGE = 1.0f;
    private static final int ENERGY_PER_VIS = 1000;

    protected int energy = 0;

    public TileVisGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileVisGenerator(BlockPos pos, BlockState state) {
        this(ModBlockEntities.VIS_GENERATOR.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(ValueOutput output) {
        super.writeSyncNBT(output);
        output.putInt("Energy", energy);
    }

    @Override
    protected void readSyncNBT(ValueInput input) {
        super.readSyncNBT(input);
        energy = input.getIntOr("Energy", 0);
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileVisGenerator tile) {
        if (!tile.isEnabled(state)) return;

        // Recharge from vis when empty
        tile.recharge();

        // Output energy to adjacent machines
        tile.outputEnergy();
    }

    /**
     * Drain vis from the aura to generate energy.
     */
    private void recharge() {
        if (energy > 0) return;
        if (level == null) return;

        float vis = AuraHandler.drainVis(level, worldPosition, VIS_PER_RECHARGE, false);
        if (vis > 0) {
            energy = (int) (vis * ENERGY_PER_VIS);
            setChanged();
            syncTile(false);
        }
    }

    /**
     * Push energy to adjacent energy receivers.
     */
    private void outputEnergy() {
        if (energy <= 0) return;
        if (level == null) return;

        Direction facing = getFacing();
        BlockPos targetPos = worldPosition.relative(facing);

        var handler = level.getCapability(Capabilities.Energy.BLOCK, targetPos, level.getBlockState(targetPos), null, facing.getOpposite());
        if (handler != null) {
            int toExtract = Math.min(energy, MAX_EXTRACT);
            int accepted = handler.insert(toExtract, null);
            if (accepted > 0) {
                energy -= accepted;
                setChanged();
                if (energy == 0) {
                    syncTile(false);
                }
            }
        }
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    private boolean isEnabled(BlockState state) {
        if (state.hasProperty(BlockStateProperties.ENABLED)) {
            return state.getValue(BlockStateProperties.ENABLED);
        }
        return true;
    }

    // ==================== EnergyHandler ====================

    @Override
    public long getAmountAsLong() {
        return energy;
    }

    @Override
    public long getCapacityAsLong() {
        return CAPACITY;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        return 0; // Cannot receive energy
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        int extracted = Math.min(energy, Math.min(this.MAX_EXTRACT, amount));
        energy -= extracted;
        setChanged();
        return extracted;
    }

    /**
     * @return the current stored energy amount
     */
    public int getEnergyStored() {
        return energy;
    }
}
