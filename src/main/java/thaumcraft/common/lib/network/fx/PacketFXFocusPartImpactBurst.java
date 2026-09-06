package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * Packet to spawn a burst of focus impact particles at a specific location.
 * Similar to PacketFXFocusPartImpact but with more spread/velocity for
 * dramatic explosion effects.
 * 
 * Used when a focus spell creates a large impact (e.g., explosion effects).
 * 
 * Ported to 1.20.1
 */
public class PacketFXFocusPartImpactBurst implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXFocusPartImpactBurst> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxfocuspartimpactburst"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXFocusPartImpactBurst> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXFocusPartImpactBurst::encode, PacketFXFocusPartImpactBurst::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final float x, y, z;
    public final String parts;
    
    public PacketFXFocusPartImpactBurst(double x, double y, double z, String[] parts) {
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
    
    private PacketFXFocusPartImpactBurst(float x, float y, float z, String parts) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.parts = parts;
    }
    
    public static void encode(PacketFXFocusPartImpactBurst packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.x);
        buf.writeFloat(packet.y);
        buf.writeFloat(packet.z);
        buf.writeUtf(packet.parts);
    }
    
    public static PacketFXFocusPartImpactBurst decode(FriendlyByteBuf buf) {
        return new PacketFXFocusPartImpactBurst(
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readUtf(32767)
        );
    }
    
    public static Consumer<PacketFXFocusPartImpactBurst> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXFocusPartImpactBurst packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
