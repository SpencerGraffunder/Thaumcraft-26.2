package thaumcraft.common.lib.network.misc;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import java.util.function.Consumer;



/**
 * PacketSealToClient - Syncs seal data from server to client.
 * 
 * Used when:
 * - A seal is placed in the world
 * - A seal configuration is changed
 * - A seal is removed (type = "REMOVE")
 * 
 * Ported from 1.12.2. Key changes:
 * - IMessage/IMessageHandler pattern replaced with static encode/decode/handle methods
 * - ByteBuf -> FriendlyByteBuf with built-in ItemStack support
 * - EnumFacing -> Direction
 * - Proxy.getClientWorld() -> Minecraft.getInstance().level
 */
public class PacketSealToClient implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSealToClient> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetsealtoclient"));

    public static final StreamCodec<FriendlyByteBuf, PacketSealToClient> STREAM_CODEC =
        StreamCodec.ofMember(PacketSealToClient::encode, PacketSealToClient::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public BlockPos pos;
    public Direction face;
    public String type;
    public long area;
    public boolean[] props;
    public boolean blacklist;
    public byte filterSize;
    public NonNullList<ItemStack> filter;
    public NonNullList<Integer> filterStackSize;
    public byte priority;
    public byte color;
    public boolean locked;
    public boolean redstone;
    public String owner;
    
    /**
     * Default constructor for decoding
     */
    public PacketSealToClient() {
        props = null;
    }
    
    /**
     * Create a packet from a seal entity
     * @param se The seal entity to sync
     */
    public PacketSealToClient(ISealEntity se) {
        props = null;
        pos = se.getSealPos().pos;
        face = se.getSealPos().face;
        type = (se.getSeal() == null) ? "REMOVE" : se.getSeal().getKey();
        
        // Area configuration
        if (se.getSeal() != null && se.getSeal() instanceof ISealConfigArea) {
            area = se.getArea().asLong();
        }
        
        // Toggle configuration
        if (se.getSeal() != null && se.getSeal() instanceof ISealConfigToggles configToggles) {
            ISealConfigToggles.SealToggle[] toggles = configToggles.getToggles();
            props = new boolean[toggles.length];
            for (int i = 0; i < toggles.length; i++) {
                props[i] = toggles[i].getValue();
            }
        }
        
        // Filter configuration
        if (se.getSeal() != null && se.getSeal() instanceof ISealConfigFilter configFilter) {
            blacklist = configFilter.isBlacklist();
            filterSize = (byte) configFilter.getFilterSize();
            filter = configFilter.getInv();
            filterStackSize = configFilter.getSizes();
        } else {
            filterSize = 0;
            filter = NonNullList.create();
            filterStackSize = NonNullList.create();
        }
        
        priority = se.getPriority();
        color = se.getColor();
        locked = se.isLocked();
        redstone = se.isRedstoneSensitive();
        owner = se.getOwner();
    }
    
    /**
     * Encode the packet to buffer
     */
    public static void encode(PacketSealToClient msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.pos.asLong());
        buf.writeByte(msg.face.ordinal());
        buf.writeByte(msg.priority);
        buf.writeByte(msg.color);
        buf.writeBoolean(msg.locked);
        buf.writeBoolean(msg.redstone);
        buf.writeUtf(msg.owner);
        buf.writeUtf(msg.type);
        buf.writeBoolean(msg.blacklist);
        buf.writeByte(msg.filterSize);
        
        // Write filter items and sizes
        for (int i = 0; i < msg.filterSize; i++) {
            net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, msg.filter.get(i));
            buf.writeShort(msg.filterStackSize.get(i));
        }
        
        // Write area if present
        if (msg.area != 0L) {
            buf.writeBoolean(true);
            buf.writeLong(msg.area);
        } else {
            buf.writeBoolean(false);
        }
        
        // Write toggles if present
        if (msg.props != null && msg.props.length > 0) {
            buf.writeByte(msg.props.length);
            for (boolean prop : msg.props) {
                buf.writeBoolean(prop);
            }
        } else {
            buf.writeByte(0);
        }
    }
    
    /**
     * Decode the packet from buffer
     */
    public static PacketSealToClient decode(FriendlyByteBuf buf) {
        PacketSealToClient msg = new PacketSealToClient();
        
        msg.pos = BlockPos.of(buf.readLong());
        msg.face = Direction.values()[buf.readByte()];
        msg.priority = buf.readByte();
        msg.color = buf.readByte();
        msg.locked = buf.readBoolean();
        msg.redstone = buf.readBoolean();
        msg.owner = buf.readUtf();
        msg.type = buf.readUtf();
        msg.blacklist = buf.readBoolean();
        msg.filterSize = buf.readByte();
        
        // Read filter items and sizes
        msg.filter = NonNullList.withSize(msg.filterSize, ItemStack.EMPTY);
        msg.filterStackSize = NonNullList.withSize(msg.filterSize, 0);
        for (int i = 0; i < msg.filterSize; i++) {
            msg.filter.set(i, net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
            msg.filterStackSize.set(i, (int) buf.readShort());
        }
        
        // Read area if present
        boolean hasArea = buf.readBoolean();
        if (hasArea) {
            msg.area = buf.readLong();
        }
        
        // Read toggles if present
        int propsLength = buf.readByte();
        if (propsLength > 0) {
            msg.props = new boolean[propsLength];
            for (int i = 0; i < propsLength; i++) {
                msg.props[i] = buf.readBoolean();
            }
        }
        
        return msg;
    }
    
    public static Consumer<PacketSealToClient> CLIENT_HANDLER = msg -> {};

    /**
     * Handle the packet on client
     */
    public static void handle(PacketSealToClient msg, IPayloadContext ctxSupplier) {
        IPayloadContext ctx = ctxSupplier;
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(msg));
    }
    
}
