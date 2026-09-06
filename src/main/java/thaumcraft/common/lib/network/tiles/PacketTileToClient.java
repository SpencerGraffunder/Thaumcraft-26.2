package thaumcraft.common.lib.network.tiles;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * PacketTileToClient - Sends custom tile entity messages from server to client.
 * 
 * Used for:
 * - Custom animations
 * - State changes that need immediate visual feedback
 * - Multi-step processes
 * 
 * Ported from 1.12.2
 */
public class PacketTileToClient implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketTileToClient> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packettiletoclient"));

    public static final StreamCodec<FriendlyByteBuf, PacketTileToClient> STREAM_CODEC =
        StreamCodec.ofMember(PacketTileToClient::encode, PacketTileToClient::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public long pos;
    public CompoundTag nbt;
    
    public PacketTileToClient() {
    }
    
    public PacketTileToClient(BlockPos pos, CompoundTag nbt) {
        this.pos = pos.asLong();
        this.nbt = nbt;
    }
    
    public static void encode(PacketTileToClient msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.pos);
        buf.writeNbt(msg.nbt);
    }
    
    public static PacketTileToClient decode(FriendlyByteBuf buf) {
        PacketTileToClient msg = new PacketTileToClient();
        msg.pos = buf.readLong();
        msg.nbt = buf.readNbt();
        return msg;
    }
    
    public static Consumer<PacketTileToClient> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketTileToClient msg, IPayloadContext ctxSupplier) {
        IPayloadContext ctx = ctxSupplier;
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(msg));
    }
    
}
