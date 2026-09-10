package thaumcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.FakeRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.common.lib.crafting.InfusionRecipeType;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.client.lib.AspectRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
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

    // Leaf-name -> recipe index, built lazily from the recipe manager.
    // Recipe files live under data/<ns>/recipes/<typeFolder>/<leaf>.json, so the
    // registry id is "<ns>:<typeFolder>/<leaf>" while research references the bare
    // "<ns>:<leaf>". This index maps the bare leaf name back to the recipe.
    // On a leaf-name collision (same item, multiple crafting methods) the
    // higher-priority Thaumcraft method wins (see folderPriority).
    private static Map<String, Object> recipeLeafIndex;
    private static RecipeManager recipeIndexManager;
    
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
            graphics.centeredText(font, "Recipe not found:", x, y - 20, 0xFF804040);
            String idStr = recipeId.toString();
            if (idStr.length() > 30) {
                idStr = "..." + idStr.substring(idStr.length() - 27);
            }
            graphics.centeredText(font, idStr, x, y - 8, 0xFF606060);
            return null;
        }
        
        // Render based on recipe type
        if (recipe instanceof CrucibleRecipeType crucibleType) {
            return renderCrucibleRecipeType(graphics, crucibleType, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof InfusionRecipeType infusionType) {
            return renderInfusionRecipeType(graphics, infusionType, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof IArcaneRecipe arcane) {
            return renderArcaneRecipe(graphics, arcane, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof CraftingRecipe crafting) {
            return renderCraftingRecipe(graphics, crafting, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof SmeltingRecipe
                || recipe instanceof BlastingRecipe
                || recipe instanceof SmokingRecipe) {
            return renderSmeltingRecipe(graphics, (SingleItemRecipe) recipe, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof CrucibleRecipe crucible) {
            return renderCrucibleRecipe(graphics, crucible, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof InfusionRecipe infusion) {
            return renderInfusionRecipe(graphics, infusion, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof ThaumcraftApi.BluePrint bp) {
            return renderBluePrint(graphics, bp, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof FakeRecipe fake) {
            return renderFake(graphics, fake, x, y, mouseX, mouseY, font);
        } else {
            // Unknown recipe type
            graphics.centeredText(font, "Unknown recipe type", x, y, 0xFF804040);
            return null;
        }
    }
    
    /**
     * Find a recipe by its resource location.
     *
     * Research entries reference recipes by bare leaf name (e.g. "thaumcraft:thaumometer"),
     * but the data-driven recipe files live under a type folder so their registry ids are
     * "thaumcraft:<typeFolder>/<leaf>". We resolve via a leaf-name index built from the live
     * recipe manager, falling back to a direct registry lookup.
     */
    public static Object findRecipe(Identifier id) {
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

        // Resolve via the leaf-name index, built from the client recipe manager
        // (getRecipeManager() picks the populated manager; see that helper).
        RecipeManager rm = getRecipeManager();
        if (rm != null) {
            buildRecipeLeafIndex();
            if (recipeLeafIndex != null) {
                Object found = recipeLeafIndex.get(leafName(id));
                if (found == null) {
                    // Legacy research data references "fake" display recipes
                    // (e.g. "salis_mundus_fake") that map to a real recipe of the
                    // base name (e.g. "salis_mundus"). Try the de-faked leaf name.
                    String defaked = stripFakeSuffix(leafName(id));
                    if (defaked != null) {
                        found = recipeLeafIndex.get(defaked);
                    }
                    if (found == null) {
                        // Legacy book ids that never matched a file name
                        // (e.g. "nitor_color", "infusion_altar").
                        found = findRecipeAlias(leafName(id));
                    }
                }
                if (found != null) {
                    return found;
                }
            }
            // Fallback: direct registry lookup (in case the id is already a full registry id)
            Optional<RecipeHolder<?>> vanillaRecipe = rm
                    .byKey(ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id));
            if (vanillaRecipe.isPresent()) {
                return vanillaRecipe.get().value();
            }
        }

        return null;
    }

    /**
     * Extract the bare leaf name from a recipe identifier (last path segment).
     */
    private static String leafName(Identifier id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /**
     * Legacy book recipe ids that do not match any recipe file or catalog entry.
     * Maps the book leaf name to the real leaf name (recipe file) or catalog id.
     */
    private static final Map<String, String> RECIPE_ALIASES = Map.ofEntries(
            Map.entry("nitor_color", "nitor_yellow"),
            Map.entry("nitor_group", "nitor_yellow"),
            Map.entry("vis_crystal_group", "vis_crystal_aer"),
            Map.entry("brass_stuff", "brass_block"),
            Map.entry("thaumium_stuff", "thaumium_block"),
            Map.entry("void_stuff", "void_block"),
            Map.entry("voidingot", "void_metal_ingot"),
            Map.entry("arcane_workbench_charger", "workbench_charger"),
            Map.entry("mnemonic_matrix", "brain_box"),
            Map.entry("jar_label", "label_blank"),
            Map.entry("jar_label_essentia", "label_filled"),
            Map.entry("banners", "banner_red"),
            Map.entry("baubles_stuff", "charm_undying"),
            Map.entry("arcane_brick", "arcane_stone_brick"));

    /**
     * Resolve a legacy book recipe id via the alias table: real catalog first
     * (multiblock BluePrints), then the fake catalog, then the leaf index.
     */
    private static Object findRecipeAlias(String leaf) {
        String alias = RECIPE_ALIASES.get(leaf);
        if (alias == null) {
            return null;
        }
        Identifier aid = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, alias);
        Object catalog = CommonInternals.getCatalogRecipe(aid);
        if (catalog != null) {
            return catalog;
        }
        Object fake = CommonInternals.getCatalogRecipeFake(aid);
        if (fake != null) {
            return fake;
        }
        if (recipeLeafIndex != null) {
            return recipeLeafIndex.get(leafName(aid));
        }
        return null;
    }

    /**
     * Strip a legacy "_fake" / "_fake_N" suffix from a recipe leaf name.
     * Returns the base name, or null if there was no such suffix.
     */
    private static String stripFakeSuffix(String leaf) {
        if (leaf.endsWith("_fake")) {
            return leaf.substring(0, leaf.length() - "_fake".length());
        }
        int idx = leaf.indexOf("_fake_");
        if (idx >= 0) {
            String num = leaf.substring(idx + "_fake_".length());
            if (!num.isEmpty() && num.chars().allMatch(Character::isDigit)) {
                return leaf.substring(0, idx);
            }
        }
        return null;
    }

    /**
     * Build (or refresh) the leaf-name -> recipe index from the live recipe manager.
     * Only Thaumcraft-namespace recipes are indexed. On a leaf-name collision the
     * higher-priority crafting method wins (see folderPriority). Rebuilt when the level
     * changes (new world / new server).
     */
    private static void buildRecipeLeafIndex() {
        RecipeManager rm = getRecipeManager();
        if (rm == null) {
            return;
        }
        if (recipeLeafIndex != null && recipeIndexManager == rm) {
            return;
        }

        Map<String, Object> index = new HashMap<>();
        Map<String, Integer> priority = new HashMap<>();

        var recipeMap = rm.recipeMap();
        for (RecipeHolder<?> holder : recipeMap.values()) {
            Identifier id = holder.id().identifier();
            if (!Thaumcraft.MODID.equals(id.getNamespace())) {
                continue;
            }
            String leaf = leafName(id);
            int pri = folderPriority(folderOf(id));
            Integer existing = priority.get(leaf);
            if (existing == null || pri < existing) {
                index.put(leaf, holder.value());
                priority.put(leaf, pri);
            }
        }


        recipeLeafIndex = index;
        recipeIndexManager = rm;
    }

    /**
     * Resolve the recipe manager to use for lookups. The client owns a populated
     * recipe manager (see "Loaded N recipes" on the Render thread), while the
     * integrated server's manager may be empty in the 26.2 client render context.
     * Prefer the client's manager (the pattern JEI and the crafting code use),
     * falling back to the server-cached one.
     */
    private static RecipeManager getRecipeManager() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                var server = mc.level.getServer();
                if (server != null) {
                    return server.getRecipeManager();
                }
            }
        } catch (Throwable ignored) {
        }
        return Thaumcraft.recipeManager;
    }


    /**
     * The type-folder part of a recipe id (first path segment), or "" if none.
     */
    private static String folderOf(Identifier id) {
        String path = id.getPath();
        int slash = path.indexOf('/');
        return slash >= 0 ? path.substring(0, slash) : "";
    }

    /**
     * Priority for a recipe type folder. Lower value = higher priority when several recipes
     * share a leaf name. The arcane workbench is the primary Thaumcraft crafting method, so
     * it wins ties.
     */
    private static int folderPriority(String folder) {
        return switch (folder) {
            case "arcane_workbench" -> 0;
            case "infusion" -> 1;
            case "crucible" -> 2;
            case "smelting" -> 3;
            case "crafting" -> 4;
            default -> 5;
        };
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
    public static ItemStack cycleIngredient(Ingredient ingredient, int slotIndex) {
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
        graphics.centeredText(font, title, x, y - 70, 0xFF505050);
        
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
        graphics.text(font, "→", x + 20, y - 18, 0xFF404040, false);
        
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
        graphics.centeredText(font, "Arcane Crafting", x, y - 70, 0xFF505050);
        
        // Draw crafting grid background
        graphics.fill(x - 30, y - 54, x + 30, y + 6, 0x20404080);
        
        // Draw arrow from grid to output
        graphics.fill(x - 1, y + 8, x + 1, y + 16, 0xFF303030);
        graphics.fill(x - 2, y + 16, x + 2, y + 18, 0xFF303030);
        graphics.fill(x - 3, y + 18, x + 3, y + 20, 0xFF303030);
        
        // Draw output
        ItemStack output = recipe.getResultItem();
        renderItem(graphics, output, x - 8, y + 24);
        tooltip = checkItemTooltip(output, x - 8, y + 24, mouseX, mouseY, tooltip);
        
        // Draw vis cost
        int visCost = recipe.getVis();
        if (visCost > 0) {
            graphics.centeredText(font, "Vis: " + visCost, x, y + 48, 0xFF8080FF);
        }
        
        // Draw crystal requirements (max 3 per row)
        AspectList crystals = recipe.getCrystals();
        if (crystals != null && crystals.size() > 0) {
            Aspect[] caspects = crystals.getAspects();
            int perRow = 3;
            for (int ci = 0; ci < caspects.length; ci++) {
                Aspect aspect = caspects[ci];
                int row = ci / perRow;
                int col = ci % perRow;
                int inRow = Math.min(perRow, caspects.length - row * perRow);
                int xStart = x - (inRow * 28 - 12) / 2;
                int crystalX = xStart + col * 28;
                int crystalY = y + 60 + row * 18;
                AspectRenderer.drawAspectSmall(graphics, crystalX, crystalY, aspect);
                graphics.text(font, "x" + crystals.getAmount(aspect), crystalX + 10, crystalY + 2, 0xFFFFFFFF, false);
            }
        }
        
        // Draw ingredients (3x3 grid)
        java.util.List<Optional<Ingredient>> ingredients = java.util.List.of();
        if (recipe instanceof thaumcraft.common.lib.crafting.ShapedArcaneRecipe sar) {
            ingredients = sar.getIngredients();
        } else if (recipe instanceof thaumcraft.common.lib.crafting.ShapelessArcaneRecipe slar) {
            ingredients = slar.getIngredients().stream().map(Optional::of).toList();
        }
        for (int i = 0; i < ingredients.size() && i < 9; i++) {
            Optional<Ingredient> oing = ingredients.get(i);
            if (oing.isEmpty()) continue;
            Ingredient ing = oing.get();
            
            int gridX = i % 3;
            int gridY = i / 3;
            int itemX = x - 28 + gridX * SLOT_SIZE;
            int itemY = y - 52 + gridY * SLOT_SIZE;
            
            ItemStack stack = cycleIngredient(ing, i);
            renderItem(graphics, stack, itemX, itemY);
            tooltip = checkItemTooltip(stack, itemX, itemY, mouseX, mouseY, tooltip);
        }
        
        // Draw the arcane workbench this recipe is crafted on
        ItemStack bench = new ItemStack(thaumcraft.init.ModBlocks.ARCANE_WORKBENCH.get());
        renderItem(graphics, bench, x - 8, y + 100);
        tooltip = checkItemTooltip(bench, x - 8, y + 100, mouseX, mouseY, tooltip);
        
        return tooltip;
    }
    
    // ==================== Crucible ====================
    
    private static List<net.minecraft.network.chat.Component> renderCrucibleRecipe(
            GuiGraphicsExtractor graphics, CrucibleRecipe recipe, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw title
        graphics.centeredText(font, "Crucible", x, y - 70, 0xFF505050);
        
        // Draw the crucible block
        ItemStack crucible = new ItemStack(thaumcraft.init.ModBlocks.CRUCIBLE.get());
        renderItem(graphics, crucible, x - 8, y - 8);
        tooltip = checkItemTooltip(crucible, x - 8, y - 8, mouseX, mouseY, tooltip);
        
        // Draw output above
        ItemStack output = recipe.getRecipeOutput();
        renderItem(graphics, output, x - 8, y - 55);
        tooltip = checkItemTooltip(output, x - 8, y - 55, mouseX, mouseY, tooltip);
        
        // Draw catalyst to the left
        ItemStack catalyst = cycleIngredient(recipe.getCatalyst(), 0);
        renderItem(graphics, catalyst, x - 50, y - 10);
        tooltip = checkItemTooltip(catalyst, x - 50, y - 10, mouseX, mouseY, tooltip);
        graphics.text(font, "→", x - 32, y - 6, 0xFF404040, false);
        
        // Draw aspects required
        AspectList aspects = recipe.getAspects();
        if (aspects != null && aspects.size() > 0) {
            int aspectY = y + 40;
            graphics.centeredText(font, "Essentia:", x, aspectY, 0xFF606060);
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
        graphics.centeredText(font, "Infusion", x, y - 70, 0xFF505050);
        
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
            graphics.centeredText(font, "Essentia:", x, aspectY, 0xFF606060);
            aspectY += 12;
            
            // Limit to 5 aspects, center them
            int displayCount = Math.min(aspects.size(), 5);
            int totalWidth = displayCount * 20 - 4;
            int aspectX = x - totalWidth / 2;
            
            int count = 0;
            for (Aspect aspect : aspects.getAspects()) {
                if (count >= 5) {
                    graphics.text(font, "...", aspectX, aspectY + 4, 0xFF808080, false);
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
            int instColor = recipe.instability > 3 ? 0xFFFF6060 : (recipe.instability > 1 ? 0xFFFFFF60 : 0xFF60FF60);
            graphics.centeredText(font, instText, x, y + 80, instColor);
        }
        
        return tooltip;
    }

    /**
     * Render a data-driven crucible recipe (CrucibleRecipeType).
     */
    private static List<net.minecraft.network.chat.Component> renderCrucibleRecipeType(
            GuiGraphicsExtractor graphics, CrucibleRecipeType recipe, int x, int y,
            int mouseX, int mouseY, Font font) {

        List<net.minecraft.network.chat.Component> tooltip = null;

        // Draw title
        graphics.centeredText(font, "Crucible", x, y - 70, 0xFF505050);

        // Draw the crucible block
        ItemStack crucible = new ItemStack(thaumcraft.init.ModBlocks.CRUCIBLE.get());
        renderItem(graphics, crucible, x - 8, y - 8);
        tooltip = checkItemTooltip(crucible, x - 8, y - 8, mouseX, mouseY, tooltip);

        // Draw output above
        ItemStack output = recipe.getResultItem();
        renderItem(graphics, output, x - 8, y - 55);
        tooltip = checkItemTooltip(output, x - 8, y - 55, mouseX, mouseY, tooltip);

        // Draw catalyst to the left
        ItemStack catalyst = cycleIngredient(recipe.getCatalyst(), 0);
        renderItem(graphics, catalyst, x - 50, y - 10);
        tooltip = checkItemTooltip(catalyst, x - 50, y - 10, mouseX, mouseY, tooltip);
        graphics.text(font, "\u2192", x - 32, y - 6, 0xFF404040, false);

        // Draw aspects required
        AspectList aspects = recipe.getAspects();
        if (aspects != null && aspects.size() > 0) {
            int aspectY = y + 40;
            graphics.centeredText(font, "Essentia:", x, aspectY, 0xFF606060);
            aspectY += 12;

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

    /**
     * Render a data-driven infusion recipe (InfusionRecipeType).
     */
    private static List<net.minecraft.network.chat.Component> renderInfusionRecipeType(
            GuiGraphicsExtractor graphics, InfusionRecipeType recipe, int x, int y,
            int mouseX, int mouseY, Font font) {

        List<net.minecraft.network.chat.Component> tooltip = null;

        // Draw title
        graphics.centeredText(font, "Infusion", x, y - 70, 0xFF505050);

        // Draw matrix shape (center circle)
        graphics.fill(x - 12, y - 12, x + 12, y + 12, 0x40800080);

        // Draw central item
        ItemStack central = recipe.getCentralItem() == null ? ItemStack.EMPTY : cycleIngredient(recipe.getCentralItem(), 0);
        renderItem(graphics, central, x - 8, y - 8);
        tooltip = checkItemTooltip(central, x - 8, y - 8, mouseX, mouseY, tooltip);

        // Draw output above
        ItemStack output = recipe.getResultItem();
        renderItem(graphics, output, x - 8, y - 55);
        tooltip = checkItemTooltip(output, x - 8, y - 55, mouseX, mouseY, tooltip);

        // Draw components in a circle
        List<Ingredient> components = recipe.getComponents();
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
            graphics.centeredText(font, "Essentia:", x, aspectY, 0xFF606060);
            aspectY += 12;

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

        // Draw instability
        if (recipe.getInstability() > 0) {
            int inst = recipe.getInstability();
            String instText = "Instability: " + inst;
            int instColor = inst > 3 ? 0xFFFF6060 : (inst > 1 ? 0xFFFFFF60 : 0xFF60FF60);
            graphics.centeredText(font, instText, x, y + 80, instColor);
        }

        return tooltip;
    }

    /**
     * Render a vanilla-style single-input recipe (smelting, blasting, smoking).
     */
    private static List<net.minecraft.network.chat.Component> renderSmeltingRecipe(
            GuiGraphicsExtractor graphics, SingleItemRecipe recipe, int x, int y,
            int mouseX, int mouseY, Font font) {

        List<net.minecraft.network.chat.Component> tooltip = null;

        // Draw title
        graphics.centeredText(font, "Furnace", x, y - 70, 0xFF505050);

        // Draw input on the left
        Ingredient input = recipe.input();
        ItemStack inputStack = cycleIngredient(input, 0);
        renderItem(graphics, inputStack, x - 50, y - 8);
        tooltip = checkItemTooltip(inputStack, x - 50, y - 8, mouseX, mouseY, tooltip);
        graphics.text(font, "\u2192", x - 32, y - 4, 0xFF404040, false);

        // Draw output on the right
        ItemStack output = recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
        renderItem(graphics, output, x + 20, y - 8);
        tooltip = checkItemTooltip(output, x + 20, y - 8, mouseX, mouseY, tooltip);

        return tooltip;
    }

    // ==================== Multiblock BluePrint ====================
    
    private static List<net.minecraft.network.chat.Component> renderBluePrint(
            GuiGraphicsExtractor graphics, ThaumcraftApi.BluePrint bp, int x, int y,
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw title
        graphics.centeredText(font, "Multiblock", x, y - 70, 0xFF505050);
        
        // Draw the display item
        ItemStack display = bp.getDisplayStack();
        renderItem(graphics, display, x - 8, y - 40);
        tooltip = checkItemTooltip(display, x - 8, y - 40, mouseX, mouseY, tooltip);
        
        // Draw arrow
        graphics.fill(x - 1, y - 16, x + 1, y - 8, 0xFF505050);
        
        // Draw ingredients (max 6 per row)
        ItemStack[] ingredients = bp.getIngredientList();
        int shown = Math.min(ingredients.length, 6);
        int startX = x - (shown * 18 - 12) / 2;
        for (int i = 0; i < shown; i++) {
            ItemStack ing = ingredients[i];
            int ix = startX + i * 18;
            renderItem(graphics, ing, ix, y);
            tooltip = checkItemTooltip(ing, ix, y, mouseX, mouseY, tooltip);
        }
        if (ingredients.length > 6) {
            graphics.text(font, "...", x + (shown * 18 - 12) / 2 + 2, y + 4, 0xFF808080, false);
        }
        
        return tooltip;
    }

    private static List<net.minecraft.network.chat.Component> renderFake(
            GuiGraphicsExtractor graphics, FakeRecipe fake, int x, int y,
            int mouseX, int mouseY, Font font) {

        graphics.centeredText(font, fake.getTitle(), x, y - 30, 0xFF202020);

        ItemStack[] items = fake.getItems();
        List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
        if (items.length > 0) {
            int startX = x - items.length * 10;
            for (int i = 0; i < items.length; i++) {
                ItemStack stack = items[i];
                int ix = startX + i * 20;
                renderItem(graphics, stack, ix, y);
                tooltip = checkItemTooltip(stack, ix, y, mouseX, mouseY, tooltip);
            }
        }
        return tooltip;
    }
    
    // ==================== Helpers ====================

    /**
     * Resolve the result item of any recipe type (used for book bookmarks).
     */
    public static net.minecraft.world.item.ItemStack resolveOutput(Object recipe) {
        if (recipe instanceof IArcaneRecipe arcane) {
            return arcane.getResultItem();
        } else if (recipe instanceof CrucibleRecipeType crucible) {
            return crucible.getResultItem();
        } else if (recipe instanceof InfusionRecipeType infusion) {
            return infusion.getResultItem();
        } else if (recipe instanceof CrucibleRecipe crucible) {
            return crucible.getRecipeOutput();
        } else if (recipe instanceof InfusionRecipe infusion) {
            Object out = infusion.getRecipeOutput();
            return out instanceof net.minecraft.world.item.ItemStack stack ? stack : net.minecraft.world.item.ItemStack.EMPTY;
        } else if (recipe instanceof CraftingRecipe crafting) {
            if (!crafting.display().isEmpty()) {
                return crafting.display().get(0).result()
                        .resolveForFirstStack(net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(Minecraft.getInstance().level));
            }
            return net.minecraft.world.item.ItemStack.EMPTY;
        } else if (recipe instanceof SingleItemRecipe single) {
            return single.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY));
        } else if (recipe instanceof ThaumcraftApi.BluePrint bp) {
            return bp.getDisplayStack();
        } else if (recipe instanceof FakeRecipe fake) {
            return fake.getItems().length > 0 ? fake.getItems()[0] : net.minecraft.world.item.ItemStack.EMPTY;
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
    /**
     * Render an item stack at the given position, on a dark slot background so
     * items read clearly against the book paper.
     */
    public static void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x50303030);
        graphics.item(stack, x, y);
        graphics.itemDecorations(Minecraft.getInstance().font, stack, x, y);
    }
    
    /**
     * Check if mouse is hovering over an item and return tooltip if so.
     */
    public static List<net.minecraft.network.chat.Component> checkItemTooltip(
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
