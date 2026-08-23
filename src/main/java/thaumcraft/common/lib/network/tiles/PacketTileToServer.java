package thaumcraft.common.lib.network.tiles;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.tiles.TileThaumcraft;


/**
 * PacketTileToServer - Sends custom tile entity messages from client to server.
 * 
 * Used for:
 * - GUI interactions
 * - Button presses
 * - Configuration changes
 * 
 * Ported from 1.12.2
 */
public class PacketTileToServer implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketTileToServer> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packettiletoserver"));

    public static final StreamCodec<FriendlyByteBuf, PacketTileToServer> STREAM_CODEC =
        StreamCodec.ofMember(PacketTileToServer::encode, PacketTileToServer::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    private long pos;
    private CompoundTag nbt;
    
    public PacketTileToServer() {
    }
    
    public PacketTileToServer(BlockPos pos, CompoundTag nbt) {
        this.pos = pos.asLong();
        this.nbt = nbt;
    }
    
    public static void encode(PacketTileToServer msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.pos);
        buf.writeNbt(msg.nbt);
    }
    
    public static PacketTileToServer decode(FriendlyByteBuf buf) {
        PacketTileToServer msg = new PacketTileToServer();
        msg.pos = buf.readLong();
        msg.nbt = buf.readNbt();
        return msg;
    }
    
    public static void handle(PacketTileToServer msg, IPayloadContext ctxSupplier) {
        IPayloadContext ctx = ctxSupplier;
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            
            Level world = player.level();
            BlockPos blockPos = BlockPos.of(msg.pos);
            
            // Security check: make sure player is close enough
            if (blockPos.distSqr(player.blockPosition()) > 64) {
                return;
            }
            
            BlockEntity te = world.getBlockEntity(blockPos);
            if (te instanceof TileThaumcraft thaumcraftTile) {
                thaumcraftTile.messageFromClient(msg.nbt != null ? msg.nbt : new CompoundTag(), player);
            }
        });
    }
}
