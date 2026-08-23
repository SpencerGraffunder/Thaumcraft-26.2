package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.init.ModBlockEntities;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Filtered tube - only allows one aspect type to pass through.
 * The filter is set by applying a phial or crystal essence.
 */
public class TileTubeFilter extends TileTube implements IAspectContainer {

    public Aspect aspectFilter = null;

    public TileTubeFilter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileTubeFilter(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TUBE_FILTER.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(ValueOutput output) {
        super.writeSyncNBT(output);
        if (aspectFilter != null) {
            output.putString("AspectFilter", aspectFilter.getTag());
        }
    }

    @Override
    protected void readSyncNBT(ValueInput input) {
        super.readSyncNBT(input);
        aspectFilter = Aspect.getAspect(input.getStringOr("AspectFilter", ""));
    }

    // ==================== Suction Override ====================

    @Override
    protected void calculateSuction(Aspect filter, boolean restrict, boolean directional) {
        // Always use our filter when calculating suction
        super.calculateSuction(aspectFilter, restrict, directional);
    }

    // ==================== IAspectContainer ====================
    // The container interface is used for setting the filter via phials

    @Override
    public AspectList getAspects() {
        if (aspectFilter != null) {
            return new AspectList().add(aspectFilter, -1);
        }
        return null;
    }

    @Override
    public void setAspects(AspectList aspects) {
        // Not used
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return false;
    }

    @Override
    public int addToContainer(Aspect tag, int amount) {
        return 0;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return 0;
    }

    // ==================== Filter Management ====================

    public void setFilter(Aspect aspect) {
        this.aspectFilter = aspect;
        markDirtyAndSync();
    }

    public Aspect getFilter() {
        return aspectFilter;
    }

    public void clearFilter() {
        this.aspectFilter = null;
        markDirtyAndSync();
    }
}
