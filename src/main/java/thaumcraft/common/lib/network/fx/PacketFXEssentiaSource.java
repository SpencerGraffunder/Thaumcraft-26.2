package thaumcraft.common.lib.network.fx;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.FXDispatcher;


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

    
    private final int x;
    private final int y;
    private final int z;
    private final byte dx;
    private final byte dy;
    private final byte dz;
    private final int color;
    private final int ext;
    
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
    
    public static void handle(PacketFXEssentiaSource packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(packet);
        });
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXEssentiaSource packet) {
        int tx = packet.x - packet.dx;
        int ty = packet.y - packet.dy;
        int tz = packet.z - packet.dz;
        
        // Draw essentia trail from source to target
        BlockPos source = new BlockPos(packet.x, packet.y, packet.z);
        BlockPos target = new BlockPos(tx, ty, tz);
        
        FXDispatcher.INSTANCE.essentiaTrailFx(source, target, 1, packet.color, 0.1f, packet.ext);
    }
}
