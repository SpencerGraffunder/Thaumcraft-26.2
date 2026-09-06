package thaumcraft.client.lib.network.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealHandler;
import java.util.function.Supplier;

import thaumcraft.common.lib.network.misc.PacketSealFilterToClient;

/** Client-side handler for {@link PacketSealFilterToClient}. */
public class PacketSealFilterToClientClient {
    public static void handle(PacketSealFilterToClient msg) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        try {
            ISealEntity seal = SealHandler.getSealEntity(
                level.dimension(), 
                new SealPos(msg.pos, msg.face)
            );
            
            if (seal != null && seal.getSeal() instanceof ISealConfigFilter configFilter) {
                for (int i = 0; i < msg.filterSize; i++) {
                    configFilter.setFilterSlot(i, msg.filter.get(i));
                    configFilter.setFilterSlotSize(i, msg.filterStackSizes.get(i));
                }
            }
        } catch (Exception e) {
            // Silently ignore errors (seal may not exist on client yet)
        }
    }
}
