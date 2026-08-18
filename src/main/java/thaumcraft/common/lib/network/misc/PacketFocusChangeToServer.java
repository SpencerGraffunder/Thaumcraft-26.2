package thaumcraft.common.lib.network.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.api.casters.ICaster;

import java.util.function.Supplier;

/**
 * Packet sent from client to server when the player wants to change their caster's focus.
 * Triggered by the focus change keybind (default: F).
 * 
 * Ported to 1.20.1
 */
public class PacketFocusChangeToServer {
    
    private final String focus;
    
    public PacketFocusChangeToServer(String focus) {
        this.focus = focus;
    }
    
    public static void encode(PacketFocusChangeToServer packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.focus);
    }
    
    public static PacketFocusChangeToServer decode(FriendlyByteBuf buf) {
        return new PacketFocusChangeToServer(buf.readUtf(32767));
    }
    
    public static void handle(PacketFocusChangeToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            
            // Check main hand first
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!mainHand.isEmpty() && mainHand.getItem() instanceof ICaster) {
                changeFocus(mainHand, player, packet.focus);
                return;
            }
            
            // Then check off hand
            ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!offHand.isEmpty() && offHand.getItem() instanceof ICaster) {
                changeFocus(offHand, player, packet.focus);
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * Change the focus on a caster item.
     * 
     * @param casterStack The caster item stack
     * @param player The player
     * @param focusKey The focus key to change to, or "REMOVE" to remove the current focus
     */
    private static void changeFocus(ItemStack casterStack, ServerPlayer player, String focusKey) {
        // TODO: Implement CasterManager.changeFocus when CasterManager is ported
        // For now this is a stub that will be completed when the caster system is fully implemented
        
        if ("REMOVE".equals(focusKey)) {
            // Remove current focus
            ICaster caster = (ICaster) casterStack.getItem();
            ItemStack currentFocus = caster.getFocusStack(casterStack);
            if (currentFocus != null && !currentFocus.isEmpty()) {
                // Give the focus back to the player
                if (!player.getInventory().add(currentFocus)) {
                    player.drop(currentFocus, false);
                }
                caster.setFocus(casterStack, ItemStack.EMPTY);
            }
        } else {
            // Change to a specific focus from inventory
            // This requires the focus selection system to be implemented
            // CasterManager.changeFocus(casterStack, player.level(), player, focusKey);
        }
    }
}
