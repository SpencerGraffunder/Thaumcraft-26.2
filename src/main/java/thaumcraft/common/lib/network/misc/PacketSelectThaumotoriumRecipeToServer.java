package thaumcraft.common.lib.network.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.NetworkEvent;
import thaumcraft.common.tiles.crafting.TileThaumatorium;

import java.util.function.Supplier;

/**
 * Packet sent from client to select/deselect a recipe in the Thaumatorium.
 * Used for automated alchemy crafting queue management.
 * 
 * Note: The full recipe queue system requires additional TileThaumatorium fields
 * that will be added when the full Thaumatorium GUI/crafting system is implemented.
 * 
 * Ported to 1.20.1
 */
public class PacketSelectThaumotoriumRecipeToServer {
    
    private final long pos;
    private final int recipeHash;
    
    public PacketSelectThaumotoriumRecipeToServer(Player player, BlockPos pos, int recipeHash) {
        this.pos = pos.asLong();
        this.recipeHash = recipeHash;
    }
    
    private PacketSelectThaumotoriumRecipeToServer(long pos, int recipeHash) {
        this.pos = pos;
        this.recipeHash = recipeHash;
    }
    
    public static void encode(PacketSelectThaumotoriumRecipeToServer packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.pos);
        buf.writeInt(packet.recipeHash);
    }
    
    public static PacketSelectThaumotoriumRecipeToServer decode(FriendlyByteBuf buf) {
        return new PacketSelectThaumotoriumRecipeToServer(
            buf.readLong(),
            buf.readInt()
        );
    }
    
    public static void handle(PacketSelectThaumotoriumRecipeToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            Level level = player.level();
            BlockPos blockPos = BlockPos.of(packet.pos);
            
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof TileThaumatorium thaumatorium) {
                // TODO: Implement recipe queue selection when TileThaumatorium
                // is updated with the full recipe queue system:
                // - recipeHash: ArrayList<Integer> of queued recipe hashes
                // - recipeEssentia: ArrayList<AspectList> of essentia costs
                // - recipePlayer: ArrayList<String> of requesting players
                // - recipes: List<CrucibleRecipe> of available recipes
                // - maxRecipes: int max queue size
                // - currentCraft: int current crafting index
                
                // For now, just trigger a sync to acknowledge the packet
                thaumatorium.setChanged();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
