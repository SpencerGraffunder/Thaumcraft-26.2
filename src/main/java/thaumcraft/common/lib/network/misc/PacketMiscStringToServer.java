package thaumcraft.common.lib.network.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.common.menu.LogisticsMenu;

import java.util.function.Supplier;

/**
 * Packet for sending misc string data from client to server.
 * Used for things like search text in the logistics GUI.
 * 
 * Message IDs:
 * - 0: Logistics search text
 * - 1-99: Reserved for future string operations
 * 
 * Ported from 1.12.2.
 */
public class PacketMiscStringToServer {
    
    private final int messageId;
    private final String text;
    
    public PacketMiscStringToServer(int messageId, String text) {
        this.messageId = messageId;
        this.text = text != null ? text : "";
    }
    
    public static void encode(PacketMiscStringToServer packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.messageId);
        buf.writeUtf(packet.text, 256); // Max 256 characters
    }
    
    public static PacketMiscStringToServer decode(FriendlyByteBuf buf) {
        int messageId = buf.readInt();
        String text = buf.readUtf(256);
        return new PacketMiscStringToServer(messageId, text);
    }
    
    public static void handle(PacketMiscStringToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            switch (packet.messageId) {
                case 0 -> {
                    // Logistics search text
                    if (player.containerMenu instanceof LogisticsMenu logisticsMenu) {
                        logisticsMenu.setSearchText(packet.text);
                    }
                }
                // Add more message IDs as needed
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
