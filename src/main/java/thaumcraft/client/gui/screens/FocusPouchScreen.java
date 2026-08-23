package thaumcraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.FocusPouchMenu;

/**
 * FocusPouchScreen - Client-side GUI for the Focus Pouch.
 * 
 * Displays a 6x3 grid of focus slots plus the player inventory.
 */
@OnlyIn(Dist.CLIENT)
public class FocusPouchScreen extends AbstractContainerScreen<FocusPouchMenu> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_focuspouch.png");
    
    public FocusPouchScreen(FocusPouchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 175, 232);
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
    }
    
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Draw title centered above pouch slots
        graphics.text(this.font, this.title, 
                (this.imageWidth - this.font.width(this.title)) / 2, 
                6, 0x404040, false);
        // Draw inventory label
        graphics.text(this.font, this.playerInventoryTitle, 
                8, this.inventoryLabelY, 0x404040, false);
    }
    
    @Override
    public boolean keyPressed(KeyEvent event) {
        // Prevent using hotbar keys to move items (could move the pouch itself)
        if (this.minecraft != null && this.minecraft.options.keyHotbarSlots[this.menu.getSlot(0).getContainerSlot()].matches(event)) {
            return true;
        }
        return super.keyPressed(event);
    }
}
