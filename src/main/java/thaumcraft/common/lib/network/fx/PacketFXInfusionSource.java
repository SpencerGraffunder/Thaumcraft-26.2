package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * PacketFXInfusionSource - Visual effect for infusion crafting.
 * Shows the stream of essentia/items flowing to the infusion matrix.
 * 
 * Server -> Client
 */
public class PacketFXInfusionSource implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXInfusionSource> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxinfusionsource"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXInfusionSource> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXInfusionSource::encode, PacketFXInfusionSource::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final long p1;
    public final long p2;
    public final int color;
    
    public PacketFXInfusionSource(BlockPos matrixPos, BlockPos sourcePos, int color) {
        this.p1 = matrixPos.asLong();
        this.p2 = sourcePos.asLong();
        this.color = color;
    }
    
    private PacketFXInfusionSource(long p1, long p2, int color) {
        this.p1 = p1;
        this.p2 = p2;
        this.color = color;
    }
    
    public static void encode(PacketFXInfusionSource packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.p1);
        buffer.writeLong(packet.p2);
        buffer.writeInt(packet.color);
    }
    
    public static PacketFXInfusionSource decode(FriendlyByteBuf buffer) {
        long p1 = buffer.readLong();
        long p2 = buffer.readLong();
        int color = buffer.readInt();
        return new PacketFXInfusionSource(p1, p2, color);
    }
    
    public static Consumer<PacketFXInfusionSource> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXInfusionSource packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
