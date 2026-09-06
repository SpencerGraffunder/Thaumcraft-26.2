package thaumcraft.client.lib.network.playerdata;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.client.gui.ResearchToast;

import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;

/** Client-side handler for {@link PacketSyncKnowledge}. */
public class PacketSyncKnowledgeClient {
    public static void handle(PacketSyncKnowledge msg) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null && msg.data != null) {
            knowledge.deserializeNBT(msg.data);
            
            // Show popup toasts for newly unlocked research
            for (String key : knowledge.getResearchList()) {
                if (knowledge.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP)) {
                    ResearchEntry entry = ResearchCategories.getResearch(key);
                    if (entry != null) {
                        // Show toast notification
                        Minecraft.getInstance().gui.toastManager().addToast(new ResearchToast(entry));
                    }
                    knowledge.clearResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP);
                }
            }
        }
    }
}
