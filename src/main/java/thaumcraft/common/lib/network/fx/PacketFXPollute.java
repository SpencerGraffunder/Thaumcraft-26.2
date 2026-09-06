package thaumcraft.common.lib.network.fx;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Consumer;



/**
 * PacketFXPollute - Visual effect for aura pollution.
 * Shows purple smoke/particles when flux is added to the aura.
 * 
 * Server -> Client
 */
public class PacketFXPollute implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXPollute> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxpollute"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXPollute> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXPollute::encode, PacketFXPollute::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final int x;
    public final int y;
    public final int z;
    public final byte amount;
    
    public PacketFXPollute(BlockPos pos, float amt) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        // Minimum amount of 1 if any pollution
        if (amt < 1.0f && amt > 0.0f) {
            amt = 1.0f;
        }
        this.amount = (byte) Math.min(amt, 127);
    }
    
    private PacketFXPollute(int x, int y, int z, byte amount) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.amount = amount;
    }
    
    public static void encode(PacketFXPollute packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.x);
        buffer.writeInt(packet.y);
        buffer.writeInt(packet.z);
        buffer.writeByte(packet.amount);
    }
    
    public static PacketFXPollute decode(FriendlyByteBuf buffer) {
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        byte amount = buffer.readByte();
        return new PacketFXPollute(x, y, z, amount);
    }
    
    public static Consumer<PacketFXPollute> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXPollute packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
