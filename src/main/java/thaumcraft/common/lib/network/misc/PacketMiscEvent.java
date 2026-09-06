package thaumcraft.common.lib.network.misc;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * Packet for miscellaneous client-side events.
 * Used for warp effects, mist/fog effects, etc.
 * 
 * Ported to 1.20.1
 */
public class PacketMiscEvent implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketMiscEvent> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetmiscevent"));

    public static final StreamCodec<FriendlyByteBuf, PacketMiscEvent> STREAM_CODEC =
        StreamCodec.ofMember(PacketMiscEvent::encode, PacketMiscEvent::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    // Event type constants
    public static final byte WARP_EVENT = 0;
    public static final byte MIST_EVENT = 1;
    public static final byte MIST_EVENT_SHORT = 2;
    
    public final byte type;
    public final int value;
    
    public PacketMiscEvent(byte type) {
        this.type = type;
        this.value = 0;
    }
    
    public PacketMiscEvent(byte type, int value) {
        this.type = type;
        this.value = value;
    }
    
    public static void encode(PacketMiscEvent packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.type);
        if (packet.value != 0) {
            buf.writeInt(packet.value);
        }
    }
    
    public static PacketMiscEvent decode(FriendlyByteBuf buf) {
        byte type = buf.readByte();
        int value = 0;
        if (buf.isReadable()) {
            value = buf.readInt();
        }
        return new PacketMiscEvent(type, value);
    }
    
    public static Consumer<PacketMiscEvent> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketMiscEvent packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
