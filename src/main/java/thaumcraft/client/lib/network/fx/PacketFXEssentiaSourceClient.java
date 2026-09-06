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

import thaumcraft.common.lib.network.fx.PacketFXEssentiaSource;

/** Client-side handler for {@link PacketFXEssentiaSource}. */
public class PacketFXEssentiaSourceClient {
    public static void handle(PacketFXEssentiaSource msg) {
        int tx = msg.x - msg.dx;
        int ty = msg.y - msg.dy;
        int tz = msg.z - msg.dz;
        
        // Draw essentia trail from source to target
        BlockPos source = new BlockPos(msg.x, msg.y, msg.z);
        BlockPos target = new BlockPos(tx, ty, tz);
        
        FXDispatcher.INSTANCE.essentiaTrailFx(source, target, 1, msg.color, 0.1f, msg.ext);
    }
}
