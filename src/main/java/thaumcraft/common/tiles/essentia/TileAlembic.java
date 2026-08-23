package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Alembic tile entity - distills essentia from crucible/smelter below.
 * Stacks on top of furnaces/crucibles and collects specific aspects.
 */
public class TileAlembic extends TileThaumcraft implements IAspectContainer, IEssentiaTransport {

    public static final int MAX_AMOUNT = 128;

    public Aspect aspect = null;
    public Aspect aspectFilter = null;
    public int amount = 0;
    public int facing = Direction.DOWN.ordinal();
    public boolean aboveFurnace = false;
    
    private Direction facingDir = null;

    public TileAlembic(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileAlembic(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ALEMBIC.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(ValueOutput output) {
        super.writeSyncNBT(output);
        if (aspect != null) {
            output.putString("Aspect", aspect.getTag());
        }
        if (aspectFilter != null) {
            output.putString("AspectFilter", aspectFilter.getTag());
        }
        output.putShort("Amount", (short) amount);
        output.putByte("Facing", (byte) facing);
    }

    @Override
    protected void readSyncNBT(ValueInput input) {
        super.readSyncNBT(input);
        aspect = Aspect.getAspect(input.getStringOr("Aspect", ""));
        aspectFilter = Aspect.getAspect(input.getStringOr("AspectFilter", ""));
        amount = (short) input.getShortOr("Amount", (short)0);
        facing = input.getByteOr("Facing", (byte)0);
        facingDir = Direction.values()[facing];
    }

    // ==================== IAspectContainer ====================

    @Override
    public AspectList getAspects() {
        return (aspect != null && amount > 0) ? new AspectList().add(aspect, amount) : new AspectList();
    }

    @Override
    public void setAspects(AspectList aspects) {
        // Alembics don't allow direct setting
    }

    @Override
    public int addToContainer(Aspect tag, int amt) {
        if (aspectFilter != null && tag != aspectFilter) {
            return amt;
        }
        if ((amount < MAX_AMOUNT && tag == aspect) || amount == 0) {
            aspect = tag;
            int added = Math.min(amt, MAX_AMOUNT - amount);
            amount += added;
            amt -= added;
        }
        markDirtyAndSync();
        return amt;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amt) {
        if (amount == 0 || aspect == null) {
            aspect = null;
            amount = 0;
        }
        if (aspect != null && amount >= amt && tag == aspect) {
            amount -= amt;
            if (amount <= 0) {
                aspect = null;
                amount = 0;
            }
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amt) {
        return amount >= amt && tag == aspect;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        return amount > 0 && aspect != null && list.getAmount(aspect) > 0;
    }

    @Override
    public int containerContains(Aspect tag) {
        return (tag == aspect) ? amount : 0;
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return aspectFilter == null || tag == aspectFilter;
    }

    // ==================== IEssentiaTransport ====================

    @Override
    public boolean isConnectable(Direction face) {
        return face != Direction.values()[facing] && face != Direction.DOWN;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return false; // Alembics only receive from below (special handling)
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face != Direction.values()[facing] && face != Direction.DOWN;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Alembics don't have suction
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return 0;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return aspect;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return amount;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return (canOutputTo(face) && takeFromContainer(aspect, amount)) ? amount : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return 0; // Alembics don't accept essentia through transport
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    // Uses default isBlocked() from interface

    // ==================== Static Helpers ====================

    /**
     * Process alembics stacked above a position.
     * Called by crucibles/smelters to push essentia up into alembics.
     * 
     * @param level The world
     * @param pos Position of the source (crucible/smelter)
     * @param aspect The aspect to try to add
     * @return true if the aspect was successfully added to an alembic
     */
    public static boolean processAlembics(Level level, BlockPos pos, Aspect aspect) {
        // First pass: find alembics that already have this aspect
        int deep = 1;
        while (true) {
            BlockEntity be = level.getBlockEntity(pos.above(deep));
            if (be instanceof TileAlembic alembic) {
                if (alembic.amount > 0 && alembic.aspect == aspect && alembic.addToContainer(aspect, 1) == 0) {
                    return true;
                }
                deep++;
            } else {
                break;
            }
        }

        // Second pass: find any alembic that can accept this aspect
        deep = 1;
        while (true) {
            BlockEntity be = level.getBlockEntity(pos.above(deep));
            if (be instanceof TileAlembic alembic) {
                if ((alembic.aspectFilter == null || alembic.aspectFilter == aspect) && alembic.addToContainer(aspect, 1) == 0) {
                    return true;
                }
                deep++;
            } else {
                break;
            }
        }

        return false;
    }

    // ==================== Getters/Setters ====================

    public Aspect getAspect() {
        return aspect;
    }

    public int getAmount() {
        return amount;
    }

    public Aspect getAspectFilter() {
        return aspectFilter;
    }

    public void setAspectFilter(Aspect filter) {
        this.aspectFilter = filter;
        markDirtyAndSync();
    }

    public int getFacing() {
        return facing;
    }

    public void setFacing(int facing) {
        this.facing = facing;
        this.facingDir = Direction.values()[facing];
        markDirtyAndSync();
    }

    // ==================== Rendering ====================

    /**
     * Custom render bounding box for rendering.
     * Note: In 1.20.1, this is accessed via TESR if needed.
     */
    public AABB getCustomRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 0.1, worldPosition.getY() - 0.1, worldPosition.getZ() - 0.1,
                worldPosition.getX() + 1.1, worldPosition.getY() + 1.1, worldPosition.getZ() + 1.1
        );
    }
}
