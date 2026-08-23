package thaumcraft.common.menu.slot;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.NeoForge;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IArcaneWorkbench;
import thaumcraft.common.lib.crafting.ArcaneWorkbenchCraftingContainer;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;

/**
 * ArcaneWorkbenchResultSlot - The output slot for arcane workbench crafting.
 * 
 * Handles:
 * - Consuming crafting ingredients
 * - Consuming crystals for arcane recipes
 * - Draining vis from the aura
 * - Firing crafting events
 */
public class ArcaneWorkbenchResultSlot extends Slot {
    
    private final CraftingContainer craftMatrix;
    private final Player player;
    private final TileArcaneWorkbench tile;
    private int amountCrafted;
    
    public ArcaneWorkbenchResultSlot(TileArcaneWorkbench tile, Player player, 
            CraftingContainer craftMatrix, Container resultContainer, int slot, int x, int y) {
        super(resultContainer, slot, x, y);
        this.tile = tile;
        this.player = player;
        this.craftMatrix = craftMatrix;
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        // Cannot place items in the result slot
        return false;
    }
    
    @Override
    public ItemStack remove(int amount) {
        if (hasItem()) {
            amountCrafted += Math.min(amount, getItem().getCount());
        }
        return super.remove(amount);
    }
    
    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        amountCrafted += amount;
        checkTakeAchievements(stack);
    }
    
    @Override
    protected void onSwapCraft(int numItems) {
        amountCrafted += numItems;
    }
    
    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        if (amountCrafted > 0) {
            stack.onCraftedBy(player, amountCrafted);
            net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(player, stack, craftMatrix);
        }
        
        Container container = this.container;
        if (container instanceof net.minecraft.world.inventory.RecipeCraftingHolder recipeCraftingHolder) {
            recipeCraftingHolder.awardUsedRecipes(player, craftMatrix.getItems());
        }
        
        amountCrafted = 0;
    }
    
    @Override
    public void onTake(Player thePlayer, ItemStack stack) {
        checkTakeAchievements(stack);
        
        // Find the matching recipe
        IArcaneRecipe arcaneRecipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(craftMatrix, thePlayer);
        
        net.neoforged.neoforge.common.CommonHooks.setCraftingPlayer(thePlayer);
        
        net.minecraft.world.item.crafting.CraftingInput.Positioned positioned = craftMatrix.asPositionedCraftInput();
        net.minecraft.world.item.crafting.CraftingInput input = positioned.input();
        NonNullList<ItemStack> remainingItems = net.minecraft.world.item.crafting.CraftingRecipe.defaultCraftingReminder(input);
        if (arcaneRecipe != null) {
            // Consume vis from aura
            int visCost = arcaneRecipe.getVis();
            // TODO: Apply vis discount from player's gear
            // visCost = (int)(visCost * (1.0f - CasterManager.getTotalVisDiscount(thePlayer)));
            if (visCost > 0) {
                tile.updateAura();
                tile.spendAura(visCost);
            }
            
            // Consume crystals
            AspectList crystals = arcaneRecipe.getCrystals();
            if (crystals != null && crystals.size() > 0) {
                consumeCrystals(crystals);
            }
        }
        
        net.neoforged.neoforge.common.CommonHooks.setCraftingPlayer(null);
        
        // Consume ingredients and handle remaining items (buckets, etc.)
        int recipeLeft = positioned.left();
        int recipeTop = positioned.top();
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                int i = x + recipeLeft + (y + recipeTop) * craftMatrix.getWidth();
                ItemStack slotStack = craftMatrix.getItem(i);
                ItemStack remaining = remainingItems.get(x + y * input.width());
                
                if (!slotStack.isEmpty()) {
                    craftMatrix.removeItem(i, 1);
                    slotStack = craftMatrix.getItem(i);
                }
                
                if (!remaining.isEmpty()) {
                    if (slotStack.isEmpty()) {
                        craftMatrix.setItem(i, remaining);
                    } else if (ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                        remaining.grow(slotStack.getCount());
                        craftMatrix.setItem(i, remaining);
                    } else if (!player.getInventory().add(remaining)) {
                        player.drop(remaining, false);
                    }
                }
            }
        }
    }
    
    /**
     * Consume crystals from the crystal slots (slots 9-14 in the crafting matrix).
     */
    private void consumeCrystals(AspectList crystals) {
        for (Aspect aspect : crystals.getAspects()) {
            int required = crystals.getAmount(aspect);
            ItemStack targetCrystal = ThaumcraftApiHelper.makeCrystal(aspect, required);
            
            // Search crystal slots (9-14)
            for (int slot = 9; slot < 15; slot++) {
                ItemStack slotStack = craftMatrix.getItem(slot);
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(targetCrystal, slotStack)) {
                    int toRemove = Math.min(required, slotStack.getCount());
                    craftMatrix.removeItem(slot, toRemove);
                    required -= toRemove;
                    if (required <= 0) break;
                }
            }
        }
    }
}
