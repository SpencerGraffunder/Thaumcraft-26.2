package thaumcraft.client.lib.network.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.FXDispatcher;

import thaumcraft.common.lib.network.fx.PacketFXPollute;

/** Client-side handler for {@link PacketFXPollute}. */
public class PacketFXPolluteClient {
    public static void handle(PacketFXPollute msg) {
        BlockPos pos = new BlockPos(msg.x, msg.y, msg.z);
        // Draw pollution particles - cap at 40 to avoid performance issues
        int particleCount = Math.min(40, Math.abs(msg.amount));
        for (int a = 0; a < particleCount; a++) {
            FXDispatcher.INSTANCE.drawPollutionParticles(pos);
        }
    }
}
