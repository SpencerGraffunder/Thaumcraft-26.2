package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * Packet to spawn a wisp zap lightning effect between two entities.
 * Used when wisps attack their targets.
 * 
 * The color of the zap is based on the wisp's aspect type.
 * 
 * Ported to 1.20.1
 */
public class PacketFXWispZap implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXWispZap> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxwispzap"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXWispZap> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXWispZap::encode, PacketFXWispZap::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final int sourceEntityId;
    public final int targetEntityId;
    
    public PacketFXWispZap(int sourceEntityId, int targetEntityId) {
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
    }
    
    public static void encode(PacketFXWispZap packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.sourceEntityId);
        buf.writeInt(packet.targetEntityId);
    }
    
    public static PacketFXWispZap decode(FriendlyByteBuf buf) {
        return new PacketFXWispZap(buf.readInt(), buf.readInt());
    }
    
    public static Consumer<PacketFXWispZap> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXWispZap packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
