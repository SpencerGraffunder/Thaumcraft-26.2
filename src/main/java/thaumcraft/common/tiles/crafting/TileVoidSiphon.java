package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * TileVoidSiphon - Harvests void seeds from flux rifts.
 * 
 * When placed near flux rifts, slowly drains their energy to
 * produce void seeds. Essential for void metal production chain.
 */
public class TileVoidSiphon extends TileThaumcraftInventory implements WorldlyContainer {
    
    private static final int[] SLOTS = {0};
    public static final int PROGRESS_REQUIRED = 2000;
    
    private int counter = 0;
    public int progress = 0;
    
    public TileVoidSiphon(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }
    
    public TileVoidSiphon(BlockPos pos, BlockState state) {
        this(ModBlockEntities.VOID_SIPHON.get(), pos, state);
    }
    
    // ==================== Tick ====================
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileVoidSiphon tile) {
        tile.counter++;
        
        if (!tile.isEnabled()) return;
        
        if (tile.counter % 20 == 0 && tile.progress < PROGRESS_REQUIRED) {
            // Check if we can store more seeds
            ItemStack stack = tile.getItem(0);
            if (stack.isEmpty() || (stack.is(ModItems.VOID_SEED.get()) && stack.getCount() < stack.getMaxStackSize())) {
                // Find valid rifts and drain them
                List<EntityFluxRift> rifts = tile.getValidRifts();
                boolean didWork = false;
                
                for (EntityFluxRift rift : rifts) {
                    double d = Math.sqrt(rift.getRiftSize());
                    tile.progress += (int)d;
                    
                    // Destabilize the rift
                    rift.setRiftStability(rift.getRiftStability() - (float)(d / 15.0));
                    
                    // Occasionally shrink the rift
                    if (level.getRandom().nextInt(33) == 0) {
                        rift.setRiftSize(rift.getRiftSize() - 1);
                    }
                    
                    if (d >= 1.0) {
                        didWork = true;
                    }
                }
                
                // Send visual effect
                if (didWork && tile.counter % 40 == 0) {
                    level.blockEvent(pos, state.getBlock(), 5, tile.counter);
                }
                
                // Create void seeds when progress is full
                while (tile.progress >= PROGRESS_REQUIRED) {
                    stack = tile.getItem(0);
                    if (stack.isEmpty() || (stack.is(ModItems.VOID_SEED.get()) && stack.getCount() < stack.getMaxStackSize())) {
                        tile.progress -= PROGRESS_REQUIRED;
                        
                        if (stack.isEmpty()) {
                            tile.setItem(0, new ItemStack(ModItems.VOID_SEED.get()));
                        } else {
                            stack.grow(1);
                        }
                        tile.syncTile(false);
                        tile.setChanged();
                    } else {
                        break;
                    }
                }
            }
        }
    }
    
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileVoidSiphon tile) {
        // Client-side effects handled by block events
    }
    
    /**
     * Find flux rifts within range that can be drained.
     */
    private List<EntityFluxRift> getValidRifts() {
        List<EntityFluxRift> result = new ArrayList<>();
        List<EntityFluxRift> rifts = EntityUtils.getEntitiesInRange(level, worldPosition, 8.0, EntityFluxRift.class);
        
        for (EntityFluxRift rift : rifts) {
            if (rift.isRemoved()) continue;
            if (rift.getRiftSize() < 2) continue;
            
            // Check line of sight
            double x = worldPosition.getX() + 0.5;
            double y = worldPosition.getY() + 1;
            double z = worldPosition.getZ() + 0.5;
            
            Vec3 siphonPos = new Vec3(x, y, z);
            Vec3 riftPos = new Vec3(rift.getX(), rift.getY(), rift.getZ());
            Vec3 checkPos = siphonPos.add(riftPos.subtract(siphonPos).normalize());
            
            if (EntityUtils.canEntityBeSeen(rift, checkPos.x, checkPos.y, checkPos.z)) {
                result.add(rift);
            }
        }
        
        return result;
    }
    
    /**
     * Check if the siphon is enabled (not powered by redstone).
     */
    private boolean isEnabled() {
        return !level.hasNeighborSignal(worldPosition);
    }
    
    // ==================== Block Events ====================
    
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 5) {
            if (level != null && level.isClientSide()) {
                List<EntityFluxRift> rifts = getValidRifts();
                for (EntityFluxRift rift : rifts) {
                    FXDispatcher.INSTANCE.voidStreak(
                            rift.getX(), rift.getY(), rift.getZ(),
                            worldPosition.getX() + 0.5, worldPosition.getY() + 0.5625, worldPosition.getZ() + 0.5,
                            param, 0.04f);
                }
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putShort("progress", (short)progress);
    }
    
    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getShortOr("progress", (short)0);
    }
    
    // ==================== Inventory ====================
    
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ModItems.VOID_SEED.get());
    }
    
    // ==================== WorldlyContainer ====================
    
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }
    
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false; // Output only
    }
    
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }
    
    // ==================== Progress ====================
    
    /**
     * Get progress as a percentage (0-1).
     */
    public float getProgressPercent() {
        return (float)progress / PROGRESS_REQUIRED;
    }
    
    /**
     * Get raw progress value.
     */
    public int getProgress() {
        return progress;
    }
    
    /**
     * Set progress value (for client sync).
     */
    public void setProgress(int value) {
        progress = value;
    }
}
