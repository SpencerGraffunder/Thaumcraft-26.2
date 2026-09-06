package thaumcraft.client.lib.network.misc;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import thaumcraft.common.lib.network.misc.PacketBiomeChange;

/** Client-side handler for {@link PacketBiomeChange}. */
public class PacketBiomeChangeClient {
    public static void handle(PacketBiomeChange msg) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        BlockPos pos = new BlockPos(msg.x, msg.y, msg.z);
        
        // Get the biome from registry
        Identifier biomeRL = Identifier.parse(msg.biomeId);
        var biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Holder<Biome> biomeHolder = biomeRegistry.get(ResourceKey.create(Registries.BIOME, biomeRL)).orElse(null);
        
        if (biomeHolder == null) return;
        
        // Update biome in the chunk section
        LevelChunk chunk = level.getChunkAt(pos);
        int sectionIndex = level.getSectionIndex(pos.getY());
        
        if (sectionIndex >= 0 && sectionIndex < chunk.getSections().length) {
            LevelChunkSection section = chunk.getSection(sectionIndex);
            
            // Calculate position within section (biomes are stored at 4x4x4 resolution)
            int bx = (pos.getX() & 15) >> 2;
            int by = (pos.getY() & 15) >> 2;
            int bz = (pos.getZ() & 15) >> 2;
            
            // The biomes container is exposed as read-only in most cases,
            // but we can cast it to PalettedContainer for writing
            if (section.getBiomes() instanceof PalettedContainer<Holder<Biome>> biomes) {
                biomes.set(bx, by, bz, biomeHolder);
            }
            
            // Mark chunk for re-render
            // TODO(feature-review): re-add client render refresh for the changed
            // biome section (26.2 removed public LevelRenderer.setSectionDirty).
        }
    }
}
