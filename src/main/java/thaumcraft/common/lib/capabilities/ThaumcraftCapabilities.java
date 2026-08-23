package thaumcraft.common.lib.capabilities;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.common.golems.seals.SealHandler;

import java.util.Optional;

/**
 * ThaumcraftCapabilities - Holds references to all Thaumcraft data attachments
 * and handles their event-based synchronization.
 * 
 * The attachment types themselves are registered in
 * {@link thaumcraft.api.capabilities.ThaumcraftCapabilities}.
 * 
 * Ported to 1.20.1 / NeoForge 26.2
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ThaumcraftCapabilities {
    
    // Resource locations for capability attachment
    public static final Identifier KNOWLEDGE_ID = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "knowledge");
    public static final Identifier WARP_ID = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "warp");
    
    /**
     * Get the knowledge attachment from a player
     * @param player the player
     * @return Optional containing the knowledge, or empty if not registered yet
     */
    public static Optional<IPlayerKnowledge> getKnowledge(Player player) {
        return Optional.ofNullable(thaumcraft.api.capabilities.ThaumcraftCapabilities.KNOWLEDGE)
                .map(player::getData);
    }
    
    /**
     * Get the warp attachment from a player
     * @param player the player
     * @return Optional containing the warp, or empty if not registered yet
     */
    public static Optional<IPlayerWarp> getWarp(Player player) {
        return Optional.ofNullable(thaumcraft.api.capabilities.ThaumcraftCapabilities.WARP)
                .map(player::getData);
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
