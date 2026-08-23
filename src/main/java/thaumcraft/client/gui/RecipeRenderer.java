package thaumcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.client.lib.AspectRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RecipeRenderer - Helper class for rendering recipes in the Thaumonomicon.
 * 
 * Supports:
 * - Vanilla crafting recipes (shaped and shapeless)
 * - Arcane crafting recipes
 * - Crucible recipes
 * - Infusion recipes
 * 
 * Ported from GuiResearchPage recipe rendering code.
 */
@OnlyIn(Dist.CLIENT)
public class RecipeRenderer {
    
    private static final Identifier OVERLAY_TEXTURE = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook_overlay.png");
    private static final Identifier BOOK_TEXTURE = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook.png");
    
    private static final int SLOT_SIZE = 18;
    private static final int ITEM_SIZE = 16;
    
    // Cycle counter for animated ingredients
    private static long lastCycleTime = 0;
    private static int cycleIndex = 0;
    
    /**
     * Render a recipe at the given position.
     * 
     * @param graphics the graphics context
     * @param recipeId the recipe resource location
     * @param x center X position
     * @param y center Y position
     * @param mouseX mouse X for tooltips
     * @param mouseY mouse Y for tooltips
     * @param font the font renderer
     * @return list of tooltip components if hovering over an item, null otherwise
     */
    public static List<net.minecraft.network.chat.Component> renderRecipe(
            GuiGraphicsExtractor graphics, Identifier recipeId, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        // Update cycle for animated ingredients
        updateCycle();
        
        // Try to find the recipe
        Object recipe = findRecipe(recipeId);
        
        if (recipe == null) {
            // Recipe not found - draw placeholder
            graphics.centeredText(font, "Recipe not found:", x, y - 20, 0x804040);
            String idStr = recipeId.toString();
            if (idStr.length() > 30) {
                idStr = "..." + idStr.substring(idStr.length() - 27);
            }
            graphics.centeredText(font, idStr, x, y - 8, 0x606060);
            return null;
        }
        
        // Render based on recipe type
        if (recipe instanceof CrucibleRecipe crucible) {
            return renderCrucibleRecipe(graphics, crucible, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof InfusionRecipe infusion) {
            return renderInfusionRecipe(graphics, infusion, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof IArcaneRecipe arcane) {
            return renderArcaneRecipe(graphics, arcane, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof CraftingRecipe crafting) {
            return renderCraftingRecipe(graphics, crafting, x, y, mouseX, mouseY, font);
        } else {
            // Unknown recipe type
            graphics.centeredText(font, "Unknown recipe type", x, y, 0x804040);
            return null;
        }
    }
    
    /**
     * Find a recipe by its resource location.
     */
    private static Object findRecipe(Identifier id) {
        // First check Thaumcraft's catalog
        IThaumcraftRecipe tcRecipe = CommonInternals.getCatalogRecipe(id);
        if (tcRecipe != null) {
            return tcRecipe;
        }
        
        // Check fake recipes
        Object fakeRecipe = CommonInternals.getCatalogRecipeFake(id);
        if (fakeRecipe != null) {
            return fakeRecipe;
        }
        
        // Try vanilla recipe manager
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Optional<RecipeHolder<?>> vanillaRecipe = mc.level.getServer().getRecipeManager()
                    .byKey(ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id));
            if (vanillaRecipe.isPresent()) {
                return vanillaRecipe.get().value();
            }
        }
        
        return null;
    }
    
    /**
     * Update the cycle index for animated ingredients.
     */
    private static void updateCycle() {
        long now = System.currentTimeMillis();
        if (now - lastCycleTime > 1000) {
            cycleIndex++;
            lastCycleTime = now;
        }
    }
    
    /**
     * Get the current item from an ingredient (cycles through options).
     */
    private static ItemStack cycleIngredient(Ingredient ingredient, int slotIndex) {
        ItemStack[] items = ingredient.items()
                .map(net.minecraft.core.Holder::value)
                .map(ItemStack::new)
                .toArray(ItemStack[]::new);
        if (items.length == 0) return ItemStack.EMPTY;
        return items[(cycleIndex + slotIndex) % items.length];
    }
    
    // ==================== Vanilla Crafting ====================
    
    private static List<net.minecraft.network.chat.Component> renderCraftingRecipe(
            GuiGraphicsExtractor graphics, CraftingRecipe recipe, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw title
        String title = recipe instanceof ShapedRecipe ? "Crafting (Shaped)" : "Crafting (Shapeless)";
        graphics.centeredText(font, title, x, y - 70, 0x505050);
        
        // Draw crafting grid background
        graphics.fill(x - 30, y - 50, x + 30, y + 10, 0x20000000);
        
        // Draw output
        ItemStack output = ItemStack.EMPTY;
        if (!recipe.display().isEmpty()) {
            output = recipe.display().get(0).result()
                    .resolveForFirstStack(net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(Minecraft.getInstance().level));
        }
        renderItem(graphics, output, x - 8, y + 25);
        tooltip = checkItemTooltip(output, x - 8, y + 25, mouseX, mouseY, tooltip);
        
        // Draw arrow
        graphics.text(font, "→", x + 20, y - 18, 0x404040, false);
        
        // Draw ingredients
        java.util.List<Optional<Ingredient>> ingredients;
        int width = 3;
        int height = 3;
        if (recipe instanceof ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
            ingredients = shaped.getIngredients();
        } else {
            ingredients = recipe.placementInfo().ingredients().stream().map(Optional::of).toList();
        }
        
        for (int i = 0; i < ingredients.size(); i++) {
            Optional<Ingredient> oing = ingredients.get(i);
            if (oing.isEmpty() || oing.get().isEmpty()) continue;
            Ingredient ing = oing.get();
            
            int gridX, gridY;
            if (recipe instanceof ShapedRecipe) {
                gridX = i % width;
                gridY = i / width;
            } else {
                gridX = i % 3;
                gridY = i / 3;
            }
            
            int itemX = x - 28 + gridX * SLOT_SIZE;
            int itemY = y - 48 + gridY * SLOT_SIZE;
            
            ItemStack stack = cycleIngredient(ing, i);
            renderItem(graphics, stack, itemX, itemY);
            tooltip = checkItemTooltip(stack, itemX, itemY, mouseX, mouseY, tooltip);
        }
        
        return tooltip;
    }
    
    // ==================== Arcane Crafting ====================
    
    private static List<net.minecraft.network.chat.Component> renderArcaneRecipe(
            GuiGraphicsExtractor graphics, IArcaneRecipe recipe, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw title
        graphics.centeredText(font, "Arcane Crafting", x, y - 70, 0x505050);
        
        // Draw crafting grid background
        graphics.fill(x - 30, y - 50, x + 30, y + 10, 0x20404080);
        
        // Draw output
        ItemStack output = recipe.getResultItem();
        renderItem(graphics, output, x - 8, y + 30);
        tooltip = checkItemTooltip(output, x - 8, y + 30, mouseX, mouseY, tooltip);
        
        // Draw vis cost
        int visCost = recipe.getVis();
        if (visCost > 0) {
            graphics.centeredText(font, "Vis: " + visCost, x, y + 52, 0x8080FF);
        }
        
        // Draw crystal requirements
        AspectList crystals = recipe.getCrystals();
        if (crystals != null && crystals.size() > 0) {
            int totalWidth = crystals.size() * 20 - 4;
            int crystalX = x - totalWidth / 2;
            int crystalY = y + 58;
            
            for (Aspect aspect : crystals.getAspects()) {
                int amount = crystals.getAmount(aspect);
                // Draw small aspect icon with amount for crystals
                AspectRenderer.drawAspectSmall(graphics, crystalX, crystalY, aspect);
                graphics.text(font, "x" + amount, crystalX + 10, crystalY + 2, 0xFFFFFF, false);
                crystalX += 28;
            }
        }
        
        // Draw ingredients (3x3 grid)
        NonNullList<Ingredient> ingredients = NonNullList.create();
        if (recipe instanceof thaumcraft.common.lib.crafting.ShapedArcaneRecipe sar) {
            ingredients = sar.getIngredients();
        } else if (recipe instanceof thaumcraft.common.lib.crafting.ShapelessArcaneRecipe slar) {
            ingredients = slar.getIngredients();
        }
        for (int i = 0; i < ingredients.size() && i < 9; i++) {
            Ingredient ing = ingredients.get(i);
            if (ing.isEmpty()) continue;
            
            int gridX = i % 3;
            int gridY = i / 3;
            int itemX = x - 28 + gridX * SLOT_SIZE;
            int itemY = y - 48 + gridY * SLOT_SIZE;
            
            ItemStack stack = cycleIngredient(ing, i);
            renderItem(graphics, stack, itemX, itemY);
            tooltip = checkItemTooltip(stack, itemX, itemY, mouseX, mouseY, tooltip);
        }
        
        return tooltip;
    }
    
    // ==================== Crucible ====================
    
    private static List<net.minecraft.network.chat.Component> renderCrucibleRecipe(
            GuiGraphicsExtractor graphics, CrucibleRecipe recipe, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw title
        graphics.centeredText(font, "Crucible", x, y - 70, 0x505050);
        
        // Draw crucible shape (simplified)
        graphics.fill(x - 25, y - 30, x + 25, y + 30, 0x30804020);
        graphics.fill(x - 20, y - 25, x + 20, y + 25, 0x40402010);
        
        // Draw output above
        ItemStack output = recipe.getRecipeOutput();
        renderItem(graphics, output, x - 8, y - 55);
        tooltip = checkItemTooltip(output, x - 8, y - 55, mouseX, mouseY, tooltip);
        
        // Draw catalyst to the left
        ItemStack catalyst = cycleIngredient(recipe.getCatalyst(), 0);
        renderItem(graphics, catalyst, x - 50, y - 10);
        tooltip = checkItemTooltip(catalyst, x - 50, y - 10, mouseX, mouseY, tooltip);
        graphics.text(font, "→", x - 32, y - 6, 0x404040, false);
        
        // Draw aspects required
        AspectList aspects = recipe.getAspects();
        if (aspects != null && aspects.size() > 0) {
            int aspectY = y + 40;
            graphics.centeredText(font, "Essentia:", x, aspectY, 0x606060);
            aspectY += 12;
            
            // Center the aspects
            int totalWidth = aspects.size() * 20 - 4;
            int aspectX = x - totalWidth / 2;
            
            for (Aspect aspect : aspects.getAspects()) {
                int amount = aspects.getAmount(aspect);
                List<net.minecraft.network.chat.Component> aspectTooltip = 
                        AspectRenderer.drawAspectWithTooltip(graphics, aspectX, aspectY, 
                                aspect, amount, mouseX, mouseY);
                if (aspectTooltip != null && tooltip == null) {
                    tooltip = aspectTooltip;
                }
                aspectX += 20;
            }
        }
        
        return tooltip;
    }
    
    // ==================== Infusion ====================
    
    private static List<net.minecraft.network.chat.Component> renderInfusionRecipe(
            GuiGraphicsExtractor graphics, InfusionRecipe recipe, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw title
        graphics.centeredText(font, "Infusion", x, y - 70, 0x505050);
        
        // Draw matrix shape (center circle)
        graphics.fill(x - 12, y - 12, x + 12, y + 12, 0x40800080);
        
        // Draw central item
        ItemStack central = cycleIngredient(recipe.getRecipeInput(), 0);
        renderItem(graphics, central, x - 8, y - 8);
        tooltip = checkItemTooltip(central, x - 8, y - 8, mouseX, mouseY, tooltip);
        
        // Draw output above
        Object outputObj = recipe.getRecipeOutput();
        if (outputObj instanceof ItemStack output) {
            renderItem(graphics, output, x - 8, y - 55);
            tooltip = checkItemTooltip(output, x - 8, y - 55, mouseX, mouseY, tooltip);
        }
        
        // Draw components in a circle
        NonNullList<Ingredient> components = recipe.getComponents();
        int numComponents = components.size();
        int radius = 35;
        
        for (int i = 0; i < numComponents; i++) {
            double angle = (2 * Math.PI * i / numComponents) - Math.PI / 2;
            int compX = x + (int)(Math.cos(angle) * radius) - 8;
            int compY = y + (int)(Math.sin(angle) * radius) - 8;
            
            ItemStack comp = cycleIngredient(components.get(i), i);
            renderItem(graphics, comp, compX, compY);
            tooltip = checkItemTooltip(comp, compX, compY, mouseX, mouseY, tooltip);
        }
        
        // Draw aspects required
        AspectList aspects = recipe.getAspects();
        if (aspects != null && aspects.size() > 0) {
            int aspectY = y + 50;
            graphics.centeredText(font, "Essentia:", x, aspectY, 0x606060);
            aspectY += 12;
            
            // Limit to 5 aspects, center them
            int displayCount = Math.min(aspects.size(), 5);
            int totalWidth = displayCount * 20 - 4;
            int aspectX = x - totalWidth / 2;
            
            int count = 0;
            for (Aspect aspect : aspects.getAspects()) {
                if (count >= 5) {
                    graphics.text(font, "...", aspectX, aspectY + 4, 0x808080, false);
                    break;
                }
                int amount = aspects.getAmount(aspect);
                List<net.minecraft.network.chat.Component> aspectTooltip = 
                        AspectRenderer.drawAspectWithTooltip(graphics, aspectX, aspectY, 
                                aspect, amount, mouseX, mouseY);
                if (aspectTooltip != null && tooltip == null) {
                    tooltip = aspectTooltip;
                }
                aspectX += 20;
                count++;
            }
        }
        
        // Draw instability
        if (recipe.instability > 0) {
            String instText = "Instability: " + recipe.instability;
            int instColor = recipe.instability > 3 ? 0xFF6060 : (recipe.instability > 1 ? 0xFFFF60 : 0x60FF60);
            graphics.centeredText(font, instText, x, y + 80, instColor);
        }
        
        return tooltip;
    }
    
    // ==================== Helpers ====================
    
    /**
     * Render an item stack at the given position.
     */
    private static void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        graphics.item(stack, x, y);
        graphics.itemDecorations(Minecraft.getInstance().font, stack, x, y);
    }
    
    /**
     * Check if mouse is hovering over an item and return tooltip if so.
     */
    private static List<net.minecraft.network.chat.Component> checkItemTooltip(
            ItemStack stack, int itemX, int itemY, int mouseX, int mouseY,
            List<net.minecraft.network.chat.Component> existingTooltip) {
        
        if (existingTooltip != null) return existingTooltip;
        if (stack.isEmpty()) return null;
        
        if (mouseX >= itemX && mouseX < itemX + ITEM_SIZE && 
            mouseY >= itemY && mouseY < itemY + ITEM_SIZE) {
            return stack.getTooltipLines(
                    net.minecraft.world.item.Item.TooltipContext.of(Minecraft.getInstance().player.level(), Minecraft.getInstance().player),
                    Minecraft.getInstance().player, 
                    Minecraft.getInstance().options.advancedItemTooltips ? 
                    net.minecraft.world.item.TooltipFlag.Default.ADVANCED : 
                    net.minecraft.world.item.TooltipFlag.Default.NORMAL);
        }
        return null;
    }
}
