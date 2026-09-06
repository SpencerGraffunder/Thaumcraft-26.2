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
 * PacketFXBlockArc - Sends arc lightning visual effect from a block to a target.
 * Used for essentia transport, infusion, and various magical effects.
 * 
 * Server -> Client
 */
public class PacketFXBlockArc implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXBlockArc> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxblockarc"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXBlockArc> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXBlockArc::encode, PacketFXBlockArc::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public final int x;
    public final int y;
    public final int z;
    public final float tx;
    public final float ty;
    public final float tz;
    public final float r;
    public final float g;
    public final float b;
    
    public PacketFXBlockArc(BlockPos pos, Entity target, float r, float g, float b) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.tx = (float) target.getX();
        this.ty = (float) (target.getBoundingBox().minY + target.getBbHeight() / 2.0f);
        this.tz = (float) target.getZ();
        this.r = r;
        this.g = g;
        this.b = b;
    }
    
    public PacketFXBlockArc(BlockPos pos, BlockPos target, float r, float g, float b) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.tx = target.getX() + 0.5f;
        this.ty = target.getY() + 0.5f;
        this.tz = target.getZ() + 0.5f;
        this.r = r;
        this.g = g;
        this.b = b;
    }
    
    public PacketFXBlockArc(int x, int y, int z, float tx, float ty, float tz, float r, float g, float b) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
        this.r = r;
        this.g = g;
        this.b = b;
    }
    
    public static void encode(PacketFXBlockArc packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.x);
        buffer.writeInt(packet.y);
        buffer.writeInt(packet.z);
        buffer.writeFloat(packet.tx);
        buffer.writeFloat(packet.ty);
        buffer.writeFloat(packet.tz);
        buffer.writeFloat(packet.r);
        buffer.writeFloat(packet.g);
        buffer.writeFloat(packet.b);
    }
    
    public static PacketFXBlockArc decode(FriendlyByteBuf buffer) {
        return new PacketFXBlockArc(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }
    
    public static Consumer<PacketFXBlockArc> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXBlockArc packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
