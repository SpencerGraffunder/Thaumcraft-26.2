package thaumcraft.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.FakeRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.ThaumcraftApi.BluePrint;
import thaumcraft.client.lib.AspectRenderer;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.common.lib.crafting.InfusionRecipeType;
import thaumcraft.common.lib.crafting.ShapelessArcaneRecipe;
import thaumcraft.common.lib.crafting.ShapedArcaneRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Renders the 1.12-style overlay panels that appear when clicking the book's
 * right-side recipe bookmarks or the left-side aspect bookmark.
 *
 * <p>Each popup draws a gilded-paper panel centered on the book and returns
 * its bounds so the screen can hit-test clicks (inside = no-op, outside =
 * close the popup). Recipe popups draw the crafting station as a line-art
 * image (crucible pot, infusion altar, arcane workbench grid) taken from the
 * book's overlay texture, with the recipe's items placed on top.
 */
@OnlyIn(Dist.CLIENT)
public class BookPopupRenderer {

    private static final Identifier PAPER = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/paper.png");
    private static final Identifier PAPER_GILDED = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/papergilded.png");
    private static final Identifier OVERLAY = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/gui_researchbook_overlay.png");
    private static final Identifier VIS_ICON = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/costvis.png");
    private static final Identifier BOOK = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/gui_researchbook.png");

    // Station line-art UV regions in gui_researchbook_overlay.png (512x512)
    private static final int[] UV_CRUCIBLE_POT = {4, 42, 105, 91};
    private static final int[] UV_ALTAR_DIAMOND = {11, 174, 106, 53};
    private static final int[] UV_ARCANE_GRID = {123, 33, 98, 98};
    private static final int[] UV_FLAME_ARROW = {36, 403, 40, 88};
    private static final int[] UV_CURVED_ARROW = {200, 168, 21, 25};

    private static final int ARROW_TEX_X0 = 0; // red "previous" arrow in gui_researchbook.png
    private static final int ARROW_TEX_X1 = 12; // red "next" arrow
    private static final int ARROW_TEX_Y = 184;

    /**
     * Panel rectangle in screen coordinates.
     */
    public static class Bounds {
        public final int x, y, w, h;

        public Bounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    // ==================== Aspect popup ====================

    /**
     * Number of aspects shown per page in the aspect popup.
     */
    public static final int ASPECTS_PER_PAGE = 5;

    public static int aspectCount() {
        return Aspect.aspects.size();
    }

    public static int aspectPageCount() {
        return Math.max(1, (Aspect.aspects.size() + ASPECTS_PER_PAGE - 1) / ASPECTS_PER_PAGE);
    }

    /**
     * Hit rect for the "previous page" arrow of a popup panel.
     */
    public static Bounds prevArrow(Bounds b) {
        return new Bounds(b.x + 20, b.y + b.h - 24, 12, 8);
    }

    /**
     * Hit rect for the "next page" arrow of a popup panel.
     */
    public static Bounds nextArrow(Bounds b) {
        return new Bounds(b.x + b.w - 32, b.y + b.h - 24, 12, 8);
    }

    /**
     * Draw the "Aspects of Essentia" popup (1.12 style): full-page paper panel
     * with aspect rows (icon, name, type), paged with red arrows at the
     * panel's bottom corners.
     */
    public static Bounds drawAspectPopup(
            GuiGraphicsExtractor graphics, int cx, int cy, int page,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        int W = 256, H = 256;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H, PAPER);

        graphics.centeredText(font, Component.translatable("tc.aspect.name").getString(),
                cx, py + 22, 0xFF202020);

        List<Aspect> all = new ArrayList<>(Aspect.aspects.values());
        int start = page * ASPECTS_PER_PAGE;
        int count = Math.min(ASPECTS_PER_PAGE, all.size() - start);
        for (int i = 0; i < count; i++) {
            Aspect aspect = all.get(start + i);
            int rowY = py + 44 + i * 40;
            int ix = px + 44, iy = rowY;

            // 1.5x aspect icon (24px)
            graphics.pose().pushMatrix();
            graphics.pose().translate(ix, iy);
            graphics.pose().scale(1.5f);
            AspectRenderer.drawAspect(graphics, 0, 0, aspect);
            graphics.pose().popMatrix();

            graphics.text(font, aspect.getName(), px + 82, rowY + 5, 0xFF404040, false);
            graphics.pose().pushMatrix();
            graphics.pose().translate(px + 82, rowY + 18);
            graphics.pose().scale(0.75f);
            graphics.text(font, aspect.isPrimal() ? "Primal Aspect" : "Secondary Aspect",
                    0, 0, 0xFF808080, false);
            graphics.pose().popMatrix();

            if (mouseX >= ix && mouseX < ix + 24 && mouseY >= iy && mouseY < iy + 24) {
                tooltip.addAll(AspectRenderer.getAspectTooltip(aspect, 0));
            }
        }

        boolean showPrev = page > 0;
        boolean showNext = start + count < all.size();
        if (showPrev || showNext) {
            graphics.fill(px + 20, py + H - 24, px + 32, py + H - 16, 0x40404040);
            graphics.fill(px + W - 32, py + H - 24, px + W - 20, py + H - 16, 0x40404040);
        }
        drawArrows(graphics, px, py, W, H, showPrev, showNext);
        return new Bounds(px, py, W, H);
    }

    // ==================== Recipe popups ====================

    /**
     * Draw the recipe popup for the given recipe id, centered at (cx, cy).
     * Returns the panel bounds.
     */
    public static Bounds drawRecipePopup(
            GuiGraphicsExtractor graphics, int cx, int cy, Identifier recipeId,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        Object recipe = RecipeRenderer.findRecipe(recipeId);
        if (recipe == null) {
            return drawNotfound(graphics, cx, cy, recipeId, font);
        }

        if (recipe instanceof IArcaneRecipe arcane) {
            return drawArcane(graphics, cx, cy, arcane, mouseX, mouseY, font, tooltip);
        }
        if (recipe instanceof CrucibleRecipeType crucible) {
            return drawCrucible(graphics, cx, cy, crucible, mouseX, mouseY, font, tooltip);
        }
        if (recipe instanceof CrucibleRecipe crucible) {
            return drawCrucible(graphics, cx, cy, crucible, mouseX, mouseY, font, tooltip);
        }
        if (recipe instanceof InfusionRecipeType infusion) {
            return drawInfusion(graphics, cx, cy, infusion, mouseX, mouseY, font, tooltip);
        }
        if (recipe instanceof CraftingRecipe crafting) {
            return drawCrafting(graphics, cx, cy, crafting, mouseX, mouseY, font, tooltip);
        }
        if (recipe instanceof SingleItemRecipe smelting) {
            return drawSmelting(graphics, cx, cy, smelting, mouseX, mouseY, font, tooltip);
        }
        if (recipe instanceof FakeRecipe fake) {
            return drawFake(graphics, cx, cy, fake, mouseX, mouseY, font, tooltip);
        }
        if (recipe instanceof BluePrint blueprint) {
            return drawBlueprint(graphics, cx, cy, blueprint, mouseX, mouseY, font, tooltip);
        }
        return drawNotfound(graphics, cx, cy, recipeId, font);
    }

    /** Arcane workbench: output on top, grid station below, crystals + vis at bottom. */
    private static Bounds drawArcane(
            GuiGraphicsExtractor graphics, int cx, int cy, IArcaneRecipe a,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {
        int W = 200, H = 240;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H, PAPER_GILDED);
        graphics.centeredText(font, "Arcane Workbench", cx, py + 20, 0xFF202020);

        // Output on top with a downward arrow
        ItemStack output = a.getResultItem();
        RecipeRenderer.renderItem(graphics, output, cx - 8, py + 38);
        tooltip = RecipeRenderer.checkItemTooltip(output, cx - 8, py + 38, mouseX, mouseY, tooltip);
        drawDownArrow(graphics, cx, py + 58, py + 74);

        // Arcane workbench grid line-art + 3x3 item slots
        drawArt(graphics, UV_ARCANE_GRID, cx - 49, py + 78, 98, 98);
        List<Optional<Ingredient>> ingredients = List.of();
        if (a instanceof ShapedArcaneRecipe sar) {
            ingredients = sar.getIngredients();
        } else if (a instanceof ShapelessArcaneRecipe slar) {
            ingredients = slar.getIngredients().stream().map(Optional::of).toList();
        }
        int slot = 30;
        int gx = cx - 47, gy = py + 80;
        for (int i = 0; i < ingredients.size() && i < 9; i++) {
            Optional<Ingredient> oing = ingredients.get(i);
            if (oing.isEmpty()) continue;
            int col = i % 3, row = i / 3;
            int x = gx + col * (slot + 2) + 7, y = gy + row * (slot + 2) + 7;
            drawSlot(graphics, x - 7, y - 7, slot);
            Ingredient ing = oing.get();
            ItemStack is = RecipeRenderer.cycleIngredient(ing, i);
            RecipeRenderer.renderItem(graphics, is, x, y);
            tooltip = RecipeRenderer.checkItemTooltip(is, x, y, mouseX, mouseY, tooltip);
        }

        // Crystals (aspect row) + vis cost at the bottom
        tooltip = drawAspectRows(graphics, cx, py + 186, a.getCrystals(), font,
                mouseX, mouseY, tooltip);
        drawVisCost(graphics, font, cx, py + 214, a.getVis());

        return new Bounds(px, py, W, H);
    }

    /** Crucible: output on top, flame + pot station below, catalyst inside, aspects at bottom. */
    private static Bounds drawCrucible(
            GuiGraphicsExtractor graphics, int cx, int cy, CrucibleRecipeType t,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {
        int W = 200, H = 240;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H, PAPER_GILDED);
        graphics.centeredText(font, "Crucible", cx, py + 20, 0xFF202020);

        // Output on top
        ItemStack output = t.getResultItem();
        RecipeRenderer.renderItem(graphics, output, cx - 8, py + 38);
        tooltip = RecipeRenderer.checkItemTooltip(output, cx - 8, py + 38, mouseX, mouseY, tooltip);

        // Flame with downward arrow, then the crucible pot line-art
        drawArt(graphics, UV_FLAME_ARROW, cx - 14, py + 56, 28, 62);
        drawArt(graphics, UV_CRUCIBLE_POT, cx - 52, py + 104, 105, 91);

        // Catalyst inside the pot
        Ingredient catalyst = t.getCatalyst();
        if (catalyst != null && !catalyst.isEmpty()) {
            ItemStack cat = RecipeRenderer.cycleIngredient(catalyst, 0);
            RecipeRenderer.renderItem(graphics, cat, cx - 8, py + 130);
            tooltip = RecipeRenderer.checkItemTooltip(cat, cx - 8, py + 130, mouseX, mouseY, tooltip);
        }

        // Aspects at the bottom
        tooltip = drawAspectRows(graphics, cx, py + 204, t.getAspects(), font,
                mouseX, mouseY, tooltip);

        return new Bounds(px, py, W, H);
    }

    /** Crucible: output on top, flame + pot station below, catalyst inside, aspects at bottom. */
    private static Bounds drawCrucible(
            GuiGraphicsExtractor graphics, int cx, int cy, CrucibleRecipe c,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {
        int W = 200, H = 240;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H, PAPER_GILDED);
        graphics.centeredText(font, "Crucible", cx, py + 20, 0xFF202020);

        ItemStack output = c.getRecipeOutput();
        RecipeRenderer.renderItem(graphics, output, cx - 8, py + 38);
        tooltip = RecipeRenderer.checkItemTooltip(output, cx - 8, py + 38, mouseX, mouseY, tooltip);

        drawArt(graphics, UV_FLAME_ARROW, cx - 14, py + 56, 28, 62);
        drawArt(graphics, UV_CRUCIBLE_POT, cx - 52, py + 104, 105, 91);

        Ingredient catalyst = c.getCatalyst();
        if (catalyst != null && !catalyst.isEmpty()) {
            ItemStack cat = RecipeRenderer.cycleIngredient(catalyst, 0);
            RecipeRenderer.renderItem(graphics, cat, cx - 8, py + 130);
            tooltip = RecipeRenderer.checkItemTooltip(cat, cx - 8, py + 130, mouseX, mouseY, tooltip);
        }

        tooltip = drawAspectRows(graphics, cx, py + 204, c.getAspects(), font,
                mouseX, mouseY, tooltip);

        return new Bounds(px, py, W, H);
    }

    /** Infusion altar: 8 component slots around the central item, altar line-art below. */
    private static Bounds drawInfusion(
            GuiGraphicsExtractor graphics, int cx, int cy, InfusionRecipeType i,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {
        int W = 190, H = 240;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H, PAPER_GILDED);
        graphics.centeredText(font, "Infusion Altar", cx, py + 20, 0xFF202020);

        // Infusion altar diamond line-art (background, behind the center item)
        drawArt(graphics, UV_ALTAR_DIAMOND, cx - 53, py + 110, 106, 53);

        // Central item
        ItemStack central = i.getCentralItem() == null
                ? ItemStack.EMPTY : RecipeRenderer.cycleIngredient(i.getCentralItem(), 0);
        if (!central.isEmpty()) {
            RecipeRenderer.renderItem(graphics, central, cx - 8, py + 96);
            tooltip = RecipeRenderer.checkItemTooltip(central, cx - 8, py + 96, mouseX, mouseY, tooltip);
        }

        // 8 component slots: cardinal (62px from center) + diagonal (44px)
        int[] dx = {0, 62, 0, -62, 44, 44, -44, -44};
        int[] dy = {-62, 0, 62, 0, -44, 44, 44, -44};
        List<Ingredient> comps = i.getIngredients();
        for (int k = 0; k < Math.min(8, comps.size()); k++) {
            int sx = cx - 10 + dx[k], sy = py + 94 + dy[k];
            drawSlot(graphics, sx, sy, 20);
            Ingredient c = comps.get(k);
            if (c != null && !c.isEmpty()) {
                ItemStack is = RecipeRenderer.cycleIngredient(c, k);
                RecipeRenderer.renderItem(graphics, is, sx + 2, sy + 2);
                tooltip = RecipeRenderer.checkItemTooltip(is, sx + 2, sy + 2, mouseX, mouseY, tooltip);
            }
        }

        // Aspects + instability at the bottom
        tooltip = drawAspectRows(graphics, cx, py + 178, i.getAspects(), font,
                mouseX, mouseY, tooltip);
        graphics.text(font, "Instability: " + i.getInstability(),
                px + 20, py + 212, 0xFF505050, false);

        return new Bounds(px, py, W, H);
    }

    /** Crafting table: 3x3 grid on the left, output on the right with an arrow. */
    private static Bounds drawCrafting(
            GuiGraphicsExtractor graphics, int cx, int cy, CraftingRecipe cr,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {
        int W = 170, H = 165;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H, PAPER_GILDED);
        graphics.centeredText(font, "Crafting", cx, py + 20, 0xFF202020);

        List<Ingredient> ingredients = new ArrayList<>();
        if (cr instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
            List<Optional<Ingredient>> slots = shaped.getIngredients();
            int n = shaped.getWidth() * shaped.getHeight();
            for (int i = 0; i < n; i++) {
                ingredients.add(i < slots.size() ? slots.get(i).orElse(null) : null);
            }
        } else {
            for (Ingredient slot : cr.placementInfo().ingredients()) {
                ingredients.add(slot);
            }
        }
        int slotSize = 20;
        int gx = cx - 32, gy = py + 40;
        for (int i = 0; i < Math.min(9, ingredients.size()); i++) {
            Ingredient ing = ingredients.get(i);
            if (ing == null || ing.isEmpty()) continue;
            int x = gx + (i % 3) * (slotSize + 2) + 2, y = gy + (i / 3) * (slotSize + 2) + 2;
            drawSlot(graphics, x - 2, y - 2, slotSize);
            ItemStack is = RecipeRenderer.cycleIngredient(ing, i);
            RecipeRenderer.renderItem(graphics, is, x, y);
            tooltip = RecipeRenderer.checkItemTooltip(is, x, y, mouseX, mouseY, tooltip);
        }

        // Arrow + output
        drawStraightArrow(graphics, cx + 36, py + 70, cx + 44);
        ItemStack output = RecipeRenderer.resolveOutput(cr);
        RecipeRenderer.renderItem(graphics, output, cx + 46, py + 62);
        tooltip = RecipeRenderer.checkItemTooltip(output, cx + 46, py + 62, mouseX, mouseY, tooltip);

        return new Bounds(px, py, W, H);
    }

    /** Furnace: input -> arrow -> output. */
    private static Bounds drawSmelting(
            GuiGraphicsExtractor graphics, int cx, int cy, SingleItemRecipe sr,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {
        int W = 150, H = 120;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H, PAPER_GILDED);
        graphics.centeredText(font, "Furnace", cx, py + 20, 0xFF202020);

        ItemStack input = RecipeRenderer.cycleIngredient(sr.input(), 0);
        tooltip = RecipeRenderer.checkItemTooltip(input, cx - 48, py + 54, mouseX, mouseY, tooltip);

        drawStraightArrow(graphics, cx - 28, py + 62, cx - 4);

        ItemStack output = RecipeRenderer.resolveOutput(sr);
        tooltip = RecipeRenderer.checkItemTooltip(output, cx + 8, py + 54, mouseX, mouseY, tooltip);

        return new Bounds(px, py, W, H);
    }

    /** Multiblock blueprint: item + ingredient row. */
    private static Bounds drawBlueprint(
            GuiGraphicsExtractor graphics, int cx, int cy, BluePrint bp,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {
        int W = 150, H = 160;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H, PAPER_GILDED);
        graphics.centeredText(font, "Multiblock", cx, py + 20, 0xFF202020);

        ItemStack item = bp.getDisplayStack();
        if (item == null) item = ItemStack.EMPTY;
        RecipeRenderer.renderItem(graphics, item, cx - 8, py + 40);
        tooltip = RecipeRenderer.checkItemTooltip(item, cx - 8, py + 40, mouseX, mouseY, tooltip);

        ItemStack[] parts = bp.getIngredientList();
        int n = Math.min(6, parts.length);
        int ix = cx - (n * 20) / 2;
        for (int i = 0; i < n; i++) {
            ItemStack p = parts[i];
            RecipeRenderer.renderItem(graphics, p, ix + i * 20, py + 74);
            tooltip = RecipeRenderer.checkItemTooltip(p, ix + i * 20, py + 74, mouseX, mouseY, tooltip);
        }
        if (parts.length > 6) {
            graphics.text(font, "...", cx + 62, py + 80, 0xFF505050, false);
        }

        return new Bounds(px, py, W, H);
    }

    /** Fake/display recipe: title + item row. */
    private static Bounds drawFake(
            GuiGraphicsExtractor graphics, int cx, int cy, FakeRecipe fr,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {
        int W = 190, H = 150;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H, PAPER_GILDED);
        graphics.centeredText(font, fr.getTitle(), cx, py + 20, 0xFF202020);

        ItemStack[] items = fr.getItems();
        if (items.length > 0) {
            int ix = cx - (items.length * 20) / 2;
            for (ItemStack s : items) {
                RecipeRenderer.renderItem(graphics, s, ix, py + 50);
                tooltip = RecipeRenderer.checkItemTooltip(s, ix, py + 50, mouseX, mouseY, tooltip);
                ix += 20;
            }
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate(px + 30, py + 50);
            graphics.pose().scale(0.75f);
            graphics.text(font, "Dynamic recipe (see in-game station)", 0, 0, 0xFF808080, false);
            graphics.pose().popMatrix();
        }

        return new Bounds(px, py, W, H);
    }

    /** "Recipe not found" fallback. */
    private static Bounds drawNotfound(
            GuiGraphicsExtractor graphics, int cx, int cy, Identifier recipeId, Font font) {
        int W = 150, H = 90;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H, PAPER_GILDED);
        graphics.centeredText(font, "Recipe not found", cx, py + 26, 0xFF800000);
        if (recipeId != null) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(px + 16, py + 46);
            graphics.pose().scale(0.7f);
            graphics.text(font, recipeId.toString(), 0, 0, 0xFF505050, false);
            graphics.pose().popMatrix();
        }
        return new Bounds(px, py, W, H);
    }

    // ==================== Shared drawing helpers ====================

    /** Draw a parchment panel (full 256x256 texture stretched to w x h). */
    private static void drawPanel(GuiGraphicsExtractor graphics, int x, int y,
            int w, int h, Identifier tex) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, tex,
                x, y, 0f, 0f, w, h, 256, 256);
    }

    /** Blit a station line-art region from the overlay texture at a target size. */
    private static void drawArt(GuiGraphicsExtractor graphics, int[] uv,
            int x, int y, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY,
                x, y, (float) uv[0], (float) uv[1], w, h, 512, 512);
    }

    /** Light slot backdrop behind an item. */
    private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0x30FFFFFF);
    }

    /** Simple downward arrow (line + filled head). */
    private static void drawDownArrow(GuiGraphicsExtractor graphics, int x, int y1, int y2) {
        graphics.fill(x - 1, y1, x + 1, y2 - 4, 0xFF505050);
        graphics.fill(x - 3, y2 - 4, x + 3, y2 - 3, 0xFF505050);
        graphics.fill(x - 2, y2 - 3, x + 2, y2 - 2, 0xFF505050);
        graphics.fill(x - 1, y2 - 2, x + 1, y2 - 1, 0xFF505050);
        graphics.fill(x, y2 - 1, x + 1, y2, 0xFF505050);
    }

    /** Simple rightward arrow (line + filled head). */
    private static void drawStraightArrow(GuiGraphicsExtractor graphics, int x1, int y1, int x2) {
        graphics.fill(x1, y1 - 1, x2 - 3, y1 + 1, 0xFF505050);
        graphics.fill(x2 - 3, y1 - 2, x2, y1, 0xFF505050);
        graphics.fill(x2 - 2, y1 - 3, x2 + 1, y1 + 1, 0xFF505050);
        graphics.fill(x2 - 3, y1, x2 - 1, y1 + 2, 0xFF505050);
    }

    /**
     * Draw an aspect list in up to two centered rows (max 8 aspects).
     * Each aspect: icon + optional amount; hover shows the tooltip.
     */
    private static List<Component> drawAspectRows(
            GuiGraphicsExtractor graphics, int cx, int y, AspectList aspects,
            Font font, int mouseX, int mouseY, List<Component> tooltip) {
        if (aspects == null || aspects.aspects.isEmpty()) {
            return tooltip;
        }
        List<Map.Entry<Aspect, Integer>> list = new ArrayList<>(aspects.aspects.entrySet());
        int n = Math.min(8, list.size());
        int row1 = Math.min(4, n);
        int row2 = n - row1;
        for (int i = 0; i < row1; i++) {
            int x = cx - (row1 * 22) / 2 + i * 22;
            drawAspectIcon(graphics, x, y, list.get(i), font, mouseX, mouseY, tooltip);
        }
        for (int i = row1; i < n; i++) {
            int x = cx - (row2 * 22) / 2 + (i - row1) * 22;
            drawAspectIcon(graphics, x, y + 14, list.get(i), font, mouseX, mouseY, tooltip);
        }
        return tooltip;
    }

    private static void drawAspectIcon(
            GuiGraphicsExtractor graphics, int x, int y, Map.Entry<Aspect, Integer> e,
            Font font, int mouseX, int mouseY, List<Component> tooltip) {
        AspectRenderer.drawAspect(graphics, x, y, e.getKey());
        if (e.getValue() > 1) {
            graphics.text(font, String.valueOf(e.getValue()), x + 18, y + 3, 0xFF404040, false);
        }
        if (AspectRenderer.isMouseOverAspect(x, y, mouseX, mouseY)) {
            tooltip.addAll(AspectRenderer.getAspectTooltip(e.getKey(), e.getValue()));
        }
    }

    /** Vis cost: vis icon + number, centered. */
    private static void drawVisCost(
            GuiGraphicsExtractor graphics, Font font, int cx, int y, int vis) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, VIS_ICON,
                cx - 32, y, 0f, 0f, 16, 16, 16, 16);
        graphics.text(font, String.valueOf(vis), cx - 12, y + 2, 0xFF404040, false);
    }

    /**
     * Red page arrows from the book texture (bottom corners of the panel).
     */
    private static void drawArrows(
            GuiGraphicsExtractor graphics, int px, int py, int W, int H,
            boolean showPrev, boolean showNext) {
        if (showPrev) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK, px + 20, py + H - 24,
                    ARROW_TEX_X0, ARROW_TEX_Y, 12, 8, 256, 256);
        }
        if (showNext) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK, px + W - 32, py + H - 24,
                    ARROW_TEX_X1, ARROW_TEX_Y, 12, 8, 256, 256);
        }
    }
}
