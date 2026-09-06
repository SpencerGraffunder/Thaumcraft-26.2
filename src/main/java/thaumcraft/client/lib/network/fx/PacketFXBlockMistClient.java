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

import thaumcraft.common.lib.network.fx.PacketFXBlockMist;

/** Client-side handler for {@link PacketFXBlockMist}. */
public class PacketFXBlockMistClient {
    public static void handle(PacketFXBlockMist msg) {
        BlockPos pos = BlockPos.of(msg.loc);
        FXDispatcher.INSTANCE.drawBlockMistParticles(pos, msg.color);
    }
}
