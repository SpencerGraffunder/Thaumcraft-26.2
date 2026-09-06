package thaumcraft.client.lib.network.playerdata;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.init.ModSounds;

import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;

/** Client-side handler for {@link PacketWarpMessage}. */
public class PacketWarpMessageClient {
    public static void handle(PacketWarpMessage msg) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        String textKey;
        boolean playSound = msg.change > 0;
        
        switch (msg.type) {
            case 0 -> { // Permanent warp
                if (msg.change > 0) {
                    textKey = "tc.addwarp";
                    // Play whisper sound for permanent warp gain
                    Minecraft.getInstance().level.playLocalSound(
                            player.getX(), player.getY(), player.getZ(),
                            ModSounds.WHISPERS.get(), SoundSource.PLAYERS,
                            0.5f, 1.0f, false);
                } else {
                    textKey = "tc.removewarp";
                    playSound = false;
                }
            }
            case 1 -> { // Normal (sticky) warp
                if (msg.change > 0) {
                    textKey = "tc.addwarpsticky";
                    Minecraft.getInstance().level.playLocalSound(
                            player.getX(), player.getY(), player.getZ(),
                            ModSounds.WHISPERS.get(), SoundSource.PLAYERS,
                            0.5f, 1.0f, false);
                } else {
                    textKey = "tc.removewarpsticky";
                    playSound = false;
                }
                player.sendSystemMessage(Component.translatable(textKey));
            }
            case 2 -> { // Temporary warp
                if (msg.change > 0) {
                    textKey = "tc.addwarptemp";
                } else {
                    textKey = "tc.removewarptemp";
                }
                player.sendSystemMessage(Component.translatable(textKey));
            }
            default -> {
                return;
            }
        }
    }
}
