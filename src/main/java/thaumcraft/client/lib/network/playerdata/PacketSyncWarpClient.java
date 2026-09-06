package thaumcraft.client.lib.network.playerdata;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

import thaumcraft.common.lib.network.playerdata.PacketSyncWarp;

/** Client-side handler for {@link PacketSyncWarp}. */
public class PacketSyncWarpClient {
    public static void handle(PacketSyncWarp msg) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null && msg.data != null) {
            warp.deserializeNBT(msg.data);
        }
    }
}
