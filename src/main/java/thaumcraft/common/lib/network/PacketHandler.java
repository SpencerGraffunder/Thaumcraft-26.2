package thaumcraft.common.lib.network;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
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
import thaumcraft.common.lib.network.misc.PacketFocusChangeToServer;
import thaumcraft.common.lib.network.misc.PacketItemKeyToServer;
import thaumcraft.common.lib.network.playerdata.PacketFocusNameToServer;
import thaumcraft.common.lib.network.playerdata.PacketFocusNodesToServer;
import thaumcraft.common.lib.network.playerdata.PacketPlayerFlagToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncResearchFlagsToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncWarp;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;
import thaumcraft.common.lib.network.tiles.PacketTileToClient;
import thaumcraft.common.lib.network.tiles.PacketTileToServer;
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

/**
 * PacketHandler - Manages network communication for Thaumcraft.
 * <p>
 * Migrated to the NeoForge 26.2 {@link CustomPacketPayload} model. Each packet declares
 * its own {@code TYPE} and {@code STREAM_CODEC}; all registration happens in
 * {@link #register(RegisterPayloadHandlersEvent)} from the mod event bus.
 * <p>
 * Static send helpers keep the legacy call shapes so call sites do not need to change.
 */
public class PacketHandler {

    private static final String CHANNEL = Thaumcraft.MODID + "main";

    /**
     * Register all payload handlers. Invoked by the mod event bus for
     * {@link RegisterPayloadHandlersEvent}.
     */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(CHANNEL);

        // ---- Server -> Client ----
        reg.playToClient(PacketSealToClient.TYPE, PacketSealToClient.STREAM_CODEC, PacketSealToClient::handle);
        reg.playToClient(PacketSyncKnowledge.TYPE, PacketSyncKnowledge.STREAM_CODEC, PacketSyncKnowledge::handle);
        reg.playToClient(PacketSyncWarp.TYPE, PacketSyncWarp.STREAM_CODEC, PacketSyncWarp::handle);
        reg.playToClient(PacketWarpMessage.TYPE, PacketWarpMessage.STREAM_CODEC, PacketWarpMessage::handle);
        reg.playToClient(PacketAuraToClient.TYPE, PacketAuraToClient.STREAM_CODEC, PacketAuraToClient::handle);
        reg.playToClient(PacketTileToClient.TYPE, PacketTileToClient.STREAM_CODEC, PacketTileToClient::handle);
        reg.playToClient(PacketFXSlash.TYPE, PacketFXSlash.STREAM_CODEC, PacketFXSlash::handle);
        reg.playToClient(PacketFXBlockArc.TYPE, PacketFXBlockArc.STREAM_CODEC, PacketFXBlockArc::handle);
        reg.playToClient(PacketFXBlockBamf.TYPE, PacketFXBlockBamf.STREAM_CODEC, PacketFXBlockBamf::handle);
        reg.playToClient(PacketFXZap.TYPE, PacketFXZap.STREAM_CODEC, PacketFXZap::handle);
        reg.playToClient(PacketFXEssentiaSource.TYPE, PacketFXEssentiaSource.STREAM_CODEC, PacketFXEssentiaSource::handle);
        reg.playToClient(PacketFXShield.TYPE, PacketFXShield.STREAM_CODEC, PacketFXShield::handle);
        reg.playToClient(PacketFXWispZap.TYPE, PacketFXWispZap.STREAM_CODEC, PacketFXWispZap::handle);
        reg.playToClient(PacketFXFocusEffect.TYPE, PacketFXFocusEffect.STREAM_CODEC, PacketFXFocusEffect::handle);
        reg.playToClient(PacketFXFocusPartImpact.TYPE, PacketFXFocusPartImpact.STREAM_CODEC, PacketFXFocusPartImpact::handle);
        reg.playToClient(PacketFXFocusPartImpactBurst.TYPE, PacketFXFocusPartImpactBurst.STREAM_CODEC, PacketFXFocusPartImpactBurst::handle);
        reg.playToClient(PacketFXInfusionSource.TYPE, PacketFXInfusionSource.STREAM_CODEC, PacketFXInfusionSource::handle);
        reg.playToClient(PacketFXPollute.TYPE, PacketFXPollute.STREAM_CODEC, PacketFXPollute::handle);
        reg.playToClient(PacketFXBoreDig.TYPE, PacketFXBoreDig.STREAM_CODEC, PacketFXBoreDig::handle);
        reg.playToClient(PacketFXScanSource.TYPE, PacketFXScanSource.STREAM_CODEC, PacketFXScanSource::handle);
        reg.playToClient(PacketFXSonic.TYPE, PacketFXSonic.STREAM_CODEC, PacketFXSonic::handle);
        reg.playToClient(PacketFXBlockMist.TYPE, PacketFXBlockMist.STREAM_CODEC, PacketFXBlockMist::handle);
        reg.playToClient(PacketMiscEvent.TYPE, PacketMiscEvent.STREAM_CODEC, PacketMiscEvent::handle);
        reg.playToClient(PacketKnowledgeGain.TYPE, PacketKnowledgeGain.STREAM_CODEC, PacketKnowledgeGain::handle);
        reg.playToClient(PacketSealFilterToClient.TYPE, PacketSealFilterToClient.STREAM_CODEC, PacketSealFilterToClient::handle);
        reg.playToClient(PacketBiomeChange.TYPE, PacketBiomeChange.STREAM_CODEC, PacketBiomeChange::handle);


        // ---- Client -> Server ----
        reg.playToServer(PacketTileToServer.TYPE, PacketTileToServer.STREAM_CODEC, PacketTileToServer::handle);
        reg.playToServer(PacketSyncProgressToServer.TYPE, PacketSyncProgressToServer.STREAM_CODEC, PacketSyncProgressToServer::handle);
        reg.playToServer(PacketSyncResearchFlagsToServer.TYPE, PacketSyncResearchFlagsToServer.STREAM_CODEC, PacketSyncResearchFlagsToServer::handle);
        reg.playToServer(PacketFocusChangeToServer.TYPE, PacketFocusChangeToServer.STREAM_CODEC, PacketFocusChangeToServer::handle);
        reg.playToServer(PacketItemKeyToServer.TYPE, PacketItemKeyToServer.STREAM_CODEC, PacketItemKeyToServer::handle);
        reg.playToServer(PacketFocusNodesToServer.TYPE, PacketFocusNodesToServer.STREAM_CODEC, PacketFocusNodesToServer::handle);
        reg.playToServer(PacketPlayerFlagToServer.TYPE, PacketPlayerFlagToServer.STREAM_CODEC, PacketPlayerFlagToServer::handle);
        reg.playToServer(PacketLogisticsRequestToServer.TYPE, PacketLogisticsRequestToServer.STREAM_CODEC, PacketLogisticsRequestToServer::handle);
        reg.playToServer(PacketMiscStringToServer.TYPE, PacketMiscStringToServer.STREAM_CODEC, PacketMiscStringToServer::handle);
        reg.playToServer(PacketStartTheoryToServer.TYPE, PacketStartTheoryToServer.STREAM_CODEC, PacketStartTheoryToServer::handle);
        reg.playToServer(PacketSelectThaumotoriumRecipeToServer.TYPE, PacketSelectThaumotoriumRecipeToServer.STREAM_CODEC, PacketSelectThaumotoriumRecipeToServer::handle);
        reg.playToServer(PacketFocusNameToServer.TYPE, PacketFocusNameToServer.STREAM_CODEC, PacketFocusNameToServer::handle);
    }

    /** Legacy init hook. No-op: registration is handled by {@link #register(RegisterPayloadHandlersEvent)}. */
    public static void init() {
        Thaumcraft.LOGGER.info("Thaumcraft network registration is handled via RegisterPayloadHandlersEvent");
    }

    // ------------------------------------------------------------------
    // Send helpers (preserve legacy call shapes)
    // ------------------------------------------------------------------

    /** Send a payload to a specific player. */
    public static void sendToPlayer(Object payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, (CustomPacketPayload) payload);
    }

    /** Send a payload to all players in the given dimension. */
    public static void sendToDimension(Object payload, ResourceKey<Level> dimension) {
        ServerLevel level = serverLevelOrNull(dimension);
        if (level == null) {
            return;
        }
        PacketDistributor.sendToPlayersInDimension(level, (CustomPacketPayload) payload);
    }

    /** Send a payload to all players tracking the given entity. */
    public static void sendToAllTracking(Object payload, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, (CustomPacketPayload) payload);
    }

    /** Send a payload to the server (client only). */
    @OnlyIn(Dist.CLIENT)
    public static void sendToServer(Object payload) {
        net.minecraft.client.multiplayer.ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send((CustomPacketPayload) payload);
        }
    }

    /**
     * Send a payload to all players within {@code radius} blocks around {@code pos} in {@code level}.
     * Falls back to "all players in the dimension" when no nearby player exists to anchor the query.
     */
    public static void sendToAllAround(Object payload, ServerLevel level, BlockPos pos, double radius) {
        Optional<ServerPlayer> nearest = level.players().stream().findFirst();
        if (nearest.isEmpty()) {
            // No players in dimension; nothing to reach.
            return;
        }
        final double r2 = radius * radius;
        Optional<ServerPlayer> inRange = level.players().stream()
                .filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= r2)
                .findFirst();
        Optional<ServerPlayer> nearest2 = inRange.isPresent() ? inRange : nearest;
        PacketDistributor.sendToPlayersNear(level, nearest2.get(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius,
                (CustomPacketPayload) payload);
    }

    /** Send a payload to all players tracking the chunk containing {@code pos}. */
    public static void sendToAllTrackingChunk(Object payload, ServerLevel level, BlockPos pos) {
        PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(pos), (CustomPacketPayload) payload);
    }

    private static @Nullable ServerLevel serverLevelOrNull(ResourceKey<Level> dimension) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        return server.getLevel(dimension);
    }
}
