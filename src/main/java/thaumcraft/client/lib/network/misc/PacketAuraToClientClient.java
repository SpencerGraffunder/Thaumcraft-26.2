package thaumcraft.client.lib.network.misc;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.lib.events.HudHandler;
import thaumcraft.common.world.aura.AuraChunk;

import thaumcraft.common.lib.network.misc.PacketAuraToClient;

/**
 * Client-side handler for {@link PacketAuraToClient}.
 *
 * <p>1.12 PacketAuraToClient.onMessage writes the pushed aura straight into
 * {@code HudHandler.currentAura}, which is the single source the thaumometer and caster
 * HUD gauges read. Handing this packet to anything else left the gauge permanently empty.
 */
public class PacketAuraToClientClient {
    @OnlyIn(Dist.CLIENT)
    public static AuraChunk currentAura = null;

    public static void handle(PacketAuraToClient msg) {
        currentAura = new AuraChunk(null, msg.base, msg.vis, msg.flux);
        HudHandler.updateAura(currentAura);
    }
}
