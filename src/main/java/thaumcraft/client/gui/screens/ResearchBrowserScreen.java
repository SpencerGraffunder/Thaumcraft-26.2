package thaumcraft.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncResearchFlagsToServer;

import java.util.Arrays;
import thaumcraft.common.lib.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.lib.research.ResearchManager;

import java.util.*;

/**
 * ResearchBrowserScreen - The main Thaumonomicon research browser GUI.
 * 
 * Displays research categories as tabs and research entries as a scrollable/zoomable tree.
 * Players can navigate between categories, search for research, and click entries to view details.
 * 
 * Ported from 1.12.2 GuiResearchBrowser to 1.20.1
 */
@OnlyIn(Dist.CLIENT)
public class ResearchBrowserScreen extends Screen {
    
    // Textures
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_research_browser.png");
    
    // Map bounds (calculated from visible research)
    private static int guiBoundsLeft;
    private static int guiBoundsTop;
    private static int guiBoundsRight;
    private static int guiBoundsBottom;
    
    // Mouse tracking for dragging
    protected int lastMouseX;
    protected int lastMouseY;
    protected float screenZoom = 1.0f;
    
    // Map position (scrolling)
    protected double curMouseX;
    protected double curMouseY;
    protected double guiMapX;
    protected double guiMapY;
    protected double tempMapX;
    protected double tempMapY;
    private boolean isDragging = false;
    
    // Persistent position between GUI opens
    public static double lastX = -9999.0;
    public static double lastY = -9999.0;
    
    // Screen dimensions
    private int screenX;
    private int screenY;
    private final int startX = 16;
    private final int startY = 16;
    
    // Research data
    private LinkedList<ResearchEntry> research = new LinkedList<>();
    static String selectedCategory;
    private ResearchEntry currentHighlight;
    private Player player;
    
    // Popup message
    long popuptime = 0L;
    String popupmessage = "";
    
    // Search
    private EditBox searchField;
    private static boolean searching = false;
    private ArrayList<String> categoriesTC = new ArrayList<>();
    private ArrayList<String> categoriesOther = new ArrayList<>();
    static int catScrollPos = 0;
    static int catScrollMax = 0;
    public int addonShift = 0;
    
    // Visibility cache
    private ArrayList<String> invisible = new ArrayList<>();
    ArrayList<Pair<String, SearchResult>> searchResults = new ArrayList<>();
    
    // Animation time
    long t = 0L;
    
    public ResearchBrowserScreen() {
        super(Component.translatable("gui.thaumcraft.thaumonomicon"));
        initPositions(lastX, lastY);
    }
    
    public ResearchBrowserScreen(double x, double y) {
        super(Component.translatable("gui.thaumcraft.thaumonomicon"));
        initPositions(x, y);
    }
    
    private void initPositions(double x, double y) {
        tempMapX = x;
        guiMapX = x;
        curMouseX = x;
        tempMapY = y;
        guiMapY = y;
        curMouseY = y;
        player = Minecraft.getInstance().player;
    }
    
    @Override
    protected void init() {
        super.init();
        updateResearch();
    }
    
    /**
     * Rebuilds the research display based on current category and player knowledge.
     */
    public void updateResearch() {
        clearWidgets();
        
        // Add search button
        addRenderableWidget(Button.builder(Component.translatable("tc.search"), b -> toggleSearch())
                .bounds(1, height - 17, 16, 16)
                .build());
        
        // Create search field
        searchField = new EditBox(font, 20, 20, 89, font.lineHeight, Component.empty());
        searchField.setMaxLength(15);
        searchField.setBordered(true);
        searchField.setVisible(false);
        searchField.setTextColor(0xFFFFFF);
        
        if (searching) {
            searchField.setVisible(true);
            searchField.setFocused(true);
            searchField.setValue("");
            updateSearch();
        }
        
        // Calculate screen area
        screenX = width - 32;
        screenY = height - 32;
        
        // Clear and rebuild research list
        research.clear();
        
        // Select first category if none selected
        if (selectedCategory == null || selectedCategory.isEmpty()) {
            Collection<String> cats = ResearchCategories.researchCategories.keySet();
            if (!cats.isEmpty()) {
                selectedCategory = cats.iterator().next();
            }
        }
        
        // Calculate how many category buttons can fit
        int limit = (int) Math.floor((screenY - 28) / 24.0f);
        addonShift = 0;
        int count = 0;
        categoriesTC.clear();
        categoriesOther.clear();
        
        // Build category lists
        for (String rcl : ResearchCategories.researchCategories.keySet()) {
            ResearchCategory rc = ResearchCategories.getResearchCategory(rcl);
            if (rc == null) continue;
            
            // Check if category is visible (has required research)
            if (rc.researchKey != null && !ThaumcraftCapabilities.isResearchComplete(player, rc.researchKey)) {
                continue;
            }
            
            // Calculate completion percentage
            int total = 0;
            int complete = 0;
            for (ResearchEntry res : rc.research.values()) {
                if (res.hasMeta(ResearchEntry.EnumResearchMeta.AUTOUNLOCK)) {
                    continue;
                }
                total++;
                if (ThaumcraftCapabilities.isResearchKnown(player, res.getKey())) {
                    complete++;
                }
            }
            int completion = total > 0 ? (int) (complete / (float) total * 100.0f) : 0;
            
            // Check if this is a Thaumcraft core category
            boolean isTC = false;
            for (String tcc : ConfigResearch.TC_CATEGORIES) {
                if (tcc.equals(rcl)) {
                    isTC = true;
                    break;
                }
            }
            
            if (isTC) {
                categoriesTC.add(rcl);
                // Add TC category button on left side
                int yPos = 10 + categoriesTC.size() * 24;
                addRenderableWidget(new CategoryButton(rc, rcl, false, 1, yPos, completion));
            } else {
                count++;
                if (count > limit + catScrollPos) continue;
                if (count - 1 < catScrollPos) continue;
                
                categoriesOther.add(rcl);
                // Add addon category button on right side
                int yPos = 10 + categoriesOther.size() * 24;
                addRenderableWidget(new CategoryButton(rc, rcl, true, width - 17, yPos, completion));
            }
        }
        
        // Add scroll buttons if needed
        if (count > limit || count < catScrollPos) {
            addonShift = (screenY - 28) % 24 / 2;
            addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollCategories(-1))
                    .bounds(width - 14, 20, 10, 11)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollCategories(1))
                    .bounds(width - 14, screenY + 1, 10, 11)
                    .build());
        }
        catScrollMax = count - limit;
        
        // Load research for selected category
        if (selectedCategory == null || selectedCategory.isEmpty()) {
            return;
        }
        
        ResearchCategory currentCat = ResearchCategories.getResearchCategory(selectedCategory);
        if (currentCat != null) {
            research.addAll(currentCat.research.values());
        }
        
        // Calculate map bounds
        guiBoundsLeft = 99999;
        guiBoundsTop = 99999;
        guiBoundsRight = -99999;
        guiBoundsBottom = -99999;
        
        for (ResearchEntry res : research) {
            if (res != null && isVisible(res)) {
                int col = res.getDisplayColumn() * 24;
                int row = res.getDisplayRow() * 24;
                
                if (col - screenX + 48 < guiBoundsLeft) {
                    guiBoundsLeft = col - screenX + 48;
                }
                if (col - 24 > guiBoundsRight) {
                    guiBoundsRight = col - 24;
                }
                if (row - screenY + 48 < guiBoundsTop) {
                    guiBoundsTop = row - screenY + 48;
                }
                if (row - 24 > guiBoundsBottom) {
                    guiBoundsBottom = row - 24;
                }
            }
        }
        
        // Center view if out of bounds
        if (lastX == -9999.0 || guiMapX > guiBoundsRight || guiMapX < guiBoundsLeft) {
            double centerX = (guiBoundsLeft + guiBoundsRight) / 2.0;
            tempMapX = centerX;
            guiMapX = centerX;
        }
        if (lastY == -9999.0 || guiMapY > guiBoundsBottom || guiMapY < guiBoundsTop) {
            double centerY = (guiBoundsTop + guiBoundsBottom) / 2.0;
            tempMapY = centerY;
            guiMapY = centerY;
        }
    }
    
    private void toggleSearch() {
        if (!searching) {
            selectedCategory = "";
            searching = true;
            searchField.setVisible(true);
            searchField.setFocused(true);
            searchField.setValue("");
            updateSearch();
        } else {
            searching = false;
            searchField.setVisible(false);
            searchField.setFocused(false);
            updateResearch();
        }
    }
    
    private void scrollCategories(int direction) {
        if (direction < 0 && catScrollPos > 0) {
            catScrollPos--;
            updateResearch();
        } else if (direction > 0 && catScrollPos < catScrollMax) {
            catScrollPos++;
            updateResearch();
        }
    }
    
    /**
     * Checks if a research entry should be visible to the player.
     */
    private boolean isVisible(ResearchEntry res) {
        if (ThaumcraftCapabilities.isResearchKnown(player, res.getKey())) {
            return true;
        }
        if (invisible.contains(res.getKey())) {
            return false;
        }
        if (res.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN) && !canUnlockResearch(res)) {
            return false;
        }
        if (res.getParents() == null && res.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN)) {
            return false;
        }
        if (res.getParents() != null) {
            for (String r : res.getParents()) {
                String cleanParent = r.startsWith("~") ? r.substring(1) : r;
                if (cleanParent.contains("@")) {
                    cleanParent = cleanParent.substring(0, cleanParent.indexOf("@"));
                }
                ResearchEntry ri = ResearchCategories.getResearch(cleanParent);
                if (ri != null && !isVisible(ri)) {
                    invisible.add(r);
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Checks if the player has all prerequisites to unlock research.
     */
    private boolean canUnlockResearch(ResearchEntry res) {
        return ResearchManager.doesPlayerHaveRequisites(player, res.getKey());
    }
    
    @Override
    public void onClose() {
        lastX = guiMapX;
        lastY = guiMapY;
        super.onClose();
    }
    
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searching && searchField.keyPressed(event)) {
            updateSearch();
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
    
    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searching && searchField.charTyped(event)) {
            updateSearch();
            return true;
        }
        return super.charTyped(event);
    }
    
    private void updateSearch() {
        searchResults.clear();
        invisible.clear();
        String searchText = searchField.getValue().toLowerCase();
        
        if (searchText.isEmpty()) {
            return;
        }
        
        // Search categories
        for (String cat : categoriesTC) {
            String catName = Component.translatable("tc.research_category." + cat).getString();
            if (catName.toLowerCase().contains(searchText)) {
                searchResults.add(Pair.of(catName, new SearchResult(cat, null, true)));
            }
        }
        for (String cat : categoriesOther) {
            String catName = Component.translatable("tc.research_category." + cat).getString();
            if (catName.toLowerCase().contains(searchText)) {
                searchResults.add(Pair.of(catName, new SearchResult(cat, null, true)));
            }
        }
        
        // Search known research
        ThaumcraftCapabilities.getKnowledge(player).ifPresent(knowledge -> {
            for (String resKey : knowledge.getResearchList()) {
                ResearchEntry ri = ResearchCategories.getResearch(resKey);
                if (ri != null) {
                    String name = ri.getLocalizedName().getString();
                    if (name.toLowerCase().contains(searchText)) {
                        searchResults.add(Pair.of(name, new SearchResult(resKey, null, false)));
                    }
                }
            }
        });
        
        // Sort results
        searchResults.sort(Comparator.comparing(Pair::getLeft));
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Handle dragging (when not searching)
        if (!searching) {
            handleDragging(mouseX, mouseY);
            handleZoom();
        }
        
        t = System.nanoTime() / 50000000L;
        
        // Calculate interpolated map position
        int locX = Mth.floor(curMouseX + (guiMapX - curMouseX) * partialTick);
        int locY = Mth.floor(curMouseY + (guiMapY - curMouseY) * partialTick);
        
        // Clamp to bounds
        if (locX < guiBoundsLeft * screenZoom) {
            locX = (int) (guiBoundsLeft * screenZoom);
        }
        if (locY < guiBoundsTop * screenZoom) {
            locY = (int) (guiBoundsTop * screenZoom);
        }
        if (locX >= guiBoundsRight * screenZoom) {
            locX = (int) (guiBoundsRight * screenZoom - 1.0f);
        }
        if (locY >= guiBoundsBottom * screenZoom) {
            locY = (int) (guiBoundsBottom * screenZoom - 1.0f);
        }
        
        // Draw the research map
        if (!searching) {
            renderResearchBackground(graphics, locX, locY);
            
            graphics.pose().pushMatrix();
            graphics.pose().scale(1.0f / screenZoom, 1.0f / screenZoom);
            renderResearchContent(graphics, mouseX, mouseY, locX, locY);
            graphics.pose().popMatrix();
        } else {
            // Draw search results
            renderSearchResults(graphics, mouseX, mouseY);
        }
        
        // Draw frame overlay
        renderFrame(graphics);
        
        // Draw widgets (buttons)
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        
        // Draw tooltip for highlighted research
        if (currentHighlight != null && !searching) {
            renderResearchTooltip(graphics, mouseX, mouseY);
        }
        
        // Draw popup message
        if (popuptime > System.currentTimeMillis()) {
            graphics.text(font, popupmessage, 10, 34, 0xFFFFFF);
        }
    }
    
    private void handleDragging(int mouseX, int mouseY) {
        if (minecraft.hasShiftDown() || isDragging) {
            if (isDragging) {
                guiMapX -= (mouseX - lastMouseX) * screenZoom;
                guiMapY -= (mouseY - lastMouseY) * screenZoom;
                curMouseX = guiMapX;
                tempMapX = guiMapX;
                curMouseY = guiMapY;
                tempMapY = guiMapY;
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        
        // Clamp position to bounds
        if (tempMapX < guiBoundsLeft * screenZoom) {
            tempMapX = guiBoundsLeft * screenZoom;
        }
        if (tempMapY < guiBoundsTop * screenZoom) {
            tempMapY = guiBoundsTop * screenZoom;
        }
        if (tempMapX >= guiBoundsRight * screenZoom) {
            tempMapX = guiBoundsRight * screenZoom - 1.0f;
        }
        if (tempMapY >= guiBoundsBottom * screenZoom) {
            tempMapY = guiBoundsBottom * screenZoom - 1.0f;
        }
    }
    
    private void handleZoom() {
        // Zoom is handled in mouseScrolled
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!searching) {
            if (scrollY < 0) {
                screenZoom += 0.25f;
            } else if (scrollY > 0) {
                screenZoom -= 0.25f;
            }
            screenZoom = Mth.clamp(screenZoom, 1.0f, 2.0f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && !searching) {
            isDragging = true;
            guiMapX -= dragX * screenZoom;
            guiMapY -= dragY * screenZoom;
            curMouseX = guiMapX;
            tempMapX = guiMapX;
            curMouseY = guiMapY;
            tempMapY = guiMapY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            isDragging = false;
        }
        return super.mouseReleased(event);
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        popuptime = System.currentTimeMillis() - 1L;
        
        if (!searching && currentHighlight != null) {
            // Handle research click
            if (!ThaumcraftCapabilities.isResearchKnown(player, currentHighlight.getKey()) 
                    && canUnlockResearch(currentHighlight)) {
                // Start new research - send packet to server to sync progress
                PacketHandler.sendToServer(new PacketSyncProgressToServer(currentHighlight.getKey(), true));
                minecraft.gui.setScreen(new ResearchPageScreen(currentHighlight, null, guiMapX, guiMapY));
                popuptime = System.currentTimeMillis() + 3000L;
                popupmessage = Component.translatable("tc.research.popup", currentHighlight.getLocalizedName()).getString();
                return true;
            } else if (ThaumcraftCapabilities.isResearchKnown(player, currentHighlight.getKey())) {
                // View existing research - clear flags and sync to server
                ThaumcraftCapabilities.getKnowledge(player).ifPresent(knowledge -> {
                    knowledge.clearResearchFlag(currentHighlight.getKey(), IPlayerKnowledge.EnumResearchFlag.RESEARCH);
                    knowledge.clearResearchFlag(currentHighlight.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE);
                });
                // Send flag sync packet to server
                PacketHandler.sendToServer(new PacketSyncResearchFlagsToServer(currentHighlight.getKey(), false, false, false));
                minecraft.gui.setScreen(new ResearchPageScreen(currentHighlight, null, guiMapX, guiMapY));
                return true;
            }
        } else if (searching) {
            // Handle search result click
            int q = 0;
            for (Pair<String, SearchResult> p : searchResults) {
                SearchResult sr = p.getRight();
                if (mouseX > 22 && mouseX < 18 + screenX 
                        && mouseY >= 32 + q * 10 && mouseY < 40 + q * 10) {
                    if (sr.cat) {
                        // Click on category
                        searching = false;
                        searchField.setVisible(false);
                        searchField.setFocused(false);
                        selectedCategory = sr.key;
                        updateResearch();
                        // Center on category
                        double centerX = (guiBoundsLeft + guiBoundsRight) / 2.0;
                        double centerY = (guiBoundsTop + guiBoundsBottom) / 2.0;
                        tempMapX = centerX;
                        guiMapX = centerX;
                        tempMapY = centerY;
                        guiMapY = centerY;
                        return true;
                    } else if (ThaumcraftCapabilities.isResearchKnown(player, sr.key)) {
                        // Click on research
                        ResearchEntry entry = ResearchCategories.getResearch(sr.key);
                        if (entry != null) {
                            minecraft.gui.setScreen(new ResearchPageScreen(entry, sr.recipe, guiMapX, guiMapY));
                            return true;
                        }
                    }
                }
                q++;
                if (32 + (q + 1) * 10 > screenY) {
                    break;
                }
            }
        }
        
        return super.mouseClicked(event, doubleClick);
    }
    
    @Override
    public void tick() {
        super.tick();
        curMouseX = guiMapX;
        curMouseY = guiMapY;
        
        // Smooth scrolling
        double dx = tempMapX - guiMapX;
        double dy = tempMapY - guiMapY;
        if (dx * dx + dy * dy < 4.0) {
            guiMapX += dx;
            guiMapY += dy;
        } else {
            guiMapX += dx * 0.85;
            guiMapY += dy * 0.85;
        }
    }
    
    /**
     * Renders the category background texture.
     */
    private void renderResearchBackground(GuiGraphicsExtractor graphics, int locX, int locY) {
        if (selectedCategory == null || selectedCategory.isEmpty()) return;
        
        ResearchCategory cat = ResearchCategories.getResearchCategory(selectedCategory);
        if (cat == null || cat.background == null) return;
        
        // Draw main background (tiled/scrolled)
        drawScrollingTexture(graphics, cat.background, startX - 2, startY - 2, screenX + 4, screenY + 4, locX / 2.0f, locY / 2.0f);
        
        // Draw foreground layer if present
        if (cat.background2 != null) {
            drawScrollingTexture(graphics, cat.background2, startX - 2, startY - 2, screenX + 4, screenY + 4, locX / 1.5f, locY / 1.5f);
        }
    }
    
    /**
     * Draws a scrolling/tiled texture.
     */
    private void drawScrollingTexture(GuiGraphicsExtractor graphics, Identifier texture, float x, float y, float width, float height, float scrollX, float scrollY) {
        float texScale = 1.0f / 256.0f;
        graphics.blit(texture, (int)x, (int)y, (int)(x + width), (int)(y + height),
                scrollX * texScale, (scrollX + width) * texScale, scrollY * texScale, (scrollY + height) * texScale);
    }
    
    /**
     * Renders the research entries and connections.
     */
    private void renderResearchContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int locX, int locY) {
        if (research.isEmpty()) return;
        
        // Draw connection lines between research
        for (ResearchEntry source : research) {
            if (source.getParents() != null && source.getParents().length > 0) {
                for (int a = 0; a < source.getParents().length; a++) {
                    String parentKey = source.getParents()[a];
                    if (parentKey == null) continue;
                    
                    String cleanParent = source.getParentsClean()[a];
                    ResearchEntry parent = ResearchCategories.getResearch(cleanParent);
                    if (parent == null || !parent.getCategory().equals(selectedCategory)) continue;
                    
                    // Skip if sibling connection
                    if (parent.getSiblings() != null && Arrays.asList(parent.getSiblings()).contains(source.getKey())) {
                        continue;
                    }
                    
                    boolean knowsParent = ThaumcraftCapabilities.isResearchComplete(player, parentKey);
                    boolean visible = isVisible(source) && !parentKey.startsWith("~");
                    
                    if (visible) {
                        if (knowsParent) {
                            drawLine(graphics, source.getDisplayColumn(), source.getDisplayRow(),
                                    parent.getDisplayColumn(), parent.getDisplayRow(),
                                    0.6f, 0.6f, 0.6f, locX, locY, true,
                                    source.hasMeta(ResearchEntry.EnumResearchMeta.REVERSE));
                        } else if (isVisible(parent)) {
                            drawLine(graphics, source.getDisplayColumn(), source.getDisplayRow(),
                                    parent.getDisplayColumn(), parent.getDisplayRow(),
                                    0.2f, 0.2f, 0.2f, locX, locY, true,
                                    source.hasMeta(ResearchEntry.EnumResearchMeta.REVERSE));
                        }
                    }
                }
            }
            
            // Draw sibling connections
            if (source.getSiblings() != null && source.getSiblings().length > 0) {
                for (String siblingKey : source.getSiblings()) {
                    if (siblingKey == null) continue;
                    
                    ResearchEntry sibling = ResearchCategories.getResearch(siblingKey);
                    if (sibling == null || !sibling.getCategory().equals(selectedCategory)) continue;
                    
                    boolean knowsSibling = ThaumcraftCapabilities.isResearchComplete(player, siblingKey);
                    if (isVisible(source) && !siblingKey.startsWith("~")) {
                        if (knowsSibling) {
                            drawLine(graphics, sibling.getDisplayColumn(), sibling.getDisplayRow(),
                                    source.getDisplayColumn(), source.getDisplayRow(),
                                    0.3f, 0.3f, 0.4f, locX, locY, false,
                                    source.hasMeta(ResearchEntry.EnumResearchMeta.REVERSE));
                        } else if (isVisible(sibling)) {
                            drawLine(graphics, sibling.getDisplayColumn(), sibling.getDisplayRow(),
                                    source.getDisplayColumn(), source.getDisplayRow(),
                                    0.1875f, 0.1875f, 0.25f, locX, locY, false,
                                    source.hasMeta(ResearchEntry.EnumResearchMeta.REVERSE));
                        }
                    }
                }
            }
        }
        
        // Draw research icons
        currentHighlight = null;
        
        for (ResearchEntry iconResearch : research) {
            int iconPosX = iconResearch.getDisplayColumn() * 24 - locX;
            int iconPosY = iconResearch.getDisplayRow() * 24 - locY;
            
            // Skip if off screen
            if (iconPosX < -24 || iconPosY < -24 || iconPosX > screenX * screenZoom || iconPosY > screenY * screenZoom) {
                continue;
            }
            
            int iconX = startX + iconPosX;
            int iconY = startY + iconPosY;
            
            if (isVisible(iconResearch)) {
                // Determine icon brightness based on research status
                float brightness = 0.3f;
                if (ThaumcraftCapabilities.isResearchComplete(player, iconResearch.getKey())) {
                    brightness = 1.0f;
                } else if (canUnlockResearch(iconResearch)) {
                    brightness = (float) Math.sin(System.currentTimeMillis() % 600L / 600.0 * Math.PI * 2.0) * 0.25f + 0.75f;
                }
                
                int frameColor = ARGB.colorFromFloat(1.0f, brightness, brightness, brightness);
                
                // Draw frame
                int frameU = 80;
                int frameV = 48;
                if (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN)) {
                    frameV += 32;
                }
                if (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.ROUND)) {
                    frameU = 144;
                } else if (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.HEX)) {
                    frameU = 112;
                }
                
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, iconX - 8, iconY - 8, frameU, frameV, 32, 32, 256, 256, frameColor);
                
                // Draw spiky overlay if needed
                if (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.SPIKY)) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, iconX - 8, iconY - 8, 176.0F, 
                            48 + (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN) ? 32 : 0), 32, 32, 256, 256, frameColor);
                }
                
                // Draw research/page flags
                ThaumcraftCapabilities.getKnowledge(player).ifPresent(knowledge -> {
                    if (knowledge.hasResearchFlag(iconResearch.getKey(), IPlayerKnowledge.EnumResearchFlag.RESEARCH)) {
                        graphics.pose().pushMatrix();
                        graphics.pose().translate(iconX - 9, iconY - 9);
                        graphics.pose().scale(0.5f);
                        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 176.0F, 16.0F, 32, 32, 256, 256);
                        graphics.pose().popMatrix();
                    }
                    if (knowledge.hasResearchFlag(iconResearch.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE)) {
                        graphics.pose().pushMatrix();
                        graphics.pose().translate(iconX - 9, iconY + 9);
                        graphics.pose().scale(0.5f);
                        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 208.0F, 16.0F, 32, 32, 256, 256);
                        graphics.pose().popMatrix();
                    }
                });
                
                // Draw icon
                float iconBrightness = canUnlockResearch(iconResearch) ? 1.0f : 0.1f;
                drawResearchIcon(graphics, iconResearch, iconX, iconY, ARGB.colorFromFloat(1.0f, iconBrightness, iconBrightness, iconBrightness));
                
                // Check for hover
                int scaledMouseX = (int) (mouseX * screenZoom);
                int scaledMouseY = (int) (mouseY * screenZoom);
                if (scaledMouseX >= startX * screenZoom && scaledMouseY >= startY * screenZoom
                        && scaledMouseX < (startX + screenX) * screenZoom && scaledMouseY < (startY + screenY) * screenZoom
                        && scaledMouseX >= iconX - 2 && scaledMouseX <= iconX + 18
                        && scaledMouseY >= iconY - 2 && scaledMouseY <= iconY + 18) {
                    currentHighlight = iconResearch;
                }
            }
        }
    }
    
    /**
     * Draws an icon for a research entry.
     */
    private void drawResearchIcon(GuiGraphicsExtractor graphics, ResearchEntry research, int x, int y, int color) {
        if (research.getIcons() == null || research.getIcons().length == 0) {
            return;
        }
        
        int idx = (int) (System.currentTimeMillis() / 1000L % research.getIcons().length);
        Object icon = research.getIcons()[idx];
        
        if (icon instanceof Identifier) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, (Identifier) icon, x, y, 0.0F, 0.0F, 16, 16, 16, 16, color);
        } else if (icon instanceof ItemStack) {
            graphics.item((ItemStack) icon, x, y);
        }
    }
    
    /**
     * Draws a connection line between two research entries.
     */
    private void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, 
                         float r, float g, float b, int locX, int locY, boolean arrow, boolean flipped) {
        // Simplified line drawing - draw rectangles for connections
        int startPosX, startPosY, endPosX, endPosY;
        if (flipped) {
            startPosX = x2 * 24 + 8 - locX + startX;
            startPosY = y2 * 24 + 8 - locY + startY;
            endPosX = x1 * 24 + 8 - locX + startX;
            endPosY = y1 * 24 + 8 - locY + startY;
        } else {
            startPosX = x1 * 24 + 8 - locX + startX;
            startPosY = y1 * 24 + 8 - locY + startY;
            endPosX = x2 * 24 + 8 - locX + startX;
            endPosY = y2 * 24 + 8 - locY + startY;
        }
        
        // Draw simple line using fill (horizontal then vertical, or vice versa)
        int color = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255) | 0xFF000000;
        
        // Draw vertical segment
        if (startPosY != endPosY) {
            int minY = Math.min(startPosY, endPosY);
            int maxY = Math.max(startPosY, endPosY);
            graphics.fill(startPosX - 1, minY, startPosX + 1, maxY, color);
        }
        
        // Draw horizontal segment
        if (startPosX != endPosX) {
            int minX = Math.min(startPosX, endPosX);
            int maxX = Math.max(startPosX, endPosX);
            graphics.fill(minX, endPosY - 1, maxX, endPosY + 1, color);
        }
    }
    
    /**
     * Renders the decorative frame around the research area.
     */
    private void renderFrame(GuiGraphicsExtractor graphics) {
        // Draw horizontal borders
        for (int c = 16; c < width - 16; c += 64) {
            int p = Math.min(64, width - 16 - c);
            if (p > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, c, -2, 48.0F, 13.0F, p, 22, 256, 256);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, c, height - 20, 48.0F, 13.0F, p, 22, 256, 256);
            }
        }
        
        // Draw vertical borders
        for (int c = 16; c < height - 16; c += 64) {
            int p = Math.min(64, height - 16 - c);
            if (p > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, -2, c, 13.0F, 48.0F, 22, p, 256, 256);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, width - 20, c, 13.0F, 48.0F, 22, p, 256, 256);
            }
        }
        
        // Draw corners
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, -2, -2, 13.0F, 13.0F, 22, 22, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, -2, height - 20, 13.0F, 13.0F, 22, 22, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, width - 20, -2, 13.0F, 13.0F, 22, 22, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, width - 20, height - 20, 13.0F, 13.0F, 22, 22, 256, 256);
    }
    
    /**
     * Renders search results list.
     */
    private void renderSearchResults(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        searchField.extractRenderState(graphics, mouseX, mouseY, 0);
        
        int q = 0;
        for (Pair<String, SearchResult> p : searchResults) {
            SearchResult sr = p.getRight();
            int color = sr.cat ? 0xDDCF9A : (sr.recipe == null ? 0xDDDDDD : 0xAAAAAA);
            
            // Highlight on hover
            if (mouseX > 22 && mouseX < 18 + screenX 
                    && mouseY >= 32 + q * 10 && mouseY < 40 + q * 10) {
                color = sr.recipe == null ? 0xFFFFFF : (sr.cat ? 0xFFDD6C : 0xCCFFFF);
            }
            
            // Draw recipe icon if applicable
            if (sr.recipe != null) {
                graphics.pose().pushMatrix();
                graphics.pose().scale(0.5f, 0.5f);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 44, (32 + q * 10) * 2, 224.0F, 48.0F, 16, 16, 256, 256);
                graphics.pose().popMatrix();
            }
            
            graphics.text(font, p.getLeft(), 32, 32 + q * 10, color);
            q++;
            
            if (32 + (q + 1) * 10 > screenY) {
                graphics.text(font, Component.translatable("tc.search.more").getString(), 22, 34 + q * 10, 0xAAAAAA);
                break;
            }
        }
    }
    
    /**
     * Renders tooltip for currently highlighted research.
     */
    private void renderResearchTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6" + currentHighlight.getLocalizedName().getString()));
        
        if (canUnlockResearch(currentHighlight)) {
            if (!ThaumcraftCapabilities.isResearchComplete(player, currentHighlight.getKey()) 
                    && currentHighlight.getStages() != null) {
                ThaumcraftCapabilities.getKnowledge(player).ifPresent(knowledge -> {
                    int stage = knowledge.getResearchStage(currentHighlight.getKey());
                    if (stage > 0) {
                        lines.add(Component.literal("§b" + Component.translatable("tc.research.stage").getString() 
                                + " " + stage + "/" + currentHighlight.getStages().length));
                    } else {
                        lines.add(Component.literal("§a" + Component.translatable("tc.research.begin").getString()));
                    }
                });
            }
        } else {
            lines.add(Component.literal("§c" + Component.translatable("tc.researchmissing").getString()));
            
            // List missing requirements
            if (currentHighlight.getParents() != null) {
                int idx = 0;
                for (String parent : currentHighlight.getParents()) {
                    if (!ThaumcraftCapabilities.isResearchComplete(player, parent)) {
                        String name = "?";
                        try {
                            String cleanParent = currentHighlight.getParentsClean()[idx];
                            ResearchEntry parentEntry = ResearchCategories.getResearch(cleanParent);
                            if (parentEntry != null) {
                                name = parentEntry.getLocalizedName().getString();
                            }
                        } catch (Exception ignored) {}
                        lines.add(Component.literal("§e - " + name));
                    }
                    idx++;
                }
            }
        }
        
        // Show new research/page flags
        ThaumcraftCapabilities.getKnowledge(player).ifPresent(knowledge -> {
            if (knowledge.hasResearchFlag(currentHighlight.getKey(), IPlayerKnowledge.EnumResearchFlag.RESEARCH)) {
                lines.add(Component.translatable("tc.research.newresearch"));
            }
            if (knowledge.hasResearchFlag(currentHighlight.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE)) {
                lines.add(Component.translatable("tc.research.newpage"));
            }
        });
        
        graphics.setTooltipForNextFrame(font, lines, java.util.Optional.empty(), mouseX + 3, mouseY - 3);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Search result data holder.
     */
    private static class SearchResult {
        String key;
        Identifier recipe;
        boolean cat;
        
        SearchResult(String key, Identifier recipe, boolean cat) {
            this.key = key;
            this.recipe = recipe;
            this.cat = cat;
        }
    }
    
    /**
     * Category tab button.
     */
    private class CategoryButton extends Button {
        private final ResearchCategory category;
        private final String catKey;
        private final boolean isRightSide;
        private final int completion;
        
        public CategoryButton(ResearchCategory category, String key, boolean rightSide, int x, int y, int completion) {
            super(x, y, 16, 16, Component.translatable("tc.research_category." + key), 
                    b -> {}, Button.DEFAULT_NARRATION);
            this.category = category;
            this.catKey = key;
            this.isRightSide = rightSide;
            this.completion = completion;
        }
        
        @Override
        public void onPress(InputWithModifiers input) {
            searching = false;
            searchField.setVisible(false);
            searchField.setFocused(false);
            selectedCategory = catKey;
            updateResearch();
            // Center view
            double centerX = (guiBoundsLeft + guiBoundsRight) / 2.0;
            double centerY = (guiBoundsTop + guiBoundsBottom) / 2.0;
            tempMapX = centerX;
            guiMapX = centerX;
            tempMapY = centerY;
            guiMapY = centerY;
        }
        
        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            // Draw button frame
            float frameColor = selectedCategory != null && selectedCategory.equals(catKey) ? 0.6f : 1.0f;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX() - 3, getY() - 3 + addonShift, 13.0F, 13.0F, 22, 22, 256, 256,
                    ARGB.colorFromFloat(1.0f, frameColor, 1.0f, 1.0f));
            
            // Draw category icon
            if (category.icon != null) {
                float iconBrightness = (selectedCategory != null && selectedCategory.equals(catKey)) || isHovered ? 1.0f : 0.66f;
                graphics.blit(RenderPipelines.GUI_TEXTURED, category.icon, getX(), getY() + addonShift, 0.0F, 0.0F, 16, 16, 16, 16,
                        ARGB.colorFromFloat(isHovered ? 1.0f : 0.8f, iconBrightness, iconBrightness, iconBrightness));
            }
            
            // Draw hover text
            if (isHovered) {
                String displayText = getMessage().getString() + " (" + completion + "%)";
                int textX = isRightSide ? (screenX + 9 - font.width(displayText)) : (getX() + 22);
                graphics.text(font, displayText, textX, getY() + 4 + addonShift, 0xFFFFFF);
            }
        }
    }
}
