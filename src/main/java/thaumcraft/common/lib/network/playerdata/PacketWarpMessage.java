package thaumcraft.common.lib.network.playerdata;
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


/**
 * PacketWarpMessage - Notifies client when warp is gained or lost.
 * 
 * Shows a message on screen and plays a sound when gaining permanent warp.
 * 
 * Type values:
 * - 0 = Permanent warp
 * - 1 = Normal (sticky) warp
 * - 2 = Temporary warp
 * 
 * Ported from 1.12.2
 */
public class PacketWarpMessage implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketWarpMessage> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetwarpmessage"));

    public static final StreamCodec<FriendlyByteBuf, PacketWarpMessage> STREAM_CODEC =
        StreamCodec.ofMember(PacketWarpMessage::encode, PacketWarpMessage::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    private int change;
    private byte type;
    
    public PacketWarpMessage() {
        this.change = 0;
        this.type = 0;
    }
    
    public PacketWarpMessage(Player player, byte type, int change) {
        this.change = change;
        this.type = type;
    }
    
    public static void encode(PacketWarpMessage msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.change);
        buf.writeByte(msg.type);
    }
    
    public static PacketWarpMessage decode(FriendlyByteBuf buf) {
        PacketWarpMessage msg = new PacketWarpMessage();
        msg.change = buf.readInt();
        msg.type = buf.readByte();
        return msg;
    }
    
    public static void handle(PacketWarpMessage msg, IPayloadContext ctxSupplier) {
        IPayloadContext ctx = ctxSupplier;
        if (msg.change != 0) {
            ctx.enqueueWork(() -> handleOnClient(msg));
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(PacketWarpMessage msg) {
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
