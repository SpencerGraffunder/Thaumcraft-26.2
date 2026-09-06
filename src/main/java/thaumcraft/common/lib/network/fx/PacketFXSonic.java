package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import java.util.function.Consumer;



/**
 * PacketFXSonic - Sonic boom visual effect.
 * Used by the sonic focus and other sonic-based effects.
 * Creates an expanding ring particle effect around the source entity.
 * 
 * Server -> Client
 */
public class PacketFXSonic implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXSonic> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxsonic"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXSonic> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXSonic::encode, PacketFXSonic::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final int sourceId;
    
    public PacketFXSonic(Entity source) {
        this.sourceId = source.getId();
    }
    
    public PacketFXSonic(int sourceId) {
        this.sourceId = sourceId;
    }
    
    public static void encode(PacketFXSonic packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.sourceId);
    }
    
    public static PacketFXSonic decode(FriendlyByteBuf buffer) {
        int sourceId = buffer.readInt();
        return new PacketFXSonic(sourceId);
    }
    
    public static Consumer<PacketFXSonic> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXSonic packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
