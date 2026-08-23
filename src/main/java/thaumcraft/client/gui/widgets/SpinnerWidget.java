package thaumcraft.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.NodeSetting;

/**
 * SpinnerWidget - A value spinner for focus node settings.
 * Shows left/right arrows with the current value text in the middle.
 * 
 * Ported from GuiFocusSettingSpinnerButton in 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class SpinnerWidget extends AbstractWidget {
    
    private static final Identifier TEXTURE = 
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    
    private final NodeSetting setting;
    private final OnValueChange onValueChange;
    
    @FunctionalInterface
    public interface OnValueChange {
        void onValueChange(NodeSetting setting, int newValue);
    }
    
    public SpinnerWidget(int x, int y, int width, NodeSetting setting, OnValueChange onValueChange) {
        super(x, y, width, 10, Component.literal(setting.getLocalizedName()));
        this.setting = setting;
        this.onValueChange = onValueChange;
    }
    
    public NodeSetting getSetting() {
        return setting;
    }
    
    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        
        // Hover state affects color
        boolean hovered = isHoveredOrFocused();
        float brightness = hovered ? 1.0f : 0.9f;
        
        // Draw left arrow
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(), 20.0F, 0.0F, 10, 10, 256, 256,
                ARGB.colorFromFloat(brightness, brightness, brightness, brightness));
        
        // Draw right arrow
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX() + width, getY(), 30.0F, 0.0F, 10, 10, 256, 256,
                ARGB.colorFromFloat(brightness, brightness, brightness, brightness));
        
        // Draw value text centered between arrows
        String valueText = setting.getValueText();
        int textX = getX() + (width + 10) / 2 - font.width(valueText) / 2;
        graphics.text(font, valueText, textX, getY() + 1, 0xFFFFFF, true);
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (!visible || !active) return false;
        
        // Check if clicked on left arrow (decrement)
        if (mouseX >= getX() && mouseX < getX() + 10 && 
            mouseY >= getY() && mouseY < getY() + height) {
            setting.decrement();
            if (onValueChange != null) {
                onValueChange.onValueChange(setting, setting.getValue());
            }
            return true;
        }
        
        // Check if clicked on right arrow (increment)
        if (mouseX >= getX() + width && mouseX < getX() + width + 10 && 
            mouseY >= getY() && mouseY < getY() + height) {
            setting.increment();
            if (onValueChange != null) {
                onValueChange.onValueChange(setting, setting.getValue());
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
