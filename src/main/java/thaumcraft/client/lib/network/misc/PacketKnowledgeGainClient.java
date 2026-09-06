package thaumcraft.client.lib.network.misc;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.init.ModSounds;

import thaumcraft.common.lib.network.misc.PacketKnowledgeGain;

/** Client-side handler for {@link PacketKnowledgeGain}. */
public class PacketKnowledgeGainClient {
    public static void handle(PacketKnowledgeGain msg) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        
        IPlayerKnowledge.EnumKnowledgeType type = IPlayerKnowledge.EnumKnowledgeType.values()[msg.type];
        ResearchCategory cat = (msg.category.length() > 0) 
            ? ResearchCategories.getResearchCategory(msg.category) 
            : null;
        
        // TODO: Add HUD handler integration when client rendering is implemented
        // RenderEventHandler.hudHandler.knowledgeGainTrackers.add(
        //     new HudHandler.KnowledgeGainTracker(type, cat, 40 + rand.nextInt(20), rand.nextLong())
        // );
        
        // Play knowledge gain sound
        if (ModSounds.LEARN.get() != null) {
            mc.level.playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                ModSounds.LEARN.get(), SoundSource.AMBIENT,
                1.0f, 1.0f, false
            );
        }
    }
}
