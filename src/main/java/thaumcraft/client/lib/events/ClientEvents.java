package thaumcraft.client.lib.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.client.gui.screens.ResearchBrowserScreen;
import thaumcraft.init.ModItems;

/**
 * Client-side event handlers for Thaumcraft.
 * Registered on the FORGE event bus.
 * 
 * Ported to 1.20.1
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ClientEvents {
    
    /**
     * Handle client tick events.
     * Used for:
     * - Key input processing
     * - Client-side particle/effect updates
     * - HUD updates
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        // Process key bindings
        KeyHandler.onClientTick(event);
        
        // TODO: Add other client tick processing as needed
        // - Radial menu updates
        // - Goggle/HUD overlay updates
        // - Client-side particle systems
    }

    /**
     * Open the Thaumonomicon (research browser) GUI when the player right-clicks
     * with it. In modern Minecraft {@code Item.use()} is only invoked on the
     * server, so the client-side open must be driven from a client event.
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.THAUMONOMICON.get())) {
            Minecraft.getInstance().gui.setScreen(new ResearchBrowserScreen());
        }
    }
}
