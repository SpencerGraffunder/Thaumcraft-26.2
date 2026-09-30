package thaumcraft.common.lib.network.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

/**
 * PacketFXCrucible - 1.12 crucible block-event replacement.
 * The 1.12 port used world.addBlockEvent(pos, crucible, type, data) for crucible FX;
 * 26.3 has no block-event system, so this packet carries the same (type, data) pairs:
 *
 *   type 99: craft done  -> bamf above crucible + spill sound
 *   type  1: item dropped into crucible -> bamf on the block
 *   type  2: dissolve/boil (data = 1) or full spill (data = 5) -> 10x crucibleBoil + spill sound
 *
 * Server -> Client
 */
public class PacketFXCrucible implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXCrucible> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetfxcrucible"));

    public static final StreamCodec<FriendlyByteBuf, PacketFXCrucible> STREAM_CODEC =
        StreamCodec.ofMember(PacketFXCrucible::encode, PacketFXCrucible::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    public final BlockPos pos;
    public final int type;
    public final int data;

    public PacketFXCrucible(BlockPos pos, int type, int data) {
        this.pos = pos.immutable();
        this.type = type;
        this.data = data;
    }

    public static void encode(PacketFXCrucible packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeInt(packet.type);
        buffer.writeInt(packet.data);
    }

    public static PacketFXCrucible decode(FriendlyByteBuf buffer) {
        return new PacketFXCrucible(buffer.readBlockPos(), buffer.readInt(), buffer.readInt());
    }

    public static Consumer<PacketFXCrucible> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketFXCrucible packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
}
