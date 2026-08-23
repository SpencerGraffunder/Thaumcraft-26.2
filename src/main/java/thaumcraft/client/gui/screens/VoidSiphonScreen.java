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
import thaumcraft.common.menu.VoidSiphonMenu;

/**
 * VoidSiphonScreen - Client-side GUI for the Void Siphon.
 * 
 * Displays the output slot and siphoning progress.
 */
@OnlyIn(Dist.CLIENT)
public class VoidSiphonScreen extends AbstractContainerScreen<VoidSiphonMenu> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_voidsiphon.png");
    
    public VoidSiphonScreen(VoidSiphonMenu menu, Inventory playerInventory, Component title) {
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
        
        // Draw progress indicator (circular or bar)
        int progress = menu.getProgress();
        int maxProgress = 2000; // PROGRESS_REQUIRED from TileVoidSiphon
        int progressWidth = progress * 24 / maxProgress;
        if (progressWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 76, y + 55, 176.0F, 0.0F, progressWidth, 16, 256, 256);
        }
    }
    
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Draw inventory label
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}
