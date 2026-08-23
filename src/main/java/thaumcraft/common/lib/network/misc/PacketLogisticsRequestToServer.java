package thaumcraft.common.lib.network.misc;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.GolemHelper;


/**
 * Packet sent from client to server to request item provisioning via golems.
 * Used for the logistics system to request items from golem networks.
 * 
 * Ported to 1.20.1
 */
public class PacketLogisticsRequestToServer implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketLogisticsRequestToServer> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetlogisticsrequesttoserver"));

    public static final StreamCodec<FriendlyByteBuf, PacketLogisticsRequestToServer> STREAM_CODEC =
        StreamCodec.ofMember(PacketLogisticsRequestToServer::encode, PacketLogisticsRequestToServer::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    private final BlockPos pos;
    private final Direction side;
    private final ItemStack stack;
    private final int stackSize;
    
    /**
     * Request provisioning to a player
     */
    public PacketLogisticsRequestToServer(ItemStack stack, int size) {
        this.pos = null;
        this.side = null;
        this.stack = stack;
        this.stackSize = size;
    }
    
    /**
     * Request provisioning to a block position
     */
    public PacketLogisticsRequestToServer(BlockPos pos, Direction side, ItemStack stack, int size) {
        this.pos = pos;
        this.side = side;
        this.stack = stack;
        this.stackSize = size;
    }
    
    public static void encode(PacketLogisticsRequestToServer packet, FriendlyByteBuf buf) {
        if (packet.pos == null || packet.side == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeLong(packet.pos.asLong());
            buf.writeByte(packet.side.ordinal());
        }
        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, packet.stack);
        buf.writeInt(packet.stackSize);
    }
    
    public static PacketLogisticsRequestToServer decode(FriendlyByteBuf buf) {
        boolean hasPos = buf.readBoolean();
        BlockPos pos = null;
        Direction side = null;
        if (hasPos) {
            pos = BlockPos.of(buf.readLong());
            side = Direction.values()[buf.readByte()];
        }
        ItemStack stack = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
        int stackSize = buf.readInt();
        
        if (pos != null) {
            return new PacketLogisticsRequestToServer(pos, side, stack, stackSize);
        } else {
            return new PacketLogisticsRequestToServer(stack, stackSize);
        }
    }
    
    public static void handle(PacketLogisticsRequestToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            
            Level level = player.level();
            int remainingSize = packet.stackSize;
            int uniqueId = 0;
            
            // Split requests into max stack sizes
            while (remainingSize > 0) {
                ItemStack requestStack = packet.stack.copy();
                int count = Math.min(remainingSize, requestStack.getMaxStackSize());
                requestStack.setCount(count);
                remainingSize -= count;
                
                if (packet.pos != null) {
                    // Request to a specific block position
                    GolemHelper.requestProvisioning(level, packet.pos, packet.side, requestStack, uniqueId);
                } else {
                    // Request to the player
                    GolemHelper.requestProvisioning(level, player, requestStack, uniqueId);
                }
                uniqueId++;
            }
        });
    }
}
