package thaumcraft.client.lib.network.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.FXDispatcher;

import thaumcraft.common.lib.network.fx.PacketFXBoreDig;

/** Client-side handler for {@link PacketFXBoreDig}. */
public class PacketFXBoreDigClient {
    public static void handle(PacketFXBoreDig msg) {
        try {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            
            BlockPos pos = new BlockPos(msg.x, msg.y, msg.z);
            Entity bore = level.getEntity(msg.boreId);
            
            if (bore == null) {
                return;
            }
            
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.AIR)) {
                return;
            }
            
            // Create delayed digging effects
            // In the original, this used ServerEvents.addRunnableClient for delayed execution
            // For 1.20.1, we'll spawn the effect directly - the FXDispatcher handles the visual
            FXDispatcher.INSTANCE.boreDigFx(
                    pos.getX(), pos.getY(), pos.getZ(),
                    bore, state, 0, msg.delay
            );
        } catch (Exception ignored) {
            // Silently ignore client-side FX errors
        }
    }
}
