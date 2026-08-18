package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemHandlerProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.neoforged.neoforge.energy.EnergyStorage;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Vis Generator tile entity - converts aura vis into Forge Energy (RF/FE).
 * Drains vis from the local aura and outputs energy to adjacent machines.
 */
public class TileVisGenerator extends TileThaumcraft implements IEnergyStorage {

    private static final int CAPACITY = 1000;
    private static final int MAX_EXTRACT = 20;
    private static final float VIS_PER_RECHARGE = 1.0f;
    private static final int ENERGY_PER_VIS = 1000;

    protected int energy = 0;
    
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> this);

    public TileVisGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileVisGenerator(BlockPos pos, BlockState state) {
        this(ModBlockEntities.VIS_GENERATOR.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putInt("Energy", energy);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        energy = tag.getInt("Energy");
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
        BlockEntity targetTile = level.getBlockEntity(targetPos);

        if (targetTile != null) {
            targetTile.getCapability(ForgeCapabilities.ENERGY, facing.getOpposite()).ifPresent(handler -> {
                if (handler.canReceive()) {
                    int toExtract = Math.min(energy, MAX_EXTRACT);
                    int accepted = handler.receiveEnergy(toExtract, false);
                    if (accepted > 0) {
                        energy -= accepted;
                        setChanged();
                        if (energy == 0) {
                            syncTile(false);
                        }
                    }
                }
            });
        }
    }

    private Direction getFacing() {
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

    // ==================== Capability ====================

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side == getFacing()) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
    }

    // ==================== IEnergyStorage ====================

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0; // Cannot receive energy
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = Math.min(energy, Math.min(this.MAX_EXTRACT, maxExtract));
        if (!simulate) {
            energy -= extracted;
            setChanged();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return CAPACITY;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
