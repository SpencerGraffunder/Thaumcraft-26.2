package thaumcraft.common.lib.network.playerdata;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

import java.util.function.Supplier;

/**
 * PacketSyncWarp - Syncs all player warp data from server to client.
 * 
 * Sent when:
 * - Player logs in
 * - Warp is gained or lost
 * 
 * Ported from 1.12.2
 */
public class PacketSyncWarp {
    
    private CompoundTag data;
    
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
    
    public static void handle(PacketSyncWarp msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleOnClient(msg));
        ctx.setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(PacketSyncWarp msg) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null && msg.data != null) {
            warp.deserializeNBT(msg.data);
        }
    }
}
