package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.init.ModBlockEntities;

import java.util.Arrays;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * TileDioptra - Vis/Flux visualization device.
 * 
 * Displays a 13x13 chunk grid showing either vis or flux levels
 * in the surrounding area. Toggle between vis/flux display with redstone.
 */
public class TileDioptra extends TileThaumcraft {
    
    public int counter = 0;
    
    /** Grid data - 13x13 = 169 values, each 0-64 representing vis/flux level */
    public byte[] grid_amt = new byte[169];
    
    public TileDioptra(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Arrays.fill(grid_amt, (byte)0);
    }
    
    public TileDioptra(BlockPos pos, BlockState state) {
        this(ModBlockEntities.DIOPTRA.get(), pos, state);
    }
    
    // ==================== Tick ====================
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileDioptra tile) {
        tile.counter++;
        
        if (tile.counter % 20 == 0) {
            tile.updateGrid();
            tile.setChanged();
            tile.syncTile(false);
        }
    }
    
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileDioptra tile) {
        // Reset counter on client - used for animation
        tile.counter = 0;
    }
    
    /**
     * Update the grid data from surrounding aura chunks.
     */
    private void updateGrid() {
        Arrays.fill(grid_amt, (byte)0);
        
        boolean showVis = isShowingVis();
        int chunkX = worldPosition.getX() >> 4;
        int chunkZ = worldPosition.getZ() >> 4;
        
        for (int xx = 0; xx < 13; xx++) {
            for (int zz = 0; zz < 13; zz++) {
                AuraChunk ac = AuraHandler.getAuraChunk(level.dimension(), chunkX + xx - 6, chunkZ + zz - 6);
                if (ac != null) {
                    float value;
                    if (showVis) {
                        value = ac.getVis() / 500.0f * 64.0f;
                    } else {
                        value = ac.getFlux() / 500.0f * 64.0f;
                    }
                    grid_amt[xx + zz * 13] = (byte)Math.min(64, (int)value);
                }
            }
        }
    }
    
    /**
     * Check if showing vis (true) or flux (false).
     * Toggled by redstone signal.
     */
    private boolean isShowingVis() {
        return !level.hasNeighborSignal(worldPosition);
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void writeSyncNBT(ValueOutput output) {
        super.writeSyncNBT(output);
                int[] gridInt = new int[grid_amt.length];
        for (int i = 0; i < grid_amt.length; i++) {
            gridInt[i] = grid_amt[i];
        }
        output.putIntArray("grid_a", gridInt);
    }
    
    @Override
    protected void readSyncNBT(ValueInput input) {
        super.readSyncNBT(input);
        if (input.keySet().contains("grid_a")) {
            int[] data = input.getIntArray("grid_a").orElse(new int[0]);
            if (data.length == 169) {
                grid_amt = new byte[169];
                for (int i = 0; i < 169; i++) {
                    grid_amt[i] = (byte) data[i];
                }
            }
        }
    }
    
    // ==================== Rendering ====================
    
    public AABB getRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 0.3, worldPosition.getY() - 0.3, worldPosition.getZ() - 0.3,
                worldPosition.getX() + 1.3, worldPosition.getY() + 2.3, worldPosition.getZ() + 1.3);
    }
    
    /**
     * Get the value at a grid position.
     * @param x X coordinate (0-12)
     * @param z Z coordinate (0-12)
     * @return Value 0-64
     */
    public int getGridValue(int x, int z) {
        if (x < 0 || x >= 13 || z < 0 || z >= 13) return 0;
        return grid_amt[x + z * 13] & 0xFF;
    }
    
    /**
     * Check if currently displaying vis (vs flux).
     */
    public boolean isDisplayingVis() {
        return isShowingVis();
    }
    
    /**
     * Get the vis value at the center of the grid (this chunk).
     * Used for comparator output.
     */
    public int getVisAtCenter() {
        // Center is at index 84 (6 + 6*13)
        return grid_amt[84] & 0xFF;
    }
}
