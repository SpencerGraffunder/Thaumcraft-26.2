package thaumcraft.client.lib.network.misc;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.init.ModSounds;

import thaumcraft.common.lib.network.misc.PacketMiscEvent;

/** Client-side handler for {@link PacketMiscEvent}. */
public class PacketMiscEventClient {
    public static void handle(PacketMiscEvent msg) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        
        switch (msg.type) {
        case PacketMiscEvent.WARP_EVENT -> {
                // Play heartbeat sound for warp effects
                // Check ModConfig.nostress
                if (!thaumcraft.common.config.ModConfig.isWussMode()) {
                if (ModSounds.HEARTBEAT.get() != null) {
                    mc.level.playLocalSound(
                        player.getX(), player.getY(), player.getZ(),
                        ModSounds.HEARTBEAT.get(), SoundSource.AMBIENT,
                        1.0f, 1.0f, false
                    );
                }
            }
        case PacketMiscEvent.MIST_EVENT -> {
                // Long duration fog effect
                // Fog effect
                if (minecraft.level != null && minecraft.player != null) {
                    minecraft.level.getLevelData().setFogDensity(0.5f);
                }
                // RenderEventHandler.fogFiddled = true;
                // RenderEventHandler.fogDuration = 2400;
            }
        case PacketMiscEvent.MIST_EVENT_SHORT -> {
                // Short duration fog effect
                // Render event
                if (minecraft.level != null && minecraft.player != null) {
                    minecraft.level.getLevelData().setFogDensity(0.3f);
                }
                // RenderEventHandler.fogFiddled = true;
                // if (RenderEventHandler.fogDuration < 200) {
                //     RenderEventHandler.fogDuration = 200;
                // }
            }
        }
    }
}
