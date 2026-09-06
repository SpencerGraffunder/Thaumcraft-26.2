package thaumcraft.client.lib.network.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.FXDispatcher;

import thaumcraft.common.lib.network.fx.PacketFXSlash;

/** Client-side handler for {@link PacketFXSlash}. */
public class PacketFXSlashClient {
    public static void handle(PacketFXSlash msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        Entity source = mc.level.getEntity(msg.sourceId);
        Entity target = mc.level.getEntity(msg.targetId);
        
        if (source != null && target != null) {
            double sourceY = source.getBoundingBox().minY + source.getBbHeight() / 2.0f;
            double targetY = target.getBoundingBox().minY + target.getBbHeight() / 2.0f;
            
            FXDispatcher.INSTANCE.drawSlash(
                    source.getX(), sourceY, source.getZ(),
                    target.getX(), targetY, target.getZ(),
                    8);
        }
    }
}
