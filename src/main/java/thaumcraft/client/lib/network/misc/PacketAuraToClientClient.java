package thaumcraft.client.lib.network.misc;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.world.aura.AuraChunk;

import thaumcraft.common.lib.network.misc.PacketAuraToClient;

/** Client-side handler for {@link PacketAuraToClient}. */
public class PacketAuraToClientClient {
    @OnlyIn(Dist.CLIENT)
    public static AuraChunk currentAura = null;

    public static void handle(PacketAuraToClient msg) {
        // Store the current aura for HUD display
        currentAura = new AuraChunk(null, msg.base, msg.vis, msg.flux);
    }
}
