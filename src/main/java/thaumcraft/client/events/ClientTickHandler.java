package thaumcraft.client.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.ThaumometerHUD;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.lib.network.misc.PacketMiscEventClient;
import thaumcraft.common.items.tools.ItemThaumometer;

/**
 * Client game-bus tick handler.
 *
 * Drives:
 * - the mist particle effect from {@link PacketMiscEventClient}
 * - the thaumometer scan highlight (1.12 ItemThaumometer.onUpdate client half)
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        PacketMiscEventClient.tickMist();
        FXDispatcher.tickDelayed();
        tickThaumometerHighlight();
    }

    /**
     * 1.12 ItemThaumometer.onUpdate client half: while the thaumometer is held, every
     * 5 client ticks the pointed entity and the wild-ray-traced block are highlighted
     * (when still scannable) and the pointed entity is stored as
     * {@link ThaumometerHUD#target} so {@code ThaumometerTagRenderer} can draw the
     * aspect tags above it. 26.3 gives items no client-side tick, so this runs here
     * instead.
     */
    private static void tickThaumometerHighlight() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        if (isHoldingThaumometer(player)) {
            if (player.tickCount % 5 == 0) {
                ItemThaumometer.highlightScannables(mc.level, player);
            }
        } else if (ThaumometerHUD.target != null) {
            ThaumometerHUD.target = null;
        }
    }

    private static boolean isHoldingThaumometer(Player player) {
        return player.getMainHandItem().getItem() instanceof ItemThaumometer
                || player.getOffhandItem().getItem() instanceof ItemThaumometer;
    }
}
