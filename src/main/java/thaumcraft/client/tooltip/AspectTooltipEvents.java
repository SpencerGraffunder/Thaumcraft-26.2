package thaumcraft.client.tooltip;

import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.tooltip.AspectTooltipComponent;

/**
 * Wires up the essentia-aspect tooltip icon row (1.12 parity: aspect symbols + amounts shown
 * under items that carry essentia, in addition to the text lines from {@code ItemTooltipEvent}).
 *
 * <ul>
 *   <li><b>Mod bus</b> ({@code RegisterClientTooltipComponentFactoriesEvent}): map
 *       {@link AspectTooltipComponent} &rarr; {@link AspectClientTooltipComponent} so the
 *       vanilla tooltip renderer knows how to draw the row.</li>
 *   <li><b>Game bus</b> ({@code RenderTooltipEvent.GatherComponents}): append the component to
 *       any item tooltip whose item implements {@link IEssentiaContainerItem} and has aspects.</li>
 * </ul>
 *
 * <p>{@code @EventBusSubscriber} registers the class on both buses (loader 11.x), so each
 * {@code @SubscribeEvent} method fires on the bus that posts its event type.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class AspectTooltipEvents {

    @SubscribeEvent
    public static void registerTooltipComponentFactory(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(AspectTooltipComponent.class, comp -> new AspectClientTooltipComponent(comp.getAspects()));
    }

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Item item = stack.getItem();
        if (!(item instanceof IEssentiaContainerItem container)) {
            return;
        }
        AspectList aspects = container.getAspects(stack);
        if (aspects == null || aspects.size() == 0) {
            return;
        }
        event.getTooltipElements().add(Either.<FormattedText, TooltipComponent>right(new AspectTooltipComponent(aspects)));
    }
}
