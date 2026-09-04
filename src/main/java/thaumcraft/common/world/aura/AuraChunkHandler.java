package thaumcraft.common.world.aura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.golems.seals.SealHandler;

/**
 * AuraChunkHandler - Handles aura generation and persistence for chunks.
 *
 * This handler:
 * - Generates initial aura for newly created chunks
 * - Restores aura data from the chunk attachment on load
 * - Persists aura + seal data into the chunk attachment on change/unload
 *
 * Persistence model (26.2):
 * Per-chunk Thaumcraft data lives in a chunk {@link AttachmentType}, which the
 * server serializes into the chunk NBT automatically. We update the attachment
 * whenever data changes and call {@code markUnsaved()} so the chunk is picked
 * up by the next save cycle.
 *
 * IMPORTANT: never write attachment data or call {@code markUnsaved()} from
 * inside a chunk-save callback. In 26.2 the save snapshot is taken BEFORE the
 * save event fires, so writes there are lost - and re-dirtying the chunk
 * inside the save event makes the chunk-save do-while loop run forever
 * (observed as an OOM crash on first save).
 *
 * Aura is generated based on biome modifiers and nearby chunks.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class AuraChunkHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraChunkHandler.class);
    
    // NBT tag names for aura data
    private static final String TAG_THAUMCRAFT = "thaumcraft";
    private static final String TAG_AURA = "aura";
    private static final String TAG_BASE = "base";
    private static final String TAG_VIS = "vis";
    private static final String TAG_FLUX = "flux";
    private static final String TAG_SEALS = "seals";
    
    /**
     * Chunk attachment storing Thaumcraft per-chunk data (aura, seals, etc.).
     * Serialized into the chunk NBT automatically on save.
     */
    public static AttachmentType<CompoundTag> CHUNK_DATA;

    @SubscribeEvent
    public static void registerAttachmentTypes(RegisterEvent event) {
        if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.ATTACHMENT_TYPES)) {
            CHUNK_DATA = AttachmentType.<CompoundTag>builder(() -> new CompoundTag())
                    .serialize(new IAttachmentSerializer<CompoundTag>() {
                        @Override
                        public CompoundTag read(IAttachmentHolder holder, ValueInput input) {
                            return input.read("data", CompoundTag.CODEC).orElse(new CompoundTag());
                        }

                        @Override
                        public boolean write(CompoundTag tag, ValueOutput output) {
                            output.store("data", CompoundTag.CODEC, tag);
                            return true;
                        }
                    })
                    .build();
            event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper ->
                    helper.register(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "chunk_data"), CHUNK_DATA));
        }
    }
    
    /**
     * Called when a chunk is fully loaded (from disk or newly generated).
     * Restores persisted aura from the chunk attachment; generates and
     * persists fresh aura for chunks that have none.
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
        ChunkPos chunkPos = chunk.getPos();
        ResourceKey<Level> dimension = level.dimension();
        
        // Restore aura persisted in the chunk attachment (if any)
        CompoundTag data = chunk.getData(CHUNK_DATA);
        if (data != null && data.contains(TAG_THAUMCRAFT)) {
            CompoundTag tcData = data.getCompoundOrEmpty(TAG_THAUMCRAFT);
            if (tcData.contains(TAG_AURA)) {
                CompoundTag auraData = tcData.getCompoundOrEmpty(TAG_AURA);
                AuraHandler.addAuraChunk(dimension, chunk,
                        auraData.getShortOr(TAG_BASE, (short)0),
                        auraData.getFloatOr(TAG_VIS, 0.0F),
                        auraData.getFloatOr(TAG_FLUX, 0.0F));
                return; // persisted aura exists - don't regenerate
            }
        }
        
        // No saved aura: generate for this new chunk and persist it
        if (!ModConfig.generateAura) {
            return;
        }
        
        generateChunkAura(level, chunk);
        persistChunk(level, chunkPos);
    }
    
    /**
     * Called when a chunk is unloaded.
     * Persists the final state into the attachment, then drops it from memory.
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
        persistChunk(level, chunkPos);
        AuraHandler.removeAuraChunk(level.dimension(), chunkPos.x(), chunkPos.z());
    }
    
    /**
     * Persists current aura + seal data into the chunk's Thaumcraft attachment
     * and marks the chunk to be saved (picked up by the next save cycle).
     *
     * Safe to call from any server-thread context EXCEPT inside a chunk-save
     * callback - see the class-level note on the 26.2 save loop.
     *
     * @param level    the level the chunk belongs to
     * @param chunkPos chunk coordinates; no-op if the chunk is not loaded
     */
    public static void persistChunk(Level level, ChunkPos chunkPos) {
        if (level == null || level.isClientSide()) {
            return;
        }
        
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
        if (chunk == null) {
            return;
        }
        
        CompoundTag tcData = buildThaumcraftData(level, chunkPos);
        
        CompoundTag data = chunk.getData(CHUNK_DATA);
        if (data == null) {
            data = new CompoundTag();
        }
        
        // Skip re-dirtying the chunk when nothing changed
        CompoundTag existing = data.contains(TAG_THAUMCRAFT)
                ? data.getCompoundOrEmpty(TAG_THAUMCRAFT)
                : new CompoundTag();
        if (tcData.equals(existing)) {
            return;
        }
        
        if (tcData.isEmpty()) {
            data.remove(TAG_THAUMCRAFT);
        } else {
            data.put(TAG_THAUMCRAFT, tcData);
        }
        chunk.setData(CHUNK_DATA, data);
        chunk.markUnsaved();
    }
    
    /**
     * Builds the current Thaumcraft payload (aura + seals) for a chunk.
     */
    private static CompoundTag buildThaumcraftData(Level level, ChunkPos chunkPos) {
        CompoundTag tcData = new CompoundTag();
        
        AuraChunk aura = AuraHandler.getAuraChunk(level.dimension(), chunkPos.x(), chunkPos.z());
        if (aura != null) {
            CompoundTag auraData = new CompoundTag();
            auraData.putShort(TAG_BASE, aura.getBase());
            auraData.putFloat(TAG_VIS, aura.getVis());
            auraData.putFloat(TAG_FLUX, aura.getFlux());
            tcData.put(TAG_AURA, auraData);
        }
        
        ListTag sealList = new ListTag();
        for (ISealEntity seal : SealHandler.getSealsInChunk(level, chunkPos)) {
            if (seal.getSeal() != null) {
                sealList.add(seal.writeNBT());
            }
        }
        if (!sealList.isEmpty()) {
            tcData.put(TAG_SEALS, sealList);
        }
        
        return tcData;
    }
    
    /**
     * Generate initial aura for a newly created chunk.
     */
    private static void generateChunkAura(ServerLevel level, LevelChunk chunk) {
        RandomSource random = level.getRandom();
        AuraHandler.generateAura(chunk, random);
        
        // Track as dirty so the periodic drain persists it
        ResourceKey<Level> dimension = level.dimension();
        ChunkPos chunkPos = chunk.getPos();
        
        if (!AuraHandler.dirtyChunks.containsKey(dimension)) {
            AuraHandler.dirtyChunks.put(dimension, new java.util.concurrent.CopyOnWriteArrayList<>());
        }
        AuraHandler.dirtyChunks.get(dimension).add(chunkPos);
    }
}
