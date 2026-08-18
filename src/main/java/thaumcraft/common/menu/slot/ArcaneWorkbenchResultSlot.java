package thaumcraft.common.menu.slot;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeHolder;
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
            stack.onCraftedBy(player.level(), player, amountCrafted);
            net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(player, stack, craftMatrix);
        }
        
        Container container = this.container;
        if (container instanceof RecipeHolder recipeHolder) {
            var recipe = recipeHolder.getRecipeUsed();
            if (recipe != null && !recipe.isSpecial()) {
                player.awardRecipes(java.util.Collections.singleton(recipe));
                recipeHolder.setRecipeUsed(null);
            }
        }
        
        amountCrafted = 0;
    }
    
    @Override
    public void onTake(Player thePlayer, ItemStack stack) {
        checkTakeAchievements(stack);
        
        // Find the matching recipe
        IArcaneRecipe arcaneRecipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(craftMatrix, thePlayer);
        
        ForgeHooks.setCraftingPlayer(thePlayer);
        
        NonNullList<ItemStack> remainingItems;
        if (arcaneRecipe != null && craftMatrix instanceof IArcaneWorkbench workbench) {
            // Get remaining items from arcane recipe
            remainingItems = arcaneRecipe.getRemainingItems(workbench);
            
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
        } else if (arcaneRecipe != null) {
            // Fallback - shouldn't happen but safety check
            remainingItems = NonNullList.withSize(craftMatrix.getContainerSize(), ItemStack.EMPTY);
        } else {
            // Check for vanilla recipe
            var level = thePlayer.level();
            var recipeOpt = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftMatrix, level);
            
            if (recipeOpt.isPresent()) {
                CraftingRecipe vanillaRecipe = recipeOpt.get();
                remainingItems = vanillaRecipe.getRemainingItems(craftMatrix);
            } else {
                remainingItems = NonNullList.withSize(craftMatrix.getContainerSize(), ItemStack.EMPTY);
            }
        }
        
        ForgeHooks.setCraftingPlayer(null);
        
        // Consume ingredients and handle remaining items (buckets, etc.)
        for (int i = 0; i < Math.min(9, remainingItems.size()); i++) {
            ItemStack slotStack = craftMatrix.getItem(i);
            ItemStack remaining = remainingItems.get(i);
            
            if (!slotStack.isEmpty()) {
                craftMatrix.removeItem(i, 1);
                slotStack = craftMatrix.getItem(i);
            }
            
            if (!remaining.isEmpty()) {
                if (slotStack.isEmpty()) {
                    craftMatrix.setItem(i, remaining);
                } else if (ItemStack.isSameItemSameTags(slotStack, remaining)) {
                    remaining.grow(slotStack.getCount());
                    craftMatrix.setItem(i, remaining);
                } else if (!player.getInventory().add(remaining)) {
                    player.drop(remaining, false);
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
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(targetCrystal, slotStack)) {
                    int toRemove = Math.min(required, slotStack.getCount());
                    craftMatrix.removeItem(slot, toRemove);
                    required -= toRemove;
                    if (required <= 0) break;
                }
            }
        }
    }
}
