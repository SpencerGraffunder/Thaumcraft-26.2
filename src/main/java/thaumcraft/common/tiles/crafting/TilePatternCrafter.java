package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.ItemHandlerProvider;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModSounds;

import java.util.Optional;
import java.util.Random;

/**
 * TilePatternCrafter - Automated crafting device.
 * 
 * Takes items from an inventory above, applies them in a pattern
 * to a crafting grid, and outputs the result below.
 * 
 * Patterns (type):
 * 0 - Full 3x3 grid (9 items)
 * 1 - Single item (1 item)
 * 2 - Horizontal 2 items (top row)
 * 3 - Vertical 2 items (left column)
 * 4 - 2x2 square (4 items)
 * 5 - Horizontal 3 items (top row)
 * 6 - Vertical 3 items (left column)
 * 7 - 2x3 horizontal (6 items)
 * 8 - 3x2 vertical (6 items)
 * 9 - Hollow 3x3 (8 items, center empty)
 */
public class TilePatternCrafter extends TileThaumcraft {
    
    public byte type = 0;
    private int count;
    private float power = 0.0f;
    
    // Client-side rotation animation
    public float rot = 0;
    public float rp = 0;
    public int rotTicks = 0;
    
    private ItemStack outStack = ItemStack.EMPTY;
    private CraftingContainer craftMatrix;
    
    public TilePatternCrafter(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.count = new Random(System.currentTimeMillis()).nextInt(20);
        initCraftMatrix();
    }
    
    public TilePatternCrafter(BlockPos pos, BlockState state) {
        this(ModBlockEntities.PATTERN_CRAFTER.get(), pos, state);
    }
    
    private void initCraftMatrix() {
        // Create a dummy crafting container
        this.craftMatrix = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }
            
            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        }, 3, 3);
    }
    
    // ==================== Tick ====================
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, TilePatternCrafter tile) {
        tile.tickServer();
    }
    
    public static void clientTick(Level level, BlockPos pos, BlockState state, TilePatternCrafter tile) {
        tile.tickClient();
    }
    
    private void tickClient() {
        if (rotTicks > 0) {
            rotTicks--;
            if (rotTicks % Math.max(1, (int)rp) == 0) {
                level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        ModSounds.CLACK.get(), SoundSource.BLOCKS, 0.2f, 1.7f, false);
            }
            rp++;
        } else {
            rp *= 0.8f;
        }
        rot += rp;
    }
    
    private void tickServer() {
        count++;
        if (count % 20 != 0 || !isEnabled()) {
            return;
        }
        
        // Drain vis if needed
        if (power <= 0.0f) {
            power += AuraHelper.drainVis(level, worldPosition, 5.0f, false);
        }
        
        // Get input and output inventories
        IItemHandler above = getItemHandler(worldPosition.above(), Direction.DOWN);
        IItemHandler below = getItemHandler(worldPosition.below(), Direction.UP);
        
        if (above == null || below == null) {
            return;
        }
        
        int amt = getPatternItemCount();
        
        // Search for items to craft
        for (int a = 0; a < above.getSlots(); a++) {
            ItemStack testStack = above.getStackInSlot(a).copy();
            if (testStack.isEmpty()) continue;
            
            testStack.setCount(amt);
            
            // Check if we can extract enough items
            if (!canExtractStack(above, testStack)) continue;
            
            // Try to craft
            if (!craft(testStack)) continue;
            
            // Check if we have power
            if (power < 1.0f) continue;
            
            // Check if we can insert output
            if (!ItemHandlerHelper.insertItem(below, outStack.copy(), true).isEmpty()) continue;
            
            // Check if we can insert crafting byproducts
            boolean canInsertByproducts = true;
            for (int i = 0; i < 9; i++) {
                ItemStack byproduct = craftMatrix.getItem(i);
                if (!byproduct.isEmpty() && !ItemHandlerHelper.insertItem(below, byproduct.copy(), true).isEmpty()) {
                    canInsertByproducts = false;
                    break;
                }
            }
            
            if (!canInsertByproducts) continue;
            
            // Do the craft!
            ItemHandlerHelper.insertItem(below, outStack.copy(), false);
            
            // Insert byproducts
            for (int i = 0; i < 9; i++) {
                ItemStack byproduct = craftMatrix.getItem(i);
                if (!byproduct.isEmpty()) {
                    ItemHandlerHelper.insertItem(below, byproduct.copy(), false);
                }
            }
            
            // Extract items from above
            extractStack(above, testStack);
            
            // Trigger animation
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
            power--;
            break;
        }
    }
    
    private IItemHandler getItemHandler(BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }
    
    private boolean canExtractStack(IItemHandler handler, ItemStack stack) {
        int needed = stack.getCount();
        int found = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack slot = handler.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(slot, stack)) {
                found += slot.getCount();
                if (found >= needed) return true;
            }
        }
        return false;
    }
    
    private void extractStack(IItemHandler handler, ItemStack stack) {
        int remaining = stack.getCount();
        for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
            ItemStack extracted = handler.extractItem(i, remaining, false);
            remaining -= extracted.getCount();
        }
    }
    
    private int getPatternItemCount() {
        return switch (type) {
            case 0 -> 9;
            case 1 -> 1;
            case 2, 3 -> 2;
            case 4 -> 4;
            case 5, 6 -> 3;
            case 7, 8 -> 6;
            case 9 -> 8;
            default -> 9;
        };
    }
    
    private boolean craft(ItemStack inStack) {
        outStack = ItemStack.EMPTY;
        
        // Clear craft matrix
        for (int i = 0; i < 9; i++) {
            craftMatrix.setItem(i, ItemStack.EMPTY);
        }
        
        // Fill based on pattern type
        ItemStack single = inStack.copyWithCount(1);
        switch (type) {
            case 0 -> { // Full 3x3
                for (int a = 0; a < 9; a++) {
                    craftMatrix.setItem(a, single.copy());
                }
            }
            case 1 -> { // Single
                craftMatrix.setItem(0, single.copy());
            }
            case 2 -> { // Horizontal 2
                craftMatrix.setItem(0, single.copy());
                craftMatrix.setItem(1, single.copy());
            }
            case 3 -> { // Vertical 2
                craftMatrix.setItem(0, single.copy());
                craftMatrix.setItem(3, single.copy());
            }
            case 4 -> { // 2x2
                craftMatrix.setItem(0, single.copy());
                craftMatrix.setItem(1, single.copy());
                craftMatrix.setItem(3, single.copy());
                craftMatrix.setItem(4, single.copy());
            }
            case 5 -> { // Horizontal 3
                for (int a = 0; a < 3; a++) {
                    craftMatrix.setItem(a, single.copy());
                }
            }
            case 6 -> { // Vertical 3
                for (int a = 0; a < 3; a++) {
                    craftMatrix.setItem(a * 3, single.copy());
                }
            }
            case 7 -> { // 2x3 horizontal
                for (int a = 0; a < 6; a++) {
                    craftMatrix.setItem(a, single.copy());
                }
            }
            case 8 -> { // 3x2 vertical
                for (int a = 0; a < 2; a++) {
                    for (int b = 0; b < 3; b++) {
                        craftMatrix.setItem(a + b * 3, single.copy());
                    }
                }
            }
            case 9 -> { // Hollow 3x3
                for (int a = 0; a < 9; a++) {
                    if (a != 4) { // Skip center
                        craftMatrix.setItem(a, single.copy());
                    }
                }
            }
        }
        
        // Find matching recipe
        Optional<CraftingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftMatrix, level);
        
        if (recipe.isEmpty()) {
            return false;
        }
        
        outStack = recipe.get().assemble(craftMatrix, level.registryAccess());
        
        // Handle remaining items (buckets, etc.)
        NonNullList<ItemStack> remaining = recipe.get().getRemainingItems(craftMatrix);
        for (int i = 0; i < remaining.size(); i++) {
            ItemStack original = craftMatrix.getItem(i);
            ItemStack leftover = remaining.get(i);
            
            if (!original.isEmpty()) {
                craftMatrix.setItem(i, ItemStack.EMPTY);
            }
            if (!leftover.isEmpty()) {
                craftMatrix.setItem(i, leftover);
            }
        }
        
        return !outStack.isEmpty();
    }
    
    // ==================== Configuration ====================
    
    /**
     * Cycle to the next pattern type.
     */
    public void cycle() {
        type++;
        if (type > 9) {
            type = 0;
        }
        syncTile(false);
        setChanged();
    }
    
    /**
     * Check if the crafter is enabled (not redstone disabled).
     */
    private boolean isEnabled() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.ENABLED)) {
            return state.getValue(BlockStateProperties.ENABLED);
        }
        return !level.hasNeighborSignal(worldPosition);
    }
    
    // ==================== Block Events ====================
    
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            if (level.isClientSide) {
                rotTicks = 10;
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putByte("type", type);
    }
    
    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        type = tag.getByte("type");
    }
    
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("power", power);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        power = tag.getFloat("power");
        initCraftMatrix(); // Ensure matrix is initialized after load
    }
}
