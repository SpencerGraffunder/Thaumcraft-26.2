package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * PacketFXBlockBamf - Teleport/poof visual effect at a location.
 * Used for teleportation, warp effects, and magical displacement.
 * 
 * Server -> Client
 */
public class PacketFXBlockBamf implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXBlockBamf> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxblockbamf"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXBlockBamf> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXBlockBamf::encode, PacketFXBlockBamf::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final double x;
    public final double y;
    public final double z;
    public final int color;
    public final byte flags;
    public final byte face;
    
    public PacketFXBlockBamf(double x, double y, double z, int color, boolean sound, boolean flair, Direction side) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        int f = 0;
        if (sound) f = setBit(f, 0);
        if (flair) f = setBit(f, 1);
        this.face = (side != null) ? (byte) side.ordinal() : -1;
        this.flags = (byte) f;
    }
    
    public PacketFXBlockBamf(BlockPos pos, int color, boolean sound, boolean flair, Direction side) {
        this(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, color, sound, flair, side);
    }
    
    private PacketFXBlockBamf(double x, double y, double z, int color, byte flags, byte face) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.flags = flags;
        this.face = face;
    }
    
    public static void encode(PacketFXBlockBamf packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeInt(packet.color);
        buffer.writeByte(packet.flags);
        buffer.writeByte(packet.face);
    }
    
    public static PacketFXBlockBamf decode(FriendlyByteBuf buffer) {
        return new PacketFXBlockBamf(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readByte(),
                buffer.readByte()
        );
    }
    
    public static Consumer<PacketFXBlockBamf> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXBlockBamf packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
    
    // Bit manipulation helpers
    private static int setBit(int value, int bit) {
        return value | (1 << bit);
    }
    
    public static boolean getBit(int value, int bit) {
        return (value & (1 << bit)) != 0;
    }
}
