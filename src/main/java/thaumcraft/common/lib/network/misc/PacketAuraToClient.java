package thaumcraft.common.lib.network.misc;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import thaumcraft.common.world.aura.AuraChunk;
import java.util.function.Consumer;



/**
 * PacketAuraToClient - Syncs aura data for a chunk from server to client.
 * 
 * Sent when:
 * - Player enters a new chunk
 * - Aura values change significantly
 * - Player uses a thaumometer or similar device
 * 
 * Ported from 1.12.2
 */
public class PacketAuraToClient implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketAuraToClient> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetauratoclient"));

    public static final StreamCodec<FriendlyByteBuf, PacketAuraToClient> STREAM_CODEC =
        StreamCodec.ofMember(PacketAuraToClient::encode, PacketAuraToClient::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public short base;
    public float vis;
    public float flux;
    
    // Client-side storage for current aura display
    
    public PacketAuraToClient() {
    }
    
    public PacketAuraToClient(AuraChunk auraChunk) {
        this.base = auraChunk.getBase();
        this.vis = auraChunk.getVis();
        this.flux = auraChunk.getFlux();
    }
    
    public PacketAuraToClient(short base, float vis, float flux) {
        this.base = base;
        this.vis = vis;
        this.flux = flux;
    }
    
    public static void encode(PacketAuraToClient msg, FriendlyByteBuf buf) {
        buf.writeShort(msg.base);
        buf.writeFloat(msg.vis);
        buf.writeFloat(msg.flux);
    }
    
    public static PacketAuraToClient decode(FriendlyByteBuf buf) {
        PacketAuraToClient msg = new PacketAuraToClient();
        msg.base = buf.readShort();
        msg.vis = buf.readFloat();
        msg.flux = buf.readFloat();
        return msg;
    }
    
    public static Consumer<PacketAuraToClient> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketAuraToClient msg, IPayloadContext ctxSupplier) {
        IPayloadContext ctx = ctxSupplier;
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(msg));
    }
    
}
