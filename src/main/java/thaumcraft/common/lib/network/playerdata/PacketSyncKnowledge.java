package thaumcraft.common.lib.network.playerdata;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import java.util.function.Consumer;



/**
 * PacketSyncKnowledge - Syncs all player research/knowledge data from server to client.
 * 
 * Sent when:
 * - Player logs in
 * - Research is completed
 * - Knowledge is gained
 * 
 * Ported from 1.12.2
 */
public class PacketSyncKnowledge implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSyncKnowledge> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "packetsyncknowledge"));

    public static final StreamCodec<FriendlyByteBuf, PacketSyncKnowledge> STREAM_CODEC =
        StreamCodec.ofMember(PacketSyncKnowledge::encode, PacketSyncKnowledge::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return this.TYPE;
    }

    
    public CompoundTag data;
    
    public PacketSyncKnowledge() {
        this.data = new CompoundTag();
    }
    
    public PacketSyncKnowledge(Player player) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null) {
            this.data = knowledge.serializeNBT();
            // Clear popup flags after sending
            for (String key : knowledge.getResearchList()) {
                knowledge.clearResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP);
            }
        } else {
            this.data = new CompoundTag();
        }
    }
    
    public static void encode(PacketSyncKnowledge msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }
    
    public static PacketSyncKnowledge decode(FriendlyByteBuf buf) {
        PacketSyncKnowledge msg = new PacketSyncKnowledge();
        msg.data = buf.readNbt();
        return msg;
    }
    
    /**
     * Apply the server's knowledge payload to the client player.
     *
     * This was a no-op, which meant the client player never held any research. Every
     * client-side research gate therefore failed: the Salis Mundus dust trigger checks
     * FIRSTSTEPS@1 in Item.onItemUseFirst BEFORE the crafting table's own useItemOn, so on
     * the client the check failed, the item returned PASS and vanilla opened the crafting
     * table GUI locally while the server converted the table 2.5s later behind it.
     */
    public static Consumer<PacketSyncKnowledge> CLIENT_HANDLER = msg -> {
        if (msg.data == null) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null) {
            knowledge.deserializeNBT(msg.data);
        }
    };

    public static void handle(PacketSyncKnowledge msg, IPayloadContext ctxSupplier) {
        IPayloadContext ctx = ctxSupplier;
        ctx.enqueueWork(() -> CLIENT_HANDLER.accept(msg));
    }
    
}
