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
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.FakeRecipe;
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
 * Renders the Thaumonomicon recipe and aspect popups (1.12-style overlay panels
 * that appear when clicking the book's right-side recipe bookmarks or the
 * left-side aspect bookmark).
 *
 * <p>Each popup draws a gilded-paper panel centered on the book and returns
 * its bounds so the screen can hit-test clicks (inside = no-op, outside =
 * close the popup).
 */
@OnlyIn(Dist.CLIENT)
public class BookPopupRenderer {

    private static final Identifier PAPER = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/papergilded.png");
    private static final Identifier RING = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/arcaneworkbench.png");
    private static final Identifier VIS_ICON = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/costvis.png");
    private static final Identifier BOOK = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/gui_researchbook.png");

    private static final int ARROW_TEX_X0 = 0;    // red "previous" arrow in gui_researchbook.png
    private static final int ARROW_TEX_X1 = 12;   // red "next" arrow
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
        return new Bounds(b.x + 8, b.y + b.h - 18, 12, 8);
    }

    /**
     * Hit rect for the "next page" arrow of a popup panel.
     */
    public static Bounds nextArrow(Bounds b) {
        return new Bounds(b.x + b.w - 20, b.y + b.h - 18, 12, 8);
    }

    /**
     * Draw the "Aspects of Essentia" popup: 5 aspects per page, paged with the
     * red arrows at the panel's bottom corners.
     */
    public static Bounds drawAspectPopup(
            GuiGraphicsExtractor graphics, int cx, int cy, int page,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        tooltip.clear();

        int W = 150, H = 175;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H);

        graphics.centeredText(font, Component.translatable("tc.aspect.name").getString(),
                cx, py + 8, 0xFF202020);

        List<Aspect> all = new ArrayList<>(Aspect.aspects.values());
        int start = page * ASPECTS_PER_PAGE;
        for (int i = 0; i < ASPECTS_PER_PAGE && start + i < all.size(); i++) {
            Aspect aspect = all.get(start + i);
            int y = py + 34 + i * 26;
            AspectRenderer.drawAspect(graphics, cx - 48, y, aspect);
            graphics.centeredText(font, aspect.getName(), cx - 40, y + 17, 0xFF404040);
            graphics.text(font, aspect.isPrimal() ? "Primal Aspect" : "Secondary Aspect",
                    cx + 4, y + 4, 0xFF606060, false);
            if (AspectRenderer.isMouseOverAspect(cx - 48, y, mouseX, mouseY)) {
                tooltip.addAll(AspectRenderer.getAspectTooltip(aspect, 0));
            }
        }

        drawArrows(graphics, px, py, W, H, page > 0, page < aspectPageCount() - 1);
        return new Bounds(px, py, W, H);
    }

    // ==================== Recipe popups ====================

    /**
     * Draw the recipe popup for the given recipe id, centered at (cx, cy).
     * Returns the panel bounds (null if the recipe could not be resolved).
     */
    public static Bounds drawRecipePopup(
            GuiGraphicsExtractor graphics, int cx, int cy, Identifier recipeId,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        tooltip.clear();
        Object recipe = RecipeRenderer.findRecipe(recipeId);
        if (recipe == null) {
            int W = 150, H = 90;
            int px = cx - W / 2, py = cy - H / 2;
            drawPanel(graphics, px, py, W, H);
            graphics.centeredText(font, "Recipe not found", cx, py + 38, 0xFF904040);
            return new Bounds(px, py, W, H);
        }

        if (recipe instanceof IArcaneRecipe arcane) {
            int W = 190, H = 225;
            int px = cx - W / 2, py = cy - H / 2;
            drawPanel(graphics, px, py, W, H);
            drawArcane(graphics, px, py, W, H, arcane, mouseX, mouseY, font, tooltip);
            return new Bounds(px, py, W, H);
        } else if (recipe instanceof CrucibleRecipeType || recipe instanceof CrucibleRecipe) {
            int W = 190, H = 220;
            int px = cx - W / 2, py = cy - H / 2;
            drawPanel(graphics, px, py, W, H);
            drawCrucible(graphics, px, py, W, H, recipe, mouseX, mouseY, font, tooltip);
            return new Bounds(px, py, W, H);
        } else if (recipe instanceof InfusionRecipeType || recipe instanceof InfusionRecipe) {
            int W = 165, H = 190;
            int px = cx - W / 2, py = cy - H / 2;
            drawPanel(graphics, px, py, W, H);
            drawInfusion(graphics, cx, py, H, recipe, mouseX, mouseY, font, tooltip);
            return new Bounds(px, py, W, H);
        } else if (recipe instanceof CraftingRecipe crafting) {
            int W = 135, H = 150;
            int px = cx - W / 2, py = cy - H / 2;
            drawPanel(graphics, px, py, W, H);
            drawCrafting(graphics, cx, py, H, crafting, mouseX, mouseY, font, tooltip);
            return new Bounds(px, py, W, H);
        } else if (recipe instanceof SingleItemRecipe single) {
            int W = 135, H = 115;
            int px = cx - W / 2, py = cy - H / 2;
            drawPanel(graphics, px, py, W, H);
            drawSmelting(graphics, cx, py, single, mouseX, mouseY, font, tooltip);
            return new Bounds(px, py, W, H);
        } else if (recipe instanceof thaumcraft.api.ThaumcraftApi.BluePrint bp) {
            int W = 150, H = 140;
            int px = cx - W / 2, py = cy - H / 2;
            drawPanel(graphics, px, py, W, H);
            drawBlueprint(graphics, cx, py, bp, mouseX, mouseY, font, tooltip);
            return new Bounds(px, py, W, H);
        } else if (recipe instanceof FakeRecipe fake) {
            int W = 190, H = 140;
            int px = cx - W / 2, py = cy - H / 2;
            drawPanel(graphics, px, py, W, H);
            drawFake(graphics, px, py, W, H, fake, mouseX, mouseY, font, tooltip);
            return new Bounds(px, py, W, H);
        }

        int W = 150, H = 90;
        int px = cx - W / 2, py = cy - H / 2;
        drawPanel(graphics, px, py, W, H);
        graphics.centeredText(font, recipe.getClass().getSimpleName(), cx, py + 38, 0xFF904040);
        return new Bounds(px, py, W, H);
    }

    // ==================== Per-type content ====================

    private static void drawArcane(
            GuiGraphicsExtractor graphics, int px, int py, int W, int H, IArcaneRecipe recipe,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        int cx = px + W / 2;
        graphics.centeredText(font, "Arcane Workbench", cx, py + 30, 0xFF202020);

        // 3x3 ingredient grid (left)
        int gx = px + 30, gy = py + 52;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                drawSlot(graphics, gx + c * 18, gy + r * 18);
            }
        }
        List<Ingredient> ings = gridIngredients(recipe);
        for (int i = 0; i < Math.min(9, ings.size()); i++) {
            Ingredient ing = ings.get(i);
            if (ing == null || ing.isEmpty()) continue;
            ItemStack stack = RecipeRenderer.cycleIngredient(ing, i);
            int ix = gx + (i % 3) * 18, iy = gy + (i / 3) * 18;
            RecipeRenderer.renderItem(graphics, stack, ix, iy);
            tooltip = RecipeRenderer.checkItemTooltip(stack, ix, iy, mouseX, mouseY, tooltip);
        }

        // Workbench item below the grid (identifies the station)
        RecipeRenderer.renderItem(graphics, new ItemStack(thaumcraft.init.ModBlocks.ARCANE_WORKBENCH.get()), px + 49, py + 118);

        // Arrow: grid -> output
        drawStraightArrow(graphics, px + 92, py + 79, px + 118);

        // Output with decorative ring (right)
        ItemStack output = recipe.getResultItem();
        drawRing(graphics, px + 142, py + 79, 44);
        RecipeRenderer.renderItem(graphics, output, px + 134, py + 71);
        tooltip = RecipeRenderer.checkItemTooltip(output, px + 134, py + 71, mouseX, mouseY, tooltip);

        // Required crystals
        AspectList crystals = recipe.getCrystals();
        if (crystals != null && !crystals.aspects.isEmpty()) {
            int n = Math.min(6, crystals.aspects.size());
            int startX = px + (W - n * 22) / 2;
            int y = py + 152;
            int i = 0;
            for (Map.Entry<Aspect, Integer> e : crystals.aspects.entrySet()) {
                if (i >= 6) break;
                int x = startX + i * 22;
                AspectRenderer.drawAspect(graphics, x, y, e.getKey());
                if (e.getValue() > 1) {
                    graphics.text(font, String.valueOf(e.getValue()), x + 17, y + 3, 0xFF404040, false);
                }
                if (AspectRenderer.isMouseOverAspect(x, y, mouseX, mouseY)) {
                    tooltip.addAll(AspectRenderer.getAspectTooltip(e.getKey(), e.getValue()));
                }
                i++;
            }
        }

        // Vis cost
        int tw = font.width(String.valueOf(recipe.getVis()));
        graphics.blit(RenderPipelines.GUI_TEXTURED, VIS_ICON, px + (W / 2 - tw / 2 - 18), py + 186,
                0, 0, 16, 16, 16, 16);
        graphics.text(font, String.valueOf(recipe.getVis()), px + (W / 2 - tw / 2 + 2), py + 189,
                0xFF306090, false);
    }

    private static void drawCrucible(
            GuiGraphicsExtractor graphics, int px, int py, int W, int H, Object recipe,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        int cx = px + W / 2;
        graphics.centeredText(font, "Crucible", cx, py + 30, 0xFF202020);

        Ingredient catalyst;
        AspectList aspects;
        ItemStack output;
        if (recipe instanceof CrucibleRecipeType t) {
            catalyst = t.getCatalyst();
            aspects = t.getAspects();
            output = t.getResultItem();
        } else {
            CrucibleRecipe c = (CrucibleRecipe) recipe;
            catalyst = c.getCatalyst();
            aspects = c.getAspects();
            output = c.getRecipeOutput();
        }

        // Crucible image in the center (block item + soft backdrop)
        int cbx = px + 67, cby = py + 82;
        graphics.fill(cbx - 3, cby - 3, cbx + 19, cby + 19, 0x30402030);
        RecipeRenderer.renderItem(graphics, new ItemStack(thaumcraft.init.ModBlocks.CRUCIBLE.get()), cbx, cby);

        // Catalyst above the crucible (as in the in-game station)
        if (catalyst != null && !catalyst.isEmpty()) {
            ItemStack cat = RecipeRenderer.cycleIngredient(catalyst, 0);
            RecipeRenderer.renderItem(graphics, cat, cbx + 2, py + 56);
            tooltip = RecipeRenderer.checkItemTooltip(cat, cbx + 2, py + 56, mouseX, mouseY, tooltip);
        }

        // Arrow: crucible -> output
        drawStraightArrow(graphics, px + 92, py + 90, px + 120);

        // Output with decorative ring (right)
        drawRing(graphics, px + 142, py + 90, 44);
        RecipeRenderer.renderItem(graphics, output, px + 134, py + 82);
        tooltip = RecipeRenderer.checkItemTooltip(output, px + 134, py + 82, mouseX, mouseY, tooltip);

        // Aspects (up to 2 rows of 4)
        if (aspects != null && !aspects.aspects.isEmpty()) {
            int n = Math.min(8, aspects.aspects.size());
            int row1 = Math.min(4, n);
            int row2 = n - row1;
            int i = 0;
            for (Map.Entry<Aspect, Integer> e : aspects.aspects.entrySet()) {
                if (i >= n) break;
                int x = px + (W - row1 * 22) / 2 + (i % row1) * 22;
                int y = py + 150;
                AspectRenderer.drawAspect(graphics, x, y, e.getKey());
                if (e.getValue() > 1) {
                    graphics.text(font, String.valueOf(e.getValue()), x + 17, y + 3, 0xFF404040, false);
                }
                if (AspectRenderer.isMouseOverAspect(x, y, mouseX, mouseY)) {
                    tooltip.addAll(AspectRenderer.getAspectTooltip(e.getKey(), e.getValue()));
                }
                i++;
            }
            i = 0;
            for (Map.Entry<Aspect, Integer> e : aspects.aspects.entrySet()) {
                if (i >= row1) {
                    if (i - row1 >= row2) break;
                    int x = px + (W - row2 * 22) / 2 + (i - row1) * 22;
                    int y = py + 170;
                    AspectRenderer.drawAspect(graphics, x, y, e.getKey());
                    if (e.getValue() > 1) {
                        graphics.text(font, String.valueOf(e.getValue()), x + 17, y + 3, 0xFF404040, false);
                    }
                    if (AspectRenderer.isMouseOverAspect(x, y, mouseX, mouseY)) {
                        tooltip.addAll(AspectRenderer.getAspectTooltip(e.getKey(), e.getValue()));
                    }
                }
                i++;
            }
        }
    }

    private static void drawFake(
            GuiGraphicsExtractor graphics, int px, int py, int W, int H, FakeRecipe fake,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        int cx = px + W / 2;
        graphics.centeredText(font, fake.getTitle(), cx, py + 30, 0xFF202020);

        ItemStack[] items = fake.getItems();
        if (items.length > 0) {
            int startX = px + (W - items.length * 20) / 2;
            for (int i = 0; i < items.length; i++) {
                ItemStack stack = items[i];
                int x = startX + i * 20;
                RecipeRenderer.renderItem(graphics, stack, x, py + 75);
                tooltip = RecipeRenderer.checkItemTooltip(stack, x, py + 75, mouseX, mouseY, tooltip);
            }
        } else {
            graphics.centeredText(font, "(dynamic recipe)", cx, py + 75, 0xFF606060);
        }
    }

    // Straight arrow with filled head (left -> right)
    private static void drawStraightArrow(GuiGraphicsExtractor graphics, int x1, int y, int x2) {
        graphics.fill(x1, y - 1, x2 - 4, y + 1, 0xFF808080);
        graphics.fill(x2 - 5, y - 3, x2, y - 1, 0xFF808080);
        graphics.fill(x2 - 5, y + 1, x2, y + 3, 0xFF808080);
        graphics.fill(x2 - 2, y - 1, x2, y + 1, 0xFF808080);
    }

    private static void drawInfusion(
            GuiGraphicsExtractor graphics, int cx, int py, int H, Object recipe,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        graphics.centeredText(font, "Infusion Altar", cx, py + 8, 0xFF202020);

        Ingredient center;
        List<Ingredient> components;
        AspectList aspects;
        int instability;
        if (recipe instanceof InfusionRecipeType t) {
            center = t.getCentralItem();
            components = t.getComponents();
            aspects = t.getAspects();
            instability = t.getInstability();
        } else {
            InfusionRecipe inf = (InfusionRecipe) recipe;
            center = inf.getRecipeInput();
            components = new ArrayList<>(inf.getComponents());
            aspects = inf.getAspects();
            try {
                instability = inf.getInstability(
                        net.minecraft.client.Minecraft.getInstance().player,
                        center != null ? RecipeRenderer.cycleIngredient(center, 0) : ItemStack.EMPTY,
                        new ArrayList<>());
            } catch (Throwable t) {
                instability = 0;
            }
        }

        // Altar "image": green table circle + 8 pedestals
        int acx = cx, acy = py + 70;
        drawCircle(graphics, acx, acy, 24, 0xFF2E4A2E);
        drawCircle(graphics, acx, acy, 20, 0xFF4A6C4A);

        for (int i = 0; i < 8; i++) {
            double ang = Math.PI / 4 * i - Math.PI / 2;
            int pxp = acx + (int) (Math.cos(ang) * 36);
            int pyp = acy + (int) (Math.sin(ang) * 36);
            graphics.fill(pxp - 5, pyp - 5, pxp + 5, pyp + 5, 0xFF606060);
            if (components != null && i < components.size()) {
                Ingredient comp = components.get(i);
                if (comp == null || comp.isEmpty()) continue;
                ItemStack it = RecipeRenderer.cycleIngredient(comp, i);
                RecipeRenderer.renderItem(graphics, it, pxp - 8, pyp - 8);
                tooltip = RecipeRenderer.checkItemTooltip(it, pxp - 8, pyp - 8, mouseX, mouseY, tooltip);
            }
        }

        // Central item
        if (center != null && !center.isEmpty()) {
            ItemStack it = RecipeRenderer.cycleIngredient(center, 0);
            RecipeRenderer.renderItem(graphics, it, acx - 8, acy - 8);
            tooltip = RecipeRenderer.checkItemTooltip(it, acx - 8, acy - 8, mouseX, mouseY, tooltip);
        }

        // Aspects (up to 2 rows of 4)
        if (aspects != null && !aspects.aspects.isEmpty()) {
            int i = 0;
            for (Map.Entry<Aspect, Integer> e : aspects.aspects.entrySet()) {
                int x = cx - 60 + (i % 4) * 34;
                int y = py + 116 + (i / 4) * 18;
                AspectRenderer.drawAspect(graphics, x, y, e.getKey());
                if (e.getValue() > 1) {
                    graphics.text(font, String.valueOf(e.getValue()), x + 17, y + 3, 0xFF404040, false);
                }
                if (AspectRenderer.isMouseOverAspect(x, y, mouseX, mouseY)) {
                    tooltip.addAll(AspectRenderer.getAspectTooltip(e.getKey(), e.getValue()));
                }
                i++;
            }
        }

        // Instability
        if (instability > 0) {
            graphics.centeredText(font, "Instability: " + instability, cx, py + H - 18,
                    instability > 3 ? 0xFFC04040 : 0xFF806020);
        }
    }

    private static void drawCrafting(
            GuiGraphicsExtractor graphics, int cx, int py, int H, CraftingRecipe recipe,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        graphics.centeredText(font, "Crafting", cx, py + 8, 0xFF202020);

        ItemStack output = RecipeRenderer.resolveOutput(recipe);
        drawRing(graphics, cx, py + 36, 36);
        RecipeRenderer.renderItem(graphics, output, cx - 8, py + 28);
        tooltip = RecipeRenderer.checkItemTooltip(output, cx - 8, py + 28, mouseX, mouseY, tooltip);

        int gx = cx - 27, gy = py + 62;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                drawSlot(graphics, gx + c * 18, gy + r * 18);
            }
        }
        List<Ingredient> ings = gridIngredients(recipe);
        for (int i = 0; i < Math.min(9, ings.size()); i++) {
            Ingredient ing = ings.get(i);
            if (ing == null || ing.isEmpty()) continue;
            ItemStack stack = RecipeRenderer.cycleIngredient(ing, i);
            int ix = gx + (i % 3) * 18, iy = gy + (i / 3) * 18;
            RecipeRenderer.renderItem(graphics, stack, ix, iy);
            tooltip = RecipeRenderer.checkItemTooltip(stack, ix, iy, mouseX, mouseY, tooltip);
        }
    }

    private static void drawSmelting(
            GuiGraphicsExtractor graphics, int cx, int py, SingleItemRecipe recipe,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        graphics.centeredText(font, "Furnace", cx, py + 8, 0xFF202020);

        ItemStack input = RecipeRenderer.cycleIngredient(recipe.input(), 0);
        RecipeRenderer.renderItem(graphics, input, cx - 42, py + 50);
        tooltip = RecipeRenderer.checkItemTooltip(input, cx - 42, py + 50, mouseX, mouseY, tooltip);

        graphics.text(font, "\u2192", cx - 14, py + 54, 0xFF404040, false);

        ItemStack output = recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(ItemStack.EMPTY));
        RecipeRenderer.renderItem(graphics, output, cx + 22, py + 50);
        tooltip = RecipeRenderer.checkItemTooltip(output, cx + 22, py + 50, mouseX, mouseY, tooltip);
    }

    private static void drawBlueprint(
            GuiGraphicsExtractor graphics, int cx, int py,
            thaumcraft.api.ThaumcraftApi.BluePrint bp,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        graphics.centeredText(font, "Multiblock", cx, py + 8, 0xFF202020);

        ItemStack display = bp.getDisplayStack();
        RecipeRenderer.renderItem(graphics, display, cx - 8, py + 26);
        tooltip = RecipeRenderer.checkItemTooltip(display, cx - 8, py + 26, mouseX, mouseY, tooltip);

        ItemStack[] ingredients = bp.getIngredientList();
        int shown = Math.min(ingredients.length, 6);
        int startX = cx - (shown * 18 - 12) / 2;
        for (int i = 0; i < shown; i++) {
            ItemStack ing = ingredients[i];
            int ix = startX + i * 18;
            RecipeRenderer.renderItem(graphics, ing, ix, py + 66);
            tooltip = RecipeRenderer.checkItemTooltip(ing, ix, py + 66, mouseX, mouseY, tooltip);
        }
        if (ingredients.length > 6) {
            graphics.text(font, "...", cx + (shown * 18 - 12) / 2 + 2, py + 70, 0xFF808080, false);
        }
    }

    // ==================== Shared helpers ====================

    /**
     * Normalize a recipe's ingredients into a row-major list of up to 9 slots
     * (null = empty slot).
     */
    private static List<Ingredient> gridIngredients(Object recipe) {
        List<Ingredient> out = new ArrayList<>();
        if (recipe instanceof ShapedArcaneRecipe shaped) {
            int n = shaped.getWidth() * shaped.getHeight();
            List<Optional<Ingredient>> slots = shaped.getIngredients();
            for (int i = 0; i < n; i++) {
                out.add(i < slots.size() ? slots.get(i).orElse(null) : null);
            }
        } else if (recipe instanceof ShapelessArcaneRecipe shapeless) {
            out.addAll(shapeless.getIngredients());
        } else if (recipe instanceof CraftingRecipe crafting) {
            if (crafting instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
                List<Optional<Ingredient>> slots = shaped.getIngredients();
                int n = shaped.getWidth() * shaped.getHeight();
                for (int i = 0; i < n; i++) {
                    out.add(i < slots.size() ? slots.get(i).orElse(null) : null);
                }
            } else {
                for (var slot : crafting.placementInfo().ingredients()) {
                    out.add(slot);
                }
            }
        }
        while (out.size() < 9) {
            out.add(null);
        }
        return out;
    }

    /**
     * Gilded-paper panel background, scaled from the 128x128 texture.
     */
    private static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(w / 128f, h / 128f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PAPER, 0, 0, 0, 0, 128, 128, 128, 128);
        graphics.pose().popMatrix();
    }

    /**
     * The white decorative ring from the arcane workbench texture (94,0,32,32).
     */
    private static void drawRing(GuiGraphicsExtractor graphics, int cx, int cy, int size) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx - size / 2f, cy - size / 2f);
        graphics.pose().scale(size / 32f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, RING, 0, 0, 94, 0, 32, 32, 128, 128);
        graphics.pose().popMatrix();
    }

    /**
     * A flat gray slot square (16x16 item area).
     */
    private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x30202020);
    }

    /**
     * Filled circle approximated with horizontal rows.
     */
    private static void drawCircle(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            double frac = (double) dy / radius;
            int half = (int) (radius * Math.sqrt(Math.max(0, 1 - frac * frac)));
            graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    /**
     * A shallow curved arrow from (x0,y0) to (x1,y1) with the apex raised.
     */
    private static void drawCurvedArrow(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        int apexY = Math.min(y0, y1) - 14;
        int steps = 16;
        for (int s = 0; s <= steps; s++) {
            float t = s / (float) steps;
            float a = (1 - t) * (1 - t);
            float b = 2 * (1 - t) * t;
            float c = t * t;
            int mx = (int) (a * x0 + b * ((x0 + x1) / 2f) + c * x1);
            int my = (int) (a * y0 + b * apexY + c * y1);
            graphics.fill(mx, my, mx + 1, my + 1, 0xFF505050);
        }
        // Arrowhead
        graphics.fill(x1 - 1, y1 - 4, x1 + 2, y1 - 2, 0xFF505050);
        graphics.fill(x1 - 3, y1 - 2, x1 + 2, y1 + 1, 0xFF505050);
    }

    /**
     * Red page arrows from the book texture (bottom corners of the panel).
     */
    private static void drawArrows(
            GuiGraphicsExtractor graphics, int px, int py, int W, int H,
            boolean showPrev, boolean showNext) {
        if (showPrev) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK, px + 8, py + H - 18,
                    ARROW_TEX_X0, ARROW_TEX_Y, 12, 8, 256, 256);
        }
        if (showNext) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK, px + W - 20, py + H - 18,
                    ARROW_TEX_X1, ARROW_TEX_Y, 12, 8, 256, 256);
        }
    }
}
