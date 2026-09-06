package thaumcraft.client.lib.network.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.FXDispatcher;

import thaumcraft.common.lib.network.fx.PacketFXBlockArc;

/** Client-side handler for {@link PacketFXBlockArc}. */
public class PacketFXBlockArcClient {
    public static void handle(PacketFXBlockArc msg) {
        FXDispatcher.INSTANCE.arcLightning(
                msg.tx, msg.ty, msg.tz,
                msg.x + 0.5, msg.y + 0.5, msg.z + 0.5,
                msg.r, msg.g, msg.b,
                0.5f);
    }
}
