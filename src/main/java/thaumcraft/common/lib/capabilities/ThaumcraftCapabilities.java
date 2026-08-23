package thaumcraft.common.lib.capabilities;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.common.golems.seals.SealHandler;

/**
 * ThaumcraftCapabilities - Holds references to all Thaumcraft capabilities
 * and handles their registration and attachment.
 * 
 * Ported to 1.20.1
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, bus = Mod.EventBusSubscriber.Bus.GAME)
public class ThaumcraftCapabilities {
    
    // Capability instances
    public static final Capability<IPlayerKnowledge> KNOWLEDGE = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IPlayerWarp> WARP = CapabilityManager.get(new CapabilityToken<>() {});
    
    // Resource locations for capability attachment
    public static final Identifier KNOWLEDGE_ID = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "knowledge");
    public static final Identifier WARP_ID = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "warp");
    
    /**
     * Get the knowledge capability from a player
     * @param player the player
     * @return LazyOptional containing the capability, or empty if not present
     */
    public static LazyOptional<IPlayerKnowledge> getKnowledge(Player player) {
        return player.getCapability(KNOWLEDGE);
    }
    
    /**
     * Get the warp capability from a player
     * @param player the player
     * @return LazyOptional containing the capability, or empty if not present
     */
    public static LazyOptional<IPlayerWarp> getWarp(Player player) {
        return player.getCapability(WARP);
    }
    
    /**
     * Check if research is known by a player
     * Convenience method
     */
    public static boolean isResearchKnown(Player player, String research) {
        return getKnowledge(player)
                .map(k -> k.isResearchKnown(research))
                .orElse(false);
    }
    
    /**
     * Check if research is complete for a player
     * Convenience method
     */
    public static boolean isResearchComplete(Player player, String research) {
        return getKnowledge(player)
                .map(k -> k.isResearchComplete(research))
                .orElse(false);
    }
    
    // ==================== Event Handlers ====================
    
    /**
     * Attach capabilities to players
     */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            // Attach knowledge capability
            if (!event.getObject().getCapability(KNOWLEDGE).isPresent()) {
                event.addCapability(KNOWLEDGE_ID, new PlayerKnowledge.Provider());
            }
            
            // Attach warp capability
            if (!event.getObject().getCapability(WARP).isPresent()) {
                event.addCapability(WARP_ID, new PlayerWarp.Provider());
            }
        }
    }
    
    /**
     * Copy capabilities when player is cloned (death, dimension change, etc.)
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // Copy knowledge (research persists through death)
            event.getOriginal().getCapability(KNOWLEDGE).ifPresent(oldKnowledge -> {
                event.getEntity().getCapability(KNOWLEDGE).ifPresent(newKnowledge -> {
                    newKnowledge.deserializeNBT(oldKnowledge.serializeNBT());
                });
            });
            
            // Copy warp (warp persists through death, though temporary warp might decay)
            event.getOriginal().getCapability(WARP).ifPresent(oldWarp -> {
                event.getEntity().getCapability(WARP).ifPresent(newWarp -> {
                    newWarp.deserializeNBT(oldWarp.serializeNBT());
                });
            });
        }
    }
    
    /**
     * Sync capabilities when player respawns or changes dimension
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Sync knowledge to client
            getKnowledge(serverPlayer).ifPresent(k -> k.sync(serverPlayer));
            // Sync warp to client
            getWarp(serverPlayer).ifPresent(w -> w.sync(serverPlayer));
        }
    }
    
    /**
     * Sync capabilities when player logs in
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Sync knowledge to client
            getKnowledge(serverPlayer).ifPresent(k -> k.sync(serverPlayer));
            // Sync warp to client
            getWarp(serverPlayer).ifPresent(w -> w.sync(serverPlayer));
            // Sync all seals in the player's dimension
            SealHandler.syncAllSealsToPlayer(serverPlayer);
        }
    }
    
    /**
     * Sync capabilities when player changes dimension
     */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Sync knowledge to client
            getKnowledge(serverPlayer).ifPresent(k -> k.sync(serverPlayer));
            // Sync warp to client
            getWarp(serverPlayer).ifPresent(w -> w.sync(serverPlayer));
            // Sync all seals in the new dimension
            SealHandler.syncAllSealsToPlayer(serverPlayer);
        }
    }
}
