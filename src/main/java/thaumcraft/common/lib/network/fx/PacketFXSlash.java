package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import java.util.function.Consumer;



/**
 * PacketFXSlash - Sends slash visual effect between two entities.
 * Used by ARCING infusion enchantment to show chain lightning.
 * 
 * Server -> Client
 */
public class PacketFXSlash implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXSlash> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxslash"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXSlash> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXSlash::encode, PacketFXSlash::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final int sourceId;
    public final int targetId;
    
    public PacketFXSlash(int sourceId, int targetId) {
        this.sourceId = sourceId;
        this.targetId = targetId;
    }
    
    public PacketFXSlash(Entity source, Entity target) {
        this.sourceId = source.getId();
        this.targetId = target.getId();
    }
    
    public static void encode(PacketFXSlash packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.sourceId);
        buffer.writeInt(packet.targetId);
    }
    
    public static PacketFXSlash decode(FriendlyByteBuf buffer) {
        return new PacketFXSlash(buffer.readInt(), buffer.readInt());
    }
    
    public static Consumer<PacketFXSlash> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXSlash packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
