package thaumcraft.common.lib.network.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.api.casters.ICaster;

import java.util.function.Supplier;

/**
 * Packet sent from client to server when the player presses an item-specific key.
 * Used for:
 * - Key 0: Golem bell actions
 * - Key 1: Misc caster toggle (with modifier keys)
 * - Key 1: Elemental shovel orientation toggle
 * 
 * Modifier values:
 * - 0: No modifier
 * - 1: Ctrl key held
 * - 2: Shift key held
 * 
 * Ported to 1.20.1
 */
public class PacketItemKeyToServer {
    
    private final byte key;
    private final byte modifier;
    
    public PacketItemKeyToServer(int key) {
        this.key = (byte) key;
        this.modifier = 0;
    }
    
    public PacketItemKeyToServer(int key, int modifier) {
        this.key = (byte) key;
        this.modifier = (byte) modifier;
    }
    
    private PacketItemKeyToServer(byte key, byte modifier) {
        this.key = key;
        this.modifier = modifier;
    }
    
    public static void encode(PacketItemKeyToServer packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.key);
        buf.writeByte(packet.modifier);
    }
    
    public static PacketItemKeyToServer decode(FriendlyByteBuf buf) {
        return new PacketItemKeyToServer(buf.readByte(), buf.readByte());
    }
    
    public static void handle(PacketItemKeyToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
            
            boolean handled = false;
            
            if (!mainHand.isEmpty()) {
                // Key 1: Misc toggle for casters
                if (packet.key == 1 && mainHand.getItem() instanceof ICaster) {
                    toggleMisc(mainHand, player, packet.modifier);
                    handled = true;
                }
                
                // Key 1: Elemental shovel orientation toggle
                // TODO: Uncomment when ItemElementalShovel is ported
                // if (packet.key == 1 && mainHand.getItem() instanceof ItemElementalShovel) {
                //     byte orientation = ItemElementalShovel.getOrientation(mainHand);
                //     ItemElementalShovel.setOrientation(mainHand, (byte)((orientation + 1) % 3));
                //     handled = true;
                // }
                
                // Key 0: Golem bell key action
                // TODO: Implement when golem bell key action is needed
                // if (packet.key == 0 && mainHand.getItem() instanceof ItemGolemBell) {
                //     // Handle golem bell key action
                //     handled = true;
                // }
            }
            
            // Check offhand for casters if main hand didn't handle it
            if (!handled && !offHand.isEmpty()) {
                if (packet.key == 1 && offHand.getItem() instanceof ICaster) {
                    toggleMisc(offHand, player, packet.modifier);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * Toggle misc settings on a caster item.
     * 
     * @param casterStack The caster item stack
     * @param player The player
     * @param modifier Key modifier (0 = none, 1 = ctrl, 2 = shift)
     */
    private static void toggleMisc(ItemStack casterStack, ServerPlayer player, int modifier) {
        // TODO: Implement CasterManager.toggleMisc when CasterManager is ported
        // This typically toggles between different casting modes or settings
        // The modifier key affects which setting is toggled:
        // - No modifier: Toggle primary setting
        // - Ctrl: Toggle secondary setting  
        // - Shift: Toggle tertiary setting
        
        // For now this is a stub
        // CasterManager.toggleMisc(casterStack, player.level(), player, modifier);
    }
}
