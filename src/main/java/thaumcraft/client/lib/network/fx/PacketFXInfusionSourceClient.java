package thaumcraft.client.lib.network.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;
import thaumcraft.common.tiles.crafting.TilePedestal;

import thaumcraft.common.lib.network.fx.PacketFXInfusionSource;

/** Client-side handler for {@link PacketFXInfusionSource}. */
public class PacketFXInfusionSourceClient {
    public static void handle(PacketFXInfusionSource msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        
        BlockPos matrixPos = BlockPos.of(msg.p1);
        BlockPos sourcePos = BlockPos.of(msg.p2);
        
        String key = sourcePos.getX() + ":" + sourcePos.getY() + ":" + sourcePos.getZ() + ":" + msg.color;
        
        BlockEntity tile = level.getBlockEntity(matrixPos);
        if (tile instanceof TileInfusionMatrix matrix) {
            // Determine tick count - pedestals get longer effects
            int count = 15;
            BlockEntity sourceTile = level.getBlockEntity(sourcePos);
            if (sourceTile instanceof TilePedestal) {
                count = 60;
            }
            
            // Update or add the source FX entry
            if (matrix.sourceFX.containsKey(key)) {
                TileInfusionMatrix.SourceFX sf = matrix.sourceFX.get(key);
                sf.ticks = count;
                matrix.sourceFX.put(key, sf);
            } else {
                matrix.sourceFX.put(key, new TileInfusionMatrix.SourceFX(sourcePos, count, msg.color));
            }
        }
    }
}
