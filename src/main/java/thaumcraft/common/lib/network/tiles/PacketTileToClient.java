package thaumcraft.common.lib.network.tiles;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.common.tiles.TileThaumcraft;

import java.util.function.Supplier;

/**
 * PacketTileToClient - Sends custom tile entity messages from server to client.
 * 
 * Used for:
 * - Custom animations
 * - State changes that need immediate visual feedback
 * - Multi-step processes
 * 
 * Ported from 1.12.2
 */
public class PacketTileToClient {
    
    private long pos;
    private CompoundTag nbt;
    
    public PacketTileToClient() {
    }
    
    public PacketTileToClient(BlockPos pos, CompoundTag nbt) {
        this.pos = pos.asLong();
        this.nbt = nbt;
    }
    
    public static void encode(PacketTileToClient msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.pos);
        buf.writeNbt(msg.nbt);
    }
    
    public static PacketTileToClient decode(FriendlyByteBuf buf) {
        PacketTileToClient msg = new PacketTileToClient();
        msg.pos = buf.readLong();
        msg.nbt = buf.readNbt();
        return msg;
    }
    
    public static void handle(PacketTileToClient msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleOnClient(msg));
        ctx.setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(PacketTileToClient msg) {
        Level world = Minecraft.getInstance().level;
        if (world == null) return;
        
        BlockPos blockPos = BlockPos.of(msg.pos);
        BlockEntity te = world.getBlockEntity(blockPos);
        
        if (te instanceof TileThaumcraft thaumcraftTile) {
            thaumcraftTile.messageFromServer(msg.nbt != null ? msg.nbt : new CompoundTag());
        }
    }
}
