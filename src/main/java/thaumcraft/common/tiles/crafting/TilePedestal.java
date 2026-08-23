package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.init.ModBlockEntities;

/**
 * Tile entity for pedestals used in infusion crafting.
 * Holds a single item that can be used in infusion recipes.
 */
public class TilePedestal extends TileThaumcraftInventory {

    public TilePedestal(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PEDESTAL.get(), pos, state, 1);
        this.syncedSlots = new int[]{0};
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.isEmpty() || getItem(slot).isEmpty();
    }

    /**
     * Set the item on the pedestal, used during infusion crafting.
     */
    public void setItemFromInfusion(ItemStack stack) {
        setItem(0, stack);
        setChanged();
        if (level != null && !level.isClientSide()) {
            syncTile(false);
        }
    }

    /**
     * Get the displayed item on the pedestal.
     */
    public ItemStack getDisplayedItem() {
        return getItem(0);
    }

    /**
     * Try to insert an item held by the player.
     * Returns true if successful.
     */
    public boolean tryInsertItem(Player player, ItemStack heldItem) {
        ItemStack current = getItem(0);
        
        if (current.isEmpty() && !heldItem.isEmpty()) {
            // Place item on pedestal
            ItemStack toPlace = heldItem.copy();
            toPlace.setCount(1);
            setItem(0, toPlace);
            heldItem.shrink(1);
            
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS,
                        0.2f, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7f + 1.0f) * 1.6f);
            }
            syncTile(false);
            return true;
        }
        return false;
    }

    /**
     * Try to take the item from the pedestal.
     * Returns the item if successful, empty stack otherwise.
     */
    public ItemStack tryTakeItem(Player player) {
        ItemStack current = getItem(0);
        
        if (!current.isEmpty()) {
            ItemStack taken = current.copy();
            setItem(0, ItemStack.EMPTY);
            
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS,
                        0.2f, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7f + 1.0f) * 1.2f);
            }
            syncTile(false);
            return taken;
        }
        return ItemStack.EMPTY;
    }

    // ==================== Rendering ====================

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 2, worldPosition.getZ() + 1);
    }
}
