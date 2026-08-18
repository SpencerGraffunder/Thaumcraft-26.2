package thaumcraft.common.lib.network.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.common.tiles.TileThaumcraft;

import java.util.function.Supplier;

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
public class PacketTileToServer {
    
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
    
    public static void handle(PacketTileToServer msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
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
        ctx.setPacketHandled(true);
    }
}
