package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import java.util.function.Consumer;



/**
 * Packet to spawn shield rune visual effects on an entity.
 * Used when runic shielding blocks damage.
 * 
 * Target values:
 * - >= 0: Entity ID of attacker (shield faces attacker)
 * - -1: Shield above and below (fall damage, etc.)
 * - -2: Shield below only
 * - -3: Shield above only
 * 
 * Ported to 1.20.1
 */
public class PacketFXShield implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXShield> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxshield"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXShield> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXShield::encode, PacketFXShield::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final int sourceEntityId;
    public final int targetEntityId;
    
    public PacketFXShield(int sourceEntityId, int targetEntityId) {
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
    }
    
    public static void encode(PacketFXShield packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.sourceEntityId);
        buf.writeInt(packet.targetEntityId);
    }
    
    public static PacketFXShield decode(FriendlyByteBuf buf) {
        return new PacketFXShield(buf.readInt(), buf.readInt());
    }
    
    public static Consumer<PacketFXShield> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXShield packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
