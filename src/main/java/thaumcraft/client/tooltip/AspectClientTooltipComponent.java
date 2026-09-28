package thaumcraft.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.lib.AspectRenderer;

/**
 * Client renderer for {@code AspectTooltipComponent}. Draws a horizontal row of aspect
 * symbols (each 16x16) with their amounts overlaid, matching the 1.12 tooltip aspect display.
 *
 * <p>{@code extractImage} is called once per line with {@code x} = the tooltip's content-left
 * edge and {@code y} = this line's top, so icons are left-aligned with the text lines above.
 */
@OnlyIn(Dist.CLIENT)
public class AspectClientTooltipComponent implements ClientTooltipComponent {
    private static final int ICON_SIZE = 16;
    private static final int SPACING = 2;

    private final AspectList aspects;

    public AspectClientTooltipComponent(AspectList aspects) {
        this.aspects = aspects;
    }

    @Override
    public int getHeight(Font font) {
        return ICON_SIZE + SPACING;
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(ICON_SIZE, aspects.size() * (ICON_SIZE + SPACING) + SPACING);
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int cx = x + SPACING / 2;
        for (Aspect aspect : aspects.getAspectsSortedByName()) {
            AspectRenderer.drawAspect(graphics, cx, y + SPACING / 2, aspect, aspects.getAmount(aspect));
            cx += ICON_SIZE + SPACING;
        }
    }
}
