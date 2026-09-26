package thaumcraft.client.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.network.misc.PacketMiscEventClient;

/**
 * Client game-bus tick handler.
 *
 * Drives the mist particle effect from {@link PacketMiscEventClient}.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        PacketMiscEventClient.tickMist();
    }
}
