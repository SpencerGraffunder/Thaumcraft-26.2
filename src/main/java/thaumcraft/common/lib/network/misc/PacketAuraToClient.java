package thaumcraft.common.lib.network.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.common.world.aura.AuraChunk;

import java.util.function.Supplier;

/**
 * PacketAuraToClient - Syncs aura data for a chunk from server to client.
 * 
 * Sent when:
 * - Player enters a new chunk
 * - Aura values change significantly
 * - Player uses a thaumometer or similar device
 * 
 * Ported from 1.12.2
 */
public class PacketAuraToClient {
    
    private short base;
    private float vis;
    private float flux;
    
    // Client-side storage for current aura display
    @OnlyIn(Dist.CLIENT)
    public static AuraChunk currentAura = null;
    
    public PacketAuraToClient() {
    }
    
    public PacketAuraToClient(AuraChunk auraChunk) {
        this.base = auraChunk.getBase();
        this.vis = auraChunk.getVis();
        this.flux = auraChunk.getFlux();
    }
    
    public PacketAuraToClient(short base, float vis, float flux) {
        this.base = base;
        this.vis = vis;
        this.flux = flux;
    }
    
    public static void encode(PacketAuraToClient msg, FriendlyByteBuf buf) {
        buf.writeShort(msg.base);
        buf.writeFloat(msg.vis);
        buf.writeFloat(msg.flux);
    }
    
    public static PacketAuraToClient decode(FriendlyByteBuf buf) {
        PacketAuraToClient msg = new PacketAuraToClient();
        msg.base = buf.readShort();
        msg.vis = buf.readFloat();
        msg.flux = buf.readFloat();
        return msg;
    }
    
    public static void handle(PacketAuraToClient msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleOnClient(msg));
        ctx.setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(PacketAuraToClient msg) {
        // Store the current aura for HUD display
        currentAura = new AuraChunk(null, msg.base, msg.vis, msg.flux);
    }
}
