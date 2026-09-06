package thaumcraft.client.lib.network.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.FXDispatcher;

import thaumcraft.common.lib.network.fx.PacketFXZap;

/** Client-side handler for {@link PacketFXZap}. */
public class PacketFXZapClient {
    public static void handle(PacketFXZap msg) {
        // Extract RGB from color integer
        int color = msg.color;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        FXDispatcher.INSTANCE.arcBolt(
                msg.source.x, msg.source.y, msg.source.z,
                msg.target.x, msg.target.y, msg.target.z,
                r, g, b,
                msg.width);
    }
}
