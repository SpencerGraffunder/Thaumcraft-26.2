package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * PacketFXEssentiaSource - Essentia stream/flow visual effect.
 * Used when essentia is being transported through tubes or drawn from containers.
 * 
 * Server -> Client
 */
public class PacketFXEssentiaSource implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXEssentiaSource> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxessentiasource"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXEssentiaSource> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXEssentiaSource::encode, PacketFXEssentiaSource::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final int x;
    public final int y;
    public final int z;
    public final byte dx;
    public final byte dy;
    public final byte dz;
    public final int color;
    public final int ext;
    
    public PacketFXEssentiaSource(BlockPos source, byte dx, byte dy, byte dz, int color, int ext) {
        this.x = source.getX();
        this.y = source.getY();
        this.z = source.getZ();
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.color = color;
        this.ext = ext;
    }
    
    public PacketFXEssentiaSource(BlockPos source, BlockPos target, int color, int ext) {
        this(source, 
             (byte)(source.getX() - target.getX()),
             (byte)(source.getY() - target.getY()),
             (byte)(source.getZ() - target.getZ()),
             color, ext);
    }
    
    private PacketFXEssentiaSource(int x, int y, int z, byte dx, byte dy, byte dz, int color, int ext) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.color = color;
        this.ext = ext;
    }
    
    public static void encode(PacketFXEssentiaSource packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.x);
        buffer.writeInt(packet.y);
        buffer.writeInt(packet.z);
        buffer.writeInt(packet.color);
        buffer.writeByte(packet.dx);
        buffer.writeByte(packet.dy);
        buffer.writeByte(packet.dz);
        buffer.writeShort(packet.ext);
    }
    
    public static PacketFXEssentiaSource decode(FriendlyByteBuf buffer) {
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        int color = buffer.readInt();
        byte dx = buffer.readByte();
        byte dy = buffer.readByte();
        byte dz = buffer.readByte();
        int ext = buffer.readShort();
        return new PacketFXEssentiaSource(x, y, z, dx, dy, dz, color, ext);
    }
    
    public static Consumer<PacketFXEssentiaSource> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXEssentiaSource packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
