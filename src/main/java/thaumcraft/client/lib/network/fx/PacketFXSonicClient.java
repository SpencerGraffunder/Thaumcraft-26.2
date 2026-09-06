package thaumcraft.client.lib.network.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.FXDispatcher;

import thaumcraft.common.lib.network.fx.PacketFXSonic;

/** Client-side handler for {@link PacketFXSonic}. */
public class PacketFXSonicClient {
    public static void handle(PacketFXSonic msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        
        Entity source = level.getEntity(msg.sourceId);
        if (source != null) {
            // Create sonic boom effect at entity position
            FXDispatcher.INSTANCE.sonicBoom(source.getX(), source.getY(), source.getZ(), source, 10);
        }
    }
}
