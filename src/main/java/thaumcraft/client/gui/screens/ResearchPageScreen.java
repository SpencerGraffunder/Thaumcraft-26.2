package thaumcraft.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.*;
import thaumcraft.common.lib.capabilities.ThaumcraftCapabilities;
import thaumcraft.client.gui.BookPopupRenderer;
import thaumcraft.client.gui.RecipeRenderer;
import thaumcraft.client.lib.AspectRenderer;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.research.ResearchManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ResearchPageScreen - Displays detailed information about a research entry.
 * 
 * Shows research stages, recipes, text descriptions, requirements, and progress.
 * This is a simplified but functional implementation covering the core features.
 * 
 * Ported from 1.12.2 GuiResearchPage to 1.20.1
 */
@OnlyIn(Dist.CLIENT)
public class ResearchPageScreen extends Screen {
    
    // Textures
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook.png");
    private static final Identifier OVERLAY = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook_overlay.png");
    
    // Pane dimensions (the book is blitted at 1.3x around screen center sw/sh)
    private static final int PANE_WIDTH = 256;
    private static final int PANE_HEIGHT = 181;
    // Paper area in world coordinates, relative to sw/sh. Measured from the book
    // texture (512px, paper occupies texture x 26..486 / y 8..346; the visible book
    // is texture 512x362 drawn as 256x181 at 1.3x around the screen center).
    private static final int PAPER_TOP = -22;
    private static final int PAPER_BOTTOM = 197;
    // 1.12-style text: 1.25x font scale, wrapped to 104 font-pixels, 13 lines on
    // the first page (leaving room for the title cluster), 16 on later pages.
    private static final float TEXT_SCALE = 1.25f;
    private static final int TEXT_WIDTH = 104;
    private static final int MAX_TEXT_LINES = 16;
    private static final int MAX_TEXT_LINES_FIRST = 13; // first page leaves room for the title cluster
    
    // Research data
    private final ResearchEntry research;
    private final Identifier highlightRecipe;
    private final double returnX;
    private final double returnY;
    
    // Player data
    private Player player;
    private IPlayerKnowledge playerKnowledge;
    
    // Page tracking
    private int currentStage = 0;
    private int page = 0;
    private int maxPages = 1;
    private boolean isComplete = false;
    private boolean hasAllRequisites = false;
    
    // Parsed page content
    private ArrayList<Page> pages = new ArrayList<>();

    // Recipe bookmarks per stage (drawn on the right edge of the book; clicking
    // one opens a 1.12-style recipe popup)
    private final Map<Integer, List<Identifier>> stageBookmarks = new HashMap<>();

    // Popup state
    private boolean aspectPopup = false;
    private int aspectPage = 0;
    private Identifier activeRecipe = null;
    private BookPopupRenderer.Bounds popupBounds = null;
    
    // Requirement tracking
    private boolean[] hasItem;
    private boolean[] hasCraft;
    private boolean[] hasResearch;
    private boolean[] hasKnow;
    
    public ResearchPageScreen(ResearchEntry research, Identifier highlightRecipe, double returnX, double returnY) {
        super(Component.translatable(research.getName()));
        this.research = research;
        this.highlightRecipe = highlightRecipe;
        this.returnX = returnX;
        this.returnY = returnY;
    }
    
    @Override
    protected void init() {
        super.init();
        
        player = minecraft.player;
        ThaumcraftCapabilities.getKnowledge(player).ifPresent(k -> playerKnowledge = k);
        
        // Exit/close button (top-right of the 1.3x book)
        int sw = (width - PANE_WIDTH) / 2;
        int sh = (height - PANE_HEIGHT) / 2;
        int bookRight = (width + (int)(PANE_WIDTH * 1.3f)) / 2;
        int bookTop = (height - (int)(PANE_HEIGHT * 1.3f)) / 2;
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> this.onClose())
                .bounds(bookRight - 18, bookTop + 4, 16, 16)
                .build());
        parsePages();
    }
    
    /**
     * Strips custom formatting tags (BR, IMG, PAGE, DIV, LINE) from research text.
     */
    private String stripFormattingTags(String text) {
        text = text.replaceAll("<IMG>[^<]*</IMG>", "");
        text = text.replaceAll("<BR>", " ");
        text = text.replaceAll("<PAGE>", " ");
        text = text.replaceAll("<DIV>", " ");
        text = text.replaceAll("<LINE>", " ");
        return text;
    }

    /**
     * Parse research content into displayable pages.
     */
    private void parsePages() {
        pages.clear();
        
        if (playerKnowledge == null) {
            return;
        }
        
        // Determine current stage
        currentStage = playerKnowledge.getResearchStage(research.getKey());
        if (currentStage < 1) currentStage = 1;
        
        isComplete = playerKnowledge.isResearchComplete(research.getKey());
        hasAllRequisites = ResearchManager.doesPlayerHaveRequisites(player, research.getKey());
        
        // Get stages up to current (or all if complete)
        ResearchStage[] stages = research.getStages();
        if (stages == null || stages.length == 0) {
            // No stages - just show title page
            Page titlePage = new Page();
            titlePage.contents.add(Component.translatable("tc.research.nostages").getString());
            pages.add(titlePage);
            maxPages = 1;
            return;
        }
        
        int maxStage = isComplete ? stages.length : Math.min(currentStage, stages.length);
        
        // Build pages for each visible stage
        for (int s = 0; s < maxStage; s++) {
            ResearchStage stage = stages[s];
            int startIdx = pages.size();

            // Add stage text
            if (stage.getText() != null) {
                String text = stripFormattingTags(Component.translatable(stage.getText()).getString());
                // Split text into lines that fit the page width
                List<String> lines = wrapText(text, TEXT_WIDTH);
                addTextPages(pages, lines, pages.isEmpty() ? MAX_TEXT_LINES_FIRST : MAX_TEXT_LINES);
            } else {
                pages.add(new Page());
            }

            for (int i = startIdx; i < pages.size(); i++) {
                pages.get(i).stageId = s;
            }

            // Recipe bookmarks (1.12-style: icons on the book's right edge)
            if (stage.getRecipes() != null && stage.getRecipes().length > 0) {
                stageBookmarks.put(s, java.util.Arrays.asList(stage.getRecipes()));
            }
        }
        
        // Add addenda if research is complete
        if (isComplete && research.getAddenda() != null) {
            int a = 0;
            for (ResearchAddendum addendum : research.getAddenda()) {
                // Check if addendum requirements are met
                boolean canShow = true;
                if (addendum.getResearch() != null) {
                    for (String req : addendum.getResearch()) {
                        if (!ThaumcraftCapabilities.isResearchComplete(player, req)) {
                            canShow = false;
                            break;
                        }
                    }
                }

                if (canShow) {
                    int startIdx = pages.size();

                    if (addendum.getText() != null) {
                        String text = stripFormattingTags(Component.translatable(addendum.getText()).getString());
                        List<String> lines = wrapText(text, TEXT_WIDTH);
                        addTextPages(pages, lines, MAX_TEXT_LINES);
                    } else {
                        pages.add(new Page());
                    }

                    for (int i = startIdx; i < pages.size(); i++) {
                        pages.get(i).stageId = maxStage + a;
                        pages.get(i).isAddendum = true;
                    }

                    // Addendum recipe bookmarks
                    if (addendum.getRecipes() != null && addendum.getRecipes().length > 0) {
                        stageBookmarks.put(maxStage + a, java.util.Arrays.asList(addendum.getRecipes()));
                    }
                }
                a++;
            }
        }
        
        // Ensure we have at least one page
        if (pages.isEmpty()) {
            Page emptyPage = new Page();
            emptyPage.contents.add(Component.translatable("tc.research.empty").getString());
            pages.add(emptyPage);
        }
        
        maxPages = pages.size();
        
        // Update requirement tracking
        if (!isComplete && currentStage > 0 && currentStage <= stages.length) {
            ResearchStage currentStageData = stages[currentStage - 1];
            updateRequirementTracking(currentStageData);
        }
    }
    
    /**
     * Update tracking arrays for current stage requirements.
     */
    private void updateRequirementTracking(ResearchStage stage) {
        // Track item requirements
        if (stage.getObtain() != null) {
            hasItem = new boolean[stage.getObtain().length];
            for (int i = 0; i < stage.getObtain().length; i++) {
                Object o = stage.getObtain()[i];
                if (o instanceof ItemStack stack) {
                    hasItem[i] = playerHasItem(stack);
                }
            }
        }
        
        // Track craft requirements
        if (stage.getCraft() != null) {
            hasCraft = new boolean[stage.getCraft().length];
            int[] refs = stage.getCraftReference();
            for (int i = 0; i < stage.getCraft().length; i++) {
                String refKey = "[#]" + refs[i];
                hasCraft[i] = playerKnowledge.isResearchKnown(refKey);
            }
        }
        
        // Track research requirements
        if (stage.getResearch() != null) {
            hasResearch = new boolean[stage.getResearch().length];
            for (int i = 0; i < stage.getResearch().length; i++) {
                hasResearch[i] = ThaumcraftCapabilities.isResearchComplete(player, stage.getResearch()[i]);
            }
        }
        
        // Track knowledge requirements
        if (stage.getKnow() != null) {
            hasKnow = new boolean[stage.getKnow().length];
            for (int i = 0; i < stage.getKnow().length; i++) {
                ResearchStage.Knowledge k = stage.getKnow()[i];
                String catKey = k.category != null ? k.category.key : null;
                int playerKnow = playerKnowledge.getKnowledge(k.type, catKey);
                hasKnow[i] = playerKnow >= k.amount;
            }
        }
    }
    
    /**
     * Check if player has an item in their inventory.
     */
    private boolean playerHasItem(ItemStack required) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (ItemStack.isSameItemSameComponents(stack, required)) {
                count += stack.getCount();
                if (count >= required.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Split wrapped text lines into as many pages as needed so content
     * fits the book page height instead of overflowing the bottom edge.
     */
    private void addTextPages(List<Page> pages, List<String> lines, int maxLines) {
        for (int i = 0; i < lines.size(); i += maxLines) {
            Page p = new Page();
            p.contents.addAll(lines.subList(i, Math.min(i + maxLines, lines.size())));
            pages.add(p);
        }
        if (lines.isEmpty()) {
            pages.add(new Page());
        }
    }
    
    /**
     * Wrap text to fit within a given width.
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        
        // Split by explicit line breaks
        String[] paragraphs = text.split("<LINE>|\\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            
            // Word wrap each paragraph
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();
            
            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                int testWidth = font.width(testLine);
                
                if (testWidth > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    currentLine.append(word);
                }
            }
            
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        return lines;
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int sw = (width - PANE_WIDTH) / 2;
        int sh = (height - PANE_HEIGHT) / 2;
        
        // Draw book background at 1.3x scale (matching 1.12.2 GuiResearchPage)
        graphics.pose().pushMatrix();
        graphics.pose().translate((width - PANE_WIDTH * 1.3f) / 2.0f, (height - PANE_HEIGHT * 1.3f) / 2.0f);
        graphics.pose().scale(1.3f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, PANE_WIDTH, PANE_HEIGHT, 256, 256);
        graphics.pose().popMatrix();
        
        // Draw title and separators on first page (inside the paper area)
        if (page == 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, sw + 4, sh + 2, 24.0F, 184.0F, 96, 4, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, sw + 4, sh + 19, 24.0F, 184.0F, 96, 4, 256, 256);
            
            String title = research.getLocalizedName().getString();
            int titleWidth = font.width(title);
            graphics.text(font, title, sw + 55 - titleWidth / 2, sh + 8, 0xFF202020, false);
        }
        
        // Page content: text blocks are centered vertically inside the paper area.
        int paperTop = sh + PAPER_TOP;
        int textTop = (page == 0) ? sh + 27 : paperTop + 14;
        int textBottom = sh + PAPER_BOTTOM - 14;
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw left page
        if (page < pages.size()) {
            tooltip = drawPageContent(graphics, pages.get(page), sw - 15, textTop, textBottom, mouseX, mouseY, false);
        }
        
        // Draw right page
        if (page + 1 < pages.size()) {
            List<net.minecraft.network.chat.Component> rightTooltip = drawPageContent(graphics, pages.get(page + 1), sw + 137, textTop, textBottom, mouseX, mouseY, true);
            if (tooltip == null) tooltip = rightTooltip;
        }
        
        // Draw navigation arrows (1.12.2 positions)
        float bob = (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.2f + 0.1f;
        
        if (page > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, sw - 16, sh + 190, 0.0F, 184.0F, 12, 8, 256, 256, ARGB.white(0.8f + bob));
        }
        if (page < maxPages - 2) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, sw + 262, sh + 190, 12.0F, 184.0F, 12, 8, 256, 256, ARGB.white(0.8f + bob));
        }
        
        // Draw requirements if not complete (on first page)
        if (!isComplete && page == 0 && research.getStages() != null && currentStage > 0 && currentStage <= research.getStages().length) {
            drawRequirements(graphics, sw, sh, mouseX, mouseY, research.getStages()[currentStage - 1]);
        }
        
        // Draw complete button if all requirements are met
        if (!isComplete && hasAllRequisites && allCurrentRequirementsMet()) {
            drawCompleteButton(graphics, sw, sh, mouseX, mouseY);
        }

        // Recipe bookmarks (right edge) + aspect bookmark (left edge)
        drawBookmarks(graphics, mouseX, mouseY);

        // Recipe / aspect popup (1.12-style overlay)
        if (activeRecipe != null || aspectPopup) {
            int cx = sw + PANE_WIDTH / 2;
            int cy = sh + PANE_HEIGHT / 2;
            List<net.minecraft.network.chat.Component> popupTip = new ArrayList<>();
            if (aspectPopup) {
                popupBounds = BookPopupRenderer.drawAspectPopup(graphics, cx, cy, aspectPage, mouseX, mouseY, font, popupTip);
            } else {
                popupBounds = BookPopupRenderer.drawRecipePopup(graphics, cx, cy, activeRecipe, mouseX, mouseY, font, popupTip);
            }
            if (!popupTip.isEmpty()) tooltip = popupTip;
        }
        
        // Draw page numbers below the 1.3x book
        String pageNum = (page / 2 + 1) + " / " + ((maxPages + 1) / 2);
        int bookBottom = (height + (int)(PANE_HEIGHT * 1.3f)) / 2;
        graphics.centeredText(font, pageNum, sw + PANE_WIDTH / 2, bookBottom + 5, 0xFF808080);
        
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        
        // Draw tooltip last (on top of everything)
        if (tooltip != null && !tooltip.isEmpty()) {
            graphics.setTooltipForNextFrame(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }
    
    /**
     * Draw the content of a single page.
     * Returns tooltip to display if hovering over an item.
     */
    private List<net.minecraft.network.chat.Component> drawPageContent(GuiGraphicsExtractor graphics, Page page, int x, int y, int bottomY, int mouseX, int mouseY, boolean rightSide) {
        // Count text lines and center the block vertically inside the paper area
        int lines = 0;
        for (Object content : page.contents) {
            if (content instanceof String) lines++;
        }
        float lineHeight = font.lineHeight * TEXT_SCALE;
        int blockH = (int) Math.round(lines * lineHeight);
        int startY = y + Math.max(0, (bottomY - y - blockH) / 2);

        // Mark addendum pages (clamped inside the paper area)
        if (page.isAddendum) {
            int sh = (height - PANE_HEIGHT) / 2;
            graphics.text(font, "§o[Addendum]", x, Math.max(startY - 12, sh + PAPER_TOP + 2), 0xFF606060, false);
        }

        // 1.12-style text: 1.25x font scale
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, startY);
        graphics.pose().scale(TEXT_SCALE);
        int lineY = 0;
        for (Object content : page.contents) {
            if (content instanceof String text) {
                graphics.text(font, text, 0, lineY, 0xFF202020, false);
                lineY += font.lineHeight;
            }
        }
        graphics.pose().popMatrix();

        return null;
    }

    /**
     * Hit rect (x, y, w, h) for the i-th recipe bookmark on the book's right edge.
     */
    private int[] bookmarkRect(int i) {
        int sw = (width - PANE_WIDTH) / 2;
        int sh = (height - PANE_HEIGHT) / 2;
        return new int[] {sw + PANE_WIDTH + 22, sh + 56 + i * 26, 24, 30};
    }

    /**
     * Bookmarks for the current spread: the right page's stage, falling back to
     * the left page's stage.
     */
    private List<Identifier> currentBookmarks() {
        if (page >= pages.size()) return List.of();
        Page left = pages.get(page);
        Page right = pages.get(Math.min(page + 1, pages.size() - 1));
        List<Identifier> bm = stageBookmarks.get(right.stageId);
        if (bm == null || bm.isEmpty()) bm = stageBookmarks.get(left.stageId);
        return bm != null ? bm : List.of();
    }

    /**
     * Draw the recipe bookmarks on the book's right edge and the aspect compass
     * bookmark on the left edge.
     */
    private void drawBookmarks(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int sw = (width - PANE_WIDTH) / 2;
        int sh = (height - PANE_HEIGHT) / 2;

        // Right-edge recipe bookmarks (1.12 drawRecipeBookmarks): a ribbon texture
        // (tex1 uv 120,232 28×16) + left strip (uv 116,232 4×16) + item icon, starting near
        // the top of the book's right edge (bookY-8). Selected recipe tinted salmon (1,0.5,0.5).
        List<Identifier> bm = currentBookmarks();
        int count = Math.min(5, bm.size());
        int space = count > 0 ? Math.min(25, 200 / count) : 25;
        int by = sh - 8;
        for (int i = 0; i < count; i++) {
            int x = sw + 280, y = by + i * space;
            boolean hov = mouseX >= x && mouseX < x + 30 && mouseY >= y - 1 && mouseY < y + 15;
            int le = hov ? 0 : 3;
            int color = (bm.get(i).equals(activeRecipe)) ? 0xFFFF8080 : 0xFFFFFFFF;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y - 1, 120 + le, 232, 28, 16, 256, 256, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y - 1, 116, 232, 4, 16, 256, 256, color);
            ItemStack out = RecipeRenderer.resolveOutput(RecipeRenderer.findRecipe(bm.get(i)));
            if (!out.isEmpty()) {
                RecipeRenderer.renderItem(graphics, out, x + 7 - le, y - 1);
            }
        }

        // aspect bookmark (1.12): a texture on the book's left edge — tex1 uv 76,232 (24×16)
        // main icon + uv 100,232 (4×16) strip, near the top of the book (GuiResearchPage: bookY+8).
        int ax = sw - 52, ay = sh + 10;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, ax, ay, 76, 232, 24, 16, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, ax + 20, ay, 100, 232, 4, 16, 256, 256);
    }
    
    /**
     * Draw current stage requirements at the bottom of the page.
     */
    private void drawRequirements(GuiGraphicsExtractor graphics, int sw, int sh, int mx, int my, ResearchStage stage) {
        // 1.12 style (GuiResearchPage.drawRequirements): horizontal icon rows with a
        // translucent background bar + icons + checkmarks, stacked from the bottom of the
        // book (research lowest, then obtain, craft, know).
        int x = sw;
        int y = sh + PANE_HEIGHT - 22;
        
        if (stage.getResearch() != null && stage.getResearch().length > 0) {
            y -= 18;
            drawResearchRow(graphics, x, y, stage, mx, my);
        }
        if (stage.getObtain() != null && stage.getObtain().length > 0) {
            y -= 18;
            drawItemRow(graphics, x, y, stage.getObtain(), hasItem, 216, mx, my);
        }
        if (stage.getCraft() != null && stage.getCraft().length > 0) {
            y -= 18;
            drawItemRow(graphics, x, y, stage.getCraft(), hasCraft, 200, mx, my);
        }
        if (stage.getKnow() != null && stage.getKnow().length > 0) {
            y -= 18;
            drawKnowRow(graphics, x, y, stage, mx, my);
        }
    }
    
    /** Translucent 56×16 row background bar (1.12: tex1 uv 200,<barUvY>, alpha 0.25). */
    private void drawRowBar(GuiGraphicsExtractor g, int x, int y, int barUvY) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x - 12, y - 1, 200, barUvY, 56, 16, 256, 256, ARGB.white(0.25f));
    }
    
    /** 1.12 checkmark: tex1 uv 159,207, 10×10, drawn over a satisfied icon. */
    private void drawCheckmark(GuiGraphicsExtractor g, int iconX, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, iconX + 8, y, 159, 207, 10, 10, 256, 256);
    }
    
    private void drawResearchRow(GuiGraphicsExtractor g, int x, int y, ResearchStage stage, int mx, int my) {
        int count = stage.getResearch().length;
        drawRowBar(g, x, y, 232);
        int shift = 24, ss = 18;
        if (count > 6) ss = 110 / count;
        String[] icons = stage.getResearchIcon();
        for (int a = 0; a < count; a++) {
            int ix = x - 15 + shift;
            Object icon = resolveResearchIcon(stage, a, icons);
            drawReqIcon(g, icon, ix, y);
            if (hasResearch != null && hasResearch.length > a && hasResearch[a]) drawCheckmark(g, ix, y);
            shift += ss;
        }
    }
    
    private Object resolveResearchIcon(ResearchStage stage, int a, String[] icons) {
        String key = stage.getResearch()[a];
        if (key != null && key.startsWith("!")) {
            Aspect as = Aspect.aspects.get(key.substring(1));
            if (as != null) return as;
        }
        ResearchEntry re = ResearchCategories.getResearch(key);
        if (re != null && re.getIcons() != null && re.getIcons().length > 0) return re.getIcons()[0];
        if (icons != null && a < icons.length && icons[a] != null && !icons[a].isEmpty()) {
            try {
                String[] parts = icons[a].split(":", 2);
                return parts.length == 2
                        ? Identifier.fromNamespaceAndPath(parts[0], parts[1])
                        : Identifier.fromNamespaceAndPath(Thaumcraft.MODID, icons[a]);
            } catch (Exception ignored) {}
        }
        return null;
    }
    
    private void drawItemRow(GuiGraphicsExtractor g, int x, int y, Object[] items, boolean[] has, int barUvY, int mx, int my) {
        int count = items.length;
        drawRowBar(g, x, y, barUvY);
        int shift = 24, ss = 18;
        if (count > 6) ss = 110 / count;
        for (int i = 0; i < count; i++) {
            int ix = x - 15 + shift;
            if (items[i] instanceof ItemStack s && !s.isEmpty()) RecipeRenderer.renderItem(g, s, ix, y);
            if (has != null && has.length > i && has[i]) drawCheckmark(g, ix, y);
            shift += ss;
        }
    }
    
    private void drawKnowRow(GuiGraphicsExtractor g, int x, int y, ResearchStage stage, int mx, int my) {
        int count = stage.getKnow().length;
        drawRowBar(g, x, y, 184);
        int shift = 24, ss = 18;
        if (count > 6) ss = 110 / count;
        for (int i = 0; i < count; i++) {
            int ix = x - 15 + shift;
            // knowledge: a small book/scroll glyph (1.12 uses a knowledge icon here)
            g.fill(ix + 3, y + 2, ix + 13, y + 14, 0xFF9090A0);
            g.fill(ix + 4, y + 3, ix + 12, y + 4, 0xFFC0C0D0);
            if (hasKnow != null && hasKnow.length > i && hasKnow[i]) drawCheckmark(g, ix, y);
            shift += ss;
        }
    }
    
    /** Draw a requirement icon (Aspect / ItemStack / Identifier / placeholder). */
    private static void drawReqIcon(GuiGraphicsExtractor g, Object icon, int x, int y) {
        if (icon instanceof Aspect a) {
            AspectRenderer.drawAspect(g, x, y, a);
        } else if (icon instanceof ItemStack s) {
            if (!s.isEmpty()) RecipeRenderer.renderItem(g, s, x, y);
        } else if (icon instanceof Identifier id) {
            g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, 16, 16, 16, 16);
        } else {
            g.fill(x + 2, y + 2, x + 14, y + 14, 0xFF707070);
        }
    }
    
    /**
     * Check if all requirements for current stage are met.
     */
    private boolean allCurrentRequirementsMet() {
        if (hasItem != null) {
            for (boolean b : hasItem) if (!b) return false;
        }
        if (hasCraft != null) {
            for (boolean b : hasCraft) if (!b) return false;
        }
        if (hasResearch != null) {
            for (boolean b : hasResearch) if (!b) return false;
        }
        if (hasKnow != null) {
            for (boolean b : hasKnow) if (!b) return false;
        }
        return true;
    }
    
    /**
     * Draw the complete/progress button.
     */
    private void drawCompleteButton(GuiGraphicsExtractor graphics, int sw, int sh, int mx, int my) {
        int buttonX = sw + PANE_WIDTH / 2 - 40;
        int buttonY = sh + PANE_HEIGHT - 15;
        int buttonW = 80;
        int buttonH = 12;
        
        boolean hover = mx >= buttonX && mx < buttonX + buttonW && my >= buttonY && my < buttonY + buttonH;
        
        int color = hover ? 0x4060A060 : 0x40408040;
        graphics.fill(buttonX, buttonY, buttonX + buttonW, buttonY + buttonH, color);
        
        String text = "Complete Stage";
        graphics.centeredText(font, text, buttonX + buttonW / 2, buttonY + 2, hover ? 0xFFFFFFFF : 0xFFC0C0C0);
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int mx = (int) mouseX, my = (int) mouseY;
        int sw = (width - PANE_WIDTH) / 2;
        int sh = (height - PANE_HEIGHT) / 2;

        // A popup is open: arrows page (aspect popup), anything else closes it
        if (aspectPopup || activeRecipe != null) {
            if (aspectPopup && popupBounds != null) {
                BookPopupRenderer.Bounds prev = BookPopupRenderer.prevArrow(popupBounds);
                BookPopupRenderer.Bounds next = BookPopupRenderer.nextArrow(popupBounds);
                if (prev.contains(mx, my)) {
                    if (aspectPage > 0) aspectPage--;
                    return true;
                }
                if (next.contains(mx, my)) {
                    if (aspectPage < BookPopupRenderer.aspectPageCount() - 1) aspectPage++;
                    return true;
                }
            }
            if (popupBounds == null || !popupBounds.contains(mx, my)) {
                closePopup();
                return true;
            }
            return true; // clicks inside the panel are absorbed
        }

        // Right-edge recipe bookmarks
        List<Identifier> bm = currentBookmarks();
        for (int i = 0; i < Math.min(5, bm.size()); i++) {
            int[] r = bookmarkRect(i);
            if (mx >= r[0] - 4 && mx < r[0] + r[2] + 2 && my >= r[1] - 4 && my < r[1] + r[3] + 2) {
                activeRecipe = bm.get(i);
                return true;
            }
        }

        // Left-edge aspect bookmark
        int ax = sw - 52, ay = sh + 10;
        if (mx >= ax - 4 && mx < ax + 20 && my >= ay - 4 && my < ay + 20) {
            aspectPopup = true;
            aspectPage = 0;
            return true;
        }
        
        // Check navigation arrows
        if (page > 0 && mouseX >= sw - 16 && mouseX < sw - 4 && mouseY >= sh + 190 && mouseY < sh + 198) {
            page -= 2;
            if (page < 0) page = 0;
            return true;
        }
        
        if (page < maxPages - 2 && mouseX >= sw + 262 && mouseX < sw + 274 && mouseY >= sh + 190 && mouseY < sh + 198) {
            page += 2;
            return true;
        }
        
        // Check complete button
        if (!isComplete && hasAllRequisites && allCurrentRequirementsMet()) {
            int buttonX = sw + PANE_WIDTH / 2 - 40;
            int buttonY = sh + PANE_HEIGHT - 15;
            int buttonW = 80;
            int buttonH = 12;
            
            if (mouseX >= buttonX && mouseX < buttonX + buttonW && mouseY >= buttonY && mouseY < buttonY + buttonH) {
                // Send progress packet to server
                PacketHandler.sendToServer(new PacketSyncProgressToServer(research.getKey(), false, true, false));
                
                // Re-parse pages after progress
                parsePages();
                return true;
            }
        }
        
        return super.mouseClicked(event, doubleClick);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        // Aspect popup pages with the wheel; recipe popup swallows scrolling
        if (aspectPopup) {
            int pageCount = BookPopupRenderer.aspectPageCount();
            if (scrollY < 0 && aspectPage < pageCount - 1) {
                aspectPage++;
                return true;
            } else if (scrollY > 0 && aspectPage > 0) {
                aspectPage--;
                return true;
            }
            return true;
        }
        if (activeRecipe != null) {
            return true;
        }

        if (scrollY < 0 && page < maxPages - 2) {
            page += 2;
            return true;
        } else if (scrollY > 0 && page > 0) {
            page -= 2;
            if (page < 0) page = 0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public void onClose() {
        // Return to research browser at saved position
        minecraft.gui.setScreen(new ResearchBrowserScreen(returnX, returnY));
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Close any open popup.
     */
    private void closePopup() {
        activeRecipe = null;
        aspectPopup = false;
        aspectPage = 0;
        popupBounds = null;
    }
    // ==================== Inner Classes ====================
    
    /**
     * Represents a single page of content.
     */
    private static class Page {
        ArrayList<Object> contents = new ArrayList<>();
        boolean isAddendum = false;
        int stageId = -1;
    }
}
