package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * PacketFXScanSource - Visual effect for ore/resource scanning.
 * Used by the Thaumometer's ore scan mode to highlight ore veins.
 * Groups adjacent ores of the same type and displays colored particles
 * at the center of each group.
 * 
 * Server -> Client
 */
public class PacketFXScanSource implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXScanSource> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxscansource"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXScanSource> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXScanSource::encode, PacketFXScanSource::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    
    public final long loc;
    public final int size;
    
    public PacketFXScanSource(BlockPos pos, int size) {
        this.loc = pos.asLong();
        this.size = size;
    }
    
    private PacketFXScanSource(long loc, int size) {
        this.loc = loc;
        this.size = size;
    }
    
    public static void encode(PacketFXScanSource packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.loc);
        buffer.writeByte(packet.size);
    }
    
    public static PacketFXScanSource decode(FriendlyByteBuf buffer) {
        long loc = buffer.readLong();
        int size = buffer.readByte();
        return new PacketFXScanSource(loc, size);
    }
    
    public static Consumer<PacketFXScanSource> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXScanSource packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
