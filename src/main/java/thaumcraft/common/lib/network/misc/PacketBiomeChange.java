package thaumcraft.common.lib.network.misc;
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


/**
 * Packet to update biome at a specific position on the client.
 * Used for Taint spreading, biome transformation effects, etc.
 * 
 * In 1.20.1, biomes are stored per-section and use ResourceKeys instead of IDs.
 * 
 * Ported to 1.20.1
 */
public class PacketBiomeChange implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketBiomeChange> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetbiomechange"));

    public static final StreamCodec<FriendlyByteBuf, PacketBiomeChange> STREAM_CODEC =
        StreamCodec.ofMember(PacketBiomeChange::encode, PacketBiomeChange::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    private final int x;
    private final int y;
    private final int z;
    private final String biomeId;
    
    public PacketBiomeChange(BlockPos pos, ResourceKey<Biome> biome) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.biomeId = biome.identifier().toString();
    }
    
    public PacketBiomeChange(int x, int y, int z, String biomeId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.biomeId = biomeId;
    }
    
    public static void encode(PacketBiomeChange packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.x);
        buf.writeInt(packet.y);
        buf.writeInt(packet.z);
        buf.writeUtf(packet.biomeId);
    }
    
    public static PacketBiomeChange decode(FriendlyByteBuf buf) {
        return new PacketBiomeChange(
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readUtf(32767)
        );
    }
    
    public static void handle(PacketBiomeChange packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(packet));
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketBiomeChange packet) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        BlockPos pos = new BlockPos(packet.x, packet.y, packet.z);
        
        // Get the biome from registry
        Identifier biomeRL = Identifier.withDefaultNamespace(packet.biomeId);
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
