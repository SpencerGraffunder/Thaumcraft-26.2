package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import java.util.function.Consumer;



/**
 * PacketFXZap - Electric arc/bolt visual effect between two points.
 * Used for shock effects, wand zaps, and electrical damage.
 * 
 * Server -> Client
 */
public class PacketFXZap implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXZap> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxzap"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXZap> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXZap::encode, PacketFXZap::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final Vec3 source;
    public final Vec3 target;
    public final int color;
    public final float width;
    
    public PacketFXZap(Vec3 source, Vec3 target, int color, float width) {
        this.source = source;
        this.target = target;
        this.color = color;
        this.width = width;
    }
    
    public PacketFXZap(double sx, double sy, double sz, double tx, double ty, double tz, int color, float width) {
        this(new Vec3(sx, sy, sz), new Vec3(tx, ty, tz), color, width);
    }
    
    public static void encode(PacketFXZap packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.source.x);
        buffer.writeDouble(packet.source.y);
        buffer.writeDouble(packet.source.z);
        buffer.writeDouble(packet.target.x);
        buffer.writeDouble(packet.target.y);
        buffer.writeDouble(packet.target.z);
        buffer.writeInt(packet.color);
        buffer.writeFloat(packet.width);
    }
    
    public static PacketFXZap decode(FriendlyByteBuf buffer) {
        return new PacketFXZap(
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readInt(),
                buffer.readFloat()
        );
    }
    
    public static Consumer<PacketFXZap> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXZap packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
