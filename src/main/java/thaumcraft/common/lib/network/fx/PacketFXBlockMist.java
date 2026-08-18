package thaumcraft.common.lib.network.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.client.fx.FXDispatcher;

import java.util.function.Supplier;

/**
 * PacketFXBlockMist - Misty/foggy particle effect at a block.
 * Used for various mystical effects on blocks like infusion pedestals,
 * flux goo, or magical fog.
 * 
 * Server -> Client
 */
public class PacketFXBlockMist {
    
    private final long loc;
    private final int color;
    
    public PacketFXBlockMist(BlockPos pos, int color) {
        this.loc = pos.asLong();
        this.color = color;
    }
    
    private PacketFXBlockMist(long loc, int color) {
        this.loc = loc;
        this.color = color;
    }
    
    public static void encode(PacketFXBlockMist packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.loc);
        buffer.writeInt(packet.color);
    }
    
    public static PacketFXBlockMist decode(FriendlyByteBuf buffer) {
        long loc = buffer.readLong();
        int color = buffer.readInt();
        return new PacketFXBlockMist(loc, color);
    }
    
    public static void handle(PacketFXBlockMist packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            handleClient(packet);
        });
        ctx.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXBlockMist packet) {
        BlockPos pos = BlockPos.of(packet.loc);
        FXDispatcher.INSTANCE.drawBlockMistParticles(pos, packet.color);
    }
}
