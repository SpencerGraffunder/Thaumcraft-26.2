package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import java.util.function.Consumer;



/**
 * PacketFXBoreDig - Visual effect for arcane bore digging blocks.
 * Creates particle streams from the bore to the block being mined,
 * with delayed effects matching the mining progress.
 * 
 * Server -> Client
 */
public class PacketFXBoreDig implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXBoreDig> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxboredig"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXBoreDig> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXBoreDig::encode, PacketFXBoreDig::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final int x;
    public final int y;
    public final int z;
    public final int boreId;
    public final int delay;
    
    public PacketFXBoreDig(BlockPos pos, Entity bore, int delay) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.boreId = bore.getId();
        this.delay = delay;
    }
    
    private PacketFXBoreDig(int x, int y, int z, int boreId, int delay) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.boreId = boreId;
        this.delay = delay;
    }
    
    public static void encode(PacketFXBoreDig packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.x);
        buffer.writeInt(packet.y);
        buffer.writeInt(packet.z);
        buffer.writeInt(packet.boreId);
        buffer.writeInt(packet.delay);
    }
    
    public static PacketFXBoreDig decode(FriendlyByteBuf buffer) {
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        int boreId = buffer.readInt();
        int delay = buffer.readInt();
        return new PacketFXBoreDig(x, y, z, boreId, delay);
    }
    
    public static Consumer<PacketFXBoreDig> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXBoreDig packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
