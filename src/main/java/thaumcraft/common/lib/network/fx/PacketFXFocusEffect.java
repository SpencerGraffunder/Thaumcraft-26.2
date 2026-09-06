package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * Packet to spawn focus effect particles along a trajectory.
 * Used when a focus spell is traveling through the air.
 * 
 * Multiple effect parts can be combined (e.g., fire + air creates
 * a fiery wind effect).
 * 
 * Ported to 1.20.1
 */
public class PacketFXFocusEffect implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXFocusEffect> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxfocuseffect"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXFocusEffect> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXFocusEffect::encode, PacketFXFocusEffect::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final float x, y, z;
    public final float motionX, motionY, motionZ;
    public final String parts;
    
    public PacketFXFocusEffect(float x, float y, float z, float motionX, float motionY, float motionZ, String[] parts) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        
        // Join parts with % separator
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append("%");
            sb.append(parts[i]);
        }
        this.parts = sb.toString();
    }
    
    private PacketFXFocusEffect(float x, float y, float z, float motionX, float motionY, float motionZ, String parts) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.parts = parts;
    }
    
    public static void encode(PacketFXFocusEffect packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.x);
        buf.writeFloat(packet.y);
        buf.writeFloat(packet.z);
        buf.writeFloat(packet.motionX);
        buf.writeFloat(packet.motionY);
        buf.writeFloat(packet.motionZ);
        buf.writeUtf(packet.parts);
    }
    
    public static PacketFXFocusEffect decode(FriendlyByteBuf buf) {
        return new PacketFXFocusEffect(
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readUtf(32767)
        );
    }
    
    public static Consumer<PacketFXFocusEffect> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXFocusEffect packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
