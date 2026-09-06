package thaumcraft.common.lib.network.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealEntity;
import java.util.function.Consumer;



/**
 * Packet to sync seal filter configuration to the client.
 * Contains the filter inventory and stack size limits for filtered seals.
 * 
 * Ported to 1.20.1
 */
public class PacketSealFilterToClient implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketSealFilterToClient> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetsealfiltertoclient"));

    public static final StreamCodec<FriendlyByteBuf, PacketSealFilterToClient> STREAM_CODEC =
        StreamCodec.ofMember(PacketSealFilterToClient::encode, PacketSealFilterToClient::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    public final BlockPos pos;
    public final Direction face;
    public final byte filterSize;
    public final NonNullList<ItemStack> filter;
    public final NonNullList<Integer> filterStackSizes;
    
    public PacketSealFilterToClient(ISealEntity sealEntity) {
        this.pos = sealEntity.getSealPos().pos;
        this.face = sealEntity.getSealPos().face;
        
        if (sealEntity.getSeal() instanceof ISealConfigFilter configFilter) {
            this.filterSize = (byte) configFilter.getFilterSize();
            this.filter = configFilter.getInv();
            this.filterStackSizes = configFilter.getSizes();
        } else {
            this.filterSize = 0;
            this.filter = NonNullList.create();
            this.filterStackSizes = NonNullList.create();
        }
    }
    
    private PacketSealFilterToClient(BlockPos pos, Direction face, byte filterSize, 
                                     NonNullList<ItemStack> filter, NonNullList<Integer> filterStackSizes) {
        this.pos = pos;
        this.face = face;
        this.filterSize = filterSize;
        this.filter = filter;
        this.filterStackSizes = filterStackSizes;
    }
    
    public static void encode(PacketSealFilterToClient packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.pos.asLong());
        buf.writeByte(packet.face.ordinal());
        buf.writeByte(packet.filterSize);
        
        for (int i = 0; i < packet.filterSize; i++) {
            net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, packet.filter.get(i));
            buf.writeShort(packet.filterStackSizes.get(i));
        }
    }
    
    public static PacketSealFilterToClient decode(FriendlyByteBuf buf) {
        BlockPos pos = BlockPos.of(buf.readLong());
        Direction face = Direction.values()[buf.readByte()];
        byte filterSize = buf.readByte();
        
        NonNullList<ItemStack> filter = NonNullList.withSize(filterSize, ItemStack.EMPTY);
        NonNullList<Integer> filterStackSizes = NonNullList.withSize(filterSize, 0);
        
        for (int i = 0; i < filterSize; i++) {
            filter.set(i, net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
            filterStackSizes.set(i, (int) buf.readShort());
        }
        
        return new PacketSealFilterToClient(pos, face, filterSize, filter, filterStackSizes);
    }
    
    public static Consumer<PacketSealFilterToClient> CLIENT_HANDLER = msg -> {};

    public static void handle(PacketSealFilterToClient packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(packet));
    }
    
}
