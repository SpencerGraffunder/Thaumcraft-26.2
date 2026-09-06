package thaumcraft.client.lib.network;

import thaumcraft.client.lib.network.fx.PacketFXBlockArcClient;
import thaumcraft.client.lib.network.fx.PacketFXBlockBamfClient;
import thaumcraft.client.lib.network.fx.PacketFXBlockMistClient;
import thaumcraft.client.lib.network.fx.PacketFXBoreDigClient;
import thaumcraft.client.lib.network.fx.PacketFXEssentiaSourceClient;
import thaumcraft.client.lib.network.fx.PacketFXFocusEffectClient;
import thaumcraft.client.lib.network.fx.PacketFXFocusPartImpactBurstClient;
import thaumcraft.client.lib.network.fx.PacketFXFocusPartImpactClient;
import thaumcraft.client.lib.network.fx.PacketFXInfusionSourceClient;
import thaumcraft.client.lib.network.fx.PacketFXPolluteClient;
import thaumcraft.client.lib.network.fx.PacketFXScanSourceClient;
import thaumcraft.client.lib.network.fx.PacketFXShieldClient;
import thaumcraft.client.lib.network.fx.PacketFXSlashClient;
import thaumcraft.client.lib.network.fx.PacketFXSonicClient;
import thaumcraft.client.lib.network.fx.PacketFXWispZapClient;
import thaumcraft.client.lib.network.fx.PacketFXZapClient;
import thaumcraft.client.lib.network.misc.PacketAuraToClientClient;
import thaumcraft.client.lib.network.misc.PacketBiomeChangeClient;
import thaumcraft.client.lib.network.misc.PacketKnowledgeGainClient;
import thaumcraft.client.lib.network.misc.PacketMiscEventClient;
import thaumcraft.client.lib.network.misc.PacketSealFilterToClientClient;
import thaumcraft.client.lib.network.misc.PacketSealToClientClient;
import thaumcraft.client.lib.network.playerdata.PacketSyncKnowledgeClient;
import thaumcraft.client.lib.network.playerdata.PacketSyncWarpClient;
import thaumcraft.client.lib.network.playerdata.PacketWarpMessageClient;
import thaumcraft.client.lib.network.tiles.PacketTileToClientClient;
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
import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.lib.network.misc.PacketBiomeChange;
import thaumcraft.common.lib.network.misc.PacketKnowledgeGain;
import thaumcraft.common.lib.network.misc.PacketMiscEvent;
import thaumcraft.common.lib.network.misc.PacketSealFilterToClient;
import thaumcraft.common.lib.network.misc.PacketSealToClient;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;
import thaumcraft.common.lib.network.playerdata.PacketSyncWarp;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;
import thaumcraft.common.lib.network.tiles.PacketTileToClient;

/**
 * Wires client-side packet handlers for all server-bound-to-client payloads.
 * Called once during client mod setup; safe to call only on the client dist.
 */
public class PacketClientWiring {

    public static void init() {
        PacketSealToClient.CLIENT_HANDLER = PacketSealToClientClient::handle;
        PacketSyncKnowledge.CLIENT_HANDLER = PacketSyncKnowledgeClient::handle;
        PacketSyncWarp.CLIENT_HANDLER = PacketSyncWarpClient::handle;
        PacketWarpMessage.CLIENT_HANDLER = PacketWarpMessageClient::handle;
        PacketAuraToClient.CLIENT_HANDLER = PacketAuraToClientClient::handle;
        PacketTileToClient.CLIENT_HANDLER = PacketTileToClientClient::handle;
        PacketFXSlash.CLIENT_HANDLER = PacketFXSlashClient::handle;
        PacketFXBlockArc.CLIENT_HANDLER = PacketFXBlockArcClient::handle;
        PacketFXBlockBamf.CLIENT_HANDLER = PacketFXBlockBamfClient::handle;
        PacketFXZap.CLIENT_HANDLER = PacketFXZapClient::handle;
        PacketFXEssentiaSource.CLIENT_HANDLER = PacketFXEssentiaSourceClient::handle;
        PacketFXShield.CLIENT_HANDLER = PacketFXShieldClient::handle;
        PacketFXWispZap.CLIENT_HANDLER = PacketFXWispZapClient::handle;
        PacketFXFocusEffect.CLIENT_HANDLER = PacketFXFocusEffectClient::handle;
        PacketFXFocusPartImpact.CLIENT_HANDLER = PacketFXFocusPartImpactClient::handle;
        PacketFXFocusPartImpactBurst.CLIENT_HANDLER = PacketFXFocusPartImpactBurstClient::handle;
        PacketFXInfusionSource.CLIENT_HANDLER = PacketFXInfusionSourceClient::handle;
        PacketFXPollute.CLIENT_HANDLER = PacketFXPolluteClient::handle;
        PacketFXBoreDig.CLIENT_HANDLER = PacketFXBoreDigClient::handle;
        PacketFXScanSource.CLIENT_HANDLER = PacketFXScanSourceClient::handle;
        PacketFXSonic.CLIENT_HANDLER = PacketFXSonicClient::handle;
        PacketFXBlockMist.CLIENT_HANDLER = PacketFXBlockMistClient::handle;
        PacketMiscEvent.CLIENT_HANDLER = PacketMiscEventClient::handle;
        PacketKnowledgeGain.CLIENT_HANDLER = PacketKnowledgeGainClient::handle;
        PacketSealFilterToClient.CLIENT_HANDLER = PacketSealFilterToClientClient::handle;
        PacketBiomeChange.CLIENT_HANDLER = PacketBiomeChangeClient::handle;
    }
}
