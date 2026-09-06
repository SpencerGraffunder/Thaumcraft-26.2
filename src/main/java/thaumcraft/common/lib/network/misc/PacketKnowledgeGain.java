package thaumcraft.common.lib.network.misc;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import java.util.function.Consumer;



/**
 * Packet sent to client when player gains knowledge (observation, theory, etc).
 * Triggers the knowledge gain visual effect and sound on the client.
 * 
 * Ported to 1.20.1
 */
public class PacketKnowledgeGain implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketKnowledgeGain> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetknowledgegain"));

    public static final StreamCodec<FriendlyByteBuf, PacketKnowledgeGain> STREAM_CODEC =
        StreamCodec.ofMember(PacketKnowledgeGain::encode, PacketKnowledgeGain::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final byte type;
    public final String category;
    
    public PacketKnowledgeGain(IPlayerKnowledge.EnumKnowledgeType type, String category) {
        this.type = (byte) type.ordinal();
        this.category = (category == null) ? "" : category;
    }
    
    private PacketKnowledgeGain(byte type, String category) {
        this.type = type;
        this.category = category;
    }
    
    public static void encode(PacketKnowledgeGain packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.type);
        buf.writeUtf(packet.category);
    }
    
    public static PacketKnowledgeGain decode(FriendlyByteBuf buf) {
        return new PacketKnowledgeGain(
            buf.readByte(),
            buf.readUtf(32767)
        );
    }
    
    public static Consumer<PacketKnowledgeGain> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketKnowledgeGain packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
