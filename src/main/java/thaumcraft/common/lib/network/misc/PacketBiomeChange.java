package thaumcraft.common.lib.network.misc;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import java.util.function.Consumer;



/**
 * Packet to update biome at a specific position on the client.
 * Used for Taint spreading, biome transformation effects, etc.
 * 
 * In 1.20.1, biomes are stored per-section and use ResourceKeys instead of IDs.
 * 
 * Ported to 1.20.1
 */
public class PacketBiomeChange implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketBiomeChange> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetbiomechange"));

    public static final StreamCodec<FriendlyByteBuf, PacketBiomeChange> STREAM_CODEC =
        StreamCodec.ofMember(PacketBiomeChange::encode, PacketBiomeChange::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final int x;
    public final int y;
    public final int z;
    public final String biomeId;
    
    public PacketBiomeChange(BlockPos pos, ResourceKey<Biome> biome) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.biomeId = biome.identifier().toString();
    }
    
    public PacketBiomeChange(int x, int y, int z, String biomeId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.biomeId = biomeId;
    }
    
    public static void encode(PacketBiomeChange packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.x);
        buf.writeInt(packet.y);
        buf.writeInt(packet.z);
        buf.writeUtf(packet.biomeId);
    }
    
    public static PacketBiomeChange decode(FriendlyByteBuf buf) {
        return new PacketBiomeChange(
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readUtf(32767)
        );
    }
    
    public static Consumer<PacketBiomeChange> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketBiomeChange packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
