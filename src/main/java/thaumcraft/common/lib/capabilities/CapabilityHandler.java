package thaumcraft.common.lib.capabilities;

import net.neoforged.neoforge.registries.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;

/**
 * CapabilityHandler - Handles capability registration on the mod event bus.
 * 
 * Ported to 1.20.1
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CapabilityHandler {
    
    /**
     * Register all Thaumcraft capabilities
     */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerKnowledge.class);
        event.register(IPlayerWarp.class);
        
        Thaumcraft.LOGGER.info("Thaumcraft capabilities registered");
    }
}
