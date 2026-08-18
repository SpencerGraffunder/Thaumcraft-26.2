package thaumcraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.construct.EntityTurretCrossbowAdvanced;
import thaumcraft.common.menu.TurretMenu;

/**
 * TurretScreen - Client-side GUI for crossbow turrets.
 * 
 * Basic turret: Simple arrow slot
 * Advanced turret: Arrow slot + targeting options
 */
@OnlyIn(Dist.CLIENT)
public class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    
    private static final Identifier TEXTURE_BASIC = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_turret.png");
    private static final Identifier TEXTURE_ADVANCED = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_turret_advanced.png");
    
    private final boolean isAdvanced;
    
    public TurretScreen(TurretMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.isAdvanced = menu.isAdvanced();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Add targeting buttons for advanced turret
        if (isAdvanced && menu.getTurret() instanceof EntityTurretCrossbowAdvanced advanced) {
            int buttonX = this.leftPos + 100;
            int buttonY = this.topPos + 20;
            
            // Target Mobs button
            this.addRenderableWidget(Button.builder(
                    Component.literal(advanced.getTargetMob() ? "[Mobs]" : "Mobs"),
                    btn -> {
                        sendButtonClick(2);
                        btn.setMessage(Component.literal(advanced.getTargetMob() ? "Mobs" : "[Mobs]"));
                    })
                    .bounds(buttonX, buttonY, 60, 14)
                    .build());
            
            // Target Animals button
            this.addRenderableWidget(Button.builder(
                    Component.literal(advanced.getTargetAnimal() ? "[Animals]" : "Animals"),
                    btn -> {
                        sendButtonClick(1);
                        btn.setMessage(Component.literal(advanced.getTargetAnimal() ? "Animals" : "[Animals]"));
                    })
                    .bounds(buttonX, buttonY + 16, 60, 14)
                    .build());
            
            // Target Players button
            this.addRenderableWidget(Button.builder(
                    Component.literal(advanced.getTargetPlayer() ? "[Players]" : "Players"),
                    btn -> {
                        sendButtonClick(3);
                        btn.setMessage(Component.literal(advanced.getTargetPlayer() ? "Players" : "[Players]"));
                    })
                    .bounds(buttonX, buttonY + 32, 60, 14)
                    .build());
            
            // Target Friendly button
            this.addRenderableWidget(Button.builder(
                    Component.literal(advanced.getTargetFriendly() ? "[Friendly]" : "Friendly"),
                    btn -> {
                        sendButtonClick(4);
                        btn.setMessage(Component.literal(advanced.getTargetFriendly() ? "Friendly" : "[Friendly]"));
                    })
                    .bounds(buttonX, buttonY + 48, 60, 14)
                    .build());
        }
    }
    
    private void sendButtonClick(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int x = this.leftPos;
        int y = this.topPos;
        
        // Draw main background
        Identifier texture = isAdvanced ? TEXTURE_ADVANCED : TEXTURE_BASIC;
        graphics.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Draw inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}
