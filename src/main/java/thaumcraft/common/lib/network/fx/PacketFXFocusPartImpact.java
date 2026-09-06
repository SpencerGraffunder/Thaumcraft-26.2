package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * Packet to spawn focus impact particles at a specific location.
 * Used when a focus spell hits a block or entity.
 * 
 * Creates a burst of particles at the impact point, spreading outward.
 * 
 * Ported to 1.20.1
 */
public class PacketFXFocusPartImpact implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXFocusPartImpact> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxfocuspartimpact"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXFocusPartImpact> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXFocusPartImpact::encode, PacketFXFocusPartImpact::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final float x, y, z;
    public final String parts;
    
    public PacketFXFocusPartImpact(double x, double y, double z, String[] parts) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        
        // Join parts with % separator
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append("%");
            sb.append(parts[i]);
        }
        this.parts = sb.toString();
    }
    
    private PacketFXFocusPartImpact(float x, float y, float z, String parts) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.parts = parts;
    }
    
    public static void encode(PacketFXFocusPartImpact packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.x);
        buf.writeFloat(packet.y);
        buf.writeFloat(packet.z);
        buf.writeUtf(packet.parts);
    }
    
    public static PacketFXFocusPartImpact decode(FriendlyByteBuf buf) {
        return new PacketFXFocusPartImpact(
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readUtf(32767)
        );
    }
    
    public static Consumer<PacketFXFocusPartImpact> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXFocusPartImpact packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
