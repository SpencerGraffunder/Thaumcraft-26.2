package thaumcraft.common.lib.network.playerdata;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

import java.util.function.Supplier;

/**
 * Packet sent from client to update the name of a focus being crafted
 * in the Focal Manipulator.
 * 
 * Ported to 1.20.1
 */
public class PacketFocusNameToServer {
    
    private final long pos;
    private final String name;
    
    public PacketFocusNameToServer(BlockPos pos, String name) {
        this.pos = pos.asLong();
        this.name = name;
    }
    
    private PacketFocusNameToServer(long pos, String name) {
        this.pos = pos;
        this.name = name;
    }
    
    public static void encode(PacketFocusNameToServer packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.pos);
        buf.writeUtf(packet.name);
    }
    
    public static PacketFocusNameToServer decode(FriendlyByteBuf buf) {
        return new PacketFocusNameToServer(
            buf.readLong(),
            buf.readUtf(32767)
        );
    }
    
    public static void handle(PacketFocusNameToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            Level level = player.level();
            BlockPos blockPos = BlockPos.of(packet.pos);
            
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof TileFocalManipulator focalManipulator) {
                focalManipulator.focusName = packet.name;
                focalManipulator.setChanged();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
