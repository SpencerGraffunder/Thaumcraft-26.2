package thaumcraft.client.lib.network.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.FXDispatcher;

import thaumcraft.common.lib.network.fx.PacketFXBlockBamf;

/** Client-side handler for {@link PacketFXBlockBamf}. */
public class PacketFXBlockBamfClient {
    public static void handle(PacketFXBlockBamf msg) {
        Direction side = null;
        if (msg.face >= 0 && msg.face < Direction.values().length) {
            side = Direction.values()[msg.face];
        }
        
        boolean sound = PacketFXBlockBamf.getBit(msg.flags, 0);
        boolean flair = PacketFXBlockBamf.getBit(msg.flags, 1);
        
        if (msg.color != -9999) {
            FXDispatcher.INSTANCE.drawBamf(msg.x, msg.y, msg.z, msg.color, sound, flair, side);
        } else {
            FXDispatcher.INSTANCE.drawBamf(msg.x, msg.y, msg.z, sound, flair, side);
        }
    }
}
