package thaumcraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.SmelterMenu;

/**
 * SmelterScreen - Client-side GUI for the Alchemical Smelter.
 * 
 * Displays:
 * - Input slot (items with aspects)
 * - Fuel slot
 * - Burn time indicator
 * - Smelting progress
 * - Vis/essentia storage level
 */
@OnlyIn(Dist.CLIENT)
public class SmelterScreen extends AbstractContainerScreen<SmelterMenu> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_smelter.png");
    
    public SmelterScreen(SmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        
        // Draw main background
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        
        // Draw burn time indicator (fire icon)
        if (menu.isBurning()) {
            int burnProgress = menu.getBurnTimeScaled(20);
            // Draw fire from bottom up
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 80, y + 26 + 20 - burnProgress, 176.0F, 20.0F - burnProgress, 16, burnProgress, 256, 256);
        }
        
        // Draw cooking progress (vertical bar on right)
        int cookProgress = menu.getCookProgressScaled(46);
        // Draw from bottom up
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 106, y + 13 + 46 - cookProgress, 216.0F, 46.0F - cookProgress, 9, cookProgress, 256, 256);
        
        // Draw vis storage level (vertical bar on left)
        int visLevel = getVisScaled(48);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 61, y + 12 + 48 - visLevel, 200.0F, 48.0F - visLevel, 8, visLevel, 256, 256);
        
        // Draw vis bar frame overlay
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 60, y + 8, 232.0F, 0.0F, 10, 55, 256, 256);
    }
    
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Draw inventory label
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
    
    /**
     * Scale the vis amount to the given scale.
     */
    private int getVisScaled(int scale) {
        int vis = menu.getVis();
        int maxVis = 256; // Max vis storage
        return vis * scale / maxVis;
    }
}
