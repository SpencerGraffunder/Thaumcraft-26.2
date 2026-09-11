package thaumcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.FakeRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.client.lib.AspectRenderer;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.common.lib.crafting.InfusionRecipeType;
import thaumcraft.common.lib.crafting.ShapelessArcaneRecipe;
import thaumcraft.common.lib.crafting.ShapedArcaneRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Renders the 1.12-style Thaumonomicon popups (recipe pages + "Aspects of
 * Essentia" page) over the book.
 *
 * <p>Layout is a faithful port of 1.12's {@code GuiResearchPage} (BETA26):
 * a 255×255 paper panel centered on the book, station line-art from
 * {@code gui_researchbook_overlay.png} (the byte-identical 1.12 texture) drawn
 * with the exact 1.12 UVs/scales, and 16×16 items with count overlays — no
 * slot backgrounds, matching how the old book rendered items.
 *
 * <p>1.12 addressed the 512×512 overlay with 256-space UVs under a 2× GL
 * scale, so a 1.12 {@code drawTexturedModalRect(lx, ly, u, v, w, h)} becomes
 * {@code blit(OVERLAY, cx+2lx, cy+2ly, 2u, 2v, 2w, 2h, 512, 512)} — see
 * {@link #blit2x}.
 */
@OnlyIn(Dist.CLIENT)
public class BookPopupRenderer {

    private static final Identifier PAPER = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/paper.png");
    private static final Identifier OVERLAY = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/gui_researchbook_overlay.png");
    private static final Identifier BOOK = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/gui_researchbook.png");
    private static final Identifier ASPECT_BACK = Identifier.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/aspects/_back.png");

    private static final int PANEL = 255; // 1.12 paper panel size
    private static final int TEXT_DARK = 0xFF505050;   // 1.12 5263440
    private static final int TEXT_MID = 0xFF777777;    // 1.12 7829367
    private static final int TEXT_SYM = 0xFF999999;    // 1.12 10066329

    private BookPopupRenderer() {}

    // ==================== Bounds / paging arrows ====================

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

    /** 1.12 drawRecipe: prev arrow at (panelX+40, panelY+232), 12×8. */
    public static Bounds prevArrow(Bounds b) {
        return new Bounds(b.x + 40, b.y + 232, 12, 8);
    }

    /** 1.12 drawRecipe: next arrow at (panelX+204, panelY+232), 12×8. */
    public static Bounds nextArrow(Bounds b) {
        return new Bounds(b.x + 204, b.y + 232, 12, 8);
    }

    // ==================== Aspect popup ====================

    public static final int ASPECTS_PER_PAGE = 5;

    public static int aspectCount() {
        return Aspect.aspects.size();
    }

    public static int aspectPageCount() {
        return Math.max(1, (Aspect.aspects.size() + ASPECTS_PER_PAGE - 1) / ASPECTS_PER_PAGE);
    }

    /**
     * 1.12 drawAspectsInsert/drawAspectPage: paper panel; 5 rows of 40px with a
     * 24px aspect icon + name on the left and "= comp0 + comp1" (or
     * "Primal Aspect") on the right; red book arrows at the bottom.
     */
    public static Bounds drawAspectPopup(
            GuiGraphicsExtractor graphics, int cx, int cy, int page,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        int px = cx - PANEL / 2;
        int py = cy - PANEL / 2;
        drawPaper(graphics, px, py);

        // 1.12 content origin: (panelX+60, panelY+24)
        int ox = px + 60;
        int oy = py + 24;

        List<Aspect> all = new ArrayList<>(Aspect.aspects.values());
        all.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        int start = page * ASPECTS_PER_PAGE;

        for (int i = 0; i < ASPECTS_PER_PAGE && start + i < all.size(); i++) {
            Aspect aspect = all.get(start + i);
            int ry = oy + i * 40;

            boolean hover = mouseX >= ox && mouseY >= ry && mouseX < ox + 40 && mouseY < ry + 40;
            if (hover) {
                // 1.12: _back.png 16×16 @2×, α0.5, at (x-2, y-2)
                graphics.blit(RenderPipelines.GUI_TEXTURED, ASPECT_BACK,
                        ox - 2, ry - 2, 0, 0, 32, 32, 16, 16, ARGB.colorFromFloat(0.5f, 1f, 1f, 1f));
            }

            // 1.12: aspect tag 16×16 @1.5× at (x+2, y+2)
            graphics.pose().pushMatrix();
            graphics.pose().translate(ox + 2, ry + 2);
            graphics.pose().scale(1.5f);
            AspectRenderer.drawAspect(graphics, 0, 0, aspect);
            graphics.pose().popMatrix();

            // 1.12: name 0.5× centered at (x+16, y+29)
            String name = aspect.getName();
            int nw = font.width(name);
            graphics.text(font, name, ox + 16 - nw / 2, ry + 29, TEXT_DARK, false);

            Aspect[] comps = aspect.getComponents();
            if (comps != null && comps.length == 2) {
                // 1.12: components 16×16 @1.25× at (x+60, y+4) and (x+102, y+4)
                graphics.pose().pushMatrix();
                graphics.pose().translate(ox + 60, ry + 4);
                graphics.pose().scale(1.25f);
                AspectRenderer.drawAspect(graphics, 0, 0, comps[0]);
                graphics.pose().popMatrix();
                graphics.pose().pushMatrix();
                graphics.pose().translate(ox + 102, ry + 4);
                graphics.pose().scale(1.25f);
                AspectRenderer.drawAspect(graphics, 0, 0, comps[1]);
                graphics.pose().popMatrix();

                // 1.12: "=" at (x+41, y+12), "+" at (x+89, y+12)
                graphics.text(font, "=", ox + 41, ry + 12, TEXT_SYM, false);
                graphics.text(font, "+", ox + 89, ry + 12, TEXT_SYM, false);

                // 1.12: component names 0.5× centered at (x+72, y+29) / (x+114, y+29)
                String c0 = comps[0].getName();
                graphics.text(font, c0, ox + 72 - font.width(c0) / 2, ry + 29, TEXT_DARK, false);
                String c1 = comps[1].getName();
                graphics.text(font, c1, ox + 114 - font.width(c1) / 2, ry + 29, TEXT_DARK, false);
            } else {
                // 1.12: "tc.aspect.primal" at (x+54, y+12)
                graphics.text(font, "Primal Aspect", ox + 54, ry + 12, TEXT_MID, false);
            }

            if (hover && (tooltip == null || tooltip.isEmpty())) {
                tooltip.addAll(AspectRenderer.getAspectTooltip(aspect, 0));
            }
        }

        boolean showPrev = page > 0;
        boolean showNext = start + ASPECTS_PER_PAGE < all.size();
        drawBookArrows(graphics, px, py, showPrev, showNext);
        return new Bounds(px, py, PANEL + 1, PANEL + 1);
    }

    // ==================== Recipe popups ====================

    /**
     * Draw the recipe popup for the given recipe id, centered at (cx, cy).
     * Returns the panel bounds for hit-testing.
     */
    public static Bounds drawRecipePopup(
            GuiGraphicsExtractor graphics, int cx, int cy, Identifier recipeId,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        Bounds panel = new Bounds(cx - PANEL / 2, cy - PANEL / 2, PANEL + 1, PANEL + 1);
        Object recipe = RecipeRenderer.findRecipe(recipeId);
        if (recipe == null) {
            drawPaper(graphics, panel.x, panel.y);
            graphics.centeredText(font, "Recipe not found", cx, cy - 20, 0xFF800000);
            if (recipeId != null) {
                String id = recipeId.toString();
                if (id.length() > 30) {
                    id = "..." + id.substring(id.length() - 27);
                }
                graphics.centeredText(font, id, cx, cy - 8, 0xFF606060);
            }
            return panel;
        }

        if (recipe instanceof IArcaneRecipe arcane) {
            drawArcane(graphics, cx, cy, arcane, mouseX, mouseY, font, tooltip);
        } else if (recipe instanceof CrucibleRecipeType t) {
            drawCrucible(graphics, cx, cy, t.getResultItem(), t.getCatalyst(), t.getAspects(),
                    mouseX, mouseY, font, tooltip);
        } else if (recipe instanceof CrucibleRecipe c) {
            drawCrucible(graphics, cx, cy, c.getRecipeOutput(), c.getCatalyst(), c.getAspects(),
                    mouseX, mouseY, font, tooltip);
        } else if (recipe instanceof InfusionRecipeType i) {
            drawInfusion(graphics, cx, cy, i.getResultItem(), i.getCentralItem(),
                    i.getComponents(), i.getAspects(), i.getInstability(), mouseX, mouseY, font, tooltip);
        } else if (recipe instanceof InfusionRecipe i) {
            Object out = i.getRecipeOutput();
            drawInfusion(graphics, cx, cy,
                    out instanceof ItemStack s ? s : ItemStack.EMPTY,
                    i.getRecipeInput(), i.getComponents(), i.getAspects(),
                    i.instability, mouseX, mouseY, font, tooltip);
        } else if (recipe instanceof CraftingRecipe cr) {
            drawCrafting(graphics, cx, cy, cr, mouseX, mouseY, font, tooltip);
        } else if (recipe instanceof SingleItemRecipe sr) {
            drawSmelting(graphics, cx, cy, sr, mouseX, mouseY, font, tooltip);
        } else if (recipe instanceof ThaumcraftApi.BluePrint bp) {
            drawBlueprint(graphics, cx, cy, bp, mouseX, mouseY, font, tooltip);
        } else if (recipe instanceof FakeRecipe fake) {
            drawFake(graphics, cx, cy, fake, mouseX, mouseY, font, tooltip);
        } else {
            drawPaper(graphics, panel.x, panel.y);
            graphics.centeredText(font, "Unknown recipe type", cx, cy, 0xFF804040);
        }
        return panel;
    }

    // ==================== 1.12 station popups ====================

    /** 1.12 drawArcaneCraftingPage. */
    private static void drawArcane(GuiGraphicsExtractor g, int cx, int cy, IArcaneRecipe a,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        drawPaper(g, cx - PANEL / 2, cy - PANEL / 2);

        // Station art: grid + output arrow (1.12 uv 112,15 / 20,3 @2×)
        blit2x(g, cx, cy, -26, -26, 112, 15, 52, 52);
        blit2x(g, cx, cy, -8, -46, 20, 3, 16, 16);
        // Vis (wand) glyph, faded (1.12 uv 68,76 @2×, α0.4)
        blit2x(g, cx, cy, -6, 40, 68, 76, 12, 12, ARGB.colorFromFloat(0.4f, 1f, 1f, 1f));

        // Title (1.12: recipe.type.arcane/.shapeless centered at y-104)
        String title = (a instanceof ShapelessArcaneRecipe)
                ? "Arcane Workbench (Shapeless)" : "Arcane Workbench";
        g.centeredText(font, title, cx, cy - 104, TEXT_DARK);

        // Output above the arrow
        ItemStack output = a.getResultItem();
        drawItem(g, output, cx - 8, cy - 84);
        addHoverTooltip(tooltip, output, cx - 8, cy - 84, mouseX, mouseY);

        // 3×3 grid items (1.12: (x-40+i*32, y-40+j*32), count overlay)
        List<Optional<Ingredient>> ingredients = new ArrayList<>();
        if (a instanceof ShapedArcaneRecipe sar) {
            ingredients.addAll(sar.getIngredients());
        } else if (a instanceof ShapelessArcaneRecipe slar) {
            slar.getIngredients().forEach(i -> ingredients.add(Optional.of(i)));
        }
        for (int i = 0; i < ingredients.size() && i < 9; i++) {
            Optional<Ingredient> oing = ingredients.get(i);
            if (oing.isEmpty()) continue;
            int vx = cx - 40 + (i % 3) * 32;
            int vy = cy - 40 + (i / 3) * 32;
            ItemStack is = RecipeRenderer.cycleIngredient(oing.get(), i);
            drawItem(g, is, vx, vy);
            addHoverTooltip(tooltip, is, vx, vy, mouseX, mouseY);
        }

        // Required crystals as crystal items (1.12: (x+4-sz*10+a*20, y+59))
        AspectList crystals = a.getCrystals();
        if (crystals != null && crystals.size() > 0) {
            int sz = crystals.size();
            int idx = 0;
            for (Aspect aspect : crystals.getAspects()) {
                ItemStack crystal = ThaumcraftApiHelper.makeCrystal(aspect, crystals.getAmount(aspect));
                int vx = cx + 4 - sz * 10 + idx * 20;
                int vy = cy + 59;
                drawItem(g, crystal, vx, vy);
                addHoverTooltip(tooltip, crystal, vx, vy, mouseX, mouseY);
                idx++;
            }
        }

        // Vis number over the faded glyph (1.12: centered at y+90)
        g.centeredText(font, String.valueOf(a.getVis()), cx, cy + 90, TEXT_DARK);
    }

    /** 1.12 drawCraftingPage. */
    private static void drawCrafting(GuiGraphicsExtractor g, int cx, int cy, CraftingRecipe cr,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        drawPaper(g, cx - PANEL / 2, cy - PANEL / 2);

        // Station art: workbench grid + arrow (1.12 uv 60,15 / 20,3 @2×)
        blit2x(g, cx, cy, -26, -26, 60, 15, 51, 52);
        blit2x(g, cx, cy, -8, -46, 20, 3, 16, 16);

        boolean shaped = cr instanceof net.minecraft.world.item.crafting.ShapedRecipe;
        g.centeredText(font, shaped ? "Workbench" : "Workbench (Shapeless)", cx, cy - 104, TEXT_DARK);

        ItemStack output = RecipeRenderer.resolveOutput(cr);
        drawItem(g, output, cx - 8, cy - 84);
        addHoverTooltip(tooltip, output, cx - 8, cy - 84, mouseX, mouseY);

        List<Optional<Ingredient>> ingredients = new ArrayList<>();
        if (shaped) {
            ingredients.addAll(((net.minecraft.world.item.crafting.ShapedRecipe) cr).getIngredients());
        } else {
            cr.placementInfo().ingredients().forEach(i -> ingredients.add(Optional.of(i)));
        }
        for (int i = 0; i < ingredients.size() && i < 9; i++) {
            Optional<Ingredient> oing = ingredients.get(i);
            if (oing.isEmpty()) continue;
            int vx = cx - 40 + (i % 3) * 32;
            int vy = cy - 40 + (i / 3) * 32;
            ItemStack is = RecipeRenderer.cycleIngredient(oing.get(), i);
            drawItem(g, is, vx, vy);
            addHoverTooltip(tooltip, is, vx, vy, mouseX, mouseY);
        }
    }

    /** 1.12 drawCruciblePage. */
    private static void drawCrucible(GuiGraphicsExtractor g, int cx, int cy,
            ItemStack output, Ingredient catalyst, AspectList aspects,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        drawPaper(g, cx - PANEL / 2, cy - PANEL / 2);
        g.centeredText(font, "Crucible", cx, cy - 104, TEXT_DARK);

        // Station art: flame, pot, redstone/curved arrow (1.12 uv 0,3 / 0,20 / 100,84 @2×)
        blit2x(g, cx, cy, -28, -29, 0, 3, 56, 17);
        blit2x(g, cx, cy, -28, -12, 0, 20, 56, 48);
        blit2x(g, cx, cy, -25, -26, 100, 84, 11, 13);

        // Output above the pot
        drawItem(g, output, cx - 8, cy - 50);
        addHoverTooltip(tooltip, output, cx - 8, cy - 50, mouseX, mouseY);

        // Catalyst (e.g. redstone) on the left
        if (catalyst != null && !catalyst.isEmpty()) {
            ItemStack cat = RecipeRenderer.cycleIngredient(catalyst, 0);
            drawItem(g, cat, cx - 64, cy - 56);
            addHoverTooltip(tooltip, cat, cx - 64, cy - 56, mouseX, mouseY);
        }

        // Essentia costs: 16×16 tags with counts, 3 per row (1.12 drawTag layout)
        drawAspectGrid(g, cx, cy, 3, -28, cy + 8, aspects, mouseX, mouseY, tooltip);
    }

    /** 1.12 drawInfusionPage. */
    private static void drawInfusion(GuiGraphicsExtractor g, int cx, int cy,
            ItemStack output, Ingredient central, List<Ingredient> components,
            AspectList aspects, int instability,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {

        drawPaper(g, cx - PANEL / 2, cy - PANEL / 2);
        g.centeredText(font, "Arcane Infusion", cx, cy - 104, TEXT_DARK);

        // Station art: band + altar (1.12 uv 0,3 / 200,77 @2×, base offset y+20 baked in)
        blit2x(g, cx, cy + 20, -28, -56, 0, 3, 56, 17);
        blit2x(g, cx, cy + 20, -28, -36, 200, 77, 60, 44);

        // Output above the altar
        drawItem(g, output, cx - 8, cy - 85);
        addHoverTooltip(tooltip, output, cx - 8, cy - 85, mouseX, mouseY);

        // Central item on the altar
        if (central != null && !central.isEmpty()) {
            ItemStack c = RecipeRenderer.cycleIngredient(central, 0);
            drawItem(g, c, cx - 8, cy - 16);
            addHoverTooltip(tooltip, c, cx - 8, cy - 16, mouseX, mouseY);
        }

        // Components around the center (1.12: radius 40, start -90°, 16×16 with count)
        if (components != null && !components.isEmpty()) {
            int le = components.size();
            float slice = 360f / le;
            float rot = -90f;
            for (int a = 0; a < le; a++) {
                float rad = (float) Math.toRadians(rot);
                int vx = cx + (int) (Math.cos(rad) * 40) - 8;
                int vy = cy - 8 + (int) (Math.sin(rad) * 40) - 8;
                if (a < components.size() && components.get(a) != null && !components.get(a).isEmpty()) {
                    ItemStack is = RecipeRenderer.cycleIngredient(components.get(a), a);
                    drawItem(g, is, vx, vy);
                    addHoverTooltip(tooltip, is, vx, vy, mouseX, mouseY);
                }
                rot += slice;
            }
        }

        // Essentia costs: 16×16 tags with counts, 5 per row
        drawAspectGrid(g, cx, cy, 5, -48, cy + 50, aspects, mouseX, mouseY, tooltip);

        // Instability (1.12: "tc.inst tc.inst.<min(5, inst/2)>" centered at y+94)
        int level = Math.min(5, instability / 2);
        String[] instNames = {"Negligible", "Minor", "Moderate", "High", "Very High", "Dangerous"};
        g.centeredText(font, "Instability: " + instNames[level], cx, cy + 94, TEXT_DARK);
    }

    // ==================== Fallback popups ====================

    private static void drawSmelting(GuiGraphicsExtractor g, int cx, int cy, SingleItemRecipe sr,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {
        drawPaper(g, cx - PANEL / 2, cy - PANEL / 2);
        g.centeredText(font, "Furnace", cx, cy - 104, TEXT_DARK);

        ItemStack input = RecipeRenderer.cycleIngredient(sr.input(), 0);
        drawItem(g, input, cx - 48, cy - 8);
        addHoverTooltip(tooltip, input, cx - 48, cy - 8, mouseX, mouseY);

        ItemStack output = RecipeRenderer.resolveOutput(sr);
        drawItem(g, output, cx + 24, cy - 8);
        addHoverTooltip(tooltip, output, cx + 24, cy - 8, mouseX, mouseY);
    }

    private static void drawBlueprint(GuiGraphicsExtractor g, int cx, int cy,
            ThaumcraftApi.BluePrint bp, int mouseX, int mouseY, Font font, List<Component> tooltip) {
        drawPaper(g, cx - PANEL / 2, cy - PANEL / 2);
        g.centeredText(font, "Multiblock", cx, cy - 104, TEXT_DARK);

        ItemStack display = bp.getDisplayStack();
        if (display != null) {
            drawItem(g, display, cx - 8, cy - 40);
            addHoverTooltip(tooltip, display, cx - 8, cy - 40, mouseX, mouseY);
        }

        ItemStack[] parts = bp.getIngredientList();
        int shown = Math.min(6, parts.length);
        int startX = cx - (shown * 20 - 4) / 2;
        for (int i = 0; i < shown; i++) {
            int ix = startX + i * 20;
            drawItem(g, parts[i], ix, cy + 20);
            addHoverTooltip(tooltip, parts[i], ix, cy + 20, mouseX, mouseY);
        }
        if (parts.length > 6) {
            g.text(font, "...", startX + shown * 20 + 2, cy + 24, TEXT_MID, false);
        }
    }

    private static void drawFake(GuiGraphicsExtractor g, int cx, int cy, FakeRecipe fake,
            int mouseX, int mouseY, Font font, List<Component> tooltip) {
        drawPaper(g, cx - PANEL / 2, cy - PANEL / 2);
        g.centeredText(font, fake.getTitle(), cx, cy - 104, TEXT_DARK);

        ItemStack[] items = fake.getItems();
        if (items.length > 0) {
            int shown = Math.min(8, items.length);
            int startX = cx - (shown * 20 - 4) / 2;
            for (int i = 0; i < shown; i++) {
                int ix = startX + i * 20;
                drawItem(g, items[i], ix, cy);
                addHoverTooltip(tooltip, items[i], ix, cy, mouseX, mouseY);
            }
        }
    }

    // ==================== 1.12-faithful drawing helpers ====================

    /**
     * Paper panel (1.12 drawRecipe/drawAspectsInsert: 255×255 paper.png).
     */
    private static void drawPaper(GuiGraphicsExtractor g, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, PAPER, x, y, 0, 0, PANEL, PANEL, 256, 256);
    }

    /**
     * Blit overlay line-art the way 1.12 did: {@code drawTexturedModalRect(lx, ly, u, v, w, h)}
     * under {@code glTranslate(x, y) + glScale(2, 2)}. Since the overlay is 512×512 and 1.12
     * used 256-space UVs, the 26.2 blit doubles both UV and size and lands at
     * (cx+2lx, cy+2ly).
     */
    private static void blit2x(GuiGraphicsExtractor g, int cx, int cy,
            int lx, int ly, int u, int v, int w, int h) {
        blit2x(g, cx, cy, lx, ly, u, v, w, h, -1);
    }

    private static void blit2x(GuiGraphicsExtractor g, int cx, int cy,
            int lx, int ly, int u, int v, int w, int h, int color) {
        if (color == -1) {
            g.blit(RenderPipelines.GUI_TEXTURED, OVERLAY,
                    cx + 2 * lx, cy + 2 * ly, 2 * u, 2 * v, 2 * w, 2 * h, 512, 512);
        } else {
            g.blit(RenderPipelines.GUI_TEXTURED, OVERLAY,
                    cx + 2 * lx, cy + 2 * ly, 2 * u, 2 * v, 2 * w, 2 * h, 512, 512, color);
        }
    }

    /**
     * Draw a 16×16 item with its count decoration and no slot background
     * (1.12's drawStackAt draws items straight onto the paper).
     */
    private static void drawItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        g.item(stack, x, y);
        g.itemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    /**
     * Hover-tooltip helper. {@code RecipeRenderer.checkItemTooltip} returns its
     * {@code existingTooltip} argument unchanged whenever it is non-null, so the
     * caller's (empty, non-null) list must be passed as {@code null} here and the
     * result appended back — first hovered item wins.
     */
    private static void addHoverTooltip(List<Component> out, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (out == null || !out.isEmpty() || stack == null || stack.isEmpty()) return;
        List<Component> found = RecipeRenderer.checkItemTooltip(stack, x, y, mouseX, mouseY, null);
        if (found != null && !found.isEmpty()) out.addAll(found);
    }

    /**
     * 1.12 aspect-cost grid: {@code rows=(n-1)/perRow; shift=(perRow-n%perRow)*10;
     * sx=cx+offset; sy=baseY-10*rows;} each tag at
     * {@code (sx + i%perRow*20 + shift*m, sy + i/perRow*20)} — 16×16 with count.
     */
    private static List<Component> drawAspectGrid(GuiGraphicsExtractor g, int cx, int cy,
            int perRow, int xoffset, int baseY, AspectList aspects,
            int mouseX, int mouseY, List<Component> tooltip) {
        if (aspects == null || aspects.size() == 0) return tooltip;

        int size = aspects.size();
        int rows = (size - 1) / perRow;
        int shift = (perRow - size % perRow) * 10;
        int sx = cx + xoffset;
        int sy = baseY - 10 * rows;
        int total = 0;
        for (Aspect tag : aspects.getAspects()) {
            int m = (total / perRow >= rows && (rows > 1 || size < perRow)) ? 1 : 0;
            int vx = sx + (total % perRow) * 20 + shift * m;
            int vy = sy + (total / perRow) * 20;
            AspectRenderer.drawAspect(g, vx, vy, tag, aspects.getAmount(tag));
            if (AspectRenderer.isMouseOverAspect(vx, vy, mouseX, mouseY)
                    && (tooltip == null || tooltip.isEmpty())) {
                tooltip.addAll(AspectRenderer.getAspectTooltip(tag, aspects.getAmount(tag)));
            }
            total++;
        }
        return tooltip;
    }

    /** Red book arrows from gui_researchbook.png (1.12 uv 0,184 / 12,184, 12×8). */
    private static void drawBookArrows(GuiGraphicsExtractor g, int px, int py,
            boolean showPrev, boolean showNext) {
        if (showPrev) {
            g.blit(RenderPipelines.GUI_TEXTURED, BOOK, px + 40, py + 232, 0, 184, 12, 8, 256, 256);
        }
        if (showNext) {
            g.blit(RenderPipelines.GUI_TEXTURED, BOOK, px + 204, py + 232, 12, 184, 12, 8, 256, 256);
        }
    }
}
