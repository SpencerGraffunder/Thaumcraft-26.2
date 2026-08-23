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
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.menu.ArcaneWorkbenchMenu;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;

/**
 * ArcaneWorkbenchScreen - Client-side GUI for the Arcane Workbench.
 * 
 * Displays:
 * - 3x3 crafting grid
 * - 6 crystal slots around the grid (one per primal aspect)
 * - Output slot
 * - Vis cost and availability
 * - Player inventory
 */
@OnlyIn(Dist.CLIENT)
public class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/arcaneworkbench.png");
    
    // Crystal highlight colors for each primal aspect
    private static final int[] ASPECT_COLORS = {
        Aspect.AIR.getColor(),
        Aspect.FIRE.getColor(),
        Aspect.WATER.getColor(),
        Aspect.EARTH.getColor(),
        Aspect.ORDER.getColor(),
        Aspect.ENTROPY.getColor()
    };
    
    public ArcaneWorkbenchScreen(ArcaneWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 190, 234);
        // Adjust label positions for the larger GUI
        this.titleLabelY = 6;
        this.inventoryLabelX = 16;
        this.inventoryLabelY = 140;
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
        
        // Get recipe info for highlighting crystals
        IArcaneRecipe recipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(
                menu.getCraftMatrix(), this.minecraft.player);
        
        AspectList crystals = null;
        int visCost = 0;
        
        if (recipe != null) {
            crystals = recipe.getCrystals();
            visCost = recipe.getVis();
            // Apply vis discount from player gear (robes, baubles, etc.)
            float discount = CasterManager.getTotalVisDiscount(this.minecraft.player);
            visCost = Math.max(1, (int)(visCost * (1.0f - discount)));
        }
        
        // Draw crystal slot highlights if recipe requires crystals
        if (crystals != null && crystals.size() > 0) {
            for (Aspect aspect : crystals.getAspects()) {
                int slotIndex = getAspectSlotIndex(aspect);
                if (slotIndex >= 0) {
                    int slotX = x + ArcaneWorkbenchMenu.CRYSTAL_X[slotIndex];
                    int slotY = y + ArcaneWorkbenchMenu.CRYSTAL_Y[slotIndex];
                    
                    // Draw colored highlight
                    int color = aspect.getColor();
                    float r = ((color >> 16) & 0xFF) / 255.0f;
                    float g = ((color >> 8) & 0xFF) / 255.0f;
                    float b = (color & 0xFF) / 255.0f;
                    
                    graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, slotX - 1, slotY - 1, 192.0F, 0.0F, 18, 18, 256, 256);
                }
            }
        }
    }
    
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Don't draw default title - draw custom vis info instead
        
        TileArcaneWorkbench tile = menu.getBlockEntity();
        int auraVis = menu.getAuraVis();
        
        // Get recipe info
        IArcaneRecipe recipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(
                menu.getCraftMatrix(), this.minecraft.player);
        
        int visCost = 0;
        int discount = 0;
        
        if (recipe != null) {
            int baseVisCost = recipe.getVis();
            // Calculate vis discount from player gear (robes, baubles, etc.)
            float discountPct = CasterManager.getTotalVisDiscount(this.minecraft.player);
            discount = (int)(discountPct * 100);
            visCost = Math.max(1, (int)(baseVisCost * (1.0f - discountPct)));
        }
        
        // Draw vis available text (right side, scaled down)
        graphics.pose().pushMatrix();
        graphics.pose().translate(168, 46);
        graphics.pose().scale(0.5f);
        
        String availText = auraVis + " " + Component.translatable("gui.thaumcraft.workbench.available").getString();
        int textWidth = this.font.width(availText);
        int textColor = (auraVis < visCost) ? 0xEE4444 : 0x6E8E5E; // Red if not enough);
        
        graphics.pose().popMatrix();
        
        // Draw vis cost if there's a recipe
        if (visCost > 0) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(168, 38);
            graphics.pose().scale(0.5f);
            
            String costText = visCost + " " + Component.translatable("gui.thaumcraft.workbench.cost").getString();
            if (discount > 0) {
                costText += " (" + discount + "% " + Component.translatable("gui.thaumcraft.workbench.discount").getString() + ")";
            }
            textWidth = this.font.width(costText);
            graphics.text(this.font);
            
            graphics.pose().popMatrix();
            
            // If not enough vis, gray out the result slot area
            if (auraVis < visCost) {
                // Draw semi-transparent overlay on output slot
                graphics.fill(159, 63, 177, 81, 0x80000000);
            }
        }
        
        // Draw inventory label
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
    
    /**
     * Get the slot index (0-5) for a primal aspect.
     */
    private int getAspectSlotIndex(Aspect aspect) {
        for (int i = 0; i < ArcaneWorkbenchMenu.PRIMAL_ASPECTS.length; i++) {
            if (ArcaneWorkbenchMenu.PRIMAL_ASPECTS[i] == aspect) {
                return i;
            }
        }
        return -1;
    }
}
