package thaumcraft.common.lib.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketSealToClient;
import thaumcraft.common.world.aura.AuraChunkHandler;

/**
 * ChunkEvents - Handles chunk-based seal syncing.
 *
 * Responsibilities:
 * - Restore seals from the chunk attachment when a chunk loads
 * - Drop seals from memory when a chunk unloads
 * - Sync seals to players when they start watching a chunk
 *
 * Persistence of seals/aura into the chunk attachment is handled by
 * {@link AuraChunkHandler#persistChunk(Level, ChunkPos)} (attachment
 * auto-saves with the chunk NBT). This class deliberately does NOT write
 * inside chunk-save callbacks - see the class-level note in AuraChunkHandler
 * on the 26.2 save loop.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ChunkEvents {
    
    private static final String THAUMCRAFT_DATA_KEY = "thaumcraft";
    private static final String SEALS_KEY = "seals";
    
    /**
     * Called when a chunk is fully loaded: restore seals from the chunk attachment.
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() == null || event.getLevel().isClientSide()) {
            return;
        }
        
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        LevelChunk chunk = event.getChunk();
        CompoundTag data = chunk.getData(AuraChunkHandler.CHUNK_DATA);
        if (data == null || !data.contains(THAUMCRAFT_DATA_KEY)) {
            return;
        }
        
        CompoundTag thaumcraftData = data.getCompoundOrEmpty(THAUMCRAFT_DATA_KEY);
        if (!thaumcraftData.contains(SEALS_KEY)) {
            return;
        }
        
        ListTag sealList = thaumcraftData.getListOrEmpty(SEALS_KEY);
        for (int i = 0; i < sealList.size(); i++) {
            CompoundTag sealNbt = sealList.getCompoundOrEmpty(i);
            
            try {
                SealEntity seal = new SealEntity();
                seal.readNBT(sealNbt);
                
                if (seal.getSeal() != null && seal.getSealPos() != null) {
                    SealHandler.addSealEntity(level, seal);
                }
            } catch (Exception e) {
                Thaumcraft.LOGGER.error("Failed to load seal from chunk data", e);
            }
        }
    }
    
    /**
     * Called when a chunk is unloaded: drop its seals from memory.
     * (Their data was persisted into the attachment by AuraChunkHandler.)
     */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() == null || event.getLevel().isClientSide()) {
            return;
        }
        
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        ChunkPos chunkPos = event.getChunk().getPos();
        for (ISealEntity seal : SealHandler.getSealsInChunk(level, chunkPos)) {
            if (seal.getSeal() != null) {
                SealHandler.removeSealEntity(level, seal.getSealPos(), true);
            }
        }
    }
    
    /**
     * When a player starts watching a chunk, sync all seals in that chunk to them
     */
    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerPlayer player = event.getPlayer();
        Level level = player.level();
        ChunkPos chunkPos = event.getPos();
        
        // Send all seals in the chunk to the player
        for (ISealEntity seal : SealHandler.getSealsInChunk(level, chunkPos)) {
            if (seal.getSeal() != null) {
                PacketHandler.sendToPlayer(new PacketSealToClient(seal), player);
            }
        }
    }
    
    /**
     * When a player stops watching a chunk, we could optionally clean up client-side data
     * For now, we don't need to do anything special here
     */
    @SubscribeEvent
    public static void onChunkUnWatch(ChunkWatchEvent.UnWatch event) {
        // Optional: Could send a packet to remove seals from client cache
        // For now, client keeps the data until the next login/dimension change
    }
}
