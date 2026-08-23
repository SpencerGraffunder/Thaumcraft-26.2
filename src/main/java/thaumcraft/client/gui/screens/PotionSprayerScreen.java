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
import thaumcraft.common.menu.PotionSprayerMenu;

/**
 * PotionSprayerScreen - Client-side GUI for the Potion Sprayer.
 * 
 * Displays the potion slot and charge indicators.
 */
@OnlyIn(Dist.CLIENT)
public class PotionSprayerScreen extends AbstractContainerScreen<PotionSprayerMenu> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_potionsprayer.png");
    
    public PotionSprayerScreen(PotionSprayerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 232);
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
        
        // Draw charge indicators (8 max charges)
        int charges = menu.getBlockEntity().charges;
        for (int i = 0; i < charges && i < 8; i++) {
            // Draw filled charge indicator
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 78 + (i % 4) * 10, y + 30 + (i / 4) * 10, 176.0F, 0.0F, 8, 8, 256, 256);
        }
    }
    
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Draw inventory label
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }
}
