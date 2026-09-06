package thaumcraft.common.lib.network.playerdata;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import java.util.function.Consumer;



/**
 * PacketSyncWarp - Syncs all player warp data from server to client.
 * 
 * Sent when:
 * - Player logs in
 * - Warp is gained or lost
 * 
 * Ported from 1.12.2
 */
public class PacketSyncWarp implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSyncWarp> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetsyncwarp"));

    public static final StreamCodec<FriendlyByteBuf, PacketSyncWarp> STREAM_CODEC =
        StreamCodec.ofMember(PacketSyncWarp::encode, PacketSyncWarp::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public CompoundTag data;
    
    public PacketSyncWarp() {
        this.data = new CompoundTag();
    }
    
    public PacketSyncWarp(Player player) {
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null) {
            this.data = warp.serializeNBT();
        } else {
            this.data = new CompoundTag();
        }
    }
    
    public static void encode(PacketSyncWarp msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }
    
    public static PacketSyncWarp decode(FriendlyByteBuf buf) {
        PacketSyncWarp msg = new PacketSyncWarp();
        msg.data = buf.readNbt();
        return msg;
    }
    
    public static Consumer<PacketSyncWarp> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketSyncWarp msg, IPayloadContext ctxSupplier) {
        IPayloadContext ctx = ctxSupplier;
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(msg));
    }
    
}
