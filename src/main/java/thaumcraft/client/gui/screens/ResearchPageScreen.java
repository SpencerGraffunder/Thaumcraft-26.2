package thaumcraft.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerKnowledge.EnumKnowledgeType;
import thaumcraft.api.research.*;
import thaumcraft.common.lib.capabilities.ThaumcraftCapabilities;
import thaumcraft.client.gui.BookPopupRenderer;
import thaumcraft.client.gui.RecipeRenderer;
import thaumcraft.client.lib.AspectRenderer;
import thaumcraft.init.ModSounds;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.research.ResearchManager;

import java.util.ArrayList;
import java.util.List;

/**
 * ResearchPageScreen - Displays detailed information about a research entry.
 *
 * 1.12-parity port of GuiResearchPage:
 *  - Two-column page text flow with a height-budget pagination system
 *    (first page 182px, later pages 210px; each line costs lineHeight, each
 *    requirement row 18px, divider 15px, &lt;IMG&gt; ah+2px, &lt;BR&gt; +0.66*lineHeight).
 *  - Requirement rows (research/obtain/craft/know) with the 1.12 background
 *    bars, icon spacing (shift=24, ss=18 or 110/count when &gt;6), checkmarks,
 *    row tooltips (tc.need.*) and per-icon tooltips.
 *  - Complete button (tex uv 84,216, 64x12, tc.stage.complete / tc.stage.hold).
 *  - Aspect + knowledge edge bookmarks, warp warning, in-page knowledge grid
 *    for the KNOWLEDGETYPES research, knowledge popup overlay.
 */
@OnlyIn(Dist.CLIENT)
public class ResearchPageScreen extends Screen {

    // Textures
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook.png");
    private static final Identifier PAPER = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/paper.png");
    private static final Identifier AURA_NODES = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/auranodes.png");

    // 1.12 inline divider images (GuiResearchPage static init):
    // PILINE = gui_researchbook 24,184 95x6; PIDIV = 28,192 140x6
    private static final PageImage PILINE = PageImage.parse("thaumcraft:textures/gui/gui_researchbook.png:24:184:95:6:1");
    private static final PageImage PIDIV = PageImage.parse("thaumcraft:textures/gui/gui_researchbook.png:28:192:140:6:1");

    // Pane dimensions (the book is blitted at 1.3x around screen center sw/sh)
    private static final int PANE_WIDTH = 256;
    private static final int PANE_HEIGHT = 181;
    // 1.12 page text column width (PAGEWIDTH)
    private static final int TEXT_WIDTH = 140;
    // 1.12 page height budgets (GuiResearchPage.parsePages): first page 182, later pages 210
    private static final int FIRST_PAGE_BUDGET = 182;
    private static final int PAGE_BUDGET = 210;

    // Research data
    private final ResearchEntry research;
    private final Identifier highlightRecipe;
    private final double returnX;
    private final double returnY;

    // Player data
    private Player player;
    private IPlayerKnowledge playerKnowledge;

    // Stage / page tracking (1.12 semantics: stored stage is 1-based "current
    // stage number"; unknown research -> getResearchStage() == -1)
    private int currentStage = 0;
    private int lastStage = 0;
    private boolean hold = false;
    private int page = 0;
    private int maxPages = 1;
    private boolean isComplete = false;
    private boolean hasAllRequisites = false;
    private boolean[] hasItem = null;
    private boolean[] hasCraft = null;
    private boolean[] hasResearch = null;
    private boolean[] hasKnow = null;

    // Parsed page content
    private final List<Page> pages = new ArrayList<>();

    // Recipe bookmarks for the current stage (drawn on the book's right edge)
    private List<Identifier> bookmarks = List.of();

    // Popup state (exclusive, like 1.12 showingAspects/showingKnowledge/shownRecipe)
    private boolean aspectPopup = false;
    private int aspectPage = 0;
    private Identifier activeRecipe = null;
    private boolean showingKnowledge = false;
    private BookPopupRenderer.Bounds popupBounds = null;

    // Complete button rect (set in drawRequirements, like 1.12 hrx/hry)
    private int hrx = 0;
    private int hry = 0;

    // Tooltip collected during the render pass (1.12 tipText)
    private List<Component> tipText = new ArrayList<>();

    // hold re-parse cadence (1.12 re-parses every 250ms while hold, 2s otherwise)
    private int reparseTicks = 0;

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
        int bookRight = (width + (int)(PANE_WIDTH * 1.3f)) / 2;
        int bookTop = (height - (int)(PANE_HEIGHT * 1.3f)) / 2;
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> this.onClose())
                .bounds(bookRight - 18, bookTop + 4, 16, 16)
                .build());
        parsePages();
    }

    /**
     * While the "Completing..." hold state is active, re-parse at ~250ms so the
     * book refreshes as soon as the server confirms the stage (1.12 drawScreen
     * lastCheck/hold logic). Clears hold once the stage has advanced past lastStage.
     */
    @Override
    public void tick() {
        super.tick();
        if (!hold || playerKnowledge == null) return;
        reparseTicks++;
        if (reparseTicks >= 15) {
            reparseTicks = 0;
            int before = currentStage;
            parsePages();
            if (currentStage > before) {
                hold = false;
                reparseTicks = 0;
            }
        }
    }

    // ==================== Page parsing (1.12 parsePages parity) ====================

    private void parsePages() {
        checkRequisites();
        pages.clear();
        if (research.getStages() == null) {
            maxPages = 1;
            return;
        }

        // 1.12: currentStage = getResearchStage(key) - 1; clamp to [0, length-1].
        // 26.2 getResearchStage(): -1 unknown, else 1-based current stage number.
        currentStage = playerKnowledge != null ? playerKnowledge.getResearchStage(research.getKey()) - 1 : 0;
        if (currentStage < 0) currentStage = 0;
        while (currentStage >= research.getStages().length) {
            --currentStage;
        }
        if (currentStage < 0) currentStage = 0;

        // 1.12 getResearchStatus: complete when stored stage > stages.length
        isComplete = playerKnowledge != null
                && playerKnowledge.getResearchStage(research.getKey()) > research.getStages().length;

        ResearchStage stage = research.getStages()[currentStage];
        ResearchAddendum[] addenda = null;
        if (research.getAddenda() != null && isComplete) {
            addenda = research.getAddenda();
        }

        bookmarks = buildBookmarks(stage, addenda);

        if (bookmarks.isEmpty()) {
            // No bookmark for this stage - hide the recipe popup if it was showing it
            activeRecipe = null;
        }

        // Stage text + addenda (1.12: addenda append <PAGE> + "Addendum N" + text)
        String rawText = stage.getText() != null ? Component.translatable(stage.getText()).getString() : "";
        if (addenda != null) {
            int ac = 0;
            for (ResearchAddendum addendum : addenda) {
                if (addendumSatisfied(addendum)) {
                    ++ac;
                    String addText = addendum.getText() != null
                            ? Component.translatable(addendum.getText()).getString() : "";
                    rawText = rawText + "<PAGE>" + Component.translatable("tc.addendumtext", ac).getString() + "<BR>" + addText;
                }
            }
        }

        rawText = rawText.replaceAll("<BR>", "~B\n\n")
                .replaceAll("<BR/>", "~B\n\n")
                .replaceAll("<LINE>", "~L")
                .replaceAll("<LINE/>", "~L")
                .replaceAll("<DIV>", "~D")
                .replaceAll("<DIV/>", "~D")
                .replaceAll("<PAGE>", "~P")
                .replaceAll("<PAGE/>", "~P");

        // <IMG> -> ~I markers (keep parse order in the images list)
        ArrayList<PageImage> images = new ArrayList<>();
        String[] split = rawText.split("<IMG>");
        for (String s : split) {
            int i = s.indexOf("</IMG>");
            if (i >= 0) {
                String clean = s.substring(0, i);
                PageImage pi = PageImage.parse(clean);
                if (pi == null) {
                    rawText = rawText.replaceFirst(clean, "\n");
                } else {
                    images.add(pi);
                    rawText = rawText.replaceFirst(clean, "~I");
                }
            }
        }
        rawText = rawText.replaceAll("<IMG>", "").replaceAll("</IMG>", "");

        // First pass: split into a linear sequence of text chunks and markers
        List<String> firstPassText = new ArrayList<>();
        String[] temp = rawText.split("~P");
        for (int a = 0; a < temp.length; ++a) {
            String t = temp[a];
            String[] temp2 = t.split("~D");
            for (int x = 0; x < temp2.length; ++x) {
                String t2 = temp2[x];
                String[] temp3 = t2.split("~L");
                for (int b = 0; b < temp3.length; ++b) {
                    String t3 = temp3[b];
                    String[] temp4 = t3.split("~I");
                    for (int c = 0; c < temp4.length; ++c) {
                        firstPassText.add(temp4[c]);
                        if (c != temp4.length - 1) {
                            firstPassText.add("~I");
                        }
                    }
                    if (b != temp3.length - 1) {
                        firstPassText.add("~L");
                    }
                }
                if (x != temp2.length - 1) {
                    firstPassText.add("~D");
                }
            }
            if (a != temp.length - 1) {
                firstPassText.add("~P");
            }
        }

        // Word-wrap each chunk to the 140px page column (1.12 listFormattedStringToWidth)
        List<String> parsedText = new ArrayList<>();
        for (String s2 : firstPassText) {
            parsedText.addAll(wrapWidth(s2, TEXT_WIDTH));
        }

        // Height-budget flow (1.12 parsePages): first page 182px, later 210px;
        // each requirement row subtracts 18px, divider 15px (12px for the
        // KNOWLEDGETYPES in-page knowledge grid)
        int lineHeight = font.lineHeight;
        int heightRemaining = FIRST_PAGE_BUDGET;
        // 1.12: the left side of the first spread has a 28px title, so reduce
        // the budget for text content to match the available space.
        if (page == 0) {
            heightRemaining -= 28;
        }
        int dividerSpace = 0;
        if ("KNOWLEDGETYPES".equals(research.getKey())) {
            heightRemaining -= 2;
            int tc = 0;
            if (playerKnowledge != null) {
                for (EnumKnowledgeType type : EnumKnowledgeType.values()) {
                    for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
                        if (!type.hasFields() && category != null) {
                            continue;
                        }
                        if (playerKnowledge.getKnowledgeRaw(type, category.key) > 0) {
                            ++tc;
                            break;
                        }
                    }
                }
            }
            heightRemaining -= 20 * tc;
            dividerSpace = 12;
        }
        if (!isComplete) {
            if (stage.getCraft() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
            if (stage.getObtain() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
            if (stage.getKnow() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
            if (stage.getResearch() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
        }
        heightRemaining -= dividerSpace;

        Page page1 = new Page();
        ArrayList<PageImage> tempImages = new ArrayList<>();
        for (String line : parsedText) {
            if (line.contains("~I")) {
                if (!images.isEmpty()) {
                    tempImages.add(images.remove(0));
                }
                line = "";
            }
            if (line.contains("~L")) {
                tempImages.add(PILINE);
                line = "";
            }
            if (line.contains("~D")) {
                tempImages.add(PIDIV);
                line = "";
            }
            if (line.contains("~P")) {
                heightRemaining = PAGE_BUDGET;
                pages.add(page1.copy());
                page1 = new Page();
                line = "";
            }
            if (!line.isEmpty()) {
                line = line.trim();
                page1.contents.add(line);
                heightRemaining -= lineHeight;
                if (line.endsWith("~B")) {
                    heightRemaining -= (int)(lineHeight * 0.66);
                }
            }
            while (!tempImages.isEmpty() && heightRemaining >= tempImages.get(0).ah + 2) {
                heightRemaining -= tempImages.get(0).ah + 2;
                page1.contents.add(tempImages.remove(0));
            }
            if (heightRemaining < lineHeight && !page1.contents.isEmpty()) {
                heightRemaining = PAGE_BUDGET;
                pages.add(page1.copy());
                page1 = new Page();
            }
        }
        if (!page1.contents.isEmpty()) {
            pages.add(page1.copy());
        }
        page1 = new Page();
        heightRemaining = PAGE_BUDGET;
        while (!tempImages.isEmpty()) {
            if (heightRemaining < tempImages.get(0).ah + 2) {
                heightRemaining = PAGE_BUDGET;
                pages.add(page1.copy());
                page1 = new Page();
            } else {
                heightRemaining -= tempImages.get(0).ah + 2;
                page1.contents.add(tempImages.remove(0));
            }
        }
        if (!page1.contents.isEmpty()) {
            pages.add(page1.copy());
        }
        maxPages = Math.max(1, pages.size());
    }

    private boolean addendumSatisfied(ResearchAddendum addendum) {
        if (addendum.getResearch() == null || addendum.getResearch().length == 0) {
            return true;
        }
        if (player == null) return false;
        for (String req : addendum.getResearch()) {
            if (!ThaumcraftCapabilities.isResearchComplete(player, req)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Requirement tracking for the current stage (1.12 checkRequisites).
     */
    private void checkRequisites() {
        if (playerKnowledge == null) return;
        hasAllRequisites = true;
        hasItem = null;
        hasCraft = null;
        hasResearch = null;
        hasKnow = null;
        ResearchStage[] stages = research.getStages();
        if (stages == null) return;
        int cs = playerKnowledge.getResearchStage(research.getKey()) - 1;
        if (cs < 0) cs = 0;
        if (cs >= stages.length) return;
        ResearchStage stage = stages[cs];

        if (stage.getObtain() != null) {
            hasItem = new boolean[stage.getObtain().length];
            for (int i = 0; i < stage.getObtain().length; i++) {
                Object o = stage.getObtain()[i];
                if (o instanceof ItemStack stack && !stack.isEmpty()) {
                    hasItem[i] = playerHasItem(stack);
                } else {
                    hasItem[i] = false; // ore/tag requirement - not carried
                }
                if (!hasItem[i]) hasAllRequisites = false;
            }
        }
        if (stage.getCraft() != null) {
            hasCraft = new boolean[stage.getCraft().length];
            int[] refs = stage.getCraftReference();
            for (int i = 0; i < stage.getCraft().length; i++) {
                String refKey = refs != null && i < refs.length ? "[#]" + refs[i] : "";
                hasCraft[i] = playerKnowledge.isResearchKnown(refKey);
                if (!hasCraft[i]) hasAllRequisites = false;
            }
        }
        if (stage.getResearch() != null) {
            hasResearch = new boolean[stage.getResearch().length];
            for (int i = 0; i < stage.getResearch().length; i++) {
                hasResearch[i] = ThaumcraftCapabilities.isResearchComplete(player, stage.getResearch()[i]);
                if (!hasResearch[i]) hasAllRequisites = false;
            }
        }
        if (stage.getKnow() != null) {
            hasKnow = new boolean[stage.getKnow().length];
            for (int i = 0; i < stage.getKnow().length; i++) {
                ResearchStage.Knowledge k = stage.getKnow()[i];
                int playerKnow = playerKnowledge.getKnowledge(k.type, k.category != null ? k.category.key : null);
                hasKnow[i] = playerKnow >= k.amount;
                if (!hasKnow[i]) hasAllRequisites = false;
            }
        }
    }

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
     * Word-wrap text to a pixel width, preserving explicit newlines and inline
     * markers (e.g. the ~B paragraph marker stays attached to its word).
     */
    private List<String> wrapWidth(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) continue;
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                if (font.width(testLine) > maxWidth && currentLine.length() > 0) {
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

    // ==================== Rendering ====================

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int sw = (width - PANE_WIDTH) / 2;
        int sh = (height - PANE_HEIGHT) / 2;
        tipText = new ArrayList<>();

        // Draw book background at 1.3x scale (matching 1.12.2 GuiResearchPage)
        graphics.pose().pushMatrix();
        graphics.pose().translate((width - PANE_WIDTH * 1.3f) / 2.0f, (height - PANE_HEIGHT * 1.3f) / 2.0f);
        graphics.pose().scale(1.3f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, PANE_WIDTH, PANE_HEIGHT, 256, 256);
        graphics.pose().popMatrix();

        // Pages of the current spread (1.12: drawPage(pages.get(a), a % 2, sw, sh - 10))
        for (int a = 0; a < pages.size(); a++) {
            if (a == page || a == page + 1) {
                drawPage(graphics, pages.get(a), a % 2, sw, sh - 10, mouseX, mouseY);
            }
        }

        // Overlay popups (exclusive, like 1.12 showingAspects/showingKnowledge/shownRecipe)
        if (aspectPopup) {
            List<Component> popupTip = new ArrayList<>();
            popupBounds = BookPopupRenderer.drawAspectPopup(graphics, sw + PANE_WIDTH / 2, sh + PANE_HEIGHT / 2, aspectPage, mouseX, mouseY, font, popupTip);
            if (!popupTip.isEmpty()) tipText = popupTip;
        } else if (showingKnowledge) {
            popupBounds = drawKnowledgePopup(graphics, sw, sh, mouseX, mouseY);
        } else if (activeRecipe != null) {
            List<Component> popupTip = new ArrayList<>();
            popupBounds = BookPopupRenderer.drawRecipePopup(graphics, sw + PANE_WIDTH / 2, sh + PANE_HEIGHT / 2, activeRecipe, mouseX, mouseY, font, popupTip);
            if (!popupTip.isEmpty()) tipText = popupTip;
        }

        // Draw navigation arrows (1.12.2 positions)
        float bob = (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.2f + 0.1f;
        if (page > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, sw - 16, sh + 190, 0.0F, 184.0F, 12, 8, 256, 256, ARGB.white(0.8f + bob));
        }
        if (page < maxPages - 2) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, sw + 262, sh + 190, 12.0F, 184.0F, 12, 8, 256, 256, ARGB.white(0.8f + bob));
        }

        // Draw page numbers below the 1.3x book
        String pageNum = (page / 2 + 1) + " / " + ((maxPages + 1) / 2);
        int bookBottom = (height + (int)(PANE_HEIGHT * 1.3f)) / 2;
        graphics.centeredText(font, pageNum, sw + PANE_WIDTH / 2, bookBottom + 5, 0xFF808080);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Draw tooltip last (1.12: drawCustomTooltip at mouse + 12)
        if (!tipText.isEmpty()) {
            graphics.setTooltipForNextFrame(font, tipText, java.util.Optional.empty(), mouseX, mouseY + 12);
        }
    }

    /**
     * Draw one page of the spread (1.12 drawPage). x/y = sw, sh-10; side 0 = left
     * column (x-15), side 1 = right column (x+137).
     */
    private void drawPage(GuiGraphicsExtractor graphics, Page pageParm, int side, int x, int y, int mx, int my) {
        boolean noPopup = !aspectPopup && !showingKnowledge && activeRecipe == null;
        if (page == 0 && side == 0) {
            // 1.12 title cluster: two 96x4 bars (uv 24,184) around the title
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 4, y - 7, 24.0F, 184.0F, 96, 4, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 4, y + 10, 24.0F, 184.0F, 96, 4, 256, 256);
            String title = research.getLocalizedName().getString();
            int offset = font.width(title);
            if (offset <= 140) {
                graphics.text(font, title, x - 15 + 140 / 2 - offset / 2, y, 0xFF202020, false);
            } else {
                float vv = 140.0f / offset;
                graphics.pose().pushMatrix();
                graphics.pose().translate(x - 15 + 140 / 2 - offset / 2 * vv, y + 1.0f * vv);
                graphics.pose().scale(vv);
                graphics.text(font, title, 0, 0, 0xFF202020, false);
                graphics.pose().popMatrix();
            }
            y += 28;
        }

        // Page content: top-anchored flow (1.12: text at y-6, images at y-5)
        for (Object content : pageParm.contents) {
            if (content instanceof String) {
                String ss = ((String) content).replace("~B", "");
                graphics.text(font, ss, x - 15 + side * 152, y - 6, 0xFF505050, false);
                y += font.lineHeight;
                if (((String) content).endsWith("~B")) {
                    y += (int)(font.lineHeight * 0.66);
                }
            } else if (content instanceof PageImage pi) {
                int pad = (140 - pi.aw) / 2;
                graphics.pose().pushMatrix();
                graphics.pose().translate(x - 15 + side * 152 + pad, y - 5);
                graphics.pose().scale(pi.scale);
                graphics.blit(RenderPipelines.GUI_TEXTURED, pi.texture, 0, 0, pi.u, pi.v, pi.w, pi.h, 256, 256);
                graphics.pose().popMatrix();
                y += pi.ah + 2;
            }
        }

        int sh = (height - PANE_HEIGHT) / 2;

        // Aspect bookmark on the book's left edge (1.12: x-48, sh+9; uv 76,232)
        if (playerKnowledge != null && playerKnowledge.isResearchComplete("FIRSTSTEPS")) {
            int ay = sh + 9;
            boolean hov = noPopup && mouseInside(x - 48, ay, 25, 16, mx, my);
            int le = hov ? 0 : 3;
            if (hov) setTip("tc.aspect.name");
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x - 48 + le, ay, 76, 232, 24 - le, 16, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x - 28, ay, 100, 232, 4, 16, 256, 256);
        }

        // Knowledge bookmark (1.12: x-49, sh+32; uv 44,232)
        if (playerKnowledge != null && playerKnowledge.isResearchComplete("KNOWLEDGETYPES")
                && !"KNOWLEDGETYPES".equals(research.getKey())) {
            int ky = sh + 32;
            boolean hov = noPopup && mouseInside(x - 49, ky, 25, 16, mx, my);
            int le = hov ? 0 : 3;
            if (hov) setTip("tc.knowledge.name");
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x - 49 + le, ky, 44, 232, 24 - le, 16, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x - 29, ky, 100, 232, 4, 16, 256, 256);
        }

        // Recipe bookmarks on the book's right edge (1.12 drawRecipeBookmarks)
        int space = bookmarks.size() > 0 ? Math.min(25, 200 / bookmarks.size()) : 25;
        for (int i = 0; i < Math.min(5, bookmarks.size()); i++) {
            int ry = sh - 8 + i * space;
            boolean hov = noPopup && mouseInside(x + 280, ry - 1, 30, 16, mx, my);
            int le = hov ? 0 : 3;
            int color = bookmarks.get(i).equals(activeRecipe) ? 0xFFFF8080 : 0xFFFFFFFF;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 280, ry - 1, 120 + le, 232, 28, 16, 256, 256, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 280, ry - 1, 116, 232, 4, 16, 256, 256, color);
            ItemStack out = RecipeRenderer.resolveOutput(RecipeRenderer.findRecipe(bookmarks.get(i)));
            if (!out.isEmpty()) {
                RecipeRenderer.renderItem(graphics, out, x + 280 + 7 - le, ry - 1);
                if (hov) {
                    // 1.12 drawStackAt: show item tooltip on hover
                    setTip(out.getHoverName().getString());
                }
            }
        }

        // Requirements (left page only, first spread, not complete) - sets hrx/hry
        if (page == 0 && side == 0 && !isComplete && research.getStages() != null) {
            y = drawRequirements(graphics, x, y, mx, my, research.getStages()[currentStage]);
        }

        // Warp warning (1.12: forbidden node icon + level name, below the text).
        // 1.12 draws it in the else-if after the popups: only when no popup is open.
        ResearchStage[] stages = research.getStages();
        if (stages != null && currentStage < stages.length) {
            ResearchStage stage = stages[currentStage];
            if (stage.getWarp() > 0 && !isComplete && noPopup) {
                drawWarpWarning(graphics, x, sh, mx, my, stage.getWarp());
            }
        }

        // KNOWLEDGETYPES in-page knowledge grid (1.12 drawKnowledges inpage=true)
        if ("KNOWLEDGETYPES".equals(research.getKey())) {
            drawKnowledges(graphics, x, sh - 16 + 210, mx, my, true, noPopup);
        }
    }

    /**
     * Warp warning (1.12): animated purple auranodes cell at (x-57, y-40) where y is
     * the pane top (1.12 drawPage's y = (height-256)/2+32), level name centered below
     * it, tooltip "tc.warp.warn". Called only when no popup is open (1.12 else-if).
     */
    private void drawWarpWarning(GuiGraphicsExtractor graphics, int x, int sh, int mx, int my, int warp) {
        warp = Math.min(5, warp);
        int y = sh + 32; // 1.12 drawPage cursor position at this point
        int count = (int)(System.currentTimeMillis() / 1000L) % 32;
        // auranodes.png is 2048px, 32x32 grid of 64px cells; forbidden art = row 5,
        // frame count%32. 1.12 draws it as a 90px quad tinted purple (0.33,0,0.44),
        // brightness 220, opacity 0.9.
        int cell = 2048 / 32;
        int u0 = count * cell, v0 = 5 * cell;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x - 57 + 45, y - 40 + 45);
        graphics.pose().translate(-45, -45);
        graphics.blit(RenderPipelines.GUI_TEXTURED, AURA_NODES, 0, 0, u0, v0, cell, cell, 2048, 2048, 0xE6540070);
        graphics.pose().popMatrix();
        String s = Component.translatable("tc.forbidden.level." + warp).getString();
        graphics.centeredText(font, s, x - 57 + 45, y - 43 + 8, 0xFFAA00EE);
        if (mouseInside(x - 67, y - 50, 20, 20, mx, my)) {
            String text = Component.translatable("tc.warp.warn").getString();
            setTip(text.replaceAll("%n", s));
        }
    }

    /**
     * Draw current stage requirement rows (1.12 drawRequirements geometry):
     * rows stack upward from sh-16+210; each row is an 8px-high translucent bar
     * (uv 200,232/216/200/184) at (x-12, y-1) plus icons at x-15+shift with
     * shift=24, step 18 (or 110/count when more than 6 icons). Returns the
     * updated row cursor y.
     */
    private int drawRequirements(GuiGraphicsExtractor graphics, int x, int y, int mx, int my, ResearchStage stage) {
        int y0 = (height - PANE_HEIGHT) / 2 - 16 + 210;
        y = y0;
        boolean b = false;
        boolean noPopup = !aspectPopup && !showingKnowledge && activeRecipe == null;

        if (stage.getResearch() != null) {
            y -= 18;
            b = true;
            int shift = 24;
            drawRowBar(graphics, x, y, 232, "tc.need.research", mx, my, noPopup);
            int ss = 18;
            if (stage.getResearch().length > 6) {
                ss = 110 / stage.getResearch().length;
            }
            String[] icons = stage.getResearchIcon();
            for (int a = 0; a < stage.getResearch().length; a++) {
                int ix = x - 15 + shift;
                String key = stage.getResearch()[a];
                String text = key != null ? Component.translatable("research." + key + ".text").getString() : "";
                boolean found = false;
                if (key != null && key.startsWith("!")) {
                    Aspect as = Aspect.aspects.get(key.substring(1));
                    if (as != null) {
                        AspectRenderer.drawAspect(graphics, ix, y, as);
                        text = as.getName();
                        found = true;
                    }
                }
                ResearchEntry re = key != null ? ResearchCategories.getResearch(key) : null;
                if (re != null && re.getIcons() != null && re.getIcons().length > 0) {
                    int idx = (int)(System.currentTimeMillis() / 1000L % re.getIcons().length);
                    Object icon = re.getIcons()[idx];
                    drawReqIcon(graphics, icon, ix, y);
                    text = re.getLocalizedName().getString();
                    found = true;
                } else if (key != null && key.startsWith("m_")) {
                    drawFlatIcon(graphics, "textures/research/rd_map.png", ix, y, 0xFFFFFFFF);
                    found = true;
                } else if (key != null && key.startsWith("c_")) {
                    drawFlatIcon(graphics, "textures/research/rd_chest.png", ix, y, 0xFFFFFFFF);
                    found = true;
                } else if (key != null && key.startsWith("f_")) {
                    drawFlatIcon(graphics, "textures/research/rd_flask.png", ix, y, 0xFFFFFFFF);
                    found = true;
                }
                if (!found) {
                    // 1.12: unknown research -> _unknown.png, blue-tinted
                    drawFlatIcon(graphics, "textures/aspects/_unknown.png", ix, y, 0xFF80BFFF);
                }
                if (hasResearch != null && a < hasResearch.length && hasResearch[a]) {
                    drawCheckmark(graphics, ix, y);
                }
                if (noPopup && mouseInside(ix, y, 16, 16, mx, my)) {
                    setTip(text);
                }
                shift += ss;
            }
        }
        if (stage.getObtain() != null) {
            y -= 18;
            b = true;
            int shift = 24;
            drawRowBar(graphics, x, y, 216, "tc.need.obtain", mx, my, noPopup);
            int ss = 18;
            if (stage.getObtain().length > 6) {
                ss = 110 / stage.getObtain().length;
            }
            for (int i = 0; i < stage.getObtain().length; i++) {
                int ix = x - 15 + shift;
                if (stage.getObtain()[i] instanceof ItemStack s && !s.isEmpty()) {
                    RecipeRenderer.renderItem(graphics, s, ix, y);
                    if (noPopup && mouseInside(ix, y, 16, 16, mx, my)) {
                        setStackTip(graphics, s, ix, y, mx, my);
                    }
                }
                if (hasItem != null && i < hasItem.length && hasItem[i]) {
                    drawCheckmark(graphics, ix, y);
                }
                shift += ss;
            }
        }
        if (stage.getCraft() != null) {
            y -= 18;
            b = true;
            int shift = 24;
            drawRowBar(graphics, x, y, 200, "tc.need.craft", mx, my, noPopup);
            int ss = 18;
            if (stage.getCraft().length > 6) {
                ss = 110 / stage.getCraft().length;
            }
            for (int i = 0; i < stage.getCraft().length; i++) {
                int ix = x - 15 + shift;
                if (stage.getCraft()[i] instanceof ItemStack s && !s.isEmpty()) {
                    RecipeRenderer.renderItem(graphics, s, ix, y);
                    if (noPopup && mouseInside(ix, y, 16, 16, mx, my)) {
                        setStackTip(graphics, s, ix, y, mx, my);
                    }
                }
                if (hasCraft != null && i < hasCraft.length && hasCraft[i]) {
                    drawCheckmark(graphics, ix, y);
                }
                shift += ss;
            }
        }
        if (stage.getKnow() != null) {
            y -= 18;
            b = true;
            int shift = 24;
            drawRowBar(graphics, x, y, 184, "tc.need.know", mx, my, noPopup);
            int ss = 18;
            if (stage.getKnow().length > 6) {
                ss = 110 / stage.getKnow().length;
            }
            for (int i = 0; i < stage.getKnow().length; i++) {
                int ix = x - 15 + shift;
                ResearchStage.Knowledge kn = stage.getKnow()[i];
                Identifier knowTex = knowTypeTexture(kn.type);
                if (knowTex != null) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, knowTex, ix, y, 0, 0, ss, ss, 256, 256);
                }
                if (kn.type.hasFields() && kn.category != null) {
                    Identifier catIcon = kn.category.icon;
                    if (catIcon != null) {
                        graphics.blit(RenderPipelines.GUI_TEXTURED, catIcon, ix + 8, y + 8, 0, 0, 10, 10, 16, 16, ARGB.white(0.75f));
                    }
                }
                boolean ok = hasKnow != null && i < hasKnow.length && hasKnow[i];
                String am = kn.amount + "";
                int m = font.width(am);
                graphics.text(font, am, ix + 16 - m / 2, y + 12, ok ? 0xFFFFFFFF : 0xFFFF5555, false);
                if (ok) {
                    drawCheckmark(graphics, ix, y);
                }
                String s = Component.translatable("tc.type." + kn.type.toString().toLowerCase()).getString();
                if (kn.type.hasFields() && kn.category != null) {
                    s = s + ": " + Component.translatable(kn.category.key).getString();
                }
                if (noPopup && mouseInside(ix, y, 16, 16, mx, my)) {
                    setTip(s);
                }
                shift += ss;
            }
        }

        // Divider bar + complete button (1.12: y -= 12, bar uv 24,184 96x8,
        // button uv 84,216 64x12 at (x+20, y-6), label centered at x+52)
        if (b) {
            y -= 12;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 4, y - 2, 24.0F, 184.0F, 96, 8, 256, 256);
            if (hasAllRequisites) {
                hrx = x + 20;
                hry = y - 6;
                String s2 = Component.translatable(hold ? "tc.stage.hold" : "tc.stage.complete").getString();
                if (hold) {
                    graphics.centeredText(font, s2, x + 52, y - 4, 0xFFFFFFFF);
                } else {
                    boolean hov = mouseInside(hrx, hry, 64, 12, mx, my);
                    graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, hrx, hry, 84, 216, 64, 12, 256, 256,
                            hov ? ARGB.white(1f) : ARGB.color(255, 204, 204, 230));
                    graphics.centeredText(font, s2, x + 52, y - 4, 0xFFFFFFFF);
                }
            }
        }
        return y;
    }

    /** Row background bar (uv 200,<y>, 56x16, alpha 0.25) + row-label tooltip. */
    private void drawRowBar(GuiGraphicsExtractor g, int x, int y, int barUvY, String tipKey, int mx, int my, boolean noPopup) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x - 12, y - 1, 200, barUvY, 56, 16, 256, 256, ARGB.white(0.25f));
        if (noPopup && mouseInside(x - 15, y, 16, 16, mx, my)) {
            setTip(Component.translatable(tipKey).getString());
        }
    }

    /** 1.12 checkmark: tex1 uv 159,207, 10x10, over a satisfied icon. */
    private void drawCheckmark(GuiGraphicsExtractor g, int iconX, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, iconX + 8, y, 159, 207, 10, 10, 256, 256);
    }

    private void drawFlatIcon(GuiGraphicsExtractor g, String path, int x, int y, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath(Thaumcraft.MODID, path), x, y, 0, 0, 16, 16, 16, 16, color);
    }

    private void drawReqIcon(GuiGraphicsExtractor g, Object icon, int x, int y) {
        if (icon instanceof Aspect a) {
            AspectRenderer.drawAspect(g, x, y, a);
        } else if (icon instanceof ItemStack s) {
            if (!s.isEmpty()) RecipeRenderer.renderItem(g, s, x, y);
        } else if (icon instanceof Identifier id) {
            // 1.12 rendered research icons that are items as 3D items
            try {
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    RecipeRenderer.renderItem(g, new ItemStack(item), x, y);
                    return;
                }
            } catch (Exception ignored) {
            }
            g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, 16, 16, 16, 16);
        } else {
            g.fill(x + 2, y + 2, x + 14, y + 14, 0xFF707070);
        }
    }

    private void setStackTip(GuiGraphicsExtractor g, ItemStack stack, int ix, int y, int mx, int my) {
        List<Component> tip = new ArrayList<>();
        try {
            tip.addAll(stack.getTooltipLines(
                    net.minecraft.world.item.Item.TooltipContext.of(Minecraft.getInstance().player.level(), Minecraft.getInstance().player),
                    Minecraft.getInstance().player,
                    Minecraft.getInstance().options.advancedItemTooltips
                            ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED
                            : net.minecraft.world.item.TooltipFlag.Default.NORMAL));
        } catch (Exception ignored) {
        }
        if (!tip.isEmpty()) {
            tipText = tip;
        }
    }

    private void setTip(String text) {
        tipText = List.of(Component.literal(text));
    }

    /**
     * Recipe bookmarks (1.12 generateRecipesLists): the stage's recipes plus, when the
     * research is complete, the recipes of satisfied addenda (checked by their
     * getResearch() prerequisites). Order: recipe key hash (1.12 LinkedHashMap keyed by
     * hash), capped at the first 5 (1.12 draws up to 5 slots of 25px spacing).
     */
    private List<Identifier> buildBookmarks(ResearchStage stage, ResearchAddendum[] addenda) {
        List<Identifier> out = new ArrayList<>();
        if (stage.getRecipes() != null) {
            for (Identifier rk : stage.getRecipes()) {
                if (RecipeRenderer.findRecipe(rk) != null) {
                    out.add(rk);
                }
            }
        }
        if (addenda != null) {
            for (ResearchAddendum addendum : addenda) {
                if (addendum.getRecipes() != null && addendumSatisfied(addendum)) {
                    for (Identifier rk : addendum.getRecipes()) {
                        if (RecipeRenderer.findRecipe(rk) != null && !out.contains(rk)) {
                            out.add(rk);
                        }
                    }
                }
            }
        }
        if (out.size() > 5) {
            out = out.subList(0, 5);
        }
        return out;
    }

    // ==================== Knowledge grid / popup (1.12 drawKnowledges) ====================

    private Identifier knowTypeTexture(EnumKnowledgeType type) {
        if (type == EnumKnowledgeType.THEORY) {
            return Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/research/knowledge_theory.png");
        }
        if (type == EnumKnowledgeType.OBSERVATION) {
            return Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/research/knowledge_observation.png");
        }
        return null;
    }

    /**
     * Knowledge grid (1.12 drawKnowledges). inpage=true: 16px icons, 20px rows,
     * in-page bar; inpage=false: 255px icons, 28px rows (popup overlay).
     */
    private void drawKnowledges(GuiGraphicsExtractor graphics, int x, int y, int mx, int my, boolean inpage, boolean noPopup) {
        y -= 18;
        boolean drewSomething = false;
        int ka = ResearchCategories.researchCategories.values().size();
        int tc = 0;
        for (EnumKnowledgeType type : EnumKnowledgeType.values()) {
            int fc = 0;
            int hs = (int)(164.0f / ka);
            boolean rowDrew = false;
            for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
                if (!type.hasFields() && category != null) {
                    continue;
                }
                int amt = playerKnowledge.getKnowledge(type, category != null ? category.key : null);
                int par = playerKnowledge.getKnowledgeRaw(type, category != null ? category.key : null) % type.getProgression();
                if (amt <= 0 && par <= 0) {
                    continue;
                }
                drewSomething = true;
                rowDrew = true;
                int ix = x - 10 + (inpage ? 18 : hs) * fc;
                int iy = y - tc * (inpage ? 20 : 28);
                Identifier knowTex = knowTypeTexture(type);
                int s = inpage ? 16 : 255;
                if (knowTex != null) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, knowTex, ix, iy, 0, 0, 255, 255, s, s);
                }
                if (type.hasFields() && category != null && category.icon != null) {
                    int os = (int)(s * 0.66);
                    int o = (s - os) / 2;
                    graphics.blit(RenderPipelines.GUI_TEXTURED, category.icon, ix + o, iy + o, 0, 0, 15, 15, 16, 16, ARGB.white(0.75f));
                }
                String as = "" + amt;
                int m = font.width(as);
                graphics.text(font, as, ix + s - m, iy + (inpage ? 8 : 28), 0xFFFFFFFF, true);
                String s2 = Component.translatable("tc.type." + type.toString().toLowerCase()).getString();
                if (type.hasFields() && category != null) {
                    s2 = s2 + ": " + Component.translatable(category.key).getString();
                }
                if (noPopup && mouseInside(ix, iy, s, s, mx, my)) {
                    setTip(s2);
                }
                if (par > 0) {
                    int l = (int)(par / (float)type.getProgression() * 16.0f);
                    graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, ix, iy + s + 1, 0, 232, l, 2, 256, 256, ARGB.white(0.75f));
                    graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, ix + l, iy + s + 1, l, 234, 16 - l, 2, 256, 256, ARGB.white(0.75f));
                }
                ++fc;
            }
            if (rowDrew) {
                ++tc;
            }
        }
        if (inpage && drewSomething) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 4, y - tc * 20 + 12, 24.0F, 184.0F, 96, 8, 256, 256);
        }
    }

    /**
     * Knowledge popup overlay (1.12 drawKnowledgesInsert): paper background with
     * the large knowledge grid. Returns bounds for click handling.
     */
    private BookPopupRenderer.Bounds drawKnowledgePopup(GuiGraphicsExtractor graphics, int sw, int sh, int mx, int my) {
        int x = (width - 256) / 2;
        int y = (height - 256) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, PAPER, x, y, 0, 0, 255, 255, 256, 256);
        drawKnowledges(graphics, x + 60, sh + 75, mx, my, false, true);
        return new BookPopupRenderer.Bounds(x, y, 255, 255);
    }

    // ==================== Input ====================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int sw = (width - PANE_WIDTH) / 2;
        int sh = (height - PANE_HEIGHT) / 2;

        // A popup is open: aspect popup arrows page, anything outside closes it
        if (aspectPopup || activeRecipe != null || showingKnowledge) {
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

        // Complete button (1.12: hrx/hry 64x12, packet + write sound + hold state)
        if (!hold && hasAllRequisites && mouseInside(hrx, hry, 64, 12, mx, my)) {
            boolean first = playerKnowledge != null && !playerKnowledge.isResearchKnown(research.getKey());
            PacketHandler.sendToServer(new PacketSyncProgressToServer(research.getKey(), first, true, true));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.WRITE.get(), 0.66f));
            lastStage = currentStage;
            hold = true;
            reparseTicks = 0;
            return true;
        }

        // Left-edge aspect bookmark
        if (playerKnowledge != null && playerKnowledge.isResearchComplete("FIRSTSTEPS")
                && mouseInside(sw - 48, sh + 9, 25, 16, mx, my)) {
            aspectPopup = true;
            showingKnowledge = false;
            activeRecipe = null;
            aspectPage = 0;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PAGE.get(), 0.7f));
            return true;
        }

        // Left-edge knowledge bookmark
        if (playerKnowledge != null && playerKnowledge.isResearchComplete("KNOWLEDGETYPES")
                && !"KNOWLEDGETYPES".equals(research.getKey())
                && mouseInside(sw - 49, sh + 32, 25, 16, mx, my)) {
            showingKnowledge = true;
            aspectPopup = false;
            activeRecipe = null;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PAGE.get(), 0.7f));
            return true;
        }

        // Right-edge recipe bookmarks (1.12: 30x16 at sw+280, sh-8+aa*space)
        int space = bookmarks.size() > 0 ? Math.min(25, 200 / bookmarks.size()) : 25;
        for (int i = 0; i < Math.min(5, bookmarks.size()); i++) {
            if (mouseInside(sw + 280, sh - 8 + i * space - 1, 30, 16, mx, my)) {
                activeRecipe = bookmarks.get(i);
                aspectPopup = false;
                showingKnowledge = false;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PAGE.get(), 0.7f));
                return true;
            }
        }

        // Navigation arrows (1.12 hit boxes: 14x10 at sw-17/sh+189 and sw+261/sh+189)
        if (page > 0 && mouseInside(sw - 17, sh + 189, 14, 10, mx, my)) {
            page -= 2;
            if (page < 0) page = 0;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PAGE.get(), 0.66f));
            return true;
        }
        if (page < maxPages - 2 && mouseInside(sw + 261, sh + 189, 14, 10, mx, my)) {
            page += 2;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PAGE.get(), 0.66f));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private boolean mouseInside(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        // Aspect popup pages with the wheel; recipe/knowledge popups swallow it
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
        if (activeRecipe != null || showingKnowledge) {
            return true;
        }

        if (scrollY < 0 && page < maxPages - 2) {
            page += 2;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PAGE.get(), 0.66f));
            return true;
        } else if (scrollY > 0 && page > 0) {
            page -= 2;
            if (page < 0) page = 0;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PAGE.get(), 0.66f));
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

    private void closePopup() {
        activeRecipe = null;
        aspectPopup = false;
        aspectPage = 0;
        showingKnowledge = false;
        popupBounds = null;
    }

    // ==================== Inner Classes ====================

    /**
     * Represents a single page of content (lines + inline images/markers).
     */
    private static class Page {
        ArrayList<Object> contents = new ArrayList<>();

        Page copy() {
            Page p = new Page();
            p.contents.addAll(contents);
            return p;
        }
    }

    /**
     * An inline image in page text, parsed from an &lt;IMG&gt; tag:
     * {@code <IMG>domain:path:u:v:w:h:scale</IMG>} (7 colon-separated fields, 1.12 format).
     */
    private static class PageImage {
        int u, v, w, h;
        float scale;
        int aw, ah;
        Identifier texture;

        static PageImage parse(String text) {
            String[] s = text.trim().split(":");
            if (s.length != 7) return null;
            try {
                PageImage pi = new PageImage();
                pi.texture = Identifier.parse(s[0] + ":" + s[1]);
                pi.u = Integer.parseInt(s[2]);
                pi.v = Integer.parseInt(s[3]);
                pi.w = Integer.parseInt(s[4]);
                pi.h = Integer.parseInt(s[5]);
                pi.scale = Float.parseFloat(s[6]);
                pi.aw = (int) (pi.w * pi.scale);
                pi.ah = (int) (pi.h * pi.scale);
                if (pi.ah > 208 || pi.aw > 140) return null;
                return pi;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
