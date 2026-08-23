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
import thaumcraft.common.menu.HungryChestMenu;

/**
 * HungryChestScreen - Client-side GUI for the Hungry Chest.
 * Standard 27-slot chest layout, similar to vanilla chest.
 */
@OnlyIn(Dist.CLIENT)
public class HungryChestScreen extends AbstractContainerScreen<HungryChestMenu> {
    
    // Use vanilla chest texture for now, or create custom one
    private static final Identifier TEXTURE = 
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    
    // Hungry chest has 3 rows (same as small chest)
    private static final int ROWS = 3;
    
    public HungryChestScreen(HungryChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 114 + ROWS * 18); // 168 for 3 rows
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        
        // Draw top section (chest slots area)
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, ROWS * 18 + 17, 256, 256);
        
        // Draw bottom section (player inventory)
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y + ROWS * 18 + 17, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
    }
    
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Draw inventory label
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}
