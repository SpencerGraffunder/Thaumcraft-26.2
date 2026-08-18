package thaumcraft.common.lib.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.NetworkDirection;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.lib.network.misc.PacketBiomeChange;
import thaumcraft.common.lib.network.misc.PacketKnowledgeGain;
import thaumcraft.common.lib.network.misc.PacketLogisticsRequestToServer;
import thaumcraft.common.lib.network.misc.PacketMiscEvent;
import thaumcraft.common.lib.network.misc.PacketMiscStringToServer;
import thaumcraft.common.lib.network.misc.PacketSealFilterToClient;
import thaumcraft.common.lib.network.misc.PacketSealToClient;
import thaumcraft.common.lib.network.misc.PacketSelectThaumotoriumRecipeToServer;
import thaumcraft.common.lib.network.misc.PacketStartTheoryToServer;
import thaumcraft.common.lib.network.playerdata.PacketFocusNameToServer;
import thaumcraft.common.lib.network.playerdata.PacketFocusNodesToServer;
import thaumcraft.common.lib.network.playerdata.PacketPlayerFlagToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncResearchFlagsToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncWarp;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;
import thaumcraft.common.lib.network.fx.PacketFXBlockArc;
import thaumcraft.common.lib.network.fx.PacketFXBlockBamf;
import thaumcraft.common.lib.network.fx.PacketFXBlockMist;
import thaumcraft.common.lib.network.fx.PacketFXBoreDig;
import thaumcraft.common.lib.network.fx.PacketFXEssentiaSource;
import thaumcraft.common.lib.network.fx.PacketFXFocusEffect;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpactBurst;
import thaumcraft.common.lib.network.fx.PacketFXInfusionSource;
import thaumcraft.common.lib.network.fx.PacketFXPollute;
import thaumcraft.common.lib.network.fx.PacketFXScanSource;
import thaumcraft.common.lib.network.fx.PacketFXShield;
import thaumcraft.common.lib.network.fx.PacketFXSlash;
import thaumcraft.common.lib.network.fx.PacketFXSonic;
import thaumcraft.common.lib.network.fx.PacketFXWispZap;
import thaumcraft.common.lib.network.fx.PacketFXZap;
import thaumcraft.common.lib.network.misc.PacketFocusChangeToServer;
import thaumcraft.common.lib.network.misc.PacketItemKeyToServer;
import thaumcraft.common.lib.network.tiles.PacketTileToClient;
import thaumcraft.common.lib.network.tiles.PacketTileToServer;

/**
 * PacketHandler - Manages network communication for Thaumcraft.
 * Uses Forge's SimpleChannel system for packet registration and handling.
 * 
 * Ported to 1.20.1
 */
public class PacketHandler {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    private static int nextId() {
        return packetId++;
    }
    
    /**
     * Register all packets
     * Called during mod initialization
     */
    public static void init() {
        Thaumcraft.LOGGER.info("Registering Thaumcraft network packets");
        
        // Seal sync packets (server -> client)
        INSTANCE.messageBuilder(PacketSealToClient.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketSealToClient::encode)
                .decoder(PacketSealToClient::decode)
                .consumerMainThread(PacketSealToClient::handle)
                .add();
        
        // Player data sync packets (server -> client)
        INSTANCE.messageBuilder(PacketSyncKnowledge.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketSyncKnowledge::encode)
                .decoder(PacketSyncKnowledge::decode)
                .consumerMainThread(PacketSyncKnowledge::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketSyncWarp.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketSyncWarp::encode)
                .decoder(PacketSyncWarp::decode)
                .consumerMainThread(PacketSyncWarp::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketWarpMessage.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketWarpMessage::encode)
                .decoder(PacketWarpMessage::decode)
                .consumerMainThread(PacketWarpMessage::handle)
                .add();
        
        // Aura sync packets (server -> client)
        INSTANCE.messageBuilder(PacketAuraToClient.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketAuraToClient::encode)
                .decoder(PacketAuraToClient::decode)
                .consumerMainThread(PacketAuraToClient::handle)
                .add();
        
        // Tile entity sync packets (server -> client)
        INSTANCE.messageBuilder(PacketTileToClient.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketTileToClient::encode)
                .decoder(PacketTileToClient::decode)
                .consumerMainThread(PacketTileToClient::handle)
                .add();
        
        // Tile entity update packets (client -> server)
        INSTANCE.messageBuilder(PacketTileToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketTileToServer::encode)
                .decoder(PacketTileToServer::decode)
                .consumerMainThread(PacketTileToServer::handle)
                .add();
        
        // FX packets (server -> client)
        INSTANCE.messageBuilder(PacketFXSlash.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXSlash::encode)
                .decoder(PacketFXSlash::decode)
                .consumerMainThread(PacketFXSlash::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXBlockArc.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXBlockArc::encode)
                .decoder(PacketFXBlockArc::decode)
                .consumerMainThread(PacketFXBlockArc::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXBlockBamf.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXBlockBamf::encode)
                .decoder(PacketFXBlockBamf::decode)
                .consumerMainThread(PacketFXBlockBamf::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXZap.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXZap::encode)
                .decoder(PacketFXZap::decode)
                .consumerMainThread(PacketFXZap::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXEssentiaSource.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXEssentiaSource::encode)
                .decoder(PacketFXEssentiaSource::decode)
                .consumerMainThread(PacketFXEssentiaSource::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXShield.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXShield::encode)
                .decoder(PacketFXShield::decode)
                .consumerMainThread(PacketFXShield::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXWispZap.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXWispZap::encode)
                .decoder(PacketFXWispZap::decode)
                .consumerMainThread(PacketFXWispZap::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXFocusEffect.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXFocusEffect::encode)
                .decoder(PacketFXFocusEffect::decode)
                .consumerMainThread(PacketFXFocusEffect::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXFocusPartImpact.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXFocusPartImpact::encode)
                .decoder(PacketFXFocusPartImpact::decode)
                .consumerMainThread(PacketFXFocusPartImpact::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXFocusPartImpactBurst.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXFocusPartImpactBurst::encode)
                .decoder(PacketFXFocusPartImpactBurst::decode)
                .consumerMainThread(PacketFXFocusPartImpactBurst::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXInfusionSource.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXInfusionSource::encode)
                .decoder(PacketFXInfusionSource::decode)
                .consumerMainThread(PacketFXInfusionSource::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXPollute.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXPollute::encode)
                .decoder(PacketFXPollute::decode)
                .consumerMainThread(PacketFXPollute::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXBoreDig.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXBoreDig::encode)
                .decoder(PacketFXBoreDig::decode)
                .consumerMainThread(PacketFXBoreDig::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXScanSource.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXScanSource::encode)
                .decoder(PacketFXScanSource::decode)
                .consumerMainThread(PacketFXScanSource::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXSonic.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXSonic::encode)
                .decoder(PacketFXSonic::decode)
                .consumerMainThread(PacketFXSonic::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketFXBlockMist.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketFXBlockMist::encode)
                .decoder(PacketFXBlockMist::decode)
                .consumerMainThread(PacketFXBlockMist::handle)
                .add();
        
        // Research packets (client -> server)
        INSTANCE.messageBuilder(PacketSyncProgressToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketSyncProgressToServer::encode)
                .decoder(PacketSyncProgressToServer::decode)
                .consumerMainThread(PacketSyncProgressToServer::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketSyncResearchFlagsToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketSyncResearchFlagsToServer::encode)
                .decoder(PacketSyncResearchFlagsToServer::decode)
                .consumerMainThread(PacketSyncResearchFlagsToServer::handle)
                .add();
        
        // Key action packets (client -> server)
        INSTANCE.messageBuilder(PacketFocusChangeToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketFocusChangeToServer::encode)
                .decoder(PacketFocusChangeToServer::decode)
                .consumerMainThread(PacketFocusChangeToServer::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketItemKeyToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketItemKeyToServer::encode)
                .decoder(PacketItemKeyToServer::decode)
                .consumerMainThread(PacketItemKeyToServer::handle)
                .add();
        
        // Focus packets (client -> server)
        INSTANCE.messageBuilder(PacketFocusNodesToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketFocusNodesToServer::encode)
                .decoder(PacketFocusNodesToServer::decode)
                .consumerMainThread(PacketFocusNodesToServer::handle)
                .add();
        
        // Player flag packets (client -> server)
        INSTANCE.messageBuilder(PacketPlayerFlagToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketPlayerFlagToServer::encode)
                .decoder(PacketPlayerFlagToServer::decode)
                .consumerMainThread(PacketPlayerFlagToServer::handle)
                .add();
        
        // Misc event packets
        INSTANCE.messageBuilder(PacketMiscEvent.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketMiscEvent::encode)
                .decoder(PacketMiscEvent::decode)
                .consumerMainThread(PacketMiscEvent::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketKnowledgeGain.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketKnowledgeGain::encode)
                .decoder(PacketKnowledgeGain::decode)
                .consumerMainThread(PacketKnowledgeGain::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketSealFilterToClient.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketSealFilterToClient::encode)
                .decoder(PacketSealFilterToClient::decode)
                .consumerMainThread(PacketSealFilterToClient::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketBiomeChange.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketBiomeChange::encode)
                .decoder(PacketBiomeChange::decode)
                .consumerMainThread(PacketBiomeChange::handle)
                .add();
        
        // Logistics packets (client -> server)
        INSTANCE.messageBuilder(PacketLogisticsRequestToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketLogisticsRequestToServer::encode)
                .decoder(PacketLogisticsRequestToServer::decode)
                .consumerMainThread(PacketLogisticsRequestToServer::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketMiscStringToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketMiscStringToServer::encode)
                .decoder(PacketMiscStringToServer::decode)
                .consumerMainThread(PacketMiscStringToServer::handle)
                .add();
        
        // Research/Crafting packets (client -> server)
        INSTANCE.messageBuilder(PacketStartTheoryToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketStartTheoryToServer::encode)
                .decoder(PacketStartTheoryToServer::decode)
                .consumerMainThread(PacketStartTheoryToServer::handle)
                .add();
        
        INSTANCE.messageBuilder(PacketSelectThaumotoriumRecipeToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketSelectThaumotoriumRecipeToServer::encode)
                .decoder(PacketSelectThaumotoriumRecipeToServer::decode)
                .consumerMainThread(PacketSelectThaumotoriumRecipeToServer::handle)
                .add();
        
        // Focus manipulator packets (client -> server)
        INSTANCE.messageBuilder(PacketFocusNameToServer.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketFocusNameToServer::encode)
                .decoder(PacketFocusNameToServer::decode)
                .consumerMainThread(PacketFocusNameToServer::handle)
                .add();
        
        Thaumcraft.LOGGER.info("Thaumcraft network packets registered");
    }
    
    /**
     * Send a packet to a specific player
     * @param packet the packet to send
     * @param player the player to send to
     */
    public static <MSG> void sendToPlayer(MSG packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    /**
     * Send a packet to all players
     * @param packet the packet to send
     */
    public static <MSG> void sendToAll(MSG packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }
    
    /**
     * Send a packet to all players tracking a specific entity
     * @param packet the packet to send
     * @param entity the entity being tracked
     */
    public static <MSG> void sendToAllTracking(MSG packet, net.minecraft.world.entity.Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }
    
    /**
     * Send a packet to the server
     * @param packet the packet to send
     */
    public static <MSG> void sendToServer(MSG packet) {
        INSTANCE.sendToServer(packet);
    }
    
    /**
     * Send a packet to all players near a point
     * @param packet the packet to send
     * @param targetPoint the point to check distance from
     */
    public static <MSG> void sendToNear(MSG packet, PacketDistributor.TargetPoint targetPoint) {
        INSTANCE.send(PacketDistributor.NEAR.with(() -> targetPoint), packet);
    }
    
    /**
     * Send a packet to all players in a dimension
     * @param packet the packet to send
     * @param dimension the dimension key
     */
    public static <MSG> void sendToDimension(MSG packet, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        INSTANCE.send(PacketDistributor.DIMENSION.with(() -> dimension), packet);
    }
    
    /**
     * Send a packet to all players tracking a specific block entity
     * @param packet the packet to send
     * @param blockEntity the block entity being tracked
     */
    public static <MSG> void sendToAllTracking(MSG packet, BlockEntity blockEntity) {
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos pos = blockEntity.getBlockPos();
            serverLevel.getChunkSource().chunkMap.getPlayers(
                new net.minecraft.world.level.ChunkPos(pos), false
            ).forEach(player -> sendToPlayer(packet, player));
        }
    }
    
    /**
     * Send a packet to all players tracking a specific chunk
     * @param packet the packet to send
     * @param level the server level
     * @param pos the block position to find the chunk for
     */
    public static <MSG> void sendToAllTrackingChunk(MSG packet, ServerLevel level, BlockPos pos) {
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), packet);
    }
}
