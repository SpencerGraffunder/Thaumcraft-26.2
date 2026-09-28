package thaumcraft.common.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import thaumcraft.api.aspects.AspectList;

/**
 * Server/common-side marker for a tooltip row that displays an item's essentia aspects
 * as icons (symbols + amounts).
 *
 * <p>Rendered on the client by {@code AspectClientTooltipComponent}, which is mapped from
 * this class via {@code RegisterClientTooltipComponentFactoriesEvent}. Appended to the
 * tooltip in {@code RenderTooltipEvent.GatherComponents}.
 *
 * <p>Port of the 1.12 {@code RenderTooltipEvent.PostBackground} aspect-icon row, which drew
 * the aspect symbols + amounts at the bottom of an item's tooltip.
 */
public class AspectTooltipComponent implements TooltipComponent {
    private final AspectList aspects;

    public AspectTooltipComponent(AspectList aspects) {
        this.aspects = aspects;
    }

    public AspectList getAspects() {
        return aspects;
    }
}
