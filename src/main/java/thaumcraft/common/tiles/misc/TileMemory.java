package thaumcraft.common.tiles.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * TileMemory - Base tile entity that remembers the original block state.
 * Used by blocks that temporarily replace other blocks (like portable hole).
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class TileMemory extends BlockEntity {
    
    /** The original block state that was replaced */
    protected BlockState oldBlock = Blocks.AIR.defaultBlockState();
    
    /** Optional: stored tile entity data from the replaced block */
    protected CompoundTag tileEntityCompound = null;
    
    public TileMemory(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public TileMemory(BlockEntityType<?> type, BlockPos pos, BlockState state, BlockState originalBlock) {
        super(type, pos, state);
        this.oldBlock = originalBlock;
    }
    
    /**
     * Get the original block state that was replaced.
     */
    public BlockState getOldBlock() {
        return oldBlock;
    }
    
    /**
     * Set the original block state.
     */
    public void setOldBlock(BlockState state) {
        this.oldBlock = state;
        setChanged();
    }
    
    /**
     * Get the stored tile entity data, if any.
     */
    public CompoundTag getTileEntityCompound() {
        return tileEntityCompound;
    }
    
    /**
     * Set the stored tile entity data.
     */
    public void setTileEntityCompound(CompoundTag tag) {
        this.tileEntityCompound = tag;
        setChanged();
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // Save block state using NbtUtils
        output.put("oldBlock", NbtUtils.writeBlockState(oldBlock));
        if (tileEntityCompound != null) {
            output.put("tileData", tileEntityCompound);
        }
    }
    
    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Load block state using NbtUtils
        if (input.contains("oldBlock")) {
            oldBlock = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), input.getCompoundOrEmpty("oldBlock"));
        } else {
            oldBlock = Blocks.AIR.defaultBlockState();
        }
        if (input.contains("tileData")) {
            tileEntityCompound = input.getCompoundOrEmpty("tileData");
        }
    }
}
