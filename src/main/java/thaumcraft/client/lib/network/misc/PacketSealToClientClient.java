package thaumcraft.client.lib.network.misc;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;

import thaumcraft.common.lib.network.misc.PacketSealToClient;

/** Client-side handler for {@link PacketSealToClient}. */
public class PacketSealToClientClient {
    public static void handle(PacketSealToClient msg) {
        try {
            if (msg.type.equals("REMOVE")) {
                // Remove seal from client
                SealHandler.removeSealEntity(
                    net.minecraft.client.Minecraft.getInstance().level, 
                    new SealPos(msg.pos, msg.face), 
                    true
                );
            } else {
                // Create or update seal on client
                ISeal template = SealHandler.getSeal(msg.type);
                if (template == null) {
                    Thaumcraft.LOGGER.warn("Unknown seal type in packet: {}", msg.type);
                    return;
                }
                
                // Create new seal instance
                ISeal seal = template.getClass().getDeclaredConstructor().newInstance();
                SealEntity sealEntity = new SealEntity(
                    net.minecraft.client.Minecraft.getInstance().level, 
                    new SealPos(msg.pos, msg.face), 
                    seal
                );
                
                // Apply area configuration
                if (msg.area != 0L) {
                    sealEntity.setArea(BlockPos.of(msg.area));
                }
                
                // Apply toggle configuration
                if (msg.props != null && seal instanceof ISealConfigToggles configToggles) {
                    for (int i = 0; i < msg.props.length; i++) {
                        configToggles.setToggle(i, msg.props[i]);
                    }
                }
                
                // Apply filter configuration
                if (seal instanceof ISealConfigFilter configFilter) {
                    configFilter.setBlacklist(msg.blacklist);
                    for (int i = 0; i < msg.filterSize; i++) {
                        configFilter.setFilterSlot(i, msg.filter.get(i));
                        configFilter.setFilterSlotSize(i, msg.filterStackSize.get(i));
                    }
                }
                
                // Apply common properties
                sealEntity.setPriority(msg.priority);
                sealEntity.setColor(msg.color);
                sealEntity.setLocked(msg.locked);
                sealEntity.setRedstoneSensitive(msg.redstone);
                sealEntity.setOwner(msg.owner);
                
                // Add to handler (replaces existing if present)
                SealHandler.addSealEntity(net.minecraft.client.Minecraft.getInstance().level, sealEntity);
            }
        } catch (Exception e) {
            Thaumcraft.LOGGER.error("Error handling seal packet at {}", msg.pos, e);
        }
    }
}
