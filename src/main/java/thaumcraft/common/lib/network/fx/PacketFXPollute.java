package thaumcraft.common.lib.network.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.client.fx.FXDispatcher;

import java.util.function.Supplier;

/**
 * PacketFXPollute - Visual effect for aura pollution.
 * Shows purple smoke/particles when flux is added to the aura.
 * 
 * Server -> Client
 */
public class PacketFXPollute {
    
    private final int x;
    private final int y;
    private final int z;
    private final byte amount;
    
    public PacketFXPollute(BlockPos pos, float amt) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        // Minimum amount of 1 if any pollution
        if (amt < 1.0f && amt > 0.0f) {
            amt = 1.0f;
        }
        this.amount = (byte) Math.min(amt, 127);
    }
    
    private PacketFXPollute(int x, int y, int z, byte amount) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.amount = amount;
    }
    
    public static void encode(PacketFXPollute packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.x);
        buffer.writeInt(packet.y);
        buffer.writeInt(packet.z);
        buffer.writeByte(packet.amount);
    }
    
    public static PacketFXPollute decode(FriendlyByteBuf buffer) {
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        byte amount = buffer.readByte();
        return new PacketFXPollute(x, y, z, amount);
    }
    
    public static void handle(PacketFXPollute packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            handleClient(packet);
        });
        ctx.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXPollute packet) {
        BlockPos pos = new BlockPos(packet.x, packet.y, packet.z);
        // Draw pollution particles - cap at 40 to avoid performance issues
        int particleCount = Math.min(40, Math.abs(packet.amount));
        for (int a = 0; a < particleCount; a++) {
            FXDispatcher.INSTANCE.drawPollutionParticles(pos);
        }
    }
}
