package thaumcraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketStartTheoryToServer;
import thaumcraft.common.menu.ResearchTableMenu;
import thaumcraft.common.tiles.crafting.TileResearchTable;
import thaumcraft.init.ModSounds;

import java.util.*;

/**
 * ResearchTableScreen - Full theorycraft minigame GUI for the Research Table.
 * 
 * This screen implements the card-based research minigame where players:
 * 1. Select aids from nearby blocks to add special cards
 * 2. Draw cards from a deck
 * 3. Select cards to play, spending inspiration
 * 4. Gain progress in research categories
 * 5. Complete the theory when inspiration runs out
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    
    // Textures
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_research_table.png");
    private static final Identifier TX_BASE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    private static final Identifier TX_PAPER = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/paper.png");
    private static final Identifier TX_PAPER_GILDED = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/papergilded.png");
    private static final Identifier TX_QUESTION = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/aspects/_unknown.png");
    
    // Buttons
    private Button buttonCreate;
    private Button buttonComplete;
    private Button buttonScrap;
    
    // Animation state for cards
    private float[] cardHover = new float[] { 0.0f, 0.0f, 0.0f };
    private float[] cardZoomOut = new float[] { 0.0f, 0.0f, 0.0f };
    private float[] cardZoomIn = new float[] { 0.0f, 0.0f, 0.0f };
    private boolean[] cardActive = new boolean[] { true, true, true };
    private boolean cardSelected = false;
    
    // Category display animation
    private HashMap<String, Integer> tempCatTotals = new HashMap<>();
    private long nextCatCheck = 0L;
    
    // Aid selection (pre-game)
    private Set<String> currentAids = new HashSet<>();
    private Set<String> selectedAids = new HashSet<>();
    private long nextAidCheck = 0L;
    private int dummyInspirationStart = 0;
    
    // Card tracking
    private ArrayList<ResearchTableData.CardChoice> cardChoices = new ArrayList<>();
    private ResearchTableData.CardChoice lastDraw = null;
    
    // Reference to tile entity
    private final TileResearchTable table;
    private final Player player;
    
    public ResearchTableScreen(ResearchTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 255, 255);
        this.table = menu.getBlockEntity();
        this.player = playerInventory.player;
        
        // Initialize from existing data
        if (table.data != null) {
            for (String cat : table.data.categoryTotals.keySet()) {
                tempCatTotals.put(cat, table.data.categoryTotals.get(cat));
            }
            syncFromTableChoices();
            lastDraw = table.data.lastDraw;
        }
    }
    
    private void syncFromTableChoices() {
        cardChoices.clear();
        if (table.data != null) {
            for (ResearchTableData.CardChoice cc : table.data.cardChoices) {
                cardChoices.add(cc);
            }
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        int x = this.leftPos;
        int y = this.topPos;
        
        // Create Theory button (shown when no active theory)
        buttonCreate = addRenderableWidget(Button.builder(
                Component.translatable("button.create.theory"), 
                btn -> onCreateTheory())
                .bounds(x + 103, y + 22, 50, 12)
                .build());
        
        // Complete Theory button (shown when theory complete)
        buttonComplete = addRenderableWidget(Button.builder(
                Component.translatable("button.complete.theory"),
                btn -> onCompleteTheory())
                .bounds(x + 166, y + 96, 50, 12)
                .build());
        
        // Scrap Theory button (shown during active theory)
        buttonScrap = addRenderableWidget(Button.builder(
                Component.translatable("button.scrap.theory"),
                btn -> onScrapTheory())
                .bounds(x + 103, y + 168, 50, 12)
                .build());
        
        updateButtonStates();
    }
    
    private void onCreateTheory() {
        playButtonClick();
        // Send packet to server to start theory with selected aids
        PacketHandler.sendToServer(new PacketStartTheoryToServer(table.getBlockPos(), selectedAids));
        selectedAids.clear();
    }
    
    private void onCompleteTheory() {
        playButtonClick();
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
        }
        tempCatTotals.clear();
        lastDraw = null;
    }
    
    private void onScrapTheory() {
        playButtonClick();
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
        }
        tempCatTotals.clear();
        lastDraw = null;
        cardChoices.clear();
    }
    
    private void updateButtonStates() {
        boolean hasTheory = table.data != null;
        boolean hasTools = menu.hasScribingTools();
        boolean hasPaper = menu.hasPaper();
        
        // Create button visible when no theory
        buttonCreate.visible = !hasTheory;
        buttonCreate.active = !hasTheory && hasTools && hasPaper;
        
        // Complete button visible when theory is complete
        buttonComplete.visible = hasTheory && table.data != null && table.data.isComplete();
        buttonComplete.active = buttonComplete.visible;
        
        // Scrap button visible during active (incomplete) theory
        buttonScrap.visible = hasTheory && table.data != null && !table.data.isComplete();
        buttonScrap.active = buttonScrap.visible;
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateButtonStates();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        
        // Draw overlays that go on top
        renderOverlay(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int xx = this.leftPos;
        int yy = this.topPos;
        
        // Draw main background
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xx, yy, 0.0F, 0.0F, 255, 255, 256, 256);
        
        if (table.data == null) {
            // Pre-game: show aids and dummy inspiration
            renderPreGameState(graphics, mouseX, mouseY, partialTick);
        } else {
            // Active game: show cards and progress
            renderActiveGameState(graphics, mouseX, mouseY, partialTick);
        }
    }
    
    /**
     * Render the pre-game state showing available aids and inspiration preview.
     */
    private void renderPreGameState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int xx = leftPos;
        int yy = topPos;
        
        // Check for nearby aids periodically
        if (nextAidCheck < player.tickCount) {
            currentAids = table.checkSurroundingAids();
            dummyInspirationStart = ResearchTableData.getAvailableInspiration(player);
            nextAidCheck = player.tickCount + 100;
        }
        
        // Draw inspiration preview (hearts)
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(xx + 128 - dummyInspirationStart * 5, yy + 55);
        pose.scale(0.5f, 0.5f);
        
        for (int a = 0; a < dummyInspirationStart; a++) {
            // Hearts that will be used by aids are shown as empty
            int texU = (dummyInspirationStart - selectedAids.size() <= a) ? 48 : 32;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TX_BASE, 20 * a, 0, texU, 96.0F, 16, 16, 256, 256);
        }
        pose.popMatrix();
        
        // Draw available aids
        if (!currentAids.isEmpty()) {
            int side = Math.min(currentAids.size(), 6);
            int c = 0;
            int r = 0;
            
            for (String key : currentAids) {
                ITheorycraftAid aid = TheorycraftManager.aids.get(key);
                if (aid == null) continue;
                
                int x = xx + 128 + 20 * c - side * 10;
                int y = yy + 85 + 35 * r;
                
                // Draw selection highlight
                if (selectedAids.contains(key)) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, TX_BASE, x, y, 0.0F, 96.0F, 16, 16, 256, 256);
                }
                
                // Draw aid icon
                Object aidObj = aid.getAidObject();
                if (aidObj instanceof ItemStack stack) {
                    graphics.item(stack, x, y);
                } else if (aidObj instanceof Block block) {
                    graphics.item(new ItemStack(block), x, y);
                }
                
                if (++c >= side) {
                    r++;
                    c = 0;
                }
            }
        }
    }
    
    /**
     * Render the active game state with cards, inspiration, and category progress.
     */
    private void renderActiveGameState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int xx = leftPos;
        int yy = topPos;
        ResearchTableData data = table.data;
        
        checkCards();
        
        // Draw bonus draw indicator
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(xx + 15, yy + 150);
        for (int a = 0; a < data.bonusDraws; a++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TX_BASE, a * 2, a, 64.0F, 96.0F, 16, 16, 256, 256);
        }
        pose.popMatrix();
        
        // Draw inspiration hearts
        pose.pushMatrix();
        pose.translate(xx + 128 - data.inspirationStart * 5, yy + 16);
        pose.scale(0.5f, 0.5f);
        for (int a = 0; a < data.inspirationStart; a++) {
            int texU = (data.inspiration <= a) ? 48 : 32;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TX_BASE, 20 * a, 0, texU, 96.0F, 16, 16, 256, 256);
        }
        pose.popMatrix();
        
        // Draw paper stack (question mark area for drawing cards)
        int sheets = 0;
        ItemStack paperStack = table.getItem(TileResearchTable.SLOT_PAPER);
        if (!paperStack.isEmpty()) {
            sheets = 1 + paperStack.getCount() / 4;
        }
        
        Random r = new Random(55L);
        if (sheets > 0 && !data.isComplete()) {
            // Draw paper sheets
            for (int a = 0; a < sheets; a++) {
                drawSheet(graphics, xx + 65, yy + 100, 6.0f, r, 1.0f, 1.0f, null);
            }
            
            // Draw question mark (clickable area to draw cards)
            boolean highlight = false;
            int var7 = mouseX - (25 + xx);
            int var8 = mouseY - (55 + yy);
            if (cardChoices.isEmpty() && var7 >= 0 && var8 >= 0 && var7 < 75 && var8 < 90) {
                highlight = true;
            }
            
            pose.pushMatrix();
            pose.translate(xx + 65, yy + 100);
            float scale = highlight ? 1.75f : 1.5f;
            pose.scale(scale, scale);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TX_QUESTION, -8, -8, 0.0F, 0.0F, 16, 16, 16, 16,
                    ARGB.white(highlight ? 1.0f : 0.5f));
            pose.popMatrix();
        }
        
        // Draw saved cards pile
        for (Long seed : data.savedCards) {
            r = new Random(seed);
            drawSheet(graphics, xx + 191, yy + 100, 6.0f, r, 1.0f, 1.0f, null);
        }
        
        // Draw last played card on saved pile
        if (lastDraw != null) {
            r = new Random(lastDraw.card.getSeed());
            drawSheet(graphics, xx + 191, yy + 100, 6.0f, r, 1.0f, 1.0f, lastDraw);
        }
        
        // Animate category totals
        updateCategoryTotals();
        
        // Draw category progress on the right side
        drawCategoryProgress(graphics, mouseX, mouseY);
        
        // Draw card choices with animation
        drawCardChoices(graphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * Draw a paper sheet with optional card content.
     */
    private void drawSheet(GuiGraphicsExtractor graphics, float x, float y, float scale, Random r, 
                          float alpha, float tilt, ResearchTableData.CardChoice cardChoice) {
        var pose = graphics.pose();
        pose.pushMatrix();
        
        pose.translate((float)(x + r.nextGaussian()), (float)(y + r.nextGaussian()));
        pose.scale(scale, scale);
        
        // Random rotation
        float rotation = (float)(r.nextGaussian() * tilt);
        pose.rotate((float)Math.toRadians(rotation));
        
        pose.pushMatrix();
        // Select paper texture
        Identifier paperTex = (cardChoice != null && cardChoice.fromAid) ? TX_PAPER_GILDED : TX_PAPER;
        
        // Random flip
        if (r.nextBoolean()) {
            pose.rotate((float)Math.PI);
        }
        if (r.nextBoolean()) {
            pose.scale(-1.0f, 1.0f);
        }
        
        // Draw paper texture
        graphics.blit(RenderPipelines.GUI_TEXTURED, paperTex, -8, -8, 0.0F, 0.0F, 16, 16, 16, 16, ARGB.white(alpha));
        pose.popMatrix();
        
        // Draw card content
        if (cardChoice != null && alpha == 1.0f) {
            drawCardContent(graphics, cardChoice);
        }
        
        pose.popMatrix();
    }
    
    /**
     * Draw card content (category icon, name, text, cost, items).
     */
    private void drawCardContent(GuiGraphicsExtractor graphics, ResearchTableData.CardChoice cardChoice) {
        var pose = graphics.pose();
        TheorycraftCard card = cardChoice.card;
        
        // Draw category icon as watermark
        if (card.getResearchCategory() != null) {
            ResearchCategory rc = ResearchCategories.getResearchCategory(card.getResearchCategory());
            if (rc != null && rc.icon != null) {
                pose.pushMatrix();
                pose.scale(0.5f, 0.5f);
                graphics.blit(RenderPipelines.GUI_TEXTURED, rc.icon, -8, -8, 0.0F, 0.0F, 16, 16, 16, 16,
                        ARGB.white(1.0f / 6.0f));
                pose.popMatrix();
            }
        }
        
        // Draw card name
        pose.pushMatrix();
        pose.scale(0.0625f, 0.0625f);
        String name = Component.translatable(card.getLocalizedName()).getString();
        int nameWidth = font.width(name);
        graphics.text(font, name, -nameWidth / 2, -65, 0, false);
        
        // Draw card text (split into lines)
        String text = Component.translatable(card.getLocalizedText()).getString();
        drawSplitString(graphics, text, -70, -48, 140, 0);
        pose.popMatrix();
        
        // Draw inspiration cost (hearts)
        pose.pushMatrix();
        pose.scale(0.0625f, 0.0625f);
        int cost = card.getInspirationCost();
        boolean isGain = cost < 0;
        int hearts = isGain ? Math.abs(cost) + 1 : cost;
        
        for (int a = 0; a < hearts; a++) {
            int texU = (a == 0 && isGain) ? 48 : 32; // First heart green if gain
            graphics.blit(RenderPipelines.GUI_TEXTURED, TX_BASE, -10 * hearts + 20 * a, -95, texU, (isGain && a == 0) ? 0 : 96, 16, 16, 256, 256);
        }
        pose.popMatrix();
        
        // Draw required items
        ItemStack[] items = card.getRequiredItems();
        if (items != null) {
            pose.pushMatrix();
            for (int a = 0; a < items.length; a++) {
                pose.pushMatrix();
                pose.scale(0.125f, 0.125f);
                
                if (items[a] == null || items[a].isEmpty()) {
                    // Draw question mark for unknown items
                    pose.translate(-9 * items.length + 18 * a, 35);
                    graphics.blit(RenderPipelines.GUI_TEXTURED, TX_QUESTION, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16,
                            ARGB.colorFromFloat(1.0f, 0.75f, 0.75f, 0.75f));
                } else {
                    // Draw item
                    graphics.item(items[a], -9 * items.length + 18 * a, 35);
                    
                    // Draw fire icon if consumed
                    try {
                        if (card.getRequiredItemsConsumed()[a]) {
                            pose.pushMatrix();
                            pose.translate(-2 - 9 * items.length + 18 * a, 45);
                            pose.scale(0.5f, 0.5f);
                            graphics.blit(RenderPipelines.GUI_TEXTURED, TX_BASE, 0, 0, 64.0F, 120.0F, 16, 16, 256, 256);
                            pose.popMatrix();
                        }
                    } catch (Exception ignored) {}
                }
                pose.popMatrix();
            }
            pose.popMatrix();
        }
    }
    
    private void drawSplitString(GuiGraphicsExtractor graphics, String text, int x, int y, int width, int color) {
        // Simple word wrap
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : text.split(" ")) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            if (font.width(testLine) > width) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(font, lines.get(i), x, y + i * 10, color, false);
        }
    }
    
    /**
     * Update category totals with animation.
     */
    private void updateCategoryTotals() {
        if (table.data == null) return;
        
        if (nextCatCheck < player.tickCount) {
            for (String cat : ResearchCategories.researchCategories.keySet()) {
                int actual = table.data.categoryTotals.getOrDefault(cat, 0);
                int display = tempCatTotals.getOrDefault(cat, 0);
                
                if (actual == 0 && display == 0) {
                    tempCatTotals.remove(cat);
                } else {
                    if (display > actual) display--;
                    if (display < actual) display++;
                    tempCatTotals.put(cat, display);
                }
            }
            nextCatCheck = player.tickCount + 1;
        }
    }
    
    /**
     * Draw category progress on the right side.
     */
    private void drawCategoryProgress(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int xx = leftPos;
        int yy = topPos;
        ResearchTableData data = table.data;
        
        // Sort by total (highest first)
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(tempCatTotals.entrySet());
        sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        sorted.removeIf(e -> e.getValue() <= 0);
        
        int i = 0;
        for (Map.Entry<String, Integer> entry : sorted) {
            String cat = entry.getKey();
            int total = entry.getValue();
            
            var pose = graphics.pose();
            pose.pushMatrix();
            
            // Draw category icon
            pose.translate(xx + 253, yy + 16 + i * 18 + (i > 0 ? 4 : 0));
            pose.scale(0.0625f, 0.0625f);
            
            ResearchCategory rc = ResearchCategories.getResearchCategory(cat);
            if (rc != null && rc.icon != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, rc.icon, 0, 0, 0.0F, 0.0F, 255, 255, 256, 256);
            }
            pose.popMatrix();
            
            // Draw percentage text
            String s = total + "%";
            if (i > data.penaltyStart) {
                int penalty = total / 3;
                s = s + " (-" + penalty + ")";
            }
            
            int color = data.categoriesBlocked.contains(cat) ? 0x606060 : 
                       (i <= data.penaltyStart ? 0x00E100 : 0xFFFFFF);
            
            graphics.text(font, s, xx + 276, yy + 20 + i * 18 + (i > data.penaltyStart ? 4 : 0), color, true);
            
            // Tooltip on hover
            int var9 = mouseX - (xx + 256);
            int var10 = mouseY - (yy + 16 + i * 18 + (i > data.penaltyStart ? 4 : 0));
            if (var9 >= 0 && var10 >= 0 && var9 < 16 && var10 < 16) {
                graphics.setTooltipForNextFrame(font, ResearchCategories.getCategoryName(cat), mouseX, mouseY);
            }
            
            i++;
            if (i > 8) break; // Limit display
        }
    }
    
    /**
     * Draw card choices with animation.
     */
    private void drawCardChoices(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int xx = leftPos;
        int yy = topPos;
        int sx = 128;
        int cw = 110;
        int sz = cardChoices.size();
        
        int a = 0;
        for (ResearchTableData.CardChoice cardChoice : cardChoices) {
            Random r = new Random(cardChoice.card.getSeed());
            
            // Calculate hover state
            int var11 = mouseX - (5 + sx - 55 * sz + xx + cw * a);
            int var12 = mouseY - (100 + yy - 60);
            
            if (cardZoomOut[a] >= 0.95f && !cardSelected) {
                if (var11 >= 0 && var12 >= 0 && var11 < 100 && var12 < 120) {
                    cardHover[a] += Math.max((0.25f - cardHover[a]) / 3.0f * partialTick, 0.0025f);
                } else {
                    cardHover[a] -= 0.1f * partialTick;
                }
            }
            
            // Animate zoom out (card appearing from deck)
            if (a == sz - 1 || cardZoomOut[a + 1] > 0.6f) {
                float prev = cardZoomOut[a];
                cardZoomOut[a] += Math.max((1.0f - cardZoomOut[a]) / 5.0f * partialTick, 0.0025f);
                if (cardZoomOut[a] > 0.0f && prev == 0.0f) {
                    playPageFlip();
                }
            }
            
            // Animate zoom in (card being selected)
            float prevZoomIn = cardZoomIn[a];
            if (cardSelected) {
                cardZoomIn[a] += (float)(cardActive[a] ? 
                    Math.max((1.0f - cardZoomIn[a]) / 3.0f * partialTick, 0.0025) : 
                    0.3f * partialTick);
                cardHover[a] = 1.0f - cardZoomIn[a];
            }
            
            // Clamp values
            cardZoomIn[a] = Mth.clamp(cardZoomIn[a], 0.0f, 1.0f);
            cardHover[a] = Mth.clamp(cardHover[a], 0.0f, 0.25f);
            cardZoomOut[a] = Mth.clamp(cardZoomOut[a], 0.0f, 1.0f);
            
            // Calculate position
            float dx = 55 + sx - 55 * sz + xx + cw * a - (xx + 65);
            float fx = xx + 65 + dx * cardZoomOut[a];
            float qx = xx + 191 - fx;
            if (cardActive[a]) {
                fx += qx * cardZoomIn[a];
            }
            
            // Draw the card
            float alpha = cardActive[a] ? 1.0f : (1.0f - cardZoomIn[a]);
            float tilt = Math.max(1.0f - cardZoomOut[a], cardZoomIn[a]);
            drawSheet(graphics, fx, yy + 100, 
                     6.0f + cardZoomOut[a] * 2.0f - cardZoomIn[a] * 2.0f + cardHover[a],
                     r, alpha, tilt, cardChoice);
            
            // Complete animation
            if (cardSelected && cardActive[a] && cardZoomIn[a] >= 1.0f && prevZoomIn < 1.0f) {
                playWrite();
                cardChoices.clear();
                cardSelected = false;
                lastDraw = table.data != null ? table.data.lastDraw : null;
                break;
            }
            
            a++;
        }
    }
    
    /**
     * Render overlay elements (tooltips for cards).
     */
    private void renderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int xx = leftPos;
        int yy = topPos;
        
        if (table.data == null) {
            // Aid hover highlights
            if (!currentAids.isEmpty()) {
                int side = Math.min(currentAids.size(), 6);
                int c = 0;
                int r = 0;
                
                for (String key : currentAids) {
                    ITheorycraftAid aid = TheorycraftManager.aids.get(key);
                    if (aid == null) continue;
                    
                    int x = xx + 128 + 20 * c - side * 10;
                    int y = yy + 85 + 35 * r;
                    
                    if (isHovering(x - xx, y - yy, 16, 16, mouseX, mouseY) && !selectedAids.contains(key)) {
                        graphics.blit(RenderPipelines.GUI_TEXTURED, TX_BASE, x, y, 0.0F, 96.0F, 16, 16, 256, 256,
                                ARGB.white(0.2f));
                    }
                    
                    if (++c >= side) {
                        r++;
                        c = 0;
                    }
                }
            }
        } else {
            // Card item tooltips
            int sx = 128;
            int cw = 110;
            int sz = cardChoices.size();
            int a = 0;
            
            if (!cardSelected) {
                for (ResearchTableData.CardChoice cardChoice : cardChoices) {
                    if (cardZoomOut[a] >= 1.0f) {
                        float dx = 55 + sx - 55 * sz + cw * a - 65;
                        float fx = 65 + dx * cardZoomOut[a];
                        float qx = 191 - fx;
                        if (cardActive[a]) {
                            fx += qx * cardZoomIn[a];
                        }
                        drawSheetOverlay(graphics, fx, 100, cardChoice, mouseX, mouseY);
                    }
                    a++;
                }
            }
            
            // No ink warning
            ItemStack tools = table.getItem(TileResearchTable.SLOT_SCRIBING_TOOLS);
            if (tools.isEmpty() || tools.getDamageValue() >= tools.getMaxDamage()) {
                List<Component> tooltip = List.of(
                    Component.translatable("tile.researchtable.noink.0"),
                    Component.translatable("tile.researchtable.noink.1")
                );
                graphics.setTooltipForNextFrame(font, tooltip, java.util.Optional.empty(), xx + 100, yy + 60);
            }
            
            // No paper warning
            ItemStack paper = table.getItem(TileResearchTable.SLOT_PAPER);
            if (paper.isEmpty()) {
                graphics.setTooltipForNextFrame(font, Component.translatable("tile.researchtable.nopaper.0"), xx + 100, yy + 100);
            }
        }
    }
    
    /**
     * Draw item tooltips for card requirements.
     */
    private void drawSheetOverlay(GuiGraphicsExtractor graphics, float x, float y, 
                                  ResearchTableData.CardChoice cardChoice, int mouseX, int mouseY) {
        if (cardChoice == null || cardChoice.card.getRequiredItems() == null) return;
        
        ItemStack[] items = cardChoice.card.getRequiredItems();
        for (int a = 0; a < items.length; a++) {
            int itemX = (int)(x - 9 * items.length + 18 * a);
            int itemY = (int)(y + 36);
            
            if (isHovering(itemX, itemY, 15, 15, mouseX + leftPos, mouseY + topPos)) {
                if (items[a] == null || items[a].isEmpty()) {
                    graphics.setTooltipForNextFrame(font, Component.translatable("tc.card.unknown"), mouseX, mouseY);
                } else {
                    graphics.setTooltipForNextFrame(font, items[a], mouseX, mouseY);
                }
            }
        }
    }
    
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Don't draw default labels
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        int xx = leftPos;
        int yy = topPos;
        int mx = (int) mouseX;
        int my = (int) mouseY;
        
        if (table.data == null) {
            // Aid selection
            if (!currentAids.isEmpty()) {
                int side = Math.min(currentAids.size(), 6);
                int c = 0;
                int r = 0;
                
                for (String key : currentAids) {
                    ITheorycraftAid aid = TheorycraftManager.aids.get(key);
                    if (aid == null) continue;
                    
                    int x = 128 + 20 * c - side * 10;
                    int y = 85 + 35 * r;
                    
                    if (isHovering(x, y, 16, 16, mx, my)) {
                        if (selectedAids.contains(key)) {
                            selectedAids.remove(key);
                        } else if (selectedAids.size() + 1 < dummyInspirationStart) {
                            selectedAids.add(key);
                        }
                        return true;
                    }
                    
                    if (++c >= side) {
                        r++;
                        c = 0;
                    }
                }
            }
        } else {
            // Card selection or draw
            int sx = 128;
            int cw = 110;
            
            if (!cardChoices.isEmpty()) {
                // Check card clicks
                int pressed = -1;
                for (int a = 0; a < cardChoices.size(); a++) {
                    int var7 = mx - (5 + sx - 55 * cardChoices.size() + xx + cw * a);
                    int var8 = my - (100 + yy - 60);
                    
                    if (cardZoomOut[a] >= 0.95f && !cardSelected && 
                        var7 >= 0 && var8 >= 0 && var7 < 100 && var8 < 120) {
                        pressed = a;
                        break;
                    }
                }
                
                if (pressed >= 0) {
                    ItemStack tools = table.getItem(TileResearchTable.SLOT_SCRIBING_TOOLS);
                    if (!tools.isEmpty() && tools.getDamageValue() < tools.getMaxDamage()) {
                        // Send card selection to server
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 10 + pressed);
                        }
                        return true;
                    }
                }
            } else {
                // Check draw click (question mark area)
                int var9 = mx - (25 + xx);
                int var10 = my - (55 + yy);
                
                if (var9 >= 0 && var10 >= 0 && var9 < 75 && var10 < 90) {
                    ItemStack paper = table.getItem(TileResearchTable.SLOT_PAPER);
                    if (!paper.isEmpty()) {
                        drawCards();
                        return true;
                    }
                }
            }
        }
        
        return super.mouseClicked(event, doubleClick);
    }
    
    /**
     * Request new cards to be drawn.
     */
    private void drawCards() {
        cardSelected = false;
        cardHover = new float[] { 0.0f, 0.0f, 0.0f };
        cardZoomOut = new float[] { 0.0f, 0.0f, 0.0f };
        cardZoomIn = new float[] { 0.0f, 0.0f, 0.0f };
        cardActive = new boolean[] { true, true, true };
        
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 20);
        }
        cardChoices.clear();
    }
    
    /**
     * Check for card selection updates from server.
     */
    private void checkCards() {
        if (table.data == null) return;
        
        if (table.data.cardChoices.size() > 0 && cardChoices.isEmpty()) {
            syncFromTableChoices();
        }
        
        if (!cardSelected) {
            for (int a = 0; a < cardChoices.size(); a++) {
                try {
                    if (table.data.cardChoices.size() > a && table.data.cardChoices.get(a).selected) {
                        for (int q = 0; q < cardChoices.size(); q++) {
                            cardActive[q] = table.data.cardChoices.get(q).selected;
                        }
                        cardSelected = true;
                        playPageSelect();
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonStates();
    }
    
    // Sound helpers
    private void playPageFlip() {
        if (player != null) {
            player.playSound(ModSounds.PAGE.get(), 1.0f, 1.0f);
        }
    }
    
    private void playPageSelect() {
        if (player != null) {
            player.playSound(ModSounds.PAGE_TURN.get(), 1.0f, 1.0f);
        }
    }
    
    private void playButtonClick() {
        if (player != null) {
            player.playSound(ModSounds.CLACK.get(), 0.4f, 1.0f);
        }
    }
    
    private void playWrite() {
        if (player != null) {
            player.playSound(ModSounds.WRITE.get(), 0.3f, 1.0f);
        }
    }
}
