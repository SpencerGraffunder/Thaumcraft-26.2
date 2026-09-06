package thaumcraft.client.lib.network.tiles;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.tiles.TileThaumcraft;

import thaumcraft.common.lib.network.tiles.PacketTileToClient;

/** Client-side handler for {@link PacketTileToClient}. */
public class PacketTileToClientClient {
    public static void handle(PacketTileToClient msg) {
        Level world = Minecraft.getInstance().level;
        if (world == null) return;
        
        BlockPos blockPos = BlockPos.of(msg.pos);
        BlockEntity te = world.getBlockEntity(blockPos);
        
        if (te instanceof TileThaumcraft thaumcraftTile) {
            thaumcraftTile.messageFromServer(msg.nbt != null ? msg.nbt : new CompoundTag());
        }
    }
}
