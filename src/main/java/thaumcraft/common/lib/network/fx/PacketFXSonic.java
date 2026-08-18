package thaumcraft.common.lib.network.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.client.fx.FXDispatcher;

import java.util.function.Supplier;

/**
 * PacketFXSonic - Sonic boom visual effect.
 * Used by the sonic focus and other sonic-based effects.
 * Creates an expanding ring particle effect around the source entity.
 * 
 * Server -> Client
 */
public class PacketFXSonic {
    
    private final int sourceId;
    
    public PacketFXSonic(Entity source) {
        this.sourceId = source.getId();
    }
    
    public PacketFXSonic(int sourceId) {
        this.sourceId = sourceId;
    }
    
    public static void encode(PacketFXSonic packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.sourceId);
    }
    
    public static PacketFXSonic decode(FriendlyByteBuf buffer) {
        int sourceId = buffer.readInt();
        return new PacketFXSonic(sourceId);
    }
    
    public static void handle(PacketFXSonic packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            handleClient(packet);
        });
        ctx.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXSonic packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        
        Entity source = level.getEntity(packet.sourceId);
        if (source != null) {
            // Create sonic boom effect at entity position
            FXDispatcher.INSTANCE.sonicBoom(source.getX(), source.getY(), source.getZ(), source, 10);
        }
    }
}
