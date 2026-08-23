package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.lib.crafting.ArcaneWorkbenchCraftingContainer;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.menu.slot.ArcaneWorkbenchResultSlot;
import thaumcraft.common.menu.slot.CrystalSlot;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModMenuTypes;

/**
 * ArcaneWorkbenchMenu - Server-side menu for the Arcane Workbench.
 * 
 * Slot Layout:
 * - 0: Output slot
 * - 1-9: 3x3 crafting grid
 * - 10-15: Crystal slots (Air, Fire, Water, Earth, Order, Entropy)
 * - 16-42: Player inventory (3 rows of 9)
 * - 43-51: Player hotbar (9 slots)
 * 
 * Crystal slot positions around the grid:
 *       [Air]
 * [Fire]     [Water]
 *     [Grid]
 * [Earth]   [Order]
 *      [Entropy]
 */
public class ArcaneWorkbenchMenu extends AbstractContainerMenu {
    
    // Slot indices
    public static final int OUTPUT_SLOT = 0;
    public static final int GRID_START = 1;
    public static final int GRID_END = 9;
    public static final int CRYSTAL_START = 10;
    public static final int CRYSTAL_END = 15;
    public static final int PLAYER_INV_START = 16;
    public static final int PLAYER_INV_END = 42;
    public static final int PLAYER_HOTBAR_START = 43;
    public static final int PLAYER_HOTBAR_END = 51;
    
    // Crystal slot positions (from original TC6)
    public static final int[] CRYSTAL_X = {64, 17, 112, 17, 112, 64};
    public static final int[] CRYSTAL_Y = {13, 35, 35, 93, 93, 115};
    
    // Primal aspects in order
    public static final Aspect[] PRIMAL_ASPECTS = {
        Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY
    };
    
    private final TileArcaneWorkbench blockEntity;
    private final ContainerLevelAccess access;
    private final Player player;
    private final ArcaneWorkbenchCraftingContainer craftMatrix;
    private final ResultContainer craftResult;
    private final ContainerData data;
    
    private int lastVis = -1;
    private long lastCheck = 0L;
    
    // Client constructor
    public ArcaneWorkbenchMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }
    
    // Server constructor
    public ArcaneWorkbenchMenu(int containerId, Inventory playerInventory, TileArcaneWorkbench blockEntity) {
        super(ModMenuTypes.ARCANE_WORKBENCH.get(), containerId);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.craftResult = new ResultContainer();
        this.craftMatrix = new ArcaneWorkbenchCraftingContainer(blockEntity, this);
        
        // Data for syncing vis amount
        this.data = new SimpleContainerData(1);
        addDataSlots(this.data);
        
        // Slot 0: Output
        addSlot(new ArcaneWorkbenchResultSlot(blockEntity, player, craftMatrix, craftResult, 0, 160, 64));
        
        // Slots 1-9: 3x3 crafting grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                // Grid position: 40 + col*24, 40 + row*24
                addSlot(new Slot(craftMatrix, col + row * 3, 40 + col * 24, 40 + row * 24));
            }
        }
        
        // Slots 10-15: Crystal slots
        for (int i = 0; i < 6; i++) {
            addSlot(new CrystalSlot(PRIMAL_ASPECTS[i], craftMatrix, 9 + i, CRYSTAL_X[i], CRYSTAL_Y[i]));
        }
        
        // Slots 16-42: Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 16 + col * 18, 151 + row * 18));
            }
        }
        
        // Slots 43-51: Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 16 + col * 18, 209));
        }
        
        // Initial recipe check
        slotsChanged(craftMatrix);
    }
    
    private static TileArcaneWorkbench getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof TileArcaneWorkbench tileArcaneWorkbench) {
            return tileArcaneWorkbench;
        }
        throw new IllegalStateException("Block entity is not a TileArcaneWorkbench");
    }
    
    @Override
    public void slotsChanged(Container container) {
        access.execute((level, pos) -> slotChangedCraftingGrid(this, level, player, craftMatrix, craftResult));
    }
    
    /**
     * Called when the crafting grid changes. Updates the output slot.
     */
    protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, 
            Player player, ArcaneWorkbenchCraftingContainer craftMatrix, ResultContainer craftResult) {
        if (level.isClientSide()) {
            return;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        
        // First check for arcane recipes
        IArcaneRecipe arcaneRecipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(craftMatrix, player);
        
        if (arcaneRecipe != null) {
            // TODO: Check if player has required research
            // TODO: Check if there's enough vis
            // TODO: Check if crystals are available
            
            TileArcaneWorkbench tile = craftMatrix.getTile();
            tile.updateAura();
            
            int visCost = arcaneRecipe.getVis();
            // TODO: Apply vis discount
            
            boolean hasVis = tile.auraVisServer >= visCost;
            boolean hasCrystals = true; // TODO: Check crystal requirements
            
            if (hasVis && hasCrystals) {
                craftResult.setRecipeUsed(arcaneRecipe);
                result = arcaneRecipe.assemble(craftMatrix, level.registryAccess());
            }
        } else {
            // Check for vanilla recipes
            var vanillaRecipe = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftMatrix, level);
            
            if (vanillaRecipe.isPresent()) {
                CraftingRecipe recipe = vanillaRecipe.get();
                // TODO: Check recipe book/limited crafting
                craftResult.setRecipeUsed(recipe);
                result = recipe.assemble(craftMatrix, level.registryAccess());
            }
        }
        
        craftResult.setItem(0, result);
        menu.setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, 
                menu.incrementStateId(), 0, result));
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
        // Periodically update aura
        long currentTime = System.currentTimeMillis();
        if (currentTime > lastCheck) {
            lastCheck = currentTime + 500L;
            blockEntity.updateAura();
        }
        
        // Check if vis changed
        if (lastVis != blockEntity.auraVisServer) {
            slotsChanged(craftMatrix);
            data.set(0, blockEntity.auraVisServer);
            lastVis = blockEntity.auraVisServer;
        }
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ARCANE_WORKBENCH.get());
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            
            // Output slot
            if (index == OUTPUT_SLOT) {
                // Move to player inventory
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, result);
            }
            // Player inventory/hotbar
            else if (index >= PLAYER_INV_START && index <= PLAYER_HOTBAR_END) {
                // Try to move crystals to crystal slots
                for (int i = 0; i < 6; i++) {
                    if (CrystalSlot.isValidCrystal(stackInSlot, PRIMAL_ASPECTS[i])) {
                        if (!moveItemStackTo(stackInSlot, CRYSTAL_START + i, CRYSTAL_START + i + 1, false)) {
                            // Crystal slot full, continue
                        }
                        if (stackInSlot.isEmpty()) {
                            break;
                        }
                    }
                }
                
                // If still have items, try moving between inventory and hotbar
                if (!stackInSlot.isEmpty()) {
                    if (index < PLAYER_HOTBAR_START) {
                        // Move from inventory to hotbar
                        if (!moveItemStackTo(stackInSlot, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // Move from hotbar to inventory
                        if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_START, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
            // Crafting grid or crystal slots
            else if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_END + 1, false)) {
                return ItemStack.EMPTY;
            }
            
            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            if (stackInSlot.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(player, stackInSlot);
        }
        
        return result;
    }
    
    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != craftResult && super.canTakeItemForPickAll(stack, slot);
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        // Don't drop items - they're stored in the tile entity
    }
    
    // ==================== Accessors ====================
    
    public TileArcaneWorkbench getBlockEntity() {
        return blockEntity;
    }
    
    public int getAuraVis() {
        return data.get(0);
    }
    
    public ArcaneWorkbenchCraftingContainer getCraftMatrix() {
        return craftMatrix;
    }
}
